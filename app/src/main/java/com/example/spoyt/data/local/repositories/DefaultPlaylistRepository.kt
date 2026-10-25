package com.example.spoyt.data.local.repositories

import com.example.spoyt.data.local.AppDatabase
import com.example.spoyt.data.local.dao.PlaylistDao
import com.example.spoyt.data.local.entities.PlaylistEntity
import kotlinx.coroutines.flow.Flow

class DefaultPlaylistRepository(private val database: AppDatabase) : PlaylistRepository {

    private val dao: PlaylistDao = database.playlistDao()

    override suspend fun createPlaylist(name: String): Long {
        return dao.insert(PlaylistEntity(name = name))
    }

    override suspend fun renamePlaylist(id: Long, name: String) {
        dao.rename(id, name)
    }

    override suspend fun deletePlaylist(id: Long) {
        val playlist = dao.getPlaylist(id)
        if (playlist != null) {
            dao.delete(playlist)
        }
    }

    override fun getAllPlaylists(): Flow<List<PlaylistEntity>> = dao.getAllPlaylists()

    override suspend fun getPlaylist(id: Long): PlaylistEntity? = dao.getPlaylist(id)
}
