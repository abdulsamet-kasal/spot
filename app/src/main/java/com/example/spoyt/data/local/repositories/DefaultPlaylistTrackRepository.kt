package com.example.spoyt.data.local.repositories

import com.example.spoyt.data.local.AppDatabase
import com.example.spoyt.data.local.dao.PlaylistTrackDao
import com.example.spoyt.data.local.entities.PlaylistTrackCrossRef
import com.example.spoyt.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

class DefaultPlaylistTrackRepository(private val database: AppDatabase) : PlaylistTrackRepository {

    private val dao: PlaylistTrackDao = database.playlistTrackDao()

    override suspend fun addTrack(playlistId: Int, videoId: String, position: Int) {
        dao.insert(PlaylistTrackCrossRef(playlistId, videoId, position))
    }

    override suspend fun addTracks(playlistId: Int, videoIds: List<String>) {
        val refs = videoIds.mapIndexed { index, videoId ->
            PlaylistTrackCrossRef(playlistId, videoId, index)
        }
        dao.insertAll(refs)
    }

    override suspend fun removeTrack(playlistId: Int, videoId: String) {
        dao.remove(playlistId, videoId)
    }

    override suspend fun removeAllTracks(playlistId: Int) {
        dao.removeAll(playlistId)
    }

    override fun getTracksInPlaylist(playlistId: Int): Flow<List<TrackEntity>> =
        dao.getTracksInPlaylist(playlistId)

    override suspend fun getVideoIdsInPlaylist(playlistId: Int): List<String> =
        dao.getVideoIdsInPlaylist(playlistId)

    override suspend fun isInAnyPlaylist(videoId: String): Boolean = dao.isInAnyPlaylist(videoId)

    override suspend fun getAllCrossRefs(): List<PlaylistTrackCrossRef> = dao.getAllCrossRefs()
}
