package com.gigakin.stockbuddy.data.db.dao

import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.SessionResultItemEntity

@Dao
interface SessionResultItemDao {
    @Query("SELECT * FROM session_result_items WHERE session_id = :sessionId")
    suspend fun getForSession(sessionId: String): List<SessionResultItemEntity>

    @Query("SELECT * FROM session_result_items WHERE session_id = :sessionId AND status = :status")
    suspend fun getForSessionAndStatus(sessionId: String, status: String): List<SessionResultItemEntity>

    @Query("SELECT COUNT(*) FROM session_result_items WHERE session_id = :sessionId AND status = :status")
    suspend fun countForStatus(sessionId: String, status: String): Int

    @Insert
    suspend fun insertAll(items: List<SessionResultItemEntity>)

    @Insert
    suspend fun insert(item: SessionResultItemEntity)

    @Delete
    suspend fun deleteAll(items: List<SessionResultItemEntity>)
}
