package dev.compromisso.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import dev.compromisso.app.policy.ProtectionController
import dev.compromisso.app.security.SecurityConfigStore
import dev.compromisso.app.daily.DailyVerseScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_TIME_CHANGED
        ) return
        // Word of the Day works independently of whether the VPN can start at boot.
        runCatching { DailyVerseScheduler(context.applicationContext).update() }
        if (action != Intent.ACTION_BOOT_COMPLETED) return
        val config = SecurityConfigStore(context.applicationContext)
        if (!config.protectionRequested) return
        if (VpnService.prepare(context) != null) return
        // Never crash the boot receiver if the OS disallows a foreground start.
        runCatching { ProtectionController(context.applicationContext, config).startProtection() }
    }
}
