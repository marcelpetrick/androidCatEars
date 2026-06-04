// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.marcelpetrick.catears.capture.CaptureRuntime
import it.marcelpetrick.catears.capture.ImageSaver
import it.marcelpetrick.catears.domain.CaptureState
import it.marcelpetrick.catears.domain.EarStyle
import it.marcelpetrick.catears.domain.EarTint
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.domain.PermissionState
import it.marcelpetrick.catears.domain.RecordingState
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

    private val _overlayPlacements = MutableStateFlow<List<OverlayPlacement>>(emptyList())
    val overlayPlacements: StateFlow<List<OverlayPlacement>> = _overlayPlacements.asStateFlow()

    private val _earStyle = MutableStateFlow(EarStyle.CLASSIC)
    val earStyle: StateFlow<EarStyle> = _earStyle.asStateFlow()

    private val _earTint = MutableStateFlow(EarTint.NATURAL)
    val earTint: StateFlow<EarTint> = _earTint.asStateFlow()

    private val _partyModeEnabled = MutableStateFlow(false)
    val partyModeEnabled: StateFlow<Boolean> = _partyModeEnabled.asStateFlow()

    private val partyFaceSlots = LinkedHashMap<Int, Int>()
    private var partyRerollGeneration = 0

    /** Advances to the next [EarStyle] in cycle order, wrapping from last back to first. */
    fun onCycleEarStyle() {
        val styles = EarStyle.entries
        _earStyle.value = styles[(_earStyle.value.ordinal + 1) % styles.size]
        applyAppearance()
    }

    /** Advances to the next [EarTint] in cycle order, wrapping from last back to first. */
    fun onCycleEarTint() {
        val tints = EarTint.entries
        _earTint.value = tints[(_earTint.value.ordinal + 1) % tints.size]
        applyAppearance()
    }

    fun onTogglePartyMode() {
        _partyModeEnabled.value = !_partyModeEnabled.value
        applyAppearance()
    }

    fun onRerollPartyAssignments() {
        if (!_partyModeEnabled.value) return
        partyRerollGeneration += 1
        applyAppearance()
    }

    private fun applyAppearance() {
        _overlayPlacements.value = withAppearance(_overlayPlacements.value)
    }

    /** Called from the face-detection callback with all smoothed placements for the frame. */
    fun onFaceDetected(placements: List<OverlayPlacement>) {
        val activeIds = placements.mapNotNull { it.trackingId }.toSet()
        partyFaceSlots.keys.retainAll(activeIds)
        _overlayPlacements.value = withAppearance(placements)
    }

    private fun withAppearance(placements: List<OverlayPlacement>): List<OverlayPlacement> =
        if (_partyModeEnabled.value) {
            placements.mapIndexed { index, placement ->
                val slot = placement.trackingId?.let { id ->
                    partyFaceSlots.getOrPut(id) { partyFaceSlots.size }
                } ?: index
                val appearance = partyAppearance(slot)
                placement.copy(earStyle = appearance.style, tint = appearance.tint)
            }
        } else {
            placements.map { it.copy(earStyle = _earStyle.value, tint = _earTint.value) }
        }

    private fun partyAppearance(slot: Int): FaceAppearance {
        val styles = PARTY_STYLE_ORDER
        val tints = PARTY_TINT_ORDER
        val styleIndex = shuffledIndex(slot, partyRerollGeneration, styles.size, STYLE_SEED)
        val tintIndex = shuffledIndex(slot, partyRerollGeneration, tints.size, TINT_SEED)
        return FaceAppearance(style = styles[styleIndex], tint = tints[tintIndex])
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

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    /** Starts a recording if currently Idle; ignored if recording or in any other state. */
    fun onRecordTap() {
        if (_recordingState.value is RecordingState.Idle) {
            _recordingState.value = RecordingState.Recording
        }
    }

    /** Called by the camera layer when the recording finishes; null uri means failure. */
    fun onRecordingSaved(uriString: String?) {
        _recordingState.value = if (uriString != null) RecordingState.Saved(uriString) else RecordingState.Failed
    }

    /** Resets recording state to Idle after the clip has been shared. */
    fun onRecordingConsumed() {
        _recordingState.value = RecordingState.Idle
    }

    /** Camera hardware failed to bind; transitions to [MainUiState.CameraError]. */
    fun onCameraBindFailed() {
        _uiState.value = MainUiState.CameraError
    }

    /** User tapped retry after a camera error; re-enters [MainUiState.Ready]. */
    fun onRetryCamera() {
        _uiState.value = MainUiState.Ready
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

    private data class FaceAppearance(val style: EarStyle, val tint: EarTint)

    companion object {
        private val PARTY_STYLE_ORDER = listOf(
            EarStyle.CLASSIC,
            EarStyle.FOX,
            EarStyle.LYNX_TUFTED,
            EarStyle.RABBIT,
            EarStyle.DENSE_FLUFFY,
            EarStyle.SHARP_FELINE,
            EarStyle.CANINE_PERKY,
            EarStyle.ROUNDED_FELINE,
            EarStyle.BEAR,
            EarStyle.CANINE_FLOPPY,
        )
        private val PARTY_TINT_ORDER = listOf(
            EarTint.NATURAL,
            EarTint.SKY,
            EarTint.LAVENDER,
            EarTint.ROSE,
            EarTint.GOLD,
            EarTint.MINT,
        )
        private const val STYLE_SEED = 3
        private const val TINT_SEED = 5

        private fun shuffledIndex(slot: Int, generation: Int, size: Int, seed: Int): Int = if (generation == 0) {
            slot % size
        } else {
            Math.floorMod(slot * seed + generation * (seed + 2), size)
        }
    }
}
