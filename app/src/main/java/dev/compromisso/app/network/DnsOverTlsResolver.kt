package dev.compromisso.app.network

import android.net.VpnService
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * DNS-over-TLS with platform PKIX certificate validation + hostname verification.
 * No UDP/53 fallback. The underlying socket is exempted from VPN routing before connecting.
 * Each resolver is confined to one bounded worker thread; never share an instance across workers.
 */
class DnsOverTlsResolver(private val vpn: VpnService) : Closeable {
    private var tls: SSLSocket? = null

    /** One bounded retry after a broken/stale TLS connection; never use unencrypted DNS. */
    fun query(request: ByteArray): ByteArray? {
        if (request.size !in 12..MAX_DNS_MESSAGE_SIZE || DnsCodec.readFirstQuestion(request) == null) return null
        repeat(2) {
            try {
                val socket = tls?.takeIf { it.isConnected && !it.isClosed } ?: connect().also { tls = it }
                val output = DataOutputStream(socket.outputStream)
                val input = DataInputStream(socket.inputStream)
                output.write(DnsOverTlsCodec.encode(request))
                output.flush()
                val length = DnsOverTlsCodec.decodeLength(input.readUnsignedByte(), input.readUnsignedByte())
                if (length < 12 || length > MAX_DNS_MESSAGE_SIZE) {
                    close()
                    return null
                }
                val response = ByteArray(length)
                input.readFully(response)
                // A mismatched reply is suspicious and must not be retried as valid data.
                if (!DnsCodec.isMatchingResponse(request, response)) {
                    close()
                    return null
                }
                return response
            } catch (_: Exception) {
                close()
            }
        }
        return null
    }

    private fun connect(): SSLSocket {
        val raw = Socket()
        try {
            check(vpn.protect(raw)) { "Unable to protect DNS socket" }
            raw.connect(InetSocketAddress(DNS_IP, DOT_PORT), TIMEOUT_MS)
            raw.soTimeout = TIMEOUT_MS
            val socket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                .createSocket(raw, DNS_HOSTNAME, DOT_PORT, true) as SSLSocket
            socket.soTimeout = TIMEOUT_MS
            val parameters: SSLParameters = socket.sslParameters
            parameters.endpointIdentificationAlgorithm = "HTTPS"
            socket.sslParameters = parameters
            socket.startHandshake()
            return socket
        } catch (error: Exception) {
            runCatching { raw.close() }
            throw error
        }
    }

    override fun close() {
        runCatching { tls?.close() }
        tls = null
    }

    companion object {
        private const val DNS_IP = "1.1.1.1"
        private const val DNS_HOSTNAME = "cloudflare-dns.com"
        private const val DOT_PORT = 853
        private const val TIMEOUT_MS = 3000
        private const val MAX_DNS_MESSAGE_SIZE = 8192
    }
}
