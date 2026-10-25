package com.example.spoyt.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One past search query; the query text is the primary key so re-searching bumps it. */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)
