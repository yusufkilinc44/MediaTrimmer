package com.yusufkilinc.mediatrimmer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.Level
import com.yusufkilinc.mediatrimmer.core.util.NotificationUtils
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MediaTrimmerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationUtils: NotificationUtils

    override fun onCreate() {
        super.onCreate()
        notificationUtils.createNotificationChannels()

        // Suppress FFmpegKit verbose logs in release builds
        if (!BuildConfig.DEBUG) {
            FFmpegKitConfig.setLogLevel(Level.AV_LOG_QUIET)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
