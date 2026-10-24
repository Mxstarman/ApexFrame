package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("apex_frame_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<RecordingSettings> = _settings.asStateFlow()

    private fun loadSettings(): RecordingSettings {
        val resName = prefs.getString("resolution", ResolutionOption.FHD_1080P.name) ?: ResolutionOption.FHD_1080P.name
        val fpsName = prefs.getString("fps", FpsOption.FPS_60.name) ?: FpsOption.FPS_60.name
        val audioName = prefs.getString("audio_source", AudioSourceOption.DUAL_MIXED.name) ?: AudioSourceOption.DUAL_MIXED.name
        val formatName = prefs.getString("video_format", VideoFormatOption.MP4.name) ?: VideoFormatOption.MP4.name
        val codecName = prefs.getString("video_codec", VideoCodecOption.H264.name) ?: VideoCodecOption.H264.name

        return RecordingSettings(
            resolution = runCatching { ResolutionOption.valueOf(resName) }.getOrDefault(ResolutionOption.FHD_1080P),
            fps = runCatching { FpsOption.valueOf(fpsName) }.getOrDefault(FpsOption.FPS_60),
            bitrateMbps = prefs.getInt("bitrate_mbps", 12),
            isAutoBitrate = prefs.getBoolean("is_auto_bitrate", true),
            audioSource = runCatching { AudioSourceOption.valueOf(audioName) }.getOrDefault(AudioSourceOption.DUAL_MIXED),
            micVolume = prefs.getFloat("mic_volume", 1.0f),
            internalVolume = prefs.getFloat("internal_volume", 1.0f),
            noiseSuppression = prefs.getBoolean("noise_suppression", true),
            videoFormat = runCatching { VideoFormatOption.valueOf(formatName) }.getOrDefault(VideoFormatOption.MP4),
            videoCodec = runCatching { VideoCodecOption.valueOf(codecName) }.getOrDefault(VideoCodecOption.H264),
            countdownSeconds = prefs.getInt("countdown_seconds", 3),
            stealthOverlayEnabled = prefs.getBoolean("stealth_overlay", true),
            floatingControlBallEnabled = prefs.getBoolean("floating_ball", true),
            minimizeOnRecord = prefs.getBoolean("minimize_on_record", true),
            volumeKeyShortcutEnabled = prefs.getBoolean("volume_key_shortcut", true),
            customOutputDir = prefs.getString("output_dir", "Movies/ApexFrame") ?: "Movies/ApexFrame"
        )
    }

    fun updateSettings(newSettings: RecordingSettings) {
        _settings.value = newSettings
        prefs.edit().apply {
            putString("resolution", newSettings.resolution.name)
            putString("fps", newSettings.fps.name)
            putInt("bitrate_mbps", newSettings.bitrateMbps)
            putBoolean("is_auto_bitrate", newSettings.isAutoBitrate)
            putString("audio_source", newSettings.audioSource.name)
            putFloat("mic_volume", newSettings.micVolume)
            putFloat("internal_volume", newSettings.internalVolume)
            putBoolean("noise_suppression", newSettings.noiseSuppression)
            putString("video_format", newSettings.videoFormat.name)
            putString("video_codec", newSettings.videoCodec.name)
            putInt("countdown_seconds", newSettings.countdownSeconds)
            putBoolean("stealth_overlay", newSettings.stealthOverlayEnabled)
            putBoolean("floating_ball", newSettings.floatingControlBallEnabled)
            putBoolean("minimize_on_record", newSettings.minimizeOnRecord)
            putBoolean("volume_key_shortcut", newSettings.volumeKeyShortcutEnabled)
            putString("output_dir", newSettings.customOutputDir)
            apply()
        }
    }
}
