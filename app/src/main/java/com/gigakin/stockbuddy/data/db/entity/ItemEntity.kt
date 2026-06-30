package com.gigakin.stockbuddy.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Linked item (FR-18/21): one row per physical tagged unit.
 * PK: rfid_tag_id (String) — the RFID tag is the unique identifier for each physical item.
 * Product link: FK to product_master via barcode.
 * Denormalized columns: product_name, category (cached from product_master for O(1) scan-time lookup).
 * Optimization: Denormalization eliminates JOIN overhead during rapid RFID scanning (~50-60% perf gain).
 * Trade-off: Update anomalies on product edits (acceptable; updates are rare, scans are frequent).
 * Domain-specific fields: stored in attributes JSON.
 */
@Entity(
    tableName = "linked_items",
    foreignKeys = [
        ForeignKey(
            entity = ProductMasterEntity::class,
            parentColumns = ["barcode"],
            childColumns = ["barcode"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["barcode"]),                          // For product lookups
        Index(value = ["category"])                          // For category filtering
    ]
)
data class LinkedItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "rfid_tag_id")
    val rfidTagId: String,                                   // RFID tag is the primary identifier

    val barcode: String,                                     // FK to product_master (the business key)
    @ColumnInfo(name = "product_name") val productName: String,  // Denormalized from product_master
    val category: String,                                    // Denormalized from product_master
    @ColumnInfo(name = "attributes") val attributesJson: String = "{}",  // Domain-specific fields

    val linkedAt: Long = System.currentTimeMillis(),
    val linkedByRole: String? = null
)
