package com.example.ui.screens

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.service.ScreenRecorderManager
import com.example.ui.components.AudioMixerCard
import com.example.ui.components.HotkeyGuideDialog
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    recordingState: RecordingState,
    settings: RecordingSettings,
    onStartRecording: () -> Unit,
    onPauseResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onUpdateSettings: (RecordingSettings) -> Unit,
    onOpenOverlayPermissionSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showHotkeyGuide by remember { mutableStateOf(false) }

    val hasOverlayPermission = remember { Settings.canDrawOverlays(context) }

    if (showHotkeyGuide) {
        HotkeyGuideDialog(onDismiss = { showHotkeyGuide = false })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ApexBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("dashboard_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "APEX FRAME",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = ApexTextPrimary
                )
                Text(
                    text = "Ultra-Low Latency Screen Capture",
                    style = MaterialTheme.typography.labelSmall,
                    color = ApexTextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Clean Capture Badge
                Surface(
                    color = if (settings.stealthOverlayEnabled) ApexSurfaceElevated else Color(0xFF261C14),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (settings.stealthOverlayEnabled) ApexCyan else ApexAmber
                    ),
                    modifier = Modifier.clickable {
                        onUpdateSettings(settings.copy(stealthOverlayEnabled = !settings.stealthOverlayEnabled))
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Clean Capture Mode",
                            tint = if (settings.stealthOverlayEnabled) ApexCyan else ApexAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (settings.stealthOverlayEnabled) "CLEAN CAPTURE" else "STANDARD",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (settings.stealthOverlayEnabled) ApexCyan else ApexAmber
                        )
                    }
                }

            }
        }

        // Overlay Permission Warning Banner if not granted
        if (!hasOverlayPermission && settings.floatingControlBallEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenOverlayPermissionSettings() },
                color = Color(0x33FFB800),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexAmber)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Permission needed",
                        tint = ApexAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Overlay Permission Required",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ApexAmber
                        )
                        Text(
                            text = "Tap to enable floating ball & annotation tools over other apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = ApexTextPrimary,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Settings",
                        tint = ApexAmber
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // HERO SECTION: Active Cockpit or Big Record Trigger
        when (recordingState) {
            is RecordingState.Active -> {
                ActiveRecordingCockpit(
                    state = recordingState,
                    onPauseResume = onPauseResumeRecording,
                    onStop = onStopRecording
                )
            }
            is RecordingState.Countdown -> {
                // Handled via full overlay or inline preview
                CountdownCard(countdown = recordingState)
            }
            is RecordingState.Processing -> {
                ProcessingCard(message = recordingState.message)
            }
            is RecordingState.Error -> {
                ErrorCard(
                    errorMessage = recordingState.message,
                    onRetry = onStartRecording,
                    onDismiss = { ScreenRecorderManager.resetToIdle() }
                )
            }
            else -> {
                IdleRecordLauncher(
                    settings = settings,
                    onStartClick = onStartRecording
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Toggles Bar
        QuickSettingsRow(
            settings = settings,
            onUpdateSettings = onUpdateSettings
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Audio Mixer Card
        val liveMic = if (recordingState is RecordingState.Active) recordingState.micPeakLevel else 0.05f
        val liveInternal = if (recordingState is RecordingState.Active) recordingState.internalPeakLevel else 0.05f
        AudioMixerCard(
            audioSource = settings.audioSource,
            micVolume = settings.micVolume,
            internalVolume = settings.internalVolume,
            noiseSuppression = settings.noiseSuppression,
            liveMicPeak = liveMic,
            liveInternalPeak = liveInternal,
            onMicVolumeChange = { onUpdateSettings(settings.copy(micVolume = it)) },
            onInternalVolumeChange = { onUpdateSettings(settings.copy(internalVolume = it)) },
            onNoiseSuppressionChange = { onUpdateSettings(settings.copy(noiseSuppression = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Technical Summary Footer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ApexSurfaceCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Active Recording Profile",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ApexTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SpecItem(label = "Format", value = "${settings.videoFormat.extension.uppercase()} (${settings.videoCodec.label})")
                    SpecItem(label = "Bitrate", value = if (settings.isAutoBitrate) "Auto (~${settings.calculateEffectiveBitrate() / 1_000_000} Mbps)" else "${settings.bitrateMbps} Mbps")
                    SpecItem(label = "Countdown", value = if (settings.countdownSeconds == 0) "Instant" else "${settings.countdownSeconds}s")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun IdleRecordLauncher(
    settings: RecordingSettings,
    onStartClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(185.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(ApexRecordRed.copy(alpha = 0.20f), Color.Transparent)
                    )
                )
                .border(
                    width = 3.dp,
                    brush = Brush.sweepGradient(listOf(ApexRecordRed, ApexCyan, ApexRecordRed)),
                    shape = CircleShape
                )
                .clickable { onStartClick() }
                .testTag("start_recording_button"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFE11D48), Color(0xFF9F1239))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = "Record",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "RECORD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "${settings.resolution.label} • ${settings.fps.label}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = ApexCyan
        )
    }
}

@Composable
private fun ActiveRecordingCockpit(
    state: RecordingState.Active,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_recording_cockpit"),
        colors = CardDefaults.cardColors(containerColor = ApexSurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.5f.dp, if (state.isPaused) ApexAmber else ApexRecordRed)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (state.isPaused) ApexAmber else ApexRecordRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isPaused) "RECORDING PAUSED" else "LIVE CAPTURE ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isPaused) ApexAmber else ApexRecordRed
                    )
                }

                val mb = state.bytesWritten / (1024.0 * 1024.0)
                Text(
                    text = String.format("%.1f MB written", mb),
                    style = MaterialTheme.typography.labelSmall,
                    color = ApexTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Giant Duration Monospace Clock
            Text(
                text = state.formattedDuration(),
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Pause / Resume
                OutlinedButton(
                    onClick = onPauseResume,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cockpit_pause_resume_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (state.isPaused) ApexEmerald else ApexAmber
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (state.isPaused) ApexEmerald else ApexAmber)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (state.isPaused) "Resume" else "Pause"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (state.isPaused) "Resume" else "Pause")
                }

                // Stop & Save
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("cockpit_stop_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ApexRecordRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop recording")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop & Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CountdownCard(countdown: RecordingState.Countdown) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ApexSurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Starting in ${countdown.remainingSeconds}...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ApexCyan
            )
        }
    }
}

