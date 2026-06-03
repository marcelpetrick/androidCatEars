// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.capture

import it.marcelpetrick.catears.capture.OverlayCompositor.DrawTransform
import it.marcelpetrick.catears.capture.OverlayCompositor.computeDrawTransform
import it.marcelpetrick.catears.domain.OverlayPlacement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs

private const val DELTA = 0.01f

class OverlayCompositorTest {

    // Face bounding box: 120 px wide, centred at x=200, top at y=50
    private val placement = OverlayPlacement(centerX = 200f, topY = 50f, width = 120f, rotationDegrees = 0f)

    // ---- computeDrawTransform (pure Kotlin, no Android classes) ----

    private fun transform(overlayW: Int = 120, overlayH: Int = 60, p: OverlayPlacement = placement): DrawTransform =
        computeDrawTransform(p, overlayW, overlayH)

    @Test
    fun `scaleX fits overlay width to placement width`() {
        // overlayWidth=120, placement.width=120 → scaleX=1.0
        assertEquals(1.0f, transform(overlayW = 120).scaleX, DELTA)
    }

    @Test
    fun `scaleX scales correctly for different asset width`() {
        // overlayWidth=200, placement.width=120 → scaleX=0.6
        assertEquals(0.6f, transform(overlayW = 200).scaleX, DELTA)
    }

    @Test
    fun `scaleY derived from overlay aspect ratio`() {
        // renderedHeight = 120 * 0.5 = 60; overlayHeight=60 → scaleY=1.0
        assertEquals(1.0f, transform(overlayH = 60).scaleY, DELTA)
    }

    @Test
    fun `translateX positions left edge correctly`() {
        // centerX=200, width=120 → left=140
        assertEquals(140f, transform().translateX, DELTA)
    }

    @Test
    fun `translateY is placement topY`() {
        assertEquals(50f, transform().translateY, DELTA)
    }

    @Test
    fun `rotateDegrees matches placement headEulerAngle`() {
        val rotated = placement.copy(rotationDegrees = 15f)
        assertEquals(15f, transform(p = rotated).rotateDegrees, DELTA)
    }

    @Test
    fun `pivot is at centre of rendered overlay`() {
        val t = transform()
        assertEquals(60f, t.rotatePivotX, DELTA) // width/2
        assertEquals(30f, t.rotatePivotY, DELTA) // renderedHeight/2 = (120*0.5)/2
    }

    @Test
    fun `zero rotation produces zero skew in transform`() {
        val t = transform(p = placement.copy(rotationDegrees = 0f))
        // With zero rotation the rotate pivot is irrelevant but rotateDegrees must be 0
        assert(abs(t.rotateDegrees) < DELTA) { "Expected 0 rotation, got ${t.rotateDegrees}" }
    }
}
