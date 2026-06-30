package com.gigakin.stockbuddy.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Session result item (§4.2.2): immutable snapshot of results computed at session completion.
 * PK: auto-generated id. FK: session_id (TEXT UUID) to inventory_sessions.CASCADE.
 * Status: AVAILABLE | MISSING | EXCESS. For EXCESS rows, product fields are NULL.
 * For AVAILABLE/MISSING, all product fields are populated from product_master snapshot.
 * Barcode is the business identifier (PK of product_master).
 */
@Entity(
    tableName = "session_result_items",
    foreignKeys = [
        ForeignKey(
            entity = InventorySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["session_id"]),              // I-07
        Index(value = ["session_id", "status"])     // I-08 (composite)
    ]
)
data class SessionResultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "status") val status: String,  // AVAILABLE | MISSING | EXCESS
    @ColumnInfo(name = "rfid_tag_id") val rfidTagId: String,

    // Snapshot columns (NULL for EXCESS rows)
    @ColumnInfo(name = "product_name") val productName: String?,
    val barcode: String?,
    val category: String?,
    @ColumnInfo(name = "attributes") val attributesJson: String? = null
)
