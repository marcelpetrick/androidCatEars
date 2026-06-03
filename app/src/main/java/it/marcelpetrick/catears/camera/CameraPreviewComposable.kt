// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import android.graphics.Bitmap
import android.util.Log
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
import it.marcelpetrick.catears.capture.OverlayCompositor
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.domain.PlacementSmoother
import it.marcelpetrick.catears.domain.TransformContext
import it.marcelpetrick.catears.domain.computeOverlayPlacement
import it.marcelpetrick.catears.domain.imageToViewBoundingBox
import it.marcelpetrick.catears.domain.imageToViewCoordinates
import it.marcelpetrick.catears.facedetect.FaceDetectorSeam
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "CatEars"

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
    cameraControllerFactory: () -> CameraXControllerImpl,
    faceDetectorFactory: () -> FaceDetectorSeam,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val detector = remember { faceDetectorFactory() }
    val smoother = remember { PlacementSmoother() }
    val controller = remember { cameraControllerFactory() }
    val latestPlacement = remember { AtomicReference<OverlayPlacement?>(null) }
    val previewViewRef = remember { AtomicReference<PreviewView?>(null) }

    LaunchedEffect(captureRequested) {
        if (captureRequested) {
            onComposited(captureComposited(previewViewRef.get(), latestPlacement.get()))
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
            controller.close()
            detector.close()
        }
    }
}

/**
 * Grabs the view-space preview frame and composites the cat-ear overlay at [placement].
 *
 * Bulletproof by design: if there is no preview frame yet, returns null (capture fails
 * gracefully); if no face was detected ([placement] is null), returns the plain frame and
 * applies **no** ears; if compositing itself fails, falls back to the plain frame. Never throws.
 */
private fun captureComposited(previewView: PreviewView?, placement: OverlayPlacement?): Bitmap? {
    val frame = previewView?.bitmap
    if (frame == null) {
        Log.w(TAG, "Capture skipped: preview frame not ready")
        return null
    }
    return when (placement) {
        null -> {
            Log.d(TAG, "Capturing without ears: no face detected")
            frame
        }

        else -> compositeEarsOrFrame(frame, placement)
    }
}

/** Draws procedural ears onto [frame]; any failure yields the plain frame. */
@Suppress("TooGenericExceptionCaught")
private fun compositeEarsOrFrame(frame: Bitmap, placement: OverlayPlacement): Bitmap = try {
    OverlayCompositor.composite(frame, placement)
} catch (e: Exception) {
    Log.e(TAG, "Compositing ears failed; saving plain frame", e)
    frame
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
    val leftEar = face.leftEarPosition?.let { imageToViewCoordinates(it, transform) }
    val rightEar = face.rightEarPosition?.let { imageToViewCoordinates(it, transform) }
    val eyeOpennessMean = listOfNotNull(face.leftEyeOpenProbability, face.rightEyeOpenProbability)
        .takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 1f
    return smoother.smooth(
        computeOverlayPlacement(
            viewBox = viewBox,
            headEulerAngleZ = face.headEulerAngleZ,
            headEulerAngleY = face.headEulerAngleY,
            leftEarAnchor = leftEar,
            rightEarAnchor = rightEar,
            smilingProbability = face.smilingProbability ?: 0f,
            eyeOpennessMean = eyeOpennessMean,
        ),
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun wireController(
    controller: CameraXControllerImpl,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    detector: FaceDetectorSeam,
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
