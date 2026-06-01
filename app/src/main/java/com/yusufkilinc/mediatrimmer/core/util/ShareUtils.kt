package com.yusufkilinc.mediatrimmer.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {

    /**
     * Creates an Android share intent for the given output file.
     * Uses FileProvider for proper URI handling on Android 7+.
     */
    fun createShareIntent(context: Context, filePath: String): Intent {
        val file = File(filePath)
        val contentUri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            Uri.fromFile(file)
        }

        val mimeType = getMimeType(filePath)
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            },
            null  // System will show default "Share via" title
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
