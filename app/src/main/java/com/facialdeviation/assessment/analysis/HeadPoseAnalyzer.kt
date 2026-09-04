package com.facialdeviation.assessment.analysis

import com.facialdeviation.assessment.model.FacialLandmarkFrame
import com.facialdeviation.assessment.model.FacialLandmarkPoint
import com.facialdeviation.assessment.utils.FaceLandmarkUtils
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Estimates head pose from landmark geometry. The landmarks fed into this
 * analyzer MUST be in the upright display-equivalent coordinate frame (i.e. the
 * CameraX sensor rotation must have been passed to MediaPipe via
 * ImageProcessingOptions so MediaPipe returns upright landmarks).
 *
 * Thresholds (V1, deliberately lenient):
 *  - |yaw|   <= 25 degrees
 *  - |pitch| <= 20 degrees
 *  - |roll|  <= 20 degrees
 *
 * Coordinate references (all landmarks from the SAME 478-point mesh, upright):
 *  - yaw:   nose tip displaced from the lateral mid-point of the outer eye
 *           corners (33, 263). Eyes are always present and hair-proof; the old
 *           ear-based reference (234, 454) collapsed to a ~0 span on the device
 *           (jaw-hinge points overlap the nose line at small yaw), which made
 *           atan2(noseOffset, ~0.001) saturate to +-90 degrees on a frontal face.
 *  - pitch: nose tip vs. brow-to-chin mid-point (vertical), unchanged.
 *  - roll:  tilt of the outer eye corner line, unchanged.
 *
 * Span floors: a real face's eye horizontal span on a portrait frame is >= ~0.04
 * (normalized). Flooring at 0.04 instead of 0.001 prevents a transient near-zero
 * span from being amplified into a +-90 degree reading.
 */
