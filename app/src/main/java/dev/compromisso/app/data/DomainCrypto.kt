package dev.compromisso.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts blocklisted domains with AES-256-GCM and creates a separate keyed,
 * deterministic fingerprint for SQLite uniqueness. Keys never leave Android Keystore.
 * All plaintext exists only inside this process while needed by the filtering engine.
 */
class DomainCrypto(@Suppress("UNUSED_PARAMETER") context: Context) {
    fun encrypt(domain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(AES_ALIAS, KeyProperties.KEY_ALGORITHM_AES))
        cipher.updateAAD(DOMAIN_AAD)
        val ciphertext = cipher.doFinal(domain.toByteArray(Charsets.UTF_8))
        return PREFIX + b64(cipher.iv + ciphertext)
    }

    fun decrypt(encoded: String): String {
        require(encoded.startsWith(PREFIX)) { "Database domain is not encrypted" }
        val bytes = Base64.decode(encoded.removePrefix(PREFIX), Base64.NO_WRAP or Base64.URL_SAFE)
        require(bytes.size >= NONCE_SIZE + GCM_TAG_BYTES) { "Truncated domain ciphertext" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            key(AES_ALIAS, KeyProperties.KEY_ALGORITHM_AES),
            GCMParameterSpec(128, bytes, 0, NONCE_SIZE),
        )
        cipher.updateAAD(DOMAIN_AAD)
        return cipher.doFinal(bytes, NONCE_SIZE, bytes.size - NONCE_SIZE).toString(Charsets.UTF_8)
    }

    fun fingerprint(domain: String): String = b64(
        Mac.getInstance("HmacSHA256").run {
            init(key(HMAC_ALIAS, KeyProperties.KEY_ALGORITHM_HMAC_SHA256))
            doFinal(domain.toByteArray(Charsets.UTF_8))
        }
    )

    private fun key(alias: String, algorithm: String): SecretKey = synchronized(KEYSTORE_LOCK) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return@synchronized it }
        val generator = KeyGenerator.getInstance(algorithm, "AndroidKeyStore")
        val spec = if (algorithm == KeyProperties.KEY_ALGORITHM_AES) {
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        } else {
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        }
        generator.init(spec)
        generator.generateKey()
    }

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(
        bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING,
    )

    companion object {
        private val KEYSTORE_LOCK = Any()
        private const val AES_ALIAS = "compromisso.blocklist.aes.v1"
        private const val HMAC_ALIAS = "compromisso.blocklist.fingerprint.v1"
        private const val PREFIX = "enc:v1:"
        private const val NONCE_SIZE = 12
        private const val GCM_TAG_BYTES = 16
        private val DOMAIN_AAD = "compromisso:domain:v1".toByteArray(Charsets.UTF_8)
    }
}
