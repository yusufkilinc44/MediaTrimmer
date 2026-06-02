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
    var sliderStart by remember(startMs) { mutableFloatStateOf(startMs.toFloat()) }
    var sliderEnd   by remember(endMs)   { mutableFloatStateOf(endMs.toFloat()) }

    val minGapMs = 500L

    // Manual input state
    var startText by remember(startMs) { mutableStateOf(formatForInput(startMs)) }
    var endText   by remember(endMs)   { mutableStateOf(formatForInput(endMs)) }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        // Manual time input fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = startText,
                onValueChange = { startText = it },
                label = { Text("Start", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    val parsed = parseTimecode(startText)
                    if (parsed != null && parsed < sliderEnd.toLong() - minGapMs && parsed >= 0) {
                        sliderStart = parsed.toFloat()
                        onRangeChange(parsed, sliderEnd.toLong())
                    } else {
                        startText = formatForInput(sliderStart.toLong())
                    }
                    focusManager.clearFocus()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Text(
                text = "→  ${TimeUtils.formatDuration(sliderEnd.toLong() - sliderStart.toLong())}  →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = endText,
                onValueChange = { endText = it },
                label = { Text("End", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    val parsed = parseTimecode(endText)
                    if (parsed != null && parsed > sliderStart.toLong() + minGapMs && parsed <= durationMs) {
                        sliderEnd = parsed.toFloat()
                        onRangeChange(sliderStart.toLong(), parsed)
                    } else {
                        endText = formatForInput(sliderEnd.toLong())
                    }
                    focusManager.clearFocus()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        RangeSlider(
            value = sliderStart..sliderEnd,
            onValueChange = { range ->
                val newStart = range.start.toLong()
                val newEnd   = range.endInclusive.toLong()
                if (newEnd - newStart >= minGapMs) {
                    sliderStart = range.start
                    sliderEnd   = range.endInclusive
                    startText = formatForInput(newStart)
                    endText = formatForInput(newEnd)
                    onRangeChange(newStart, newEnd)
                }
            },
            valueRange = 0f..durationMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor            = MaterialTheme.colorScheme.tertiary,
                activeTrackColor      = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                inactiveTrackColor    = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Total duration indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0:00",
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

/**
 * Formats milliseconds as MM:SS.m or HH:MM:SS.m for the input field.
 */
private fun formatForInput(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    val tenths = (ms % 1000) / 100

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d.%d", hours, minutes, seconds, tenths)
    } else {
        String.format(Locale.US, "%d:%02d.%d", minutes, seconds, tenths)
    }
}

/**
 * Parses a timecode string (MM:SS, MM:SS.m, HH:MM:SS, HH:MM:SS.m) to milliseconds.
 * Returns null if parsing fails.
 */
private fun parseTimecode(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    return try {
        val parts = trimmed.split(":")
        var ms = 0L

        when (parts.size) {
            1 -> {
                // Just seconds (possibly with decimal)
                ms = parseSecondsToMs(parts[0])
            }
            2 -> {
                // MM:SS or MM:SS.m
                val minutes = parts[0].toLong()
                ms = minutes * 60_000 + parseSecondsToMs(parts[1])
            }
            3 -> {
                // HH:MM:SS or HH:MM:SS.m
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                ms = hours * 3_600_000 + minutes * 60_000 + parseSecondsToMs(parts[2])
            }
            else -> return null
        }

        if (ms < 0) null else ms
    } catch (_: Exception) {
        null
    }
}

private fun parseSecondsToMs(secStr: String): Long {
    val dotParts = secStr.split(".")
    val secs = dotParts[0].toLong()
    val fracMs = if (dotParts.size > 1) {
        val frac = dotParts[1].take(3).padEnd(3, '0')
        frac.toLong()
    } else 0L
    return secs * 1000 + fracMs
}
