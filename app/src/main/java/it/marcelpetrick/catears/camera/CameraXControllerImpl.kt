// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.camera.view.TransformExperimental::class)

package it.marcelpetrick.catears.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.effects.OverlayEffect
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.camera.view.transform.OutputTransform
import androidx.core.content.ContextCompat
import androidx.core.graphics.withMatrix
import it.marcelpetrick.catears.capture.OverlayCompositor
import it.marcelpetrick.catears.domain.FaceModel
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.facedetect.FaceDetectorSeam
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * CameraX-backed implementation of [CameraControllerSeam].
 *
 * Excluded from Kover coverage (lifecycle-bound; requires a real device to test).
 * All testable logic lives behind the [CameraControllerSeam] interface and in the
 * pure-domain transform/placement helpers used by the caller.
 */
class CameraXControllerImpl @Inject constructor() : CameraControllerSeam {

    private var cameraProvider: ProcessCameraProvider? = null
    private var bindConfig: CameraBindConfig? = null
    private var context: Context? = null

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private val autoStopHandler = Handler(Looper.getMainLooper())
    private var autoStopRunnable: Runnable? = null

    private val overlayEffectThread = HandlerThread("CatEarsOverlayEffect").also { it.start() }
    private val overlayEffectHandler = Handler(overlayEffectThread.looper)
    private val videoOverlayState = AtomicReference<VideoOverlayState?>(null)
    private val imageProxyTransformFactory = ImageProxyTransformFactory().apply {
        setUsingCropRect(true)
        setUsingRotationDegrees(true)
    }
    private var currentOverlayEffect: OverlayEffect? = null
    private var boundLens: LensSelector? = null

    @Volatile private var isFrontCamera: Boolean = false

    /** Supplies all camera-session dependencies in one call before [bindPreview]. */
    fun configure(config: CameraBindConfig) {
        bindConfig = config
        context = config.context
    }

    fun updateOverlayPlacements(placements: List<OverlayPlacement>, viewWidth: Int, viewHeight: Int) {
        videoOverlayState.set(VideoOverlayState(placements, viewWidth, viewHeight))
    }

    fun setCameraProvider(provider: ProcessCameraProvider) {
        cameraProvider = provider
    }

