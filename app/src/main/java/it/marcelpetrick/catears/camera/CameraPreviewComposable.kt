// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.camera.view.TransformExperimental::class)

package it.marcelpetrick.catears.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.media.MediaActionSound
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.OutputTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
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
import java.util.concurrent.atomic.AtomicBoolean
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
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val detector = remember { faceDetectorFactory() }
    val smoother = remember { MultiFaceSmoother() }
    val currentOnCameraError by rememberUpdatedState(onCameraError)
    val capturePlacements = rememberUpdatedState(overlayPlacements)
    val disposedRef = remember { AtomicBoolean(false) }
    val previewViewRef = remember { AtomicReference<PreviewView?>(null) }
    val previewWidthRef = remember { java.util.concurrent.atomic.AtomicInteger(1) }
    val previewHeightRef = remember { java.util.concurrent.atomic.AtomicInteger(1) }
    // Stable reference so the factory-block closure always reads the current lens.
    val currentLens by rememberUpdatedState(lens)
    val soundPlayer = remember {
        MediaActionSound().also {
            it.load(MediaActionSound.SHUTTER_CLICK)
            it.load(MediaActionSound.START_VIDEO_RECORDING)
            it.load(MediaActionSound.STOP_VIDEO_RECORDING)
        }
    }

    LaunchedEffect(lens) {
        smoother.reset()
        onFaceDetected(emptyList())
        controller.updateOverlayPlacements(
            emptyList(),
            previewWidthRef.get(),
            previewHeightRef.get(),
        )
    }

    LaunchedEffect(overlayPlacements) {
        if (previewViewRef.get() == null) return@LaunchedEffect
        controller.updateOverlayPlacements(
            overlayPlacements,
            previewWidthRef.get(),
            previewHeightRef.get(),
        )
    }

    LaunchedEffect(captureRequested) {
        if (captureRequested) {
            val bitmap = captureComposited(resources, previewViewRef.get(), capturePlacements.value)
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
            previewView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                previewWidthRef.set((right - left).coerceAtLeast(1))
                previewHeightRef.set((bottom - top).coerceAtLeast(1))
            }
            controller.configure(
                CameraBindConfig(
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
                    context = ctx,
                    faceDetector = detector,
                    onFaceResult = { faces, imageOutputTransform, w, h ->
                        previewView.post {
                            if (disposedRef.get()) return@post
                            val fallbackTransform = TransformContext(
                                imageWidth = w,
                                imageHeight = h,
                                viewWidth = previewWidthRef.get(),
                                viewHeight = previewHeightRef.get(),
                                isFrontCamera = currentLens == LensSelector.Front,
                            )
                            onFaceDetected(
                                facePlacements(
                                    faces = faces,
                                    imageToViewMatrix = imageToViewMatrix(imageOutputTransform, previewView),
                                    fallbackTransform = fallbackTransform,
                                    smoother = smoother,
                                ),
                            )
                        }
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
        disposedRef.set(false)
        onDispose {
            disposedRef.set(true)
            onFaceDetected(emptyList())
            controller.updateOverlayPlacements(
                emptyList(),
                previewWidthRef.get(),
                previewHeightRef.get(),
            )
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
private fun captureComposited(
    resources: android.content.res.Resources,
    previewView: PreviewView?,
    placements: List<OverlayPlacement>,
): Bitmap? {
    val frame = previewView?.bitmap
    if (frame == null) {
        Log.w(TAG, "Capture skipped: preview frame not ready")
        return null
    }
    return if (placements.isEmpty()) {
        Log.d(TAG, "Capturing without ears: no face detected")
        frame
    } else {
        compositeEarsOrFrame(frame, placements, resources)
    }
}

/** Draws procedural ears onto [frame]; any failure yields the plain frame. */
@Suppress("TooGenericExceptionCaught")
private fun compositeEarsOrFrame(
    frame: Bitmap,
    placements: List<OverlayPlacement>,
    resources: android.content.res.Resources,
): Bitmap = try {
    OverlayCompositor.composite(frame, placements, resources)
} catch (e: Exception) {
    Log.e(TAG, "Compositing ears failed; saving plain frame", e)
    frame
}

/** Transforms all detected faces to smoothed view-space [OverlayPlacement]s. */
private fun facePlacements(
    faces: List<it.marcelpetrick.catears.domain.FaceModel>,
    imageToViewMatrix: Matrix?,
    fallbackTransform: TransformContext,
    smoother: MultiFaceSmoother,
): List<OverlayPlacement> {
    if (faces.isEmpty()) {
        smoother.reset()
        return emptyList()
    }
    val entries = faces.mapNotNull { face ->
        val placement = singleFacePlacement(face, imageToViewMatrix, fallbackTransform) ?: return@mapNotNull null
        Pair(face.trackingId, placement)
    }
    return if (entries.isEmpty()) emptyList() else smoother.smooth(entries)
}

private fun singleFacePlacement(
    face: it.marcelpetrick.catears.domain.FaceModel,
    imageToViewMatrix: Matrix?,
    fallbackTransform: TransformContext,
): OverlayPlacement? {
    val viewBox = if (imageToViewMatrix != null) {
        mapBoundingBox(face.boundingBox, imageToViewMatrix)
    } else {
        imageToViewBoundingBox(face.boundingBox, fallbackTransform)
    }
    val leftEye = face.leftEyePosition?.let { point ->
        imageToViewMatrix?.let { mapPoint(point, it) } ?: imageToViewCoordinates(point, fallbackTransform)
    }
    val rightEye = face.rightEyePosition?.let { point ->
        imageToViewMatrix?.let { mapPoint(point, it) } ?: imageToViewCoordinates(point, fallbackTransform)
    }
    val eyeOpennessMean = listOfNotNull(face.leftEyeOpenProbability, face.rightEyeOpenProbability)
        .takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 1f
    val leftEyeOpenness = face.leftEyeOpenProbability ?: 1f
    val rightEyeOpenness = face.rightEyeOpenProbability ?: 1f
    return computeOverlayPlacement(
        viewBox = viewBox,
        headEulerAngleZ = face.headEulerAngleZ,
        headEulerAngleY = face.headEulerAngleY,
        leftEyeAnchor = leftEye,
        rightEyeAnchor = rightEye,
        smilingProbability = face.smilingProbability ?: 0f,
        eyeOpennessMean = eyeOpennessMean,
        leftEyeOpenness = leftEyeOpenness,
        rightEyeOpenness = rightEyeOpenness,
        trackingId = face.trackingId,
    )
}

@androidx.annotation.OptIn(TransformExperimental::class)
private fun imageToViewMatrix(imageOutputTransform: OutputTransform?, previewView: PreviewView): Matrix? {
    val previewOutputTransform = previewView.outputTransform ?: return null
    return imageOutputTransform?.let { imageTransform ->
        Matrix().also { matrix ->
            CoordinateTransform(imageTransform, previewOutputTransform).transform(matrix)
        }
    }
}

private fun mapBoundingBox(
    box: it.marcelpetrick.catears.domain.BoundingBox,
    matrix: Matrix,
): it.marcelpetrick.catears.domain.BoundingBox {
    val rect = RectF(box.left, box.top, box.right, box.bottom)
    matrix.mapRect(rect)
    return it.marcelpetrick.catears.domain.BoundingBox(
        left = rect.left,
        top = rect.top,
        right = rect.right,
        bottom = rect.bottom,
    )
}

private fun mapPoint(
    point: it.marcelpetrick.catears.domain.Point2D,
    matrix: Matrix,
): it.marcelpetrick.catears.domain.Point2D {
    val mapped = floatArrayOf(point.x, point.y)
    matrix.mapPoints(mapped)
    return it.marcelpetrick.catears.domain.Point2D(mapped[0], mapped[1])
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
