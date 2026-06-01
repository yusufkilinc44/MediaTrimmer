package com.yusufkilinc.mediatrimmer.presentation.trim

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufkilinc.mediatrimmer.R
import com.yusufkilinc.mediatrimmer.core.util.FileUtils
import com.yusufkilinc.mediatrimmer.core.util.ShareUtils
import com.yusufkilinc.mediatrimmer.core.util.TimeUtils
import com.yusufkilinc.mediatrimmer.domain.model.OperationType
import com.yusufkilinc.mediatrimmer.presentation.trim.components.FormatSelector
import com.yusufkilinc.mediatrimmer.presentation.trim.components.TrimRangeSlider
import com.yusufkilinc.mediatrimmer.presentation.trim.components.VideoPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimScreen(
    filePath: String,
    onBack: () -> Unit,
    viewModel: TrimViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var playerPositionMs by remember { mutableLongStateOf(0L) }

    // Load media when screen opens
    LaunchedEffect(filePath) {
        viewModel.loadMedia(filePath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.mediaFile?.displayName ?: stringResource(R.string.trim_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val mediaFile = state.mediaFile

        if (mediaFile == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Media Preview ────────────────────────────────────────────────
            VideoPreview(
                filePath = mediaFile.resolvedPath,
                startMs = state.startMs,
                endMs = state.endMs,
                isVideo = mediaFile.isVideo,
                onPositionChanged = { playerPositionMs = it }
            )

            // ── File info chip row ────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(mediaFile.format?.displayName ?: "?") }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(TimeUtils.formatDuration(mediaFile.durationMs)) }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(FileUtils.formatFileSize(mediaFile.sizeBytes)) }
                )
            }

            // ── Trim range slider ─────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Range",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    TrimRangeSlider(
                        durationMs = mediaFile.durationMs,
                        startMs = state.startMs,
                        endMs = state.endMs,
                        currentPositionMs = playerPositionMs,
                        onRangeChange = { start, end -> viewModel.setTrimRange(start, end) }
                    )
                }
            }

            // ── Operation selector ────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.trim_operation),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val ops = if (mediaFile.isVideo) {
                            listOf(OperationType.TRIM, OperationType.EXTRACT_AUDIO, OperationType.CONVERT)
                        } else {
                            listOf(OperationType.TRIM, OperationType.CONVERT)
                        }
                        ops.forEachIndexed { index, op ->
                            SegmentedButton(
                                selected = state.operation == op,
                                onClick = { viewModel.setOperation(op) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ops.size),
                                label = {
                                    Text(
                                        text = when (op) {
                                            OperationType.TRIM          -> stringResource(R.string.trim_op_trim)
                                            OperationType.EXTRACT_AUDIO -> stringResource(R.string.trim_op_extract_audio)
                                            OperationType.CONVERT       -> stringResource(R.string.trim_op_convert)
                                        },
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Output format selector
                    FormatSelector(
                        label = stringResource(R.string.trim_output_format),
                        formats = viewModel.availableOutputFormats(),
                        selected = state.outputFormat,
                        onSelect = { viewModel.setOutputFormat(it) }
                    )
                }
            }

            // ── Process / Result section ──────────────────────────────────────
            AnimatedVisibility(visible = state.outputPath != null) {
                val outputPath = state.outputPath ?: ""
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.trim_complete),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = FileUtils.getFileName(outputPath),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Share button
                            Button(
                                onClick = {
                                    context.startActivity(
                                        ShareUtils.createShareIntent(context, outputPath)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.trim_share))
                            }
                            OutlinedButton(onClick = { viewModel.clearResult() }) {
                                Text(stringResource(R.string.btn_close))
                            }
                        }
                    }
                }
            }

            state.error?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error, null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // ── Process button ────────────────────────────────────────────────
            if (!state.isProcessing) {
                Button(
                    onClick = { viewModel.startProcessing() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = state.outputPath == null
                ) {
                    Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (state.operation) {
                            OperationType.TRIM          -> stringResource(R.string.trim_op_trim)
                            OperationType.EXTRACT_AUDIO -> stringResource(R.string.trim_op_extract_audio)
                            OperationType.CONVERT       -> stringResource(R.string.trim_op_convert)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "${stringResource(R.string.trim_processing)} ${state.progress}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.cancelProcessing() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                stringResource(R.string.trim_cancel),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
