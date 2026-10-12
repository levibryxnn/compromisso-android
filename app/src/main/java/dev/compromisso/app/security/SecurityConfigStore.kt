package dev.compromisso.app.security

import android.content.Context
import dev.compromisso.app.domain.ProtectionMode

class SecurityConfigStore(
    context: Context,
    prefsName: String = PREFS_NAME,
) {
    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    var pinConfigured: Boolean
        get() = prefs.getBoolean(KEY_PIN_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_CONFIGURED, value).apply()

    var saltBase64: String?
        get() = prefs.getString(KEY_PIN_SALT, null)
        set(value) = prefs.edit().putString(KEY_PIN_SALT, value).apply()

    var digestBase64: String?
        get() = prefs.getString(KEY_PIN_DIGEST, null)
        set(value) = prefs.edit().putString(KEY_PIN_DIGEST, value).apply()

    var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        set(value) = check(prefs.edit().putInt(KEY_FAILED_ATTEMPTS, value).commit())

    var lockUntilEpochMs: Long
        get() = prefs.getLong(KEY_LOCK_UNTIL, 0L)
        set(value) = check(prefs.edit().putLong(KEY_LOCK_UNTIL, value).commit())

    val lockUntilElapsedMs: Long
        get() = prefs.getLong(KEY_LOCK_UNTIL_ELAPSED, 0L)

    val lastObservedElapsedMs: Long
        get() = prefs.getLong(KEY_LAST_ELAPSED, 0L)

    /** Monotonic revision of the desired rules; survives process death. */
    val rulesRevision: Long get() = prefs.getLong(KEY_RULES_REVISION, 0L)

    fun bumpRulesRevision(): Long {
        val next = rulesRevision + 1
        check(prefs.edit().putLong(KEY_RULES_REVISION, next).commit()) {
            "Não foi possível registrar a revisão das regras"
        }
        return next
    }

    var protectionRequested: Boolean
        get() = prefs.getBoolean(KEY_PROTECTION_REQUESTED, false)
        set(value) = check(prefs.edit().putBoolean(KEY_PROTECTION_REQUESTED, value).commit()) {
            "Não foi possível persistir o estado solicitado da proteção"
        }

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    var protectionMode: ProtectionMode
        get() = runCatching {
            val default = if (pinConfigured) ProtectionMode.COMMITMENT else ProtectionMode.NORMAL
            ProtectionMode.valueOf(prefs.getString(KEY_PROTECTION_MODE, default.name)!!)
        }.getOrDefault(if (pinConfigured) ProtectionMode.COMMITMENT else ProtectionMode.NORMAL)
        set(value) = check(prefs.edit().putString(KEY_PROTECTION_MODE, value.name).commit())

    fun saveCredential(saltBase64: String, digestBase64: String) {
        prefs.edit()
            .putString(KEY_PIN_SALT, saltBase64)
            .putString(KEY_PIN_DIGEST, digestBase64)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .putLong(KEY_LOCK_UNTIL_ELAPSED, 0L)
            .putLong(KEY_LAST_ELAPSED, 0L)
            .putBoolean(KEY_PIN_CONFIGURED, true)
            // Atomic: a crash between PIN creation and UI navigation must never leave NORMAL mode.
            .putString(KEY_PROTECTION_MODE, ProtectionMode.COMMITMENT.name)
            .let { check(it.commit()) { "Failed to persist credential metadata" } }
    }

    fun recordFailedAttempt(attempts: Int, wallUntilMs: Long, elapsedUntilMs: Long, elapsedNowMs: Long) {
        check(prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, attempts)
            .putLong(KEY_LOCK_UNTIL, wallUntilMs)
            .putLong(KEY_LOCK_UNTIL_ELAPSED, elapsedUntilMs)
            .putLong(KEY_LAST_ELAPSED, elapsedNowMs)
            .commit()) { "Could not persist PIN rate limit" }
    }

    fun resetFailedAttempts() {
        recordFailedAttempt(0, 0L, 0L, 0L)
    }

    fun clearCredentialMetadata() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_DIGEST)
            .putBoolean(KEY_PIN_CONFIGURED, false)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .putLong(KEY_LOCK_UNTIL_ELAPSED, 0L)
            .putLong(KEY_LAST_ELAPSED, 0L)
            .let { check(it.commit()) { "Failed to clear credential metadata" } }
    }

    companion object {
        const val PREFS_NAME = "security_config"
        private const val KEY_RULES_REVISION = "rules_revision"
        private const val KEY_PIN_CONFIGURED = "pin_configured"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_DIGEST = "pin_digest"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCK_UNTIL = "lock_until"
        private const val KEY_LOCK_UNTIL_ELAPSED = "lock_until_elapsed"
        private const val KEY_LAST_ELAPSED = "last_attempt_elapsed"
        private const val KEY_PROTECTION_REQUESTED = "protection_requested"
        private const val KEY_PROTECTION_MODE = "protection_mode"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
