// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.facedetect

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import it.marcelpetrick.catears.domain.BoundingBox
import it.marcelpetrick.catears.domain.FaceModel
import it.marcelpetrick.catears.domain.Point2D
import javax.inject.Inject

/**
 * ML Kit-backed implementation of [FaceDetectorSeam].
 *
 * Excluded from Kover coverage (ML Kit / camera are device-only).
 * Returns up to [FaceDetectorSeam.MAX_FACES] faces sorted largest-bounding-box first
 * so the most prominent face in the frame consistently receives cat ears.
 */
@ExperimentalGetImage
class MlKitFaceDetectorImpl @Inject constructor() : FaceDetectorSeam {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(MINIMUM_FACE_SIZE)
            .enableTracking()
            .build(),
    )

    override fun process(imageProxy: ImageProxy, onResult: (List<FaceModel>) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            onResult(emptyList())
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener { faces ->
                onResult(
                    faces
                        .sortedByDescending { it.boundingBox.width().toLong() * it.boundingBox.height() }
                        .take(FaceDetectorSeam.MAX_FACES)
                        .map { it.toFaceModel() },
                )
            }
            .addOnFailureListener { onResult(emptyList()) }
            .addOnCompleteListener { imageProxy.close() }
    }

    override fun close() = detector.close()

    companion object {
        private const val MINIMUM_FACE_SIZE = 0.15f
    }
}

private fun com.google.mlkit.vision.face.Face.toFaceModel(): FaceModel {
    val box = boundingBox
    return FaceModel(
        boundingBox = BoundingBox(
            left = box.left.toFloat(),
            top = box.top.toFloat(),
            right = box.right.toFloat(),
            bottom = box.bottom.toFloat(),
        ),
        leftEyePosition = getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
            ?.position?.let { Point2D(it.x, it.y) },
        rightEyePosition = getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)
            ?.position?.let { Point2D(it.x, it.y) },
        headEulerAngleZ = headEulerAngleZ,
        headEulerAngleY = headEulerAngleY,
        leftEarPosition = getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EAR)
            ?.position?.let { Point2D(it.x, it.y) },
        rightEarPosition = getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EAR)
            ?.position?.let { Point2D(it.x, it.y) },
        smilingProbability = smilingProbability,
        leftEyeOpenProbability = leftEyeOpenProbability,
        rightEyeOpenProbability = rightEyeOpenProbability,
        trackingId = trackingId,
    )
}
