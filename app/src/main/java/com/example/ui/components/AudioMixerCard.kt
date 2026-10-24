package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioSourceOption
import com.example.ui.theme.*

@Composable
fun AudioMixerCard(
    audioSource: AudioSourceOption,
    micVolume: Float,
    internalVolume: Float,
    noiseSuppression: Boolean,
    liveMicPeak: Float,
    liveInternalPeak: Float,
    onMicVolumeChange: (Float) -> Unit,
    onInternalVolumeChange: (Float) -> Unit,
    onNoiseSuppressionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_mixer_card"),
        colors = CardDefaults.cardColors(containerColor = ApexSurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Audio Mixer",
                        tint = ApexCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Audio Mixer & Levels",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ApexTextPrimary
                    )
                }

                // Noise suppression chip/toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AI Noise Suppress",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (noiseSuppression) ApexEmerald else ApexTextMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = noiseSuppression,
                        onCheckedChange = onNoiseSuppressionChange,
                        modifier = Modifier
                            .scale(0.8f)
                            .testTag("noise_suppression_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ApexEmerald,
                            checkedTrackColor = ApexEmerald.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Microphone Section
            val micEnabled = audioSource == AudioSourceOption.MIC || audioSource == AudioSourceOption.DUAL_MIXED
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ApexSurfaceElevated)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (micEnabled) ApexRecordRed else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Microphone Gain",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (micEnabled) ApexTextPrimary else ApexTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${(micVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (micEnabled) ApexCyan else ApexTextMuted
                    )
                }

                Slider(
                    value = micVolume,
                    onValueChange = onMicVolumeChange,
                    valueRange = 0.0f..1.5f,
                    enabled = micEnabled,
                    modifier = Modifier.testTag("mic_volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = ApexRecordRed,
                        activeTrackColor = ApexRecordRed,
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                // Live Audio Level Visualizer
                LiveLevelMeter(peakLevel = if (micEnabled) liveMicPeak else 0f, activeColor = ApexRecordRed)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Internal System Audio Section
            val internalEnabled = audioSource == AudioSourceOption.INTERNAL || audioSource == AudioSourceOption.DUAL_MIXED
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ApexSurfaceElevated)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Internal Audio",
                            tint = if (internalEnabled) ApexCyan else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Internal System Audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (internalEnabled) ApexTextPrimary else ApexTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${(internalVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (internalEnabled) ApexCyan else ApexTextMuted
                    )
                }

                Slider(
                    value = internalVolume,
                    onValueChange = onInternalVolumeChange,
                    valueRange = 0.0f..1.5f,
                    enabled = internalEnabled,
                    modifier = Modifier.testTag("internal_volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = ApexCyan,
                        activeTrackColor = ApexCyan,
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                // Live Internal Audio Level Visualizer
                LiveLevelMeter(peakLevel = if (internalEnabled) liveInternalPeak else 0f, activeColor = ApexCyan)
            }
        }
    }
}

@Composable
private fun LiveLevelMeter(peakLevel: Float, activeColor: Color) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val barCount = 18
        val spacing = 2.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
        val activeBars = (peakLevel * barCount).toInt().coerceIn(0, barCount)

        for (i in 0 until barCount) {
            val isActive = i <= activeBars
            val color = if (isActive) {
                if (i > barCount * 0.8f) ApexRecordRed else activeColor
            } else {
                Color(0xFF1E293B)
            }
            val startX = i * (barWidth + spacing)
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(startX, 0f),
                size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                cornerRadius = cornerRadius
            )
        }
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(Modifier.size(40.dp))
