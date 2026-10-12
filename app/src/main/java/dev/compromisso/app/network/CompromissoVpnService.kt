package dev.compromisso.app.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.compromisso.app.MainActivity
import dev.compromisso.app.R
import dev.compromisso.app.data.BlockedDomainDatabaseHelper
import dev.compromisso.app.data.SQLiteBlockedDomainRepository
import dev.compromisso.app.policy.ProtectionController
import dev.compromisso.app.policy.ProtectionRuntime
import dev.compromisso.app.policy.ProtectionState
import dev.compromisso.app.security.SecurityConfigStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CompromissoVpnService : VpnService() {
    private lateinit var config: SecurityConfigStore
    private lateinit var repository: SQLiteBlockedDomainRepository
    private val main = Handler(Looper.getMainLooper())
    private val lifecycle: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "compromisso-vpn-lifecycle").apply { isDaemon = true }
    }
    private var tunnel: ParcelFileDescriptor? = null
    private var packetReader: ExecutorService? = null
    @Volatile private var proxyLoop: DnsProxyLoop? = null
    private var starting = false
    private var generation = 0L

    override fun onCreate() {
        super.onCreate()
        config = SecurityConfigStore(applicationContext)
        repository = SQLiteBlockedDomainRepository(BlockedDomainDatabaseHelper(applicationContext))
        ProtectionRuntime.markInactive()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                config.protectionRequested = false
                stopVpn()
                stopSelf()
                START_NOT_STICKY
            }
            ACTION_RELOAD_RULES -> {
                if (!config.protectionRequested) return START_NOT_STICKY
                if (tunnel == null) startVpnIfNeeded() else reloadRulesAsync()
                START_STICKY
            }
            ACTION_START, null -> {
                if (intent?.action == ACTION_START || config.protectionRequested) {
                    startVpnIfNeeded()
                    START_STICKY
                } else {
                    stopSelf()
                    START_NOT_STICKY
                }
            }
            else -> START_NOT_STICKY
        }
    }

    override fun onRevoke() {
        runCatching { config.protectionRequested = false }
        stopVpn()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        val failed = ProtectionRuntime.state == ProtectionState.ERROR
        stopVpn()
        if (failed) {
            ProtectionRuntime.markError()
            broadcastStatus()
        }
        lifecycle.shutdownNow()
        super.onDestroy()
    }

    private fun startVpnIfNeeded() {
        if (!config.protectionRequested || tunnel != null || starting) return
        try { startAsForeground() } catch (_: RuntimeException) { failStartup(); return }
        starting = true
        val session = ++generation
        ProtectionRuntime.markStarting()
        broadcastStatus()
        lifecycle.execute {
            // Decryption and DB access happen off the Android main thread.
            val rules = runCatching { repository.listEnabledDomains() }
            val revision = config.rulesRevision
            main.post {
                if (session != generation || !starting || !config.protectionRequested) return@post
                if (rules.isFailure) { failStartup(); return@post }
                val established = runCatching {
                    Builder()
                        .setSession(getString(R.string.app_name))
                        .setMtu(1500)
                        .addAddress(CLIENT_ADDRESS, 32)
                        .addDnsServer(DNS_ADDRESS)
                        .addRoute(DNS_ADDRESS, 32)
                        .setBlocking(true)
                        .establish()
                }.getOrNull()
                if (established == null) { failStartup(); return@post }
                tunnel = established
                starting = false
                val loop = DnsProxyLoop(this, established, rules.getOrThrow()) { healthy ->
                    main.post {
                        if (tunnel !== established || !config.protectionRequested) return@post
                        val next = if (healthy) ProtectionState.OPERATIONAL else ProtectionState.DEGRADED
                        if (ProtectionRuntime.state != next) {
                            if (healthy) ProtectionRuntime.markOperational() else ProtectionRuntime.markDegraded()
                            broadcastStatus()
                        }
                    }
                }
                proxyLoop = loop
                ProtectionRuntime.markRulesApplied(revision)
                packetReader = Executors.newSingleThreadExecutor { task ->
                    Thread(task, "compromisso-tun-reader").apply { isDaemon = true }
                }.also { pool ->
                    pool.execute {
                        try { loop.run() } finally {
                            main.post {
                                if (tunnel === established) {
                                    val unexpected = config.protectionRequested
                                    runCatching { config.protectionRequested = false }
                                    stopVpn()
                                    if (unexpected) {
                                        ProtectionRuntime.markError()
                                        broadcastStatus()
                                    }
                                    stopSelf()
                                }
                            }
                        }
                    }
                }
                // If rules changed while the tunnel was being established, reload immediately.
                if (config.rulesRevision != revision) reloadRulesAsync()
                broadcastStatus()
            }
        }
    }

    private fun reloadRulesAsync() {
        val current = tunnel ?: return
        val generationAtStart = generation
        lifecycle.execute {
            val rules = runCatching { repository.listEnabledDomains() }
            val revision = config.rulesRevision
            main.post {
                if (generationAtStart != generation || tunnel !== current) return@post
                if (rules.isFailure) {
                    // Never silently keep a stale policy when storage is unavailable.
                    failStartup()
                    return@post
                }
                proxyLoop?.updateBlockedDomains(rules.getOrThrow())
                ProtectionRuntime.markRulesApplied(revision)
                broadcastStatus()
                if (revision != config.rulesRevision) reloadRulesAsync()
            }
        }
    }

    private fun failStartup() {
        runCatching { config.protectionRequested = false }
        stopVpn()
        ProtectionRuntime.markError()
        broadcastStatus()
        stopSelf()
    }

    private fun stopVpn() {
        generation++
        starting = false
        ProtectionRuntime.markInactive()
        broadcastStatus()
        proxyLoop?.shutdown()
        proxyLoop = null
        runCatching { tunnel?.close() }
        tunnel = null
        packetReader?.shutdownNow()
        packetReader = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun broadcastStatus() {
        sendBroadcast(Intent(ProtectionController.ACTION_STATUS_CHANGED)
            .setPackage(packageName)
            .putExtra(ProtectionController.EXTRA_STATE, ProtectionRuntime.state.name)
            .putExtra(ProtectionController.EXTRA_RULES_REVISION, ProtectionRuntime.appliedRulesRevision))
    }

    private fun startAsForeground() {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_compromisso)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setContentIntent(pending)
            .setOngoing(true).setSilent(true).setCategory(NotificationCompat.CATEGORY_SERVICE).build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID,
                getString(R.string.vpn_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.vpn_notification_text)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "dev.compromisso.app.action.START_VPN"
        const val ACTION_STOP = "dev.compromisso.app.action.STOP_VPN"
        const val ACTION_RELOAD_RULES = "dev.compromisso.app.action.RELOAD_RULES"
        private const val CLIENT_ADDRESS = "10.111.222.2"
        private const val DNS_ADDRESS = "10.111.222.1"
        private const val NOTIFICATION_CHANNEL_ID = "compromisso_protection"
        private const val NOTIFICATION_ID = 1001
    }
}
