package com.gigakin.stockbuddy.data.db.dao

import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.SessionTagEntity

@Dao
interface SessionTagDao {
    @Query("SELECT * FROM session_tags WHERE session_id = :sessionId")
    suspend fun getForSession(sessionId: String): List<SessionTagEntity>

    @Query("SELECT COUNT(DISTINCT rfid_tag_id) FROM session_tags WHERE session_id = :sessionId")
    suspend fun countDistinctForSession(sessionId: String): Int

    // FR-37: dedup — IGNORE on conflict means a re-read of the same tag in the same
    // session is a no-op, satisfying real-time dedup without extra query logic.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: SessionTagEntity)
}
