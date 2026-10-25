package com.example.spoyt.data.local.repositories

import com.example.spoyt.data.local.AppDatabase
import com.example.spoyt.data.local.entities.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

class DefaultSearchHistoryRepository(private val database: AppDatabase) : SearchHistoryRepository {

    private val dao = database.searchHistoryDao()

    override suspend fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dao.insert(SearchHistoryEntity(query = trimmed))
    }

    override fun getRecent(limit: Int): Flow<List<SearchHistoryEntity>> = dao.getRecent(limit)

    override suspend fun delete(query: String) = dao.delete(query.trim())

    override suspend fun clearAll() = dao.clearAll()
}
