package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.RecordingsRepository
import com.example.model.RecordingState
import kotlinx.coroutines.*

class ScreenRecordService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stateCollectJob: Job? = null

    companion object {
        private const val TAG = "ScreenRecordService"
        const val CHANNEL_ID = "apex_screen_record_channel"
        const val NOTIFICATION_ID = 8801

        const val ACTION_START_RECORDING = "com.example.service.ACTION_START_RECORDING"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_PAUSE_RESUME = "com.example.service.ACTION_PAUSE_RESUME"

        fun startServiceWithProjection(context: Context) {
            val intent = Intent(context, ScreenRecordService::class.java).apply {
                action = ACTION_START_RECORDING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ScreenRecordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                Log.d(TAG, "onStartCommand: ACTION_START_RECORDING received")
                val notification = buildNotification("ApexFrame is recording your screen...", "00:00", false)

                // CRITICAL ANDROID 14+ (API 34+) REQUIREMENT:
                // startForeground() with FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION MUST be called
                // BEFORE mediaProjectionManager.getMediaProjection() is invoked!
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val types = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    }
                    startForeground(NOTIFICATION_ID, notification, types)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                observeRecordingState()

                // Now notify ScreenRecorderManager to execute MediaProjection capture safely
                ScreenRecorderManager.onForegroundServiceReady(applicationContext)
            }
            ACTION_STOP -> {
                val repo = RecordingsRepository(applicationContext)
                ScreenRecorderManager.stopRecording(applicationContext, repo)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_PAUSE_RESUME -> {
                ScreenRecorderManager.togglePauseResume()
            }
        }
        return START_STICKY
    }

    private fun observeRecordingState() {
        stateCollectJob?.cancel()
        stateCollectJob = serviceScope.launch {
            ScreenRecorderManager.recordingState.collect { state ->
                when (state) {
                    is RecordingState.Active -> {
                        val notification = buildNotification(
                            contentTitle = if (state.isPaused) "Recording Paused" else "Recording in Progress",
                            timeText = state.formattedDuration(),
                            isPaused = state.isPaused
                        )
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }
                    is RecordingState.Idle, is RecordingState.Finished, is RecordingState.Error -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun buildNotification(contentTitle: String, timeText: String, isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ACTION_PAUSE_RESUME
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this, 1, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText("Elapsed: $timeText • ApexFrame")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pauseResumePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ApexFrame Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status and controls for active screen recording"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stateCollectJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
