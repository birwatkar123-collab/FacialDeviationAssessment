package com.facialdeviation.assessment

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.facialdeviation.assessment.analysis.*
import com.facialdeviation.assessment.mediapipe.FaceLandmarkerService
import com.facialdeviation.assessment.model.*
import com.facialdeviation.assessment.utils.FaceLandmarkUtils
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class AssessmentViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AssessmentViewModel"
        private const val MIN_NEUTRAL_FRAMES = 30
        private const val MIN_SMILE_FRAMES = 30
        private const val SMILE_COLLECTION_DURATION_MS = 3000L
        private const val NEUTRAL_COLLECTION_DURATION_MS = 3000L
    }

    enum class AssessmentState {
        HOME,
        PERMISSION_REQUEST,
        CALIBRATING,
        NEUTRAL_FACE,
        SMILE_TEST,
        COLLECTING_SMILE,
        RESULTS,
        ERROR
    }

    private val _state = MutableStateFlow(AssessmentState.HOME)
    val state: StateFlow<AssessmentState> = _state.asStateFlow()

    private val _faceDetected = MutableStateFlow(false)
    val faceDetected: StateFlow<Boolean> = _faceDetected.asStateFlow()

    private val _faceCount = MutableStateFlow(0)
    val faceCount: StateFlow<Int> = _faceCount.asStateFlow()

    private val _headPoseValid = MutableStateFlow(true)
    val headPoseValid: StateFlow<Boolean> = _headPoseValid.asStateFlow()

    private val _yaw = MutableStateFlow(0f)
    val yaw: StateFlow<Float> = _yaw.asStateFlow()
    private val _pitch = MutableStateFlow(0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()
    private val _roll = MutableStateFlow(0f)
    val roll: StateFlow<Float> = _roll.asStateFlow()
    private val _landmarkCount = MutableStateFlow(0)
    val landmarkCount: StateFlow<Int> = _landmarkCount.asStateFlow()

    private val _trackingQuality = MutableStateFlow(TrackingQuality.GOOD)
    val trackingQuality: StateFlow<TrackingQuality> = _trackingQuality.asStateFlow()

    private val _instruction = MutableStateFlow("")
    val instruction: StateFlow<String> = _instruction.asStateFlow()

    private val _result = MutableStateFlow<FacialAssessmentResult?>(null)
    val result: StateFlow<FacialAssessmentResult?> = _result.asStateFlow()

    private val _currentLandmarks = MutableStateFlow<List<FacialLandmarkPoint>>(emptyList())
    val currentLandmarks: StateFlow<List<FacialLandmarkPoint>> = _currentLandmarks.asStateFlow()

    private val _imageDimensions = MutableStateFlow(Pair(0, 0))
    val imageDimensions: StateFlow<Pair<Int, Int>> = _imageDimensions.asStateFlow()

    private val _fps = MutableStateFlow(0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val symmetryCalculator = FacialSymmetryCalculator()
    private val smileAnalyzer = SmileAnalyzer()
    private val headPoseAnalyzer = HeadPoseAnalyzer()
    private val trackingQualityAnalyzer = TrackingQualityAnalyzer()
    private val smoother = LandmarkSmoother(alpha = 0.35f)

    private var neutralFrames = mutableListOf<FacialLandmarkFrame>()
    private var smileFrames = mutableListOf<FacialLandmarkFrame>()
    private var frameCount = 0
    private var lastFrameTime = 0L
    private var neutralStartTime = 0L
    private var smileStartTime = 0L
    private var calibrationFrames = 0

    val faceLandmarkerService = FaceLandmarkerService(application)

    fun initializeMediaPipe() {
        faceLandmarkerService.onResult = { result: FaceLandmarkerResult, timestampMs: Long ->
            handleFaceLandmarkerResult(result, timestampMs)
        }
        faceLandmarkerService.onError = { error: String ->
            Log.e(TAG, "MediaPipe onError: $error")
            viewModelScope.launch {
                _errorMessage.value = error
            }
        }
        faceLandmarkerService.initialize()
    }

    fun startAssessment() {
        _state.value = AssessmentState.PERMISSION_REQUEST
    }

    fun onCameraPermissionGranted() {
        _state.value = AssessmentState.CALIBRATING
        _instruction.value = "Position your face in the center of the screen."
        calibrationFrames = 0
        smoother.reset()
    }

    fun onCameraPermissionDenied() {
        _errorMessage.value = "Camera permission is required for face assessment."
    }

    private fun handleFaceLandmarkerResult(result: FaceLandmarkerResult, resultTimestampMs: Long) {
        viewModelScope.launch {
            val faceCount = result.faceLandmarks().size
            _faceCount.value = faceCount
            _faceDetected.value = faceCount > 0

            Log.d(
                TAG,
                "Detect tsMs=$resultTimestampMs faces=$faceCount state=${_state.value}"
            )

            if (faceCount == 0) {
                Log.d(TAG, "No face detected tsMs=$resultTimestampMs")
                _instruction.value = "Position your face inside the guide."
                return@launch
            }

            if (faceCount > 1) {
                Log.d(TAG, "Multiple faces detected: $faceCount")
                _instruction.value = "Please ensure only one face is visible."
                return@launch
            }

            val landmarks = result.faceLandmarks()[0]
            val timestampMs = resultTimestampMs
            val imageWidth = result.faceLandmarks()[0].size

            val facialPoints = landmarks.mapIndexed { index, landmark ->
                FacialLandmarkPoint(
                    x = landmark.x(),
                    y = landmark.y(),
                    z = landmark.z(),
                    index = index
                )
            }

            _currentLandmarks.value = facialPoints
            _landmarkCount.value = facialPoints.size
            _imageDimensions.value = Pair(640, 480)

            val frame = FacialLandmarkFrame(
                landmarks = facialPoints,
                timestampMs = timestampMs,
                imageWidth = 640,
                imageHeight = 480
            )

            val smoothedFrame = smoother.smooth(frame)
            val stability = smoother.stability()

            val headPose = headPoseAnalyzer.analyze(smoothedFrame)
            _headPoseValid.value = headPose.isAcceptable
            _yaw.value = headPose.yaw
            _pitch.value = headPose.pitch
            _roll.value = headPose.roll

            Log.d(
                TAG,
                "HEAD_POSE yaw=${headPose.yaw} pitch=${headPose.pitch} " +
                    "roll=${headPose.roll} isAcceptable=${headPose.isAcceptable} " +
                    "landmarkCount=${facialPoints.size} faceCount=$faceCount"
            )

            val quality = trackingQualityAnalyzer.analyze(
                frame = smoothedFrame,
                stability = stability,
                headPoseYaw = headPose.yaw,
                headPosePitch = headPose.pitch
            )
            _trackingQuality.value = quality

            // Periodic debug summary (every ~30 frames) for measurement validation.
            if (frameCount % 30 == 0) {
                val jitter = smoother.regionJitter()
                Log.d(
                    TAG,
                    "SUMMARY frame=$frameCount landmarks=${facialPoints.size} " +
                        "stability=$stability quality=$quality " +
                        "yaw=${headPose.yaw} pitch=${headPose.pitch} roll=${headPose.roll} " +
                        "fps=${_fps.value} jitter=$jitter"
                )
            }

            Log.d(
                TAG,
                "Landmarks=${facialPoints.size} stability=$stability quality=$quality " +
                    "yaw=${headPose.yaw} pitch=${headPose.pitch} roll=${headPose.roll}" +
                    "tsMs=$timestampMs"
            )

            if (!headPose.isAcceptable) {
                Log.d(TAG, "Head pose not acceptable: yaw=${headPose.yaw} pitch=${headPose.pitch}")
                _instruction.value = "Please face the camera directly."
                return@launch
            }

            if (quality == TrackingQuality.POOR) {
                Log.d(TAG, "Tracking quality POOR, stability=$stability")
                _instruction.value = "Tracking quality is poor. Please reposition your face."
                return@launch
            }

            updateFps(timestampMs)

            when (_state.value) {
                AssessmentState.CALIBRATING -> handleCalibration(smoothedFrame)
                AssessmentState.NEUTRAL_FACE -> handleNeutralFace(smoothedFrame)
                AssessmentState.SMILE_TEST -> handleSmileCollection(smoothedFrame)
                AssessmentState.COLLECTING_SMILE -> handleSmileCollection(smoothedFrame)
                else -> {}
            }
        }
    }

    private fun handleCalibration(frame: FacialLandmarkFrame) {
        calibrationFrames++
        if (calibrationFrames >= 15) {
            _state.value = AssessmentState.NEUTRAL_FACE
            _instruction.value = "Look straight at the camera and relax your face."
            neutralStartTime = System.currentTimeMillis()
            neutralFrames.clear()
            smileAnalyzer.reset()
        } else {
            _instruction.value = "Position your face in the center of the screen."
        }
    }

    private fun handleNeutralFace(frame: FacialLandmarkFrame) {
        val elapsed = System.currentTimeMillis() - neutralStartTime
        if (elapsed < NEUTRAL_COLLECTION_DURATION_MS) {
            neutralFrames.add(frame)
            val remaining = ((NEUTRAL_COLLECTION_DURATION_MS - elapsed) / 1000).toInt() + 1
            _instruction.value = "Hold still... $remaining seconds remaining."
        } else {
            if (neutralFrames.isNotEmpty()) {
                val avgFrame = averageFrames(neutralFrames)
                smileAnalyzer.setNeutralFrame(avgFrame)
            }
            _state.value = AssessmentState.SMILE_TEST
            _instruction.value = "Now smile naturally."
            smileStartTime = System.currentTimeMillis()
            smileFrames.clear()
            smileAnalyzer.startSmileCapture()
            Log.d(TAG, "SMILE_TEST_STARTED smileStartTimeMs=$smileStartTime")
        }
    }

    private fun handleSmileCollection(frame: FacialLandmarkFrame) {
        val elapsed = System.currentTimeMillis() - smileStartTime
        smileFrames.add(frame)
        smileAnalyzer.addSmileFrame(frame)
        if (smileFrames.size == 1) {
            Log.d(
                TAG,
                "SMILE_DETECTED firstSmileFrame collected, frames=${smileFrames.size}"
            )
        }
        Log.d(
            TAG,
            "SMILE_PROGRESS elapsedMs=$elapsed frames=${smileFrames.size} " +
                "remainingMs=${(SMILE_COLLECTION_DURATION_MS - elapsed).coerceAtLeast(0)}"
        )
        if (elapsed < SMILE_COLLECTION_DURATION_MS) {
            val remaining = ((SMILE_COLLECTION_DURATION_MS - elapsed) / 1000).toInt() + 1
            _instruction.value = "Smile! $remaining seconds remaining."
        } else {
            Log.d(
                TAG,
                "SMILE_TEST_COMPLETED frames=${smileFrames.size} elapsedMs=$elapsed " +
                    "requiredMs=${SMILE_COLLECTION_DURATION_MS}"
            )
            finishAssessment()
        }
    }

    private fun averageFrames(frames: List<FacialLandmarkFrame>): FacialLandmarkFrame {
        if (frames.isEmpty()) return frames.firstOrNull() ?: throw IllegalStateException("No frames")

        val avgLandmarks = mutableListOf<FacialLandmarkPoint>()
        val firstFrame = frames.first()

        for (i in firstFrame.landmarks.indices) {
            var sumX = 0f
            var sumY = 0f
            var sumZ = 0f
            var count = 0
            for (frame in frames) {
                if (i < frame.landmarks.size) {
                    sumX += frame.landmarks[i].x
                    sumY += frame.landmarks[i].y
                    sumZ += frame.landmarks[i].z
                    count++
                }
            }
            if (count > 0) {
                avgLandmarks.add(
                    FacialLandmarkPoint(
                        sumX / count,
                        sumY / count,
                        sumZ / count,
                        firstFrame.landmarks[i].index
                    )
                )
            }
        }

        return FacialLandmarkFrame(
            landmarks = avgLandmarks,
            timestampMs = firstFrame.timestampMs,
            imageWidth = firstFrame.imageWidth,
            imageHeight = firstFrame.imageHeight
        )
    }

    private fun finishAssessment() {
        _state.value = AssessmentState.CALIBRATING
        _instruction.value = "Calculating results..."

        viewModelScope.launch {
            try {
                val neutralFrame = if (neutralFrames.isNotEmpty()) averageFrames(neutralFrames) else null
                val measurements = if (neutralFrame != null) {
                    symmetryCalculator.calculate(neutralFrame)
                } else {
                    FacialMeasurements(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                }

                val smileAssessment = smileAnalyzer.calculate()

                val landmarkStability = smoother.stability()

                val result = FacialAssessmentResult(
                    measurements = measurements,
                    smileAssessment = smileAssessment,
                    trackingQuality = _trackingQuality.value,
                    headPoseValid = _headPoseValid.value,
                    faceDetected = _faceDetected.value,
                    faceCount = _faceCount.value,
                    fps = _fps.value,
                    neutralFrameCount = neutralFrames.size,
                    smileFrameCount = smileFrames.size,
                    landmarkStability = landmarkStability
                )

                _result.value = result
                _state.value = AssessmentState.RESULTS
                Log.d(
                    TAG,
                    "RESULT_SUMMARY " +
                        "overall=${format(measurements.overallSymmetryScore)} " +
                        "mouthCornerAsym=${format(measurements.mouthCornerAsymmetry)} " +
                        "mouthLineTilt=${format(measurements.mouthAngle)} " +
                        "mouthDeviation=${format(measurements.mouthCornerDeviation)} " +
                        "eyeAsym=${format(measurements.eyeOpeningAsymmetry)} " +
                        "eyeDeviation=${format(measurements.eyeOpeningDeviation)} " +
                        "eyebrowAsym=${format(measurements.eyebrowAsymmetry)} " +
                        "eyebrowDeviation=${format(measurements.eyebrowDeviation)} " +
                        "smileSymmetry=${format(smileAssessment.smileSymmetry)} " +
                        "leftExcursion=${format2(smileAssessment.leftExcursion)} " +
                        "rightExcursion=${format2(smileAssessment.rightExcursion)} " +
                        "excursionRatio=${format2(smileAssessment.excursionRatio)} " +
                        "trackingStability=${format(landmarkStability * 100f)} " +
                        "fps=${format(_fps.value)} " +
                        "neutralFrames=${neutralFrames.size} " +
                        "smileFrames=${smileFrames.size}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating results", e)
                _errorMessage.value = "Error calculating results: ${e.message}"
                _state.value = AssessmentState.HOME
            }
        }
    }

    private fun updateFps(timestampMs: Long) {
        frameCount++
        if (lastFrameTime > 0) {
            val elapsed = timestampMs - lastFrameTime
            if (elapsed > 0) {
                _fps.value = 1000f / elapsed
            }
        }
        lastFrameTime = timestampMs
    }

    private fun format(value: Float): String = String.format(Locale.US, "%.1f", value)

    private fun format2(value: Float): String = String.format(Locale.US, "%.2f", value)

    fun repeatAssessment() {
        _state.value = AssessmentState.CALIBRATING
        _instruction.value = "Position your face in the center of the screen."
        calibrationFrames = 0
        neutralFrames.clear()
        smileFrames.clear()
        smoother.reset()
        trackingQualityAnalyzer.reset()
        smileAnalyzer.reset()
        _result.value = null
    }

    fun newAssessment() {
        repeatAssessment()
    }

    fun goHome() {
        _state.value = AssessmentState.HOME
        _result.value = null
        neutralFrames.clear()
        smileFrames.clear()
        smoother.reset()
        trackingQualityAnalyzer.reset()
        smileAnalyzer.reset()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        faceLandmarkerService.close()
    }
}
