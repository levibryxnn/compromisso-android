package dev.compromisso.app.network

/** Deterministic, non-user-related A query. Sent once per VPN start to test upstream DoT. */
object DnsProbeQuery {

    fun request(): ByteArray {
        val header = byteArrayOf(
            0x43, 0x50, 0x01, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0,
        )
        val name = byteArrayOf(7) + "example".toByteArray(Charsets.US_ASCII) +
            byteArrayOf(3) + "com".toByteArray(Charsets.US_ASCII) + byteArrayOf(0)
        return header + name + byteArrayOf(0, 1, 0, 1)
    }

    fun isHealthy(response: ByteArray?): Boolean = response != null && response.size >= 12 &&
        (response[3].toInt() and 0x0F) == 0 && DnsCodec.isMatchingResponse(request(), response)
}
