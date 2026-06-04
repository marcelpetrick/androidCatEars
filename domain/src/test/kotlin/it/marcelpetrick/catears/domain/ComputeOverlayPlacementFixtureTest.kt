// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Parameterized golden-fixture tests for [computeOverlayPlacement].
 *
 * Each fixture specifies a fully-described input scenario and the exact
 * expected [EarAnchor] field values to within [TOLERANCE] px / degrees.
 * Fixtures are defined in [PlacementFixture.all] so they can be read and
 * verified independently of the test infrastructure.
 */
class ComputeOverlayPlacementFixtureTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    fun `computeOverlayPlacement matches golden fixture`(f: PlacementFixture) {
        val result = computeOverlayPlacement(
            viewBox = f.box,
            headEulerAngleZ = f.eulerZ,
            headEulerAngleY = f.eulerY,
            leftEarAnchor = f.leftEarLandmark,
            rightEarAnchor = f.rightEarLandmark,
            widthRatio = f.widthRatio,
        )
        assertAnchor("left", f.expectedLeft, result.leftEar)
        assertAnchor("right", f.expectedRight, result.rightEar)
        assertEquals(f.eulerY, result.headEulerAngleY, TOLERANCE, "headEulerAngleY")
    }

    private fun assertAnchor(side: String, expected: EarAnchor, actual: EarAnchor) {
        assertEquals(expected.x, actual.x, TOLERANCE, "$side x")
        assertEquals(expected.y, actual.y, TOLERANCE, "$side y")
        assertEquals(expected.size, actual.size, TOLERANCE, "$side size")
        assertEquals(expected.tiltDegrees, actual.tiltDegrees, TOLERANCE, "$side tiltDegrees")
        assertEquals(expected.xScale, actual.xScale, TOLERANCE, "$side xScale")
    }

    companion object {
        private const val TOLERANCE = 0.01f

        @JvmStatic
        fun fixtures(): Stream<PlacementFixture> = Stream.of(*PlacementFixture.all)
    }
}

/**
 * A single fixture row for [ComputeOverlayPlacementFixtureTest].
 *
 * Named so failures print a human-readable label rather than an index.
 */
data class PlacementFixture(
    val name: String,
    val box: BoundingBox,
    val eulerZ: Float,
    val eulerY: Float,
    val leftEarLandmark: Point2D?,
    val rightEarLandmark: Point2D?,
    val widthRatio: Float,
    val expectedLeft: EarAnchor,
    val expectedRight: EarAnchor,
) {
    override fun toString(): String = name

    companion object {
        // Standard face box: 100×100, centred at (200, 300).
        private val BOX = BoundingBox(left = 150f, top = 250f, right = 250f, bottom = 350f)
        private const val W = 0.65f // default widthRatio

        // Derived geometry for the standard box.
        private const val EAR_SIZE = 100f * W // 65
        private const val HALF_SPC = EAR_SIZE * 0.35f // 22.75
        private const val EAR_BOT = 250f + 100f * 0.04f // 254  (earHeightRatio=0.04)
        private const val TOP_Y = EAR_BOT - EAR_SIZE // 189
        private const val LX = 200f - HALF_SPC // 177.25
        private const val RX = 200f + HALF_SPC // 222.75

        @JvmField
        val all: Array<PlacementFixture> = arrayOf(

            // ---- fallback path (no ear landmarks) ----

            PlacementFixture(
                name = "frontal face — no rotation, no landmarks",
                box = BOX, eulerZ = 0f, eulerY = 0f,
                leftEarLandmark = null, rightEarLandmark = null, widthRatio = W,
                expectedLeft = EarAnchor(x = LX, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 0f, xScale = 1f),
                expectedRight = EarAnchor(x = RX, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 0f, xScale = 1f),
            ),

            PlacementFixture(
                name = "frontal face — 15° roll, no landmarks",
                box = BOX, eulerZ = 15f, eulerY = 0f,
                leftEarLandmark = null, rightEarLandmark = null, widthRatio = W,
                expectedLeft = EarAnchor(x = LX, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 15f, xScale = 1f),
                expectedRight = EarAnchor(x = RX, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 15f, xScale = 1f),
            ),

            PlacementFixture(
                name = "head turned right 30° — right ear wider (perspective)",
                box = BOX, eulerZ = 0f, eulerY = 30f,
                leftEarLandmark = null, rightEarLandmark = null, widthRatio = W,
                // yawFraction = 30/45 = 0.6667; left = 1 - 0.6667*0.5 = 0.6667; right = 1 + 0.6667*0.5 = 1.3333
                expectedLeft = EarAnchor(x = LX, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 0f, xScale = 0.6667f),
                expectedRight = EarAnchor(x = RX, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 0f, xScale = 1.3333f),
            ),

            PlacementFixture(
                name = "head turned left 30° — left ear wider (perspective)",
                box = BOX, eulerZ = 0f, eulerY = -30f,
                leftEarLandmark = null, rightEarLandmark = null, widthRatio = W,
                expectedLeft = EarAnchor(x = LX, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 0f, xScale = 1.3333f),
                expectedRight = EarAnchor(x = RX, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 0f, xScale = 0.6667f),
            ),

            PlacementFixture(
                name = "extreme 90° yaw — xScale clamped to [0.4, 1.6]",
                box = BOX, eulerZ = 0f, eulerY = 90f,
                leftEarLandmark = null, rightEarLandmark = null, widthRatio = W,
                // yawFraction clamped to 1.0; left = 1 - 0.5 = 0.5; right = 1 + 0.5 = 1.5
                expectedLeft = EarAnchor(x = LX, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 0f, xScale = 0.5f),
                expectedRight = EarAnchor(x = RX, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 0f, xScale = 1.5f),
            ),

            // ---- ear-landmark anchor path ----

            PlacementFixture(
                name = "ear landmarks present — x follows landmark, y attaches to top of head",
                box = BOX, eulerZ = 0f, eulerY = 0f,
                leftEarLandmark = Point2D(150f, 300f),
                rightEarLandmark = Point2D(250f, 300f),
                widthRatio = W,
                expectedLeft = EarAnchor(x = 150f, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 0f, xScale = 1f),
                expectedRight = EarAnchor(x = 250f, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 0f, xScale = 1f),
            ),

            PlacementFixture(
                name = "ear landmarks present — tilt propagates to both ears",
                box = BOX, eulerZ = 20f, eulerY = 0f,
                leftEarLandmark = Point2D(150f, 300f),
                rightEarLandmark = Point2D(250f, 300f),
                widthRatio = W,
                expectedLeft = EarAnchor(x = 150f, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 20f, xScale = 1f),
                expectedRight = EarAnchor(x = 250f, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 20f, xScale = 1f),
            ),

            PlacementFixture(
                name = "partial landmarks (left only) — falls back to bounding-box path",
                box = BOX, eulerZ = 0f, eulerY = 0f,
                leftEarLandmark = Point2D(150f, 300f),
                rightEarLandmark = null,
                widthRatio = W,
                // Partial → fallback; same result as no-landmark case.
                expectedLeft = EarAnchor(x = LX, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 0f, xScale = 1f),
                expectedRight = EarAnchor(x = RX, y = TOP_Y, size = EAR_SIZE, tiltDegrees = 0f, xScale = 1f),
            ),
        )
    }
}
