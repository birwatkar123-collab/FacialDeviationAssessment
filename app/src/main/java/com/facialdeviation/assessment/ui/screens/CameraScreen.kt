package com.facialdeviation.assessment.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.facialdeviation.assessment.AssessmentViewModel
import com.facialdeviation.assessment.camera.CameraManager
import com.facialdeviation.assessment.model.FacialLandmarkFrame
import com.facialdeviation.assessment.model.FacialLandmarkPoint
import com.facialdeviation.assessment.model.TrackingQuality
import com.facialdeviation.assessment.ui.theme.*
import com.facialdeviation.assessment.utils.FaceLandmarkUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: AssessmentViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.state.collectAsState()
    val instruction by viewModel.instruction.collectAsState()
    val faceDetected by viewModel.faceDetected.collectAsState()
    val faceCount by viewModel.faceCount.collectAsState()
    val headPoseValid by viewModel.headPoseValid.collectAsState()
    val yaw by viewModel.yaw.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val roll by viewModel.roll.collectAsState()
    val landmarkCount by viewModel.landmarkCount.collectAsState()
    val trackingQuality by viewModel.trackingQuality.collectAsState()
    val currentLandmarks by viewModel.currentLandmarks.collectAsState()
    val imageDimensions by viewModel.imageDimensions.collectAsState()
    val fps by viewModel.fps.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.onCameraPermissionGranted()
        } else {
            viewModel.onCameraPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            viewModel.onCameraPermissionGranted()
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraManager = remember {
        CameraManager(context, lifecycleOwner, previewView)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.initializeMediaPipe()
            cameraManager.startCamera()
            cameraManager.onFrameAnalyzed = { bitmap, timestampMs, rotationDegrees ->
                viewModel.faceLandmarkerService.detectAsync(bitmap, timestampMs, rotationDegrees)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.onFrameAnalyzed = null
            cameraManager.stopCamera()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            CameraOverlay(
                landmarks = currentLandmarks,
                imageWidth = imageDimensions.first,
                imageHeight = imageDimensions.second,
                faceDetected = faceDetected,
                faceCount = faceCount
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            cameraManager.stopCamera()
                            viewModel.goHome()
                            onBack()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    DebugPosePanel(
                        faceDetected = faceDetected,
                        landmarkCount = landmarkCount,
                        yaw = yaw,
                        pitch = pitch,
                        roll = roll,
                        headPoseValid = headPoseValid,
                        trackingQuality = trackingQuality,
                        fps = fps
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(
                            text = instruction,
                            modifier = Modifier.padding(16.dp),
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!faceDetected) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = WarningColor.copy(alpha = 0.9f)
                            )
                        ) {
                            Text(
                                text = "Position your face inside the guide.",
                                modifier = Modifier.padding(8.dp),
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (faceCount > 1) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = ErrorColor.copy(alpha = 0.9f)
                            )
                        ) {
                            Text(
                                text = "Please ensure only one face is visible.",
                                modifier = Modifier.padding(8.dp),
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (!headPoseValid) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = WarningColor.copy(alpha = 0.9f)
                            )
                        ) {
                            Text(
                                text = "Please face the camera directly.",
                                modifier = Modifier.padding(8.dp),
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera permission is required for face assessment.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = OnBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onBack() }) {
                    Text("Go Back")
                }
            }
        }
    }

    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CameraOverlay(
    landmarks: List<FacialLandmarkPoint>,
    imageWidth: Int,
    imageHeight: Int,
    faceDetected: Boolean,
    faceCount: Int
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val guideSize = canvasWidth * 0.7f
        val guideLeft = (canvasWidth - guideSize) / 2f
        val guideTop = (canvasHeight - guideSize) / 2f

        drawOval(
            color = GuideColor,
            topLeft = Offset(guideLeft, guideTop),
            size = Size(guideSize, guideSize),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        if (faceDetected && faceCount == 1 && landmarks.isNotEmpty()) {
            drawOval(
                color = SuccessColor.copy(alpha = 0.3f),
                topLeft = Offset(guideLeft, guideTop),
                size = Size(guideSize, guideSize),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )

            for (landmark in landmarks) {
                val x = (1f - landmark.x) * canvasWidth
                val y = landmark.y * canvasHeight

                drawCircle(
                    color = LandmarkColor,
                    radius = 2f,
                    center = Offset(x, y)
                )
            }

            val keyLandmarks = listOf(
                FaceLandmarkUtils.LEFT_MOUTH,
                FaceLandmarkUtils.RIGHT_MOUTH,
                FaceLandmarkUtils.UPPER_LIP,
                FaceLandmarkUtils.LOWER_LIP,
                FaceLandmarkUtils.LEFT_EYE_TOP,
                FaceLandmarkUtils.LEFT_EYE_BOTTOM,
                FaceLandmarkUtils.RIGHT_EYE_TOP,
                FaceLandmarkUtils.RIGHT_EYE_BOTTOM,
                FaceLandmarkUtils.LEFT_EYEBROW,
                FaceLandmarkUtils.RIGHT_EYEBROW,
                FaceLandmarkUtils.NOSE_TIP,
                FaceLandmarkUtils.NOSE_BRIDGE
            )

            for (idx in keyLandmarks) {
                val lp = landmarks.find { it.index == idx }
                if (lp != null) {
                    val x = (1f - lp.x) * canvasWidth
                    val y = lp.y * canvasHeight
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = LandmarkColor,
                        radius = 3f,
                        center = Offset(x, y)
                    )
                }
            }

            val midline = FaceLandmarkUtils.facialMidline(
                FacialLandmarkFrame(
                    landmarks = landmarks,
                    timestampMs = 0,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight
                )
            )
            if (midline != null) {
                val top = midline.first
                val bottom = midline.second
                drawLine(
                    color = Color.Red.copy(alpha = 0.6f),
                    start = Offset((1f - top.x) * canvasWidth, top.y * canvasHeight),
                    end = Offset((1f - bottom.x) * canvasWidth, bottom.y * canvasHeight),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                )
            }
        }
    }
}

@Composable
private fun DebugPosePanel(
    faceDetected: Boolean,
    landmarkCount: Int,
    yaw: Float,
    pitch: Float,
    roll: Float,
    headPoseValid: Boolean,
    trackingQuality: TrackingQuality,
    fps: Float
) {
    val poseLabel = if (headPoseValid) "GOOD" else "ADJUST"
    val poseColor = if (headPoseValid) GoodQuality else WarningColor
    val qualityColor = when (trackingQuality) {
        TrackingQuality.GOOD -> GoodQuality
        TrackingQuality.FAIR -> FairQuality
        TrackingQuality.POOR -> PoorQuality
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "FACE: ${if (faceDetected) "DETECTED" else "NONE"}",
            color = if (faceDetected) GoodQuality else PoorQuality,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "LANDMARKS: $landmarkCount",
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "YAW: ${String.format("%.0f", yaw)}°  PITCH: ${String.format("%.0f", pitch)}°",
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "ROLL: ${String.format("%.0f", roll)}°  FPS: ${String.format("%.1f", fps)}",
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "POSE: $poseLabel  TRACK: $trackingQuality",
            color = if (headPoseValid) poseColor else qualityColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
