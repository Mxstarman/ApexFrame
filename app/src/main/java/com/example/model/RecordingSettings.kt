package com.example.model

enum class ResolutionOption(
    val width: Int,
    val height: Int,
    val label: String,
    val description: String
) {
    SD_720P(1280, 720, "720p HD", "1280 × 720 • Lower file size"),
    FHD_1080P(1920, 1080, "1080p FHD", "1920 × 1080 • Standard Full HD"),
    UHD_4K(3840, 2160, "4K UHD", "3840 × 2160 • Extreme clarity"),
    NATIVE_DISPLAY(0, 0, "Native Display", "Auto-match device screen resolution")
}

enum class FpsOption(val fps: Int, val label: String) {
    FPS_30(30, "30 FPS"),
    FPS_60(60, "60 FPS (Default)"),
    FPS_90(90, "90 FPS (High Smoothness)"),
    FPS_120(120, "120 FPS (Ultra)")
}

enum class AudioSourceOption(val label: String, val description: String) {
    NONE("Mute (No Audio)", "Silent recording without any sound"),
    MIC("Microphone Only", "Record voice and ambient commentary"),
    INTERNAL("Internal Audio Only", "Record game & app sounds without mic"),
    DUAL_MIXED("Dual Audio (Internal + Mic)", "Mix app sound and voice commentary")
}

enum class VideoFormatOption(val extension: String, val mimeType: String, val label: String) {
    MP4("mp4", "video/mp4", "MP4 (MPEG-4 Part 14)"),
    MKV("mkv", "video/x-matroska", "MKV (Matroska Video)")
}

enum class VideoCodecOption(val label: String, val codecName: String) {
    H264("H.264 / AVC", "video/avc"),
    HEVC("H.265 / HEVC", "video/hevc")
}

data class RecordingSettings(
    val resolution: ResolutionOption = ResolutionOption.FHD_1080P,
    val fps: FpsOption = FpsOption.FPS_60,
    val bitrateMbps: Int = 12, // 2 to 50 Mbps
    val isAutoBitrate: Boolean = true,
    val audioSource: AudioSourceOption = AudioSourceOption.DUAL_MIXED,
    val micVolume: Float = 1.0f, // 0.0 to 1.5
    val internalVolume: Float = 1.0f, // 0.0 to 1.5
    val noiseSuppression: Boolean = true,
    val videoFormat: VideoFormatOption = VideoFormatOption.MP4,
    val videoCodec: VideoCodecOption = VideoCodecOption.H264,
    val countdownSeconds: Int = 3, // 0, 3, 5, 10
    val stealthOverlayEnabled: Boolean = true, // Invisible in video via FLAG_SECURE
    val floatingControlBallEnabled: Boolean = true,
    val minimizeOnRecord: Boolean = true,
    val volumeKeyShortcutEnabled: Boolean = true,
    val customOutputDir: String = "Movies/ApexFrame"
) {
    fun calculateEffectiveBitrate(): Int {
        if (!isAutoBitrate) return bitrateMbps * 1_000_000
        // Auto-optimized bitrate based on resolution and fps
        val baseBitrate = when (resolution) {
            ResolutionOption.SD_720P -> 6_000_000
            ResolutionOption.FHD_1080P -> 12_000_000
            ResolutionOption.UHD_4K -> 35_000_000
            ResolutionOption.NATIVE_DISPLAY -> 14_000_000
        }
        val fpsFactor = when (fps) {
            FpsOption.FPS_30 -> 0.75f
            FpsOption.FPS_60 -> 1.0f
            FpsOption.FPS_90 -> 1.3f
            FpsOption.FPS_120 -> 1.5f
        }
        return (baseBitrate * fpsFactor).toInt()
    }
}
