// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import it.marcelpetrick.catears.domain.LensSelector
import java.util.concurrent.Executors

/**
 * Full-screen CameraX preview composable.
 *
 * Binds the camera on first composition (or when [lens] changes) and
 * unbinds cleanly on disposal.
 */
@Composable
fun CameraPreview(lens: LensSelector, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    val controller = remember { CameraXControllerImpl() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            controller.previewView = previewView
            controller.lifecycleOwner = lifecycleOwner

            val providerFuture: ListenableFuture<ProcessCameraProvider> =
                ProcessCameraProvider.getInstance(ctx)

            providerFuture.addListener(
                {
                    controller.setCameraProvider(providerFuture.get())
                    controller.bindPreview(lens)
                },
                executor,
            )
            previewView
        },
        update = { _ ->
            controller.bindPreview(lens)
        },
        modifier = modifier,
    )

    DisposableEffect(Unit) {
        onDispose {
            controller.unbind()
            executor.shutdown()
        }
    }
}
