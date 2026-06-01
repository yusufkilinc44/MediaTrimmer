package com.yusufkilinc.mediatrimmer.data.local.db.dao

import androidx.room.*
import com.yusufkilinc.mediatrimmer.data.local.db.entity.ProcessingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessingHistoryDao {

    @Query("SELECT * FROM processing_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<ProcessingHistoryEntity>>

    @Query("SELECT * FROM processing_history ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<ProcessingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProcessingHistoryEntity)

    @Query("DELETE FROM processing_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM processing_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM processing_history")
    suspend fun getCount(): Int
}
