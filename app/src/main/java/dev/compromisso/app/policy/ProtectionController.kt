package dev.compromisso.app.policy

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import dev.compromisso.app.network.CompromissoVpnService
import dev.compromisso.app.security.SecurityConfigStore

class ProtectionController(
    private val context: Context,
    private val config: SecurityConfigStore,
) {
    fun vpnPermissionIntent(): Intent? = VpnService.prepare(context)

    fun startProtection() {
        config.protectionRequested = true
        val intent = Intent(context, CompromissoVpnService::class.java).apply {
            action = CompromissoVpnService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (error: RuntimeException) {
            config.protectionRequested = false
            throw error
        }
    }

    fun stopProtection() {
        config.protectionRequested = false
        ProtectionRuntime.markInactive()
        context.stopService(Intent(context, CompromissoVpnService::class.java))
        context.sendBroadcast(
            Intent(ACTION_STATUS_CHANGED)
                .setPackage(context.packageName)
                .putExtra(EXTRA_STATE, ProtectionRuntime.state.name)
        )
    }

    fun reloadRules() {
        if (ProtectionRuntime.state == ProtectionState.OFF || ProtectionRuntime.state == ProtectionState.ERROR) return
        val intent = Intent(context, CompromissoVpnService::class.java).apply {
            action = CompromissoVpnService.ACTION_RELOAD_RULES
        }
        context.startService(intent)
    }

    companion object {
        const val ACTION_STATUS_CHANGED = "dev.compromisso.app.action.PROTECTION_STATUS_CHANGED"
        const val EXTRA_STATE = "state"
        const val EXTRA_RULES_REVISION = "rules_revision"
    }
}
