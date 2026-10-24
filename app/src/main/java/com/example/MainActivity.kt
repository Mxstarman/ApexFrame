package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.RecordingsRepository
import com.example.data.SettingsRepository
import com.example.model.RecordingItem
import com.example.model.RecordingState
import com.example.service.ScreenRecorderManager
import com.example.ui.components.CountdownOverlay
import com.example.ui.components.PermissionsOnboardingDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexFrameTheme
import com.example.ui.theme.ApexRecordRed
import com.example.ui.theme.ApexSurfaceCard
import kotlinx.coroutines.launch

enum class ScreenTab {
    DASHBOARD,
    GALLERY,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var recordingsRepository: RecordingsRepository
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var appPrefs: SharedPreferences

    private var currentTab by mutableStateOf(ScreenTab.DASHBOARD)
    private var lastRecordedItem by mutableStateOf<RecordingItem?>(null)

    // Dynamic Permission States
    private var hasOverlayPermissionState by mutableStateOf(false)
    private var hasStoragePermissionState by mutableStateOf(false)
    private var hasAudioPermissionState by mutableStateOf(false)
    private var hasNotificationPermissionState by mutableStateOf(false)
    private var showPermissionsOnboarding by mutableStateOf(false)

    // MediaProjection screen capture authorization contract
    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            // Save token directly
            ScreenRecorderManager.setProjectionData(result.resultCode, result.data!!)
            val settings = settingsRepository.settings.value
            ScreenRecorderManager.startCountdownAndRecord(
                context = this,
                settings = settings,
                recordingsRepository = recordingsRepository
            ) { item ->
                lastRecordedItem = item
                runOnUiThread {
                    Toast.makeText(this, "Recording Saved: ${item.title}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "Screen recording permission was cancelled.", Toast.LENGTH_SHORT).show()
            ScreenRecorderManager.cancelCountdown()
        }
    }

    // Storage, Audio & Notification permissions contract
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        refreshPermissionStates()
        val allGranted = grants.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "Permissions granted successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = SettingsRepository(this)
        recordingsRepository = RecordingsRepository(this)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        appPrefs = getSharedPreferences("apex_app_state", Context.MODE_PRIVATE)
        ScreenRecorderManager.initialize(this)

        refreshPermissionStates()

        // Check if first launch or required permissions missing
        val hasSeenOnboarding = appPrefs.getBoolean("has_seen_permissions_onboarding", false)
        if (!hasSeenOnboarding || !hasOverlayPermissionState || !hasStoragePermissionState) {
            showPermissionsOnboarding = true
            appPrefs.edit().putBoolean("has_seen_permissions_onboarding", true).apply()
        }

        lifecycleScope.launch {
            recordingsRepository.loadRecordings()
        }

        setContent {
            ApexFrameTheme {
                val settings by settingsRepository.settings.collectAsStateWithLifecycle()
                val recordings by recordingsRepository.recordings.collectAsStateWithLifecycle()
                val recordingState by ScreenRecorderManager.recordingState.collectAsStateWithLifecycle()

                // If recording has actively begun and user chose minimizeOnRecord, send to back
                LaunchedEffect(recordingState) {
                    if (recordingState is RecordingState.Active && settings.minimizeOnRecord) {
                        moveTaskToBack(true)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color(0xFF090D16),
                        bottomBar = {
                            NavigationBar(
                                containerColor = ApexSurfaceCard,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .testTag("bottom_nav_bar")
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.DASHBOARD,
                                    onClick = { currentTab = ScreenTab.DASHBOARD },
                                    icon = { Icon(Icons.Default.Videocam, contentDescription = "Dashboard") },
                                    label = { Text("Recorder") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ApexRecordRed,
                                        selectedTextColor = ApexRecordRed,
                                        indicatorColor = ApexRecordRed.copy(alpha = 0.2f),
                                        unselectedIconColor = Color(0xFF94A3B8),
                                        unselectedTextColor = Color(0xFF94A3B8)
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.GALLERY,
                                    onClick = {
                                        currentTab = ScreenTab.GALLERY
                                        lifecycleScope.launch { recordingsRepository.loadRecordings() }
                                    },
                                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Gallery") },
                                    label = { Text("Gallery") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ApexCyan,
                                        selectedTextColor = ApexCyan,
                                        indicatorColor = ApexCyan.copy(alpha = 0.2f),
                                        unselectedIconColor = Color(0xFF94A3B8),
                                        unselectedTextColor = Color(0xFF94A3B8)
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.SETTINGS,
                                    onClick = { currentTab = ScreenTab.SETTINGS },
                                    icon = { Icon(Icons.Default.Tune, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ApexCyan,
                                        selectedTextColor = ApexCyan,
                                        indicatorColor = ApexCyan.copy(alpha = 0.2f),
                                        unselectedIconColor = Color(0xFF94A3B8),
                                        unselectedTextColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .windowInsetsPadding(WindowInsets.statusBars)
                        ) {
                            when (currentTab) {
                                ScreenTab.DASHBOARD -> {
                                    DashboardScreen(
                                        recordingState = recordingState,
                                        settings = settings,
                                        onStartRecording = { checkPermissionsAndStart() },
                                        onPauseResumeRecording = { ScreenRecorderManager.togglePauseResume() },
                                        onStopRecording = {
                                            ScreenRecorderManager.stopRecording(this@MainActivity, recordingsRepository)
                                        },
                                        onUpdateSettings = { settingsRepository.updateSettings(it) },
                                        onOpenOverlayPermissionSettings = { openOverlaySettings() }
                                    )
                                }
                                ScreenTab.GALLERY -> {
                                    GalleryScreen(
                                        recordings = recordings,
                                        recordingsRepository = recordingsRepository,
                                        onRefresh = { lifecycleScope.launch { recordingsRepository.loadRecordings() } }
                                    )
                                }
                                ScreenTab.SETTINGS -> {
                                    SettingsScreen(
                                        settings = settings,
                                        onUpdateSettings = { settingsRepository.updateSettings(it) },
                                        onOpenOverlayPermissionSettings = { openOverlaySettings() }
                                    )
                                }
                            }
                        }
                    }

                    // Pre-recording countdown visual overlay
                    AnimatedVisibility(
                        visible = recordingState is RecordingState.Countdown,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val countdown = recordingState as? RecordingState.Countdown
                        if (countdown != null) {
                            CountdownOverlay(
                                remainingSeconds = countdown.remainingSeconds,
                                totalSeconds = countdown.totalSeconds,
                                onCancel = { ScreenRecorderManager.cancelCountdown() }
                            )
                        }
                    }

                    // First Launch Permissions Dialog
                    if (showPermissionsOnboarding) {
                        PermissionsOnboardingDialog(
                            hasOverlayPermission = hasOverlayPermissionState,
                            hasStoragePermission = hasStoragePermissionState,
                            hasAudioPermission = hasAudioPermissionState,
                            hasNotificationPermission = hasNotificationPermissionState,
                            onRequestStorageAndAudio = { requestRuntimePermissions() },
                            onRequestOverlay = { openOverlaySettings() },
                            onDismiss = { showPermissionsOnboarding = false }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
    }

    private fun refreshPermissionStates() {
        hasOverlayPermissionState = Settings.canDrawOverlays(this)

        hasStoragePermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        hasAudioPermissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        hasNotificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestRuntimePermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Storage / Media
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasStoragePermissionState) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (!hasNotificationPermissionState) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (!hasStoragePermissionState) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // Audio
        if (!hasAudioPermissionState) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Toast.makeText(this, "Standard permissions already granted.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionsAndStart() {
        // If storage or audio permissions missing, request them
        val needsPermissions = mutableListOf<String>()

        if (!hasAudioPermissionState) {
            needsPermissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermissionState) {
                needsPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (!hasStoragePermissionState) {
                needsPermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (!hasStoragePermissionState) {
                needsPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (needsPermissions.isNotEmpty()) {
            permissionsLauncher.launch(needsPermissions.toTypedArray())
            return
        }

        // Check overlay permission
        if (!hasOverlayPermissionState && settingsRepository.settings.value.floatingControlBallEnabled) {
            Toast.makeText(this, "Granting overlay permission enables the floating control ball & drawing tools", Toast.LENGTH_LONG).show()
            openOverlaySettings()
            return
        }

        launchScreenCaptureIntent()
    }

    private fun launchScreenCaptureIntent() {
        try {
            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            projectionLauncher.launch(captureIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error requesting screen capture: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivity(fallbackIntent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("OPEN_TRIMMER_LAST", false)) {
            currentTab = ScreenTab.GALLERY
            lifecycleScope.launch {
                recordingsRepository.loadRecordings()
            }
        }
    }
}
