package com.example.spoyt.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spoyt.data.local.entities.PlayHistoryEntity
import com.example.spoyt.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PlayHistoryEntity)

    @Query("DELETE FROM play_history")
    suspend fun clearAll()

    @Query("""
        SELECT t.* FROM tracks t
        JOIN play_history ph ON t.videoId = ph.videoId
        GROUP BY t.videoId
        ORDER BY MAX(ph.playedAt) DESC
    """)
    fun getRecentTracks(): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.*, COUNT(ph.id) AS playCount FROM tracks t
        JOIN play_history ph ON t.videoId = ph.videoId
        GROUP BY t.videoId
        ORDER BY playCount DESC, MAX(ph.playedAt) DESC
        LIMIT :limit
    """)
    fun getMostPlayedTracks(limit: Int): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM play_history WHERE videoId = :videoId")
    suspend fun count(videoId: String): Int
}
