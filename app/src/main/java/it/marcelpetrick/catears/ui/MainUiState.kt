// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

/**
 * Full UI state for the main screen.
 *
 * Transitions:
 *   Initialising → PermissionRequired | PermissionPermanentlyDenied | Ready
 *   PermissionRequired → Ready | PermissionPermanentlyDenied
 */
sealed interface MainUiState {
    /** App is starting up — show nothing or a splash. */
    data object Initialising : MainUiState

    /** Camera permission not granted; can request it. */
    data object PermissionRequired : MainUiState

    /**
     * Camera permission denied with "Don't ask again"; cannot request again.
     * Must guide the user to app settings.
     */
    data object PermissionPermanentlyDenied : MainUiState

    /** Camera permission granted; ready to show the preview. */
    data object Ready : MainUiState
}
