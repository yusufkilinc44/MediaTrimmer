package com.yusufkilinc.mediatrimmer.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject

class FileDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    data class DownloadProgress(val downloaded: Long, val total: Long) {
        val percent: Int get() = if (total > 0) ((downloaded.toDouble() / total) * 100).toInt() else -1
    }

    /**
     * Downloads a file from [url] into the app's downloads directory.
     * [onProgress] is called with (downloadedBytes, totalBytes).
     */
    suspend fun downloadFile(
        url: String,
        onProgress: (DownloadProgress) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }

            // Determine filename from URL or Content-Disposition header
            val contentDisposition = response.header("Content-Disposition")
            val urlFileName = url.substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() }
            val fileName = parseFileName(contentDisposition) ?: urlFileName
                ?: "download_${System.currentTimeMillis()}"

            val destDir = context.getExternalFilesDir("downloads")
                ?: File(context.filesDir, "downloads")
            destDir.mkdirs()

            val destFile = File(destDir, sanitizeFileName(fileName))
            val body = response.body ?: return@withContext Result.failure(IOException("Empty response body"))
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(DownloadProgress(downloaded, totalBytes))
                    }
                }
            }

            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFileName(contentDisposition: String?): String? {
        if (contentDisposition == null) return null
        val match = Regex("filename\\*?=['\"]?(?:UTF-8'')?([^'\"\\n]+)['\"]?", RegexOption.IGNORE_CASE)
            .find(contentDisposition)
        return match?.groupValues?.getOrNull(1)?.trim()
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9._\\-]"), "_").take(120)
}
