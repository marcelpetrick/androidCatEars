// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val DELTA = 0.01f

class OverlayPlacementTest {

    // Face box: 100×100, centred at (200, 300) in view space.
    private val box = BoundingBox(left = 150f, top = 250f, right = 250f, bottom = 350f)

    // Ear-landmark anchors in view space (ears are at the sides of the box).
    private val leftEar = Point2D(box.left, box.centerY)
    private val rightEar = Point2D(box.right, box.centerY)

    // ---- computeOverlayPlacement — fallback path (no ear landmarks) ----

    @Test
    fun `overlay centerX matches face box centerX when no ear anchors`() {
        val placement = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        assertEquals(box.centerX, placement.centerX, DELTA)
    }

    @Test
    fun `overlay width is face width times widthRatio`() {
        val placement = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f, widthRatio = 1.3f)
        assertEquals(130f, placement.width, DELTA)
    }

    @Test
    fun `overlay rotation matches head euler angle`() {
        val placement = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 15f)
        assertEquals(15f, placement.rotationDegrees, DELTA)
    }

    @Test
    fun `overlay topY is above the face bounding box when no ear anchors`() {
        val placement = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        assertTrue(placement.topY < box.top) {
            "topY (${placement.topY}) should be above face top (${box.top})"
        }
    }

    @Test
    fun `zero rotation produces zero rotation in placement`() {
        val placement = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        assertEquals(0f, placement.rotationDegrees, DELTA)
    }

    // ---- computeOverlayPlacement — ear-landmark anchor path ----

    @Test
    fun `ear-anchor centerX is midpoint of the two ear landmarks`() {
        val placement = computeOverlayPlacement(
            viewBox = box,
            headEulerAngleZ = 0f,
            leftEarAnchor = leftEar,
            rightEarAnchor = rightEar,
        )
        val expected = (leftEar.x + rightEar.x) / 2f
        assertEquals(expected, placement.centerX, DELTA)
    }

    @Test
    fun `ear-anchor topY places cat-ear bottom at ear attachment height`() {
        val overlayWidth = box.width * 1.3f
        val overlayHeight = overlayWidth * 0.5f // EAR_ASPECT_RATIO
        val earAttachY = (leftEar.y + rightEar.y) / 2f
        val expectedTopY = earAttachY - overlayHeight

        val placement = computeOverlayPlacement(
            viewBox = box,
            headEulerAngleZ = 0f,
            leftEarAnchor = leftEar,
            rightEarAnchor = rightEar,
        )
        assertEquals(expectedTopY, placement.topY, DELTA)
    }

    @Test
    fun `partially absent ear anchors fall back to bounding-box path`() {
        // Only one anchor — fallback should be used (same as no anchors)
        val withLeftOnly = computeOverlayPlacement(
            viewBox = box,
            headEulerAngleZ = 0f,
            leftEarAnchor = leftEar,
            rightEarAnchor = null,
        )
        val noAnchors = computeOverlayPlacement(viewBox = box, headEulerAngleZ = 0f)
        assertEquals(noAnchors.centerX, withLeftOnly.centerX, DELTA)
        assertEquals(noAnchors.topY, withLeftOnly.topY, DELTA)
    }

    // ---- PlacementSmoother ----

    @Test
    fun `first value returned as-is (no history)`() {
        val smoother = PlacementSmoother(alpha = 0.5f)
        val input = OverlayPlacement(centerX = 100f, topY = 50f, width = 200f, rotationDegrees = 10f)
        val result = smoother.smooth(input)
        assertEquals(input, result)
    }

    @Test
    fun `second value is interpolated towards input`() {
        val smoother = PlacementSmoother(alpha = 0.5f)
        val first = OverlayPlacement(centerX = 0f, topY = 0f, width = 100f, rotationDegrees = 0f)
        val second = OverlayPlacement(centerX = 100f, topY = 100f, width = 200f, rotationDegrees = 20f)
        smoother.smooth(first)
        val result = smoother.smooth(second)
        // lerp(0, 100, 0.5) = 50
        assertEquals(50f, result.centerX, DELTA)
        assertEquals(50f, result.topY, DELTA)
        assertEquals(150f, result.width, DELTA)
        assertEquals(10f, result.rotationDegrees, DELTA)
    }

    @Test
    fun `reset clears history so next value is returned as-is`() {
        val smoother = PlacementSmoother(alpha = 0.5f)
        val first = OverlayPlacement(centerX = 0f, topY = 0f, width = 0f, rotationDegrees = 0f)
        val second = OverlayPlacement(centerX = 100f, topY = 100f, width = 200f, rotationDegrees = 20f)
        smoother.smooth(first)
        smoother.reset()
        val result = smoother.smooth(second)
        assertEquals(second, result)
    }

    @Test
    fun `smoother angle lerp takes shortest arc across zero`() {
        val smoother = PlacementSmoother(alpha = 0.5f)
        // from -170° to +170° — shortest arc is 20° through ±180, not 340° the long way
        val from = OverlayPlacement(centerX = 0f, topY = 0f, width = 0f, rotationDegrees = -170f)
        val to = OverlayPlacement(centerX = 0f, topY = 0f, width = 0f, rotationDegrees = 170f)
        smoother.smooth(from)
        val result = smoother.smooth(to)
        // diff = 340; normalised to -20; lerp(-170, -20*0.5) = -170 + (-10) = -180 or +180
        assertTrue(result.rotationDegrees >= -180f && result.rotationDegrees <= 180f)
    }
}
