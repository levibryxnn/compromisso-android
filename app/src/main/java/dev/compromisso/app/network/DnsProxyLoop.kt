package dev.compromisso.app.network

import android.net.VpnService
import android.os.ParcelFileDescriptor
import dev.compromisso.app.domain.DomainPolicyEngine
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * DNS-only IPv4 VPN. Read loop never waits for the network: bounded DNS-over-TLS workers
 * handle requests; writes are serialized. No history or URLs are stored.
 * This deliberately does NOT claim full-tunnel, DoH or IPv6 interception.
 */
class DnsProxyLoop(
    private val vpnService: VpnService,
    private val tunnel: ParcelFileDescriptor,
    initialBlockedDomains: Set<String>,
    private val upstreamHealthChanged: (Boolean) -> Unit,
) : Runnable {
    private val blockedDomains = AtomicReference(initialBlockedDomains.toSet())
    private val running = AtomicBoolean(true)
    private val resolvers = ConcurrentHashMap.newKeySet<DnsOverTlsResolver>()
    private val localResolver = ThreadLocal.withInitial {
        DnsOverTlsResolver(vpnService).also { resolvers.add(it) }
    }
    private val tasks = ThreadPoolExecutor(
        4, 4, 20L, TimeUnit.SECONDS, ArrayBlockingQueue(64),
        { task -> Thread(task, "compromisso-dot-worker").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy(),
    )
    private val outputLock = Any()
    private val realLookupCompleted = AtomicBoolean(false)
    private var failedUpstreamRequests = 0
    private val healthLock = Any()
    private var lastReportedHealth: Boolean? = null

    fun updateBlockedDomains(domains: Set<String>) { blockedDomains.set(domains.toSet()) }

    private fun onUpstreamResult(success: Boolean) {
        val changed = synchronized(healthLock) {
            if (success) failedUpstreamRequests = 0
            else failedUpstreamRequests = (failedUpstreamRequests + 1).coerceAtMost(3)
            val healthy = failedUpstreamRequests < 2
            if (lastReportedHealth == healthy) null else {
                lastReportedHealth = healthy
                healthy
            }
        }
        // Avoid posting one Android main-thread task for every resolved domain.
        if (changed != null) upstreamHealthChanged(changed)
    }

    override fun run() {
        val input = FileInputStream(tunnel.fileDescriptor)
        val output = FileOutputStream(tunnel.fileDescriptor)
        try {
            // A protected TLS query proves upstream operation, not merely TUN creation.
            submitTask {
                val healthy = DnsProbeQuery.isHealthy(localResolver.get().query(DnsProbeQuery.request()))
                // For the first probe, one failure is enough to show DEGRADED.
                if (!realLookupCompleted.get()) {
                    if (!healthy) synchronized(healthLock) { failedUpstreamRequests = 2 }
                    onUpstreamResult(healthy)
                }
            }
            val packetBuffer = ByteArray(MAX_PACKET_SIZE)
            while (running.get() && !Thread.currentThread().isInterrupted) {
                val length = try { input.read(packetBuffer) } catch (_: Exception) { break }
                if (length <= 0) continue
                val packet = Ipv4UdpPacket.parseDns(packetBuffer, length) ?: continue
                val question = DnsCodec.readFirstQuestion(packet.dnsPayload) ?: continue
                val blocked = DomainPolicyEngine.isBlocked(question.name, blockedDomains.get())
                if (blocked) {
                    writeReply(output, packet, DnsCodec.buildNxDomain(packet.dnsPayload))
                } else {
                    if (!submitTask {
                        val reply = localResolver.get().query(packet.dnsPayload)
                        realLookupCompleted.set(true)
                        onUpstreamResult(reply != null && (reply[3].toInt() and 15) != 2)
                        writeReply(output, packet, reply ?: DnsCodec.buildServerFailure(packet.dnsPayload))
                    }) {
                        // Saturation must not cause unbounded memory growth or fail-open behavior.
                        writeReply(output, packet, DnsCodec.buildServerFailure(packet.dnsPayload))
                    }
                }
            }
        } finally {
            running.set(false)
            tasks.shutdownNow()
            resolvers.forEach { runCatching { it.close() } }
            resolvers.clear()
        }
    }

    private fun submitTask(task: () -> Unit): Boolean = try {
        tasks.execute(task)
        true
    } catch (_: java.util.concurrent.RejectedExecutionException) {
        false
    }

    private fun writeReply(output: FileOutputStream, request: Ipv4UdpPacket.DnsPacket, payload: ByteArray?) {
        if (!running.get() || payload == null) return
        val packet = Ipv4UdpPacket.buildResponse(request, payload)
        synchronized(outputLock) {
            if (running.get()) {
                try {
                    output.write(packet)
                } catch (_: Exception) {
                    // A dead TUN must terminate the reader/service, never leave a green indicator.
                    running.set(false)
                    runCatching { tunnel.close() }
                }
            }
        }
    }

    fun shutdown() {
        running.set(false)
        tasks.shutdownNow()
        resolvers.forEach { runCatching { it.close() } }
    }

    companion object { private const val MAX_PACKET_SIZE = 32_768 }
}
