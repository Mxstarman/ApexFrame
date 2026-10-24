package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexRecordRed
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VideoPlayerView(
    videoPath: String,
    modifier: Modifier = Modifier,
    initialPlay: Boolean = true,
    onProgressUpdate: ((currentMs: Long, totalMs: Long) -> Unit)? = null
) {
    var isPlaying by remember { mutableStateOf(initialPlay) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var totalDurationMs by remember { mutableStateOf(1L) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    // Position polling coroutine
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    val pos = vv.currentPosition.toLong()
                    val dur = vv.duration.toLong().coerceAtLeast(1L)
                    currentPositionMs = pos
                    totalDurationMs = dur
                    onProgressUpdate?.invoke(pos, dur)
                }
            }
            delay(250L)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .testTag("video_player_box"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                VideoView(context).apply {
                    setVideoURI(Uri.fromFile(File(videoPath)))
                    setOnPreparedListener { mp ->
                        totalDurationMs = mp.duration.toLong().coerceAtLeast(1L)
                        mp.isLooping = true
                        if (initialPlay) {
                            start()
                            isPlaying = true
                        }
                    }
                    setOnCompletionListener {
                        isPlaying = false
                    }
                    videoViewRef = this
                }
            },
            update = { view ->
                videoViewRef = view
            }
        )

        // Bottom playback control overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xB30A0F1D))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Scrubber
            Slider(
                value = (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f),
                onValueChange = { frac ->
                    val seekTo = (frac * totalDurationMs).toInt()
                    videoViewRef?.seekTo(seekTo)
                    currentPositionMs = seekTo.toLong()
                },
                colors = SliderDefaults.colors(
                    thumbColor = ApexCyan,
                    activeTrackColor = ApexCyan,
                    inactiveTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            videoViewRef?.let { vv ->
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            videoViewRef?.let { vv ->
                                val target = (vv.currentPosition - 10000).coerceAtLeast(0)
                                vv.seekTo(target)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "${formatTime(currentPositionMs)} / ${formatTime(totalDurationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", min, sec)
}
