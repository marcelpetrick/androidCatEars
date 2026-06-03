// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import android.graphics.Bitmap
import it.marcelpetrick.catears.domain.LensSelector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CameraControllerSeamTest {

    /** Minimal fake to record calls and exercise default interface logic. */
    private class FakeSeam : CameraControllerSeam {
        val calls = mutableListOf<String>()
        var captureResult: Bitmap? = null

        override fun bindPreview(lens: LensSelector) {
            calls += "bind:$lens"
        }

        override fun unbind() {
            calls += "unbind"
        }

        override fun capturePhoto(onResult: (Bitmap?) -> Unit) {
            calls += "capture"
            onResult(captureResult)
        }
    }

    @Test
    fun `switchLens unbinds then rebinds with new lens`() {
        val fake = FakeSeam()
        fake.switchLens(LensSelector.Rear)
        assertEquals(listOf("unbind", "bind:Rear"), fake.calls)
    }

    @Test
    fun `switchLens front unbinds then binds front`() {
        val fake = FakeSeam()
        fake.switchLens(LensSelector.Front)
        assertEquals(listOf("unbind", "bind:Front"), fake.calls)
    }

    @Test
    fun `capturePhoto delivers null when no bitmap available`() {
        val fake = FakeSeam()
        // Use a sentinel non-null value that isn't a real Bitmap (Bitmap is not mockable in JVM tests)
        var resultWasNull = true
        fake.captureResult = null
        fake.capturePhoto { result -> resultWasNull = (result == null) }
        assertTrue(resultWasNull)
        assertEquals(listOf("capture"), fake.calls)
    }
}
