// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import it.marcelpetrick.catears.BuildConfig
import it.marcelpetrick.catears.camera.CameraXControllerImpl
import it.marcelpetrick.catears.domain.CaptureState
import it.marcelpetrick.catears.domain.RecordingState
import it.marcelpetrick.catears.facedetect.FaceDetectorSeam
import it.marcelpetrick.catears.share.buildShareConfig
import it.marcelpetrick.catears.share.toChooserIntent
import it.marcelpetrick.catears.ui.theme.CatEarsTheme
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
@androidx.annotation.OptIn(ExperimentalGetImage::class)
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var cameraControllerProvider: Provider<CameraXControllerImpl>

    @Inject
    lateinit var faceDetectorProvider: Provider<FaceDetectorSeam>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        enableEdgeToEdge()
        setContent { CatEarsTheme { AppContent() } }
    }

    @Composable
    private fun AppContent() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val lens by viewModel.lens.collectAsStateWithLifecycle()
        val overlayPlacements by viewModel.overlayPlacements.collectAsStateWithLifecycle()
        val captureState by viewModel.captureState.collectAsStateWithLifecycle()
        val earStyle by viewModel.earStyle.collectAsStateWithLifecycle()
        val earTint by viewModel.earTint.collectAsStateWithLifecycle()
        val partyModeEnabled by viewModel.partyModeEnabled.collectAsStateWithLifecycle()
        val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            val showRationale = ActivityCompat
                .shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
            viewModel.onPermissionResult(granted, showRationale)
        }
        LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

        val savedState = captureState as? CaptureState.Saved
        val savedRecording = recordingState as? RecordingState.Saved

        MainScreen(
            uiState = uiState,
            lens = lens,
            overlayPlacements = overlayPlacements,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = { openAppSettings() },
            onCameraError = viewModel::onCameraBindFailed,
            onRetryCamera = viewModel::onRetryCamera,
            onToggleLens = viewModel::onToggleLens,
            onCapture = { viewModel.onCaptureRequested() },
            onShare = savedState?.let { saved ->
                {
                    startActivity(buildShareConfig(saved.uriString.toUri()).toChooserIntent())
                    viewModel.onCaptureConsumed()
                }
            },
            onFaceDetected = viewModel::onFaceDetected,
            earStyle = earStyle,
            onCycleEarStyle = viewModel::onCycleEarStyle,
            earTint = earTint,
            onCycleEarTint = viewModel::onCycleEarTint,
            partyModeEnabled = partyModeEnabled,
            onTogglePartyMode = viewModel::onTogglePartyMode,
            onRerollPartyAssignments = viewModel::onRerollPartyAssignments,
            captureRequested = captureState is CaptureState.Capturing,
            captureEnabled = captureState !is CaptureState.Capturing,
            onComposited = viewModel::onCompositedBitmap,
            cameraControllerFactory = { cameraControllerProvider.get() },
            faceDetectorFactory = { faceDetectorProvider.get() },
            captureStatus = when (captureState) {
                CaptureState.Capturing -> "Saving photo..."
                is CaptureState.Saved -> "Photo saved to gallery · tap share"
                CaptureState.Failed -> "Capture failed — please try again"
                else -> null
            },
            recordingState = recordingState,
            onRecordTap = viewModel::onRecordTap,
            onRecordingSaved = viewModel::onRecordingSaved,
            onShareVideo = savedRecording?.let { saved ->
                {
                    startActivity(buildShareConfig(saved.uriString.toUri(), "video/mp4").toChooserIntent())
                    viewModel.onRecordingConsumed()
                }
            },
        )
    }

    private fun openAppSettings() {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", packageName, null),
        )
        startActivity(intent)
    }
}