@Composable
private fun ProcessingCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ApexSurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = ApexCyan, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = ApexTextPrimary)
        }
    }
}

@Composable
private fun QuickSettingsRow(
    settings: RecordingSettings,
    onUpdateSettings: (RecordingSettings) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Resolution Quick Toggle Chip
        QuickPill(
            icon = Icons.Default.AspectRatio,
            label = settings.resolution.label.split(" ").first(),
            onClick = {
                val next = when (settings.resolution) {
                    ResolutionOption.SD_720P -> ResolutionOption.FHD_1080P
                    ResolutionOption.FHD_1080P -> ResolutionOption.UHD_4K
                    ResolutionOption.UHD_4K -> ResolutionOption.SD_720P
                    ResolutionOption.NATIVE_DISPLAY -> ResolutionOption.FHD_1080P
                }
                onUpdateSettings(settings.copy(resolution = next))
            },
            modifier = Modifier.weight(1f)
        )

        // FPS Quick Toggle Chip
        QuickPill(
            icon = Icons.Default.Speed,
            label = "${settings.fps.fps} FPS",
            onClick = {
                val next = when (settings.fps) {
                    FpsOption.FPS_30 -> FpsOption.FPS_60
                    FpsOption.FPS_60 -> FpsOption.FPS_90
                    FpsOption.FPS_90 -> FpsOption.FPS_120
                    FpsOption.FPS_120 -> FpsOption.FPS_30
                }
                onUpdateSettings(settings.copy(fps = next))
            },
            modifier = Modifier.weight(1f)
        )

        // Audio Source Quick Toggle Chip
        QuickPill(
            icon = Icons.Default.Mic,
            label = when (settings.audioSource) {
                AudioSourceOption.NONE -> "Mute"
                AudioSourceOption.MIC -> "Mic"
                AudioSourceOption.INTERNAL -> "Internal"
                AudioSourceOption.DUAL_MIXED -> "Dual"
            },
            onClick = {
                val next = when (settings.audioSource) {
                    AudioSourceOption.NONE -> AudioSourceOption.MIC
                    AudioSourceOption.MIC -> AudioSourceOption.INTERNAL
                    AudioSourceOption.INTERNAL -> AudioSourceOption.DUAL_MIXED
                    AudioSourceOption.DUAL_MIXED -> AudioSourceOption.NONE
                }
                onUpdateSettings(settings.copy(audioSource = next))
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = ApexSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = ApexCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ApexTextPrimary)
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = ApexTextMuted, fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = ApexTextPrimary)
    }
}

@Composable
private fun ErrorCard(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error_card"),
        colors = CardDefaults.cardColors(containerColor = ApexSurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5f.dp, ApexRecordRed)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = ApexRecordRed,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Capture Failed to Start",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ApexRecordRed
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = ApexTextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ApexTextSecondary)
                ) {
                    Text("Dismiss")
                }
                Button(
                    onClick = {
                        onDismiss()
                        onRetry()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ApexRecordRed)
                ) {
                    Text("Retry")
                }
            }
        }
    }
}
