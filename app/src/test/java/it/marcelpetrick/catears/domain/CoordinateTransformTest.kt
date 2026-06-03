// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private const val DELTA = 0.01f

class CoordinateTransformTest {

    // Image: 640×480, View: 1280×960 (2× scale, no aspect change)
    private val rearCtx =
        TransformContext(imageWidth = 640, imageHeight = 480, viewWidth = 1280, viewHeight = 960, isFrontCamera = false)
    private val frontCtx =
        TransformContext(imageWidth = 640, imageHeight = 480, viewWidth = 1280, viewHeight = 960, isFrontCamera = true)

    // ---- imageToViewCoordinates ----

    @Test
    fun `rear camera scales point by view-to-image ratio`() {
        val result = imageToViewCoordinates(Point2D(100f, 200f), rearCtx)
        assertEquals(200f, result.x, DELTA)
        assertEquals(400f, result.y, DELTA)
    }

    @Test
    fun `front camera mirrors x after scaling`() {
        // scaledX = 100 * (1280/640) = 200; mirrored = 1280 - 200 = 1080
        val result = imageToViewCoordinates(Point2D(100f, 200f), frontCtx)
        assertEquals(1080f, result.x, DELTA)
        assertEquals(400f, result.y, DELTA)
    }

    @Test
    fun `origin maps to view origin on rear camera`() {
        val result = imageToViewCoordinates(Point2D(0f, 0f), rearCtx)
        assertEquals(0f, result.x, DELTA)
        assertEquals(0f, result.y, DELTA)
    }

    @Test
    fun `far corner maps to view far corner on rear camera`() {
        val result = imageToViewCoordinates(Point2D(640f, 480f), rearCtx)
        assertEquals(1280f, result.x, DELTA)
        assertEquals(960f, result.y, DELTA)
    }

    @Test
    fun `front camera origin x maps to view right edge`() {
        // x=0 in image → scaledX=0 → mirrored = viewW - 0 = viewW
        val result = imageToViewCoordinates(Point2D(0f, 0f), frontCtx)
        assertEquals(1280f, result.x, DELTA)
        assertEquals(0f, result.y, DELTA)
    }

    // ---- imageToViewBoundingBox ----

    @Test
    fun `bounding box scales correctly on rear camera`() {
        val box = BoundingBox(left = 100f, top = 50f, right = 200f, bottom = 150f)
        val result = imageToViewBoundingBox(box, rearCtx)
        assertEquals(200f, result.left, DELTA)
        assertEquals(100f, result.top, DELTA)
        assertEquals(400f, result.right, DELTA)
        assertEquals(300f, result.bottom, DELTA)
    }

    @Test
    fun `bounding box left is always less than right after front mirror`() {
        val box = BoundingBox(left = 100f, top = 50f, right = 200f, bottom = 150f)
        val result = imageToViewBoundingBox(box, frontCtx)
        // After mirror, left > right in raw coords; the function must normalise
        assert(result.left < result.right) { "left (${result.left}) must be < right (${result.right})" }
    }
}
