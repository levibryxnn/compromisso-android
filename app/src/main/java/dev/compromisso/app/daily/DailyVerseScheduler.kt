package dev.compromisso.app.daily

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.ZonedDateTime

/** One inexact alarm per day, no polling, no background network and no exact-alarm permission. */
class DailyVerseScheduler(private val context: Context) {
    private val manager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun update() {
        cancel()
        val prefs = DailyVersePreferences(context)
        if (!prefs.enabled) return
        val next = DailyVerseSchedulePolicy.nextAfter(ZonedDateTime.now(), prefs.hour, prefs.minute)
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), pendingIntent())
    }

    fun cancel() {
        manager.cancel(pendingIntent())
    }

    private fun pendingIntent() = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, DailyVerseReceiver::class.java).setAction(ACTION_SHOW_VERSE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val REQUEST_CODE = 201
        const val ACTION_SHOW_VERSE = "dev.compromisso.app.action.DAILY_VERSE"
    }
}
