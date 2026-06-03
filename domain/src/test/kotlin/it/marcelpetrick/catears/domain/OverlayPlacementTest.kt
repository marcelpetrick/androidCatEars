// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val DELTA = 0.01f

class OverlayPlacementTest {

    // 100×100 face box centred at (200, 300).
    private val box = BoundingBox(left = 150f, top = 250f, right = 250f, bottom = 350f)

    // Ear landmarks at the sides of the box, at mid-height.
    private val leftEar = Point2D(box.left, box.centerY)
    private val rightEar = Point2D(box.right, box.centerY)

    // ---- computeOverlayPlacement — fallback (no ear landmarks) ----

    @Test
    fun `fallback —ears are symmetric about face box centre`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        val midX = (p.leftEar.x + p.rightEar.x) / 2f
        assertEquals(box.centerX, midX, DELTA)
    }

    @Test
    fun `fallback —ears are placed above the bounding box`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        assertTrue(p.leftEar.y < box.top) { "Left ear top should be above box top" }
        assertTrue(p.rightEar.y < box.top) { "Right ear top should be above box top" }
    }

    @Test
    fun `fallback —ear size scales with face box width`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f, widthRatio = 0.65f)
        val expected = box.width * 0.65f
        assertEquals(expected, p.leftEar.size, DELTA)
        assertEquals(expected, p.rightEar.size, DELTA)
    }

    @Test
    fun `fallback —tilt matches head euler angle Z`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 15f)
        assertEquals(15f, p.leftEar.tiltDegrees, DELTA)
        assertEquals(15f, p.rightEar.tiltDegrees, DELTA)
    }

    @Test
    fun `fallback —zero rotation produces zero tilt`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        assertEquals(0f, p.leftEar.tiltDegrees, DELTA)
        assertEquals(0f, p.rightEar.tiltDegrees, DELTA)
    }

    @Test
    fun `headEulerAngleY stored in placement`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f, headEulerAngleY = 20f)
        assertEquals(20f, p.headEulerAngleY, DELTA)
    }

    @Test
    fun `zero yaw produces symmetric xScale of 1 on both ears`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f, headEulerAngleY = 0f)
        assertEquals(1f, p.leftEar.xScale, DELTA)
        assertEquals(1f, p.rightEar.xScale, DELTA)
    }

    @Test
    fun `positive yaw makes right ear wider and left ear narrower`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f, headEulerAngleY = 30f)
        assertTrue(p.rightEar.xScale > 1f) { "Near ear should be wider than 1" }
        assertTrue(p.leftEar.xScale < 1f) { "Far ear should be narrower than 1" }
    }

    @Test
    fun `xScale is symmetric for equal and opposite yaw`() {
        val right = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f, headEulerAngleY = 30f)
        val left = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f, headEulerAngleY = -30f)
        assertEquals(right.rightEar.xScale, left.leftEar.xScale, DELTA)
        assertEquals(right.leftEar.xScale, left.rightEar.xScale, DELTA)
    }

    @Test
    fun `xScale is clamped and never below minimum`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f, headEulerAngleY = 90f)
        assertTrue(p.leftEar.xScale >= 0.4f) { "xScale must not go below 0.4" }
        assertTrue(p.rightEar.xScale <= 1.6f) { "xScale must not exceed 1.6" }
    }

    // ---- computeOverlayPlacement — ear-landmark anchor path ----

    @Test
    fun `ear-anchor —left ear x is left landmark x`() {
        val p = computeOverlayPlacement(
            viewBox = box,
            headEulerAngleZ = 0f,
            leftEarAnchor = leftEar,
            rightEarAnchor = rightEar,
        )
        assertEquals(leftEar.x, p.leftEar.x, DELTA)
    }

    @Test
    fun `ear-anchor —right ear x is right landmark x`() {
        val p = computeOverlayPlacement(
            viewBox = box,
            headEulerAngleZ = 0f,
            leftEarAnchor = leftEar,
            rightEarAnchor = rightEar,
        )
        assertEquals(rightEar.x, p.rightEar.x, DELTA)
    }

    @Test
    fun `ear-anchor —cat ear top is above the human ear attachment point`() {
        val earSize = box.width * 0.65f
        val p = computeOverlayPlacement(
            viewBox = box,
            headEulerAngleZ = 0f,
            leftEarAnchor = leftEar,
            rightEarAnchor = rightEar,
        )
        assertEquals(leftEar.y - earSize, p.leftEar.y, DELTA)
        assertEquals(rightEar.y - earSize, p.rightEar.y, DELTA)
    }

    @Test
    fun `partial anchors fall back to bounding-box path`() {
        val withLeftOnly = computeOverlayPlacement(
            viewBox = box,
            headEulerAngleZ = 0f,
            leftEarAnchor = leftEar,
            rightEarAnchor = null,
        )
        val noAnchors = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        val midWithLeft = (withLeftOnly.leftEar.x + withLeftOnly.rightEar.x) / 2f
        val midNoAnchor = (noAnchors.leftEar.x + noAnchors.rightEar.x) / 2f
        assertEquals(midNoAnchor, midWithLeft, DELTA)
    }

    // ---- PlacementSmoother ----

    @Test
    fun `first value returned as-is (no history)`() {
        val smoother = PlacementSmoother(alpha = 0.5f)
        val input = makePlacement(lx = 100f, ly = 50f, rx = 200f, ry = 50f)
        val result = smoother.smooth(input)
        assertEquals(input, result)
    }

    @Test
    fun `second value is interpolated towards input`() {
        val smoother = PlacementSmoother(alpha = 0.5f)
        val first = makePlacement(lx = 0f, ly = 0f, rx = 100f, ry = 0f, size = 100f)
        val second = makePlacement(lx = 100f, ly = 100f, rx = 200f, ry = 100f, size = 200f)
        smoother.smooth(first)
        val result = smoother.smooth(second)
        // lerp(0, 100, 0.5) = 50
        assertEquals(50f, result.leftEar.x, DELTA)
        assertEquals(50f, result.leftEar.y, DELTA)
        assertEquals(150f, result.rightEar.x, DELTA)
        assertEquals(150f, result.rightEar.size, DELTA)
    }

    @Test
    fun `reset clears history so next value is returned as-is`() {
        val smoother = PlacementSmoother(alpha = 0.5f)
        smoother.smooth(makePlacement(lx = 0f, ly = 0f, rx = 100f, ry = 0f))
        smoother.reset()
        val second = makePlacement(lx = 100f, ly = 100f, rx = 200f, ry = 100f)
        val result = smoother.smooth(second)
        assertEquals(second, result)
    }

    @Test
    fun `smoother angle lerp takes shortest arc across zero`() {
        val smoother = PlacementSmoother(alpha = 0.5f)
        smoother.smooth(makePlacement(tilt = -170f))
        val result = smoother.smooth(makePlacement(tilt = 170f))
        assertTrue(result.leftEar.tiltDegrees >= -180f && result.leftEar.tiltDegrees <= 180f)
    }

    // ---- helpers ----

    private fun makePlacement(
        lx: Float = 100f,
        ly: Float = 50f,
        rx: Float = 200f,
        ry: Float = 50f,
        size: Float = 80f,
        tilt: Float = 0f,
    ) = OverlayPlacement(
        leftEar = EarAnchor(x = lx, y = ly, size = size, tiltDegrees = tilt),
        rightEar = EarAnchor(x = rx, y = ry, size = size, tiltDegrees = tilt),
    )
}
