package com.gigakin.stockbuddy.data.repo

import android.util.Log
import androidx.lifecycle.LiveData
import com.gigakin.stockbuddy.data.db.dao.InventorySessionDao
import com.gigakin.stockbuddy.data.db.dao.LinkedItemDao
import com.gigakin.stockbuddy.data.db.dao.SessionResultItemDao
import com.gigakin.stockbuddy.data.db.dao.SessionTagDao
import com.gigakin.stockbuddy.data.db.entity.InventorySessionEntity
import com.gigakin.stockbuddy.data.db.entity.SessionResultItemEntity
import com.gigakin.stockbuddy.data.db.entity.SessionTagEntity
import com.gigakin.stockbuddy.util.DemoLimits
import java.util.UUID

private const val TAG = "InventoryRepository"
private const val UNRECOGNIZED = "(Unrecognized tag)"

/**
 * FR-29-47: Inventory session lifecycle, scanning, and Available/Missing/Excess computation.
 * v2 (§4.0.4/4.0.5): live counts use an in-memory master RFID set; at STOP an immutable
 * denormalized snapshot is written to session_result_items and all reads come from it.
 */
class InventoryRepository(
    private val sessionDao: InventorySessionDao,
    private val tagDao: SessionTagDao,
    private val linkedItemDao: LinkedItemDao,
    private val resultItemDao: SessionResultItemDao
) {
    fun observeSessions(): LiveData<List<InventorySessionEntity>> = sessionDao.observeAll()

    /**
     * FR-29/35: start a new session, enforcing rolling retention at MAX_SESSIONS (Section 4).
     * categoryFilter (null = "All") is a display/reporting filter only (FR-35) — it never
     * restricts what the C72 scans — and is persisted so it can carry over to Results (FR-46).
     */
    suspend fun startSession(code: String, categoryFilter: String? = null): String {
        val count = sessionDao.count()
        if (count >= DemoLimits.MAX_SESSIONS) {
            sessionDao.getAll().firstOrNull()?.let { sessionDao.delete(it) }
        }
        val newSessionId = UUID.randomUUID().toString()
        sessionDao.insert(InventorySessionEntity(id = newSessionId, code = code, categoryFilter = categoryFilter))
        return newSessionId
    }

    suspend fun getAllSessions(): List<InventorySessionEntity> = sessionDao.getAll()

    suspend fun getSession(sessionId: String): InventorySessionEntity? = sessionDao.getById(sessionId)

    /** FR-37: real-time dedup via DAO's OnConflictStrategy.IGNORE. */
    suspend fun recordScan(sessionId: String, rfidTagId: String) {
        tagDao.insert(SessionTagEntity(sessionId = sessionId, rfidTagId = rfidTagId))
    }

    suspend fun distinctScanCount(sessionId: String): Int = tagDao.countDistinctForSession(sessionId)

    /** FR-36: stop the session — timestamp, compute + persist the immutable snapshot, write KPIs. */
    suspend fun stopSession(sessionId: String) {
        val session = sessionDao.getById(sessionId) ?: return
        sessionDao.update(session.copy(stoppedAt = System.currentTimeMillis()))
        computeAndPersistSnapshot(sessionId)
    }

    /** §4.0.5: one snapshot row per master unit (AVAILABLE/MISSING) + one per excess tag. */
    private suspend fun computeAndPersistSnapshot(sessionId: String) {
        val scanned = tagDao.getForSession(sessionId).map { it.rfidTagId }.toSet()
        val master = linkedItemDao.getMasterWithDetails()
        val masterRfids = master.map { it.rfidTagId }.toSet()

        val rows = mutableListOf<SessionResultItemEntity>()
        var available = 0; var missing = 0
        master.forEach { m ->
            val isAvailable = m.rfidTagId in scanned
            if (isAvailable) available++ else missing++
            rows += SessionResultItemEntity(
                sessionId = sessionId,
                status = if (isAvailable) Status.AVAILABLE.name else Status.MISSING.name,
                rfidTagId = m.rfidTagId,
                productName = m.productName,
                barcode = m.barcode,
                category = m.category,
                attributesJson = m.attributesJson
            )
        }
        var excess = 0
        scanned.filter { it !in masterRfids }.forEach { rfid ->
            excess++
            rows += SessionResultItemEntity(
                sessionId = sessionId,
                status = Status.EXCESS.name,
                rfidTagId = rfid,
                productName = null, barcode = null, category = null, attributesJson = null
            )
        }
        resultItemDao.insertAll(rows)

        sessionDao.getById(sessionId)?.let { s ->
            sessionDao.update(
                s.copy(
                    totalInMaster = master.size,
                    totalScanned = scanned.size,
                    availableCount = available, missingCount = missing, excessCount = excess,
                    filteredAvailable = available, filteredMissing = missing, filteredExcess = excess
                )
            )
        }
        Log.d(TAG, "Snapshot for $sessionId: available=$available, missing=$missing, excess=$excess")
    }

    data class ResultItem(
        val rfidTagId: String,
        val productName: String,
        val barcode: String,
        val category: String,
        val attributesJson: String,
        val status: Status
    )
    enum class Status { AVAILABLE, MISSING, EXCESS }

    /**
     * FR-40/41/45/46: reads the immutable snapshot. Category filter is a re-appliable read-side
     * predicate over the frozen rows (EXCESS always retained). No recompute, no join.
     */
    suspend fun computeResults(sessionId: String, categoryFilter: String?): List<ResultItem> {
        return resultItemDao.getForSession(sessionId).mapNotNull { r ->
            val status = Status.valueOf(r.status)
            if (categoryFilter != null && status != Status.EXCESS && r.category != categoryFilter) return@mapNotNull null
            ResultItem(
                rfidTagId = r.rfidTagId,
                productName = r.productName ?: UNRECOGNIZED,
                barcode = r.barcode ?: "",
                category = r.category ?: "",
                attributesJson = r.attributesJson ?: "{}",
                status = status
            )
        }
    }

    /**
     * FR-46 (v3.19): single source of truth for "what should be shown/exported for this
     * session" — applies the same category scope-lock as the Results screen. If the session was
     * scanned with "All", returns everything. If scanned with a specific category, returns only
     * that category's Available/Missing rows plus all Excess (always unscoped). Used by both the
     * Results screen's initial load and CSV export, so the two can never drift out of sync again.
     */
    suspend fun computeScopedResults(sessionId: String): List<ResultItem> {
        val scope = getSession(sessionId)?.categoryFilter
        return computeResults(sessionId, scope)
    }

    data class SessionStatsData(val available: Int = 0, val missing: Int = 0, val excess: Int = 0)

    /**
     * Real-time session statistics during scanning (§4.0.4).
     * FR-35: when the session is scoped to a category, Available/Missing are computed against
     * only that category's master items (the operator is intentionally counting that category).
     * Excess always stays unscoped — it means "not in master at all" (genuinely unregistered),
     * not "outside today's category," so it's computed against the full master set regardless.
     */
    suspend fun computeSessionStats(sessionId: String, categoryFilter: String? = null): SessionStatsData {
        val scanned = tagDao.getForSession(sessionId).map { it.rfidTagId }.toSet()
        val allMasterRfids = linkedItemDao.getAllRfids().toSet()
        val scopedMasterRfids = if (categoryFilter == null) allMasterRfids else linkedItemDao.getRfidsByCategory(categoryFilter).toSet()
        val available = scanned.count { it in scopedMasterRfids }
        val missing = scopedMasterRfids.size - available
        val excess = scanned.count { it !in allMasterRfids }
        return SessionStatsData(available, missing, excess)
    }
}
