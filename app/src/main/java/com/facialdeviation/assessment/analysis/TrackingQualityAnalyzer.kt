package com.facialdeviation.assessment.analysis

import android.util.Log
import com.facialdeviation.assessment.model.FacialLandmarkFrame
import com.facialdeviation.assessment.model.TrackingQuality
import com.facialdeviation.assessment.utils.FaceLandmarkUtils

/**
 * Evaluates tracking quality from multiple independent signals:
 *
 *  1. Landmark completeness  - all key landmarks present (severe issue -> POOR)
 *  2. Landmark stability     - temporal jitter of smoothed landmarks (0..1)
 *  3. Head pose              - yaw/pitch within acceptable range
 *
 * Thresholds (documented):
 *  - stability >= 0.60 -> GOOD
 *  - stability >= 0.25 -> FAIR
 *  - stability <  0.25 -> POOR
 *
 * Stability is computed on smoothed landmarks, so raw jitter of a still face
 * (~0.002-0.006 normalized units) maps to stability ~0.65-0.90 -> GOOD.
 * A genuinely unstable face (jitter >= 0.0075) maps to POOR.
 *
 * A single missing landmark pair does NOT force POOR; we only require the
 * majority of key landmarks to be present. Only if most are missing do we
 * classify as POOR.
 */
class TrackingQualityAnalyzer {

    private val recentStabilities = mutableListOf<Float>()
    private val maxSamples = 10

    companion object {
        private const val TAG = "TrackingQualityAnalyzer"
        const val STABILITY_GOOD = 0.60f
        const val STABILITY_FAIR = 0.25f
        const val REQUIRED_KEY_RATIO = 0.5f
    }

    fun reset() {
        recentStabilities.clear()
    }

    fun analyze(
        frame: FacialLandmarkFrame,
        stability: Float,
        headPoseYaw: Float,
        headPosePitch: Float,
        maxYawDegrees: Float = 20f,
        maxPitchDegrees: Float = 20f
    ): TrackingQuality {
        recentStabilities.add(stability)
        if (recentStabilities.size > maxSamples) {
            recentStabilities.removeAt(0)
        }

        val avgStability = recentStabilities.average().toFloat()

        val keyLandmarks = checkKeyLandmarks(frame)
        val quality = when {
            !keyLandmarks.allPresent -> TrackingQuality.POOR
            avgStability < STABILITY_FAIR -> TrackingQuality.POOR
            abs(headPoseYaw) > maxYawDegrees || abs(headPosePitch) > maxPitchDegrees ->
                TrackingQuality.POOR
            avgStability >= STABILITY_GOOD -> TrackingQuality.GOOD
            else -> TrackingQuality.FAIR
        }

        Log.d(
            TAG,
            "stability=$avgStability (raw=${stability}) present=${keyLandmarks.presentCount}/" +
                "${keyLandmarks.total} quality=$quality yaw=$headPoseYaw pitch=$headPosePitch"
        )
        return quality
    }

    private fun checkKeyLandmarks(frame: FacialLandmarkFrame): KeyLandmarkCount {
        val keyIndices = listOf(
            FaceLandmarkUtils.NOSE_TIP,
            FaceLandmarkUtils.LEFT_MOUTH,
            FaceLandmarkUtils.RIGHT_MOUTH,
            FaceLandmarkUtils.LEFT_EYE_TOP,
            FaceLandmarkUtils.RIGHT_EYE_TOP,
            FaceLandmarkUtils.LEFT_EYEBROW,
            FaceLandmarkUtils.RIGHT_EYEBROW,
            FaceLandmarkUtils.NOSE_BRIDGE,
            FaceLandmarkUtils.CHIN,
            FaceLandmarkUtils.LEFT_EYE_INNER,
            FaceLandmarkUtils.RIGHT_EYE_INNER,
            FaceLandmarkUtils.LEFT_EYE_OUTER,
            FaceLandmarkUtils.RIGHT_EYE_OUTER
        )

        val present = keyIndices.count { idx ->
            FaceLandmarkUtils.getLandmark(frame, idx) != null
        }
        val allPresent = present >= (keyIndices.size * REQUIRED_KEY_RATIO)
        return KeyLandmarkCount(present, keyIndices.size, allPresent)
    }

    private data class KeyLandmarkCount(
        val presentCount: Int,
        val total: Int,
        val allPresent: Boolean
    )

    private fun abs(value: Float): Float = kotlin.math.abs(value)
}