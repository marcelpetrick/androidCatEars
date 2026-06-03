// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EarStyleTest {

    @Test
    fun `EarStyle has exactly five values`() {
        assertEquals(5, EarStyle.entries.size)
    }

    @Test
    fun `CLASSIC is the first entry`() {
        assertEquals(EarStyle.CLASSIC, EarStyle.entries.first())
    }

    @Test
    fun `OverlayPlacement defaults to CLASSIC style`() {
        val placement = OverlayPlacement(
            leftEar = EarAnchor(x = 0f, y = 0f, size = 60f, tiltDegrees = 0f),
            rightEar = EarAnchor(x = 100f, y = 0f, size = 60f, tiltDegrees = 0f),
        )
        assertEquals(EarStyle.CLASSIC, placement.earStyle)
    }

    @Test
    fun `OverlayPlacement accepts explicit EarStyle`() {
        val placement = OverlayPlacement(
            leftEar = EarAnchor(x = 0f, y = 0f, size = 60f, tiltDegrees = 0f),
            rightEar = EarAnchor(x = 100f, y = 0f, size = 60f, tiltDegrees = 0f),
            earStyle = EarStyle.LYNX_TUFTED,
        )
        assertEquals(EarStyle.LYNX_TUFTED, placement.earStyle)
    }

    @Test
    fun `cycling through all styles wraps to CLASSIC`() {
        val styles = EarStyle.entries
        val cycled = styles.map { styles[(it.ordinal + 1) % styles.size] }
        assertEquals(EarStyle.SHARP_FELINE, cycled[EarStyle.CLASSIC.ordinal])
        assertEquals(EarStyle.CLASSIC, cycled[EarStyle.CANINE_PERKY.ordinal])
    }
}
