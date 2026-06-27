package com.gigakin.stockbuddy.data.repo

import android.util.Log
import androidx.lifecycle.LiveData
import com.gigakin.stockbuddy.data.db.dao.InventorySessionDao
import com.gigakin.stockbuddy.data.db.dao.ItemDao
import com.gigakin.stockbuddy.data.db.dao.ScannedTagDao
import com.gigakin.stockbuddy.data.db.entity.InventorySessionEntity
import com.gigakin.stockbuddy.data.db.entity.ItemEntity
import com.gigakin.stockbuddy.data.db.entity.ScannedTagEntity
import com.gigakin.stockbuddy.util.DemoLimits

private const val TAG = "InventoryRepository"

/**
 * FR-29-47: Inventory session lifecycle, scanning, and Available/Missing/Excess computation.
 * Session demo cap uses rolling retention (oldest auto-purged), not a hard block — Section 4,
 * MVP Scope doc, since session count grows passively across repeated demos.
 */
class InventoryRepository(
    private val sessionDao: InventorySessionDao,
    private val tagDao: ScannedTagDao,
    private val itemDao: ItemDao
) {
    fun observeSessions(): LiveData<List<InventorySessionEntity>> = sessionDao.observeAll()

    /** FR-29: start a new session, enforcing rolling retention at MAX_SESSIONS (Section 4). */
    suspend fun startSession(code: String): Long {
        val count = sessionDao.count()
        Log.d(TAG, "startSession: Current session count = $count, MAX_SESSIONS = ${DemoLimits.MAX_SESSIONS}")

        if (count >= DemoLimits.MAX_SESSIONS) {
            val oldest = sessionDao.getAllAscending().firstOrNull()
            if (oldest != null) {
                Log.d(TAG, "Rolling purge: Deleting oldest session (id=${oldest.id}, code=${oldest.code})")
                sessionDao.delete(oldest)
            }
        }

        val newSessionId = sessionDao.insert(InventorySessionEntity(code = code))
        Log.d(TAG, "Created new session (id=$newSessionId, code=$code)")
        return newSessionId
    }

    /** Get all sessions for debugging. */
    suspend fun getAllSessions(): List<InventorySessionEntity> {
        val sessions = sessionDao.getAllAscending().reversed() // Reverse to get descending order
        Log.d(TAG, "getAllSessions: Found ${sessions.size} sessions")
        sessions.forEach { Log.d(TAG, "  - Session(id=${it.id}, code=${it.code}, createdAt=${it.createdAt})") }
        return sessions
    }

    /** FR-37: real-time dedup via DAO's OnConflictStrategy.IGNORE. */
    suspend fun recordScan(sessionId: Long, rfidTagId: String) {
        tagDao.insert(ScannedTagEntity(sessionId = sessionId, rfidTagId = rfidTagId))
    }

    suspend fun distinctScanCount(sessionId: Long): Int = tagDao.countDistinctForSession(sessionId)

    /** FR-36: stop the session. */
    suspend fun stopSession(sessionId: Long) {
        sessionDao.getById(sessionId)?.let {
            sessionDao.update(it.copy(stoppedAt = System.currentTimeMillis()))
        }
    }

    data class ResultItem(val item: ItemEntity, val status: Status)
    enum class Status { AVAILABLE, MISSING, EXCESS }

    /**
     * FR-40/41/45/46: computes Available/Missing/Excess against the local master, with an
     * optional category filter applied at display time (re-appliable without re-scanning —
     * the raw scanned_tags set is never discarded, FR-46).
     */
    suspend fun computeResults(sessionId: Long, categoryFilter: String?): List<ResultItem> {
        val scanned = tagDao.getForSession(sessionId).map { it.rfidTagId }.toSet()
        val master = itemDao.getAll().filter { categoryFilter == null || it.categoryName == categoryFilter }
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
                ItemEntity(rfidTagId = excessRfid, barcode = "", name = "(Unrecognized tag)", categoryName = ""),
                Status.EXCESS
            )
        }
        return results
    }
}
