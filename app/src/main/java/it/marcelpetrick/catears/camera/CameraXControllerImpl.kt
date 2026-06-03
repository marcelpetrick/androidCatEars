// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import it.marcelpetrick.catears.domain.LensSelector
import javax.inject.Inject

/**
 * CameraX-backed implementation of [CameraControllerSeam].
 *
 * Excluded from Kover coverage (lifecycle-bound; requires a real device to test).
 * All testable logic lives behind the [CameraControllerSeam] interface.
 */
class CameraXControllerImpl @Inject constructor() : CameraControllerSeam {

    private var cameraProvider: ProcessCameraProvider? = null
    var previewView: PreviewView? = null
    var lifecycleOwner: LifecycleOwner? = null

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

        provider.unbindAll()
        provider.bindToLifecycle(owner, selector, preview)
    }

    override fun unbind() {
        cameraProvider?.unbindAll()
    }
}
