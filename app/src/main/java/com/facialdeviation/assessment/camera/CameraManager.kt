package com.facialdeviation.assessment.camera

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {

    companion object {
        private const val TAG = "CameraManager"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var analysisExecutor: ExecutorService? = null
    private var lastTimestampMs = -1L
    var onFrameAnalyzed: ((android.graphics.Bitmap, Long, Int) -> Unit)? = null

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        val preview = Preview.Builder()
            .setTargetResolution(Size(640, 480))
            .build()
            .also {
                it.surfaceProvider = previewView.surfaceProvider
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        analysisExecutor = Executors.newSingleThreadExecutor()
        imageAnalysis.setAnalyzer(analysisExecutor!!) { imageProxy ->
            processImage(imageProxy)
        }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            Log.d(TAG, "Camera started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Camera bind failed", e)
        }
    }

    private fun processImage(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxy.toBitmap()
            if (bitmap != null) {
                // Use CameraX frame timestamp (nanoseconds), converted to milliseconds.
                // MediaPipe LIVE_STREAM requires strictly increasing timestamps, so we
                // never use System.currentTimeMillis() here.
                val rawTimestampNs = imageProxy.imageInfo.timestamp
                var timestampMs = if (rawTimestampNs > 0) {
                    rawTimestampNs / 1_000_000L
                } else {
                    0L
                }

                // Guard: ensure timestamps are strictly increasing (dedupe/skip out-of-order).
                if (timestampMs <= lastTimestampMs) {
                    timestampMs = lastTimestampMs + 1
                }
                lastTimestampMs = timestampMs

                Log.d(TAG, "CameraFrame tsNs=$rawTimestampNs tsMs=$timestampMs")

                // ONE consistent orientation strategy (A):
                // 1) toBitmap() above produces the buffer in SENSOR orientation (landscape).
                // 2) Physically rotate the bitmap upright here using CameraX's
                //    rotationDegrees (the clockwise rotation that makes the buffer display
                //    upright). MediaPipe therefore receives an ALREADY-UPRIGHT image.
                // 3) MediaPipe is passed rotationDegrees = 0 so it never applies its own
                //    rotation on top. This guarantees the returned landmark coordinates
                //    are in the upright frame: for a frontal face the eye corners
                //    (33, 263) are horizontally separated and eyeAngle ~ 0.
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                Log.d(
                    TAG,
                    "Rotation sensorRot=$rotationDegrees buffer=${imageProxy.width}x${imageProxy.height}"
                )
                val uprightBitmap = rotateToUpright(bitmap, rotationDegrees)
                Log.d(
                    TAG,
                    "Rotated to upright ${uprightBitmap.width}x${uprightBitmap.height}, " +
                        "handing MediaPipe rotation=0 (Strategy A)"
                )

                onFrameAnalyzed?.invoke(uprightBitmap, timestampMs, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "processImage error", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun rotateToUpright(source: android.graphics.Bitmap, rotationDegrees: Int): android.graphics.Bitmap {
        if (rotationDegrees % 360 == 0) return source
        val matrix = android.graphics.Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return android.graphics.Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        )
    }

    fun stopCamera() {
        lastTimestampMs = -1L
        analysisExecutor?.shutdown()
        analysisExecutor = null
        cameraProvider?.unbindAll()
    }

    private fun ImageProxy.toBitmap(): android.graphics.Bitmap? {
        try {
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap = android.graphics.Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            return if (rowPadding > 0) {
                android.graphics.Bitmap.createBitmap(bitmap, 0, 0, width, height)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap conversion failed", e)
            return null
        }
    }
}
