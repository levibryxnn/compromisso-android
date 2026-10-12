package dev.compromisso.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BlockedDomainDatabaseHelper(
    context: Context,
    databaseName: String? = DATABASE_NAME,
) : SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {
    val crypto = DomainCrypto(context.applicationContext)

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.execSQL("PRAGMA secure_delete=ON")
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE blocked_domains (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                domain TEXT NOT NULL UNIQUE,
                domain_fingerprint TEXT NOT NULL UNIQUE,
                enabled INTEGER NOT NULL DEFAULT 1 CHECK(enabled IN (0,1)),
                category TEXT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_blocked_domains_enabled ON blocked_domains(enabled)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion != 1 || newVersion != 2) error("No migration path from $oldVersion to $newVersion")
        // SQLiteOpenHelper wraps onUpgrade in a transaction. If the Android Keystore is
        // unavailable, the migration fails instead of accepting insecure plaintext.
        db.execSQL("ALTER TABLE blocked_domains ADD COLUMN domain_fingerprint TEXT")
        val rows = ArrayList<Triple<Long, String, String?>>()
        db.query("blocked_domains", arrayOf("id", "domain", "category"), null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) rows += Triple(cursor.getLong(0), cursor.getString(1), if (cursor.isNull(2)) null else cursor.getString(2))
        }
        for ((id, plainDomain, category) in rows) {
            val update = ContentValues().apply {
                put("domain", crypto.encrypt(plainDomain))
                put("domain_fingerprint", crypto.fingerprint(plainDomain))
                put("category", category?.let(crypto::encrypt))
            }
            check(db.update("blocked_domains", update, "id=?", arrayOf(id.toString())) == 1)
        }
        db.execSQL("CREATE UNIQUE INDEX idx_blocked_domains_fingerprint ON blocked_domains(domain_fingerprint)")
    }

    companion object {
        private const val DATABASE_NAME = "compromisso.db"
        private const val DATABASE_VERSION = 2
    }
}
