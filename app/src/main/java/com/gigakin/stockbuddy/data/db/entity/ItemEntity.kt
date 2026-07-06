package com.gigakin.stockbuddy.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Linked item (FR-18/21): one row per physical tagged unit.
 * PK: rfid_tag_id (String) — the RFID tag uniquely identifies each physical unit (NFR-18),
 * so a tag maps to exactly one barcode. This subsumes RFID+barcode uniqueness (System Design §4.0.6).
 * Normalized (v2): holds NO product data — product_name/category/attributes live on product_master
 * and are reached via the barcode FK. The scan hot path only needs RFID-set membership (no join).
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
        Index(value = ["barcode"])                           // FK lookup; cascade/count on product delete
    ]
)
data class LinkedItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "rfid_tag_id")
    val rfidTagId: String,                                   // RFID tag is the primary identifier

    val barcode: String,                                     // FK to product_master (the business key)

    val linkedAt: Long = System.currentTimeMillis(),
    val linkedByRole: String? = null                         // aspirational (RBAC); unused in MVP
)
