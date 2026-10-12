package dev.compromisso.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedDomainInstrumentedTest {
    @Test fun encryptedDomainIsNotStoredAsPlaintext() {
        val helper = BlockedDomainDatabaseHelper(ApplicationProvider.getApplicationContext(), null)
        try {
            val repo = SQLiteBlockedDomainRepository(helper)
            repo.add("example.com", "sensitive-category")
            helper.readableDatabase.rawQuery("SELECT domain, domain_fingerprint, category FROM blocked_domains", null).use { cur ->
                assertTrue(cur.moveToFirst())
                assertTrue(cur.getString(0).startsWith("enc:v1:"))
                assertFalse(cur.getString(0).contains("example.com"))
                assertFalse(cur.getString(1).contains("example.com"))
                assertTrue(cur.getString(2).startsWith("enc:v1:"))
                assertFalse(cur.getString(2).contains("sensitive-category"))
            }
            assertEquals(setOf("example.com"), repo.listEnabledDomains())
            assertEquals("sensitive-category", repo.listAll().single().category)
            val duplicate = runCatching { repo.add("example.com", "sensitive-category") }
            assertTrue(duplicate.isFailure)
        } finally { helper.close() }
    }

    @Test fun upgradesPlaintextV1WithoutLosingRules() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-security-test.db"
        context.deleteDatabase(name)
        try {
            SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
                db.execSQL("CREATE TABLE blocked_domains (id INTEGER PRIMARY KEY AUTOINCREMENT, domain TEXT NOT NULL UNIQUE, enabled INTEGER NOT NULL DEFAULT 1, category TEXT NULL, created_at INTEGER NOT NULL)")
                db.execSQL("INSERT INTO blocked_domains(domain,enabled,category,created_at) VALUES ('old-example.com',1,'private-category',0)")
                db.version = 1
            }
            val helper = BlockedDomainDatabaseHelper(context, name)
            try {
                assertEquals(setOf("old-example.com"), SQLiteBlockedDomainRepository(helper).listEnabledDomains())
                assertEquals("private-category", SQLiteBlockedDomainRepository(helper).listAll().single().category)
                helper.readableDatabase.rawQuery("SELECT domain FROM blocked_domains", null).use { cur ->
                    assertTrue(cur.moveToFirst())
                    assertTrue(cur.getString(0).startsWith("enc:v1:"))
                }
            } finally { helper.close() }
        } finally { context.deleteDatabase(name) }
    }
}
