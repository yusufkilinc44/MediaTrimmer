package com.yusufkilinc.mediatrimmer.core.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.yusufkilinc.mediatrimmer.MainActivity
import com.yusufkilinc.mediatrimmer.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_PROCESSING = "media_processing"
        const val CHANNEL_DOWNLOAD   = "media_download"
        const val CHANNEL_COMPLETE   = "media_complete"
        const val NOTIFICATION_PROCESSING_ID = 1001
        const val NOTIFICATION_DOWNLOAD_ID   = 1002
    }

    fun createNotificationChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)

        val processingChannel = NotificationChannel(
            CHANNEL_PROCESSING,
            context.getString(R.string.notif_channel_processing),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notif_channel_processing_desc)
            setShowBadge(false)
        }

        val downloadChannel = NotificationChannel(
            CHANNEL_DOWNLOAD,
            context.getString(R.string.notif_channel_download),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notif_channel_download_desc)
            setShowBadge(false)
        }

        val completeChannel = NotificationChannel(
            CHANNEL_COMPLETE,
            context.getString(R.string.notif_channel_complete),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        manager.createNotificationChannels(
            listOf(processingChannel, downloadChannel, completeChannel)
        )
    }

    fun buildProgressNotification(
        channelId: String,
        title: String,
        progressText: String,
        progress: Int,
        indeterminate: Boolean = false,
        cancelIntent: PendingIntent? = null
    ): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_media_processing)
            .setContentTitle(title)
            .setContentText(progressText)
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .apply {
                cancelIntent?.let {
                    addAction(R.drawable.ic_cancel, context.getString(R.string.btn_cancel), it)
                }
            }
            .build()
    }

    fun showCompletionNotification(outputPath: String, title: String, body: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val file = File(outputPath)

        val contentUri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            Uri.fromFile(file)
        }

        val openIntent = PendingIntent.getActivity(
            context,
            outputPath.hashCode(),
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, getMimeType(outputPath))
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Share intent
        val shareIntent = PendingIntent.getActivity(
            context,
            outputPath.hashCode() + 1,
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = getMimeType(outputPath)
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                },
                context.getString(R.string.share_output)
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_share, context.getString(R.string.share_output), shareIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun openMainActivity(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getMimeType(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "m4v" -> "video/mp4"
            "mkv"        -> "video/x-matroska"
            "avi"        -> "video/x-msvideo"
            "mov"        -> "video/quicktime"
            "webm"       -> "video/webm"
            "flv"        -> "video/x-flv"
            "wmv"        -> "video/x-ms-wmv"
            "3gp"        -> "video/3gpp"
            "mp3"        -> "audio/mpeg"
            "aac"        -> "audio/aac"
            "ogg"        -> "audio/ogg"
            "flac"       -> "audio/flac"
            "wav"        -> "audio/wav"
            "m4a"        -> "audio/mp4"
            "opus"       -> "audio/opus"
            "wma"        -> "audio/x-ms-wma"
            else         -> "*/*"
        }
    }
}
