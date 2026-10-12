package dev.compromisso.app.security

import org.junit.Assert.assertEquals
import org.junit.Test

class PinLockoutClockTest {
    @Test fun advancingClockCannotBypassLiveBootLockout() {
        assertEquals(40_000L, PinLockoutClock.remainingMs(
            wallNowMs = 999_999_999L, elapsedNowMs = 20_000L,
            wallUntilMs = 120_000L, elapsedUntilMs = 60_000L,
            lastObservedElapsedMs = 10_000L,
        ))
    }

    @Test fun rollbackCannotBypassStoredWallDeadline() {
        assertEquals(90_000L, PinLockoutClock.remainingMs(
            wallNowMs = 10_000L, elapsedNowMs = 20_000L,
            wallUntilMs = 100_000L, elapsedUntilMs = 60_000L,
            lastObservedElapsedMs = 10_000L,
        ))
    }

    @Test fun elapsedResetFallsBackToWallDeadline() {
        assertEquals(10_000L, PinLockoutClock.remainingMs(
            wallNowMs = 90_000L, elapsedNowMs = 1_000L,
            wallUntilMs = 100_000L, elapsedUntilMs = 60_000L,
            lastObservedElapsedMs = 10_000L,
        ))
    }
}
