package com.gigakin.stockbuddy.data.db.dao

import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.ProductMasterEntity

@Dao
interface ProductMasterDao {
    @Query("SELECT * FROM product_master WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductMasterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductMasterEntity)
}
