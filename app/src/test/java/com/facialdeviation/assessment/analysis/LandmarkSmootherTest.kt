package com.facialdeviation.assessment.analysis

import com.facialdeviation.assessment.model.FacialLandmarkFrame
import com.facialdeviation.assessment.model.FacialLandmarkPoint
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LandmarkSmootherTest {

    private lateinit var smoother: LandmarkSmoother

    @Before
    fun setup() {
        smoother = LandmarkSmoother(alpha = 0.5f)
    }

    private fun createFrame(x: Float): FacialLandmarkFrame {
        val landmarks = listOf(
            FacialLandmarkPoint(x, 0.5f, 0f, 0),
            FacialLandmarkPoint(x + 0.1f, 0.5f, 0f, 1)
        )
        return FacialLandmarkFrame(landmarks, 0L, 640, 480)
    }

    @Test
    fun firstFrameShouldReturnUnchanged() {
        val frame = createFrame(0.5f)
        val smoothed = smoother.smooth(frame)
        assertEquals(0.5f, smoothed.landmarks[0].x, 0.001f)
    }

    @Test
    fun secondFrameShouldBeSmoothed() {
        smoother.smooth(createFrame(0.5f))
        val smoothed = smoother.smooth(createFrame(0.6f))
        assertTrue("Smoothed should be between 0.5 and 0.6",
            smoothed.landmarks[0].x > 0.5f && smoothed.landmarks[0].x < 0.6f)
    }

    @Test
    fun stabilityShouldStartHigh() {
        smoother.smooth(createFrame(0.5f))
        val stability = smoother.stability()
        assertEquals(1f, stability, 0.01f)
    }

    @Test
    fun stabilityShouldDecreaseWithNoisyFrames() {
        smoother.smooth(createFrame(0.5f))
        smoother.smooth(createFrame(0.6f))
        smoother.smooth(createFrame(0.4f))
        smoother.smooth(createFrame(0.7f))
        smoother.smooth(createFrame(0.3f))
        val stability = smoother.stability()
        assertTrue("Stability should decrease with noise", stability < 0.9f)
    }

    @Test
    fun resetShouldClearState() {
        smoother.smooth(createFrame(0.5f))
        smoother.smooth(createFrame(0.6f))
        smoother.reset()
        val stability = smoother.stability()
        assertEquals(1f, stability, 0.01f)
    }
}
