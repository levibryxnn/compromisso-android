package dev.compromisso.app.security

/**
 * Uses both wall and monotonic clocks. Wall time persists over reboot; monotonic
 * time prevents simple same-boot clock-forward bypass. Not tamper-resistant on root.
 */
object PinLockoutClock {
    fun remainingMs(
        wallNowMs: Long,
        elapsedNowMs: Long,
        wallUntilMs: Long,
        elapsedUntilMs: Long,
        lastObservedElapsedMs: Long,
    ): Long {
        val wallRemaining = (wallUntilMs - wallNowMs).coerceAtLeast(0L)
        val monotonicRemaining = if (
            lastObservedElapsedMs > 0L && elapsedNowMs >= lastObservedElapsedMs
        ) (elapsedUntilMs - elapsedNowMs).coerceAtLeast(0L) else 0L
        return maxOf(wallRemaining, monotonicRemaining)
    }
}
