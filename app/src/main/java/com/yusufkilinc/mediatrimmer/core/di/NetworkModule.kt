package com.yusufkilinc.mediatrimmer.core.di

import android.content.Context
import com.yusufkilinc.mediatrimmer.BuildConfig
import com.yusufkilinc.mediatrimmer.data.remote.FileDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)      // No timeout — large media files
        .writeTimeout(0, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
                    else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    @Provides
    @Singleton
    fun provideFileDownloader(
        client: OkHttpClient,
        @ApplicationContext context: Context
    ): FileDownloader = FileDownloader(client, context)
}
