package com.gigakin.stockbuddy.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity

@Dao
interface LinkedItemDao {
    @Query("SELECT * FROM linked_items ORDER BY product_name ASC")
    fun observeAll(): LiveData<List<LinkedItemEntity>>

    @Query("""
        SELECT * FROM linked_items
        WHERE (:category IS NULL OR category = :category)
        AND (product_name LIKE '%' || :query || '%'
             OR barcode LIKE '%' || :query || '%'
             OR rfid_tag_id LIKE '%' || :query || '%')
        ORDER BY product_name ASC
    """)
    fun search(query: String, category: String?): LiveData<List<LinkedItemEntity>>

    @Query("SELECT * FROM linked_items")
    suspend fun getAll(): List<LinkedItemEntity>

    @Query("SELECT * FROM linked_items WHERE rfid_tag_id = :rfid LIMIT 1")
    suspend fun getByRfid(rfid: String): LinkedItemEntity?

    @Query("SELECT COUNT(*) FROM linked_items")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: LinkedItemEntity)

    @Update
    suspend fun update(item: LinkedItemEntity)

    @Delete
    suspend fun delete(item: LinkedItemEntity)
}
