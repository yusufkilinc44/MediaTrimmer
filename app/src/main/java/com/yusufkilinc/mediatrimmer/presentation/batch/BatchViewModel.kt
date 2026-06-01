package com.yusufkilinc.mediatrimmer.presentation.batch

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.yusufkilinc.mediatrimmer.core.util.FileUtils
import com.yusufkilinc.mediatrimmer.domain.model.*
import com.yusufkilinc.mediatrimmer.domain.repository.MediaRepository
import com.yusufkilinc.mediatrimmer.worker.MediaProcessingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

data class BatchItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val filePath: String,
    val fileName: String,
    val durationMs: Long = 0L
)

data class BatchUiState(
    val items: List<BatchItem> = emptyList(),
    val operation: OperationType = OperationType.TRIM,
    val outputFormat: MediaFormat = MediaFormat.MP4,
    val isProcessing: Boolean = false,
    val progress: Int = 0,
    val currentFileIndex: Int = 0,
    val completedPaths: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class BatchViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatchUiState())
    val uiState: StateFlow<BatchUiState> = _uiState.asStateFlow()

    fun addFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val items = uris.mapNotNull { uri ->
                val path = try { FileUtils.resolveUriToPath(context, uri) } catch (_: Exception) { null }
                    ?: return@mapNotNull null
                val dur = mediaRepository.probeMediaDurationMs(path)
                BatchItem(filePath = path, fileName = File(path).name, durationMs = dur)
            }
            _uiState.update { it.copy(items = it.items + items) }
        }
    }

    fun addFilePath(path: String) {
        viewModelScope.launch {
            val dur = mediaRepository.probeMediaDurationMs(path)
            val item = BatchItem(filePath = path, fileName = File(path).name, durationMs = dur)
            _uiState.update { it.copy(items = it.items + item) }
        }
    }

    fun removeItem(id: String) {
        _uiState.update { it.copy(items = it.items.filter { item -> item.id != id }) }
    }

    fun setOperation(op: OperationType) {
        val suggestedFormat = when (op) {
            OperationType.EXTRACT_AUDIO -> MediaFormat.MP3
            else -> _uiState.value.outputFormat
        }
        _uiState.update { it.copy(operation = op, outputFormat = suggestedFormat) }
    }

    fun setOutputFormat(fmt: MediaFormat) {
        _uiState.update { it.copy(outputFormat = fmt) }
    }

    fun startBatchProcessing() {
        val state = _uiState.value
        if (state.items.isEmpty()) return

        val configs = state.items.map { item ->
            val outputPath = FileUtils.generateOutputPath(
                context, item.fileName, state.outputFormat.extension,
                suffix = state.operation.name.lowercase()
            )
            TrimConfig(
                sourceFilePath   = item.filePath,
                sourceFileName   = item.fileName,
                sourceDurationMs = item.durationMs,
                startMs          = 0L,
                endMs            = item.durationMs,
                outputFormat     = state.outputFormat.name,
                outputPath       = outputPath,
                operation        = state.operation
            )
        }

        val job = ProcessingJob(configs = configs, isBatch = true)
        val workData = workDataOf(
            MediaProcessingWorker.KEY_JOB_JSON to Json.encodeToString(ProcessingJob.serializer(), job)
        )

        val request = OneTimeWorkRequestBuilder<MediaProcessingWorker>()
            .setInputData(workData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("batch_${job.id}")
            .build()

        workManager.enqueueUniqueWork(job.id, ExistingWorkPolicy.KEEP, request)
        _uiState.update { it.copy(isProcessing = true, progress = 0, error = null) }

        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("batch_${job.id}").collect { infos ->
                val info = infos.firstOrNull() ?: return@collect
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        val pct = info.progress.getInt(MediaProcessingWorker.KEY_PROGRESS, 0)
                        _uiState.update { it.copy(progress = pct) }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        _uiState.update { it.copy(isProcessing = false, progress = 100) }
                    }
                    WorkInfo.State.FAILED -> {
                        val err = info.outputData.getString(MediaProcessingWorker.KEY_ERROR)
                        _uiState.update { it.copy(isProcessing = false, error = err) }
                    }
                    else -> {}
                }
            }
        }
    }
}
