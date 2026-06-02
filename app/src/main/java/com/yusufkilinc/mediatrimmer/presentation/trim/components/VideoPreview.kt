package com.yusufkilinc.mediatrimmer.presentation.trim.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
    var isSeeking by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (filePath.startsWith("/")) Uri.fromFile(java.io.File(filePath)) else Uri.parse(filePath)
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            seekTo(startMs)
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (!isSeeking) {
                val pos = player.currentPosition
                currentPositionMs = pos
                onPositionChanged(pos)
                if (pos >= endMs && player.isPlaying) {
                    player.pause()
                    player.seekTo(startMs)
                    isPlaying = false
                }
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
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = TimeUtils.formatTimecode(currentPositionMs),
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Seekbar slider
        val totalDuration = (endMs - startMs).coerceAtLeast(1L)
        Slider(
            value = currentPositionMs.toFloat().coerceIn(startMs.toFloat(), endMs.toFloat()),
            onValueChange = { value ->
                isSeeking = true
                currentPositionMs = value.toLong()
                onPositionChanged(value.toLong())
            },
            onValueChangeFinished = {
                player.seekTo(currentPositionMs)
                isSeeking = false
            },
            valueRange = startMs.toFloat()..endMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        // Play/Pause + time display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = {
                    if (isPlaying) {
                        player.pause()
                    } else {
                        if (currentPositionMs >= endMs) player.seekTo(startMs)
                        player.play()
                    }
                    isPlaying = !isPlaying
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = "${TimeUtils.formatTimecode(currentPositionMs)} / ${TimeUtils.formatTimecode(endMs - startMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
