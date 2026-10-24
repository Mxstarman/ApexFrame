package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.RecordingsRepository
import com.example.model.RecordingItem
import com.example.ui.components.TrimmerDialog
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GalleryScreen(
    recordings: List<RecordingItem>,
    recordingsRepository: RecordingsRepository,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var previewItem by remember { mutableStateOf<RecordingItem?>(null) }
    var trimItem by remember { mutableStateOf<RecordingItem?>(null) }
    var renameItem by remember { mutableStateOf<RecordingItem?>(null) }
    var deleteCandidate by remember { mutableStateOf<RecordingItem?>(null) }

    // Trimmer Dialog
    trimItem?.let { item ->
        TrimmerDialog(
            item = item,
            recordingsRepository = recordingsRepository,
            onDismiss = { trimItem = null },
            onTrimSuccess = { trimmed ->
                trimItem = null
                onRefresh()
            }
        )
    }

    // Video Playback Preview Dialog
    previewItem?.let { item ->
        Dialog(onDismissRequest = { previewItem = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.75f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, ApexBorder, RoundedCornerShape(16.dp)),
                color = ApexSurface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { previewItem = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = ApexTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        VideoPlayerView(videoPath = item.filePath, modifier = Modifier.fillMaxSize())
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                shareVideo(context, File(item.filePath))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ApexCyan)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }

                        Button(
                            onClick = {
                                previewItem = null
                                trimItem = item
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ApexRecordRed)
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = "Trim", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trim Video")
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    renameItem?.let { item ->
        var newTitle by remember { mutableStateOf(item.title) }
        AlertDialog(
            onDismissRequest = { renameItem = null },
            title = { Text("Rename Recording") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ApexCyan,
                        unfocusedBorderColor = ApexBorder
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val res = recordingsRepository.renameRecording(item, newTitle)
                            if (res != null) {
                                Toast.makeText(context, "Renamed to $newTitle", Toast.LENGTH_SHORT).show()
                            }
                            renameItem = null
                        }
                    }
                ) {
                    Text("Save", color = ApexCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameItem = null }) {
                    Text("Cancel", color = ApexTextSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deleteCandidate?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete Recording?") },
            text = { Text("Are you sure you want to delete '${item.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            recordingsRepository.deleteRecording(item)
                            deleteCandidate = null
                            Toast.makeText(context, "Recording deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ApexRecordRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Cancel", color = ApexTextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ApexBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("gallery_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RECORDINGS GALLERY",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = ApexTextPrimary
                )
                Text(
                    text = "${recordings.size} saved recordings",
                    style = MaterialTheme.typography.labelSmall,
                    color = ApexTextSecondary
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh list", tint = ApexCyan)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (recordings.isEmpty()) {
            EmptyGalleryState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recordings, key = { it.id }) { item ->
                    RecordingCard(
                        item = item,
                        onPlay = { previewItem = item },
                        onTrim = { trimItem = item },
                        onShare = { shareVideo(context, File(item.filePath)) },
                        onRename = { renameItem = item },
                        onDelete = { deleteCandidate = item }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingCard(
    item: RecordingItem,
    onPlay: () -> Unit,
    onTrim: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("recording_card_${item.title}"),
        colors = CardDefaults.cardColors(containerColor = ApexSurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ApexTextPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val dateFormatted = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(Date(item.timestamp))
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = ApexTextMuted,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    color = ApexSurfaceElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.format,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ApexCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetaPill(icon = Icons.Default.Timer, text = item.formattedDuration())
                MetaPill(icon = Icons.Default.SdStorage, text = item.formattedSize())
                MetaPill(icon = Icons.Default.AspectRatio, text = "${item.width}×${item.height}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button
                IconButton(onClick = onPlay, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = ApexCyan, modifier = Modifier.size(20.dp))
                }

                // Trim Button
                IconButton(onClick = onTrim, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ContentCut, contentDescription = "Trim", tint = ApexEmerald, modifier = Modifier.size(18.dp))
                }

                // Share Button
                IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                // Rename Button
                IconButton(onClick = onRename, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = ApexTextSecondary, modifier = Modifier.size(18.dp))
                }

                // Delete Button
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ApexRecordRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun MetaPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        color = ApexSurfaceElevated,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ApexTextSecondary, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = ApexTextSecondary)
        }
    }
}

@Composable
private fun EmptyGalleryState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = ApexTextMuted,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Recordings Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ApexTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hit the big red Record button on the Dashboard to capture your first video session.",
                style = MaterialTheme.typography.bodySmall,
                color = ApexTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
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
        val uri = Uri.fromFile(file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share screen recording"))
    }
}
