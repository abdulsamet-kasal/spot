package com.example.spoyt.data.local.repositories

import com.example.spoyt.data.local.entities.PlayHistoryEntity
import com.example.spoyt.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {

    suspend fun addPlay(videoId: String)
    suspend fun clearHistory()
    fun getRecentTracks(): Flow<List<TrackEntity>>
    fun getMostPlayedTracks(limit: Int = 50): Flow<List<TrackEntity>>
    suspend fun playCount(videoId: String): Int
}
