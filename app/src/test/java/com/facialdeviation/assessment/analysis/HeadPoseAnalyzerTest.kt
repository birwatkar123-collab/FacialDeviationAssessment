package com.facialdeviation.assessment.analysis

import com.facialdeviation.assessment.model.FacialLandmarkFrame
import com.facialdeviation.assessment.model.FacialLandmarkPoint
import com.facialdeviation.assessment.utils.FaceLandmarkUtils
import org.junit.Assert.*
import org.junit.Test

class HeadPoseAnalyzerTest {

    private val analyzer = HeadPoseAnalyzer()

    private fun createStraightFrame(): FacialLandmarkFrame {
        val landmarks = listOf(
            FacialLandmarkPoint(0.5f, 0.1f, 0f, FaceLandmarkUtils.NOSE_TIP),
            FacialLandmarkPoint(0.5f, 0.05f, 0f, FaceLandmarkUtils.NOSE_BRIDGE),
            FacialLandmarkPoint(0.5f, 0.2f, 0f, FaceLandmarkUtils.CHIN),
            FacialLandmarkPoint(0.2f, 0.1f, 0f, FaceLandmarkUtils.LEFT_EAR),
            FacialLandmarkPoint(0.8f, 0.1f, 0f, FaceLandmarkUtils.RIGHT_EAR),
            FacialLandmarkPoint(0.5f, 0.03f, 0f, FaceLandmarkUtils.FOREHEAD_TOP)
        )
        return FacialLandmarkFrame(landmarks, 0L, 640, 480)
    }

    private fun createRotatedFrame(): FacialLandmarkFrame {
        val landmarks = listOf(
            FacialLandmarkPoint(0.6f, 0.1f, 0f, FaceLandmarkUtils.NOSE_TIP),
            FacialLandmarkPoint(0.55f, 0.05f, 0f, FaceLandmarkUtils.NOSE_BRIDGE),
            FacialLandmarkPoint(0.6f, 0.2f, 0f, FaceLandmarkUtils.CHIN),
            FacialLandmarkPoint(0.25f, 0.1f, 0f, FaceLandmarkUtils.LEFT_EAR),
            FacialLandmarkPoint(0.85f, 0.1f, 0f, FaceLandmarkUtils.RIGHT_EAR),
            FacialLandmarkPoint(0.55f, 0.03f, 0f, FaceLandmarkUtils.FOREHEAD_TOP)
        )
        return FacialLandmarkFrame(landmarks, 0L, 640, 480)
    }

    @Test
    fun straightFrameShouldBeAcceptable() {
        val frame = createStraightFrame()
        val pose = analyzer.analyze(frame)
        assertTrue("Straight frame should be acceptable", pose.isAcceptable)
    }

    @Test
    fun rotatedFrameShouldBeDetected() {
        val frame = createRotatedFrame()
        val pose = analyzer.analyze(frame)
        assertNotNull("Pose should be calculated", pose)
    }

    @Test
    fun missingLandmarksShouldBeDetected() {
        val landmarks = listOf(
            FacialLandmarkPoint(0.5f, 0.1f, 0f, FaceLandmarkUtils.NOSE_TIP)
        )
        val frame = FacialLandmarkFrame(landmarks, 0L, 640, 480)
        val pose = analyzer.analyze(frame)
        assertFalse("Missing landmarks should be not acceptable", pose.isAcceptable)
    }
}
