package dev.compromisso.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DnsCodecTest {
    @Test
    fun readsQuestionAndBuildsNxDomain() {
        val query = queryFor("sub.example.com")
        val question = DnsCodec.readFirstQuestion(query)
        assertNotNull(question)
        assertEquals("sub.example.com", question!!.name)

        val response = DnsCodec.buildNxDomain(query)!!
        val flags = ((response[2].toInt() and 0xFF) shl 8) or (response[3].toInt() and 0xFF)
        assertEquals(1, flags ushr 15)
        assertEquals(3, flags and 0x000F)
    }

    private fun queryFor(host: String): ByteArray {
        val labels = host.split('.')
        val qnameSize = labels.sumOf { 1 + it.length } + 1
        val message = ByteArray(12 + qnameSize + 4)
        message[0] = 0x12
        message[1] = 0x34
        message[2] = 0x01
        message[5] = 0x01
        var offset = 12
        labels.forEach { label ->
            message[offset++] = label.length.toByte()
            label.toByteArray(Charsets.US_ASCII).copyInto(message, offset)
            offset += label.length
        }
        message[offset++] = 0
        message[offset++] = 0
        message[offset++] = 1 // A
        message[offset++] = 0
        message[offset] = 1 // IN
        return message
    }
}
