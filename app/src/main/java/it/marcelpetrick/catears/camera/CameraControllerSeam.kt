// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import it.marcelpetrick.catears.domain.LensSelector

/**
 * Abstraction over the CameraX controller.
 *
 * Concrete implementations bind CameraX use cases to the lifecycle;
 * the interface allows test doubles in unit tests without a device.
 */
interface CameraControllerSeam : AutoCloseable {

    /** Bind the camera preview (and analysis) use cases. */
    fun bindPreview(lens: LensSelector)

    /** Release all CameraX use cases and unbind from the lifecycle. */
    fun unbind()

    /** Switch to a different lens by rebinding the use cases. */
    fun switchLens(lens: LensSelector) {
        unbind()
        bindPreview(lens)
    }

    /** Start a fixed-duration video recording; [onFinished] receives the MediaStore URI or null on failure. */
    fun startVideoRecording(onFinished: (uriString: String?) -> Unit): Unit = Unit

    /** Stop an in-progress recording early (the camera impl handles auto-stop after the clip duration). */
    fun stopVideoRecording(): Unit = Unit

    override fun close() {
        unbind()
    }
}
