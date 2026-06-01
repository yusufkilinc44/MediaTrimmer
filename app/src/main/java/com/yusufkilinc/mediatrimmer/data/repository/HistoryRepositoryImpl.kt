package com.yusufkilinc.mediatrimmer.data.repository

import com.yusufkilinc.mediatrimmer.data.local.db.dao.ProcessingHistoryDao
import com.yusufkilinc.mediatrimmer.data.local.db.entity.ProcessingHistoryEntity
import com.yusufkilinc.mediatrimmer.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val dao: ProcessingHistoryDao
) : HistoryRepository {

    override fun getAllHistory(): Flow<List<ProcessingHistoryEntity>> = dao.getAllHistory()

    override fun getRecentHistory(limit: Int): Flow<List<ProcessingHistoryEntity>> =
        dao.getRecentHistory(limit)

    override suspend fun saveEntry(entity: ProcessingHistoryEntity) = dao.insert(entity)

    override suspend fun deleteById(id: String) = dao.deleteById(id)

    override suspend fun clearAll() = dao.clearAll()
}
