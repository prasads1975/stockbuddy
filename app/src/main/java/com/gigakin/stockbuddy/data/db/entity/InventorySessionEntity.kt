package com.gigakin.stockbuddy.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Inventory session (FR-29): a named stock-take session with KPI snapshots.
 * PK: id as UUID TEXT. Code: human-readable inventory code.
 * KPIs: full-scope (unfiltered) counts + filtered-scope counts for optional category filter.
 */
@Entity(
    tableName = "inventory_sessions",
    indices = [
        Index(value = ["stopped_at"])  // I-05: reverse-chron session list
    ]
)
data class InventorySessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "inventory_code")
    val code: String,
    @ColumnInfo(name = "started_at")
    val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "stopped_at")
    val stoppedAt: Long? = null,
    @ColumnInfo(name = "category_filter")
    val categoryFilter: String? = null,

    // Full scope KPIs (unfiltered)
    val totalInMaster: Int = 0,
    val totalScanned: Int = 0,
    val availableCount: Int = 0,
    val missingCount: Int = 0,
    val excessCount: Int = 0,

    // Filtered scope KPIs
    val filteredAvailable: Int = 0,
    val filteredMissing: Int = 0,
    val filteredExcess: Int = 0
)
