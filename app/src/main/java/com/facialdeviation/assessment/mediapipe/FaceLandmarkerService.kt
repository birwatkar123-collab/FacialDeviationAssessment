package com.facialdeviation.assessment.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class FaceLandmarkerService(private val context: Context) {

    companion object {
        private const val TAG = "FaceLandmarkerService"
        private const val MODEL_FILE = "face_landmarker.task"
    }

    private var faceLandmarker: FaceLandmarker? = null
    private var isInitialized = false
    private var lastTimestampMs = -1L
    private var submittedFrames = 0L
    private var skippedFrames = 0L

    var onResult: ((FaceLandmarkerResult, Long) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun initialize() {
        try {
            // Close any previously open instance before re-initializing.
            close()
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath(MODEL_FILE)
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setOutputFaceBlendshapes(true)
                .setResultListener { result, _ ->
                    val timestampMs = lastTimestampMs
                    Log.d(TAG, "Result tsMs=$timestampMs faces=${result.faceLandmarks().size}")
                    onResult?.invoke(result, timestampMs)
                }
                .setErrorListener { error ->
                    Log.e(TAG, "MediaPipe error: ${error.message}")
                    onError?.invoke(error.message ?: "Unknown MediaPipe error")
                }
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            isInitialized = true
            lastTimestampMs = -1L
            submittedFrames = 0L
            skippedFrames = 0L
            Log.d(TAG, "FaceLandmarker initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FaceLandmarker", e)
            onError?.invoke("Failed to initialize face detection: ${e.message}")
            isInitialized = false
        }
    }

    fun detectAsync(bitmap: Bitmap, timestampMs: Long, rotationDegrees: Int) {
        if (!isInitialized || faceLandmarker == null) {
            onError?.invoke("FaceLandmarker not initialized")
            return
        }

        // MediaPipe LIVE_STREAM requires strictly increasing timestamps.
        if (timestampMs <= lastTimestampMs) {
            skippedFrames++
            Log.d(
                TAG,
                "Skipped frame tsMs=$timestampMs lastTsMs=$lastTimestampMs skipped=$skippedFrames"
            )
            return
        }
        lastTimestampMs = timestampMs
        submittedFrames++

        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            // Orientation strategy A: CameraManager already physically rotated the
            // bitmap to the upright (display) orientation, so MediaPipe must NOT
            // apply a further rotation on top. rotationDegrees is therefore always
            // 0 here (any nonzero value would double-rotate and return landmarks
            // in a non-upright coordinate frame). MediaPipe's returned normalized
            // coordinates are then in the upright frame by construction.
            val options = ImageProcessingOptions.builder()
                .setRotationDegrees(rotationDegrees)
                .build()
            faceLandmarker?.detectAsync(mpImage, options, timestampMs)
            Log.d(
                TAG,
                "Submitted tsMs=$timestampMs lastTsMs=$lastTimestampMs rotDeg=$rotationDegrees " +
                    "submitted=$submittedFrames"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Detection error", e)
            onError?.invoke("Detection error: ${e.message}")
        }
    }

    fun resetTimestamps() {
        lastTimestampMs = -1L
        submittedFrames = 0L
        skippedFrames = 0L
    }

    fun close() {
        try {
            faceLandmarker?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing FaceLandmarker", e)
        }
        faceLandmarker = null
        isInitialized = false
        lastTimestampMs = -1L
    }

    fun isReady(): Boolean = isInitialized
}