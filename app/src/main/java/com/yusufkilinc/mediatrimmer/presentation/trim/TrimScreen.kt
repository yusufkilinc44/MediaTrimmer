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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
    onNavigateHome: () -> Unit = onBack,
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
            ResultScreen(
                state = state,
                mediaFile = mediaFile,
                onNavigateHome = onNavigateHome,
                onShare = { outputPath ->
                    val shareIntent = if (outputPath.startsWith("content://")) {
                        val uri = Uri.parse(outputPath)
                        val mime = context.contentResolver.getType(uri) ?: "*/*"
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = mime
                                putExtra(Intent.EXTRA_STREAM, uri)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }, null
                        )
                    } else {
                        ShareUtils.createShareIntent(context, outputPath)
                    }
                    context.startActivity(shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
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

            // ── Operation + Format selector (ABOVE range slider) ────────
            OutlinedCard(
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tune, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.trim_operation),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
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

            // ── Trim range slider (only for TRIM operation) ─────────────
            if (state.operation == OperationType.TRIM) {
                OutlinedCard(
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ContentCut, null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.trim_select_range),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
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

// ═══════════════════════════════════════════════════════════════════════════
//  RESULT SCREEN composable
// ═══════════════════════════════════════════════════════════════════════════
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun ResultScreen(
    state: TrimUiState,
    mediaFile: com.yusufkilinc.mediatrimmer.domain.model.MediaFile,
    onNavigateHome: () -> Unit,
    onShare: (String) -> Unit
) {
    val context = LocalContext.current
    val outputPath = state.outputPath!!
    val isContentUri = outputPath.startsWith("content://")

    // Output file info
    val outputFileName = if (isContentUri) {
        FileUtils.getDisplayNameFromUri(context, Uri.parse(outputPath)) ?: "output"
    } else {
        FileUtils.getFileName(outputPath)
    }
    val outputFileSize = if (isContentUri) {
        FileUtils.getFileSizeFromUri(context, Uri.parse(outputPath))
    } else {
        val f = File(outputPath)
        if (f.exists()) f.length() else 0L
    }

    // In-app player
    val contentUri = remember(outputPath) {
        if (isContentUri) {
            Uri.parse(outputPath)
        } else {
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(outputPath))
            } catch (_: Exception) {
                Uri.fromFile(File(outputPath))
            }
        }
    }

    val isOutputVideo = state.operation != OperationType.EXTRACT_AUDIO &&
            !state.outputFormat.isAudioOnly

    val player = remember(contentUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(contentUri))
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
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

        // Processing time badge with label
        AssistChip(
            onClick = {},
            label = {
                Text(
                    "${stringResource(R.string.result_processing_time)}: ${
                        String.format("%.1f s", state.processingDurationMs / 1000.0)
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Speed, null, modifier = Modifier.size(16.dp))
            },
            colors = AssistChipDefaults.assistChipColors(
                leadingIconContentColor = MaterialTheme.colorScheme.tertiary
            )
        )

        // In-app player
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            if (isOutputVideo) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
            } else {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.MusicNote, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = true
                                controllerShowTimeoutMs = 0
                                showController()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )
                }
            }
        }

        // ── INPUT file info ──────────────────────────────────────
        val sourceFriendlyDir = FileUtils.friendlyDirPath(state.originalSourcePath.ifEmpty { mediaFile.resolvedPath })
        FileInfoCard(
            title = stringResource(R.string.result_input),
            icon = Icons.Default.FileOpen,
            cardColor = MaterialTheme.colorScheme.surfaceContainer,
            onCardColor = MaterialTheme.colorScheme.onSurface,
            fileName = mediaFile.displayName,
            filePath = sourceFriendlyDir,
            fileType = mediaFile.format?.displayName ?: "?",
            fileSize = FileUtils.formatFileSize(mediaFile.sizeBytes),
            duration = TimeUtils.formatDuration(mediaFile.durationMs)
        )

        // ── OUTPUT file info ─────────────────────────────────────
        FileInfoCard(
            title = stringResource(R.string.result_output),
            icon = Icons.Default.SaveAlt,
            cardColor = MaterialTheme.colorScheme.secondaryContainer,
            onCardColor = MaterialTheme.colorScheme.onSecondaryContainer,
            fileName = outputFileName,
            filePath = FileUtils.friendlyDirPath(outputPath),
            fileType = state.outputFormat.displayName,
            fileSize = FileUtils.formatFileSize(outputFileSize),
            duration = if (state.operation == OperationType.CONVERT) {
                TimeUtils.formatDuration(mediaFile.durationMs)
            } else {
                TimeUtils.formatDuration(state.endMs - state.startMs)
            }
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = { onShare(outputPath) },
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.trim_share))
            }
        }

        OutlinedButton(
            onClick = onNavigateHome,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Home, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_close))
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = onCardColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = onCardColor
                )
            }
            Spacer(Modifier.height(10.dp))

            // File name - bigger font
            Text(
                fileName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = onCardColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))

            // Info chips row - bigger font
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoBadge(Icons.Default.AudioFile, fileType, onCardColor)
                InfoBadge(Icons.Default.Storage, fileSize, onCardColor)
                InfoBadge(Icons.Default.Timer, duration, onCardColor)
            }

            Spacer(Modifier.height(8.dp))

            // Path - friendly format
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Folder, null,
                    tint = onCardColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(15.dp).padding(top = 2.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    filePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = onCardColor.copy(alpha = 0.5f),
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
        Icon(icon, null, modifier = Modifier.size(15.dp), tint = tintColor.copy(alpha = 0.7f))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = tintColor.copy(alpha = 0.8f))
    }
}
