package com.gigakin.stockbuddy.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.InventorySessionEntity

@Dao
interface InventorySessionDao {
    @Query("SELECT * FROM inventory_sessions ORDER BY CASE WHEN stopped_at IS NULL THEN 0 ELSE 1 END ASC, stopped_at DESC")
    fun observeAll(): LiveData<List<InventorySessionEntity>>

    @Query("SELECT * FROM inventory_sessions ORDER BY CASE WHEN stopped_at IS NULL THEN 0 ELSE 1 END ASC, stopped_at DESC")
    suspend fun getAll(): List<InventorySessionEntity>

    @Query("SELECT * FROM inventory_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InventorySessionEntity?

    @Query("SELECT COUNT(*) FROM inventory_sessions")
    suspend fun count(): Int

    @Insert
    suspend fun insert(session: InventorySessionEntity)

    @Update
    suspend fun update(session: InventorySessionEntity)

    @Delete
    suspend fun delete(session: InventorySessionEntity)
}
