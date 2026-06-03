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
    fun `onPermissionResult true transitions to Ready`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(MainUiState.Initialising, awaitItem())
            vm.onPermissionResult(granted = true)
            assertEquals(MainUiState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPermissionResult false transitions to PermissionRequired`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(MainUiState.Initialising, awaitItem())
            vm.onPermissionResult(granted = false)
            assertEquals(MainUiState.PermissionRequired, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `permission can transition from denied to granted`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(MainUiState.Initialising, awaitItem())
            vm.onPermissionResult(granted = false)
            assertEquals(MainUiState.PermissionRequired, awaitItem())
            vm.onPermissionResult(granted = true)
            assertEquals(MainUiState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
