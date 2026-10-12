package dev.compromisso.app.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsValidationSecurityTest {
    private fun dns(host: String = "Example.COM"): ByteArray {
        val labels = host.split('.')
        val result = ByteArray(12 + labels.sumOf { it.length + 1 } + 1 + 4)
        result[0] = 0x12
        result[1] = 0x34
        result[2] = 0x01
        result[5] = 0x01
        var pos = 12
        for (label in labels) {
            result[pos++] = label.length.toByte()
            label.toByteArray(Charsets.US_ASCII).copyInto(result, pos)
            pos += label.length
        }
        result[pos++] = 0
        result[pos++] = 0
        result[pos++] = 1
        result[pos++] = 0
        result[pos] = 1
        return result
    }

    @Test fun acceptsOnlyMatchingDnsResponse() {
        val query = dns()
        val reply = query.copyOf().apply { this[2] = 0x81.toByte(); this[3] = 0x80.toByte() }
        assertTrue(DnsCodec.isMatchingResponse(query, reply))
        assertFalse(DnsCodec.isMatchingResponse(query, reply.copyOf().apply { this[1] = 0x35 }))
        assertFalse(DnsCodec.isMatchingResponse(query, reply.copyOf().apply { this[lastIndex - 2] = 28 }))
        assertFalse(DnsCodec.isMatchingResponse(query, reply.copyOf().apply { this[lastIndex] = 3 }))
        assertFalse(DnsCodec.isMatchingResponse(query, query))
        assertFalse(DnsCodec.isMatchingResponse(query, reply.copyOf().apply { this[2] = 0x89.toByte() }))
        val caseFolded = reply.copyOf().apply {
            "example".toByteArray(Charsets.US_ASCII).copyInto(this, 13)
        }
        assertTrue(DnsCodec.isMatchingResponse(query, caseFolded))
    }

    @Test fun rejectsTruncatedAndUnsolicitedDns() {
        assertFalse(DnsCodec.isMatchingResponse(dns(), byteArrayOf(1, 2)))
        assertFalse(DnsCodec.isMatchingResponse(dns(), dns("other.com").apply { this[2] = 0x81.toByte() }))
    }

    @Test fun rejectsResponseMasqueradingAsIncomingQuery() {
        val reply = dns().apply { this[2] = 0x81.toByte(); this[3] = 0x80.toByte() }
        assertTrue(DnsCodec.readFirstQuestion(reply) == null)
        assertTrue(DnsCodec.buildNxDomain(reply) == null)
    }

    @Test fun rejectsQueriesContainingAnswerRecords() {
        val query = dns().apply { this[7] = 1 }
        assertTrue(DnsCodec.readFirstQuestion(query) == null)
    }
}