class HeadPoseAnalyzer(
    private val maxYawDegrees: Float = 25f,
    private val maxPitchDegrees: Float = 20f,
    private val maxRollDegrees: Float = 20f
) {

    data class HeadPose(
        val yaw: Float,
        val pitch: Float,
        val roll: Float,
        val isAcceptable: Boolean
    )

    fun analyze(frame: FacialLandmarkFrame): HeadPose {
        val noseTip = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.NOSE_TIP)
            ?: return HeadPose(0f, 0f, 0f, false)
        val noseBridge = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.NOSE_BRIDGE)
            ?: return HeadPose(0f, 0f, 0f, false)
        val foreheadTop = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.FOREHEAD_TOP)
        val chin = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.CHIN)

        val leftEyeOuter = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.LEFT_EYE_OUTER)
        val rightEyeOuter = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.RIGHT_EYE_OUTER)
        val leftEar = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.LEFT_EAR)
        val rightEar = FaceLandmarkUtils.getLandmark(frame, FaceLandmarkUtils.RIGHT_EAR)

        val minLateralSpan = 0.04f
        val minVerticalSpan = 0.04f

        // yaw: lateral reference is the outer-eye line; falls back to the ears,
        // then to a symmetric synthetic span, if those landmarks are absent.
        val lateralLeft: Float
        val lateralRight: Float
        val lateralSource: String
        if (leftEyeOuter != null && rightEyeOuter != null) {
            lateralLeft = leftEyeOuter.x
            lateralRight = rightEyeOuter.x
            lateralSource = "eyes(33,263)"
        } else if (leftEar != null && rightEar != null) {
            lateralLeft = leftEar.x
            lateralRight = rightEar.x
            lateralSource = "ears(234,454)"
        } else {
            lateralLeft = noseTip.x - 0.15f
            lateralRight = noseTip.x + 0.15f
            lateralSource = "fallback"
        }
        val lateralSpan = abs(lateralRight - lateralLeft).coerceAtLeast(minLateralSpan)
        val faceMidX = (lateralLeft + lateralRight) / 2f
        val yaw = atan2(
            (noseTip.x - faceMidX).toDouble(),
            lateralSpan.toDouble()
        )
        val yawDegrees = Math.toDegrees(yaw).toFloat()

        val topY = foreheadTop?.y ?: (noseTip.y - 0.2f)
        val bottomY = chin?.y ?: (noseTip.y + 0.2f)
        val verticalSpan = abs(bottomY - topY).coerceAtLeast(minVerticalSpan)
        val faceMidY = (topY + bottomY) / 2f
        val pitch = atan2(
            (noseTip.y - faceMidY).toDouble(),
            verticalSpan.toDouble()
        )
        val pitchDegrees = Math.toDegrees(pitch).toFloat()

        val rollDegrees = if (leftEyeOuter != null && rightEyeOuter != null) {
            val dy = leftEyeOuter.y - rightEyeOuter.y
            val horSpan = abs(leftEyeOuter.x - rightEyeOuter.x).coerceAtLeast(minLateralSpan)
            Math.toDegrees(atan2(dy.toDouble(), horSpan.toDouble())).toFloat()
        } else {
            0f
        }

        val isAcceptable =
            abs(yawDegrees) <= maxYawDegrees &&
                abs(pitchDegrees) <= maxPitchDegrees &&
                abs(rollDegrees) <= maxRollDegrees

        log(
            "yaw src=$lateralSource nose=(%.3f,%.3f) leftX=%.3f rightX=%.3f " +
                "midX=%.3f span=%.3f numerator=%.3f -> yaw=%.1f deg"
                .format(Locale.US, noseTip.x, noseTip.y, lateralLeft, lateralRight, faceMidX, lateralSpan, noseTip.x - faceMidX, yawDegrees)
        )
        log(
            "pitch brow=(%.3f,%.3f) chin=(%.3f,%.3f) midY=%.3f span=%.3f " +
                "numerator=%.3f -> pitch=%.1f deg"
                .format(Locale.US, topY, noseTip.y - 0.2f, bottomY, noseTip.y + 0.2f, faceMidY, verticalSpan, noseTip.y - faceMidY, pitchDegrees)
        )
        log(
            "roll leftEye=(%.3f,%.3f) rightEye=(%.3f,%.3f) dx=%.3f dy=%.3f -> roll=%.1f deg"
                .format(
                    Locale.US,
                    leftEyeOuter?.x ?: 0f, leftEyeOuter?.y ?: 0f,
                    rightEyeOuter?.x ?: 0f, rightEyeOuter?.y ?: 0f,
                    abs((leftEyeOuter?.x ?: 0f) - (rightEyeOuter?.x ?: 0f)),
                    (leftEyeOuter?.y ?: 0f) - (rightEyeOuter?.y ?: 0f),
                    rollDegrees
                )
        )
        log(
            "HEAD_POSE yaw=%.1f pitch=%.1f roll=%.1f acceptable=%s src=%s"
                .format(Locale.US, yawDegrees, pitchDegrees, rollDegrees, isAcceptable, lateralSource)
        )

        diagnoseOneFrame(frame, noseTip, leftEyeOuter, rightEyeOuter, faceMidX, lateralSpan, verticalSpan, topY, bottomY)

        return HeadPose(
            yaw = yawDegrees,
            pitch = pitchDegrees,
            roll = rollDegrees,
            isAcceptable = isAcceptable
        )
    }

    private fun diagnoseOneFrame(
        frame: FacialLandmarkFrame,
        noseTip: FacialLandmarkPoint,
        leftEyeOuter: FacialLandmarkPoint?,
        rightEyeOuter: FacialLandmarkPoint?,
        faceMidX: Float,
        lateralSpan: Float,
        verticalSpan: Float,
        topY: Float,
        bottomY: Float
    ) {
        if (diagnosticPrinted) return
        diagnosticPrinted = true

        val targets = intArrayOf(33, 263, 1, 168, 10, 152, 6, 234, 454)
        val sb = StringBuilder()
        for (id in targets) {
            val lp = FaceLandmarkUtils.getLandmark(frame, id)
            sb.append('[').append(id).append('=')
            if (lp != null) {
                sb.append(String.format(Locale.US, "(%.3f,%.3f,%.4f)", lp.x, lp.y, lp.z))
            } else {
                sb.append("null")
            }
            sb.append(']')
        }
        log("RAWFACE" + sb)

        val left = leftEyeOuter
        val right = rightEyeOuter
        if (left != null && right != null) {
            val eyeDx = right.x - left.x
            val eyeDy = right.y - left.y
            val eyeAngle = Math.toDegrees(atan2(eyeDy.toDouble(), eyeDx.toDouble())).toFloat()
            val rollDy = left.y - right.y
            val rollDx = abs(left.x - right.x)
            log(
                "DIAGNOSE leftEye=(%.3f,%.3f,%.4f) rightEye=(%.3f,%.3f,%.4f) nose=(%.3f,%.3f,%.4f)" +
                    " | 263-33: eyeDx=%.3f eyeDy=%.3f eyeAngle=%.1f deg" +
                    " | roll num(dy)=%.3f den(|dx|)=%.3f floor=%.3f" +
                    " | yaw num(noseX-midX)=%.3f den(span)=%.3f floor=%.3f midX=%.3f" +
                    " | pitch num(noseY-midY)=%.3f den(|bottom-top|)=%.3f topY=%.3f bottomY=%.3f"
                    .format(
                        Locale.US,
                        left.x, left.y, left.z,
                        right.x, right.y, right.z,
                        noseTip.x, noseTip.y, noseTip.z,
                        eyeDx, eyeDy, eyeAngle,
                        rollDy, rollDx, 0.04f,
                        noseTip.x - faceMidX, lateralSpan, 0.04f, faceMidX,
                        noseTip.y - (topY + bottomY) / 2f, verticalSpan, topY, bottomY
                    )
            )
        } else {
            log(
                "DIAGNOSE eye landmarks missing: leftEye=%s rightEye=%s"
                    .format(Locale.US, left != null, right != null)
            )
        }
    }

    private companion object {
        const val TAG = "HeadPoseAnalyzer"

        private var diagnosticPrinted = false

        fun log(message: String) {
            try {
                android.util.Log.d(TAG, message)
            } catch (t: Throwable) {
                // JVM unit tests run against the mockable android.jar whose
                // android.util.Log methods throw "Stub!" at runtime; swallow.
            }
        }
    }
}