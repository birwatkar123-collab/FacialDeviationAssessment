package com.facialdeviation.assessment.analysis

import com.facialdeviation.assessment.model.FacialLandmarkFrame
import com.facialdeviation.assessment.model.FacialMeasurements
import com.facialdeviation.assessment.utils.FaceLandmarkUtils
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2

class FacialSymmetryCalculator {

    fun calculate(frame: FacialLandmarkFrame): FacialMeasurements {
        val mouthCornerAsymmetry = calculateMouthCornerAsymmetry(frame)
        val mouthAngle = calculateMouthAngle(frame)
        val eyebrowAsymmetry = calculateEyebrowAsymmetry(frame)
        val eyeOpeningAsymmetry = calculateEyeOpeningAsymmetry(frame)

        val overallScore = calculateOverallSymmetry(
            mouthCornerAsymmetry,
            mouthAngle,
            eyebrowAsymmetry,
            eyeOpeningAsymmetry
        )

        val midline = FaceLandmarkUtils.facialMidline(frame)
        val midlineDeviation = if (midline != null) {
            val mouthCenter = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.LEFT_MOUTH)?.let { left ->
                FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.RIGHT_MOUTH)?.let { right ->
                    FaceLandmarkUtils.midpoint(left, right)
                }
            }
            if (mouthCenter != null) {
                FaceLandmarkUtils.distanceFromMidline(mouthCenter, midline.first, midline.second)
            } else 0f
        } else 0f

        val mouthAngleDeviation = abs(mouthAngle - 0f)

        val leftEyebrowHeight = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.LEFT_EYEBROW)?.let { eb ->
            FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.LEFT_EYE_TOP)?.let { eye ->
                FaceLandmarkUtils.distance2D(eb, eye)
            }
        } ?: 0f
        val rightEyebrowHeight = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.RIGHT_EYEBROW)?.let { eb ->
            FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.RIGHT_EYE_TOP)?.let { eye ->
                FaceLandmarkUtils.distance2D(eb, eye)
            }
        } ?: 0f
        val eyebrowDev = abs(leftEyebrowHeight - rightEyebrowHeight)
        val eyebrowDeviation = FaceLandmarkUtils.normalizeDistance(frame, eyebrowDev)

        val leftEyeOpen = FaceLandmarkUtils.eyeOpening(
            frame, FaceLandmarkUtils.LEFT_EYE_TOP, FaceLandmarkUtils.LEFT_EYE_BOTTOM
        )
        val rightEyeOpen = FaceLandmarkUtils.eyeOpening(
            frame, FaceLandmarkUtils.RIGHT_EYE_TOP, FaceLandmarkUtils.RIGHT_EYE_BOTTOM
        )
        val eyeDiff = abs(leftEyeOpen - rightEyeOpen)
        val eyeDeviation = FaceLandmarkUtils.normalizeDistance(frame, eyeDiff)

        return FacialMeasurements(
            mouthCornerAsymmetry = mouthCornerAsymmetry,
            mouthAngle = mouthAngle,
            eyebrowAsymmetry = eyebrowAsymmetry,
            eyeOpeningAsymmetry = eyeOpeningAsymmetry,
            overallSymmetryScore = overallScore,
            mouthCornerDeviation = FaceLandmarkUtils.normalizeDistance(frame, midlineDeviation),
            mouthAngleDeviation = mouthAngleDeviation,
            eyebrowDeviation = eyebrowDeviation,
            eyeOpeningDeviation = eyeDeviation
        )
    }

    private fun calculateMouthCornerAsymmetry(frame: FacialLandmarkFrame): Float {
        val left = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.LEFT_MOUTH) ?: return 0f
        val right = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.RIGHT_MOUTH) ?: return 0f
        val midline = FaceLandmarkUtils.facialMidline(frame) ?: return 0f

        val leftDist = FaceLandmarkUtils.distanceFromMidline(left, midline.first, midline.second)
        val rightDist = FaceLandmarkUtils.distanceFromMidline(right, midline.first, midline.second)

        if (leftDist + rightDist < 1e-6f) return 0f
        val asymmetry = abs(leftDist - rightDist) / ((leftDist + rightDist) / 2f) * 100f
        return asymmetry.coerceIn(0f, 100f)
    }

    private fun calculateMouthAngle(frame: FacialLandmarkFrame): Float {
        val left = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.LEFT_MOUTH) ?: return 0f
        val right = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.RIGHT_MOUTH) ?: return 0f
        val dx = abs(right.x - left.x)
        val dy = abs(right.y - left.y)
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    private fun calculateEyebrowAsymmetry(frame: FacialLandmarkFrame): Float {
        val leftEyeTop = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.LEFT_EYE_TOP) ?: return 0f
        val rightEyeTop = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.RIGHT_EYE_TOP) ?: return 0f
        val leftEb = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.LEFT_EYEBROW) ?: return 0f
        val rightEb = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.RIGHT_EYEBROW) ?: return 0f

        val leftHeight = FaceLandmarkUtils.distance2D(leftEb, leftEyeTop)
        val rightHeight = FaceLandmarkUtils.distance2D(rightEb, rightEyeTop)

        val avgHeight = (leftHeight + rightHeight) / 2f
        if (avgHeight < 1e-6f) return 0f
        val asymmetry = abs(leftHeight - rightHeight) / avgHeight * 100f
        return asymmetry.coerceIn(0f, 100f)
    }

    private fun calculateEyeOpeningAsymmetry(frame: FacialLandmarkFrame): Float {
        val leftOpen = FaceLandmarkUtils.eyeOpening(
            frame, FaceLandmarkUtils.LEFT_EYE_TOP, FaceLandmarkUtils.LEFT_EYE_BOTTOM
        )
        val rightOpen = FaceLandmarkUtils.eyeOpening(
            frame, FaceLandmarkUtils.RIGHT_EYE_TOP, FaceLandmarkUtils.RIGHT_EYE_BOTTOM
        )

        val avgOpen = (leftOpen + rightOpen) / 2f
        if (avgOpen < 1e-6f) return 0f
        val asymmetry = abs(leftOpen - rightOpen) / avgOpen * 100f
        return asymmetry.coerceIn(0f, 100f)
    }

    private fun calculateOverallSymmetry(
        mouthAsym: Float,
        mouthAngle: Float,
        eyebrowAsym: Float,
        eyeAsym: Float
    ): Float {
        val mouthScore = (100f - mouthAsym).coerceIn(0f, 100f)
        val mouthAngleScore = (100f - (mouthAngle / 90f * 100f)).coerceIn(0f, 100f)
        val eyebrowScore = (100f - eyebrowAsym).coerceIn(0f, 100f)
        val eyeScore = (100f - eyeAsym).coerceIn(0f, 100f)

        val mouthWeight = 0.30f
        val mouthAngleWeight = 0.20f
        val eyebrowWeight = 0.25f
        val eyeWeight = 0.25f

        log(
            "SYMMETRY_COMPONENTS mouthCornerAsym=%.2f%% -> score=%.2f/100 weight=%.2f | " +
                "mouthLineTilt=%.2f deg -> score=%.2f/100 weight=%.2f | " +
                "eyebrowAsym=%.2f%% -> score=%.2f/100 weight=%.2f | " +
                "eyeAsym=%.2f%% -> score=%.2f/100 weight=%.2f"
                .format(
                    Locale.US,
                    mouthAsym, mouthScore, mouthWeight,
                    mouthAngle, mouthAngleScore, mouthAngleWeight,
                    eyebrowAsym, eyebrowScore, eyebrowWeight,
                    eyeAsym, eyeScore, eyeWeight
                )
        )

        val score = (
            mouthScore * mouthWeight +
                mouthAngleScore * mouthAngleWeight +
                eyebrowScore * eyebrowWeight +
                eyeScore * eyeWeight
            ).coerceIn(0f, 100f)

        log(
            "SYMMETRY_TOTAL overall=%.2f/100 weights=(mouthCorner 0.30, mouthLineTilt 0.20, " +
                "eyebrow 0.25, eye 0.25) sumOfWeights=%.2f"
                .format(
                    Locale.US,
                    score,
                    mouthWeight + mouthAngleWeight + eyebrowWeight + eyeWeight
                )
        )

        return score
    }

    private fun log(message: String) {
        try {
            android.util.Log.d(TAG, message)
        } catch (t: Throwable) {
            // JVM unit tests run against the mockable android.jar whose
            // android.util.Log methods throw "Stub!" at runtime; swallow.
        }
    }

    private companion object {
        const val TAG = "FacialSymmetryCalculator"
    }
}
