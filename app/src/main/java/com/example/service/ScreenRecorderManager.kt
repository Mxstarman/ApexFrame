package com.example.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.data.RecordingsRepository
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object ScreenRecorderManager {
    private const val TAG = "ScreenRecorderManager"

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var countdownJob: Job? = null

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null

    private var currentOutputFile: File? = null
    private var projectionResultCode: Int = Activity.RESULT_CANCELED
    private var projectionResultData: Intent? = null

    private var recordStartTime: Long = 0L
    private var pausedDuration: Long = 0L
    private var pauseStartTime: Long = 0L
    private var isPaused: Boolean = false

    var currentSettings: RecordingSettings = RecordingSettings()
    private var currentRecordingsRepo: RecordingsRepository? = null
    private var onFinishedCallback: ((RecordingItem) -> Unit)? = null

    fun initialize(context: Context) {
        mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    fun setProjectionData(resultCode: Int, data: Intent) {
        this.projectionResultCode = resultCode
        this.projectionResultData = data
    }

    fun hasProjectionPermission(): Boolean {
        return projectionResultCode == Activity.RESULT_OK && projectionResultData != null
    }

    fun startCountdownAndRecord(
        context: Context,
        settings: RecordingSettings,
        recordingsRepository: RecordingsRepository,
        onFinished: (RecordingItem) -> Unit
    ) {
        if (!hasProjectionPermission()) {
            postToast(context, "Media projection authorization not granted.")
            _recordingState.value = RecordingState.Error("Media projection permission not granted.")
            return
        }

        currentSettings = settings
        currentRecordingsRepo = recordingsRepository
        onFinishedCallback = onFinished

        val countdownSecs = settings.countdownSeconds
        if (countdownSecs <= 0) {
            triggerForegroundRecordingService(context)
        } else {
            countdownJob?.cancel()
            countdownJob = scope.launch {
                for (i in countdownSecs downTo 1) {
                    _recordingState.value = RecordingState.Countdown(remainingSeconds = i, totalSeconds = countdownSecs)
                    delay(1000L)
                }
                triggerForegroundRecordingService(context)
            }
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _recordingState.value = RecordingState.Idle
    }

    private fun triggerForegroundRecordingService(context: Context) {
        _recordingState.value = RecordingState.Processing(0.1f, "Initializing capture service...")
        // Start foreground service first so Android 14+ accepts getMediaProjection()
        ScreenRecordService.startServiceWithProjection(context)
    }

    /**
     * Called by ScreenRecordService immediately after startForeground() has been called.
     * Guarantees Android 14+ foreground service requirement is satisfied.
     */
    fun onForegroundServiceReady(context: Context) {
        scope.launch {
            try {
                executeStartRecording(context)
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error executing recording", e)
                postToast(context, "Recording failed to start: ${e.message}")
                _recordingState.value = RecordingState.Error("Failed to start: ${e.localizedMessage ?: "Unknown"}")
                cleanup()
                ScreenRecordService.stopService(context)
            }
        }
    }

    private fun executeStartRecording(context: Context) {
        val repo = currentRecordingsRepo ?: RecordingsRepository(context)
        val settings = currentSettings

        val extension = settings.videoFormat.extension
        val outputFile = repo.generateNewOutputFile(extension)
        currentOutputFile = outputFile

        // Get MediaProjection token
        val resultData = projectionResultData
        if (resultData == null || projectionResultCode != Activity.RESULT_OK) {
            throw IllegalStateException("Missing media projection token or canceled by user.")
        }

        val mp = mediaProjectionManager?.getMediaProjection(projectionResultCode, resultData)
            ?: throw IllegalStateException("Failed to obtain MediaProjection session from system.")
        mediaProjection = mp

        // Register projection callback
        mp.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d(TAG, "MediaProjection session stopped by system")
                stopRecording(context, repo)
            }
        }, Handler(Looper.getMainLooper()))

        // Screen dimensions calculation
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val screenDensity = metrics.densityDpi

        // Determine target capture dimensions aligned to 16
        var targetWidth = when (settings.resolution) {
            ResolutionOption.SD_720P -> 720
            ResolutionOption.FHD_1080P -> 1080
            ResolutionOption.UHD_4K -> 2160
            ResolutionOption.NATIVE_DISPLAY -> minOf(screenWidth, screenHeight)
        }
        // Cap to physical display dimensions to avoid encoder rejections
        targetWidth = minOf(targetWidth, minOf(screenWidth, screenHeight))
        targetWidth = (targetWidth / 16) * 16
        if (targetWidth <= 0) targetWidth = 720

        val aspectRatio = screenHeight.toFloat() / screenWidth.toFloat()
        var targetHeight = (targetWidth * aspectRatio).toInt()
        targetHeight = (targetHeight / 16) * 16
        if (targetHeight <= 0) targetHeight = 1280

        val hasAudioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val shouldRecordAudio = hasAudioPermission && settings.audioSource != AudioSourceOption.NONE

        // Setup MediaRecorder with multi-tiered fallback to guarantee smooth execution on any device
        val mr = createMediaRecorder(context)
        mediaRecorder = mr

        var prepared = trySetupAndPrepareMediaRecorder(
            mr = mr,
            outputFile = outputFile,
            width = targetWidth,
            height = targetHeight,
            fps = settings.fps.fps,
            bitrate = settings.calculateEffectiveBitrate(),
            recordAudio = shouldRecordAudio
        )

        if (!prepared && shouldRecordAudio) {
            Log.w(TAG, "Primary configuration with audio failed. Retrying without audio...")
            mr.reset()
            prepared = trySetupAndPrepareMediaRecorder(
                mr = mr,
                outputFile = outputFile,
                width = targetWidth,
                height = targetHeight,
                fps = settings.fps.fps,
                bitrate = settings.calculateEffectiveBitrate(),
                recordAudio = false
            )
        }

        if (!prepared && settings.fps.fps > 30) {
            Log.w(TAG, "High framerate encoding failed. Retrying at 30 FPS...")
            mr.reset()
            prepared = trySetupAndPrepareMediaRecorder(
                mr = mr,
                outputFile = outputFile,
                width = targetWidth,
                height = targetHeight,
                fps = 30,
                bitrate = minOf(settings.calculateEffectiveBitrate(), 10_000_000),
                recordAudio = shouldRecordAudio
            )
        }

        if (!prepared) {
            Log.w(TAG, "Standard profile failed. Retrying with universal 720p 30 FPS fallback...")
            mr.reset()
            targetWidth = 720
            targetHeight = (720 * (screenHeight.toFloat() / screenWidth.toFloat())).toInt()
            targetHeight = (targetHeight / 16) * 16
            prepared = trySetupAndPrepareMediaRecorder(
                mr = mr,
                outputFile = outputFile,
                width = targetWidth,
                height = targetHeight,
                fps = 30,
                bitrate = 5_000_000,
                recordAudio = false
            )
        }

        if (!prepared) {
            throw IllegalStateException("Device hardware video encoder is incompatible with selected profiles.")
        }

        // Create VirtualDisplay
        virtualDisplay = mp.createVirtualDisplay(
            "ApexFrameVirtualDisplay",
            targetWidth,
            targetHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mr.surface,
            null,
            null
        )

        mr.start()
        recordStartTime = System.currentTimeMillis()
        pausedDuration = 0L
        isPaused = false

        _recordingState.value = RecordingState.Active(
            durationMs = 0L,
            bytesWritten = 0L,
            isPaused = false,
            outputFilePath = outputFile.absolutePath
        )

        // Launch floating control ball if enabled
        if (settings.floatingControlBallEnabled) {
            FloatingBallService.startService(context, settings.stealthOverlayEnabled)
        }

        // Start duration timer loop
        startTimer(outputFile)
        Log.i(TAG, "Recording started successfully: ${outputFile.name} (${targetWidth}x${targetHeight})")
    }

    private fun trySetupAndPrepareMediaRecorder(
        mr: MediaRecorder,
        outputFile: File,
        width: Int,
        height: Int,
        fps: Int,
        bitrate: Int,
        recordAudio: Boolean
    ): Boolean {
        return try {
            if (recordAudio) {
                mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            mr.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setVideoSize(width, height)
            mr.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (recordAudio) {
                mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mr.setAudioEncodingBitRate(128000)
                mr.setAudioSamplingRate(44100)
            }
            mr.setVideoEncodingBitRate(bitrate)
            mr.setVideoFrameRate(fps)
            mr.setOutputFile(outputFile.absolutePath)
            mr.prepare()
            true
        } catch (e: Exception) {
            Log.e(TAG, "trySetupAndPrepareMediaRecorder failed: ${width}x${height}@${fps}fps $bitrate bps audio=$recordAudio", e)
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(context: Context): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    private fun startTimer(file: File) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(200L)
                if (!isPaused) {
                    val currentElapsed = System.currentTimeMillis() - recordStartTime - pausedDuration
                    val currentBytes = file.length()
                    val amplitude = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    val micPeak = (amplitude / 32767.0f).coerceIn(0f, 1f)
                    val internalPeak = (micPeak * 0.85f).coerceIn(0f, 1f)

                    _recordingState.value = RecordingState.Active(
                        durationMs = currentElapsed.coerceAtLeast(0L),
                        bytesWritten = currentBytes,
                        isPaused = false,
                        micPeakLevel = micPeak,
                        internalPeakLevel = internalPeak,
                        outputFilePath = file.absolutePath
                    )
                }
            }
        }
    }

    fun pauseRecording() {
        if (_recordingState.value is RecordingState.Active && !isPaused) {
            try {
                mediaRecorder?.pause()
                isPaused = true
                pauseStartTime = System.currentTimeMillis()
                val current = _recordingState.value as RecordingState.Active
                _recordingState.value = current.copy(isPaused = true)
            } catch (e: Exception) {
                Log.e(TAG, "Pause recording failed", e)
            }
        }
    }

    fun resumeRecording() {
        if (_recordingState.value is RecordingState.Active && isPaused) {
            try {
                mediaRecorder?.resume()
                isPaused = false
                pausedDuration += (System.currentTimeMillis() - pauseStartTime)
                val current = _recordingState.value as RecordingState.Active
                _recordingState.value = current.copy(isPaused = false)
            } catch (e: Exception) {
                Log.e(TAG, "Resume recording failed", e)
            }
        }
    }

    fun togglePauseResume() {
        if (isPaused) resumeRecording() else pauseRecording()
    }

    fun stopRecording(context: Context, recordingsRepository: RecordingsRepository) {
        if (_recordingState.value !is RecordingState.Active && _recordingState.value !is RecordingState.Countdown) {
            return
        }

        timerJob?.cancel()
        countdownJob?.cancel()

        _recordingState.value = RecordingState.Processing(0.5f, "Finalizing video...")

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    runCatching {
                        mediaRecorder?.stop()
                        mediaRecorder?.reset()
                    }
                    cleanup()
                    recordingsRepository.loadRecordings()
                }

                // Stop services
                ScreenRecordService.stopService(context)
                FloatingBallService.stopService(context)

                val file = currentOutputFile
                if (file != null && file.exists() && file.length() > 0) {
                    val recordings = recordingsRepository.recordings.value
                    val createdItem = recordings.find { it.filePath == file.absolutePath } ?: RecordingItem(
                        id = file.absolutePath,
                        title = file.nameWithoutExtension,
                        filePath = file.absolutePath,
                        sizeBytes = file.length(),
                        timestamp = file.lastModified()
                    )
                    _recordingState.value = RecordingState.Finished(createdItem)
                    onFinishedCallback?.invoke(createdItem)
                } else {
                    _recordingState.value = RecordingState.Error("Recorded file is empty or missing.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
                _recordingState.value = RecordingState.Error("Error finalizing recording: ${e.message}")
            }
        }
    }

    fun resetToIdle() {
        _recordingState.value = RecordingState.Idle
    }

    private fun cleanup() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            mediaProjection?.stop()
            mediaProjection = null
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    private fun postToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
