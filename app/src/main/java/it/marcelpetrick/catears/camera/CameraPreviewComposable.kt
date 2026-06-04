// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import android.graphics.Bitmap
import android.media.MediaActionSound
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import it.marcelpetrick.catears.capture.OverlayCompositor
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.domain.MultiFaceSmoother
import it.marcelpetrick.catears.domain.OverlayPlacement
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
    overlayPlacements: List<OverlayPlacement>,
    onFaceDetected: (List<OverlayPlacement>) -> Unit,
    captureRequested: Boolean,
    onComposited: (Bitmap?) -> Unit,
    controller: CameraXControllerImpl,
    faceDetectorFactory: () -> FaceDetectorSeam,
    modifier: Modifier = Modifier,
    recordingRequested: Boolean = false,
    onRecordingSaved: (String?) -> Unit = {},
    onCameraError: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val detector = remember { faceDetectorFactory() }
    val smoother = remember { MultiFaceSmoother() }
    val currentOnCameraError by rememberUpdatedState(onCameraError)
    val capturePlacements = rememberUpdatedState(overlayPlacements)
    val previewViewRef = remember { AtomicReference<PreviewView?>(null) }
    // Stable reference so the factory-block closure always reads the current lens.
    val currentLens by rememberUpdatedState(lens)
    val soundPlayer = remember {
        MediaActionSound().also {
            it.load(MediaActionSound.SHUTTER_CLICK)
            it.load(MediaActionSound.START_VIDEO_RECORDING)
            it.load(MediaActionSound.STOP_VIDEO_RECORDING)
        }
    }

    LaunchedEffect(overlayPlacements) {
        val pv = previewViewRef.get() ?: return@LaunchedEffect
        controller.updateOverlayPlacements(
            overlayPlacements,
            pv.width.coerceAtLeast(1),
            pv.height.coerceAtLeast(1),
        )
    }

    LaunchedEffect(captureRequested) {
        if (captureRequested) {
            val bitmap = captureComposited(previewViewRef.get(), capturePlacements.value)
            if (bitmap != null) soundPlayer.play(MediaActionSound.SHUTTER_CLICK)
            onComposited(bitmap)
        }
    }

    LaunchedEffect(recordingRequested) {
        if (recordingRequested) {
            soundPlayer.play(MediaActionSound.START_VIDEO_RECORDING)
            controller.startVideoRecording { uriString ->
                if (uriString != null) soundPlayer.play(MediaActionSound.STOP_VIDEO_RECORDING)
                onRecordingSaved(uriString)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            previewViewRef.set(previewView)
            controller.configure(
                CameraBindConfig(
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
                    context = ctx,
                    faceDetector = detector,
                    onFaceResult = { faces, w, h ->
                        val transform = TransformContext(
                            imageWidth = w,
                            imageHeight = h,
                            viewWidth = previewView.width.coerceAtLeast(1),
                            viewHeight = previewView.height.coerceAtLeast(1),
                            isFrontCamera = currentLens == LensSelector.Front,
                        )
                        onFaceDetected(facePlacements(faces, transform, smoother))
                    },
                    onBindFailed = { currentOnCameraError() },
                ),
            )
            startCamera(ctx, controller, lens)
            previewView
        },
        update = { controller.bindPreview(lens) },
        modifier = modifier,
    )

    DisposableEffect(Unit) {
        onDispose {
            soundPlayer.release()
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
private fun captureComposited(previewView: PreviewView?, placements: List<OverlayPlacement>): Bitmap? {
    val frame = previewView?.bitmap
    if (frame == null) {
        Log.w(TAG, "Capture skipped: preview frame not ready")
        return null
    }
    return if (placements.isEmpty()) {
        Log.d(TAG, "Capturing without ears: no face detected")
        frame
    } else {
        compositeEarsOrFrame(frame, placements)
    }
}

/** Draws procedural ears onto [frame]; any failure yields the plain frame. */
@Suppress("TooGenericExceptionCaught")
private fun compositeEarsOrFrame(frame: Bitmap, placements: List<OverlayPlacement>): Bitmap = try {
    OverlayCompositor.composite(frame, placements)
} catch (e: Exception) {
    Log.e(TAG, "Compositing ears failed; saving plain frame", e)
    frame
}

/** Transforms all detected faces to smoothed view-space [OverlayPlacement]s. */
private fun facePlacements(
    faces: List<it.marcelpetrick.catears.domain.FaceModel>,
    transform: TransformContext,
    smoother: MultiFaceSmoother,
): List<OverlayPlacement> {
    if (faces.isEmpty()) {
        smoother.reset()
        return emptyList()
    }
    val entries = faces.mapNotNull { face ->
        val placement = singleFacePlacement(face, transform) ?: return@mapNotNull null
        Pair(face.trackingId, placement)
    }
    return if (entries.isEmpty()) emptyList() else smoother.smooth(entries)
}

private fun singleFacePlacement(
    face: it.marcelpetrick.catears.domain.FaceModel,
    transform: TransformContext,
): OverlayPlacement? {
    val viewBox = imageToViewBoundingBox(face.boundingBox, transform)
    val leftEye = face.leftEyePosition?.let { imageToViewCoordinates(it, transform) }
    val rightEye = face.rightEyePosition?.let { imageToViewCoordinates(it, transform) }
    val eyeOpennessMean = listOfNotNull(face.leftEyeOpenProbability, face.rightEyeOpenProbability)
        .takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 1f
    return computeOverlayPlacement(
        viewBox = viewBox,
        headEulerAngleZ = face.headEulerAngleZ,
        headEulerAngleY = face.headEulerAngleY,
        leftEyeAnchor = leftEye,
        rightEyeAnchor = rightEye,
        smilingProbability = face.smilingProbability ?: 0f,
        eyeOpennessMean = eyeOpennessMean,
        trackingId = face.trackingId,
    )
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
