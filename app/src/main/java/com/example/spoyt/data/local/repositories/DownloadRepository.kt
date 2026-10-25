package com.example.spoyt.data.local.repositories

import com.example.spoyt.data.local.entities.DownloadEntity
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {

    suspend fun insertOrUpdate(download: DownloadEntity)
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    suspend fun getDownload(videoId: String): DownloadEntity?
    suspend fun setState(videoId: String, state: String)
    suspend fun markDone(videoId: String, filePath: String, sizeBytes: Long)
    suspend fun delete(videoId: String)
}
