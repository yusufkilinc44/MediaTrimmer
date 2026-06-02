package com.yusufkilinc.mediatrimmer.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yusufkilinc.mediatrimmer.data.local.db.dao.ProcessingHistoryDao
import com.yusufkilinc.mediatrimmer.data.local.db.entity.ProcessingHistoryEntity

@Database(
    entities = [ProcessingHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): ProcessingHistoryDao
}
