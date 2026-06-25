package com.gigakin.stockbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** FR-29: a named stock-take session. */
@Entity(tableName = "inventory_sessions")
data class InventorySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val createdAt: Long = System.currentTimeMillis(),
    val stoppedAt: Long? = null
)
