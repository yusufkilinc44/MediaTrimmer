package com.yusufkilinc.mediatrimmer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.yusufkilinc.mediatrimmer.core.util.NotificationUtils
import com.yusufkilinc.mediatrimmer.data.remote.FileDownloader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val fileDownloader: FileDownloader,
    private val notificationUtils: NotificationUtils
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_URL         = "url"
        const val KEY_LOCAL_PATH  = "local_path"
        const val KEY_ERROR       = "error"
        const val KEY_PROGRESS    = "progress"
        const val NOTIFICATION_ID = 1002
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing URL"))

        setForeground(createForegroundInfo(0))

        return fileDownloader.downloadFile(url) { progress ->
            if (progress.percent >= 0) {
                setProgressAsync(workDataOf(KEY_PROGRESS to progress.percent))
                // Update foreground notification
                val notification = notificationUtils.buildProgressNotification(
                    channelId    = NotificationUtils.CHANNEL_DOWNLOAD,
                    title        = "Downloading…",
                    progressText = "${progress.percent}%",
                    progress     = progress.percent,
                    indeterminate = progress.total <= 0
                )
                // Can't call setForeground from callback — update notification directly
                val nm = applicationContext.getSystemService(android.app.NotificationManager::class.java)
                nm.notify(NOTIFICATION_ID, notification)
            }
        }.fold(
            onSuccess = { localPath ->
                Result.success(workDataOf(KEY_LOCAL_PATH to localPath))
            },
            onFailure = { e ->
                Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Download failed")))
            }
        )
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notification = notificationUtils.buildProgressNotification(
            channelId     = NotificationUtils.CHANNEL_DOWNLOAD,
            title         = "Downloading file…",
            progressText  = if (progress > 0) "$progress%" else "Starting…",
            progress      = progress,
            indeterminate = progress == 0
        )
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
