package com.yusufkilinc.mediatrimmer.presentation.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.yusufkilinc.mediatrimmer.core.util.FileUtils
import com.yusufkilinc.mediatrimmer.data.local.db.entity.ProcessingHistoryEntity
import com.yusufkilinc.mediatrimmer.domain.repository.HistoryRepository
import com.yusufkilinc.mediatrimmer.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentHistory: List<ProcessingHistoryEntity> = emptyList(),
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadedFilePath: String? = null,
    val downloadError: String? = null,
    val urlInput: String = "",
    val showUrlSheet: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.getRecentHistory(limit = 5).collect { history ->
                _uiState.update { it.copy(recentHistory = history) }
            }
        }
    }

    fun setUrlInput(url: String) {
        _uiState.update { it.copy(urlInput = url, downloadError = null) }
    }

    fun setShowUrlSheet(show: Boolean) {
        _uiState.update { it.copy(showUrlSheet = show) }
    }

    fun resolveFileUri(uri: Uri): String {
        return try {
            FileUtils.resolveUriToPath(context, uri)
        } catch (e: Exception) {
            ""
        }
    }

    fun startDownload(url: String) {
        if (url.isBlank()) {
            _uiState.update { it.copy(downloadError = "Please enter a valid URL") }
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _uiState.update { it.copy(downloadError = "URL must start with http:// or https://") }
            return
        }

        val workData = workDataOf(DownloadWorker.KEY_URL to url)
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("download_url")
            .build()

        workManager.enqueueUniqueWork("url_download", ExistingWorkPolicy.REPLACE, request)
        _uiState.update { it.copy(isDownloading = true, downloadProgress = 0, downloadError = null) }

        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("download_url").collect { infos ->
                val info = infos.firstOrNull() ?: return@collect
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        val pct = info.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                        _uiState.update { it.copy(downloadProgress = pct) }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val path = info.outputData.getString(DownloadWorker.KEY_LOCAL_PATH)
                        _uiState.update {
                            it.copy(
                                isDownloading = false,
                                downloadedFilePath = path,
                                showUrlSheet = false
                            )
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        val err = info.outputData.getString(DownloadWorker.KEY_ERROR)
                        _uiState.update {
                            it.copy(isDownloading = false, downloadError = err ?: "Download failed")
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearDownloadedPath() {
        _uiState.update { it.copy(downloadedFilePath = null) }
    }
}
