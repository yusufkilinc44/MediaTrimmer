package com.yusufkilinc.mediatrimmer.domain.repository

import com.yusufkilinc.mediatrimmer.data.local.db.entity.ProcessingHistoryEntity
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getAllHistory(): Flow<List<ProcessingHistoryEntity>>
    fun getRecentHistory(limit: Int): Flow<List<ProcessingHistoryEntity>>
    suspend fun saveEntry(entity: ProcessingHistoryEntity)
    suspend fun deleteById(id: String)
    suspend fun clearAll()
}
