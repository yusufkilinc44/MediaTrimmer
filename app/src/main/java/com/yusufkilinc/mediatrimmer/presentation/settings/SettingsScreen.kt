package com.yusufkilinc.mediatrimmer.presentation.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufkilinc.mediatrimmer.R
import com.yusufkilinc.mediatrimmer.core.util.FileUtils
import com.yusufkilinc.mediatrimmer.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAbout: () -> Unit,
    onLanguageChanged: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val defaultOutputDir = remember { FileUtils.getOutputDirectory(context).absolutePath }

    // SAF directory picker
    val dirPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Take persistent permission
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flags)
            viewModel.setOutputDirUri(it.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // ── Language ────────────────────────────────────────────────────
            item {
                SettingsSectionHeader(stringResource(R.string.settings_language))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_language)) },
                    leadingContent = {
                        Icon(Icons.Default.Language, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        SegmentedButton(
                            options = listOf("EN", "TR"),
                            selected = if (settings.language == "en") 0 else 1,
                            onSelect = { idx ->
                                val lang = if (idx == 0) "en" else "tr"
                                scope.launch {
                                    viewModel.setLanguageAndWait(lang)
                                    onLanguageChanged()
                                }
                            }
                        )
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

            // ── Theme ────────────────────────────────────────────────────────
            item {
                SettingsSectionHeader(stringResource(R.string.settings_theme))
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        val icon = when (settings.themeMode) {
                            ThemeMode.DARK   -> Icons.Default.DarkMode
                            ThemeMode.LIGHT  -> Icons.Default.LightMode
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        }
                        Icon(icon, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.settings_theme),
                            style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(
                            stringResource(R.string.settings_theme_system),
                            stringResource(R.string.settings_theme_light),
                            stringResource(R.string.settings_theme_dark)
                        )
                        val selected = when (settings.themeMode) {
                            ThemeMode.SYSTEM -> 0
                            ThemeMode.LIGHT  -> 1
                            ThemeMode.DARK   -> 2
                        }
                        options.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = selected == index,
                                onClick = {
                                    val theme = when (index) {
                                        1 -> ThemeMode.LIGHT
                                        2 -> ThemeMode.DARK
                                        else -> ThemeMode.SYSTEM
                                    }
                                    viewModel.setTheme(theme)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

            // ── Output Directory ──────────────────────────────────────────────
            item {
                SettingsSectionHeader(stringResource(R.string.settings_output_dir))
            }
            item {
                val currentDir = settings.outputDirUri?.let { FileUtils.friendlyDirFromTreeUri(it) }
                    ?: FileUtils.friendlyPath(defaultOutputDir)

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_output_dir)) },
                    supportingContent = {
                        Column {
                            Text(
                                currentDir,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.settings_change_output_dir),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    leadingContent = {
                        Icon(Icons.Default.Folder, contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary)
                    },
                    trailingContent = {
                        if (settings.outputDirUri != null) {
                            IconButton(onClick = { viewModel.setOutputDirUri(null) }) {
                                Icon(Icons.Default.RestartAlt,
                                    contentDescription = stringResource(R.string.settings_output_dir_reset),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { dirPicker.launch(null) }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

            // ── Data ──────────────────────────────────────────────────────────
            item {
                SettingsSectionHeader("Data")
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_clear_history)) },
                    leadingContent = {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.clickable { showClearDialog = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

            // ── About ─────────────────────────────────────────────────────────
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_about)) },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onNavigateToAbout() }
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.settings_clear_history)) },
            text = { Text(stringResource(R.string.history_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
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
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SegmentedButton(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = selected == index,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
