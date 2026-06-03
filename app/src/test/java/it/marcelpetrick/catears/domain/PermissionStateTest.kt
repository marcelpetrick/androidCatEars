// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PermissionStateTest {

    @Test
    fun `permissionResultToState true returns Granted`() {
        assertEquals(PermissionState.Granted, permissionResultToState(true))
    }

    @Test
    fun `permissionResultToState false returns Denied`() {
        assertEquals(PermissionState.Denied, permissionResultToState(false))
    }

    @Test
    fun `Unknown is the initial unchecked state`() {
        // PermissionState.Unknown represents "not yet checked" — it is the
        // initial state before any permission request has been issued.
        val state: PermissionState = PermissionState.Unknown
        assertEquals(PermissionState.Unknown, state)
    }
}
