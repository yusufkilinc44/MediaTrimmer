package com.yusufkilinc.mediatrimmer.presentation.trim.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.yusufkilinc.mediatrimmer.core.util.TimeUtils

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

    val minGapMs = 500L  // Minimum selection of 500ms

    Column(modifier = modifier) {
        // Timecode row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = TimeUtils.formatTimecode(sliderStart.toLong()),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Text(
                text = "→  ${TimeUtils.formatDuration(sliderEnd.toLong() - sliderStart.toLong())}  →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = TimeUtils.formatTimecode(sliderEnd.toLong()),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        RangeSlider(
            value = sliderStart..sliderEnd,
            onValueChange = { range ->
                val newStart = range.start.toLong()
                val newEnd   = range.endInclusive.toLong()
                // Enforce minimum gap
                if (newEnd - newStart >= minGapMs) {
                    sliderStart = range.start
                    sliderEnd   = range.endInclusive
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
