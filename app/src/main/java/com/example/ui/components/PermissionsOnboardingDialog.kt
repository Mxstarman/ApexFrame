package com.example.ui.components

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@Composable
fun PermissionsOnboardingDialog(
    hasOverlayPermission: Boolean,
    hasStoragePermission: Boolean,
    hasAudioPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestStorageAndAudio: () -> Unit,
    onRequestOverlay: () -> Unit,
    onDismiss: () -> Unit
) {
    val allGranted = hasOverlayPermission && hasStoragePermission && hasAudioPermission

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5f.dp, ApexBorder, RoundedCornerShape(24.dp))
                .testTag("permissions_onboarding_dialog"),
            color = ApexSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                brush = Brush.radialGradient(listOf(ApexRecordRed, Color(0xFF990022))),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Welcome to ApexFrame",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = ApexTextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Grant permissions to enable high-fps capture",
                            style = MaterialTheme.typography.labelSmall,
                            color = ApexTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Item 1: Overlay Permission (REQUIRED for Floating Ball & Annotations)
                PermissionItemCard(
                    title = "Display Over Other Apps (Overlay)",
                    description = "Enables the stealth floating control ball, countdown HUD, and screen drawing pen over other apps and games.",
                    isGranted = hasOverlayPermission,
                    buttonText = "Grant Overlay",
                    buttonColor = ApexCyan,
                    icon = Icons.Default.Layers,
                    onGrantClick = onRequestOverlay
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Item 2: Storage & Media Permission
                PermissionItemCard(
                    title = "Storage & Media Access",
                    description = "Required to save Full HD/4K videos to your device and allow the built-in trimmer and gallery to access clips.",
                    isGranted = hasStoragePermission,
                    buttonText = "Grant Storage",
                    buttonColor = ApexEmerald,
                    icon = Icons.Default.SdStorage,
                    onGrantClick = onRequestStorageAndAudio
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Item 3: Audio & Notifications
                PermissionItemCard(
                    title = "Microphone & Notifications",
                    description = "Captures your commentary alongside game audio, and provides ongoing recording controls in the status bar.",
                    isGranted = hasAudioPermission && hasNotificationPermission,
                    buttonText = "Grant Audio & Alerts",
                    buttonColor = ApexPurple,
                    icon = Icons.Default.Mic,
                    onGrantClick = onRequestStorageAndAudio
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Continue / Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("permissions_continue_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allGranted) ApexEmerald else ApexRecordRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (allGranted) "All Permissions Granted • Continue" else "Continue to ApexFrame",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    isGranted: Boolean,
    buttonText: String,
    buttonColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ApexSurfaceCard),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) ApexEmerald.copy(alpha = 0.5f) else ApexBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) ApexEmerald else buttonColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = ApexTextPrimary
                    )
                }

                Surface(
                    color = if (isGranted) ApexEmerald.copy(alpha = 0.2f) else Color(0x33FFB800),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isGranted) "GRANTED" else "REQUIRED",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) ApexEmerald else ApexAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = ApexTextSecondary
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = buttonText,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
