package com.gigakin.stockbuddy.data.repo

import android.util.Log
import androidx.lifecycle.LiveData
import com.gigakin.stockbuddy.data.db.dao.InventorySessionDao
import com.gigakin.stockbuddy.data.db.dao.LinkedItemDao
import com.gigakin.stockbuddy.data.db.dao.SessionTagDao
import com.gigakin.stockbuddy.data.db.entity.InventorySessionEntity
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity
import com.gigakin.stockbuddy.data.db.entity.SessionTagEntity
import com.gigakin.stockbuddy.util.DemoLimits
import java.util.UUID

private const val TAG = "InventoryRepository"

/**
 * FR-29-47: Inventory session lifecycle, scanning, and Available/Missing/Excess computation.
 * Session demo cap uses rolling retention (oldest auto-purged), not a hard block — Section 4,
 * MVP Scope doc, since session count grows passively across repeated demos.
 * Sessions now use UUID ids and carry computed KPI snapshots.
 */
class InventoryRepository(
    private val sessionDao: InventorySessionDao,
    private val tagDao: SessionTagDao,
    private val linkedItemDao: LinkedItemDao
) {
    fun observeSessions(): LiveData<List<InventorySessionEntity>> = sessionDao.observeAll()

    /** FR-29: start a new session, enforcing rolling retention at MAX_SESSIONS (Section 4). */
    suspend fun startSession(code: String): String {
        val count = sessionDao.count()
        Log.d(TAG, "startSession: Current session count = $count, MAX_SESSIONS = ${DemoLimits.MAX_SESSIONS}")

        if (count >= DemoLimits.MAX_SESSIONS) {
            val oldest = sessionDao.getAll().firstOrNull()
            if (oldest != null) {
                Log.d(TAG, "Rolling purge: Deleting oldest session (id=${oldest.id}, code=${oldest.code})")
                sessionDao.delete(oldest)
            }
        }

        val newSessionId = UUID.randomUUID().toString()
        val session = InventorySessionEntity(id = newSessionId, code = code)
        sessionDao.insert(session)
        Log.d(TAG, "Created new session (id=$newSessionId, code=$code)")
        return newSessionId
    }

    /** Get all sessions for debugging. */
    suspend fun getAllSessions(): List<InventorySessionEntity> {
        val sessions = sessionDao.getAll()
        Log.d(TAG, "getAllSessions: Found ${sessions.size} sessions")
        sessions.forEach { Log.d(TAG, "  - Session(id=${it.id}, code=${it.code}, startedAt=${it.startedAt})") }
        return sessions
    }

    /** FR-37: real-time dedup via DAO's OnConflictStrategy.IGNORE. */
    suspend fun recordScan(sessionId: String, rfidTagId: String) {
        tagDao.insert(SessionTagEntity(sessionId = sessionId, rfidTagId = rfidTagId))
    }

    suspend fun distinctScanCount(sessionId: String): Int = tagDao.countDistinctForSession(sessionId)

    /** FR-36: stop the session. */
    suspend fun stopSession(sessionId: String) {
        sessionDao.getById(sessionId)?.let {
            sessionDao.update(it.copy(stoppedAt = System.currentTimeMillis()))
        }
    }

    data class ResultItem(val item: LinkedItemEntity, val status: Status)
    enum class Status { AVAILABLE, MISSING, EXCESS }

    /**
     * FR-40/41/45/46: computes Available/Missing/Excess against the local master, with an
     * optional category filter applied at display time (re-appliable without re-scanning —
     * the raw session_tags set is never discarded, FR-46).
     */
    suspend fun computeResults(sessionId: String, categoryFilter: String?): List<ResultItem> {
        val scanned = tagDao.getForSession(sessionId).map { it.rfidTagId }.toSet()
        val master = linkedItemDao.getAll().filter { categoryFilter == null || it.category == categoryFilter }
        val masterByRfid = master.associateBy { it.rfidTagId }

        val results = mutableListOf<ResultItem>()
        master.forEach { item ->
            results += ResultItem(item, if (item.rfidTagId in scanned) Status.AVAILABLE else Status.MISSING)
        }
        // Excess: scanned tags with no matching master record (within the filtered category
        // scope, master items only — excess by definition isn't in any master record).
        scanned.filter { it !in masterByRfid }.forEach { excessRfid ->
            // Excess items have no item record; represented with a synthetic placeholder name.
            results += ResultItem(
                LinkedItemEntity(
                    rfidTagId = excessRfid,
                    productName = "(Unrecognized tag)",
                    barcode = "",
                    category = ""
                ),
                Status.EXCESS
            )
        }
        return results
    }

    data class SessionStatsData(
        val available: Int = 0,
        val missing: Int = 0,
        val excess: Int = 0
    )

    /** Real-time session statistics during scanning (no category filter). */
    suspend fun computeSessionStats(sessionId: String): SessionStatsData {
        val scanned = tagDao.getForSession(sessionId).map { it.rfidTagId }.toSet()
        val master = linkedItemDao.getAll()
        val masterByRfid = master.associateBy { it.rfidTagId }

        val available = master.count { it.rfidTagId in scanned }
        val missing = master.count { it.rfidTagId !in scanned }
        val excess = scanned.count { it !in masterByRfid }

        return SessionStatsData(available, missing, excess)
    }
}
