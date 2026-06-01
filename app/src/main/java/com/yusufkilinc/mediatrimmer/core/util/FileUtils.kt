package com.yusufkilinc.mediatrimmer.core.util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File

object FileUtils {

    /**
     * Resolves a content:// URI to an absolute file path for FFmpegKit.
     * Tries MediaStore DATA column first; falls back to copying to cache.
     */
    fun resolveUriToPath(context: Context, uri: Uri): String {
        // Attempt 1: Try MediaStore DATA column (works for most local files)
        try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val path = cursor.getString(0)
                    if (!path.isNullOrBlank() && File(path).exists()) {
                        return path
                    }
                }
            }
        } catch (_: Exception) {}

        // Attempt 2: Copy to app cache directory
        val ext = getExtensionFromUri(context, uri) ?: "tmp"
        val dest = File(context.cacheDir, "input_${System.currentTimeMillis()}.$ext")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Cannot resolve URI to file path: ${e.message}", e)
        }
        return dest.absolutePath
    }

    fun getExtensionFromUri(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri) ?: return null
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }

    fun getOutputDirectory(context: Context): File {
        val dir = context.getExternalFilesDir("MediaTrimmer")
            ?: File(context.filesDir, "MediaTrimmer")
        dir.mkdirs()
        return dir
    }

    fun generateOutputPath(
        context: Context,
        sourceFileName: String,
        extension: String,
        suffix: String = "trimmed"
    ): String {
        val baseName = sourceFileName.substringBeforeLast(".", sourceFileName)
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(40)
        val timestamp = System.currentTimeMillis()
        val outputDir = getOutputDirectory(context)
        return File(outputDir, "${baseName}_${suffix}_$timestamp.$extension").absolutePath
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    fun getFileName(path: String): String = File(path).name

    fun deleteFile(path: String): Boolean = try {
        File(path).delete()
    } catch (_: Exception) { false }
}
