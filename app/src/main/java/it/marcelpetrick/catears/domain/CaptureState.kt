// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/** Models the lifecycle of a photo-capture operation. Pure domain — no Android deps. */
sealed interface CaptureState {
    /** No capture in progress. */
    data object Idle : CaptureState

    /** Capture is in progress — shutter released, waiting for the frame. */
    data object Capturing : CaptureState

    /** Capture composited and saved; holds the gallery URI string for sharing. */
    data class Saved(val uriString: String) : CaptureState

    /** Capture or save failed for any reason. */
    data object Failed : CaptureState
}
