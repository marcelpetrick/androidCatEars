// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.facedetect

import it.marcelpetrick.catears.domain.FaceModel

/**
 * Abstraction over the ML Kit face detector.
 *
 * Concrete implementation wraps ML Kit and runs on an image analysis executor.
 * The interface allows test doubles in unit tests without a device or camera.
 */
interface FaceDetectorSeam {

    /**
     * Process a single camera frame.
     *
     * The implementation is responsible for closing [imageProxy] — either synchronously before
     * returning or asynchronously once the underlying detector pipeline completes. The caller
     * must not close it.
     *
     * @param imageProxy The current camera frame.
     * @param onResult Called once detection completes; receives all detected [FaceModel]s
     *   (empty list when no face found, up to [MAX_FACES]).
     */
    fun process(imageProxy: androidx.camera.core.ImageProxy, onResult: (List<FaceModel>) -> Unit)

    companion object {
        /** Maximum number of faces tracked simultaneously. */
        const val MAX_FACES = 4
    }

    /** Release any held ML Kit resources. */
    fun close()
}
