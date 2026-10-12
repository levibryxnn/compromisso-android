package dev.compromisso.app.daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneId

class DailyVerseSchedulePolicyTest {
    private val zone = ZoneId.of("America/Sao_Paulo")

    @Test fun schedulesTodayIfBeforeTime() {
        val now = ZonedDateTime.of(2026, 10, 8, 7, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 10, 8, 8, 30, 0, 0, zone).toInstant(),
            DailyVerseSchedulePolicy.nextAfter(now, 8, 30))
    }

    @Test fun schedulesTomorrowWhenSameOrAfterTime() {
        val now = ZonedDateTime.of(2026, 10, 8, 8, 30, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 10, 9, 8, 30, 0, 0, zone).toInstant(),
            DailyVerseSchedulePolicy.nextAfter(now, 8, 30))
    }

    @Test fun rejectsInvalidTime() {
        var rejected = false
        try {
            DailyVerseSchedulePolicy.nextAfter(ZonedDateTime.now(zone), 24, 0)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test fun choosesSameVerseAllDayWithoutTracking() {
        val day = LocalDate.of(2026, 10, 8)
        assertEquals(DailyVerseCatalog.forDate(day), DailyVerseCatalog.forDate(day))
        assertTrue(DailyVerseCatalog.forDate(day).reference.isNotBlank())
    }
}
