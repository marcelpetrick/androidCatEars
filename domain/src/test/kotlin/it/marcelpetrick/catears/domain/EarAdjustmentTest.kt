// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val DELTA = 0.0001f

class EarAdjustmentTest {

    // ---- isCustom ----

    @Test
    fun `default adjustment is not custom`() {
        assertFalse(EarAdjustment.DEFAULT.isCustom)
        assertFalse(EarAdjustment().isCustom)
    }

    @Test
    fun `vertical offset alone makes the adjustment custom`() {
        assertTrue(EarAdjustment(verticalOffset = 0.1f).isCustom)
    }

    @Test
    fun `spread offset alone makes the adjustment custom`() {
        assertTrue(EarAdjustment(spreadOffset = -0.2f).isCustom)
    }

    // ---- coerced ----

    @Test
    fun `coerced keeps in-range values unchanged`() {
        val adjustment = EarAdjustment(verticalOffset = 0.1f, spreadOffset = -0.3f)
        assertEquals(adjustment, adjustment.coerced())
    }

    @Test
    fun `coerced clamps vertical offset to both bounds`() {
        assertEquals(
            EarAdjustment.MAX_VERTICAL_OFFSET,
            EarAdjustment(verticalOffset = 5f).coerced().verticalOffset,
            DELTA,
        )
        assertEquals(
            -EarAdjustment.MAX_VERTICAL_OFFSET,
            EarAdjustment(verticalOffset = -5f).coerced().verticalOffset,
            DELTA,
        )
    }

    @Test
    fun `coerced clamps spread offset to both bounds`() {
        assertEquals(
            EarAdjustment.MAX_SPREAD_OFFSET,
            EarAdjustment(spreadOffset = 5f).coerced().spreadOffset,
            DELTA,
        )
        assertEquals(
            -EarAdjustment.MAX_SPREAD_OFFSET,
            EarAdjustment(spreadOffset = -5f).coerced().spreadOffset,
            DELTA,
        )
    }

    @Test
    fun `coerced clamps both axes independently`() {
        val coerced = EarAdjustment(verticalOffset = -1f, spreadOffset = 1f).coerced()
        assertEquals(-EarAdjustment.MAX_VERTICAL_OFFSET, coerced.verticalOffset, DELTA)
        assertEquals(EarAdjustment.MAX_SPREAD_OFFSET, coerced.spreadOffset, DELTA)
    }
}
