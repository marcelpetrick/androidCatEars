// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.marcelpetrick.catears.BuildConfig
import it.marcelpetrick.catears.camera.CameraPreview
import it.marcelpetrick.catears.camera.CameraXControllerImpl
import it.marcelpetrick.catears.domain.EarStyle
import it.marcelpetrick.catears.domain.EarTint
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.facedetect.FaceDetectorSeam
import it.marcelpetrick.catears.overlay.CatEarOverlay
import it.marcelpetrick.catears.ui.theme.CatEarsTheme

@Composable
fun MainScreen(
    uiState: MainUiState,
    lens: LensSelector,
    overlayPlacements: List<OverlayPlacement>,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleLens: () -> Unit,
    onCapture: () -> Unit,
    onShare: (() -> Unit)?,
    onFaceDetected: (List<OverlayPlacement>) -> Unit,
    earStyle: EarStyle,
    onCycleEarStyle: () -> Unit,
    earTint: EarTint,
    onCycleEarTint: () -> Unit,
    captureRequested: Boolean,
    captureEnabled: Boolean,
    onComposited: (android.graphics.Bitmap?) -> Unit,
    cameraControllerFactory: () -> CameraXControllerImpl,
    faceDetectorFactory: () -> FaceDetectorSeam,
    captureStatus: String?,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                MainUiState.Initialising -> InitialisingContent()

                MainUiState.PermissionRequired -> PermissionRequiredContent(onRequestPermission)

                MainUiState.PermissionPermanentlyDenied -> PermissionDeniedContent(onOpenSettings)

                MainUiState.Ready -> CameraContent(
                    lens = lens,
                    overlayPlacements = overlayPlacements,
                    onFaceDetected = onFaceDetected,
                    earStyle = earStyle,
                    onCycleEarStyle = onCycleEarStyle,
                    earTint = earTint,
                    onCycleEarTint = onCycleEarTint,
                    captureRequested = captureRequested,
                    captureEnabled = captureEnabled,
                    onComposited = onComposited,
                    cameraControllerFactory = cameraControllerFactory,
                    faceDetectorFactory = faceDetectorFactory,
                    onToggleLens = onToggleLens,
                    onCapture = onCapture,
                    onShare = onShare,
                )
            }
            AppTitleBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp),
            )
            if (captureStatus != null) {
                CaptureStatusBanner(
                    message = captureStatus,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun AppTitleBar(modifier: Modifier = Modifier) {
    Text(
        text = "AndroidCatEars  v${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_COMMIT})",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = TITLE_BG_ALPHA),
                shape = RoundedCornerShape(TITLE_CORNER_DP.dp),
            )
            .padding(horizontal = 14.dp, vertical = 4.dp),
    )
}

@Composable
private fun CaptureStatusBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.inverseSurface,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun InitialisingContent() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PermissionRequiredContent(onRequestPermission: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = "Camera access needed",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This app needs the camera to show the cat-ear overlay.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) { Text("Grant permission") }
        }
    }
}

@Composable
private fun PermissionDeniedContent(onOpenSettings: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = "Permission required",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Camera permission was denied permanently. Please enable it in app settings.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onOpenSettings) { Text("Open settings") }
        }
    }
}

@Composable
private fun CameraContent(
    lens: LensSelector,
    overlayPlacements: List<OverlayPlacement>,
    onFaceDetected: (List<OverlayPlacement>) -> Unit,
    earStyle: EarStyle,
    onCycleEarStyle: () -> Unit,
    earTint: EarTint,
    onCycleEarTint: () -> Unit,
    captureRequested: Boolean,
    captureEnabled: Boolean,
    onComposited: (android.graphics.Bitmap?) -> Unit,
    cameraControllerFactory: () -> CameraXControllerImpl,
    faceDetectorFactory: () -> FaceDetectorSeam,
    onToggleLens: () -> Unit,
    onCapture: () -> Unit,
    onShare: (() -> Unit)?,
) {
    val haptic = LocalHapticFeedback.current
    val onCaptureTap: () -> Unit = {
        if (captureEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onCapture()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            lens = lens,
            onFaceDetected = onFaceDetected,
            captureRequested = captureRequested,
            onComposited = onComposited,
            cameraControllerFactory = cameraControllerFactory,
            faceDetectorFactory = faceDetectorFactory,
            modifier = Modifier.fillMaxSize(),
        )
        CatEarOverlay(placements = overlayPlacements)
        CameraFabRow(
            earStyle,
            onCycleEarStyle,
            earTint,
            onCycleEarTint,
            onToggleLens,
            onCaptureTap,
            captureEnabled,
            onShare,
        )
    }
}

