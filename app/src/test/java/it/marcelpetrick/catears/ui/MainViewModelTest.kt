// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainViewModelTest {

    private fun viewModel() = MainViewModel()

    @Test
    fun `initial state is Initialising`() = runTest {
        viewModel().uiState.test {
            assertEquals(MainUiState.Initialising, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `granted true transitions to Ready`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(MainUiState.Initialising, awaitItem())
            vm.onPermissionResult(granted = true, showRationale = false)
            assertEquals(MainUiState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `granted false with rationale transitions to PermissionRequired`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(MainUiState.Initialising, awaitItem())
            vm.onPermissionResult(granted = false, showRationale = true)
            assertEquals(MainUiState.PermissionRequired, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `granted false without rationale transitions to PermissionPermanentlyDenied`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(MainUiState.Initialising, awaitItem())
            vm.onPermissionResult(granted = false, showRationale = false)
            assertEquals(MainUiState.PermissionPermanentlyDenied, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `permission can transition from denied to granted`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(MainUiState.Initialising, awaitItem())
            vm.onPermissionResult(granted = false, showRationale = true)
            assertEquals(MainUiState.PermissionRequired, awaitItem())
            vm.onPermissionResult(granted = true, showRationale = false)
            assertEquals(MainUiState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
