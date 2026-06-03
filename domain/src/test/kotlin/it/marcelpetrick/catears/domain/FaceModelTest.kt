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
        val leftEye = Point2D(30f, 50f)
        val rightEye = Point2D(80f, 50f)
        val leftEar = Point2D(10f, 60f)
        val rightEar = Point2D(100f, 60f)
        val face = FaceModel(
            boundingBox = box,
            leftEyePosition = leftEye,
            rightEyePosition = rightEye,
            headEulerAngleZ = 5f,
            headEulerAngleY = 12f,
            leftEarPosition = leftEar,
            rightEarPosition = rightEar,
        )
        assertEquals(box, face.boundingBox)
        assertEquals(leftEye, face.leftEyePosition)
        assertEquals(rightEye, face.rightEyePosition)
        assertEquals(5f, face.headEulerAngleZ)
        assertEquals(12f, face.headEulerAngleY)
        assertEquals(leftEar, face.leftEarPosition)
        assertEquals(rightEar, face.rightEarPosition)
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
        assertEquals(null, face.leftEarPosition)
        assertEquals(null, face.rightEarPosition)
        assertEquals(0f, face.headEulerAngleY)
    }

    @Test
    fun `FaceModel ear tip positions default to null`() {
        val face = FaceModel(
            boundingBox = box,
            leftEyePosition = null,
            rightEyePosition = null,
            headEulerAngleZ = 0f,
        )
        assertEquals(null, face.leftEarPosition)
        assertEquals(null, face.rightEarPosition)
    }

    @Test
    fun `FaceModel headEulerAngleY defaults to zero`() {
        val face = FaceModel(
            boundingBox = box,
            leftEyePosition = null,
            rightEyePosition = null,
            headEulerAngleZ = 0f,
        )
        assertEquals(0f, face.headEulerAngleY)
    }

    @Test
    fun `FaceModel expression probabilities default to null`() {
        val face = FaceModel(
            boundingBox = box,
            leftEyePosition = null,
            rightEyePosition = null,
            headEulerAngleZ = 0f,
        )
        assertEquals(null, face.smilingProbability)
        assertEquals(null, face.leftEyeOpenProbability)
        assertEquals(null, face.rightEyeOpenProbability)
    }

    @Test
    fun `FaceModel holds expression probabilities when set`() {
        val face = FaceModel(
            boundingBox = box,
            leftEyePosition = null,
            rightEyePosition = null,
            headEulerAngleZ = 0f,
            smilingProbability = 0.92f,
            leftEyeOpenProbability = 0.88f,
            rightEyeOpenProbability = 0.05f,
        )
        assertEquals(0.92f, face.smilingProbability)
        assertEquals(0.88f, face.leftEyeOpenProbability)
        assertEquals(0.05f, face.rightEyeOpenProbability)
    }
}
