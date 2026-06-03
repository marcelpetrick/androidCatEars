// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PermissionStateTest {

    @Test
    fun `permissionResultToState granted true returns Granted`() {
        assertEquals(PermissionState.Granted, permissionResultToState(granted = true, showRationale = false))
    }

    @Test
    fun `permissionResultToState granted false with rationale returns Denied`() {
        assertEquals(PermissionState.Denied, permissionResultToState(granted = false, showRationale = true))
    }

    @Test
    fun `permissionResultToState granted false without rationale returns PermanentlyDenied`() {
        assertEquals(
            PermissionState.PermanentlyDenied,
            permissionResultToState(granted = false, showRationale = false),
        )
    }

    @Test
    fun `Unknown is the initial unchecked state`() {
        val state: PermissionState = PermissionState.Unknown
        assertEquals(PermissionState.Unknown, state)
    }
}
