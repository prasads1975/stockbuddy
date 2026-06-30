package com.gigakin.stockbuddy.data.db.dao

import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.GeneratedReportEntity

@Dao
interface GeneratedReportDao {
    @Query("SELECT * FROM generated_reports WHERE session_id = :sessionId ORDER BY generated_at DESC")
    suspend fun getForSession(sessionId: String): List<GeneratedReportEntity>

    @Query("SELECT * FROM generated_reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): GeneratedReportEntity?

    @Insert
    suspend fun insert(report: GeneratedReportEntity): Long

    @Update
    suspend fun update(report: GeneratedReportEntity)

    @Delete
    suspend fun delete(report: GeneratedReportEntity)
}
