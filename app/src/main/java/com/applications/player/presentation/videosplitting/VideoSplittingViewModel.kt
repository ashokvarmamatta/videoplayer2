package com.applications.player.presentation.videosplitting

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class VideoSplittingViewModel : ViewModel() {

    private val _state = MutableStateFlow(VideoSplittingState())
    val state: StateFlow<VideoSplittingState> = _state

    fun splitVideo(context: Context, videoUri: Uri, startMillis: Long, endMillis: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isSplitting = true,
                progress = 0f,
                splitVideoUri = null,
                error = null
            )
            val outputUri = withContext(Dispatchers.IO) {
                performVideoSplitting(context, videoUri, startMillis * 1000, endMillis * 1000) { progress ->
                    _state.value = _state.value.copy(progress = progress)
                }
            }
            _state.value = if (outputUri != null) {
                _state.value.copy(isSplitting = false, splitVideoUri = outputUri)
            } else {
                _state.value.copy(isSplitting = false, error = "Failed to split video.")
            }
        }
    }

    private fun performVideoSplitting(
        context: Context,
        videoUri: Uri,
        startMicro: Long,
        endMicro: Long,
        onProgress: (Float) -> Unit
    ): Uri? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, videoUri, null)

            val outputUri = createOutputUri(context.contentResolver)
            val fileDescriptor = context.contentResolver.openFileDescriptor(outputUri!!, "w")
            if (fileDescriptor == null) return null

            val muxer = MediaMuxer(fileDescriptor.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val videoTrackIndex = selectTrack(extractor, "video/")
            val audioTrackIndex = selectTrack(extractor, "audio/")

            if (videoTrackIndex == -1) return null

            val videoFormat = extractor.getTrackFormat(videoTrackIndex)
            val videoMuxerTrackIndex = muxer.addTrack(videoFormat)

            var audioMuxerTrackIndex = -1
            if (audioTrackIndex != -1) {
                val audioFormat = extractor.getTrackFormat(audioTrackIndex)
                audioMuxerTrackIndex = muxer.addTrack(audioFormat)
            }

            muxer.start()

            // Process video and audio tracks with progress updates
            splitAndWriteTrack(extractor, muxer, videoTrackIndex, videoMuxerTrackIndex, startMicro, endMicro) { progress ->
                onProgress(progress * 0.5f) // Video takes 50% of progress
            }
            if (audioTrackIndex != -1) {
                splitAndWriteTrack(extractor, muxer, audioTrackIndex, audioMuxerTrackIndex, startMicro, endMicro) { progress ->
                    onProgress(0.5f + progress * 0.5f) // Audio takes the other 50%
                }
            }

            muxer.stop()
            muxer.release()
            fileDescriptor.close()

            return outputUri
        } catch (e: Exception) {
            Log.e("VideoSplittingViewModel", "Error splitting video", e)
            return null
        } finally {
            extractor.release()
        }
    }

    private fun splitAndWriteTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        trackIndex: Int,
        muxerTrackIndex: Int,
        startMicro: Long,
        endMicro: Long,
        onProgress: (Float) -> Unit
    ) {
        extractor.selectTrack(trackIndex)
        val buffer = ByteBuffer.allocate(extractor.getTrackFormat(trackIndex).getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
        val bufferInfo = MediaCodec.BufferInfo()

        extractor.seekTo(startMicro, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        var totalTimeWritten = 0L
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            val presentationTimeUs = extractor.sampleTime

            if (presentationTimeUs > endMicro) break

            if (presentationTimeUs >= startMicro) {
                bufferInfo.set(0, sampleSize, presentationTimeUs - startMicro, extractor.sampleFlags)
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                totalTimeWritten += bufferInfo.size
            }

            val progress = (presentationTimeUs.toFloat() - startMicro.toFloat()) / (endMicro.toFloat() - startMicro.toFloat())
            onProgress(progress.coerceIn(0f, 1f))

            extractor.advance()
        }
    }

    private fun selectTrack(extractor: MediaExtractor, mimePrefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            if (extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith(mimePrefix) == true) {
                return i
            }
        }
        return -1
    }

    private fun createOutputUri(contentResolver: ContentResolver): Uri? {
        val fileName = "split_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SplittedVideos")
            }
        }

        return contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
    }
}