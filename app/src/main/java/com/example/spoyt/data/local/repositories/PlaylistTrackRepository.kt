package com.example.spoyt.data.local.repositories

import com.example.spoyt.data.local.entities.PlaylistTrackCrossRef
import com.example.spoyt.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

interface PlaylistTrackRepository {

    suspend fun addTrack(playlistId: Int, videoId: String, position: Int)
    suspend fun addTracks(playlistId: Int, videoIds: List<String>)
    suspend fun removeTrack(playlistId: Int, videoId: String)
    suspend fun removeAllTracks(playlistId: Int)
    fun getTracksInPlaylist(playlistId: Int): Flow<List<TrackEntity>>
    suspend fun getVideoIdsInPlaylist(playlistId: Int): List<String>
    suspend fun isInAnyPlaylist(videoId: String): Boolean
    suspend fun getAllCrossRefs(): List<PlaylistTrackCrossRef>
}
