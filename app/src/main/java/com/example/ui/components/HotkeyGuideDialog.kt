package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun HotkeyGuideDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, ApexBorder, RoundedCornerShape(20.dp))
                .testTag("hotkey_guide_dialog"),
            color = ApexSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Hotkeys",
                            tint = ApexCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Shortcuts & Gestures",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ApexTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HotkeyRow(
                    label = "Quick Panel Tile (1-Tap)",
                    trigger = "Apex Record in Notification Shade"
                )

                Spacer(modifier = Modifier.height(10.dp))

                HotkeyRow(
                    label = "Start / Stop Recording",
                    trigger = "Quick Tile / Notification / Recorder Cockpit"
                )

                Spacer(modifier = Modifier.height(10.dp))

                HotkeyRow(
                    label = "Pause / Resume Recording",
                    trigger = "Floating Ball / Notification Pause"
                )

                Spacer(modifier = Modifier.height(10.dp))

                HotkeyRow(
                    label = "Open / Hide Floating Ball",
                    trigger = "Tap ball to expand • Drag to screen edge"
                )

                Spacer(modifier = Modifier.height(10.dp))

                HotkeyRow(
                    label = "Screen Drawing / Annotate",
                    trigger = "Pen Icon in Floating Menu"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Clean capture explanation card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ApexSurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Clean Capture",
                            tint = ApexCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Clean Video Architecture",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ApexCyan
                            )
                            Text(
                                text = "No black covers or black lines in your videos. Use the Quick Panel tile or status bar notification for 100% overlay-free video captures.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ApexTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hotkey_guide_done_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ApexCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Got It", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun HotkeyRow(label: String, trigger: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ApexSurfaceElevated)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ApexTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = trigger,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = ApexCyan,
            fontWeight = FontWeight.Bold
        )
    }
}
