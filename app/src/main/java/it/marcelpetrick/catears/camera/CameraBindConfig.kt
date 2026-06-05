// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.camera.view.TransformExperimental::class)

package it.marcelpetrick.catears.camera

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.camera.view.transform.OutputTransform
import androidx.lifecycle.LifecycleOwner
import it.marcelpetrick.catears.domain.FaceModel
import it.marcelpetrick.catears.facedetect.FaceDetectorSeam

/**
 * All dependencies [CameraXControllerImpl] needs to bind a camera session.
 *
 * Pass to [CameraXControllerImpl.configure] before calling [CameraXControllerImpl.bindPreview].
 * This replaces six scattered `var` property injections with a single validated handoff.
 */
data class CameraBindConfig(
    val previewView: PreviewView,
    val lifecycleOwner: LifecycleOwner,
    val context: Context,
    val faceDetector: FaceDetectorSeam?,
    val onFaceResult: ((List<FaceModel>, OutputTransform?, Int, Int) -> Unit)?,
    val onBindFailed: (() -> Unit)?,
)
