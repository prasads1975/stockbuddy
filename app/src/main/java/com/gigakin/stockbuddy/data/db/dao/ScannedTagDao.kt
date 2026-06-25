package com.gigakin.stockbuddy.data.db.dao

import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.ScannedTagEntity

@Dao
interface ScannedTagDao {
    @Query("SELECT * FROM scanned_tags WHERE sessionId = :sessionId")
    suspend fun getForSession(sessionId: Long): List<ScannedTagEntity>

    @Query("SELECT COUNT(DISTINCT rfidTagId) FROM scanned_tags WHERE sessionId = :sessionId")
    suspend fun countDistinctForSession(sessionId: Long): Int

    // FR-37: dedup — IGNORE on conflict means a re-read of the same tag in the same
    // session is a no-op, satisfying real-time dedup without extra query logic.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: ScannedTagEntity)
}
