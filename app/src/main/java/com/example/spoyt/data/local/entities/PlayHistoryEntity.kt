package com.example.spoyt.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "play_history",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["videoId"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("videoId"), Index("playedAt")]
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoId: String,
    val playedAt: Long = System.currentTimeMillis()
)
