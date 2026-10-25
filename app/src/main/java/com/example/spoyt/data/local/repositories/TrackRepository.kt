package com.example.spoyt.data.local.repositories

import com.example.spoyt.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

interface TrackRepository {

    suspend fun saveTrack(track: TrackEntity)
    suspend fun saveTracks(tracks: List<TrackEntity>)
    suspend fun deleteTrack(videoId: String)
    fun getLikedTracks(): Flow<List<TrackEntity>>
    fun getAllTracks(): Flow<List<TrackEntity>>
    suspend fun getTrack(videoId: String): TrackEntity?
    suspend fun trackExists(videoId: String): Boolean
    suspend fun setLiked(videoId: String, liked: Boolean)
}
