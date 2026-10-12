package dev.compromisso.app.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionRuntimeTest {
    @Test fun startsGrayBecomesGreenOnlyAfterOperationalSignal() {
        ProtectionRuntime.markInactive()
        ProtectionRuntime.markStarting()
        assertEquals(ProtectionState.STARTING, ProtectionRuntime.state)
        assertFalse(ProtectionRuntime.active)
        ProtectionRuntime.markDegraded()
        assertTrue(ProtectionRuntime.active) // TUN running but no healthy resolver
        assertEquals(ProtectionState.DEGRADED, ProtectionRuntime.state)
        ProtectionRuntime.markOperational()
        assertEquals(ProtectionState.OPERATIONAL, ProtectionRuntime.state)
        ProtectionRuntime.markRulesApplied(6)
        assertEquals(6L, ProtectionRuntime.appliedRulesRevision)
        ProtectionRuntime.markInactive()
        assertFalse(ProtectionRuntime.active)
        assertEquals(-1L, ProtectionRuntime.appliedRulesRevision)
    }
}
