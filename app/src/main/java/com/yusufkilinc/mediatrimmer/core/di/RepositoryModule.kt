package com.yusufkilinc.mediatrimmer.core.di

import com.yusufkilinc.mediatrimmer.data.repository.HistoryRepositoryImpl
import com.yusufkilinc.mediatrimmer.data.repository.MediaRepositoryImpl
import com.yusufkilinc.mediatrimmer.domain.repository.HistoryRepository
import com.yusufkilinc.mediatrimmer.domain.repository.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
}
