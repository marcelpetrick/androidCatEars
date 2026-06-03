// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.camera

import it.marcelpetrick.catears.domain.LensSelector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CameraControllerSeamTest {

    /** Minimal fake to record calls and exercise default interface logic. */
    private class FakeSeam : CameraControllerSeam {
        val calls = mutableListOf<String>()

        override fun bindPreview(lens: LensSelector) {
            calls += "bind:$lens"
        }

        override fun unbind() {
            calls += "unbind"
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
    fun `close unbinds by default`() {
        val fake = FakeSeam()
        fake.close()
        assertEquals(listOf("unbind"), fake.calls)
    }
}
