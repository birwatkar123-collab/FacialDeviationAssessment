package com.facialdeviation.assessment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.facialdeviation.assessment.ui.screens.*
import com.facialdeviation.assessment.ui.theme.FacialDeviationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FacialDeviationTheme {
                val viewModel: AssessmentViewModel = viewModel()
                val state by viewModel.state.collectAsState()
                val result by viewModel.result.collectAsState()

                when (state) {
                    AssessmentViewModel.AssessmentState.HOME -> {
                        HomeScreen(
                            onStartAssessment = { viewModel.startAssessment() },
                            onAbout = {}
                        )
                    }
                    AssessmentViewModel.AssessmentState.PERMISSION_REQUEST,
                    AssessmentViewModel.AssessmentState.CALIBRATING,
                    AssessmentViewModel.AssessmentState.NEUTRAL_FACE,
                    AssessmentViewModel.AssessmentState.SMILE_TEST,
                    AssessmentViewModel.AssessmentState.COLLECTING_SMILE -> {
                        CameraScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.goHome() }
                        )
                    }
                    AssessmentViewModel.AssessmentState.RESULTS -> {
                        ResultsScreen(
                            result = result,
                            onRepeatAssessment = { viewModel.repeatAssessment() },
                            onNewAssessment = { viewModel.newAssessment() },
                            onBack = { viewModel.goHome() }
                        )
                    }
                    AssessmentViewModel.AssessmentState.ERROR -> {
                        HomeScreen(
                            onStartAssessment = { viewModel.startAssessment() },
                            onAbout = {}
                        )
                    }
                }
            }
        }
    }
}
