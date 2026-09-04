package com.facialdeviation.assessment.model

data class FacialLandmarkPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val index: Int
)

data class FacialLandmarkFrame(
    val landmarks: List<FacialLandmarkPoint>,
    val timestampMs: Long,
    val imageWidth: Int,
    val imageHeight: Int
)

enum class TrackingQuality {
    GOOD,
    FAIR,
    POOR
}

data class FacialMeasurements(
    val mouthCornerAsymmetry: Float,
    val mouthAngle: Float,
    val eyebrowAsymmetry: Float,
    val eyeOpeningAsymmetry: Float,
    val overallSymmetryScore: Float,
    val mouthCornerDeviation: Float,
    val mouthAngleDeviation: Float,
    val eyebrowDeviation: Float,
    val eyeOpeningDeviation: Float
)

data class SmileAssessment(
    val smileSymmetry: Float,
    val leftExcursion: Float,
    val rightExcursion: Float,
    val excursionRatio: Float,
    val smileAsymmetry: Float,
    val maxExcursion: Float,
    val movementDurationMs: Long
)

data class FacialAssessmentResult(
    val measurements: FacialMeasurements,
    val smileAssessment: SmileAssessment,
    val trackingQuality: TrackingQuality,
    val headPoseValid: Boolean,
    val faceDetected: Boolean,
    val faceCount: Int,
    val fps: Float,
    val neutralFrameCount: Int,
    val smileFrameCount: Int,
    val landmarkStability: Float
)