    override fun bindPreview(lens: LensSelector) {
        val config = bindConfig
        val provider = cameraProvider
        if (config == null || provider == null || shouldSkipBind(lens)) return
        val owner = config.lifecycleOwner
        val surface = config.previewView

        isFrontCamera = lens == LensSelector.Front
        val selector = when (lens) {
            LensSelector.Front -> CameraSelector.DEFAULT_FRONT_CAMERA
            LensSelector.Rear -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        val preview = Preview.Builder().build().also { it.surfaceProvider = surface.surfaceProvider }
        val analysis = buildAnalysisUseCase(config)
        val vc = buildVideoUseCase()
        currentOverlayEffect?.close()
        val effect = buildOverlayEffect()
        currentOverlayEffect = effect

        // Binding can fail (camera busy, unsupported use-case combination, hardware quirks).
        // Never let that crash the app: log it and leave the preview unbound.
        @Suppress("TooGenericExceptionCaught")
        try {
            provider.unbindAll()
            val groupBuilder = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(vc)
                .addEffect(effect)
            if (analysis != null) groupBuilder.addUseCase(analysis)
            provider.bindToLifecycle(owner, selector, groupBuilder.build())
            boundLens = lens
            Log.d(TAG, "Camera bound (lens=$lens)")
        } catch (e: Exception) {
            boundLens = null
            videoCapture = null
            currentOverlayEffect = null
            effect.close()
            Log.e(TAG, "Camera bind failed", e)
            config.onBindFailed?.invoke()
        }
    }

    private fun shouldSkipBind(lens: LensSelector): Boolean {
        if (activeRecording != null) {
            Log.w(TAG, "Ignoring camera rebind while recording")
            return true
        }
        return boundLens == lens && videoCapture != null
    }

    private fun buildOverlayEffect(): OverlayEffect {
        val effect = OverlayEffect(
            CameraEffect.VIDEO_CAPTURE,
            OVERLAY_QUEUE_DEPTH,
            overlayEffectHandler,
        ) { error -> Log.e(TAG, "OverlayEffect error", error) }
        effect.setOnDrawListener { frame ->
            val overlayCanvas = frame.getOverlayCanvas()
            // CameraX reuses the overlay surface; clear it so moved/lost faces do not ghost.
            overlayCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            val state = videoOverlayState.get()
            if (state != null && state.placements.isNotEmpty()) {
                val fW = frame.getSize().width.toFloat()
                val fH = frame.getSize().height.toFloat()
                val matrix = viewToBufferMatrix(
                    fW,
                    fH,
                    state.viewWidth.toFloat(),
                    state.viewHeight.toFloat(),
                    frame.getRotationDegrees(),
                )
                // Front-camera view is shown mirrored on screen but the video buffer is not.
                // Flip the view-space x axis before rotating so ears land on the correct sides.
                if (isFrontCamera && !frame.isMirroring()) {
                    matrix.preScale(-1f, 1f, state.viewWidth / 2f, state.viewHeight / 2f)
                }
                overlayCanvas.withMatrix(matrix) {
                    OverlayCompositor.drawEarsOnCanvas(this, state.placements, context?.resources)
                }
            }
            true
        }
        return effect
    }

    private fun buildAnalysisUseCase(config: CameraBindConfig): UseCase? {
        val detector = config.faceDetector
        val onFace = config.onFaceResult
        if (detector == null || onFace == null) return null
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(analysisExecutor) { proxy -> analyseFrame(detector, proxy, onFace) }
        return analysis
    }

    private fun buildVideoUseCase(): UseCase {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        return VideoCapture.withOutput(recorder).also { videoCapture = it }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class, TransformExperimental::class)
    private fun analyseFrame(
        detector: FaceDetectorSeam,
        proxy: ImageProxy,
        onFace: (List<FaceModel>, OutputTransform?, Int, Int) -> Unit,
    ) {
        val rotation = proxy.imageInfo.rotationDegrees
        val portrait = rotation == ROTATION_90 || rotation == ROTATION_270
        val width = if (portrait) proxy.height else proxy.width
        val height = if (portrait) proxy.width else proxy.height
        val imageOutputTransform = imageProxyTransformFactory.getOutputTransform(proxy)
        // Per FaceDetectorSeam contract the implementation closes proxy (sync or async).
        // We still guard against a synchronous throw that would skip the impl's own close.
        @Suppress("TooGenericExceptionCaught")
        try {
            detector.process(proxy) { face -> onFace(face, imageOutputTransform, width, height) }
        } catch (e: Exception) {
            Log.e(TAG, "Face analysis failed for frame", e)
            runCatching { proxy.close() }
        }
    }

    override fun startVideoRecording(onFinished: (uriString: String?) -> Unit) {
        val ctx = context
        val vc = videoCapture
        if (activeRecording != null) return
        if (ctx == null || vc == null) {
            onFinished(null)
        } else {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "cat_ears_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            }
            val options = MediaStoreOutputOptions
                .Builder(ctx.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build()
            @Suppress("TooGenericExceptionCaught")
            try {
                activeRecording = vc.output.prepareRecording(ctx, options)
                    .start(ContextCompat.getMainExecutor(ctx)) { event: VideoRecordEvent ->
                        if (event is VideoRecordEvent.Finalize) {
                            val uri = if (!event.hasError()) event.outputResults.outputUri.toString() else null
                            onFinished(uri)
                            activeRecording = null
                        }
                    }
                val runnable = Runnable { stopVideoRecording() }
                autoStopRunnable = runnable
                autoStopHandler.postDelayed(runnable, VIDEO_DURATION_MS)
            } catch (e: Exception) {
                Log.e(TAG, "Video recording failed to start", e)
                onFinished(null)
            }
        }
    }

    override fun stopVideoRecording() {
        autoStopRunnable?.let { autoStopHandler.removeCallbacks(it) }
        autoStopRunnable = null
        activeRecording?.stop()
        activeRecording = null
    }

    override fun unbind() {
        cameraProvider?.unbindAll()
        videoCapture = null
        boundLens = null
    }

    override fun close() {
        stopVideoRecording()
        unbind()
        currentOverlayEffect?.close()
        currentOverlayEffect = null
        analysisExecutor.shutdown()
        overlayEffectThread.quitSafely()
    }

    private data class VideoOverlayState(
        val placements: List<OverlayPlacement>,
        val viewWidth: Int,
        val viewHeight: Int,
    )

    private companion object {
        const val ROTATION_90 = 90
        const val ROTATION_180 = 180
        const val ROTATION_270 = 270
        const val TAG = "CatEars"
        const val VIDEO_DURATION_MS = 5_000L
        const val OVERLAY_QUEUE_DEPTH = 2

        /**
         * Produces a 3×3 affine [Matrix] (row-major) that maps a point (vx, vy) in view-space
         * to the correct pixel position in the video buffer, accounting for the buffer's
         * rotation relative to the display.
         *
         * Derivation for rotDeg=90 (buffer is landscape, display is portrait):
         *   bx = vy * fW/vH  (view-y maps to buffer-x)
         *   by = fH - vx * fH/vW  (view-x maps to inverted buffer-y)
         * The other three rotations are derived analogously.
         */
        fun viewToBufferMatrix(fW: Float, fH: Float, vW: Float, vH: Float, rotDeg: Int): Matrix = Matrix().also { m ->
            m.setValues(
                when (rotDeg) {
                    ROTATION_90 -> floatArrayOf(0f, fW / vH, 0f, -fH / vW, 0f, fH, 0f, 0f, 1f)
                    ROTATION_180 -> floatArrayOf(-fW / vW, 0f, fW, 0f, -fH / vH, fH, 0f, 0f, 1f)
                    ROTATION_270 -> floatArrayOf(0f, -fW / vH, fW, fH / vW, 0f, 0f, 0f, 0f, 1f)
                    else -> floatArrayOf(fW / vW, 0f, 0f, 0f, fH / vH, 0f, 0f, 0f, 1f)
                },
            )
        }
    }
}
