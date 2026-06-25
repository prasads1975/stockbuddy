package com.gigakin.stockbuddy.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity

@Dao
interface FieldDefinitionDao {
    @Query("SELECT * FROM field_definitions ORDER BY sortOrder ASC")
    fun observeAll(): LiveData<List<FieldDefinitionEntity>>

    @Query("SELECT * FROM field_definitions ORDER BY sortOrder ASC")
    suspend fun getAll(): List<FieldDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(field: FieldDefinitionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fields: List<FieldDefinitionEntity>)

    @Update
    suspend fun update(field: FieldDefinitionEntity)

    @Delete
    suspend fun delete(field: FieldDefinitionEntity)

    @Query("DELETE FROM field_definitions")
    suspend fun clearAll()
}
