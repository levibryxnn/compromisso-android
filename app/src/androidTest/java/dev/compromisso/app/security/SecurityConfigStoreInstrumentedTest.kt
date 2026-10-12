package dev.compromisso.app.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.compromisso.app.domain.ProtectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityConfigStoreInstrumentedTest {
    @Test fun desiredRulesRevisionPersistsAfterReload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = "rules-revision-instrumented-test"
        context.deleteSharedPreferences(prefs)
        try {
            val config = SecurityConfigStore(context, prefs)
            assertEquals(0L, config.rulesRevision)
            assertEquals(1L, config.bumpRulesRevision())
            assertEquals(2L, config.bumpRulesRevision())
            assertEquals(2L, SecurityConfigStore(context, prefs).rulesRevision)
        } finally { context.deleteSharedPreferences(prefs) }
    }

    @Test fun installingFirstPinAtomicallyEnablesCommitmentMode() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = "credential-atomicity-instrumented-test"
        context.deleteSharedPreferences(prefs)
        try {
            val config = SecurityConfigStore(context, prefs)
            assertEquals(ProtectionMode.NORMAL, config.protectionMode)
            config.saveCredential("test-salt", "test-digest")
            assertTrue(config.pinConfigured)
            assertEquals(ProtectionMode.COMMITMENT, SecurityConfigStore(context, prefs).protectionMode)
        } finally {
            context.deleteSharedPreferences(prefs)
        }
    }
}
