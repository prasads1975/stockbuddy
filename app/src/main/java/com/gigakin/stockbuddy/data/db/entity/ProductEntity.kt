package com.gigakin.stockbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Product Master (FR-21/22): SKU-level catalogue, auto-upserted on every Individual Linking save.
 * Matched/grouped by Barcode (Section 1.7). All domain-specific fields including Article ID
 * are stored in attributesJson for maximum flexibility.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val categoryName: String,
    val attributesJson: String = "{}" // domain-specific fields including Article ID (Section 1.7)
)
