// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.overlay

import it.marcelpetrick.catears.R
import it.marcelpetrick.catears.domain.EarStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EarSpriteResourcesTest {

    @Test
    fun `classic style resolves to classic sprite drawable`() {
        assertEquals(R.drawable.ear_classic, earSpriteDrawableId(EarStyle.CLASSIC))
    }

    @Test
    fun `lynx tufted style resolves to lynx sprite drawable`() {
        assertEquals(R.drawable.ear_lynx_tufted, earSpriteDrawableId(EarStyle.LYNX_TUFTED))
    }

    @Test
    fun `styles without extracted sprites stay procedural in app layer`() {
        val spriteStyles = setOf(EarStyle.CLASSIC, EarStyle.LYNX_TUFTED)
        EarStyle.entries
            .filterNot { it in spriteStyles }
            .forEach { style ->
                assertNull(earSpriteDrawableId(style), "$style should not resolve to a sprite yet")
            }
    }
}
