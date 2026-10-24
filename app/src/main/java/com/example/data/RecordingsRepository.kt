package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.RecordingItem
import com.example.util.VideoTrimmerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingsRepository(private val context: Context) {
    private val _recordings = MutableStateFlow<List<RecordingItem>>(emptyList())
    val recordings: StateFlow<List<RecordingItem>> = _recordings.asStateFlow()

    // High performance memory cache for metadata to eliminate I/O lag
    private val metadataCache = java.util.concurrent.ConcurrentHashMap<String, RecordingItem>()

    fun getOutputDirectory(): File {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "Movies")
        val appFolder = File(moviesDir, "ApexFrame")
        if (!appFolder.exists()) {
            appFolder.mkdirs()
        }
        return appFolder
    }

    fun generateNewOutputFile(extension: String = "mp4"): File {
        val folder = getOutputDirectory()
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val timestamp = sdf.format(Date())
        val fileName = "ApexFrame_$timestamp.$extension"
        return File(folder, fileName)
    }

    suspend fun loadRecordings() = withContext(Dispatchers.IO) {
        val folder = getOutputDirectory()
        val files = folder.listFiles { file ->
            file.isFile && (file.name.endsWith(".mp4", ignoreCase = true) || file.name.endsWith(".mkv", ignoreCase = true))
        } ?: emptyArray()

        val items = files.mapNotNull { file ->
            val cached = metadataCache[file.absolutePath]
            if (cached != null && cached.timestamp == file.lastModified() && cached.sizeBytes == file.length()) {
                cached
            } else {
                extractMetadata(file)?.also {
                    metadataCache[file.absolutePath] = it
                }
            }
        }.sortedByDescending { it.timestamp }

        _recordings.value = items
    }

    private fun extractMetadata(file: File): RecordingItem? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val width = widthStr?.toIntOrNull() ?: 1920

            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val height = heightStr?.toIntOrNull() ?: 1080

            retriever.release()

            val format = if (file.extension.equals("mkv", ignoreCase = true)) "MKV" else "MP4"

            RecordingItem(
                id = file.absolutePath,
                title = file.nameWithoutExtension,
                filePath = file.absolutePath,
                durationMs = durationMs,
                sizeBytes = file.length(),
                width = width,
                height = height,
                fps = 60,
                format = format,
                timestamp = file.lastModified()
            )
        } catch (e: Exception) {
            Log.e("RecordingsRepository", "Failed to extract metadata for ${file.name}", e)
            if (file.exists() && file.length() > 0) {
                RecordingItem(
                    id = file.absolutePath,
                    title = file.nameWithoutExtension,
                    filePath = file.absolutePath,
                    durationMs = 0L,
                    sizeBytes = file.length(),
                    timestamp = file.lastModified()
                )
            } else null
        }
    }

    suspend fun deleteRecording(item: RecordingItem): Boolean = withContext(Dispatchers.IO) {
        val file = File(item.filePath)
        val deleted = file.delete()
        if (deleted) {
            _recordings.value = _recordings.value.filter { it.id != item.id }
        }
        deleted
    }

    suspend fun renameRecording(item: RecordingItem, newName: String): RecordingItem? = withContext(Dispatchers.IO) {
        val cleanName = newName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val oldFile = File(item.filePath)
        val extension = oldFile.extension
        val newFile = File(oldFile.parentFile, "$cleanName.$extension")

        if (oldFile.renameTo(newFile)) {
            val updated = item.copy(
                id = newFile.absolutePath,
                title = cleanName,
                filePath = newFile.absolutePath
            )
            _recordings.value = _recordings.value.map { if (it.id == item.id) updated else it }
            updated
        } else {
            null
        }
    }

    suspend fun trimRecording(
        sourceItem: RecordingItem,
        startMs: Long,
        endMs: Long,
        onProgress: (Float) -> Unit
    ): RecordingItem? = withContext(Dispatchers.IO) {
        val srcFile = File(sourceItem.filePath)
        val ext = srcFile.extension
        val dstFile = File(srcFile.parentFile, "${sourceItem.title}_trimmed_${System.currentTimeMillis() % 10000}.$ext")

        val success = VideoTrimmerUtil.trimVideo(
            srcPath = srcFile.absolutePath,
            dstPath = dstFile.absolutePath,
            startMs = startMs,
            endMs = endMs,
            onProgress = onProgress
        )

        if (success && dstFile.exists()) {
            val newItem = extractMetadata(dstFile)
            loadRecordings()
            newItem
        } else {
            null
        }
    }
}
