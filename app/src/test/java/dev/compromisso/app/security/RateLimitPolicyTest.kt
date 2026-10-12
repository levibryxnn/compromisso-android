package dev.compromisso.app.security

import org.junit.Assert.assertEquals
import org.junit.Test

class RateLimitPolicyTest {
    @Test
    fun appliesProgressiveDelay() {
        assertEquals(0L, RateLimitPolicy.delayMsForFailedAttempts(4))
        assertEquals(30_000L, RateLimitPolicy.delayMsForFailedAttempts(5))
        assertEquals(60_000L, RateLimitPolicy.delayMsForFailedAttempts(6))
        assertEquals(120_000L, RateLimitPolicy.delayMsForFailedAttempts(7))
        assertEquals(300_000L, RateLimitPolicy.delayMsForFailedAttempts(8))
        assertEquals(600_000L, RateLimitPolicy.delayMsForFailedAttempts(9))
        assertEquals(1_800_000L, RateLimitPolicy.delayMsForFailedAttempts(20))
    }
}
