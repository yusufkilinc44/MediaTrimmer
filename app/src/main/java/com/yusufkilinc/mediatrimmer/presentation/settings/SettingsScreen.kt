package com.yusufkilinc.mediatrimmer.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufkilinc.mediatrimmer.R
import com.yusufkilinc.mediatrimmer.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAbout: () -> Unit,
    onLanguageChanged: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

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
                                viewModel.setLanguage(lang)
                                onLanguageChanged()
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
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_theme)) },
                    leadingContent = {
                        val icon = when (settings.themeMode) {
                            ThemeMode.DARK   -> Icons.Default.DarkMode
                            ThemeMode.LIGHT  -> Icons.Default.LightMode
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        }
                        Icon(icon, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        SegmentedButton(
                            options = listOf(
                                stringResource(R.string.settings_theme_system),
                                stringResource(R.string.settings_theme_light),
                                stringResource(R.string.settings_theme_dark)
                            ),
                            selected = when (settings.themeMode) {
                                ThemeMode.SYSTEM -> 0
                                ThemeMode.LIGHT  -> 1
                                ThemeMode.DARK   -> 2
                            },
                            onSelect = { idx ->
                                val theme = when (idx) {
                                    1 -> ThemeMode.LIGHT
                                    2 -> ThemeMode.DARK
                                    else -> ThemeMode.SYSTEM
                                }
                                viewModel.setTheme(theme)
                            }
                        )
                    }
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
                    modifier = androidx.compose.ui.Modifier.clickable(
                        onClick = { showClearDialog = true }
                    )
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
                    modifier = androidx.compose.ui.Modifier.clickable(onClick = onNavigateToAbout)
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

// Extension to make ListItem clickable
private fun Modifier.clickable(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
