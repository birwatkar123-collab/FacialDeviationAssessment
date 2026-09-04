package com.facialdeviation.assessment.analysis

import com.facialdeviation.assessment.model.FacialLandmarkFrame
import com.facialdeviation.assessment.model.FacialLandmarkPoint
import kotlin.math.sqrt

class LandmarkSmoother(private val alpha: Float = 0.3f) {

    private var smoothedFrame: FacialLandmarkFrame? = null
    private val frameHistory = mutableListOf<FacialLandmarkFrame>()
    private val maxHistory = 5

    fun reset() {
        smoothedFrame = null
        frameHistory.clear()
    }

    fun smooth(frame: FacialLandmarkFrame): FacialLandmarkFrame {
        val prev = smoothedFrame
        if (prev == null) {
            smoothedFrame = frame
            frameHistory.add(frame)
            if (frameHistory.size > maxHistory) frameHistory.removeAt(0)
            return frame
        }

        val smoothed = frame.landmarks.map { newPoint ->
            val prevPoint = prev.landmarks.find { it.index == newPoint.index }
            if (prevPoint != null) {
                FacialLandmarkPoint(
                    x = alpha * newPoint.x + (1 - alpha) * prevPoint.x,
                    y = alpha * newPoint.y + (1 - alpha) * prevPoint.y,
                    z = alpha * newPoint.z + (1 - alpha) * prevPoint.z,
                    index = newPoint.index
                )
            } else {
                newPoint
            }
        }

        val result = FacialLandmarkFrame(
            landmarks = smoothed,
            timestampMs = frame.timestampMs,
            imageWidth = frame.imageWidth,
            imageHeight = frame.imageHeight
        )
        smoothedFrame = result
        frameHistory.add(result)
        if (frameHistory.size > maxHistory) frameHistory.removeAt(0)
        return result
    }

    fun stability(): Float {
        if (frameHistory.size < 2) return 1f
        var totalJitter = 0f
        var count = 0
        for (i in 1 until frameHistory.size) {
            val prev = frameHistory[i - 1]
            val curr = frameHistory[i]
            for (j in 0 until minOf(prev.landmarks.size, curr.landmarks.size)) {
                val lp = prev.landmarks[j]
                val lc = curr.landmarks[j]
                if (lp.index == lc.index) {
                    val dx = lp.x - lc.x
                    val dy = lp.y - lc.y
                    totalJitter += sqrt(dx * dx + dy * dy)
                    count++
                }
            }
        }
        if (count == 0) return 1f
        val avgJitter = totalJitter / count
        return (1f - (avgJitter * 100f)).coerceIn(0f, 1f)
    }

    /**
     * Average per-region jitter over the recent history in normalized units.
     * Regions: mouth corners, eye corners, eyelids, eyebrows, nose, contour.
     */
    fun regionJitter(): Map<String, Float> {
        if (frameHistory.size < 2) return emptyMap()

        val regions = mapOf(
            "mouth_corners" to listOf(61, 291),
            "eye_corners" to listOf(33, 133, 362, 263),
            "eyelids" to listOf(159, 145, 386, 374),
            "eyebrows" to listOf(70, 300),
            "nose" to listOf(1, 6),
            "contour" to listOf(234, 454, 152)
        )

        val result = mutableMapOf<String, Float>()
        for ((region, indices) in regions) {
            var totalJitter = 0f
            var count = 0
            for (i in 1 until frameHistory.size) {
                val prev = frameHistory[i - 1]
                val curr = frameHistory[i]
                for (idx in indices) {
                    val lp = prev.landmarks.find { it.index == idx } ?: continue
                    val lc = curr.landmarks.find { it.index == idx } ?: continue
                    val dx = lp.x - lc.x
                    val dy = lp.y - lc.y
                    totalJitter += sqrt(dx * dx + dy * dy)
                    count++
                }
            }
            if (count > 0) result[region] = totalJitter / count
        }
        return result
    }
}
