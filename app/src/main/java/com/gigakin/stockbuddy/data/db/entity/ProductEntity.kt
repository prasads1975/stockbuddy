package com.gigakin.stockbuddy.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Product Master (FR-21/22): SKU-level catalogue, the canonical home for product data.
 * PK: barcode (TEXT) — the business identifier for product lookup in POS systems.
 * category_id: FK to categories (v2 — normalized; was a denormalized string). See System Design §4.0.
 * Domain-specific fields: stored in attributes JSON (product-level only — all units of a barcode share them).
 */
@Entity(
    tableName = "product_master",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["category_id"])     // For category filtering / delete-impact counts
    ]
)
data class ProductMasterEntity(
    @PrimaryKey
    val barcode: String,
    @ColumnInfo(name = "product_name")
    val productName: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "attributes")
    val attributesJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
