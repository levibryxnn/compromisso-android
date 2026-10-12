package dev.compromisso.app.policy

/** Process-local runtime truth; never infer healthy filtering from a saved preference. */
enum class ProtectionState { OFF, STARTING, OPERATIONAL, DEGRADED, ERROR }

object ProtectionRuntime {
    @Volatile var state: ProtectionState = ProtectionState.OFF
        private set
    @Volatile var appliedRulesRevision: Long = -1L
        private set

    /** A DEGRADED VPN still serves local blocks, but the upstream DNS may be unavailable. */
    val active: Boolean get() = state == ProtectionState.OPERATIONAL || state == ProtectionState.DEGRADED

    fun markStarting() { state = ProtectionState.STARTING; appliedRulesRevision = -1L }
    fun markOperational() { state = ProtectionState.OPERATIONAL }
    fun markDegraded() { state = ProtectionState.DEGRADED }
    fun markError() { state = ProtectionState.ERROR; appliedRulesRevision = -1L }
    fun markInactive() { state = ProtectionState.OFF; appliedRulesRevision = -1L }
    fun markRulesApplied(revision: Long) { appliedRulesRevision = revision }
}
