package dev.compromisso.app.ui

import dev.compromisso.app.domain.BlockedDomain
import dev.compromisso.app.domain.ProtectionMode
import dev.compromisso.app.policy.ProtectionState

data class HomeUiState(
    val domains: List<BlockedDomain> = emptyList(),
    val protectionActive: Boolean = false,
    val protectionState: ProtectionState = ProtectionState.OFF,
    val rulesPending: Boolean = false,
    val protectionRequested: Boolean = false,
    val pinConfigured: Boolean = false,
    val protectionMode: ProtectionMode = ProtectionMode.NORMAL,
    val reinforcedAvailable: Boolean = false,
    val reinforcedActive: Boolean = false,
    val storageAvailable: Boolean = false,
    val dailyVerseEnabled: Boolean = false,
    val dailyVerseNotificationsAllowed: Boolean = true,
    val dailyVerseHour: Int = 8,
    val dailyVerseMinute: Int = 0,
    val message: String? = null,
) {
    val enabledDomainCount: Int get() = domains.count { it.enabled }
    val isStarting: Boolean get() = protectionState == ProtectionState.STARTING ||
        (protectionRequested && protectionState == ProtectionState.OFF)
    val isDegraded: Boolean get() = protectionState == ProtectionState.DEGRADED
}
