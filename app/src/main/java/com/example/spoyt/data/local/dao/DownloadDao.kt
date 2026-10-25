package com.example.spoyt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spoyt.data.local.entities.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY rowid DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE videoId = :videoId")
    suspend fun getDownload(videoId: String): DownloadEntity?

    @Query("UPDATE downloads SET state = :state WHERE videoId = :videoId")
    suspend fun setState(videoId: String, state: String)

    @Query("UPDATE downloads SET filePath = :filePath, sizeBytes = :sizeBytes, state = 'DONE' WHERE videoId = :videoId")
    suspend fun markDone(videoId: String, filePath: String, sizeBytes: Long)

    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun delete(videoId: String)
}
