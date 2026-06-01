package com.yusufkilinc.mediatrimmer.core.di

import android.content.Context
import androidx.work.WorkManager
import com.yusufkilinc.mediatrimmer.core.util.NotificationUtils
import com.yusufkilinc.mediatrimmer.data.local.datastore.AppSettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNotificationUtils(
        @ApplicationContext context: Context
    ): NotificationUtils = NotificationUtils(context)

    @Provides
    @Singleton
    fun provideAppSettingsDataStore(
        @ApplicationContext context: Context
    ): AppSettingsDataStore = AppSettingsDataStore(context)

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)
}
