package dev.compromisso.app.network

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class DnsOverTlsCodecTest {
    @Test fun lengthPrefixMatchesWireFormat() {
        val bytes = ByteArray(260)
        val frame = DnsOverTlsCodec.encode(bytes)
        assertEquals(262, frame.size)
        assertEquals(260, DnsOverTlsCodec.decodeLength(frame[0].toInt() and 255, frame[1].toInt() and 255))
        assertArrayEquals(bytes, frame.copyOfRange(2, frame.size))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyDnsMessage() { DnsOverTlsCodec.encode(ByteArray(0)) }
}
