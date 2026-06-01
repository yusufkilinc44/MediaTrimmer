package com.yusufkilinc.mediatrimmer.presentation.trim.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yusufkilinc.mediatrimmer.domain.model.MediaFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelector(
    label: String,
    formats: List<MediaFormat>,
    selected: MediaFormat,
    onSelect: (MediaFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            formats.forEach { fmt ->
                DropdownMenuItem(
                    text = {
                        Row {
                            Text(fmt.displayName, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                ".${fmt.extension}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(fmt)
                        expanded = false
                    },
                    trailingIcon = if (fmt == selected) {{
                        Text("✓", color = MaterialTheme.colorScheme.primary)
                    }} else null
                )
            }
        }
    }
}
