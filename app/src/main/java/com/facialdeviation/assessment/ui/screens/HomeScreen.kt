package com.facialdeviation.assessment.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facialdeviation.assessment.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartAssessment: () -> Unit,
    onAbout: () -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Facial Assessment") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Primary
            )

            Text(
                text = "Facial Deviation\nAssessment",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = OnBackground
            )

            Text(
                text = "Real-time facial symmetry measurement\nusing AI-powered face landmark detection",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = OnBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStartAssessment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Assessment", fontSize = 16.sp)
            }

            OutlinedButton(
                onClick = { showAboutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("About", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WarningColor.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = "Research/assessment prototype. Results are not a medical diagnosis.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = WarningColor,
                    lineHeight = 18.sp
                )
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text("About", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Facial Deviation Assessment V1",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "This application uses MediaPipe Face Landmarker to detect 478 facial landmarks " +
                        "in real-time and quantify basic left/right facial asymmetry.",
                        fontSize = 13.sp
                    )
                    Text(
                        "This is a technical prototype / research tool, not a diagnostic medical device. " +
                        "It does not diagnose stroke, Bell's palsy, or any medical condition.",
                        fontSize = 13.sp,
                        color = ErrorColor
                    )
                    Text(
                        "Features:\n" +
                        "• Mouth corner deviation\n" +
                        "• Eyebrow height asymmetry\n" +
                        "• Eye opening asymmetry\n" +
                        "• Dynamic smile test\n" +
                        "• Overall symmetry score",
                        fontSize = 13.sp
                    )
                    Text(
                        "Technology: Android, Kotlin, Jetpack Compose, CameraX, MediaPipe",
                        fontSize = 12.sp,
                        color = OnBackground.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
