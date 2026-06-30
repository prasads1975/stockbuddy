package com.gigakin.stockbuddy.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Generated report (§4.2.3): metadata for CSV exports from a session.
 * PK: auto-generated id. FK: session_id (TEXT UUID) to inventory_sessions.CASCADE.
 * File path: null after 30-day auto-purge (CR-02b).
 * Export scope: FULL | FILTERED. Category filter: null if scope=FULL.
 */
@Entity(
    tableName = "generated_reports",
    foreignKeys = [
        ForeignKey(
            entity = InventorySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["session_id"]),   // I-09
        Index(value = ["generated_at"])  // I-10
    ]
)
data class GeneratedReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "file_path") val filePath: String?,  // null after auto-purge
    @ColumnInfo(name = "export_scope") val exportScope: String,  // FULL | FILTERED
    @ColumnInfo(name = "category_filter") val categoryFilter: String?,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long,
    val rowCount: Int
)
