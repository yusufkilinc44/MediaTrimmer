package com.yusufkilinc.mediatrimmer.presentation.batch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufkilinc.mediatrimmer.R
import com.yusufkilinc.mediatrimmer.domain.model.MediaFormat
import com.yusufkilinc.mediatrimmer.domain.model.OperationType
import com.yusufkilinc.mediatrimmer.presentation.trim.components.FormatSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(viewModel: BatchViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) viewModel.addFiles(uris) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.batch_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFB2DFDB)
                )
            )
        },
        floatingActionButton = {
            if (!state.isProcessing) {
                FloatingActionButton(onClick = {
                    filePicker.launch(arrayOf("video/*", "audio/*"))
                }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.batch_add_files))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            if (state.items.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LibraryAdd, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.batch_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            } else {
                // File count
                Text(
                    stringResource(R.string.batch_files_selected, state.items.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Operation selector
                val hasVideo = state.items.any { it.isVideo }
                val hasAudio = state.items.any { !it.isVideo }
                val sourceIsAllAudio = hasAudio && !hasVideo
                val sourceIsAllVideo = hasVideo && !hasAudio

                val ops = if (sourceIsAllAudio) {
                    listOf(OperationType.CONVERT)
                } else {
                    listOf(OperationType.EXTRACT_AUDIO, OperationType.CONVERT)
                }

                if (ops.size > 1) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ops.forEachIndexed { index, op ->
                            SegmentedButton(
                                selected = state.operation == op,
                                onClick = { viewModel.setOperation(op) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ops.size),
                                label = {
                                    Text(when (op) {
                                        OperationType.TRIM          -> stringResource(R.string.trim_op_trim)
                                        OperationType.EXTRACT_AUDIO -> stringResource(R.string.trim_op_extract_audio)
                                        OperationType.CONVERT       -> stringResource(R.string.trim_op_convert)
                                    }, maxLines = 1)
                                }
                            )
                        }
                    }
                }

                // Format selector — video source → video formats, audio → audio formats
                val formats = if (state.operation == OperationType.EXTRACT_AUDIO) {
                    MediaFormat.audioFormats
                } else if (sourceIsAllVideo) {
                    MediaFormat.videoFormats
                } else if (sourceIsAllAudio) {
                    MediaFormat.audioFormats
                } else {
                    MediaFormat.videoFormats + MediaFormat.audioFormats
                }
                FormatSelector(
                    label = stringResource(R.string.batch_output_format),
                    formats = formats,
                    selected = state.outputFormat,
                    onSelect = { viewModel.setOutputFormat(it) }
                )

                // File list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = {
                                Text(item.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            leadingContent = {
                                Icon(Icons.Default.AudioFile, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = {
                                if (!state.isProcessing) {
                                    IconButton(onClick = { viewModel.removeItem(item.id) }) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.batch_remove),
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Progress or Process button
                if (state.isProcessing) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.batch_progress,
                                    (state.progress * state.items.size / 100).coerceAtMost(state.items.size),
                                    state.items.size),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.startBatchProcessing() },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.batch_process_all))
                    }
                }

                state.error?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
