package com.yusufkilinc.mediatrimmer.presentation.trim

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.yusufkilinc.mediatrimmer.core.util.FileUtils
import com.yusufkilinc.mediatrimmer.domain.model.*
import com.yusufkilinc.mediatrimmer.domain.repository.MediaRepository
import com.yusufkilinc.mediatrimmer.domain.usecase.FFmpegCommandBuilder
import com.yusufkilinc.mediatrimmer.worker.MediaProcessingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

data class TrimUiState(
    val mediaFile: MediaFile? = null,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val operation: OperationType = OperationType.TRIM,
    val outputFormat: MediaFormat = MediaFormat.MP4,
    val isProcessing: Boolean = false,
    val progress: Int = 0,
    val outputPath: String? = null,
    val error: String? = null
)

@HiltViewModel
class TrimViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrimUiState())
    val uiState: StateFlow<TrimUiState> = _uiState.asStateFlow()

    fun loadMedia(filePath: String) {
        viewModelScope.launch {
            val durationMs = mediaRepository.probeMediaDurationMs(filePath)
            val file = File(filePath)
            val isVideo = filePath.substringAfterLast('.', "").lowercase() in
                    setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "3gp", "ts", "m4v")

            val mediaFile = MediaFile(
                uri = android.net.Uri.fromFile(file),
                resolvedPath = filePath,
                displayName = file.name,
                mimeType = if (isVideo) "video/*" else "audio/*",
                sizeBytes = file.length(),
                durationMs = durationMs.coerceAtLeast(1L),
                isVideo = isVideo
            )

            _uiState.update {
                it.copy(
                    mediaFile = mediaFile,
                    startMs = 0L,
                    endMs = durationMs.coerceAtLeast(1L),
                    outputFormat = FFmpegCommandBuilder.suggestOutputFormat(
                        mediaFile.format, OperationType.TRIM
                    )
                )
            }
        }
    }

    fun setTrimRange(startMs: Long, endMs: Long) {
        _uiState.update { it.copy(startMs = startMs, endMs = endMs) }
    }

    fun setOperation(op: OperationType) {
        val state = _uiState.value
        val newFormat = FFmpegCommandBuilder.suggestOutputFormat(state.mediaFile?.format, op)
        _uiState.update { it.copy(operation = op, outputFormat = newFormat) }
    }

    fun setOutputFormat(fmt: MediaFormat) {
        _uiState.update { it.copy(outputFormat = fmt) }
    }

    fun startProcessing() {
        val state = _uiState.value
        val mediaFile = state.mediaFile ?: return

        val outputPath = FileUtils.generateOutputPath(
            context = context,
            sourceFileName = mediaFile.displayName,
            extension = state.outputFormat.extension,
            suffix = when (state.operation) {
                OperationType.TRIM          -> "trimmed"
                OperationType.EXTRACT_AUDIO -> "audio"
                OperationType.CONVERT       -> "converted"
            }
        )

        val trimConfig = TrimConfig(
            sourceFilePath   = mediaFile.resolvedPath,
            sourceFileName   = mediaFile.displayName,
            sourceDurationMs = mediaFile.durationMs,
            startMs          = state.startMs,
            endMs            = state.endMs,
            outputFormat     = state.outputFormat.name,
            outputPath       = outputPath,
            operation        = state.operation
        )

        val job = ProcessingJob(configs = listOf(trimConfig))

        val workData = workDataOf(
            MediaProcessingWorker.KEY_JOB_JSON to Json.encodeToString(ProcessingJob.serializer(), job)
        )

        val request = OneTimeWorkRequestBuilder<MediaProcessingWorker>()
            .setInputData(workData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("trim_${job.id}")
            .build()

        workManager.enqueueUniqueWork(job.id, ExistingWorkPolicy.KEEP, request)
        _uiState.update { it.copy(isProcessing = true, progress = 0, error = null, outputPath = null) }

        // Observe work progress
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("trim_${job.id}").collect { infos ->
                val info = infos.firstOrNull() ?: return@collect
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        val pct = info.progress.getInt(MediaProcessingWorker.KEY_PROGRESS, 0)
                        _uiState.update { it.copy(progress = pct) }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val resultPath = info.outputData.getString(MediaProcessingWorker.KEY_RESULT_PATH)
                        _uiState.update { it.copy(isProcessing = false, outputPath = resultPath) }
                    }
                    WorkInfo.State.FAILED -> {
                        val err = info.outputData.getString(MediaProcessingWorker.KEY_ERROR)
                        _uiState.update { it.copy(isProcessing = false, error = err ?: "Unknown error") }
                    }
                    WorkInfo.State.CANCELLED -> {
                        _uiState.update { it.copy(isProcessing = false) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun cancelProcessing() {
        workManager.cancelAllWorkByTag("trim_${_uiState.value.mediaFile?.displayName}")
        mediaRepository.cancelCurrentJob()
        _uiState.update { it.copy(isProcessing = false) }
    }

    fun clearResult() {
        _uiState.update { it.copy(outputPath = null, error = null) }
    }

    fun availableOutputFormats(): List<MediaFormat> {
        val state = _uiState.value
        val isVideo = state.mediaFile?.isVideo ?: true
        return FFmpegCommandBuilder.availableOutputFormats(isVideo, state.operation)
    }
}
