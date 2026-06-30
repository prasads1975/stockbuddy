package com.gigakin.stockbuddy.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Session tags crash buffer (FR-34/37): minimal record of scanned RFID tags in a session.
 * Composite PK: (session_id, rfid_tag_id). No timestamps, no auto-id.
 * Purpose: crash recovery only. Immutable results snapshots are stored in session_result_items.
 */
@Entity(
    tableName = "session_tags",
    primaryKeys = ["session_id", "rfid_tag_id"],
    indices = [
        Index(value = ["session_id"])  // I-06
    ],
    foreignKeys = [
        ForeignKey(
            entity = InventorySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SessionTagEntity(
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "rfid_tag_id")
    val rfidTagId: String
)
