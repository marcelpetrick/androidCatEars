// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
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
import it.marcelpetrick.catears.domain.FaceModel
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.facedetect.FaceDetectorSeam
import java.util.concurrent.Executors
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

        // Binding can fail (camera busy, unsupported use-case combination, hardware quirks).
        // Never let that crash the app: log it and leave the preview unbound.
        @Suppress("TooGenericExceptionCaught")
        try {
            provider.unbindAll()
            if (analysis != null) {
                provider.bindToLifecycle(owner, selector, preview, analysis, vc)
            } else {
                provider.bindToLifecycle(owner, selector, preview, vc)
            }
            Log.d(TAG, "Camera bound (lens=$lens)")
        } catch (e: Exception) {
            Log.e(TAG, "Camera bind failed", e)
        }
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
    }

    private companion object {
        const val ROTATION_90 = 90
        const val ROTATION_270 = 270
        const val TAG = "CatEars"
        const val VIDEO_DURATION_MS = 5_000L
    }
}
