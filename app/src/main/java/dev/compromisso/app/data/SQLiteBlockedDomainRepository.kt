package dev.compromisso.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import dev.compromisso.app.domain.BlockedDomain
import dev.compromisso.app.domain.DomainPolicyEngine

class SQLiteBlockedDomainRepository(
    private val helper: BlockedDomainDatabaseHelper,
) : BlockedDomainRepository {

    @Synchronized
    override fun listAll(): List<BlockedDomain> {
        val db = helper.readableDatabase
        db.query(
            "blocked_domains",
            arrayOf("id", "domain", "enabled", "category", "created_at"),
            null,
            null,
            null,
            null,
            "created_at DESC",
        ).use { cursor ->
            val result = ArrayList<BlockedDomain>(cursor.count)
            while (cursor.moveToNext()) {
                result += BlockedDomain(
                    id = cursor.getLong(0),
                    domain = helper.crypto.decrypt(cursor.getString(1)),
                    enabled = cursor.getInt(2) == 1,
                    category = if (cursor.isNull(3)) null else helper.crypto.decrypt(cursor.getString(3)),
                    createdAt = cursor.getLong(4),
                )
            }
            return result
        }
    }

    @Synchronized
    override fun listEnabledDomains(): Set<String> {
        val db = helper.readableDatabase
        db.query(
            "blocked_domains",
            arrayOf("domain"),
            "enabled = 1",
            null,
            null,
            null,
            null,
        ).use { cursor ->
            val result = LinkedHashSet<String>(cursor.count)
            while (cursor.moveToNext()) result += helper.crypto.decrypt(cursor.getString(0))
            return result
        }
    }

    @Synchronized
    override fun add(domain: String, category: String?): BlockedDomain {
        val normalized = DomainPolicyEngine.normalize(domain)
            ?: throw IllegalArgumentException("Domínio inválido")
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("domain", helper.crypto.encrypt(normalized))
            put("domain_fingerprint", helper.crypto.fingerprint(normalized))
            put("enabled", 1)
            put("category", category?.let(helper.crypto::encrypt))
            put("created_at", now)
        }
        val id = try {
            helper.writableDatabase.insertOrThrow("blocked_domains", null, values)
        } catch (error: SQLiteConstraintException) {
            throw IllegalArgumentException("Esse domínio já está na lista", error)
        }
        return BlockedDomain(id, normalized, true, category, now)
    }

    @Synchronized
    override fun setEnabled(id: Long, enabled: Boolean) {
        val values = ContentValues().apply { put("enabled", if (enabled) 1 else 0) }
        helper.writableDatabase.update("blocked_domains", values, "id = ?", arrayOf(id.toString()))
    }

    @Synchronized
    override fun delete(id: Long) {
        helper.writableDatabase.delete("blocked_domains", "id = ?", arrayOf(id.toString()))
    }

    @Synchronized
    override fun countEnabled(): Int {
        helper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM blocked_domains WHERE enabled = 1",
            null,
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }
}
