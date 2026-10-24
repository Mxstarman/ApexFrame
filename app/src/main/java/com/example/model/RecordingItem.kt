package com.example.model

import android.net.Uri

data class RecordingItem(
    val id: String,
    val title: String,
    val filePath: String,
    val uri: Uri? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 60,
    val format: String = "MP4",
    val timestamp: Long = System.currentTimeMillis()
) {
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

    fun formattedSize(): String {
        val mb = sizeBytes / (1024.0 * 1024.0)
        return if (mb >= 1000) {
            String.format("%.2f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }
}
