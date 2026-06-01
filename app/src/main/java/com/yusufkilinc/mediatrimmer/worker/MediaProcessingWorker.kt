package com.yusufkilinc.mediatrimmer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.yusufkilinc.mediatrimmer.core.util.FileUtils
import com.yusufkilinc.mediatrimmer.core.util.NotificationUtils
import com.yusufkilinc.mediatrimmer.data.local.db.entity.ProcessingHistoryEntity
import com.yusufkilinc.mediatrimmer.domain.model.ProcessingJob
import com.yusufkilinc.mediatrimmer.domain.model.ProcessingState
import com.yusufkilinc.mediatrimmer.domain.repository.HistoryRepository
import com.yusufkilinc.mediatrimmer.domain.repository.MediaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json

@HiltWorker
class MediaProcessingWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val mediaRepository: MediaRepository,
    private val historyRepository: HistoryRepository,
    private val notificationUtils: NotificationUtils
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_JOB_JSON    = "job_json"
        const val KEY_RESULT_PATH = "result_path"
        const val KEY_ERROR       = "error"
        const val KEY_PROGRESS    = "progress"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val jobJson = inputData.getString(KEY_JOB_JSON)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing job data"))

        val job = try {
            Json.decodeFromString<ProcessingJob>(jobJson)
        } catch (e: Exception) {
            return Result.failure(workDataOf(KEY_ERROR to "Invalid job JSON: ${e.message}"))
        }

        setForeground(createForegroundInfo(0, "Processing…"))

        var lastOutputPath = ""
        var errorMessage: String? = null
        val startTime = System.currentTimeMillis()

        return try {
            job.configs.forEachIndexed { index, config ->
                if (errorMessage != null) return@forEachIndexed

                val overallOffset = index.toFloat() / job.configs.size

                mediaRepository.executeTransformation(config)
                    .collect { state ->
                        when (state) {
                            is ProcessingState.Progress -> {
                                val overall = (overallOffset * 100 +
                                    state.percent.toFloat() / job.configs.size).toInt()
                                setForeground(createForegroundInfo(overall,
                                    if (job.configs.size > 1) "File ${index + 1}/${job.configs.size}" else "Processing…"))
                                setProgress(workDataOf(KEY_PROGRESS to overall))
                            }

                            is ProcessingState.Complete -> {
                                lastOutputPath = state.outputPath
                                val elapsed = System.currentTimeMillis() - startTime
                                historyRepository.saveEntry(
                                    ProcessingHistoryEntity(
                                        id = config.sourceFilePath + "_" + config.startMs,
                                        operationType = config.operation.name,
                                        sourceFileName = config.sourceFileName,
                                        outputFilePath = state.outputPath,
                                        outputFormat = config.outputFormat,
                                        startMs = config.startMs,
                                        endMs = config.endMs,
                                        processingDurationMs = elapsed,
                                        outputFileSizeBytes = java.io.File(state.outputPath).length(),
                                        status = "COMPLETED"
                                    )
                                )
                            }

                            is ProcessingState.Error -> {
                                errorMessage = state.message
                                historyRepository.saveEntry(
                                    ProcessingHistoryEntity(
                                        id = config.sourceFilePath + "_" + config.startMs + "_err",
                                        operationType = config.operation.name,
                                        sourceFileName = config.sourceFileName,
                                        outputFilePath = "",
                                        outputFormat = config.outputFormat,
                                        startMs = config.startMs,
                                        endMs = config.endMs,
                                        status = "FAILED",
                                        errorMessage = state.message
                                    )
                                )
                            }

                            else -> {}
                        }
                    }
            }

            if (errorMessage != null) {
                return Result.failure(workDataOf(KEY_ERROR to errorMessage))
            }

            if (lastOutputPath.isNotEmpty()) {
                val fileName = FileUtils.getFileName(lastOutputPath)
                val title = if (job.configs.size > 1) "Batch complete" else "Processing complete"
                notificationUtils.showCompletionNotification(
                    outputPath = lastOutputPath,
                    title = title,
                    body = fileName
                )
            }

            Result.success(workDataOf(KEY_RESULT_PATH to lastOutputPath))
        } catch (_: CancellationException) {
            mediaRepository.cancelCurrentJob()
            Result.failure(workDataOf(KEY_ERROR to "Cancelled"))
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Processing failed")))
        }
    }

    private fun createForegroundInfo(progress: Int, subtitle: String): ForegroundInfo {
        val notification = notificationUtils.buildProgressNotification(
            channelId     = NotificationUtils.CHANNEL_PROCESSING,
            title         = "MediaTrimmer",
            progressText  = "$subtitle — $progress%",
            progress      = progress
        )
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
