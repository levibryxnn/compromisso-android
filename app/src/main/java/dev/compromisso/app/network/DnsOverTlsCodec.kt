package dev.compromisso.app.network

/** RFC 7858: each DNS message in DNS-over-TLS is prefixed by its unsigned 16-bit length. */
object DnsOverTlsCodec {
    fun encode(message: ByteArray): ByteArray {
        require(message.size in 12..65_535) { "Invalid DNS frame length" }
        return ByteArray(message.size + 2).also { frame ->
            frame[0] = (message.size ushr 8).toByte()
            frame[1] = message.size.toByte()
            message.copyInto(frame, 2)
        }
    }

    fun decodeLength(high: Int, low: Int): Int {
        require(high in 0..255 && low in 0..255)
        return (high shl 8) or low
    }
}
