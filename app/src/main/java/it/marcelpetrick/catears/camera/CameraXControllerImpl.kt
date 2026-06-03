// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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

    /** Optional face detector; when set, an ImageAnalysis use case is bound. */
    var faceDetector: FaceDetectorSeam? = null

    /** Receives (face, uprightImageWidth, uprightImageHeight) on each analysed frame. */
    var onFaceResult: ((FaceModel?, Int, Int) -> Unit)? = null

    private val analysisExecutor = Executors.newSingleThreadExecutor()

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

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = surface.surfaceProvider
        }

        // Binding can fail (camera busy, unsupported use-case combination, hardware
        // quirks). Never let that crash the app: log it and leave the preview unbound.
        @Suppress("TooGenericExceptionCaught")
        try {
            provider.unbindAll()
            val analysis = buildAnalysisUseCase()
            if (analysis != null) {
                provider.bindToLifecycle(owner, selector, preview, analysis)
            } else {
                provider.bindToLifecycle(owner, selector, preview)
            }
            Log.d(TAG, "Camera bound (lens=$lens, faceTracking=${analysis != null})")
        } catch (e: Exception) {
            Log.e(TAG, "Camera bind failed", e)
        }
    }

    private fun buildAnalysisUseCase(): ImageAnalysis? {
        val detector = faceDetector ?: return null
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(analysisExecutor) { proxy -> analyseFrame(detector, proxy) }
        return analysis
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyseFrame(detector: FaceDetectorSeam, proxy: ImageProxy) {
        val rotation = proxy.imageInfo.rotationDegrees
        val portrait = rotation == ROTATION_90 || rotation == ROTATION_270
        val width = if (portrait) proxy.height else proxy.width
        val height = if (portrait) proxy.width else proxy.height
        // FaceDetectorSeam.process closes the proxy on the normal path. Guard against
        // a synchronous failure (e.g. an unsupported frame) so one bad frame can never
        // crash the analysis thread or leak the proxy.
        @Suppress("TooGenericExceptionCaught")
        try {
            detector.process(proxy) { face -> onFaceResult?.invoke(face, width, height) }
        } catch (e: Exception) {
            Log.e(TAG, "Face analysis failed for frame", e)
            proxy.close()
        }
    }

    override fun unbind() {
        cameraProvider?.unbindAll()
    }

    private companion object {
        const val ROTATION_90 = 90
        const val ROTATION_270 = 270
        const val TAG = "CatEars"
    }
}
