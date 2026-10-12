package dev.compromisso.app.security

import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class TrustedPinManager(
    private val store: SecurityConfigStore,
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val elapsedMs: () -> Long = SystemClock::elapsedRealtime,
) {
    fun isValidPinFormat(pin: String): Boolean = pin.length in 6..12 && pin.all { it in '0'..'9' }

    fun configure(pin: String) {
        require(isValidPinFormat(pin)) { "O PIN deve conter de 6 a 12 números" }
        check(!store.pinConfigured) { "O PIN administrativo já está configurado" }

        val salt = ByteArray(SALT_SIZE_BYTES).also(SecureRandom()::nextBytes)
        val derived = derive(pin, salt)
        try {
            val digest = signWithKeystore(derived, createIfMissing = true)
                ?: error("Não foi possível criar a chave protegida do dispositivo")

            store.saveCredential(
                saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
                digestBase64 = Base64.encodeToString(digest, Base64.NO_WRAP),
            )
        } finally {
            derived.fill(0)
        }
    }

    fun verify(pin: String): PinVerification {
        if (!store.pinConfigured) return PinVerification.NotConfigured

        val now = nowMs()
        val elapsed = elapsedMs()
        val remaining = PinLockoutClock.remainingMs(
            wallNowMs = now,
            elapsedNowMs = elapsed,
            wallUntilMs = store.lockUntilEpochMs,
            elapsedUntilMs = store.lockUntilElapsedMs,
            lastObservedElapsedMs = store.lastObservedElapsedMs,
        )
        if (remaining > 0L) return PinVerification.Locked(remaining)

        // Reject pathological untrusted inputs before expensive PBKDF2 computation.
        if (!isValidPinFormat(pin)) return recordInvalid(now, elapsed)

        val saltEncoded = store.saltBase64 ?: return PinVerification.CredentialUnavailable
        val digestEncoded = store.digestBase64 ?: return PinVerification.CredentialUnavailable
        val salt = runCatching { Base64.decode(saltEncoded, Base64.NO_WRAP) }.getOrNull()
            ?: return PinVerification.CredentialUnavailable
        val expected = runCatching { Base64.decode(digestEncoded, Base64.NO_WRAP) }.getOrNull()
            ?: return PinVerification.CredentialUnavailable

        val derived = derive(pin, salt)
        val candidate = try {
            signWithKeystore(derived, createIfMissing = false)
                ?: return PinVerification.CredentialUnavailable
        } finally {
            derived.fill(0)
        }

        return if (MessageDigest.isEqual(expected, candidate)) {
            store.resetFailedAttempts()
            PinVerification.Success
        } else recordInvalid(now, elapsed)
    }

    private fun recordInvalid(now: Long, elapsed: Long): PinVerification {
        val attempts = store.failedAttempts.coerceAtMost(Int.MAX_VALUE - 1) + 1
        val delay = RateLimitPolicy.delayMsForFailedAttempts(attempts)
        store.recordFailedAttempt(
            attempts = attempts,
            wallUntilMs = if (delay > 0L) now + delay else 0L,
            elapsedUntilMs = if (delay > 0L) elapsed + delay else 0L,
            elapsedNowMs = elapsed,
        )
        return PinVerification.Invalid
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val chars = pin.toCharArray()
        val spec = PBEKeySpec(chars, salt, PBKDF2_ITERATIONS, DERIVED_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun signWithKeystore(data: ByteArray, createIfMissing: Boolean): ByteArray? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        var key = keyStore.getKey(keyAlias, null) as? SecretKey
        if (key == null && createIfMissing) {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                )
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build()
            )
            key = generator.generateKey()
        }
        key ?: return null

        return Mac.getInstance("HmacSHA256").run {
            init(key)
            doFinal(data)
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DEFAULT_KEY_ALIAS = "compromisso.trusted_pin.hmac.v1"
        private const val SALT_SIZE_BYTES = 16
        private const val PBKDF2_ITERATIONS = 250_000
        private const val DERIVED_KEY_BITS = 256
    }
}
