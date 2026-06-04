// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Matrix
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
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
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
    var previewView: PreviewView? = null
    var lifecycleOwner: LifecycleOwner? = null
    var context: Context? = null

    /** Optional face detector; when set, an ImageAnalysis use case is bound. */
    var faceDetector: FaceDetectorSeam? = null

    /** Receives (faces, uprightImageWidth, uprightImageHeight) on each analysed frame. */
    var onFaceResult: ((List<FaceModel>, Int, Int) -> Unit)? = null

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    private val overlayEffectThread = HandlerThread("CatEarsOverlayEffect").also { it.start() }
    private val overlayEffectHandler = Handler(overlayEffectThread.looper)
    private val videoOverlayState = AtomicReference<VideoOverlayState?>(null)

    fun updateOverlayPlacements(placements: List<OverlayPlacement>, viewWidth: Int, viewHeight: Int) {
        videoOverlayState.set(VideoOverlayState(placements, viewWidth, viewHeight))
    }

    fun setCameraProvider(provider: ProcessCameraProvider) {
        cameraProvider = provider
    }

    override fun bindPreview(lens: LensSelector) {
        val provider = cameraProvider
        val owner = lifecycleOwner
        val surface = previewView
        if (provider == null || owner == null || surface == null) return

        val selector = when (lens) {
            LensSelector.Front -> CameraSelector.DEFAULT_FRONT_CAMERA
            LensSelector.Rear -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        val preview = Preview.Builder().build().also { it.surfaceProvider = surface.surfaceProvider }
        val analysis = buildAnalysisUseCase()
        val vc = buildVideoUseCase()
        val effect = buildOverlayEffect()

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
            Log.d(TAG, "Camera bound (lens=$lens)")
        } catch (e: Exception) {
            Log.e(TAG, "Camera bind failed", e)
        }
    }

    private fun buildOverlayEffect(): OverlayEffect {
        val effect = OverlayEffect(
            CameraEffect.VIDEO_CAPTURE,
            OVERLAY_QUEUE_DEPTH,
            overlayEffectHandler,
        ) { error -> Log.e(TAG, "OverlayEffect error", error) }
        effect.setOnDrawListener { frame ->
            val state = videoOverlayState.get()
            if (state != null && state.placements.isNotEmpty()) {
                val fW = frame.getSize().width.toFloat()
                val fH = frame.getSize().height.toFloat()
                val canvas = frame.getOverlayCanvas()
                canvas.save()
                canvas.concat(
                    viewToBufferMatrix(
                        fW,
                        fH,
                        state.viewWidth.toFloat(),
                        state.viewHeight.toFloat(),
                        frame.getRotationDegrees(),
                    ),
                )
                OverlayCompositor.drawEarsOnCanvas(canvas, state.placements)
                canvas.restore()
            }
            true
        }
        return effect
    }

    private fun buildAnalysisUseCase(): UseCase? {
        val detector = faceDetector ?: return null
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(analysisExecutor) { proxy -> analyseFrame(detector, proxy) }
        return analysis
    }

    private fun buildVideoUseCase(): UseCase {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        return VideoCapture.withOutput(recorder).also { videoCapture = it }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyseFrame(detector: FaceDetectorSeam, proxy: ImageProxy) {
        val rotation = proxy.imageInfo.rotationDegrees
        val portrait = rotation == ROTATION_90 || rotation == ROTATION_270
        val width = if (portrait) proxy.height else proxy.width
        val height = if (portrait) proxy.width else proxy.height
        @Suppress("TooGenericExceptionCaught")
        try {
            detector.process(proxy) { face -> onFaceResult?.invoke(face, width, height) }
        } catch (e: Exception) {
            Log.e(TAG, "Face analysis failed for frame", e)
            proxy.close()
        }
    }

    override fun startVideoRecording(onFinished: (uriString: String?) -> Unit) {
        val ctx = context ?: return
        val vc = videoCapture ?: return
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "cat_ears_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        }
        val options = MediaStoreOutputOptions
            .Builder(ctx.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        activeRecording = vc.output.prepareRecording(ctx, options)
            .start(ContextCompat.getMainExecutor(ctx)) { event: VideoRecordEvent ->
                if (event is VideoRecordEvent.Finalize) {
                    val uri = if (!event.hasError()) event.outputResults.outputUri.toString() else null
                    onFinished(uri)
                    activeRecording = null
                }
            }
        Handler(Looper.getMainLooper()).postDelayed({ stopVideoRecording() }, VIDEO_DURATION_MS)
    }

    override fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    override fun unbind() {
        cameraProvider?.unbindAll()
    }

    override fun close() {
        stopVideoRecording()
        unbind()
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
