package com.yusufkilinc.mediatrimmer.presentation.history

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufkilinc.mediatrimmer.R
import com.yusufkilinc.mediatrimmer.core.util.FileUtils
import com.yusufkilinc.mediatrimmer.core.util.ShareUtils
import com.yusufkilinc.mediatrimmer.core.util.TimeUtils
import com.yusufkilinc.mediatrimmer.data.local.db.entity.ProcessingHistoryEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HistoryCardColor = Color(0xFFFFF8E1)
private val HistoryCardColorDark = Color(0xFF3E3A2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.history_clear_all),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        AnimatedVisibility(
            visible = history.isEmpty(),
            enter = fadeIn(), exit = fadeOut()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = history, key = { it.id }) { entry ->
                HistoryItem(
                    entry = entry,
                    onDelete = { viewModel.deleteEntry(entry.id) },
                    onShare = {
                        if (File(entry.outputFilePath).exists()) {
                            context.startActivity(
                                ShareUtils.createShareIntent(context, entry.outputFilePath)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.history_clear_all)) },
            text = { Text(stringResource(R.string.history_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) {
                    Text(stringResource(R.string.btn_confirm),
                        color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun HistoryItem(
    entry: ProcessingHistoryEntity,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val fileExists = remember(entry.outputFilePath) {
        !entry.outputFilePath.startsWith("content://") && File(entry.outputFilePath).exists()
    }
    val isSuccess = entry.status == "COMPLETED"

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardColor = if (isSuccess) {
        if (isDark) HistoryCardColorDark else HistoryCardColor
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header: status icon + operation chip + date ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isSuccess) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.height(22.dp)
                ) {
                    Text(
                        text = entry.operationType.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault())
                        .format(Date(entry.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
            Spacer(Modifier.height(12.dp))

            // ── Source file ──
            InfoRow(
                icon = Icons.Default.FileOpen,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                label = stringResource(R.string.history_source),
                value = entry.sourceFileName
            )

            if (entry.sourceFileSizeBytes > 0 || entry.sourceDurationMs > 0) {
                Spacer(Modifier.height(2.dp))
                Row(modifier = Modifier.padding(start = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (entry.sourceFileSizeBytes > 0) {
                        DetailChip(Icons.Default.Storage, FileUtils.formatFileSize(entry.sourceFileSizeBytes))
                    }
                    if (entry.sourceDurationMs > 0) {
                        DetailChip(Icons.Default.Timer, TimeUtils.formatDuration(entry.sourceDurationMs))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Output file ──
            InfoRow(
                icon = Icons.Default.SaveAlt,
                iconTint = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.result_output),
                value = entry.outputFileName.ifEmpty { FileUtils.getFileName(entry.outputFilePath) }
            )

            Spacer(Modifier.height(2.dp))
            Row(modifier = Modifier.padding(start = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailChip(Icons.Default.AudioFile, entry.outputFormat)
                if (entry.outputFileSizeBytes > 0) {
                    DetailChip(Icons.Default.Storage, FileUtils.formatFileSize(entry.outputFileSizeBytes))
                }
            }

            if (entry.startMs > 0 || entry.endMs > 0) {
                Spacer(Modifier.height(2.dp))
                Row(modifier = Modifier.padding(start = 24.dp)) {
                    DetailChip(
                        Icons.Default.ContentCut,
                        "${TimeUtils.formatTimecodeSec(entry.startMs)} → ${TimeUtils.formatTimecodeSec(entry.endMs)}"
                    )
                }
            }

            // ── Processing time ──
            if (entry.processingDurationMs > 0) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.padding(start = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Speed, null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${stringResource(R.string.history_processed_in)} ${String.format("%.1f s", entry.processingDurationMs / 1000.0)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // ── Warnings ──
            if (!fileExists && isSuccess && !entry.outputFilePath.startsWith("content://")) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.history_file_missing),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }
            if (!isSuccess && entry.errorMessage != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    entry.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
            Spacer(Modifier.height(4.dp))

            // ── Action buttons ──
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSuccess && fileExists) {
                    TextButton(onClick = onShare, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.history_share), style = MaterialTheme.typography.labelMedium)
                    }
                }
                TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.history_delete),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DetailChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

private fun Color.luminance(): Float {
    val r = red * 0.2126f
    val g = green * 0.7152f
    val b = blue * 0.0722f
    return r + g + b
}
