// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import androidx.camera.core.ExperimentalGetImage
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
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.domain.PlacementSmoother
import it.marcelpetrick.catears.domain.TransformContext
import it.marcelpetrick.catears.domain.computeOverlayPlacement
import it.marcelpetrick.catears.domain.imageToViewBoundingBox
import it.marcelpetrick.catears.facedetect.MlKitFaceDetectorImpl
import java.util.concurrent.Executors

/**
 * Full-screen CameraX preview composable with live face-tracked overlay placement.
 *
 * Binds the camera on first composition (or when [lens] changes), runs ML Kit face
 * detection on the analysis stream, transforms each detected face into view space,
 * smooths the result, and reports an [OverlayPlacement] (or null) via [onFaceDetected].
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(lens: LensSelector, onFaceDetected: (OverlayPlacement?) -> Unit, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember { MlKitFaceDetectorImpl() }
    val smoother = remember { PlacementSmoother() }
    val controller = remember { CameraXControllerImpl() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            controller.previewView = previewView
            controller.lifecycleOwner = lifecycleOwner
            controller.faceDetector = detector
            controller.onFaceResult = { face, imageWidth, imageHeight ->
                val placement = face?.let {
                    val ctxTransform = TransformContext(
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        viewWidth = previewView.width.coerceAtLeast(1),
                        viewHeight = previewView.height.coerceAtLeast(1),
                        isFrontCamera = lens == LensSelector.Front,
                    )
                    val viewBox = imageToViewBoundingBox(it.boundingBox, ctxTransform)
                    smoother.smooth(computeOverlayPlacement(viewBox, it.headEulerAngleZ))
                }
                if (placement == null) smoother.reset()
                onFaceDetected(placement)
            }

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
            detector.close()
            executor.shutdown()
        }
    }
}
