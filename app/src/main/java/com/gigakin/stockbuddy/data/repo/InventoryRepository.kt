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

    /** FR-29: start a new session, enforcing rolling retention at MAX_SESSIONS (Section 4). */
    suspend fun startSession(code: String): String {
        val count = sessionDao.count()
        if (count >= DemoLimits.MAX_SESSIONS) {
            sessionDao.getAll().firstOrNull()?.let { sessionDao.delete(it) }
        }
        val newSessionId = UUID.randomUUID().toString()
        sessionDao.insert(InventorySessionEntity(id = newSessionId, code = code))
        return newSessionId
    }

    suspend fun getAllSessions(): List<InventorySessionEntity> = sessionDao.getAll()

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

    data class SessionStatsData(val available: Int = 0, val missing: Int = 0, val excess: Int = 0)

    /** Real-time session statistics during scanning (§4.0.4) — in-memory master set, no join. */
    suspend fun computeSessionStats(sessionId: String): SessionStatsData {
        val scanned = tagDao.getForSession(sessionId).map { it.rfidTagId }.toSet()
        val masterRfids = linkedItemDao.getAllRfids().toSet()
        val available = scanned.count { it in masterRfids }
        val missing = masterRfids.size - available
        val excess = scanned.count { it !in masterRfids }
        return SessionStatsData(available, missing, excess)
    }
}
