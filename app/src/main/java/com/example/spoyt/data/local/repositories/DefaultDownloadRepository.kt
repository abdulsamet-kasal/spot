package com.example.spoyt.data.local.repositories

import com.example.spoyt.data.local.AppDatabase
import com.example.spoyt.data.local.dao.DownloadDao
import com.example.spoyt.data.local.entities.DownloadEntity
import kotlinx.coroutines.flow.Flow

class DefaultDownloadRepository(private val database: AppDatabase) : DownloadRepository {

    private val dao: DownloadDao = database.downloadDao()

    override suspend fun insertOrUpdate(download: DownloadEntity) {
        dao.insert(download)
    }

    override fun getAllDownloads(): Flow<List<DownloadEntity>> = dao.getAllDownloads()

    override suspend fun getDownload(videoId: String): DownloadEntity? = dao.getDownload(videoId)

    override suspend fun setState(videoId: String, state: String) {
        dao.setState(videoId, state)
    }

    override suspend fun markDone(videoId: String, filePath: String, sizeBytes: Long) {
        dao.markDone(videoId, filePath, sizeBytes)
    }

    override suspend fun delete(videoId: String) {
        dao.delete(videoId)
    }
}
