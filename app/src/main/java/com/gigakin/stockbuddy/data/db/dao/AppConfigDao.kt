package com.gigakin.stockbuddy.data.db.dao

import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.AppConfigEntity

@Dao
interface AppConfigDao {
    @Query("SELECT value FROM app_config WHERE key = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(config: AppConfigEntity)

    @Delete
    suspend fun delete(config: AppConfigEntity)
}
