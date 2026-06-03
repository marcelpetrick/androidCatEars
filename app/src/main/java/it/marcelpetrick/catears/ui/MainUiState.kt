// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

/**
 * Full UI state for the main screen.
 *
 * Initially the screen is in [Initialising]; once the permission check
 * has resolved it transitions to [PermissionRequired] or [Ready].
 */
sealed interface MainUiState {
    /** App is starting up — show nothing or a splash. */
    data object Initialising : MainUiState

    /** Camera permission not yet granted. */
    data object PermissionRequired : MainUiState

    /** Camera permission granted; ready to show the preview. */
    data object Ready : MainUiState
}
