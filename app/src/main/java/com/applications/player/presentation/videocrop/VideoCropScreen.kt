package com.applications.player.presentation.videocrop

import android.net.Uri
import android.view.SurfaceHolder
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import android.widget.Toast

@Composable
fun VideoCropScreen(
    videoUri: Uri,
    viewModel: VideoCropViewModel = viewModel()
) {
    val context = LocalContext.current
    val cropState by viewModel.state.collectAsState()
    var videoSize by remember { mutableStateOf(IntSize.Zero) }

    var surfaceViewRef by remember { mutableStateOf<android.view.SurfaceView?>(null) }

    LaunchedEffect(videoUri, surfaceViewRef) {
        if (surfaceViewRef == null) {
            return@LaunchedEffect
        }

        val surfaceHolder = surfaceViewRef!!.holder
        var mediaExtractor: MediaExtractor? = null
        var mediaCodec: MediaCodec? = null

        try {
            mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(context, videoUri, null)
            val videoTrackIndex = (0 until mediaExtractor.trackCount)
                .firstOrNull { mediaExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true }
                ?: throw IllegalStateException("No video track found")

            mediaExtractor.selectTrack(videoTrackIndex)
            val format = mediaExtractor.getTrackFormat(videoTrackIndex)

            videoSize = IntSize(
                format.getInteger(MediaFormat.KEY_WIDTH),
                format.getInteger(MediaFormat.KEY_HEIGHT)
            )

            mediaCodec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            mediaCodec.configure(format, surfaceHolder.surface, null, 0)
            mediaCodec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            while (isActive) {
                // Dequeue and process input buffers
                val inputBufferId = mediaCodec.dequeueInputBuffer(100)
                if (inputBufferId >= 0) {
                    val inputBuffer = mediaCodec.getInputBuffer(inputBufferId)
                    val sampleSize = mediaExtractor.readSampleData(inputBuffer!!, 0)
                    if (sampleSize < 0) {
                        mediaCodec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        break
                    } else {
                        mediaCodec.queueInputBuffer(inputBufferId, 0, sampleSize, mediaExtractor.sampleTime, 0)
                        mediaExtractor.advance()
                    }
                }

                // Dequeue and render output buffers
                val outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 100)
                if (outputBufferId >= 0) {
                    // Corrected line: release the buffer to the surface.
                    // The 'size' check ensures we render valid frames and don't render a final empty buffer.
                    mediaCodec.releaseOutputBuffer(outputBufferId, bufferInfo.size != 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaExtractor?.release()
        }
    }

    DisposableEffect(Unit) { onDispose { } }

    LaunchedEffect(cropState.croppedVideoUri) {
        if (cropState.croppedVideoUri != null) {
            Toast.makeText(context, "Cropping successful!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .onGloballyPositioned { videoSize = it.size }
                .background(Color.Black)
                .aspectRatio(16f / 9f)
        ) {
            AndroidView(
                factory = { ctx ->
                    android.view.SurfaceView(ctx).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) { surfaceViewRef = this@apply }
                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                            override fun surfaceDestroyed(holder: SurfaceHolder) { surfaceViewRef = null }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            CropOverlay(
                videoSize = videoSize,
                selectedRatio = cropState.selectedRatio
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AspectRatioButton("1:1", { viewModel.selectAspectRatio(AspectRatio.RATIO_1_1) })
            AspectRatioButton("9:16", { viewModel.selectAspectRatio(AspectRatio.RATIO_9_16) })
            AspectRatioButton("16:9", { viewModel.selectAspectRatio(AspectRatio.RATIO_16_9) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (cropState.isCropping) {
            AlertDialog(
                onDismissRequest = { /* No dismiss */ },
                title = { Text("Cropping Video") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Please wait...")
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = cropState.progress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {}
            )
        } else {
            Button(
                onClick = { viewModel.cropVideo(context, videoUri) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crop Video")
            }
        }
    }
}