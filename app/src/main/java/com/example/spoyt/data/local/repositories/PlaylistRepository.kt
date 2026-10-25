package com.example.spoyt.data.local.repositories

import com.example.spoyt.data.local.entities.PlaylistEntity
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {

    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(id: Long, name: String)
    suspend fun deletePlaylist(id: Long)
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>
    suspend fun getPlaylist(id: Long): PlaylistEntity?
}
