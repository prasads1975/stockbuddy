package com.gigakin.stockbuddy.data.repo

import com.gigakin.stockbuddy.data.db.dao.ProductMasterDao
import com.gigakin.stockbuddy.data.db.entity.ProductMasterEntity

/**
 * FR-21: auto-upsert on save, keyed by Barcode (primary business identifier).
 * Product Master is the canonical catalogue of all products in the system.
 */
class ProductRepository(private val dao: ProductMasterDao) {
    suspend fun upsertFromLinkedItem(
        barcode: String,
        productName: String,
        category: String,
        attributesJson: String
    ) {
        dao.upsert(
            ProductMasterEntity(
                barcode = barcode,
                productName = productName,
                category = category,
                attributesJson = attributesJson
            )
        )
    }

    suspend fun getByBarcode(barcode: String): ProductMasterEntity? {
        return dao.getByBarcode(barcode)
    }
}
