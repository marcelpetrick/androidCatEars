// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.share

import android.net.Uri
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShareIntentBuilderTest {

    private val fakeUri: Uri = mockk(relaxed = true)

    @Test
    fun `buildShareConfig uses default JPEG mime type`() {
        val config = buildShareConfig(fakeUri)
        assertEquals("image/jpeg", config.mimeType)
    }

    @Test
    fun `buildShareConfig preserves the uri`() {
        val config = buildShareConfig(fakeUri)
        assertEquals(fakeUri, config.uri)
    }

    @Test
    fun `buildShareConfig accepts custom mime type`() {
        val config = buildShareConfig(fakeUri, mimeType = "image/png")
        assertEquals("image/png", config.mimeType)
    }

    @Test
    fun `ShareConfig equality holds for same values`() {
        val a = ShareConfig(fakeUri, "image/jpeg")
        val b = ShareConfig(fakeUri, "image/jpeg")
        assertEquals(a, b)
    }
}
