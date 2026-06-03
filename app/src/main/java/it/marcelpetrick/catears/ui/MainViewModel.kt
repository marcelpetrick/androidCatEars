// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.marcelpetrick.catears.capture.ImageSaver
import it.marcelpetrick.catears.domain.CaptureState
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.domain.PermissionState
import it.marcelpetrick.catears.domain.permissionResultToState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val imageSaver: ImageSaver,
    private val captureRuntime: CaptureRuntime,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Initialising)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _lens = MutableStateFlow(LensSelector.Front)
    val lens: StateFlow<LensSelector> = _lens.asStateFlow()

    fun onToggleLens() {
        _lens.value = _lens.value.toggled()
    }

    private val _overlayPlacement = MutableStateFlow<OverlayPlacement?>(null)
    val overlayPlacement: StateFlow<OverlayPlacement?> = _overlayPlacement.asStateFlow()

    /** Called from the face-detection callback with the smoothed placement, or null if no face. */
    fun onFaceDetected(placement: OverlayPlacement?) {
        _overlayPlacement.value = placement
    }

    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    /** Capture button pressed; the camera layer observes this and produces the composited frame. */
    fun onCaptureRequested() {
        _captureState.value = CaptureState.Capturing
    }

    /**
     * Called by the camera layer with the composited (preview + ears) bitmap.
     * Saves it to the gallery off the main thread, then transitions to Saved or Failed.
     */
    fun onCompositedBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            _captureState.value = CaptureState.Failed
            return
        }
        viewModelScope.launch {
            val uri = withContext(captureRuntime.ioDispatcher) {
                imageSaver.save(
                    bitmap = bitmap,
                    epochMillis = captureRuntime.nowMillis(),
                    randomSuffix = captureRuntime.randomSuffix(),
                )
            }
            _captureState.value = if (uri != null) CaptureState.Saved(uri.toString()) else CaptureState.Failed
        }
    }

    /** Resets the capture state back to Idle (e.g. after the saved photo has been shared). */
    fun onCaptureConsumed() {
        _captureState.value = CaptureState.Idle
    }

    /**
     * Called by the UI once the Android permission result is known.
     *
     * @param granted Whether the CAMERA permission was granted.
     * @param showRationale Whether the rationale should still be shown — distinguishes Denied from
     *   PermanentlyDenied.
     */
    fun onPermissionResult(granted: Boolean, showRationale: Boolean) {
        _uiState.value = when (permissionResultToState(granted, showRationale)) {
            PermissionState.Granted -> MainUiState.Ready
            PermissionState.Denied -> MainUiState.PermissionRequired
            PermissionState.PermanentlyDenied -> MainUiState.PermissionPermanentlyDenied
            PermissionState.Unknown -> MainUiState.Initialising
        }
    }

}
