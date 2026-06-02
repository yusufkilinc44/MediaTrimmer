package com.yusufkilinc.mediatrimmer.presentation.trim

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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
import java.io.File

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
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        // ══════════════════════════════════════════════════════════════════
        //  RESULT SCREEN — shown when processing is done
        // ══════════════════════════════════════════════════════════════════
        if (state.outputPath != null) {
            val outputPath = state.outputPath!!
            val outputFile = File(outputPath)
            val inputFile = File(mediaFile.resolvedPath)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Success badge
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CheckCircle, null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.trim_complete),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Processing time badge
                AssistChip(
                    onClick = {},
                    label = {
                        val secs = state.processingDurationMs / 1000.0
                        Text(String.format("%.1f s", secs))
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Speed, null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        leadingIconContentColor = MaterialTheme.colorScheme.tertiary
                    )
                )

                // ── INPUT file info ──────────────────────────────────────
                FileInfoCard(
                    title = "Input",
                    icon = Icons.Default.FileOpen,
                    cardColor = MaterialTheme.colorScheme.surfaceContainer,
                    onCardColor = MaterialTheme.colorScheme.onSurface,
                    fileName = mediaFile.displayName,
                    filePath = mediaFile.resolvedPath,
                    fileType = mediaFile.format?.displayName ?: "?",
                    fileSize = FileUtils.formatFileSize(inputFile.length()),
                    duration = TimeUtils.formatDuration(mediaFile.durationMs)
                )

                // ── OUTPUT file info ─────────────────────────────────────
                FileInfoCard(
                    title = "Output",
                    icon = Icons.Default.SaveAlt,
                    cardColor = MaterialTheme.colorScheme.secondaryContainer,
                    onCardColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    fileName = FileUtils.getFileName(outputPath),
                    filePath = outputPath,
                    fileType = state.outputFormat.displayName,
                    fileSize = if (outputFile.exists()) FileUtils.formatFileSize(outputFile.length()) else "—",
                    duration = if (state.operation == OperationType.CONVERT) {
                        TimeUtils.formatDuration(mediaFile.durationMs)
                    } else {
                        TimeUtils.formatDuration(state.endMs - state.startMs)
                    }
                )

                Spacer(Modifier.height(4.dp))

                // Action buttons — 2 rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val file = File(outputPath)
                                val contentUri: Uri = try {
                                    FileProvider.getUriForFile(
                                        context, "${context.packageName}.fileprovider", file
                                    )
                                } catch (_: Exception) { Uri.fromFile(file) }
                                val ext = outputPath.substringAfterLast('.', "").lowercase()
                                val mime = when (ext) {
                                    "mp4", "m4v" -> "video/mp4"
                                    "webm" -> "video/webm"
                                    "m4a" -> "audio/mp4"
                                    "mp3" -> "audio/mpeg"
                                    "ogg" -> "audio/ogg"
                                    "wav" -> "audio/wav"
                                    "flac" -> "audio/flac"
                                    "aac" -> "audio/aac"
                                    "3gp" -> "video/3gpp"
                                    else -> "*/*"
                                }
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(contentUri, mime)
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                )
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.trim_play))
                    }

                    FilledTonalButton(
                        onClick = {
                            context.startActivity(
                                ShareUtils.createShareIntent(context, outputPath)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.trim_share))
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.clearResult() },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_close))
                }
            }
            return@Scaffold
        }

        // ══════════════════════════════════════════════════════════════════
        //  EDITOR UI
        // ══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VideoPreview(
                filePath = mediaFile.resolvedPath,
                startMs = state.startMs,
                endMs = state.endMs,
                isVideo = mediaFile.isVideo,
                onPositionChanged = { playerPositionMs = it }
            )

            // File info chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(mediaFile.format?.displayName ?: "?") },
                    leadingIcon = {
                        Icon(
                            if (mediaFile.isVideo) Icons.Default.VideoFile else Icons.Default.AudioFile,
                            null, modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        leadingIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
                AssistChip(
                    onClick = {},
                    label = { Text(TimeUtils.formatDuration(mediaFile.durationMs)) },
                    leadingIcon = {
                        Icon(Icons.Default.Timer, null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        leadingIconContentColor = MaterialTheme.colorScheme.tertiary
                    )
                )
                AssistChip(
                    onClick = {},
                    label = { Text(FileUtils.formatFileSize(mediaFile.sizeBytes)) },
                    leadingIcon = {
                        Icon(Icons.Default.Storage, null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        leadingIconContentColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }

            // Trim range slider
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ContentCut, null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Select Range",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    TrimRangeSlider(
                        durationMs = mediaFile.durationMs,
                        startMs = state.startMs,
                        endMs = state.endMs,
                        currentPositionMs = playerPositionMs,
                        onRangeChange = { s, e -> viewModel.setTrimRange(s, e) }
                    )
                }
            }

            // Operation + Format selector
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tune, null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.trim_operation),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
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
                                shape = SegmentedButtonDefaults.itemShape(index, ops.size),
                                label = {
                                    Text(
                                        when (op) {
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

                    FormatSelector(
                        label = stringResource(R.string.trim_output_format),
                        formats = viewModel.availableOutputFormats(),
                        selected = state.outputFormat,
                        onSelect = { viewModel.setOutputFormat(it) }
                    )
                }
            }

            // Error
            state.error?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(err, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // Process button / Progress
            if (!state.isProcessing) {
                Button(
                    onClick = { viewModel.startProcessing() },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (state.operation) {
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
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "${stringResource(R.string.trim_processing)} ${state.progress}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
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
                            Text(stringResource(R.string.trim_cancel), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FileInfoCard(
    title: String,
    icon: ImageVector,
    cardColor: androidx.compose.ui.graphics.Color,
    onCardColor: androidx.compose.ui.graphics.Color,
    fileName: String,
    filePath: String,
    fileType: String,
    fileSize: String,
    duration: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = onCardColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = onCardColor
                )
            }
            Spacer(Modifier.height(8.dp))

            // File name
            Text(
                fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = onCardColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))

            // Info chips row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoBadge(Icons.Default.AudioFile, fileType, onCardColor)
                InfoBadge(Icons.Default.Storage, fileSize, onCardColor)
                InfoBadge(Icons.Default.Timer, duration, onCardColor)
            }

            Spacer(Modifier.height(6.dp))

            // Path
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Folder, null,
                    tint = onCardColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(13.dp).padding(top = 2.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    filePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = onCardColor.copy(alpha = 0.4f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun InfoBadge(
    icon: ImageVector,
    text: String,
    tintColor: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(12.dp), tint = tintColor.copy(alpha = 0.6f))
        Spacer(Modifier.width(3.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = tintColor.copy(alpha = 0.7f))
    }
}
