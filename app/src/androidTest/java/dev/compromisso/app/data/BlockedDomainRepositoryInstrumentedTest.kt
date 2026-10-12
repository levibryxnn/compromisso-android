package dev.compromisso.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.compromisso.app.domain.DomainPolicyEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockedDomainRepositoryInstrumentedTest {
    private lateinit var helper: BlockedDomainDatabaseHelper
    private lateinit var repository: SQLiteBlockedDomainRepository

    @Before
    fun setup() {
        helper = BlockedDomainDatabaseHelper(ApplicationProvider.getApplicationContext(), null)
        repository = SQLiteBlockedDomainRepository(helper)
    }

    @After
    fun tearDown() {
        helper.close()
    }

    @Test
    fun persistsAndTogglesNormalizedDomain() {
        val item = repository.add("https://WWW.Example.com/path")
        assertEquals("www.example.com", item.domain)
        assertTrue(repository.listEnabledDomains().contains("www.example.com"))
        assertTrue(DomainPolicyEngine.isBlocked("x.www.example.com", repository.listEnabledDomains()))

        repository.setEnabled(item.id, false)
        assertFalse(repository.listEnabledDomains().contains("www.example.com"))

        repository.delete(item.id)
        assertTrue(repository.listAll().isEmpty())
    }
}
