package com.facialdeviation.assessment.analysis

import com.facialdeviation.assessment.model.FacialLandmarkFrame
import com.facialdeviation.assessment.model.FacialLandmarkPoint
import com.facialdeviation.assessment.model.SmileAssessment
import com.facialdeviation.assessment.utils.FaceLandmarkUtils
import kotlin.math.abs

class SmileAnalyzer {

    private var neutralFrame: FacialLandmarkFrame? = null
    private val smileFrames = mutableListOf<FacialLandmarkFrame>()
    private var smileStartTime: Long = 0
    private var captureNeutral = true

    fun reset() {
        neutralFrame = null
        smileFrames.clear()
        smileStartTime = 0
        captureNeutral = true
    }

    fun setNeutralFrame(frame: FacialLandmarkFrame) {
        neutralFrame = frame
    }

    fun startSmileCapture() {
        captureNeutral = false
        smileFrames.clear()
        smileStartTime = System.currentTimeMillis()
    }

    fun addSmileFrame(frame: FacialLandmarkFrame) {
        if (!captureNeutral) {
            smileFrames.add(frame)
        }
    }

    fun calculate(): SmileAssessment {
        val neutral = neutralFrame ?: return defaultAssessment()

        if (smileFrames.isEmpty()) {
            return defaultAssessment()
        }

        val leftNeutral = FaceLandmarkUtils.getLandmark(neutral, FaceLandmarkUtils.LEFT_MOUTH)
        val rightNeutral = FaceLandmarkUtils.getLandmark(neutral, FaceLandmarkUtils.RIGHT_MOUTH)

        if (leftNeutral == null || rightNeutral == null) {
            return defaultAssessment()
        }

        var maxLeftExcursion = 0f
        var maxRightExcursion = 0f
        var bestLeftFrame: FacialLandmarkPoint? = null
        var bestRightFrame: FacialLandmarkPoint? = null

        for (smileFrame in smileFrames) {
            val leftSmile = FaceLandmarkUtils.getLandmark(smileFrame, FaceLandmarkUtils.LEFT_MOUTH)
            val rightSmile = FaceLandmarkUtils.getLandmark(smileFrame, FaceLandmarkUtils.RIGHT_MOUTH)

            if (leftSmile != null && rightSmile != null) {
                val leftExc = FaceLandmarkUtils.distance2D(leftNeutral, leftSmile)
                val rightExc = FaceLandmarkUtils.distance2D(rightNeutral, rightSmile)

                if (leftExc > maxLeftExcursion) {
                    maxLeftExcursion = leftExc
                    bestLeftFrame = leftSmile
                }
                if (rightExc > maxRightExcursion) {
                    maxRightExcursion = rightExc
                    bestRightFrame = rightSmile
                }
            }
        }

        val maxExcursion = maxOf(maxLeftExcursion, maxRightExcursion)
        val avgExcursion = (maxLeftExcursion + maxRightExcursion) / 2f
        val excursionRatio = if (avgExcursion > 1e-6f) {
            minOf(maxLeftExcursion, maxRightExcursion) / avgExcursion
        } else 1f

        val smileAsymmetry = if (avgExcursion > 1e-6f) {
            abs(maxLeftExcursion - maxRightExcursion) / avgExcursion * 100f
        } else 0f

        val smileSymmetry = (100f - smileAsymmetry).coerceIn(0f, 100f)

        val movementDuration = if (smileFrames.isNotEmpty()) {
            smileFrames.last().timestampMs - smileFrames.first().timestampMs
        } else {
            System.currentTimeMillis() - smileStartTime
        }

        val fw = FaceLandmarkUtils.faceWidth(neutral)

        return SmileAssessment(
            smileSymmetry = smileSymmetry,
            leftExcursion = (maxRightExcursion / fw) * 100f,
            rightExcursion = (maxLeftExcursion / fw) * 100f,
            excursionRatio = excursionRatio,
            smileAsymmetry = smileAsymmetry,
            maxExcursion = (maxExcursion / fw) * 100f,
            movementDurationMs = movementDuration
        )
    }

    private fun defaultAssessment(): SmileAssessment {
        return SmileAssessment(
            smileSymmetry = 0f,
            leftExcursion = 0f,
            rightExcursion = 0f,
            excursionRatio = 0f,
            smileAsymmetry = 0f,
            maxExcursion = 0f,
            movementDurationMs = 0
        )
    }

}
