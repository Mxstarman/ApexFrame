package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.data.RecordingsRepository
import com.example.data.SettingsRepository
import com.example.service.ScreenRecorderManager

/**
 * Transparent helper activity launched from ScreenRecordTileService in the Quick Settings panel.
 * Prompts screen capture consent directly without launching the main dashboard,
 * starts recording, and immediately finishes.
 */
class TileRecordingActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var recordingsRepository: RecordingsRepository
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenRecorderManager.setProjectionData(result.resultCode, result.data!!)
            val settings = settingsRepository.settings.value
            // Launch recording immediately without pre-recording countdown delay when triggered from Quick Panel tile
            ScreenRecorderManager.startCountdownAndRecord(
                context = applicationContext,
                settings = settings.copy(countdownSeconds = 0),
                recordingsRepository = recordingsRepository
            ) { item ->
                runOnUiThread {
                    Toast.makeText(applicationContext, "Saved: ${item.title}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Screen capture was cancelled.", Toast.LENGTH_SHORT).show()
            ScreenRecorderManager.cancelCountdown()
        }
        finishAndRemoveTask()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        launchCaptureIntent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setDimAmount(0f)
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        settingsRepository = SettingsRepository(this)
        recordingsRepository = RecordingsRepository(this)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        ScreenRecorderManager.initialize(this)

        checkPermissionsAndProceed()
    }

    private fun checkPermissionsAndProceed() {
        val missingPermissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            launchCaptureIntent()
        }
    }

    private fun launchCaptureIntent() {
        try {
            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            projectionLauncher.launch(captureIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to request capture: ${e.message}", Toast.LENGTH_SHORT).show()
            finishAndRemoveTask()
        }
    }
}
