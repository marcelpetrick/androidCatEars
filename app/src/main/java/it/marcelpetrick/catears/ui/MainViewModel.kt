// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.domain.PermissionState
import it.marcelpetrick.catears.domain.permissionResultToState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

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

    /**
     * Called by the UI once the Android permission result is known.
     *
     * @param granted Whether the CAMERA permission was granted.
     * @param showRationale Whether [ActivityCompat.shouldShowRequestPermissionRationale] returned
     *   true — used to distinguish Denied from PermanentlyDenied.
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
