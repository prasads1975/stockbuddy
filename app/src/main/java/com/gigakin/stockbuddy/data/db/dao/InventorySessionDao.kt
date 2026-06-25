package com.gigakin.stockbuddy.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.InventorySessionEntity

@Dao
interface InventorySessionDao {
    @Query("SELECT * FROM inventory_sessions ORDER BY createdAt DESC")
    fun observeAll(): LiveData<List<InventorySessionEntity>>

    @Query("SELECT * FROM inventory_sessions ORDER BY createdAt ASC")
    suspend fun getAllAscending(): List<InventorySessionEntity>

    @Query("SELECT * FROM inventory_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): InventorySessionEntity?

    @Query("SELECT COUNT(*) FROM inventory_sessions")
    suspend fun count(): Int

    @Insert
    suspend fun insert(session: InventorySessionEntity): Long

    @Update
    suspend fun update(session: InventorySessionEntity)

    @Delete
    suspend fun delete(session: InventorySessionEntity)
}
