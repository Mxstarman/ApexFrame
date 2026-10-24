package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.example.R
import com.example.TileRecordingActivity
import com.example.data.RecordingsRepository
import com.example.model.RecordingState
import kotlinx.coroutines.*

/**
 * Quick Settings Tile allowing users to start and stop screen recording directly
 * from the system quick panel without opening the main app interface.
 */
class ScreenRecordTileService : TileService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stateJob: Job? = null

    companion object {
        private const val TAG = "ScreenRecordTileService"
    }

    override fun onStartListening() {
        super.onStartListening()
        stateJob?.cancel()
        stateJob = serviceScope.launch {
            ScreenRecorderManager.recordingState.collect { state ->
                val isRecording = state is RecordingState.Active || state is RecordingState.Countdown
                updateTile(isRecording)
            }
        }
    }

    override fun onStopListening() {
        stateJob?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        try {
            val currentState = ScreenRecorderManager.recordingState.value
            if (currentState is RecordingState.Active || currentState is RecordingState.Countdown) {
                // Stop active recording
                val repo = RecordingsRepository(applicationContext)
                ScreenRecorderManager.stopRecording(applicationContext, repo)
                updateTile(false)
            } else {
                // Launch instant recording from tile
                launchTileRecordingActivity()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling quick settings tile click", e)
            updateTile(false)
        }
    }

    private fun launchTileRecordingActivity() {
        val intent = Intent(this, TileRecordingActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to collapse quick settings and start recording", e)
        }
    }

    private fun updateTile(isRecording: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isRecording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isRecording) "Stop Capture" else "Apex Record"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isRecording) "Recording live" else "1-tap screen capture"
        }
        tile.icon = Icon.createWithResource(
            this,
            if (isRecording) android.R.drawable.ic_media_pause else R.drawable.ic_quick_record
        )
        tile.updateTile()
    }

    override fun onDestroy() {
        stateJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
