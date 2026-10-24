package com.example.ui.screens

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    settings: RecordingSettings,
    onUpdateSettings: (RecordingSettings) -> Unit,
    onOpenOverlayPermissionSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val hasOverlayPermission = remember { Settings.canDrawOverlays(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ApexBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_screen")
    ) {
        // Header
        Text(
            text = "RECORDING CONFIGURATION",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            color = ApexTextPrimary
        )
        Text(
            text = "Fine-tune capture resolution, stealth rendering, and audio matrix",
            style = MaterialTheme.typography.labelSmall,
            color = ApexTextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 1: VIDEO SPECIFICATIONS
        SettingsGroupCard(title = "Video Engine & Display", icon = Icons.Default.Videocam) {
            // Resolution
            Text("Resolution", style = MaterialTheme.typography.labelMedium, color = ApexTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ResolutionOption.values().forEach { res ->
                    SelectableRow(
                        title = res.label,
                        subtitle = res.description,
                        isSelected = settings.resolution == res,
                        onSelect = { onUpdateSettings(settings.copy(resolution = res)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Framerate (FPS)
            Text("Frame Rate (FPS)", style = MaterialTheme.typography.labelMedium, color = ApexTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FpsOption.values().forEach { fps ->
                    ChoiceChip(
                        label = fps.label.split(" ").first() + " FPS",
                        isSelected = settings.fps == fps,
                        modifier = Modifier.weight(1f),
                        onClick = { onUpdateSettings(settings.copy(fps = fps)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Video Bitrate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto-Optimized Bitrate", style = MaterialTheme.typography.bodySmall, color = ApexTextPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        if (settings.isAutoBitrate) "Dynamically adjusted (~${settings.calculateEffectiveBitrate() / 1_000_000} Mbps)" else "Manual: ${settings.bitrateMbps} Mbps",
                        style = MaterialTheme.typography.labelSmall,
                        color = ApexCyan
                    )
                }
                Switch(
                    checked = settings.isAutoBitrate,
                    onCheckedChange = { onUpdateSettings(settings.copy(isAutoBitrate = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = ApexCyan, checkedTrackColor = ApexCyan.copy(alpha = 0.3f))
                )
            }

            if (!settings.isAutoBitrate) {
                Slider(
                    value = settings.bitrateMbps.toFloat(),
                    onValueChange = { onUpdateSettings(settings.copy(bitrateMbps = it.toInt())) },
                    valueRange = 2f..50f,
                    colors = SliderDefaults.colors(thumbColor = ApexCyan, activeTrackColor = ApexCyan)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Container Format & Codec
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Container", style = MaterialTheme.typography.labelSmall, color = ApexTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VideoFormatOption.values().forEach { fmt ->
                            ChoiceChip(
                                label = fmt.extension.uppercase(),
                                isSelected = settings.videoFormat == fmt,
                                modifier = Modifier.weight(1f),
                                onClick = { onUpdateSettings(settings.copy(videoFormat = fmt)) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Encoder", style = MaterialTheme.typography.labelSmall, color = ApexTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VideoCodecOption.values().forEach { codec ->
                            ChoiceChip(
                                label = codec.label.split(" ").first(),
                                isSelected = settings.videoCodec == codec,
                                modifier = Modifier.weight(1f),
                                onClick = { onUpdateSettings(settings.copy(videoCodec = codec)) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: AUDIO CONFIGURATION
        SettingsGroupCard(title = "Audio Matrix", icon = Icons.Default.GraphicEq) {
            Text("Audio Source", style = MaterialTheme.typography.labelMedium, color = ApexTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AudioSourceOption.values().forEach { src ->
                    SelectableRow(
                        title = src.label,
                        subtitle = src.description,
                        isSelected = settings.audioSource == src,
                        onSelect = { onUpdateSettings(settings.copy(audioSource = src)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: PRE-RECORDING COUNTDOWN & STEALTH OVERLAY
        SettingsGroupCard(title = "Stealth Overlay & Controls", icon = Icons.Default.Security) {
            // Stealth Mode Toggle (Invisible in final video)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Clean Video Capture Mode",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = ApexCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("NO BLACK COVERS", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = ApexCyan)
                        }
                    }
                    Text(
                        text = "Prevents black lines or black covered areas in the output video. Floating controls automatically tuck to the screen edge or can be hidden completely in favor of the Quick Panel tile.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = ApexTextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = settings.stealthOverlayEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(stealthOverlayEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = ApexCyan, checkedTrackColor = ApexCyan.copy(alpha = 0.3f))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Panel Guide Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ApexSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Quick Panel",
                        tint = ApexCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quick Panel Tile Active",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary
                        )
                        Text(
                            text = "Swipe down twice from top of screen > tap Edit > drag 'Apex Record' into active tiles to start capture anywhere without opening the app.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = ApexTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pre-Recording Countdown
            Text("Pre-Recording Countdown", style = MaterialTheme.typography.labelMedium, color = ApexTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 3, 5, 10).forEach { secs ->
                    ChoiceChip(
                        label = if (secs == 0) "None" else "${secs}s",
                        isSelected = settings.countdownSeconds == secs,
                        modifier = Modifier.weight(1f),
                        onClick = { onUpdateSettings(settings.copy(countdownSeconds = secs)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Floating Ball Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Floating Control Ball", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ApexTextPrimary)
                    Text("Draggable overlay on screen with pause, stop, and annotation pen tools.", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = ApexTextSecondary)
                }
                Switch(
                    checked = settings.floatingControlBallEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(floatingControlBallEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = ApexCyan, checkedTrackColor = ApexCyan.copy(alpha = 0.3f))
                )
            }

            // Overlay Permission Check
            if (!hasOverlayPermission) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenOverlayPermissionSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = ApexAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant 'Display Over Other Apps' Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Minimize on start
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Minimize App on Start", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ApexTextPrimary)
                    Text("Hides the main window when recording begins so you can capture other apps immediately.", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = ApexTextSecondary)
                }
                Switch(
                    checked = settings.minimizeOnRecord,
                    onCheckedChange = { onUpdateSettings(settings.copy(minimizeOnRecord = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = ApexCyan, checkedTrackColor = ApexCyan.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 4: STORAGE & AUTOMATION
        SettingsGroupCard(title = "Storage & Naming", icon = Icons.Default.Folder) {
            Text("Automatic File Pattern", style = MaterialTheme.typography.labelSmall, color = ApexTextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = ApexSurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ApexFrame_YYYY-MM-DD_HH-mm-ss.${settings.videoFormat.extension}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = ApexCyan,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Output Location", style = MaterialTheme.typography.labelSmall, color = ApexTextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)}/ApexFrame",
                style = MaterialTheme.typography.bodySmall,
                color = ApexTextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ApexSurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ApexTextPrimary)
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SelectableRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        color = if (isSelected) ApexSurfaceElevated else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) ApexCyan else ApexBorder.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = ApexCyan, unselectedColor = Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ApexTextPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = ApexTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (isSelected) ApexCyan else ApexSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ApexCyan else ApexBorder)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.Black else ApexTextPrimary
            )
        }
    }
}
