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
import it.marcelpetrick.catears.domain.EarAdjustment
import it.marcelpetrick.catears.domain.EarStyle
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.domain.PermissionState
import it.marcelpetrick.catears.domain.RecordingState
import it.marcelpetrick.catears.domain.permissionResultToState
import it.marcelpetrick.catears.settings.EarAdjustmentStoreSeam
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val imageSaver: ImageSaver,
    private val captureRuntime: CaptureRuntime,
    private val earAdjustmentStore: EarAdjustmentStoreSeam,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Initialising)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _lens = MutableStateFlow(LensSelector.Front)
    val lens: StateFlow<LensSelector> = _lens.asStateFlow()

    fun onToggleLens() {
        if (_recordingState.value is RecordingState.Recording) return
        _lens.value = _lens.value.toggled()
    }

    private val _overlayPlacements = MutableStateFlow<List<OverlayPlacement>>(emptyList())
    val overlayPlacements: StateFlow<List<OverlayPlacement>> = _overlayPlacements.asStateFlow()

    private val _earStyle = MutableStateFlow(EarStyle.CLASSIC)
    val earStyle: StateFlow<EarStyle> = _earStyle.asStateFlow()

    private val _partyModeEnabled = MutableStateFlow(false)
    val partyModeEnabled: StateFlow<Boolean> = _partyModeEnabled.asStateFlow()

    private val partyFaceSlots = LinkedHashMap<Int, Int>()
    private var nextPartySlot = 0
    private var partyRerollGeneration = 0

    /** Advances to the next [EarStyle] in cycle order, wrapping from last back to first. */
    fun onCycleEarStyle() {
        val styles = EarStyle.entries
        _earStyle.value = styles[(_earStyle.value.ordinal + 1) % styles.size]
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
        if (activeIds.isEmpty()) nextPartySlot = 0
        _overlayPlacements.value = withAppearance(placements)
    }

    private fun withAppearance(placements: List<OverlayPlacement>): List<OverlayPlacement> =
        if (_partyModeEnabled.value) {
            placements.mapIndexed { index, placement ->
                val slot = placement.trackingId?.let { id ->
                    partyFaceSlots.getOrPut(id) { nextPartySlot++ }
                } ?: index
                placement.copy(earStyle = partyAppearance(slot))
            }
        } else {
            placements.map { it.copy(earStyle = _earStyle.value) }
        }

    private fun partyAppearance(slot: Int): EarStyle {
        val styles = PARTY_STYLE_ORDER
        val styleIndex = shuffledIndex(slot, partyRerollGeneration, styles.size, STYLE_SEED)
        return styles[styleIndex]
    }

    private val _earAdjustment = MutableStateFlow(EarAdjustment.DEFAULT)

    /** Current ear fit; applied live by the camera layer on every placement computation. */
    val earAdjustment: StateFlow<EarAdjustment> = _earAdjustment.asStateFlow()

    private val _fitPanelVisible = MutableStateFlow(false)
    val fitPanelVisible: StateFlow<Boolean> = _fitPanelVisible.asStateFlow()

    private val _fitStatusMessage = MutableStateFlow<String?>(null)

    /** Transient banner message for fit changes; cleared via [onFitStatusShown]. */
    val fitStatusMessage: StateFlow<String?> = _fitStatusMessage.asStateFlow()

    private var adjustmentAtPanelOpen = EarAdjustment.DEFAULT

    init {
        // Load once: this app is the only writer, and continuous collection could echo a
        // just-persisted value over a newer in-flight slider position.
        persist { _earAdjustment.value = earAdjustmentStore.adjustment.first() }
    }

    // UNDISPATCHED so a store that completes without suspending (as in tests) finishes
    // synchronously; a real DataStore write suspends and continues on the scope dispatcher.
    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { block() }
    }

    /** Opens/closes the fit panel; closing persists the adjustment and confirms a change. */
    fun onToggleFitPanel() {
        if (_fitPanelVisible.value) closeFitPanel() else openFitPanel()
    }

    private fun openFitPanel() {
        adjustmentAtPanelOpen = _earAdjustment.value
        _fitPanelVisible.value = true
    }

    private fun closeFitPanel() {
        _fitPanelVisible.value = false
        val current = _earAdjustment.value
        if (current != adjustmentAtPanelOpen && current.isCustom) {
            _fitStatusMessage.value = "Custom ear fit applied"
        }
        persist { earAdjustmentStore.save(current) }
    }

    /** Live slider movement: updates the fit immediately without touching persistence. */
    fun onAdjustmentChanged(adjustment: EarAdjustment) {
        _earAdjustment.value = adjustment.coerced()
    }

    /** Slider released: persists the current fit. */
    fun onAdjustmentCommitted() {
        persist { earAdjustmentStore.save(_earAdjustment.value) }
    }

    /** Returns to the auto-fit default and clears the stored adjustment. */
    fun onAdjustmentReset() {
        _earAdjustment.value = EarAdjustment.DEFAULT
        adjustmentAtPanelOpen = EarAdjustment.DEFAULT
        _fitStatusMessage.value = "Ear fit reset to auto"
        persist { earAdjustmentStore.reset() }
    }

    /** The transient fit banner has been displayed; clear it. */
    fun onFitStatusShown() {
        _fitStatusMessage.value = null
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
                try {
                    imageSaver.save(
                        bitmap = bitmap,
                        epochMillis = captureRuntime.nowMillis(),
                        randomSuffix = captureRuntime.randomSuffix(),
                    )
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
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

    /** Starts a recording if currently Idle or Failed; ignored if already recording or saving. */
    fun onRecordTap() {
        val state = _recordingState.value
        if (state is RecordingState.Idle || state is RecordingState.Failed) {
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

    companion object {
        private val PARTY_STYLE_ORDER = listOf(
            EarStyle.CLASSIC,
            EarStyle.FOX,
            EarStyle.LYNX_TUFTED,
            EarStyle.DENSE_FLUFFY,
            EarStyle.SHARP_FELINE,
            EarStyle.ROUNDED_FELINE,
        )
        private const val STYLE_SEED = 3

        private fun shuffledIndex(slot: Int, generation: Int, size: Int, seed: Int): Int = if (generation == 0) {
            slot % size
        } else {
            Math.floorMod(slot * seed + generation * (seed + 2), size)
        }
    }
}
