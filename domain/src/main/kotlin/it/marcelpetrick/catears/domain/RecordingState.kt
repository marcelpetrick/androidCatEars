// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

sealed class RecordingState {
    data object Idle : RecordingState()
    data object Recording : RecordingState()
    data class Saved(val uriString: String) : RecordingState()
    data object Failed : RecordingState()
}
