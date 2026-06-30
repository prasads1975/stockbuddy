package com.gigakin.stockbuddy.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Delivery history (§4.2.4): audit log of export delivery attempts.
 * PK: auto-generated id.
 * FKs: report_id → generated_reports.CASCADE, session_id → inventory_sessions.NO_ACTION (denorm convenience).
 * Channel: SHARE | API | LAN. Outcome: OK | FAIL.
 */
@Entity(
    tableName = "delivery_history",
    foreignKeys = [
        ForeignKey(
            entity = GeneratedReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["report_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InventorySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.NO_ACTION  // Denorm convenience
        )
    ],
    indices = [
        Index(value = ["report_id"]),   // I-11
        Index(value = ["session_id"])   // I-12
    ]
)
data class DeliveryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "report_id") val reportId: Long,
    @ColumnInfo(name = "session_id") val sessionId: String,
    val channel: String,  // SHARE | API | LAN
    val destination: String,
    val timestamp: Long,
    val outcome: String,  // OK | FAIL
    val errorDetail: String?
)
