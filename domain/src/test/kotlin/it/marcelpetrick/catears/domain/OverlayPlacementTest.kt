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
    fun `fallback —ear bases attach to head top`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        assertTrue(p.leftEar.y + p.leftEar.size >= box.top) { "Left ear base should touch the head top" }
        assertTrue(p.rightEar.y + p.rightEar.size >= box.top) { "Right ear base should touch the head top" }
    }

    @Test
    fun `fallback —ear size scales with face box width`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f, widthRatio = 0.42f)
        val expected = box.width * 0.42f
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
    fun `expression probabilities default to neutral`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        assertEquals(0f, p.smilingProbability, DELTA)
        assertEquals(1f, p.eyeOpennessMean, DELTA)
        assertEquals(1f, p.leftEyeOpenness, DELTA)
        assertEquals(1f, p.rightEyeOpenness, DELTA)
    }

    @Test
    fun `expression probabilities are stored in placement with per-eye openness`() {
        val p = computeOverlayPlacement(
            viewBox = box,
            headEulerAngleZ = 0f,
            smilingProbability = 0.9f,
            eyeOpennessMean = 0.1f,
            leftEyeOpenness = 0.2f,
            rightEyeOpenness = 0.8f,
        )
        assertEquals(0.9f, p.smilingProbability, DELTA)
        assertEquals(0.1f, p.eyeOpennessMean, DELTA)
        assertEquals(0.2f, p.leftEyeOpenness, DELTA)
        assertEquals(0.8f, p.rightEyeOpenness, DELTA)
    }

    @Test
    fun `per-eye openness falls back to mean for older callers`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f, eyeOpennessMean = 0.3f)
        assertEquals(0.3f, p.leftEyeOpenness, DELTA)
        assertEquals(0.3f, p.rightEyeOpenness, DELTA)
    }

    @Test
    fun `smoother interpolates expression probabilities`() {
        val smoother = PlacementSmoother(alpha = 0.5f)
        val first = makePlacement().copy(
            smilingProbability = 0f,
            eyeOpennessMean = 1f,
            leftEyeOpenness = 1f,
            rightEyeOpenness = 0f,
        )
        val second = makePlacement().copy(
            smilingProbability = 1f,
            eyeOpennessMean = 0f,
            leftEyeOpenness = 0f,
            rightEyeOpenness = 1f,
        )
        smoother.smooth(first)
        val result = smoother.smooth(second)
        assertEquals(0.5f, result.smilingProbability, DELTA)
        assertEquals(0.5f, result.eyeOpennessMean, DELTA)
        assertEquals(0.5f, result.leftEyeOpenness, DELTA)
        assertEquals(0.5f, result.rightEyeOpenness, DELTA)
    }

    @Test
    fun `smoother preserves next tracking id and appearance`() {
        val smoother = PlacementSmoother(alpha = 0.5f)
        val first = makePlacement().copy(trackingId = 1, earStyle = EarStyle.CLASSIC, tint = EarTint.NATURAL)
        val second = makePlacement().copy(trackingId = 2, earStyle = EarStyle.FOX, tint = EarTint.NATURAL)
        smoother.smooth(first)
        val result = smoother.smooth(second)
        assertEquals(2, result.trackingId)
        assertEquals(EarStyle.FOX, result.earStyle)
        assertEquals(EarTint.NATURAL, result.tint)
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

    // ---- computeOverlayPlacement — production experiment path ----

    @Test
    fun `left ear uses experiment spacing from face center`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        assertEquals(168.9998f, p.leftEar.x, DELTA)
    }

    @Test
    fun `right ear uses experiment spacing from face center`() {
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        assertEquals(231.0002f, p.rightEar.x, DELTA)
    }

    @Test
    fun `cat ear vertical placement attaches to top of head fallback`() {
        val earSize = box.width * 0.42f
        val expectedTopY = box.top + box.height * 0.065f - earSize
        val p = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        assertEquals(expectedTopY, p.leftEar.y, DELTA)
        assertEquals(expectedTopY, p.rightEar.y, DELTA)
        assertTrue(p.leftEar.y + earSize >= box.top) { "Cat ear base should start at the head" }
        assertTrue(p.rightEar.y + earSize >= box.top) { "Cat ear base should start at the head" }
        assertEquals(box.top + box.height * 0.065f, p.leftEar.y + p.leftEar.size, DELTA)
        assertEquals(box.top + box.height * 0.065f, p.rightEar.y + p.rightEar.size, DELTA)
    }

    @Test
    fun `eye anchors lower forehead attachment when face box top is too high`() {
        val p = computeOverlayPlacement(
            viewBox = box,
            headEulerAngleZ = 0f,
            leftEyeAnchor = Point2D(180f, 300f),
            rightEyeAnchor = Point2D(220f, 300f),
        )
        assertEquals(box.top + box.height * 0.24f, p.leftEar.y + p.leftEar.size, DELTA)
        assertEquals(box.top + box.height * 0.24f, p.rightEar.y + p.rightEar.size, DELTA)
    }

    @Test
    fun `eye anchors never raise ears above experiment fallback`() {
        val p = computeOverlayPlacement(
            viewBox = box,
            headEulerAngleZ = 0f,
            leftEyeAnchor = Point2D(180f, 260f),
            rightEyeAnchor = Point2D(220f, 260f),
        )
        assertEquals(box.top + box.height * 0.065f, p.leftEar.y + p.leftEar.size, DELTA)
        assertEquals(box.top + box.height * 0.065f, p.rightEar.y + p.rightEar.size, DELTA)
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

    // ---- MultiFaceSmoother ----

    @Test
    fun `MultiFaceSmoother first entry returned as-is`() {
        val smoother = MultiFaceSmoother()
        val p = makePlacement()
        val result = smoother.smooth(listOf(Pair(1, p)))
        assertEquals(p, result.first())
    }

    @Test
    fun `MultiFaceSmoother smooths second value`() {
        val smoother = MultiFaceSmoother()
        val p1 = makePlacement(lx = 0f, ly = 0f, rx = 0f, ry = 0f, size = 100f)
        val p2 = makePlacement(lx = 100f, ly = 100f, rx = 100f, ry = 100f, size = 200f)
        smoother.smooth(listOf(Pair(1, p1)))
        val result = smoother.smooth(listOf(Pair(1, p2)))
        // 0.3 alpha: lerp(0, 100, 0.3) = 30
        assertTrue(result.first().leftEar.x > 0f && result.first().leftEar.x < 100f)
    }

    @Test
    fun `MultiFaceSmoother discards stale face smoothers`() {
        val smoother = MultiFaceSmoother()
        smoother.smooth(listOf(Pair(1, makePlacement()), Pair(2, makePlacement())))
        // Only face 1 present in next frame
        val result = smoother.smooth(listOf(Pair(1, makePlacement(lx = 50f))))
        assertEquals(1, result.size)
    }

    @Test
    fun `MultiFaceSmoother reset clears all smoothers`() {
        val smoother = MultiFaceSmoother()
        smoother.smooth(listOf(Pair(1, makePlacement(lx = 0f))))
        smoother.reset()
        val p = makePlacement(lx = 100f)
        val result = smoother.smooth(listOf(Pair(1, p)))
        assertEquals(p, result.first())
    }

    @Test
    fun `MultiFaceSmoother handles null tracking id`() {
        val smoother = MultiFaceSmoother()
        val p = makePlacement()
        val result = smoother.smooth(listOf(Pair(null, p)))
        assertEquals(p, result.first())
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
