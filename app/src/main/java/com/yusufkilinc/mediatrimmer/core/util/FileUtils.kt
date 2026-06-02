package com.yusufkilinc.mediatrimmer.core.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import java.io.File

object FileUtils {

    /**
     * Resolves a content:// URI to an absolute file path for processing.
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

        // Get the original display name from content resolver
        var displayName: String? = null
        try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.takeIf { it.isNotBlank() }?.let {
                        displayName = it
                    }
                }
            }
        } catch (_: Exception) {}

        // Attempt 2: Copy to app cache directory (preserve original name)
        val ext = getExtensionFromUri(context, uri) ?: "tmp"
        val fileName = displayName ?: "media_${System.currentTimeMillis()}.$ext"
        var dest = File(context.cacheDir, fileName)
        if (dest.exists()) {
            val base = fileName.substringBeforeLast(".")
            val fileExt = fileName.substringAfterLast(".", ext)
            dest = File(context.cacheDir, "${base}_${System.currentTimeMillis()}.$fileExt")
        }
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
            .replace(Regex("[/\\\\:*?\"<>|]"), "_")
            .trim()
            .take(100)
        val outputDir = getOutputDirectory(context)

        // Try without counter first
        var candidate = File(outputDir, "${baseName}_${suffix}.$extension")
        if (!candidate.exists()) return candidate.absolutePath

        // Add counter (01), (02), etc.
        var counter = 1
        while (counter <= 99) {
            val counterStr = String.format("%02d", counter)
            candidate = File(outputDir, "${baseName}_${suffix}($counterStr).$extension")
            if (!candidate.exists()) return candidate.absolutePath
            counter++
        }

        // Fallback with timestamp
        return File(outputDir, "${baseName}_${suffix}_${System.currentTimeMillis()}.$extension").absolutePath
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    fun getFileName(path: String): String {
        if (path.startsWith("content://")) {
            // For content URIs, extract from the last path segment
            val uri = Uri.parse(path)
            return uri.lastPathSegment?.substringAfterLast("/") ?: "output"
        }
        return File(path).name
    }

    fun deleteFile(path: String): Boolean = try {
        File(path).delete()
    } catch (_: Exception) { false }

    /**
     * Returns a user-friendly display path from a full path or content URI.
     * e.g., "/storage/emulated/0/Download/file.mp4" → "Download/file.mp4"
     */
    fun friendlyPath(path: String): String {
        if (path.startsWith("content://")) {
            try {
                val uri = Uri.parse(path)
                val docId = DocumentsContract.getDocumentId(uri)
                val parts = docId.split(":")
                if (parts.size == 2) return parts[1]
            } catch (_: Exception) {}
            return path
        }
        val prefix = "/storage/emulated/0/"
        if (path.startsWith(prefix)) {
            return path.removePrefix(prefix)
        }
        // For app-specific storage, show relative to "files/" folder
        val filesMarker = "/files/"
        val idx = path.lastIndexOf(filesMarker)
        if (idx >= 0) {
            return path.substring(idx + filesMarker.length)
        }
        return path
    }

    /**
     * Copies a file to a SAF tree directory and returns the content URI of the new file.
     */
    fun copyToSafDirectory(context: Context, sourceFilePath: String, treeUriStr: String): Uri? {
        return try {
            val treeUri = Uri.parse(treeUriStr)
            val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val sourceFile = File(sourceFilePath)
            val ext = sourceFile.extension
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"

            val docFile = tree.createFile(mime, sourceFile.nameWithoutExtension) ?: return null

            context.contentResolver.openOutputStream(docFile.uri)?.use { out ->
                sourceFile.inputStream().use { inp -> inp.copyTo(out) }
            }

            docFile.uri
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Gets file size from a content URI.
     */
    fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(
                uri, arrayOf(DocumentsContract.Document.COLUMN_SIZE), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            } ?: 0L
        } catch (_: Exception) { 0L }
    }

    /**
     * Gets display name from a content URI.
     */
    fun getDisplayNameFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

    /**
     * Gets friendly directory name from a SAF tree URI.
     */
    fun friendlyDirFromTreeUri(treeUriStr: String): String {
        return try {
            val uri = Uri.parse(treeUriStr)
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val parts = docId.split(":")
            if (parts.size == 2 && parts[1].isNotBlank()) parts[1]
            else docId
        } catch (_: Exception) {
            treeUriStr
        }
    }
}
