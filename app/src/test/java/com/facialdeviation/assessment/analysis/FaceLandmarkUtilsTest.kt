package com.facialdeviation.assessment.analysis

import com.facialdeviation.assessment.model.FacialLandmarkFrame
import com.facialdeviation.assessment.model.FacialLandmarkPoint
import com.facialdeviation.assessment.utils.FaceLandmarkUtils
import org.junit.Assert.*
import org.junit.Test

class FaceLandmarkUtilsTest {

    @Test
    fun distanceShouldCalculateCorrectly() {
        val a = FacialLandmarkPoint(0f, 0f, 0f, 0)
        val b = FacialLandmarkPoint(3f, 4f, 0f, 1)
        assertEquals(5f, FaceLandmarkUtils.distance(a, b), 0.001f)
    }

    @Test
    fun distance2DShouldIgnoreZ() {
        val a = FacialLandmarkPoint(0f, 0f, 100f, 0)
        val b = FacialLandmarkPoint(3f, 4f, 200f, 1)
        assertEquals(5f, FaceLandmarkUtils.distance2D(a, b), 0.001f)
    }

    @Test
    fun midpointShouldBeCenter() {
        val a = FacialLandmarkPoint(0f, 0f, 0f, 0)
        val b = FacialLandmarkPoint(10f, 10f, 10f, 1)
        val mid = FaceLandmarkUtils.midpoint(a, b)
        assertEquals(5f, mid.x, 0.001f)
        assertEquals(5f, mid.y, 0.001f)
        assertEquals(5f, mid.z, 0.001f)
    }

    @Test
    fun angleOfStraightLineShouldBe180() {
        val a = FacialLandmarkPoint(0f, 0f, 0f, 0)
        val b = FacialLandmarkPoint(5f, 0f, 0f, 1)
        val c = FacialLandmarkPoint(10f, 0f, 0f, 2)
        assertEquals(180f, FaceLandmarkUtils.angle(a, b, c), 0.5f)
    }

    @Test
    fun angleOfRightAngleShouldBe90() {
        val a = FacialLandmarkPoint(0f, 0f, 0f, 0)
        val b = FacialLandmarkPoint(0f, 5f, 0f, 1)
        val c = FacialLandmarkPoint(5f, 5f, 0f, 2)
        assertEquals(90f, FaceLandmarkUtils.angle(a, b, c), 0.5f)
    }

    @Test
    fun eyeOpeningShouldReturnPositiveDistance() {
        val frame = FacialLandmarkFrame(
            listOf(
                FacialLandmarkPoint(0.35f, 0.07f, 0f, FaceLandmarkUtils.LEFT_EYE_TOP),
                FacialLandmarkPoint(0.35f, 0.09f, 0f, FaceLandmarkUtils.LEFT_EYE_BOTTOM)
            ),
            0L, 640, 480
        )
        val opening = FaceLandmarkUtils.eyeOpening(
            frame, FaceLandmarkUtils.LEFT_EYE_TOP, FaceLandmarkUtils.LEFT_EYE_BOTTOM
        )
        assertTrue("Eye opening should be positive", opening > 0f)
    }

    @Test
    fun faceWidthShouldBePositive() {
        val frame = FacialLandmarkFrame(
            listOf(
                FacialLandmarkPoint(0.2f, 0.1f, 0f, FaceLandmarkUtils.LEFT_EAR),
                FacialLandmarkPoint(0.8f, 0.1f, 0f, FaceLandmarkUtils.RIGHT_EAR)
            ),
            0L, 640, 480
        )
        assertTrue("Face width should be positive", FaceLandmarkUtils.faceWidth(frame) > 0f)
    }

    @Test
    fun normalizeDistanceShouldReturnPositive() {
        val frame = FacialLandmarkFrame(
            listOf(
                FacialLandmarkPoint(0.2f, 0.1f, 0f, FaceLandmarkUtils.LEFT_EAR),
                FacialLandmarkPoint(0.8f, 0.1f, 0f, FaceLandmarkUtils.RIGHT_EAR)
            ),
            0L, 640, 480
        )
        val normalized = FaceLandmarkUtils.normalizeDistance(frame, 0.1f)
        assertTrue("Normalized distance should be positive", normalized > 0f)
    }
}
