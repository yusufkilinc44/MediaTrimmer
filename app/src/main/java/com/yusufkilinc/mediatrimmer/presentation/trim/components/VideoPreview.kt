package com.yusufkilinc.mediatrimmer.presentation.trim.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.yusufkilinc.mediatrimmer.core.util.TimeUtils

@Composable
fun VideoPreview(
    filePath: String,
    startMs: Long,
    endMs: Long,
    isVideo: Boolean,
    modifier: Modifier = Modifier,
    onPositionChanged: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(startMs) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(filePath)))
            prepare()
            seekTo(startMs)
        }
    }

    // Track playback position
    LaunchedEffect(player) {
        while (true) {
            val pos = player.currentPosition
            currentPositionMs = pos
            onPositionChanged(pos)
            // Stop at end marker
            if (pos >= endMs && player.isPlaying) {
                player.pause()
                player.seekTo(startMs)
                isPlaying = false
            }
            kotlinx.coroutines.delay(100L)
        }
    }

    DisposableEffect(filePath) {
        onDispose { player.release() }
    }

    Column(modifier = modifier) {
        if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            )
        } else {
            // Audio waveform placeholder
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = TimeUtils.formatTimecode(currentPositionMs),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Playback controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skip to start
            IconButton(onClick = {
                player.seekTo(startMs)
                currentPositionMs = startMs
            }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Start",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Play / Pause
            FilledIconButton(
                onClick = {
                    if (isPlaying) {
                        player.pause()
                    } else {
                        if (currentPositionMs >= endMs) player.seekTo(startMs)
                        player.play()
                    }
                    isPlaying = !isPlaying
                }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }

            // Skip to end
            IconButton(onClick = {
                player.seekTo(endMs)
                currentPositionMs = endMs
            }) {
                Icon(Icons.Default.SkipNext, contentDescription = "End",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Time display - separate centered row
        Text(
            text = "${TimeUtils.formatTimecode(currentPositionMs)} / ${TimeUtils.formatTimecode(endMs - startMs)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}
