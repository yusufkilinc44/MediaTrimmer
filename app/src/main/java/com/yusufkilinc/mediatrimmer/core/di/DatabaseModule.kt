package com.yusufkilinc.mediatrimmer.core.di

import android.content.Context
import androidx.room.Room
import com.yusufkilinc.mediatrimmer.data.local.db.AppDatabase
import com.yusufkilinc.mediatrimmer.data.local.db.dao.ProcessingHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "mediatrimmer.db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideHistoryDao(db: AppDatabase): ProcessingHistoryDao = db.historyDao()
}
