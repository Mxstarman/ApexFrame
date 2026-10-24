package com.example.model

sealed interface RecordingState {
    data object Idle : RecordingState

    data class Countdown(
        val remainingSeconds: Int,
        val totalSeconds: Int
    ) : RecordingState

    data class Active(
        val durationMs: Long = 0L,
        val bytesWritten: Long = 0L,
        val isPaused: Boolean = false,
        val micPeakLevel: Float = 0.0f,
        val internalPeakLevel: Float = 0.0f,
        val outputFilePath: String = ""
    ) : RecordingState {
        fun formattedDuration(): String {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }

    data class Processing(
        val progress: Float = 0f,
        val message: String = "Finalizing video..."
    ) : RecordingState

    data class Finished(
        val recording: RecordingItem
    ) : RecordingState

    data class Error(
        val message: String
    ) : RecordingState
}
