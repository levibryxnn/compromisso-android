package dev.compromisso.app.daily

import java.time.Instant
import java.time.ZonedDateTime

/** Computes the next wall-clock morning in the user's current timezone.
 * No exact alarm permission is required: actual delivery may be delayed by Doze.
 */
object DailyVerseSchedulePolicy {
    fun nextAfter(now: ZonedDateTime, hour: Int, minute: Int): Instant {
        require(hour in 0..23 && minute in 0..59) { "Horário inválido" }
        val time = java.time.LocalTime.of(hour, minute)
        val today = now.toLocalDate().atTime(time).atZone(now.zone)
        return (if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)).toInstant()
    }
}
