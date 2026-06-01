package com.yusufkilinc.mediatrimmer.data.repository

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.yusufkilinc.mediatrimmer.domain.model.ProcessingState
import com.yusufkilinc.mediatrimmer.domain.repository.MediaRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor() : MediaRepository {

    override fun executeFFmpegCommand(
        command: String,
        totalDurationMs: Long
    ): Flow<ProcessingState> = callbackFlow {

        FFmpegKit.executeAsync(
            command,
            // ── Completion callback ───────────────────────────────────────
            { session ->
                val returnCode = session.returnCode
                if (ReturnCode.isSuccess(returnCode)) {
                    // Extract output path from the command (last quoted segment)
                    val outputPath = extractOutputPath(command)
                    trySend(ProcessingState.Complete(outputPath))
                } else if (ReturnCode.isCancel(returnCode)) {
                    trySend(ProcessingState.Cancelled)
                } else {
                    val logs = session.allLogsAsString ?: "Unknown error"
                    trySend(ProcessingState.Error("FFmpeg failed (rc=${returnCode})", logs))
                }
                close()
            },
            // ── Log callback (unused — statistics preferred) ──────────────
            null,
            // ── Statistics callback — most reliable progress source ────────
            { statistics ->
                if (totalDurationMs > 0 && statistics != null) {
                    val processed = statistics.time.toLong()   // ms processed so far
                    val percent = ((processed.toFloat() / totalDurationMs) * 100)
                        .toInt().coerceIn(0, 99)
                    trySend(ProcessingState.Progress(percent))
                }
            }
        )

        awaitClose {
            // Cancel FFmpeg when the Flow collector is cancelled
            FFmpegKit.cancel()
        }
    }

    override suspend fun probeMediaDurationMs(path: String): Long {
        return try {
            val session = FFprobeKit.getMediaInformation(path)
            val info = session.mediaInformation ?: return 0L
            (info.duration.toDouble() * 1000).toLong()
        } catch (_: Exception) {
            0L
        }
    }

    override fun cancelCurrentJob() {
        FFmpegKit.cancel()
    }

    private fun extractOutputPath(command: String): String {
        // The output path is always the last quoted argument in the command
        val matches = Regex("\"([^\"]+)\"").findAll(command).toList()
        return matches.lastOrNull()?.groupValues?.get(1) ?: ""
    }
}
