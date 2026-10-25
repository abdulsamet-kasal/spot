package com.example.spoyt.data.local.repositories

import com.example.spoyt.data.local.AppDatabase
import com.example.spoyt.data.local.dao.TrackDao
import com.example.spoyt.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

class DefaultTrackRepository(private val database: AppDatabase) : TrackRepository {

    private val dao: TrackDao = database.trackDao()

    override suspend fun saveTrack(track: TrackEntity) {
        dao.insert(track)
    }

    override suspend fun saveTracks(tracks: List<TrackEntity>) {
        dao.insertAll(tracks)
    }

    override suspend fun deleteTrack(videoId: String) {
        dao.delete(videoId)
    }

    override fun getLikedTracks(): Flow<List<TrackEntity>> = dao.getLikedTracks()

    override fun getAllTracks(): Flow<List<TrackEntity>> = dao.getAllTracks()

    override suspend fun getTrack(videoId: String): TrackEntity? = dao.getTrack(videoId)

    override suspend fun trackExists(videoId: String): Boolean = dao.exists(videoId)

    override suspend fun setLiked(videoId: String, liked: Boolean) {
        dao.setLiked(videoId, liked)
    }
}
