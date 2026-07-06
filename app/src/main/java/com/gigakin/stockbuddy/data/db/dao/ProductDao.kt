package com.gigakin.stockbuddy.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.ProductMasterEntity

/**
 * Read/write projection for the product-level Assets screen (v2, §4.0.6): one row per product
 * with the resolved category name and the count of physical units linked to it.
 */
data class ProductSummary(
    val barcode: String,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "attributes") val attributesJson: String,
    @ColumnInfo(name = "linked_count") val linkedCount: Int
)

@Dao
interface ProductMasterDao {
    @Query("SELECT * FROM product_master WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductMasterEntity?

    @Query("SELECT COUNT(*) FROM product_master WHERE category_id = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    /** Product-level Assets list: product ⋈ categories + linked-unit count, searchable/filterable. */
    @Query("""
        SELECT p.barcode, p.product_name, p.category_id, c.name AS category_name,
               p.attributes, COUNT(l.rfid_tag_id) AS linked_count
        FROM product_master p
        JOIN categories c ON c.id = p.category_id
        LEFT JOIN linked_items l ON l.barcode = p.barcode
        WHERE (:query = '' OR p.product_name LIKE '%' || :query || '%'
               OR p.barcode LIKE '%' || :query || '%'
               OR c.name LIKE '%' || :query || '%')
          AND (:categoryId IS NULL OR p.category_id = :categoryId)
        GROUP BY p.barcode
        ORDER BY p.product_name ASC
    """)
    fun search(query: String, categoryId: Long?): LiveData<List<ProductSummary>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(product: ProductMasterEntity)

    @Update
    suspend fun update(product: ProductMasterEntity)

    @Delete
    suspend fun delete(product: ProductMasterEntity)
}
