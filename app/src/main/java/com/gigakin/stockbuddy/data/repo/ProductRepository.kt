package com.gigakin.stockbuddy.data.repo

import androidx.lifecycle.LiveData
import com.gigakin.stockbuddy.data.db.dao.ProductMasterDao
import com.gigakin.stockbuddy.data.db.dao.ProductSummary
import com.gigakin.stockbuddy.data.db.entity.ProductMasterEntity

/**
 * FR-21/22: Product Master management, keyed by Barcode (primary business identifier).
 * Canonical catalogue + product-level Assets screen (v2, §4.0.6). category_id is an FK.
 * Append-only on link; editable/deletable from Assets (delete cascades to linked units).
 */
class ProductRepository(private val dao: ProductMasterDao) {

    /** Product-level Assets list: product ⋈ categories + linked-unit count. */
    fun search(query: String, categoryId: Long?): LiveData<List<ProductSummary>> =
        dao.search(query, categoryId)

    suspend fun getByBarcode(barcode: String): ProductMasterEntity? = dao.getByBarcode(barcode)

    suspend fun countByCategory(categoryId: Long): Int = dao.countByCategory(categoryId)

    suspend fun insert(
        barcode: String,
        productName: String,
        categoryId: Long,
        attributesJson: String = "{}"
    ) {
        dao.insert(
            ProductMasterEntity(
                barcode = barcode,
                productName = productName,
                categoryId = categoryId,
                attributesJson = attributesJson
            )
        )
    }

    suspend fun update(product: ProductMasterEntity) =
        dao.update(product.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(product: ProductMasterEntity) = dao.delete(product)
}
