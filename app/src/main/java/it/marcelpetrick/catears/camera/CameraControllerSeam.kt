// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import android.graphics.Bitmap
import it.marcelpetrick.catears.domain.LensSelector

/**
 * Abstraction over the CameraX controller.
 *
 * Concrete implementations bind CameraX use cases to the lifecycle;
 * the interface allows test doubles in unit tests without a device.
 */
interface CameraControllerSeam {

    /** Bind the camera preview (and analysis) use cases. */
    fun bindPreview(lens: LensSelector)

    /** Release all CameraX use cases and unbind from the lifecycle. */
    fun unbind()

    /** Switch to a different lens by rebinding the use cases. */
    fun switchLens(lens: LensSelector) {
        unbind()
        bindPreview(lens)
    }

    /**
     * Capture a single still frame as a [Bitmap].
     *
     * [onResult] is called on an arbitrary thread with the captured bitmap, or null on failure.
     * Callers must recycle the bitmap when done.
     */
    fun capturePhoto(onResult: (Bitmap?) -> Unit)
}
