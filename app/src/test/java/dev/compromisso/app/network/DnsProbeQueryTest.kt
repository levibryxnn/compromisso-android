package dev.compromisso.app.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsProbeQueryTest {
    @Test fun probeRequiresMatchingHealthyReply() {
        val request = DnsProbeQuery.request()
        assertTrue(DnsCodec.readFirstQuestion(request)?.name == "example.com")
        val answer = request.copyOf().apply { this[2] = 0x81.toByte(); this[3] = 0x80.toByte() }
        assertTrue(DnsProbeQuery.isHealthy(answer))
        assertFalse(DnsProbeQuery.isHealthy(null))
        assertFalse(DnsProbeQuery.isHealthy(request))
        assertFalse(DnsProbeQuery.isHealthy(answer.copyOf().apply { this[1] = 0x51 }))
        assertFalse(DnsProbeQuery.isHealthy(answer.copyOf().apply { this[3] = 0x82.toByte() }))
    }
}
