package com.gigakin.stockbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Product Master (FR-21/22): SKU-level catalogue, auto-upserted on every Individual Linking save.
 * Matched/grouped by Barcode (Section 1.7) — NOT Article ID, which is optional per-deployment.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val categoryName: String,
    val articleId: String? = null,   // present only if Article ID mode != NOT_USED
    val attributesJson: String = "{}" // domain-specific fields (Section 1.7 JSON attribute set)
)
