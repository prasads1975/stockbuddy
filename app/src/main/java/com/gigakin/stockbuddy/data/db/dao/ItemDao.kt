package com.gigakin.stockbuddy.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.ItemEntity

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun observeAll(): LiveData<List<ItemEntity>>

    @Query("""
        SELECT * FROM items
        WHERE (:category IS NULL OR categoryName = :category)
        AND (name LIKE '%' || :query || '%'
             OR barcode LIKE '%' || :query || '%'
             OR rfidTagId LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun search(query: String, category: String?): LiveData<List<ItemEntity>>

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE rfidTagId = :rfid LIMIT 1")
    suspend fun getByRfid(rfid: String): ItemEntity?

    @Query("SELECT COUNT(*) FROM items")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ItemEntity)

    @Update
    suspend fun update(item: ItemEntity)

    @Delete
    suspend fun delete(item: ItemEntity)
}
