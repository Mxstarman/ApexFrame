package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.RecordingsRepository
import com.example.model.RecordingItem
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun TrimmerDialog(
    item: RecordingItem,
    recordingsRepository: RecordingsRepository,
    onDismiss: () -> Unit,
    onTrimSuccess: (RecordingItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val totalMs = item.durationMs.coerceAtLeast(1000L)
    var startMs by remember { mutableStateOf(0L) }
    var endMs by remember { mutableStateOf(totalMs) }

    var isTrimming by remember { mutableStateOf(false) }
    var trimProgress by remember { mutableStateOf(0f) }
    var trimmedItem by remember { mutableStateOf<RecordingItem?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, ApexBorder, RoundedCornerShape(20.dp))
                .testTag("trimmer_dialog"),
            color = ApexSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Trimmer",
                            tint = ApexCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Apex Video Trimmer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ApexTextPrimary
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = ApexTextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_trimmer_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ApexTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Video Preview Player
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    val activePath = trimmedItem?.filePath ?: item.filePath
                    VideoPlayerView(
                        videoPath = activePath,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Trim Controls Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ApexSurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Start: ${formatMs(startMs)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ApexCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Duration: ${formatMs(endMs - startMs)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ApexEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "End: ${formatMs(endMs)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ApexRecordRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Start Slider
                        Text("Trim In Point", style = MaterialTheme.typography.labelSmall, color = ApexTextMuted)
                        Slider(
                            value = startMs.toFloat(),
                            onValueChange = { newStart ->
                                startMs = newStart.toLong().coerceAtMost(endMs - 500L)
                            },
                            valueRange = 0f..totalMs.toFloat(),
                            modifier = Modifier.testTag("trim_start_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = ApexCyan,
                                activeTrackColor = ApexCyan,
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )

                        // End Slider
                        Text("Trim Out Point", style = MaterialTheme.typography.labelSmall, color = ApexTextMuted)
                        Slider(
                            value = endMs.toFloat(),
                            onValueChange = { newEnd ->
                                endMs = newEnd.toLong().coerceAtLeast(startMs + 500L)
                            },
                            valueRange = 0f..totalMs.toFloat(),
                            modifier = Modifier.testTag("trim_end_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = ApexRecordRed,
                                activeTrackColor = ApexRecordRed,
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar if Trimming
                if (isTrimming) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { trimProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = ApexCyan,
                            trackColor = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lossless extraction: ${(trimProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = ApexCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Share Button
                    OutlinedButton(
                        onClick = {
                            val targetFile = File(trimmedItem?.filePath ?: item.filePath)
                            shareVideo(context, targetFile)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trim_share_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ApexCyan),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(ApexCyan)
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    // Export Trim Button
                    Button(
                        onClick = {
                            isTrimming = true
                            coroutineScope.launch {
                                val result = recordingsRepository.trimRecording(
                                    sourceItem = item,
                                    startMs = startMs,
                                    endMs = endMs,
                                    onProgress = { p -> trimProgress = p }
                                )
                                isTrimming = false
                                if (result != null) {
                                    trimmedItem = result
                                    Toast.makeText(context, "Trimmed successfully: ${result.title}", Toast.LENGTH_SHORT).show()
                                    onTrimSuccess(result)
                                } else {
                                    Toast.makeText(context, "Trimming failed.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isTrimming,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("trim_export_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ApexRecordRed)
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Trim", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lossless Trim & Save")
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    val millis = (ms % 1000) / 100
    return String.format("%02d:%02d.%d", min, sec, millis)
}

private fun shareVideo(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share screen recording"))
    } catch (e: Exception) {
        // Fallback simple share intent
        val uri = Uri.fromFile(file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share screen recording"))
    }
}
