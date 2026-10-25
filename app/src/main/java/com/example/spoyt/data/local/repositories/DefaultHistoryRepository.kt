package com.example.spoyt.data.local.repositories

import com.example.spoyt.data.local.AppDatabase
import com.example.spoyt.data.local.dao.PlayHistoryDao
import com.example.spoyt.data.local.entities.PlayHistoryEntity
import com.example.spoyt.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

class DefaultHistoryRepository(private val database: AppDatabase) : HistoryRepository {

    private val dao: PlayHistoryDao = database.playHistoryDao()

    override suspend fun addPlay(videoId: String) {
        dao.insert(PlayHistoryEntity(videoId = videoId))
    }

    override suspend fun clearHistory() {
        dao.clearAll()
    }

    override fun getRecentTracks(): Flow<List<TrackEntity>> = dao.getRecentTracks()

    override fun getMostPlayedTracks(limit: Int): Flow<List<TrackEntity>> =
        dao.getMostPlayedTracks(limit)

    override suspend fun playCount(videoId: String): Int = dao.count(videoId)
}
