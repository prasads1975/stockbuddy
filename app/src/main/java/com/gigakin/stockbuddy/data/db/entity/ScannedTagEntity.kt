package com.gigakin.stockbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Raw scan results for a session (FR-34/37). Always retained in full regardless of
 * any category filter applied afterwards (FR-46) — filtering is a display-time concern only.
 */
@Entity(
    tableName = "scanned_tags",
    indices = [Index(value = ["sessionId", "rfidTagId"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = InventorySessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ScannedTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val rfidTagId: String,
    val scannedAt: Long = System.currentTimeMillis()
)
