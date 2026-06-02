package com.yusufkilinc.mediatrimmer.presentation.trim

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufkilinc.mediatrimmer.core.util.FileUtils
import com.yusufkilinc.mediatrimmer.data.local.datastore.AppSettingsDataStore
import com.yusufkilinc.mediatrimmer.data.local.db.entity.ProcessingHistoryEntity
import com.yusufkilinc.mediatrimmer.data.repository.MediaRepositoryImpl
import com.yusufkilinc.mediatrimmer.domain.model.*
import com.yusufkilinc.mediatrimmer.domain.repository.HistoryRepository
import com.yusufkilinc.mediatrimmer.domain.usecase.FFmpegCommandBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
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
    val processingDurationMs: Long = 0L,
    val error: String? = null
)

@HiltViewModel
class TrimViewModel @Inject constructor(
    private val mediaRepository: MediaRepositoryImpl,
    private val historyRepository: HistoryRepository,
    private val settingsDataStore: AppSettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrimUiState())
    val uiState: StateFlow<TrimUiState> = _uiState.asStateFlow()
    private var processingJob: Job? = null

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

        val actualExtension = state.outputFormat.extension

        val outputPath = FileUtils.generateOutputPath(
            context = context,
            sourceFileName = mediaFile.displayName,
            extension = actualExtension,
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

        _uiState.update { it.copy(isProcessing = true, progress = 0, error = null, outputPath = null) }

        val startTime = System.currentTimeMillis()

        processingJob = viewModelScope.launch {
            try {
                val result = mediaRepository.transformMedia(trimConfig)
                val elapsed = System.currentTimeMillis() - startTime

                // Check for custom output directory
                var finalOutputPath = result
                val customDirUri = settingsDataStore.getOutputDirUri()
                if (customDirUri != null) {
                    val safUri = FileUtils.copyToSafDirectory(context, result, customDirUri)
                    if (safUri != null) {
                        File(result).delete()
                        finalOutputPath = safUri.toString()
                    }
                }

                _uiState.update { it.copy(isProcessing = false, outputPath = finalOutputPath, processingDurationMs = elapsed) }

                // Save to history
                val outputFile = File(result)
                val outputFileSize = if (finalOutputPath.startsWith("content://")) {
                    FileUtils.getFileSizeFromUri(context, android.net.Uri.parse(finalOutputPath))
                } else if (outputFile.exists()) {
                    outputFile.length()
                } else 0L

                historyRepository.saveEntry(
                    ProcessingHistoryEntity(
                        id = UUID.randomUUID().toString(),
                        operationType = state.operation.name,
                        sourceFileName = mediaFile.displayName,
                        outputFilePath = finalOutputPath,
                        outputFormat = state.outputFormat.displayName,
                        startMs = state.startMs,
                        endMs = state.endMs,
                        processingDurationMs = elapsed,
                        outputFileSizeBytes = outputFileSize,
                        sourceFileSizeBytes = mediaFile.sizeBytes,
                        sourceDurationMs = mediaFile.durationMs,
                        outputFileName = FileUtils.getFileName(finalOutputPath),
                        status = "COMPLETED"
                    )
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isProcessing = false, error = e.message ?: "Processing failed")
                }
            }
        }
    }

    fun cancelProcessing() {
        processingJob?.cancel()
        mediaRepository.cancelCurrentJob()
        _uiState.update { it.copy(isProcessing = false) }
    }

    fun clearResult() {
        _uiState.update { it.copy(outputPath = null, error = null) }
    }

    fun availableOutputFormats(): List<MediaFormat> {
        val state = _uiState.value
        val isVideo = state.mediaFile?.isVideo ?: true
        return FFmpegCommandBuilder.availableOutputFormats(isVideo, state.operation, state.mediaFile?.format)
    }
}
