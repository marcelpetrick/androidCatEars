// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import android.graphics.Bitmap
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import it.marcelpetrick.catears.R
import it.marcelpetrick.catears.capture.OverlayCompositor
import it.marcelpetrick.catears.capture.decodeDrawableToBitmap
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.domain.PlacementSmoother
import it.marcelpetrick.catears.domain.TransformContext
import it.marcelpetrick.catears.domain.computeOverlayPlacement
import it.marcelpetrick.catears.domain.imageToViewBoundingBox
import it.marcelpetrick.catears.facedetect.MlKitFaceDetectorImpl
import java.util.concurrent.atomic.AtomicReference

private const val EAR_ASSET_WIDTH = 200
private const val EAR_ASSET_HEIGHT = 100

/**
 * Full-screen CameraX preview composable with live face-tracked overlay placement and capture.
 *
 * - Runs ML Kit face detection on the analysis stream, transforms each face to view space,
 *   smooths it, and reports an [OverlayPlacement] (or null) via [onFaceDetected].
 * - When [captureRequested] flips true, grabs the current preview frame (view-space, WYSIWYG),
 *   composites the cat-ear overlay at the latest placement, and returns the result via [onComposited].
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    lens: LensSelector,
    onFaceDetected: (OverlayPlacement?) -> Unit,
    captureRequested: Boolean,
    onComposited: (Bitmap?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val detector = remember { MlKitFaceDetectorImpl() }
    val smoother = remember { PlacementSmoother() }
    val controller = remember { CameraXControllerImpl() }
    val latestPlacement = remember { AtomicReference<OverlayPlacement?>(null) }
    val previewViewRef = remember { AtomicReference<PreviewView?>(null) }

    LaunchedEffect(captureRequested) {
        if (captureRequested) {
            onComposited(captureComposited(context, previewViewRef.get(), latestPlacement.get()))
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            previewViewRef.set(previewView)
            wireController(controller, previewView, lifecycleOwner, detector) { face, w, h ->
                val transform = TransformContext(
                    imageWidth = w,
                    imageHeight = h,
                    viewWidth = previewView.width.coerceAtLeast(1),
                    viewHeight = previewView.height.coerceAtLeast(1),
                    isFrontCamera = lens == LensSelector.Front,
                )
                val placement = facePlacement(face, transform, smoother)
                latestPlacement.set(placement)
                onFaceDetected(placement)
            }
            startCamera(ctx, controller, lens)
            previewView
        },
        update = { controller.bindPreview(lens) },
        modifier = modifier,
    )

    DisposableEffect(Unit) {
        onDispose {
            controller.unbind()
            detector.close()
        }
    }
}

/** Grabs the view-space preview frame and composites the cat-ear overlay at [placement]. */
private fun captureComposited(
    context: android.content.Context,
    previewView: PreviewView?,
    placement: OverlayPlacement?,
): Bitmap? {
    val frame = previewView?.bitmap ?: return null
    val ears = placement?.let {
        decodeDrawableToBitmap(context, R.drawable.ic_cat_ears, EAR_ASSET_WIDTH, EAR_ASSET_HEIGHT)
    }
    return if (placement != null && ears != null) OverlayCompositor.composite(frame, ears, placement) else frame
}

/** Transforms a detected face to a smoothed view-space [OverlayPlacement]. */
private fun facePlacement(
    face: it.marcelpetrick.catears.domain.FaceModel?,
    transform: TransformContext,
    smoother: PlacementSmoother,
): OverlayPlacement? {
    if (face == null) {
        smoother.reset()
        return null
    }
    val viewBox = imageToViewBoundingBox(face.boundingBox, transform)
    return smoother.smooth(computeOverlayPlacement(viewBox, face.headEulerAngleZ))
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun wireController(
    controller: CameraXControllerImpl,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    detector: MlKitFaceDetectorImpl,
    onFace: (it.marcelpetrick.catears.domain.FaceModel?, Int, Int) -> Unit,
) {
    controller.previewView = previewView
    controller.lifecycleOwner = lifecycleOwner
    controller.faceDetector = detector
    controller.onFaceResult = onFace
}

private fun startCamera(context: android.content.Context, controller: CameraXControllerImpl, lens: LensSelector) {
    val providerFuture: ListenableFuture<ProcessCameraProvider> =
        ProcessCameraProvider.getInstance(context)
    // CameraX binding registers a lifecycle observer, which must happen on the
    // main thread; run the provider callback on the main executor. Binding on a
    // background thread throws "Method addObserver must be called on the main
    // thread" and crashes the moment the preview starts.
    providerFuture.addListener(
        {
            controller.setCameraProvider(providerFuture.get())
            controller.bindPreview(lens)
        },
        ContextCompat.getMainExecutor(context),
    )
}
