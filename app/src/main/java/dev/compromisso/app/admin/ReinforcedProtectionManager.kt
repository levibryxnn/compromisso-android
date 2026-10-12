package dev.compromisso.app.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

/** Only administers this application's own VPN and uninstall policy. */
class ReinforcedProtectionManager(context: Context) {
    private val appContext = context.applicationContext
    private val dpm = appContext.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(appContext, CompromissoDeviceAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(appContext.packageName)

    fun isCorePoliciesEnabled(): Boolean = runCatching {
        isDeviceOwner() &&
            dpm.getAlwaysOnVpnPackage(admin) == appContext.packageName &&
            dpm.isUninstallBlocked(admin, appContext.packageName) &&
            Build.VERSION.SDK_INT >= 29 && dpm.isAlwaysOnVpnLockdownEnabled(admin)
    }.getOrDefault(false)

    fun enableCorePolicies(): Result<Unit> = Result.failure(
        UnsupportedOperationException(
            "Lockdown bloqueado até existir VPN full-tunnel validada e recuperação segura",
        ),
    )

    fun disableCorePolicies(): Result<Unit> = runCatching {
        check(isDeviceOwner()) { "O app não está provisionado como Device Owner" }
        dpm.setUninstallBlocked(admin, appContext.packageName, false)
        dpm.setAlwaysOnVpnPackage(admin, null, false)
    }
}
