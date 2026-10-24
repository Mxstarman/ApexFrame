package com.example.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

object VideoTrimmerUtil {
    private const val TAG = "VideoTrimmerUtil"
    private const val DEFAULT_BUFFER_SIZE = 1024 * 1024 // 1 MB

    suspend fun trimVideo(
        srcPath: String,
        dstPath: String,
        startMs: Long,
        endMs: Long,
        onProgress: (Float) -> Unit
    ): Boolean {
        return try {
            val srcFile = File(srcPath)
            if (!srcFile.exists()) {
                Log.e(TAG, "Source file does not exist: $srcPath")
                return false
            }

            val extractor = MediaExtractor()
            extractor.setDataSource(srcPath)
            val trackCount = extractor.trackCount

            val muxerFormat = if (dstPath.endsWith(".mkv", ignoreCase = true)) {
                // If muxer supports WEBM or fallback to MPEG_4
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            } else {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            }

            val muxer = MediaMuxer(dstPath, muxerFormat)
            val indexMap = HashMap<Int, Int>(trackCount)
            var bufferSize = DEFAULT_BUFFER_SIZE

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    indexMap[i] = dstIndex
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (newSize > bufferSize) bufferSize = newSize
                    }
                }
            }

            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            val totalTrimDurationUs = (endUs - startUs).coerceAtLeast(1L)

            // Seek to sync/keyframe before startUs
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val dstBuf = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) {
                    // Passed the trim endpoint
                    extractor.advance()
                    continue
                }

                if (sampleTime < startUs) {
                    extractor.advance()
                    continue
                }

                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(dstBuf, 0)
                if (bufferInfo.size < 0) break

                bufferInfo.presentationTimeUs = sampleTime - startUs
                bufferInfo.flags = extractor.sampleFlags

                val muxerTrackIndex = indexMap[trackIndex]
                if (muxerTrackIndex != null) {
                    muxer.writeSampleData(muxerTrackIndex, dstBuf, bufferInfo)
                }

                val progress = ((sampleTime - startUs).toFloat() / totalTrimDurationUs.toFloat()).coerceIn(0f, 1f)
                onProgress(progress)

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            onProgress(1.0f)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming video", e)
            false
        }
    }
}
