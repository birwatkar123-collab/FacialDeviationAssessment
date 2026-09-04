package com.facialdeviation.assessment.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facialdeviation.assessment.AssessmentViewModel
import com.facialdeviation.assessment.model.FacialAssessmentResult
import com.facialdeviation.assessment.model.TrackingQuality
import com.facialdeviation.assessment.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    result: FacialAssessmentResult?,
    onRepeatAssessment: () -> Unit,
    onNewAssessment: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assessment Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary,
                    navigationIconContentColor = OnPrimary
                )
            )
        }
    ) { padding ->
        if (result == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "FACIAL ASSESSMENT",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                OverallSymmetryCard(score = result.measurements.overallSymmetryScore)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Mouth", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultRow("Corner asymmetry", "${String.format("%.1f", result.measurements.mouthCornerAsymmetry)}%")
                        ResultRow("Angle", "${String.format("%.1f", result.measurements.mouthAngle)}°")
                        ResultRow("Deviation from midline", "${String.format("%.1f", result.measurements.mouthCornerDeviation)}%")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Eyes", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultRow("Opening asymmetry", "${String.format("%.1f", result.measurements.eyeOpeningAsymmetry)}%")
                        ResultRow("Deviation", "${String.format("%.1f", result.measurements.eyeOpeningDeviation)}%")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Eyebrows", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultRow("Height asymmetry", "${String.format("%.1f", result.measurements.eyebrowAsymmetry)}%")
                        ResultRow("Deviation", "${String.format("%.1f", result.measurements.eyebrowDeviation)}%")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Smile", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultRow("Symmetry", "${String.format("%.1f", result.smileAssessment.smileSymmetry)}%")
                        ResultRow("Left excursion", "${String.format("%.2f", result.smileAssessment.leftExcursion)}%")
                        ResultRow("Right excursion", "${String.format("%.2f", result.smileAssessment.rightExcursion)}%")
                        ResultRow("Ratio", "${String.format("%.2f", result.smileAssessment.excursionRatio)}")
                        ResultRow("Movement duration", "${result.smileAssessment.movementDurationMs}ms")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tracking", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        val qualityColor = when (result.trackingQuality) {
                            TrackingQuality.GOOD -> GoodQuality
                            TrackingQuality.FAIR -> FairQuality
                            TrackingQuality.POOR -> PoorQuality
                        }
                        ResultRow("Quality", result.trackingQuality.name, qualityColor)
                        ResultRow("Stability", "${String.format("%.1f", result.landmarkStability * 100)}%")
                        ResultRow("FPS", "${String.format("%.1f", result.fps)}")
                    }
                }

                FaceDiagram(result = result)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = WarningColor.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "This is an experimental algorithmic score for facial symmetry measurement. " +
                               "It is NOT a clinical diagnosis. Consult a healthcare professional for medical concerns.",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 11.sp,
                        color = WarningColor,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRepeatAssessment,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Repeat")
                    }
                    Button(
                        onClick = onNewAssessment,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("New Assessment")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun OverallSymmetryCard(score: Float) {
    val scoreColor = when {
        score >= 80 -> GoodQuality
        score >= 60 -> FairQuality
        else -> PoorQuality
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = scoreColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Overall Symmetry",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = OnBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${String.format("%.0f", score)} / 100",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = scoreColor,
                trackColor = scoreColor.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun ResultRow(
    label: String,
    value: String,
    valueColor: Color = OnBackground
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = OnBackground.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
fun FaceDiagram(result: FacialAssessmentResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Face Symmetry Diagram",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val centerX = size.width / 2f
                val faceWidth = size.width * 0.6f
                val faceHeight = size.height * 0.8f
                val faceTop = (size.height - faceHeight) / 2f

                drawOval(
                    color = Primary.copy(alpha = 0.1f),
                    topLeft = Offset(centerX - faceWidth / 2, faceTop),
                    size = Size(faceWidth, faceHeight)
                )

                drawOval(
                    color = Primary,
                    topLeft = Offset(centerX - faceWidth / 2, faceTop),
                    size = Size(faceWidth, faceHeight),
                    style = Stroke(width = 2f)
                )

                drawLine(
                    color = Color.Red.copy(alpha = 0.5f),
                    start = Offset(centerX, faceTop),
                    end = Offset(centerX, faceTop + faceHeight),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                )

                val mouthY = faceTop + faceHeight * 0.65f
                val mouthWidth = faceWidth * 0.3f
                val asymmetry = result.measurements.mouthCornerAsymmetry / 100f * 10f

                drawCircle(
                    color = LandmarkColor,
                    radius = 4f,
                    center = Offset(centerX - mouthWidth / 2 - asymmetry, mouthY)
                )
                drawCircle(
                    color = LandmarkColor,
                    radius = 4f,
                    center = Offset(centerX + mouthWidth / 2 + asymmetry, mouthY)
                )
                drawLine(
                    color = LandmarkColor,
                    start = Offset(centerX - mouthWidth / 2 - asymmetry, mouthY),
                    end = Offset(centerX + mouthWidth / 2 + asymmetry, mouthY),
                    strokeWidth = 2f
                )

                val leftEyeY = faceTop + faceHeight * 0.38f
                val rightEyeY = faceTop + faceHeight * 0.38f
                val eyeWidth = faceWidth * 0.15f

                drawOval(
                    color = LandmarkColor,
                    topLeft = Offset(centerX - faceWidth * 0.22f - eyeWidth / 2, leftEyeY - eyeWidth / 4),
                    size = Size(eyeWidth, eyeWidth / 2),
                    style = Stroke(width = 2f)
                )
                drawOval(
                    color = LandmarkColor,
                    topLeft = Offset(centerX + faceWidth * 0.22f - eyeWidth / 2, rightEyeY - eyeWidth / 4),
                    size = Size(eyeWidth, eyeWidth / 2),
                    style = Stroke(width = 2f)
                )

                val eyebrowY = faceTop + faceHeight * 0.28f
                drawLine(
                    color = LandmarkColor,
                    start = Offset(centerX - faceWidth * 0.3f, eyebrowY),
                    end = Offset(centerX - faceWidth * 0.08f, eyebrowY - 5f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = LandmarkColor,
                    start = Offset(centerX + faceWidth * 0.08f, eyebrowY - 5f),
                    end = Offset(centerX + faceWidth * 0.3f, eyebrowY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
