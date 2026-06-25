package com.gigakin.stockbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single linked physical unit (FR-05-10). RFID Tag ID is the guaranteed-unique
 * per-unit identifier (Section 1.7) — NOT Article ID, which is optional/deployment-configured.
 */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val rfidTagId: String,
    val barcode: String,
    val name: String,
    val categoryName: String,
    val articleId: String? = null,
    val attributesJson: String = "{}",
    val linkedAt: Long = System.currentTimeMillis()
)
