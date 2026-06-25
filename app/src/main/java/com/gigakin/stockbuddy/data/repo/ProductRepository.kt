package com.gigakin.stockbuddy.data.repo

import com.gigakin.stockbuddy.data.db.dao.ProductDao
import com.gigakin.stockbuddy.data.db.entity.ProductEntity

/** FR-21: auto-upsert on save, matched/grouped by Barcode (Section 1.7) — not Article ID. */
class ProductRepository(private val dao: ProductDao) {
    suspend fun upsertFromLinkedItem(
        barcode: String, name: String, categoryName: String,
        articleId: String?, attributesJson: String
    ) {
        dao.upsert(ProductEntity(barcode, name, categoryName, articleId, attributesJson))
    }
}
