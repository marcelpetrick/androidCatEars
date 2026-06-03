// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val DELTA = 0.001f

class EarTintTest {

    @Test
    fun `EarTint has six values starting with NATURAL`() {
        assertEquals(6, EarTint.entries.size)
        assertEquals(EarTint.NATURAL, EarTint.entries.first())
    }

    @Test
    fun `NATURAL has zero hue rotation`() {
        assertEquals(0f, EarTint.NATURAL.hueDegrees, DELTA)
    }

    @Test
    fun `hueRotationMatrix returns 20 elements`() {
        assertEquals(20, hueRotationMatrix(90f).size)
    }

    @Test
    fun `zero degrees produces the identity colour matrix`() {
        val m = hueRotationMatrix(0f)
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        identity.forEachIndexed { i, expected -> assertEquals(expected, m[i], DELTA) }
    }

    @Test
    fun `360 degrees is approximately identity`() {
        val m = hueRotationMatrix(360f)
        assertEquals(1f, m[0], DELTA)
        assertEquals(1f, m[6], DELTA)
        assertEquals(1f, m[12], DELTA)
    }

    @Test
    fun `non-zero rotation changes the diagonal`() {
        val m = hueRotationMatrix(120f)
        // At 120° the red channel is no longer pure 1.0.
        assertTrue(m[0] != 1f) { "Red diagonal should change under hue rotation" }
    }

    @Test
    fun `alpha row is preserved`() {
        val m = hueRotationMatrix(200f)
        assertEquals(1f, m[18], DELTA) // alpha-to-alpha
        assertEquals(0f, m[15], DELTA) // alpha unaffected by red
    }
}
