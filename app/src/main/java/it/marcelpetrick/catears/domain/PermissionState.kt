// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/** Models whether the camera permission has been granted. Pure domain — no Android deps. */
sealed interface PermissionState {
    data object Unknown : PermissionState
    data object Granted : PermissionState
    data object Denied : PermissionState
}

/** Maps an Android permission result to our domain model. */
fun permissionResultToState(granted: Boolean): PermissionState =
    if (granted) PermissionState.Granted else PermissionState.Denied
