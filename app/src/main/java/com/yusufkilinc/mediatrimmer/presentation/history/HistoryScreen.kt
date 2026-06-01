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
import com.yusufkilinc.mediatrimmer.data.local.db.entity.ProcessingHistoryEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                        Icons.Default.History,
                        contentDescription = null,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    },
                    onOpen = {
                        if (File(entry.outputFilePath).exists()) {
                            val intent = ShareUtils.createShareIntent(context, entry.outputFilePath)
                            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    val fileExists = remember(entry.outputFilePath) { File(entry.outputFilePath).exists() }
    val isSuccess = entry.status == "COMPLETED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess)
                MaterialTheme.colorScheme.surfaceContainer
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isSuccess) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = FileUtils.getFileName(
                        entry.outputFilePath.ifEmpty { entry.sourceFileName }
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                // Operation badge
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            entry.operationType.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            Row {
                Text(
                    text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        .format(Date(entry.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.startMs > 0 || entry.endMs > 0) {
                    Text(
                        text = " · ${TimeUtils.formatTimecodeSec(entry.startMs)} → ${TimeUtils.formatTimecodeSec(entry.endMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry.outputFileSizeBytes > 0) {
                    Text(
                        text = " · ${FileUtils.formatFileSize(entry.outputFileSizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!fileExists && isSuccess) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.history_file_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!isSuccess && entry.errorMessage != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (isSuccess && fileExists) {
                    TextButton(onClick = onShare) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.history_share))
                    }
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.history_delete),
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
