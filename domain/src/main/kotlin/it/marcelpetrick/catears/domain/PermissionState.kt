// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/** Models whether the camera permission has been granted. Pure domain — no Android deps. */
sealed interface PermissionState {
    /** Not yet checked — the app has not issued a permission request. */
    data object Unknown : PermissionState

    /** User granted the CAMERA permission. */
    data object Granted : PermissionState

    /** User denied once — can still request again. */
    data object Denied : PermissionState

    /** User denied and selected "Don't ask again" — must open Settings. */
    data object PermanentlyDenied : PermissionState
}

/** Maps a raw granted flag to a [PermissionState]; caller supplies whether rationale should show. */
fun permissionResultToState(granted: Boolean, showRationale: Boolean): PermissionState = when {
    granted -> PermissionState.Granted
    showRationale -> PermissionState.Denied
    else -> PermissionState.PermanentlyDenied
}