@Composable
private fun CameraFabRow(
    earStyle: EarStyle,
    onCycleEarStyle: () -> Unit,
    earTint: EarTint,
    onCycleEarTint: () -> Unit,
    onToggleLens: () -> Unit,
    onCapture: () -> Unit,
    captureEnabled: Boolean,
    onShare: (() -> Unit)?,
) {
    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            SmallFloatingActionButton(onClick = onCycleEarTint) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = "Cycle ear colour: ${earTint.name}",
                )
            }
            ExtendedFloatingActionButton(
                onClick = onCycleEarStyle,
                icon = { Icon(Icons.Filled.Pets, contentDescription = null) },
                text = { Text(earStyle.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) },
            )
            FloatingActionButton(onClick = onToggleLens) {
                Icon(imageVector = Icons.Filled.Cameraswitch, contentDescription = "Switch camera")
            }
        }
        FloatingActionButton(
            onClick = onCapture,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        ) {
            if (captureEnabled) {
                Icon(imageVector = Icons.Filled.Camera, contentDescription = "Take photo")
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        if (onShare != null) {
            FloatingActionButton(onClick = onShare, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = "Share photo")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenReadyPreview() {
    CatEarsTheme {
        MainScreen(
            uiState = MainUiState.Ready,
            lens = LensSelector.Front,
            overlayPlacements = emptyList(),
            onRequestPermission = {},
            onOpenSettings = {},
            onToggleLens = {},
            onCapture = {},
            onShare = {},
            onFaceDetected = { _ -> },
            earStyle = EarStyle.CLASSIC,
            onCycleEarStyle = {},
            earTint = EarTint.NATURAL,
            onCycleEarTint = {},
            captureRequested = false,
            captureEnabled = true,
            onComposited = {},
            cameraControllerFactory = { CameraXControllerImpl() },
            faceDetectorFactory = { PreviewFaceDetector },
            captureStatus = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPermissionRequiredPreview() {
    CatEarsTheme {
        MainScreen(
            uiState = MainUiState.PermissionRequired,
            lens = LensSelector.Front,
            overlayPlacements = emptyList(),
            onRequestPermission = {},
            onOpenSettings = {},
            onToggleLens = {},
            onCapture = {},
            onShare = {},
            onFaceDetected = { _ -> },
            earStyle = EarStyle.CLASSIC,
            onCycleEarStyle = {},
            earTint = EarTint.NATURAL,
            onCycleEarTint = {},
            captureRequested = false,
            captureEnabled = true,
            onComposited = {},
            cameraControllerFactory = { CameraXControllerImpl() },
            faceDetectorFactory = { PreviewFaceDetector },
            captureStatus = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPermissionDeniedPreview() {
    CatEarsTheme {
        MainScreen(
            uiState = MainUiState.PermissionPermanentlyDenied,
            lens = LensSelector.Front,
            overlayPlacements = emptyList(),
            onRequestPermission = {},
            onOpenSettings = {},
            onToggleLens = {},
            onCapture = {},
            onShare = {},
            onFaceDetected = { _ -> },
            earStyle = EarStyle.CLASSIC,
            onCycleEarStyle = {},
            earTint = EarTint.NATURAL,
            onCycleEarTint = {},
            captureRequested = false,
            captureEnabled = true,
            onComposited = {},
            cameraControllerFactory = { CameraXControllerImpl() },
            faceDetectorFactory = { PreviewFaceDetector },
            captureStatus = null,
        )
    }
}

private const val TITLE_BG_ALPHA = 0.4f
private const val TITLE_CORNER_DP = 12

private object PreviewFaceDetector : FaceDetectorSeam {
    override fun process(imageProxy: ImageProxy, onResult: (List<it.marcelpetrick.catears.domain.FaceModel>) -> Unit) {
        imageProxy.close()
        onResult(emptyList())
    }

    override fun close() = Unit
}
