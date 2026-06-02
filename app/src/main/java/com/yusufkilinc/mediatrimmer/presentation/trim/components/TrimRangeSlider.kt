package com.yusufkilinc.mediatrimmer.presentation.trim.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yusufkilinc.mediatrimmer.core.util.TimeUtils
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimRangeSlider(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    currentPositionMs: Long = 0L,
    onRangeChange: (start: Long, end: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderStart by remember { mutableFloatStateOf(startMs.toFloat()) }
    var sliderEnd by remember { mutableFloatStateOf(endMs.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    val minGapMs = 500L
    val focusManager = LocalFocusManager.current

    // Sync from external state only when not dragging
    LaunchedEffect(startMs, endMs) {
        if (!isDragging) {
            sliderStart = startMs.toFloat()
            sliderEnd = endMs.toFloat()
        }
    }

    // Start time fields
    var startHH by remember { mutableStateOf("") }
    var startMM by remember { mutableStateOf("") }
    var startSS by remember { mutableStateOf("") }
    var startCS by remember { mutableStateOf("") }

    // End time fields
    var endHH by remember { mutableStateOf("") }
    var endMM by remember { mutableStateOf("") }
    var endSS by remember { mutableStateOf("") }
    var endCS by remember { mutableStateOf("") }

    fun updateStartFields(ms: Long) {
        val h = TimeUnit.MILLISECONDS.toHours(ms)
        val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        val c = (ms % 1000) / 10
        startHH = String.format(Locale.US, "%02d", h)
        startMM = String.format(Locale.US, "%02d", m)
        startSS = String.format(Locale.US, "%02d", s)
        startCS = String.format(Locale.US, "%02d", c)
    }

    fun updateEndFields(ms: Long) {
        val h = TimeUnit.MILLISECONDS.toHours(ms)
        val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        val c = (ms % 1000) / 10
        endHH = String.format(Locale.US, "%02d", h)
        endMM = String.format(Locale.US, "%02d", m)
        endSS = String.format(Locale.US, "%02d", s)
        endCS = String.format(Locale.US, "%02d", c)
    }

    // Initialize fields
    LaunchedEffect(Unit) {
        updateStartFields(startMs)
        updateEndFields(endMs)
    }

    // Update fields when slider values change (from drag or external)
    LaunchedEffect(sliderStart) {
        updateStartFields(sliderStart.toLong())
    }
    LaunchedEffect(sliderEnd) {
        updateEndFields(sliderEnd.toLong())
    }

    fun parseFieldsToMs(hh: String, mm: String, ss: String, cs: String): Long? {
        val h = hh.toLongOrNull() ?: return null
        val m = mm.toLongOrNull() ?: return null
        val s = ss.toLongOrNull() ?: return null
        val c = cs.toLongOrNull() ?: return null
        if (h < 0 || m < 0 || m > 59 || s < 0 || s > 59 || c < 0 || c > 99) return null
        return h * 3_600_000 + m * 60_000 + s * 1_000 + c * 10
    }

    fun commitStartFields() {
        val parsed = parseFieldsToMs(startHH, startMM, startSS, startCS)
        if (parsed != null && parsed < sliderEnd.toLong() - minGapMs && parsed >= 0) {
            sliderStart = parsed.toFloat()
            onRangeChange(parsed, sliderEnd.toLong())
        } else {
            updateStartFields(sliderStart.toLong())
        }
        focusManager.clearFocus()
    }

    fun commitEndFields() {
        val parsed = parseFieldsToMs(endHH, endMM, endSS, endCS)
        if (parsed != null && parsed > sliderStart.toLong() + minGapMs && parsed <= durationMs) {
            sliderEnd = parsed.toFloat()
            onRangeChange(sliderStart.toLong(), parsed)
        } else {
            updateEndFields(sliderEnd.toLong())
        }
        focusManager.clearFocus()
    }

    Column(modifier = modifier) {
        // Start time label + fields
        Text(
            "Start",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeField(value = startHH, onValueChange = { startHH = it }, label = "HH", modifier = Modifier.weight(1f),
                onDone = { commitStartFields() })
            Text(":", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TimeField(value = startMM, onValueChange = { startMM = it }, label = "MM", modifier = Modifier.weight(1f),
                onDone = { commitStartFields() })
            Text(":", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TimeField(value = startSS, onValueChange = { startSS = it }, label = "SS", modifier = Modifier.weight(1f),
                onDone = { commitStartFields() })
            Text(".", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TimeField(value = startCS, onValueChange = { startCS = it }, label = "ms", modifier = Modifier.weight(1f),
                onDone = { commitStartFields() })
        }

        Spacer(Modifier.height(8.dp))

        // Duration display
        Text(
            text = "→  ${TimeUtils.formatDuration(sliderEnd.toLong() - sliderStart.toLong())}  →",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // End time label + fields
        Text(
            "End",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeField(value = endHH, onValueChange = { endHH = it }, label = "HH", modifier = Modifier.weight(1f),
                onDone = { commitEndFields() })
            Text(":", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TimeField(value = endMM, onValueChange = { endMM = it }, label = "MM", modifier = Modifier.weight(1f),
                onDone = { commitEndFields() })
            Text(":", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TimeField(value = endSS, onValueChange = { endSS = it }, label = "SS", modifier = Modifier.weight(1f),
                onDone = { commitEndFields() })
            Text(".", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TimeField(value = endCS, onValueChange = { endCS = it }, label = "ms", modifier = Modifier.weight(1f),
                onDone = { commitEndFields() })
        }

        Spacer(Modifier.height(12.dp))

        // Range slider
        RangeSlider(
            value = sliderStart..sliderEnd,
            onValueChange = { range ->
                val newStart = range.start.toLong()
                val newEnd = range.endInclusive.toLong()
                if (newEnd - newStart >= minGapMs) {
                    isDragging = true
                    sliderStart = range.start
                    sliderEnd = range.endInclusive
                }
            },
            onValueChangeFinished = {
                isDragging = false
                onRangeChange(sliderStart.toLong(), sliderEnd.toLong())
            },
            valueRange = 0f..durationMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.tertiary,
                activeTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Total duration indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "00:00:00",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = TimeUtils.formatTimecodeSec(durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun TimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newVal ->
            val filtered = newVal.filter { it.isDigit() }.take(2)
            onValueChange(filtered)
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )
}
