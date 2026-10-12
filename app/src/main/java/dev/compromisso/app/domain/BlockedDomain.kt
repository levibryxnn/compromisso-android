package dev.compromisso.app.domain

data class BlockedDomain(
    val id: Long,
    val domain: String,
    val enabled: Boolean,
    val category: String?,
    val createdAt: Long,
)
