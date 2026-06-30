package com.gigakin.stockbuddy.data.db.dao

import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.DeliveryHistoryEntity

@Dao
interface DeliveryHistoryDao {
    @Query("SELECT * FROM delivery_history WHERE report_id = :reportId ORDER BY timestamp DESC")
    suspend fun getForReport(reportId: Long): List<DeliveryHistoryEntity>

    @Query("SELECT * FROM delivery_history WHERE session_id = :sessionId ORDER BY timestamp DESC")
    suspend fun getForSession(sessionId: String): List<DeliveryHistoryEntity>

    @Insert
    suspend fun insert(history: DeliveryHistoryEntity): Long

    @Delete
    suspend fun delete(history: DeliveryHistoryEntity)
}
