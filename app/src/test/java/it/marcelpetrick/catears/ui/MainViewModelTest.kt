// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import android.graphics.Bitmap
import android.net.Uri
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import it.marcelpetrick.catears.capture.ImageSaver
import it.marcelpetrick.catears.domain.CaptureState
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.domain.OverlayPlacement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MainViewModelTest {

    private fun viewModel(
        imageSaver: ImageSaver = mockk<ImageSaver>(relaxed = true),
        captureRuntime: CaptureRuntime = FakeCaptureRuntime(),
    ) = MainViewModel(imageSaver = imageSaver, captureRuntime = captureRuntime)

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

    @Test
    fun `initial lens is Front`() = runTest {
        viewModel().lens.test {
            assertEquals(LensSelector.Front, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onToggleLens switches Front to Rear`() = runTest {
        val vm = viewModel()
        vm.lens.test {
            assertEquals(LensSelector.Front, awaitItem())
            vm.onToggleLens()
            assertEquals(LensSelector.Rear, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial overlayPlacement is null`() = runTest {
        viewModel().overlayPlacement.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onFaceDetected updates overlayPlacement`() = runTest {
        val vm = viewModel()
        val placement = OverlayPlacement(centerX = 100f, topY = 50f, width = 200f, rotationDegrees = 0f)
        vm.overlayPlacement.test {
            assertNull(awaitItem())
            vm.onFaceDetected(placement)
            assertEquals(placement, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial captureState is Idle`() = runTest {
        viewModel().captureState.test {
            assertEquals(CaptureState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCaptureRequested transitions to Capturing`() = runTest {
        val vm = viewModel()
        vm.captureState.test {
            assertEquals(CaptureState.Idle, awaitItem())
            vm.onCaptureRequested()
            assertEquals(CaptureState.Capturing, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCompositedBitmap null transitions to Failed`() = runTest {
        val vm = viewModel()
        vm.captureState.test {
            assertEquals(CaptureState.Idle, awaitItem())
            vm.onCompositedBitmap(null)
            assertEquals(CaptureState.Failed, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCompositedBitmap saves with deterministic runtime values`() = runTest {
        val imageSaver = mockk<ImageSaver>()
        val bitmap = mockk<Bitmap>()
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://saved/photo"
        every {
            imageSaver.save(bitmap = bitmap, epochMillis = 1234L, randomSuffix = "beef")
        } returns uri

        val vm = viewModel(imageSaver = imageSaver)
        vm.captureState.test {
            assertEquals(CaptureState.Idle, awaitItem())
            vm.onCompositedBitmap(bitmap)
            assertEquals(CaptureState.Saved("content://saved/photo"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCaptureConsumed resets to Idle`() = runTest {
        val vm = viewModel()
        vm.captureState.test {
            assertEquals(CaptureState.Idle, awaitItem())
            vm.onCaptureRequested()
            assertEquals(CaptureState.Capturing, awaitItem())
            vm.onCaptureConsumed()
            assertEquals(CaptureState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onFaceDetected null clears placement`() = runTest {
        val vm = viewModel()
        val placement = OverlayPlacement(centerX = 100f, topY = 50f, width = 200f, rotationDegrees = 0f)
        vm.overlayPlacement.test {
            assertNull(awaitItem())
            vm.onFaceDetected(placement)
            assertEquals(placement, awaitItem())
            vm.onFaceDetected(null)
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onToggleLens twice returns to Front`() = runTest {
        val vm = viewModel()
        vm.lens.test {
            assertEquals(LensSelector.Front, awaitItem())
            vm.onToggleLens()
            assertEquals(LensSelector.Rear, awaitItem())
            vm.onToggleLens()
            assertEquals(LensSelector.Front, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeCaptureRuntime : CaptureRuntime {
        override val ioDispatcher: CoroutineDispatcher = Dispatchers.Unconfined

        override fun nowMillis(): Long = 1234L

        override fun randomSuffix(): String = "beef"
    }
}
