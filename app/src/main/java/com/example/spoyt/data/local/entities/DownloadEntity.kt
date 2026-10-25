package com.example.spoyt.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val videoId: String,
    val filePath: String,
    val state: String, // QUEUED, DOWNLOADING, DONE, FAILED
    val sizeBytes: Long = 0
)
