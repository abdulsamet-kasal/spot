package com.example.spoyt.data.local.repositories

import com.example.spoyt.data.local.entities.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {

    suspend fun record(query: String)
    fun getRecent(limit: Int = 10): Flow<List<SearchHistoryEntity>>
    suspend fun delete(query: String)
    suspend fun clearAll()
}
