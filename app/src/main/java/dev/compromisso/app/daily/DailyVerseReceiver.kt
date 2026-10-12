package dev.compromisso.app.daily

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.compromisso.app.MainActivity
import dev.compromisso.app.R
import dev.compromisso.app.domain.ProtectionMode
import dev.compromisso.app.policy.ProtectionRuntime
import dev.compromisso.app.policy.ProtectionState
import dev.compromisso.app.security.SecurityConfigStore
import java.time.LocalDate

/** Only presents local, predetermined Bible text. Never reads other apps or browsing data. */
class DailyVerseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != DailyVerseScheduler.ACTION_SHOW_VERSE) return
        // Always schedule the next occurrence, including when commitment is currently disabled.
        runCatching { DailyVerseScheduler(context).update() }
        if (!DailyVersePreferences(context).enabled) return
        if (SecurityConfigStore(context).protectionMode != ProtectionMode.COMMITMENT) return
        // A saved 'requested' flag alone must not be treated as an active VPN.
        if (ProtectionRuntime.state != ProtectionState.OPERATIONAL) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val verse = DailyVerseCatalog.forDate(LocalDate.now())
        val notifications = context.getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, "Palavra do Dia", NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Versículo diário, somente no Modo Compromisso" })

        val launch = PendingIntent.getActivity(
            context, 202,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_compromisso)
            .setContentTitle("Palavra do Dia · ${verse.reference}")
            .setContentText(verse.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(verse.text))
            .setContentIntent(launch)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_compromisso)
                .setContentTitle("COMPROMISSO")
                .setContentText("Palavra do Dia disponível")
                .build())
            .build()
        notifications.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "compromisso_daily_verse"
        private const val NOTIFICATION_ID = 2201
    }
}
