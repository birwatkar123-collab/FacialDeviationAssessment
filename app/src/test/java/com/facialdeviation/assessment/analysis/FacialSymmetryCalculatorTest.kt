package com.facialdeviation.assessment.analysis

import com.facialdeviation.assessment.model.FacialLandmarkFrame
import com.facialdeviation.assessment.model.FacialLandmarkPoint
import com.facialdeviation.assessment.utils.FaceLandmarkUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FacialSymmetryCalculatorTest {

    private lateinit var calculator: FacialSymmetryCalculator

    @Before
    fun setup() {
        calculator = FacialSymmetryCalculator()
    }

    private fun createSymmetricFrame(): FacialLandmarkFrame {
        val landmarks = listOf(
            FacialLandmarkPoint(0.5f, 0.1f, 0f, FaceLandmarkUtils.NOSE_TIP),
            FacialLandmarkPoint(0.5f, 0.05f, 0f, FaceLandmarkUtils.NOSE_BRIDGE),
            FacialLandmarkPoint(0.5f, 0.2f, 0f, FaceLandmarkUtils.CHIN),
            FacialLandmarkPoint(0.35f, 0.13f, 0f, FaceLandmarkUtils.LEFT_MOUTH),
            FacialLandmarkPoint(0.65f, 0.13f, 0f, FaceLandmarkUtils.RIGHT_MOUTH),
            FacialLandmarkPoint(0.5f, 0.11f, 0f, FaceLandmarkUtils.UPPER_LIP),
            FacialLandmarkPoint(0.5f, 0.15f, 0f, FaceLandmarkUtils.LOWER_LIP),
            FacialLandmarkPoint(0.35f, 0.07f, 0f, FaceLandmarkUtils.LEFT_EYE_TOP),
            FacialLandmarkPoint(0.35f, 0.09f, 0f, FaceLandmarkUtils.LEFT_EYE_BOTTOM),
            FacialLandmarkPoint(0.65f, 0.07f, 0f, FaceLandmarkUtils.RIGHT_EYE_TOP),
            FacialLandmarkPoint(0.65f, 0.09f, 0f, FaceLandmarkUtils.RIGHT_EYE_BOTTOM),
            FacialLandmarkPoint(0.35f, 0.04f, 0f, FaceLandmarkUtils.LEFT_EYEBROW),
            FacialLandmarkPoint(0.65f, 0.04f, 0f, FaceLandmarkUtils.RIGHT_EYEBROW),
            FacialLandmarkPoint(0.2f, 0.1f, 0f, FaceLandmarkUtils.LEFT_EAR),
            FacialLandmarkPoint(0.8f, 0.1f, 0f, FaceLandmarkUtils.RIGHT_EAR),
            FacialLandmarkPoint(0.5f, 0.03f, 0f, FaceLandmarkUtils.FOREHEAD_TOP)
        )
        return FacialLandmarkFrame(landmarks, 0L, 640, 480)
    }

    private fun createAsymmetricFrame(): FacialLandmarkFrame {
        val landmarks = listOf(
            FacialLandmarkPoint(0.5f, 0.1f, 0f, FaceLandmarkUtils.NOSE_TIP),
            FacialLandmarkPoint(0.5f, 0.05f, 0f, FaceLandmarkUtils.NOSE_BRIDGE),
            FacialLandmarkPoint(0.5f, 0.2f, 0f, FaceLandmarkUtils.CHIN),
            FacialLandmarkPoint(0.32f, 0.13f, 0f, FaceLandmarkUtils.LEFT_MOUTH),
            FacialLandmarkPoint(0.68f, 0.11f, 0f, FaceLandmarkUtils.RIGHT_MOUTH),
            FacialLandmarkPoint(0.5f, 0.11f, 0f, FaceLandmarkUtils.UPPER_LIP),
            FacialLandmarkPoint(0.5f, 0.15f, 0f, FaceLandmarkUtils.LOWER_LIP),
            FacialLandmarkPoint(0.35f, 0.06f, 0f, FaceLandmarkUtils.LEFT_EYE_TOP),
            FacialLandmarkPoint(0.35f, 0.09f, 0f, FaceLandmarkUtils.LEFT_EYE_BOTTOM),
            FacialLandmarkPoint(0.65f, 0.07f, 0f, FaceLandmarkUtils.RIGHT_EYE_TOP),
            FacialLandmarkPoint(0.65f, 0.09f, 0f, FaceLandmarkUtils.RIGHT_EYE_BOTTOM),
            FacialLandmarkPoint(0.35f, 0.03f, 0f, FaceLandmarkUtils.LEFT_EYEBROW),
            FacialLandmarkPoint(0.65f, 0.05f, 0f, FaceLandmarkUtils.RIGHT_EYEBROW),
            FacialLandmarkPoint(0.2f, 0.1f, 0f, FaceLandmarkUtils.LEFT_EAR),
            FacialLandmarkPoint(0.8f, 0.1f, 0f, FaceLandmarkUtils.RIGHT_EAR),
            FacialLandmarkPoint(0.5f, 0.03f, 0f, FaceLandmarkUtils.FOREHEAD_TOP)
        )
        return FacialLandmarkFrame(landmarks, 0L, 640, 480)
    }

    private fun createFrameWithMouthY(leftY: Float, rightY: Float): FacialLandmarkFrame {
        val base = createSymmetricFrame()
        val landmarks = base.landmarks.map { p ->
            when (p.index) {
                FaceLandmarkUtils.LEFT_MOUTH -> FacialLandmarkPoint(0.35f, leftY, 0f, p.index)
                FaceLandmarkUtils.RIGHT_MOUTH -> FacialLandmarkPoint(0.65f, rightY, 0f, p.index)
                else -> p
            }
        }
        return FacialLandmarkFrame(landmarks, base.timestampMs, base.imageWidth, base.imageHeight)
    }

    @Test
    fun symmetricFrameShouldHaveHighSymmetryScore() {
        val frame = createSymmetricFrame()
        val result = calculator.calculate(frame)
        assertTrue("Symmetric frame should have high score, got: ${result.overallSymmetryScore}",
            result.overallSymmetryScore > 80f)
    }

    @Test
    fun asymmetricFrameShouldHaveLowerScore() {
        val frame = createAsymmetricFrame()
        val result = calculator.calculate(frame)
        val symmetricFrame = createSymmetricFrame()
        val symmetricResult = calculator.calculate(symmetricFrame)
        assertTrue("Asymmetric should be lower than symmetric",
            result.overallSymmetryScore < symmetricResult.overallSymmetryScore)
    }

    @Test
    fun mouthCornerAsymmetryShouldBeZeroForSymmetric() {
        val frame = createSymmetricFrame()
        val result = calculator.calculate(frame)
        assertEquals("Symmetric mouth corner should be ~0", 0f, result.mouthCornerAsymmetry, 0.5f)
    }

    @Test
    fun symmetricHorizontalMouthShouldHaveZeroTilt() {
        val frame = createSymmetricFrame()
        val result = calculator.calculate(frame)
        assertEquals("Perfectly horizontal/symmetric mouth tilt should be ~0",
            0f, result.mouthAngle, 0.5f)
        assertTrue("Perfectly symmetric mouth should score near 100, got: ${result.overallSymmetryScore}",
            result.overallSymmetryScore > 98f)
    }

    @Test
    fun smallMouthTiltShouldGiveModerateScore() {
        val frame = createFrameWithMouthY(0.13f, 0.16f)
        val result = calculator.calculate(frame)
        assertEquals("Small tilt (dy=0.03, dx=0.30): atan2=5.71 deg",
            5.71f, result.mouthAngle, 1.0f)
        assertTrue("Small tilt should keep mouth score high",
            result.overallSymmetryScore > 95f)
        assertTrue("Small tilt should still lower the score below 100",
            result.overallSymmetryScore < 100f)
    }

    @Test
    fun moderateMouthTiltShouldGiveLowerScore() {
        val frame = createFrameWithMouthY(0.13f, 0.22f)
        val result = calculator.calculate(frame)
        assertEquals("Moderate tilt (dy=0.09, dx=0.30): atan2=16.70 deg",
            16.70f, result.mouthAngle, 1.0f)
        assertTrue("Moderate tilt should lower overall score below 98",
            result.overallSymmetryScore < 98f)
    }

    @Test
    fun severeMouthTiltShouldGiveNearlyVerticalScore() {
        val frame = createFrameWithMouthY(0.13f, 0.43f)
        val result = calculator.calculate(frame)
        assertEquals("Severe tilt (dy=0.30, dx=0.30): atan2=45 deg",
            45f, result.mouthAngle, 1.0f)
        assertTrue("Severe tilt should substantially lower overall score below 96",
            result.overallSymmetryScore < 96f)
    }

    @Test
    fun eyeOpeningAsymmetryShouldBeZeroForSymmetric() {
        val frame = createSymmetricFrame()
        val result = calculator.calculate(frame)
        assertEquals("Symmetric eyes should be ~0", 0f, result.eyeOpeningAsymmetry, 0.5f)
    }

    @Test
    fun eyebrowAsymmetryShouldBeZeroForSymmetric() {
        val frame = createSymmetricFrame()
        val result = calculator.calculate(frame)
        assertEquals("Symmetric eyebrows should be ~0", 0f, result.eyebrowAsymmetry, 0.5f)
    }

    @Test
    fun overallScoreShouldBeBounded() {
        val frame = createSymmetricFrame()
        val result = calculator.calculate(frame)
        assertTrue("Score should be >= 0", result.overallSymmetryScore >= 0f)
        assertTrue("Score should be <= 100", result.overallSymmetryScore <= 100f)
    }

    @Test
    fun mouthCornerDeviationShouldBeNormalized() {
        val frame = createSymmetricFrame()
        val result = calculator.calculate(frame)
        assertTrue("Deviation should be >= 0", result.mouthCornerDeviation >= 0f)
    }
}
