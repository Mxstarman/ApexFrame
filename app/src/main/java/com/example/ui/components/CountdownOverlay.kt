package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexRecordRed

@Composable
fun CountdownOverlay(
    remainingSeconds: Int,
    totalSeconds: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE60A0F1D))
            .testTag("countdown_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PREPARING CAPTURE",
                style = MaterialTheme.typography.labelLarge,
                color = ApexCyan,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Glowing Animated Circular Countdown Ring
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(ApexRecordRed.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 4.dp,
                        brush = Brush.sweepGradient(listOf(ApexRecordRed, ApexCyan, ApexRecordRed)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = remainingSeconds.toString(),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "App will minimize when recording begins",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.testTag("countdown_cancel_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF475569), Color(0xFF64748B)))
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel countdown", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel Countdown")
            }
        }
    }
}
