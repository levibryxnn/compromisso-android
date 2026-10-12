package dev.compromisso.app.data

import dev.compromisso.app.domain.BlockedDomain

interface BlockedDomainRepository {
    fun listAll(): List<BlockedDomain>
    fun listEnabledDomains(): Set<String>
    fun add(domain: String, category: String? = null): BlockedDomain
    fun setEnabled(id: Long, enabled: Boolean)
    fun delete(id: Long)
    fun countEnabled(): Int
}
