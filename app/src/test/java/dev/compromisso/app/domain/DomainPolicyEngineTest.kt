package dev.compromisso.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainPolicyEngineTest {
    @Test
    fun normalizesUrlsAndCase() {
        assertEquals("www.instagram.com", DomainPolicyEngine.normalize("HTTPS://WWW.INSTAGRAM.COM/path"))
        assertEquals("reddit.com", DomainPolicyEngine.normalize("reddit.com/"))
        assertEquals("m.tiktok.com", DomainPolicyEngine.normalize("  m.TikTok.com  "))
        assertEquals("xn--exmple-qta.com", DomainPolicyEngine.normalize("exámple.com"))
    }

    @Test
    fun blocksExactDomainAndSubdomains() {
        val blocked = setOf("instagram.com")
        assertTrue(DomainPolicyEngine.isBlocked("instagram.com", blocked))
        assertTrue(DomainPolicyEngine.isBlocked("www.instagram.com", blocked))
        assertTrue(DomainPolicyEngine.isBlocked("a.b.instagram.com", blocked))
        assertFalse(DomainPolicyEngine.isBlocked("notinstagram.com", blocked))
        assertFalse(DomainPolicyEngine.isBlocked("instagram.com.example.org", blocked))
    }

    @Test
    fun specificSubdomainDoesNotBlockApex() {
        val blocked = setOf("www.example.com")
        assertTrue(DomainPolicyEngine.isBlocked("www.example.com", blocked))
        assertTrue(DomainPolicyEngine.isBlocked("x.www.example.com", blocked))
        assertFalse(DomainPolicyEngine.isBlocked("example.com", blocked))
    }

    @Test
    fun rejectsInvalidHosts() {
        assertNull(DomainPolicyEngine.normalize(""))
        assertNull(DomainPolicyEngine.normalize("localhost"))
        assertNull(DomainPolicyEngine.normalize("https://exa mple.com"))
        assertNull(DomainPolicyEngine.normalize("example.com@other.com"))
        assertNull(DomainPolicyEngine.normalize("https://name:pass@example.com"))
        assertNull(DomainPolicyEngine.normalize("example.com?redirect=other.com"))
        assertNull(DomainPolicyEngine.normalize("exámple.com:badport"))
    }
}
