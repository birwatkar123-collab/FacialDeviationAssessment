package com.facialdeviation.assessment.utils

import com.facialdeviation.assessment.model.FacialLandmarkFrame
import com.facialdeviation.assessment.model.FacialLandmarkPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object FaceLandmarkUtils {

    const val NOSE_TIP = 1
    const val LEFT_MOUTH = 61
    const val RIGHT_MOUTH = 291
    const val UPPER_LIP = 13
    const val LOWER_LIP = 14
    const val LEFT_EYE_TOP = 159
    const val LEFT_EYE_BOTTOM = 145
    const val RIGHT_EYE_TOP = 386
    const val RIGHT_EYE_BOTTOM = 374
    const val LEFT_EYEBROW = 70
    const val RIGHT_EYEBROW = 300
    const val LEFT_EYE_INNER = 133
    const val LEFT_EYE_OUTER = 33
    const val RIGHT_EYE_INNER = 362
    const val RIGHT_EYE_OUTER = 263
    const val NOSE_BRIDGE = 6
    const val CHIN = 152
    const val FOREHEAD_TOP = 10
    const val LEFT_EAR = 234
    const val RIGHT_EAR = 454

    fun getLandmark(frame: FacialLandmarkFrame, index: Int): FacialLandmarkPoint? {
        return frame.landmarks.find { it.index == index }
    }

    fun distance(a: FacialLandmarkPoint, b: FacialLandmarkPoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun distance2D(a: FacialLandmarkPoint, b: FacialLandmarkPoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    fun midpoint(a: FacialLandmarkPoint, b: FacialLandmarkPoint): FacialLandmarkPoint {
        return FacialLandmarkPoint(
            (a.x + b.x) / 2f,
            (a.y + b.y) / 2f,
            (a.z + b.z) / 2f,
            -1
        )
    }

    fun facialMidline(frame: FacialLandmarkFrame): Pair<FacialLandmarkPoint, FacialLandmarkPoint>? {
        val noseTip = getLandmark(frame, NOSE_TIP) ?: return null
        val noseBridge = getLandmark(frame, NOSE_BRIDGE) ?: return null
        return Pair(noseBridge, noseTip)
    }

    fun facialCenterPoint(frame: FacialLandmarkFrame): FacialLandmarkPoint? {
        val noseTip = getLandmark(frame, NOSE_TIP) ?: return null
        val noseBridge = getLandmark(frame, NOSE_BRIDGE) ?: return null
        val chin = getLandmark(frame, CHIN) ?: return null
        return midpoint(midpoint(noseBridge, noseTip), chin)
    }

    fun distanceFromMidline(
        point: FacialLandmarkPoint,
        midlineTop: FacialLandmarkPoint,
        midlineBottom: FacialLandmarkPoint
    ): Float {
        val dx = midlineBottom.x - midlineTop.x
        val dy = midlineBottom.y - midlineTop.y
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1e-6f) return 0f
        val nx = -dy / len
        val ny = dx / len
        val px = point.x - midlineTop.x
        val py = point.y - midlineTop.y
        return abs(px * nx + py * ny)
    }

    fun horizontalPosition(point: FacialLandmarkPoint): Float {
        return point.x
    }

    fun verticalPosition(point: FacialLandmarkPoint): Float {
        return point.y
    }

    fun angle(a: FacialLandmarkPoint, b: FacialLandmarkPoint, c: FacialLandmarkPoint): Float {
        val ab = Pair(b.x - a.x, b.y - a.y)
        val cb = Pair(b.x - c.x, b.y - c.y)
        val dot = ab.first * cb.first + ab.second * cb.second
        val magAB = sqrt(ab.first * ab.first + ab.second * ab.second)
        val magCB = sqrt(cb.first * cb.first + cb.second * cb.second)
        if (magAB < 1e-6f || magCB < 1e-6f) return 0f
        val cosAngle = (dot / (magAB * magCB)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosAngle.toDouble())).toFloat()
    }

    fun eyeOpening(
        frame: FacialLandmarkFrame,
        topIndex: Int,
        bottomIndex: Int
    ): Float {
        val top = getLandmark(frame, topIndex) ?: return 0f
        val bottom = getLandmark(frame, bottomIndex) ?: return 0f
        return distance2D(top, bottom)
    }

    fun faceWidth(frame: FacialLandmarkFrame): Float {
        val left = getLandmark(frame, LEFT_EAR) ?: return 1f
        val right = getLandmark(frame, RIGHT_EAR) ?: return 1f
        return distance2D(left, right).coerceAtLeast(0.001f)
    }

    fun faceHeight(frame: FacialLandmarkFrame): Float {
        val top = getLandmark(frame, FOREHEAD_TOP) ?: return 1f
        val bottom = getLandmark(frame, CHIN) ?: return 1f
        return distance2D(top, bottom).coerceAtLeast(0.001f)
    }

    fun normalizeDistance(frame: FacialLandmarkFrame, distance: Float): Float {
        val fw = faceWidth(frame)
        return (distance / fw) * 100f
    }

    private fun acos(x: Double): Double {
        return when {
            x >= 1.0 -> 0.0
            x <= -1.0 -> Math.PI
            else -> kotlin.math.acos(x)
        }
    }
}
