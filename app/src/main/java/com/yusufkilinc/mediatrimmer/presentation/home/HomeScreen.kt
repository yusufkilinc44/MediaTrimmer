package com.yusufkilinc.mediatrimmer.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufkilinc.mediatrimmer.R
import com.yusufkilinc.mediatrimmer.core.util.FileUtils
import com.yusufkilinc.mediatrimmer.core.util.TimeUtils
import com.yusufkilinc.mediatrimmer.data.local.db.entity.ProcessingHistoryEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    incomingUri: Uri? = null,
    onFileReady: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // File picker
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val path = viewModel.resolveFileUri(it)
            if (path.isNotEmpty()) onFileReady(path)
        }
    }

    // Handle incoming URI from "Open with"
    LaunchedEffect(incomingUri) {
        incomingUri?.let {
            val path = viewModel.resolveFileUri(it)
            if (path.isNotEmpty()) onFileReady(path)
        }
    }

    // Navigate to TrimScreen when download finishes
    LaunchedEffect(state.downloadedFilePath) {
        state.downloadedFilePath?.let { path ->
            onFileReady(path)
            viewModel.clearDownloadedPath()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePicker.launch(arrayOf("video/*", "audio/*")) },
                icon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                text = { Text(stringResource(R.string.home_pick_file)) },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── URL Input card ──────────────────────────────────────────────
            item {
                UrlInputCard(
                    urlInput = state.urlInput,
                    isDownloading = state.isDownloading,
                    downloadProgress = state.downloadProgress,
                    downloadError = state.downloadError,
                    onUrlChange = { viewModel.setUrlInput(it) },
                    onDownload = { viewModel.startDownload(state.urlInput) }
                )
            }

            // ── Recent history ──────────────────────────────────────────────
            if (state.recentHistory.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.home_recent),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(state.recentHistory, key = { it.id }) { entry ->
                    RecentHistoryCard(entry = entry, onOpen = {
                        if (File(entry.outputFilePath).exists()) {
                            onFileReady(entry.outputFilePath)
                        }
                    })
                }
            }

            // ── Empty state hint ────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.VideoFile,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.home_open_file),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_supported_formats),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Spacer for FAB
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun UrlInputCard(
    urlInput: String,
    isDownloading: Boolean,
    downloadProgress: Int,
    downloadError: String?,
    onUrlChange: (String) -> Unit,
    onDownload: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.home_enter_url),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                placeholder = { Text(stringResource(R.string.home_url_hint),
                    style = MaterialTheme.typography.bodySmall) },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = if (urlInput.isNotEmpty()) {{
                    IconButton(onClick = { onUrlChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }} else null,
                isError = downloadError != null
            )
            downloadError?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(visible = isDownloading) {
                Column {
                    LinearProgressIndicator(
                        progress = { if (downloadProgress > 0) downloadProgress / 100f else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${stringResource(R.string.downloading)} ${if (downloadProgress > 0) "$downloadProgress%" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isDownloading) {
                Button(
                    onClick = onDownload,
                    enabled = urlInput.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.home_download_open))
                }
            }
        }
    }
}

@Composable
private fun RecentHistoryCard(
    entry: ProcessingHistoryEntity,
    onOpen: () -> Unit
) {
    val fileExists = remember(entry.outputFilePath) { File(entry.outputFilePath).exists() }

    ListItem(
        headlineContent = {
            Text(
                FileUtils.getFileName(entry.outputFilePath.ifEmpty { entry.sourceFileName }),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(entry.createdAt)) +
                " · " + entry.outputFormat,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = if (fileExists) Icons.Default.CheckCircle else Icons.Default.BrokenImage,
                contentDescription = null,
                tint = if (fileExists) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error
            )
        },
        trailingContent = if (fileExists) {{
            IconButton(onClick = onOpen) {
                Icon(Icons.Default.OpenInNew, contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }} else null,
        modifier = Modifier
            .clickable(enabled = fileExists, onClick = onOpen)
            .fillMaxWidth()
    )
}
