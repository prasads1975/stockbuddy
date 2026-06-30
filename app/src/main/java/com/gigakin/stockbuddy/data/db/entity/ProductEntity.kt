package com.gigakin.stockbuddy.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Product Master (FR-21/22): SKU-level catalogue, auto-upserted on every Individual Linking save.
 * PK: barcode (TEXT) — the business identifier for product lookup in POS systems.
 * Uniqueness: barcode uniquely identifies a product (standard in retail).
 * Denormalized fields: product_name, category for O(1) scan-time lookup.
 * Domain-specific fields: stored in attributes JSON.
 */
@Entity(
    tableName = "product_master",
    indices = [
        Index(value = ["category"])        // For category filtering
    ]
)
data class ProductMasterEntity(
    @PrimaryKey
    val barcode: String,
    @ColumnInfo(name = "product_name")
    val productName: String,
    val category: String,
    @ColumnInfo(name = "attributes")
    val attributesJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
