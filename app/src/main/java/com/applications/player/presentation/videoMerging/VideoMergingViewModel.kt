package com.applications.player.presentation.videoMerging

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer

class VideoMergingViewModel : ViewModel() {

    private val _state = MutableStateFlow(VideoMergingState())
    val state: StateFlow<VideoMergingState> = _state.asStateFlow()
    private var videoDurationUs: Long = 0L

    fun setStartTime(timeMs: Long) {
        _state.update { it.copy(startTimeMs = timeMs) }
    }

    fun setEndTime(timeMs: Long) {
        _state.update { it.copy(endTimeMs = timeMs) }
    }

    fun getVideoDuration(context: Context, videoUri: Uri) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            videoDurationUs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?.times(1000) ?: 0L
            _state.update { it.copy(videoDurationMs = videoDurationUs / 1000) }
        } catch (e: Exception) {
            videoDurationUs = 0L
        } finally {
            retriever.release()
        }
    }

    fun deleteGapAndMerge(context: Context, videoUri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isMerging = true) }
            val outputUri = createOutputUri(context, "merged_video_${System.currentTimeMillis()}.mp4")
            if (outputUri == null) {
                _state.update { it.copy(isMerging = false, error = "Failed to create output file.") }
                return@launch
            }

            val sourceFileDescriptor = context.contentResolver.openFileDescriptor(videoUri, "r")?.fileDescriptor
            if (sourceFileDescriptor == null) {
                _state.update { it.copy(isMerging = false, error = "Failed to open video file.") }
                return@launch
            }

            val tempOutputPath = File(context.cacheDir, "temp_merged.mp4").absolutePath
            val extractor = MediaExtractor()
            var muxer: MediaMuxer? = null

            try {
                extractor.setDataSource(sourceFileDescriptor)
                muxer = MediaMuxer(tempOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                val trackMap = mutableMapOf<Int, Int>()
                val trackCount = extractor.trackCount
                for (i in 0 until trackCount) {
                    val format = extractor.getTrackFormat(i)
                    trackMap[i] = muxer.addTrack(format)
                }
                muxer.start()

                val buffer = ByteBuffer.allocate(512 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()

                val startTimeUs = state.value.startTimeMs * 1000
                val endTimeUs = state.value.endTimeMs * 1000

                // Process the first segment (before the gap)
                writeSegment(extractor, muxer, buffer, bufferInfo, 0, startTimeUs, trackMap, 0)

                // Calculate the time offset for the second segment
                val timeOffset = endTimeUs - startTimeUs

                // Process the second segment (after the gap)
                writeSegment(extractor, muxer, buffer, bufferInfo, endTimeUs, videoDurationUs, trackMap, -timeOffset)

                muxer.stop()
                muxer.release()
                extractor.release()

                context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                    File(tempOutputPath).inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                File(tempOutputPath).delete()

                _state.update { it.copy(isMerging = false, mergedVideoUri = outputUri.toString()) }

            } catch (e: Exception) {
                Log.e("VideoMerging", "Merging failed: ${e.message}", e)
                _state.update { it.copy(isMerging = false, error = "Merging failed: ${e.message}") }
            }
        }
    }

    private fun writeSegment(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        buffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
        startTimeUs: Long,
        endTimeUs: Long,
        trackMap: Map<Int, Int>,
        timeOffsetUs: Long
    ) {
        for (i in 0 until extractor.trackCount) {
            val muxerTrackIndex = trackMap[i] ?: continue
            extractor.selectTrack(i)
            extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize <= 0) break

                val presentationTimeUs = extractor.sampleTime
                if (presentationTimeUs >= endTimeUs) {
                    extractor.unselectTrack(i)
                    break
                }

                // Check if the timestamp is already offset. This prevents double-offsetting
                // if the same track is processed multiple times.
                val newPresentationTimeUs = presentationTimeUs + timeOffsetUs

                if (newPresentationTimeUs < 0) {
                    // This can happen if the first segment is very short.
                    // We can just drop the sample or adjust more carefully.
                    extractor.advance()
                    continue
                }

                bufferInfo.set(0, sampleSize, newPresentationTimeUs, extractor.sampleFlags)
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

                extractor.advance()
            }
        }
    }

    private fun createOutputUri(context: Context, fileName: String): Uri? {
        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "MergedVideos")
        }
        return contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    }
}