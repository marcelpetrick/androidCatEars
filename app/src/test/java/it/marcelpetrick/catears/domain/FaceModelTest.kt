// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FaceModelTest {

    private val box = BoundingBox(left = 10f, top = 20f, right = 110f, bottom = 120f)

    @Test
    fun `BoundingBox width is right minus left`() {
        assertEquals(100f, box.width)
    }

    @Test
    fun `BoundingBox height is bottom minus top`() {
        assertEquals(100f, box.height)
    }

    @Test
    fun `BoundingBox centerX is midpoint horizontally`() {
        assertEquals(60f, box.centerX)
    }

    @Test
    fun `BoundingBox centerY is midpoint vertically`() {
        assertEquals(70f, box.centerY)
    }

    @Test
    fun `FaceModel holds all fields`() {
        val left = Point2D(30f, 50f)
        val right = Point2D(80f, 50f)
        val face = FaceModel(
            boundingBox = box,
            leftEyePosition = left,
            rightEyePosition = right,
            headEulerAngleZ = 5f,
        )
        assertEquals(box, face.boundingBox)
        assertEquals(left, face.leftEyePosition)
        assertEquals(right, face.rightEyePosition)
        assertEquals(5f, face.headEulerAngleZ)
    }

    @Test
    fun `FaceModel with no landmarks is valid`() {
        val face = FaceModel(
            boundingBox = box,
            leftEyePosition = null,
            rightEyePosition = null,
            headEulerAngleZ = 0f,
        )
        assertEquals(null, face.leftEyePosition)
        assertEquals(null, face.rightEyePosition)
    }
}
