package com.applications.player.presentation.videocrop

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.daasuu.mp4compose.Rotation
import com.daasuu.mp4compose.composer.Mp4Composer

class VideoCropViewModel : ViewModel() {

    private val _state = MutableStateFlow(VideoCropState())
    val state: StateFlow<VideoCropState> = _state.asStateFlow()

    fun selectAspectRatio(ratio: AspectRatio) {
        _state.update { it.copy(selectedRatio = ratio) }
    }

    fun cropVideo(context: Context, videoUri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isCropping = true, progress = 0f, croppedVideoUri = null, error = null) }

            val sourceFile = getFileFromUri(context, videoUri)
            if (sourceFile == null) {
                _state.update { it.copy(isCropping = false, error = "Failed to get source file from URI.") }
                return@launch
            }

            val outputUri = createOutputUri(context, "cropped_video_${System.currentTimeMillis()}.mp4")
            if (outputUri == null) {
                _state.update { it.copy(isCropping = false, error = "Failed to create output file.") }
                return@launch
            }

            val tempDestFile = File(context.filesDir, "temp_crop.mp4")

            val rotation = when (state.value.selectedRatio) {
                AspectRatio.RATIO_9_16 -> Rotation.ROTATION_90
                else -> Rotation.NORMAL
            }

            Mp4Composer(sourceFile.absolutePath, tempDestFile.absolutePath)
                .rotation(rotation)
                .listener(object : Mp4Composer.Listener {
                    override fun onProgress(progress: Double) {
                        _state.update { it.copy(progress = progress.toFloat()) }
                    }

                    override fun onCurrentWrittenVideoTime(timeUs: Long) {

                    }

                    override fun onCompleted() {
                        val outputStream = context.contentResolver.openOutputStream(outputUri)
                        outputStream?.use { os ->
                            tempDestFile.inputStream().use { inputStream ->
                                inputStream.copyTo(os)
                            }
                        }
                        tempDestFile.delete()

                        _state.update { it.copy(isCropping = false, croppedVideoUri = outputUri.toString()) }
                        Log.d("Mp4Composer", "Crop completed. Saved to: $outputUri")
                    }

                    override fun onCanceled() {
                        _state.update { it.copy(isCropping = false) }
                        tempDestFile.delete()
                    }

                    override fun onFailed(exception: Exception) {
                        _state.update { it.copy(isCropping = false, error = "Cropping failed.") }
                        tempDestFile.delete()
                        Log.e("Mp4Composer", "Cropping failed", exception)
                    }
                })
                .start()
        }
    }

    private fun createOutputUri(context: Context, fileName: String): Uri? {
        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "CroppedVideos")
        }
        return contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        // This is a basic implementation. It requires a content resolver
        // to get the actual file path from the URI.
        return null
    }
}