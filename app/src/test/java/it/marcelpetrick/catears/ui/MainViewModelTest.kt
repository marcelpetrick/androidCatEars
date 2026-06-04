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
import it.marcelpetrick.catears.domain.EarAnchor
import it.marcelpetrick.catears.domain.EarStyle
import it.marcelpetrick.catears.domain.EarTint
import it.marcelpetrick.catears.domain.LensSelector
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.domain.RecordingState
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
    fun `initial overlayPlacements is empty`() = runTest {
        viewModel().overlayPlacements.test {
            assertEquals(emptyList<OverlayPlacement>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onFaceDetected updates overlayPlacements`() = runTest {
        val vm = viewModel()
        val placement = OverlayPlacement(
            leftEar = EarAnchor(x = 100f, y = 50f, size = 80f, tiltDegrees = 0f),
            rightEar = EarAnchor(x = 200f, y = 50f, size = 80f, tiltDegrees = 0f),
        )
        vm.overlayPlacements.test {
            assertEquals(emptyList<OverlayPlacement>(), awaitItem())
            vm.onFaceDetected(listOf(placement))
            assertEquals(listOf(placement), awaitItem())
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
    fun `onFaceDetected empty list clears placements`() = runTest {
        val vm = viewModel()
        val placement = OverlayPlacement(
            leftEar = EarAnchor(x = 100f, y = 50f, size = 80f, tiltDegrees = 0f),
            rightEar = EarAnchor(x = 200f, y = 50f, size = 80f, tiltDegrees = 0f),
        )
        vm.overlayPlacements.test {
            assertEquals(emptyList<OverlayPlacement>(), awaitItem())
            vm.onFaceDetected(listOf(placement))
            assertEquals(1, awaitItem().size)
            vm.onFaceDetected(emptyList())
            assertEquals(emptyList<OverlayPlacement>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial earStyle is CLASSIC`() = runTest {
        viewModel().earStyle.test {
            assertEquals(EarStyle.CLASSIC, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCycleEarStyle advances to SHARP_FELINE`() = runTest {
        val vm = viewModel()
        vm.earStyle.test {
            assertEquals(EarStyle.CLASSIC, awaitItem())
            vm.onCycleEarStyle()
            assertEquals(EarStyle.SHARP_FELINE, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCycleEarStyle wraps from last style back to CLASSIC`() = runTest {
        val vm = viewModel()
        vm.earStyle.test {
            awaitItem() // CLASSIC
            repeat(EarStyle.entries.size) { vm.onCycleEarStyle() }
            // After full cycle, back to CLASSIC
            val last = cancelAndConsumeRemainingEvents().lastOrNull()
            assertEquals(EarStyle.CLASSIC, (last as? app.cash.turbine.Event.Item)?.value)
        }
    }

    @Test
    fun `onFaceDetected injects current earStyle into placements`() = runTest {
        val vm = viewModel()
        vm.onCycleEarStyle() // advance to SHARP_FELINE
        val raw = OverlayPlacement(
            leftEar = EarAnchor(x = 100f, y = 50f, size = 80f, tiltDegrees = 0f),
            rightEar = EarAnchor(x = 200f, y = 50f, size = 80f, tiltDegrees = 0f),
        )
        vm.overlayPlacements.test {
            assertEquals(emptyList<OverlayPlacement>(), awaitItem())
            vm.onFaceDetected(listOf(raw))
            assertEquals(EarStyle.SHARP_FELINE, awaitItem().first().earStyle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCycleEarStyle updates active placements immediately`() = runTest {
        val vm = viewModel()
        val raw = OverlayPlacement(
            leftEar = EarAnchor(x = 100f, y = 50f, size = 80f, tiltDegrees = 0f),
            rightEar = EarAnchor(x = 200f, y = 50f, size = 80f, tiltDegrees = 0f),
        )
        vm.onFaceDetected(listOf(raw))
        vm.overlayPlacements.test {
            awaitItem() // current CLASSIC placements
            vm.onCycleEarStyle()
            assertEquals(EarStyle.SHARP_FELINE, awaitItem().first().earStyle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial earTint is NATURAL`() = runTest {
        viewModel().earTint.test {
            assertEquals(EarTint.NATURAL, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCycleEarTint advances to next tint`() = runTest {
        val vm = viewModel()
        vm.earTint.test {
            assertEquals(EarTint.NATURAL, awaitItem())
            vm.onCycleEarTint()
            assertEquals(EarTint.entries[1], awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCycleEarTint injects tint into active placements`() = runTest {
        val vm = viewModel()
        val raw = OverlayPlacement(
            leftEar = EarAnchor(x = 100f, y = 50f, size = 80f, tiltDegrees = 0f),
            rightEar = EarAnchor(x = 200f, y = 50f, size = 80f, tiltDegrees = 0f),
        )
        vm.onFaceDetected(listOf(raw))
        vm.overlayPlacements.test {
            awaitItem() // NATURAL
            vm.onCycleEarTint()
            assertEquals(EarTint.entries[1], awaitItem().first().tint)
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

    @Test
    fun `initial recordingState is Idle`() = runTest {
        viewModel().recordingState.test {
            assertEquals(RecordingState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRecordTap transitions Idle to Recording`() = runTest {
        val vm = viewModel()
        vm.recordingState.test {
            assertEquals(RecordingState.Idle, awaitItem())
            vm.onRecordTap()
            assertEquals(RecordingState.Recording, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRecordTap while Recording is ignored`() = runTest {
        val vm = viewModel()
        vm.onRecordTap()
        vm.recordingState.test {
            assertEquals(RecordingState.Recording, awaitItem())
            vm.onRecordTap()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRecordingSaved with uri transitions to Saved`() = runTest {
        val vm = viewModel()
        vm.onRecordTap()
        vm.recordingState.test {
            awaitItem() // Recording
            vm.onRecordingSaved("content://media/video/1")
            assertEquals(RecordingState.Saved("content://media/video/1"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRecordingSaved null transitions to Failed`() = runTest {
        val vm = viewModel()
        vm.onRecordTap()
        vm.recordingState.test {
            awaitItem() // Recording
            vm.onRecordingSaved(null)
            assertEquals(RecordingState.Failed, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRecordingConsumed resets to Idle from Saved`() = runTest {
        val vm = viewModel()
        vm.onRecordTap()
        vm.onRecordingSaved("content://media/video/1")
        vm.recordingState.test {
            awaitItem() // Saved
            vm.onRecordingConsumed()
            assertEquals(RecordingState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeCaptureRuntime : CaptureRuntime {
        override val ioDispatcher: CoroutineDispatcher = Dispatchers.Unconfined

        override fun nowMillis(): Long = 1234L

        override fun randomSuffix(): String = "beef"
    }
}
