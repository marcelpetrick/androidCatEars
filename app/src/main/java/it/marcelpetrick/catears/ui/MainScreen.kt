// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.marcelpetrick.catears.camera.CameraPreview
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.overlay.CatEarOverlay
import it.marcelpetrick.catears.ui.theme.CatEarsTheme

@Composable
fun MainScreen(
    uiState: MainUiState,
    lens: LensSelector,
    overlayPlacement: OverlayPlacement?,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleLens: () -> Unit,
    onCapture: () -> Unit,
    onShare: (() -> Unit)?,
    onFaceDetected: (OverlayPlacement?) -> Unit,
    captureRequested: Boolean,
    onComposited: (android.graphics.Bitmap?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            MainUiState.Initialising -> InitialisingContent()

            MainUiState.PermissionRequired -> PermissionRequiredContent(onRequestPermission)

            MainUiState.PermissionPermanentlyDenied -> PermissionDeniedContent(onOpenSettings)

            MainUiState.Ready -> CameraContent(
                lens = lens,
                overlayPlacement = overlayPlacement,
                onFaceDetected = onFaceDetected,
                captureRequested = captureRequested,
                onComposited = onComposited,
                onToggleLens = onToggleLens,
                onCapture = onCapture,
                onShare = onShare,
            )
        }
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
    overlayPlacement: OverlayPlacement?,
    onFaceDetected: (OverlayPlacement?) -> Unit,
    captureRequested: Boolean,
    onComposited: (android.graphics.Bitmap?) -> Unit,
    onToggleLens: () -> Unit,
    onCapture: () -> Unit,
    onShare: (() -> Unit)?,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            lens = lens,
            onFaceDetected = onFaceDetected,
            captureRequested = captureRequested,
            onComposited = onComposited,
            modifier = Modifier.fillMaxSize(),
        )
        CatEarOverlay(placement = overlayPlacement)
        FloatingActionButton(
            onClick = onToggleLens,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(imageVector = Icons.Filled.Cameraswitch, contentDescription = "Switch camera")
        }
        FloatingActionButton(
            onClick = onCapture,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        ) {
            Icon(imageVector = Icons.Filled.Camera, contentDescription = "Take photo")
        }
        if (onShare != null) {
            FloatingActionButton(
                onClick = onShare,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
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
            overlayPlacement = null,
            onRequestPermission = {},
            onOpenSettings = {},
            onToggleLens = {},
            onCapture = {},
            onShare = {},
            onFaceDetected = {},
            captureRequested = false,
            onComposited = {},
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
            overlayPlacement = null,
            onRequestPermission = {},
            onOpenSettings = {},
            onToggleLens = {},
            onCapture = {},
            onShare = {},
            onFaceDetected = {},
            captureRequested = false,
            onComposited = {},
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
            overlayPlacement = null,
            onRequestPermission = {},
            onOpenSettings = {},
            onToggleLens = {},
            onCapture = {},
            onShare = {},
            onFaceDetected = {},
            captureRequested = false,
            onComposited = {},
        )
    }
}
