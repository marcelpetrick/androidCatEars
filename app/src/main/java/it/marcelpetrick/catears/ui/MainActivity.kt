// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import it.marcelpetrick.catears.domain.CaptureState
import it.marcelpetrick.catears.share.buildShareConfig
import it.marcelpetrick.catears.share.toChooserIntent
import it.marcelpetrick.catears.ui.theme.CatEarsTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatEarsTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val lens by viewModel.lens.collectAsStateWithLifecycle()
                val overlayPlacement by viewModel.overlayPlacement.collectAsStateWithLifecycle()
                val captureState by viewModel.captureState.collectAsStateWithLifecycle()

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    val showRationale = ActivityCompat
                        .shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                    viewModel.onPermissionResult(granted, showRationale)
                }

                // Kick off the permission check on first composition
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }

                val savedState = captureState as? CaptureState.Saved

                MainScreen(
                    uiState = uiState,
                    lens = lens,
                    overlayPlacement = overlayPlacement,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = { openAppSettings() },
                    onToggleLens = viewModel::onToggleLens,
                    onCapture = { viewModel.onCaptureRequested() },
                    onShare = savedState?.let { saved ->
                        {
                            startActivity(buildShareConfig(saved.uri).toChooserIntent())
                            viewModel.onCaptureConsumed()
                        }
                    },
                    onFaceDetected = viewModel::onFaceDetected,
                    captureRequested = captureState is CaptureState.Capturing,
                    onComposited = viewModel::onCompositedBitmap,
                )
            }
        }
    }

    private fun openAppSettings() {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", packageName, null),
        )
        startActivity(intent)
    }
}
