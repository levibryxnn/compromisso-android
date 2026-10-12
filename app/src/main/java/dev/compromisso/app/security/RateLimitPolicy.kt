package dev.compromisso.app.security

object RateLimitPolicy {
    fun delayMsForFailedAttempts(failedAttempts: Int): Long = when {
        failedAttempts < 5 -> 0L
        failedAttempts == 5 -> 30_000L
        failedAttempts == 6 -> 60_000L
        failedAttempts == 7 -> 120_000L
        failedAttempts == 8 -> 300_000L
        failedAttempts == 9 -> 600_000L
        else -> 1_800_000L
    }
}
