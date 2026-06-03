// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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

    /** Called by the UI once the Android permission result is known. */
    fun onPermissionResult(granted: Boolean) {
        _uiState.value = when (permissionResultToState(granted)) {
            PermissionState.Granted -> MainUiState.Ready
            PermissionState.Denied -> MainUiState.PermissionRequired
            PermissionState.Unknown -> MainUiState.Initialising
        }
    }
}
