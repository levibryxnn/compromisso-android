package dev.compromisso.app.domain

import java.net.IDN
import java.net.URI
import java.util.Locale

object DomainPolicyEngine {
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val host = runCatching {
            val value = if (trimmed.contains("://")) trimmed else "https://$trimmed"
            val uri = URI(value)
            val authority = uri.rawAuthority
            if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https") ||
                authority.isNullOrBlank() || uri.rawUserInfo != null || '@' in authority ||
                uri.rawQuery != null || uri.rawFragment != null
            ) null else {
                // URI.host can be null for IDN Unicode input. Only fall back to the
                // authority when it has no port or credential delimiters.
                uri.host ?: authority.takeUnless { ':' in it || '[' in it || ']' in it }
            }
        }.getOrNull() ?: return null

        val cleaned = host.trim().trimEnd('.').lowercase(Locale.ROOT)
        if (cleaned.isEmpty()) return null

        val ascii = runCatching { IDN.toASCII(cleaned, IDN.USE_STD3_ASCII_RULES) }.getOrNull()
            ?: return null

        if (ascii.length > 253) return null
        val labels = ascii.split('.')
        if (labels.size < 2) return null
        if (labels.any { it.isEmpty() || it.length > 63 || it.startsWith('-') || it.endsWith('-') }) return null
        if (labels.any { label -> label.any { !(it.isLetterOrDigit() || it == '-') } }) return null

        return ascii
    }

    fun isBlocked(host: String, blockedDomains: Set<String>): Boolean {
        val normalizedHost = normalize(host) ?: return false
        return blockedDomains.any { blocked ->
            normalizedHost == blocked || normalizedHost.endsWith(".$blocked")
        }
    }
}
