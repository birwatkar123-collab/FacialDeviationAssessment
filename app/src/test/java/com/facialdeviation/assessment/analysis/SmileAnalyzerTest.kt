package com.facialdeviation.assessment.analysis

import com.facialdeviation.assessment.model.FacialLandmarkFrame
import com.facialdeviation.assessment.model.FacialLandmarkPoint
import com.facialdeviation.assessment.utils.FaceLandmarkUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SmileAnalyzerTest {

    private lateinit var analyzer: SmileAnalyzer

    @Before
    fun setup() {
        analyzer = SmileAnalyzer()
    }

    private fun createNeutralFrame(): FacialLandmarkFrame {
        val landmarks = listOf(
            FacialLandmarkPoint(0.5f, 0.1f, 0f, FaceLandmarkUtils.NOSE_TIP),
            FacialLandmarkPoint(0.42f, 0.13f, 0f, FaceLandmarkUtils.LEFT_MOUTH),
            FacialLandmarkPoint(0.58f, 0.13f, 0f, FaceLandmarkUtils.RIGHT_MOUTH),
            FacialLandmarkPoint(0.2f, 0.1f, 0f, FaceLandmarkUtils.LEFT_EAR),
            FacialLandmarkPoint(0.8f, 0.1f, 0f, FaceLandmarkUtils.RIGHT_EAR),
            FacialLandmarkPoint(0.5f, 0.2f, 0f, FaceLandmarkUtils.CHIN),
            FacialLandmarkPoint(0.5f, 0.05f, 0f, FaceLandmarkUtils.NOSE_BRIDGE)
        )
        return FacialLandmarkFrame(landmarks, 0L, 640, 480)
    }

    private fun createSmileFrame(): FacialLandmarkFrame {
        val landmarks = listOf(
            FacialLandmarkPoint(0.5f, 0.1f, 0f, FaceLandmarkUtils.NOSE_TIP),
            FacialLandmarkPoint(0.38f, 0.11f, 0f, FaceLandmarkUtils.LEFT_MOUTH),
            FacialLandmarkPoint(0.62f, 0.11f, 0f, FaceLandmarkUtils.RIGHT_MOUTH),
            FacialLandmarkPoint(0.2f, 0.1f, 0f, FaceLandmarkUtils.LEFT_EAR),
            FacialLandmarkPoint(0.8f, 0.1f, 0f, FaceLandmarkUtils.RIGHT_EAR),
            FacialLandmarkPoint(0.5f, 0.2f, 0f, FaceLandmarkUtils.CHIN),
            FacialLandmarkPoint(0.5f, 0.05f, 0f, FaceLandmarkUtils.NOSE_BRIDGE)
        )
        return FacialLandmarkFrame(landmarks, 1000L, 640, 480)
    }

    @Test
    fun defaultShouldReturnZeros() {
        val result = analyzer.calculate()
        assertEquals(0f, result.smileSymmetry, 0.01f)
        assertEquals(0f, result.leftExcursion, 0.01f)
    }

    @Test
    fun symmetricSmileShouldHaveHighSymmetry() {
        analyzer.setNeutralFrame(createNeutralFrame())
        analyzer.startSmileCapture()
        analyzer.addSmileFrame(createSmileFrame())

        val result = analyzer.calculate()
        assertTrue("Symmetric smile should have high symmetry, got: ${result.smileSymmetry}",
            result.smileSymmetry > 80f)
    }

    @Test
    fun smileShouldHavePositiveExcursion() {
        analyzer.setNeutralFrame(createNeutralFrame())
        analyzer.startSmileCapture()
        analyzer.addSmileFrame(createSmileFrame())

        val result = analyzer.calculate()
        assertTrue("Left excursion should be > 0", result.leftExcursion > 0f)
        assertTrue("Right excursion should be > 0", result.rightExcursion > 0f)
    }

    @Test
    fun resetShouldClearState() {
        analyzer.setNeutralFrame(createNeutralFrame())
        analyzer.startSmileCapture()
        analyzer.addSmileFrame(createSmileFrame())
        analyzer.reset()

        val result = analyzer.calculate()
        assertEquals(0f, result.smileSymmetry, 0.01f)
    }
}
