// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.capture

import it.marcelpetrick.catears.capture.OverlayCompositor.computeEarGeometry
import it.marcelpetrick.catears.domain.EarAnchor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private const val DELTA = 0.5f

class OverlayCompositorTest {

    // Ear centred at x=200, top at y=50, size=100.
    private val anchor = EarAnchor(x = 200f, y = 50f, size = 100f, tiltDegrees = 0f)

    @Test
    fun `outer triangle apex is at ear anchor centre-top`() {
        val geo = computeEarGeometry(anchor)
        assertEquals(anchor.x, geo.outerPath[0], DELTA)
        assertEquals(anchor.y, geo.outerPath[1], DELTA)
    }

    @Test
    fun `outer triangle base is at anchor bottom`() {
        val geo = computeEarGeometry(anchor)
        val expectedBaseY = anchor.y + anchor.size
        assertEquals(expectedBaseY, geo.outerPath[3], DELTA) // left base y
        assertEquals(expectedBaseY, geo.outerPath[5], DELTA) // right base y
    }

    @Test
    fun `outer triangle is symmetric about anchor x`() {
        val geo = computeEarGeometry(anchor)
        val leftX = geo.outerPath[2]
        val rightX = geo.outerPath[4]
        // both sides equidistant from centre
        assertEquals(anchor.x - leftX, rightX - anchor.x, DELTA)
    }

    @Test
    fun `inner triangle apex is above its base`() {
        val geo = computeEarGeometry(anchor)
        val innerApexY = geo.innerPath[1]
        val innerBaseY = geo.innerPath[3]
        assert(innerApexY < innerBaseY) { "Inner ear apex should be above its base" }
    }

    @Test
    fun `inner triangle is narrower than outer triangle`() {
        val geo = computeEarGeometry(anchor)
        val outerWidth = geo.outerPath[4] - geo.outerPath[2]
        val innerWidth = geo.innerPath[4] - geo.innerPath[2]
        assert(innerWidth < outerWidth) { "Inner ear triangle should be narrower than outer" }
    }

    @Test
    fun `geometry scales proportionally with ear size`() {
        val small = computeEarGeometry(anchor.copy(size = 50f))
        val large = computeEarGeometry(anchor.copy(size = 100f))
        // base width of outer triangle should scale linearly with size
        val smallWidth = small.outerPath[4] - small.outerPath[2]
        val largeWidth = large.outerPath[4] - large.outerPath[2]
        assertEquals(2f, largeWidth / smallWidth, DELTA)
    }
}
