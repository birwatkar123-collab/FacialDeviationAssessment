# Facial Deviation Assessment

**Experimental V1 Android application** that quantifies facial symmetry and smile dynamics from the front camera in real time, using MediaPipe Face Landmarker (478 landmarks) and Jetpack Compose.

> **Important:** This is an experimental V1 algorithmic prototype. It is NOT a clinically validated diagnostic tool and does not provide medical advice. Consult a healthcare professional for any medical concerns.

## Purpose

The app captures a short neutral-face sequence followed by a short smile sequence, then computes an **Overall Symmetry score (0–100)** from relative asymmetries of the mouth corners, mouth-line tilt, eyebrow heights, and eye openings, plus a separate **Smile symmetry** assessment derived from anatomical left/right mouth-corner excursion during the smile.

It is designed as a research/validation scaffold: ground-truth asymmetry can be induced (e.g., reduced one-sided smile movement) and compared against the reported metrics, which is exactly what the V1 validation runs below did.

## Architecture

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3), single-Activity, state-driven navigation (`HomeScreen`, `CameraScreen`, `ResultsScreen`)
- **State management:** MVVM — `AssessmentViewModel` (AndroidViewModel) backed by `StateFlow`; an `AssessmentState` enum drives the UI flow: `HOME → PERMISSION_REQUEST → CALIBRATING → NEUTRAL_FACE → SMILE_TEST → RESULTS`
- **Camera:** CameraX (`Preview` + `ImageAnalysis`, front camera, 640×480, RGBA_8888)
- **Landmarking:** MediaPipe Tasks Vision `FaceLandmarker` (tasks-vision:0.10.29), `RunningMode.LIVE_STREAM`, CPU delegate, honest-to-GPU model `face_landmarker.task` bundled in `app/src/main/assets`

### MediaPipe Face Landmarker

- Outputs **478 3D normalized facial landmarks** per frame.
- Coordinates are consumed directly in the **unmirrored, upright** camera frame. Orientation strategy: `CameraManager` physically rotates the sensor bitmap upright (`Matrix.postRotate`) and passes `rotationDegrees = 0` to MediaPipe so its normalized coordinates land in the upright frame (`CameraManager.kt`, `FaceLandmarkerService.kt`).
- Only the front-camera *preview* is mirrored (selfie-style); the analysis stream is not.

### Assessment flow

1. **Neutral-face collection** (~3 s, ≥ 30 valid frames) — head pose must be acceptable (yaw/pitch/roll within gate) with GOOD/FAIR tracking quality before a frame is accepted (`HeadPoseAnalyzer`, `TrackingQualityAnalyzer`).
2. **Smile collection** (~3 s, ≥ 30 frames) — landmarks are compared to the neutral mouth corners (`SmileAnalyzer`).
3. **Results** — the neutral frames are averaged, and `FacialSymmetryCalculator` emits the component metrics and weighted overall score.

### Analyses

| Module | Produces |
|---|---|
| `HeadPoseAnalyzer` | yaw / pitch / roll gating (`acceptable`), head-pose validity |
| `TrackingQualityAnalyzer` | GOOD / FAIR / POOR tracking quality |
| `LandmarkSmoother` | smoothed landmarks + temporal stability (0–1) |
| `FacialSymmetryCalculator` | mouth-corner asymmetry (%), mouth-line tilt (°), eyebrow-height asymmetry (%), eye-opening asymmetry (%), deviations, and the weighted Overall Symmetry score |
| `SmileAnalyzer` | smile symmetry (%), anatomical left/right excursion (%), excursion ratio |
| `FaceLandmarkUtils` | landmark index constants (e.g., NOSE_TIP=1, LEFT_EAR=234, LEFT/ RIGHT_MOUTH=61/291, …) and geometric helpers |

**Overall Symmetry weights:** mouth corners 0.30, mouth-line tilt 0.20, eyebrows 0.25, eyes 0.25. Each component is normalized to 0–100 and combined; the result is clamped to `[0, 100]`. **Smile symmetry is computed separately** and is not part of the overall score.

### Landmark handedness (anatomical left/right)

MediaPipe labels are anatomical: landmark **61 = anatomical left** mouth corner, **291 = anatomical right** mouth corner (the analysis stream is unmirrored, so these appear at image-right / image-left respectively). The app reports smile excursions as **anatomical left/right** accordingly.

## Current V1 status

- End-to-end flow works on device: neutral capture, gated head-pose/tracking, smile capture, and a fully populated Results screen.
- **Repeatability & controlled asymmetry validation passed:**

| Control | smileSymmetry | leftExcursion % | rightExcursion % | excursionRatio |
|---|---|---|---|---|
| A – normal smile | 82.1 | 9.06 | 10.84 | 0.91 |
| B – reduced anatomical-left smile | 12.9 | 3.28 | 8.35 | 0.56 |
| C – reduced anatomical-right smile | 0.0 | 12.22 | 3.59 | 0.45 |

  (Excursion values shown after the anatomical left/right label correction — see validation notes above.)
- Diagnostic logging: each completed assessment writes a single `RESULT_SUMMARY` line (tag `AssessmentViewModel`) with every component score, smile metrics, tracking stability, FPS, and valid-frame counts.
- **Unit tests:** 31 passing, 0 failures, 0 errors.

## Build

Requirements:

- JDK 17+ (project uses `jvmToolchain(25)`: set `JAVA_HOME` to a JDK that can provide toolchain 25, e.g. the JBR bundled with Android Studio)
- Android SDK with `compileSdk = 37`
- Gradle 9.5.0 (see `gradle/wrapper/gradle-wrapper.properties`)

If the Gradle wrapper scripts are not yet present in a fresh clone, bootstrap them once:

```bash
gradle wrapper --gradle-version 9.5.0
```

Then build the debug APK and run the unit tests:

```bash
# Windows
gradlew.bat assembleDebug
gradlew.bat testDebugUnitTest

# macOS / Linux
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Configuration summary (`app/build.gradle.kts`): `minSdk 24`, `targetSdk 37`, `compileSdk 37`, Jetpack Compose (BOM 2026.02.01), CameraX 1.6.2, MediaPipe tasks-vision 0.10.29, JUnit 4 for unit tests.

## Install the debug APK

Build output: `app/build/outputs/apk/debug/app-debug.apk`

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On first launch, grant the camera permission. Hold your face centered, keep the head near-frontal, and follow the on-screen instructions (neutral pose → smile).

## Tests

Unit tests live under `app/src/test/java/com/facialdeviation/assessment/`:

- `FaceLandmarkUtilsTest`
- `HeadPoseAnalyzerTest`
- `FacialSymmetryCalculatorTest` (includes symmetric-horizontal / small / moderate / severe mouth-line tilt cases)
- `LandmarkSmootherTest`
- `SmileAnalyzerTest`

Run with `gradlew testDebugUnitTest` — expected: **31 tests, 0 failures, 0 errors**.

## Limitations

- **Experimental V1 and NOT a clinically validated diagnostic tool.** Scores are algorithmic and may not reflect clinical assessment.
- Front camera only; portrait orientation only.
- Requires reasonably stable lighting and a centered, near-frontal face; frames outside the head-pose/tracking gates are discarded.
- The reported FPS is the instantaneous frame-to-frame rate (1000 ÷ last inter-frame gap), not an averaged throughput.
- Landmark-based geometry is sensitive to image resolution and distance from the camera.