// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.capture

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.withMatrix
import it.marcelpetrick.catears.domain.EarAnchor
import it.marcelpetrick.catears.domain.OverlayPlacement

/**
 * Composites procedurally-drawn cat ears onto a captured camera frame.
 *
 * The pure geometry helpers ([EarGeometry], [computeOuterPath], [computeInnerPath]) are
 * JVM-testable. The [composite] function requires Android [Bitmap]/[Canvas] and is
 * excluded from Kover coverage.
 */
object OverlayCompositor {

    /**
     * Pure geometry for one ear — all values in canvas pixel coordinates.
     * No Android dependencies; fully testable on the JVM.
     */
    data class EarGeometry(
        val outerPath: FloatArray, // triangle vertices: [x0,y0, x1,y1, x2,y2]
        val innerPath: FloatArray,
    )

    /** Computes the six vertices for the outer and inner ear triangles. */
    fun computeEarGeometry(anchor: EarAnchor): EarGeometry {
        val cx = anchor.x
        val top = anchor.y
        val s = anchor.size
        val outerHalf = s * OUTER_HALF_BASE
        val innerHalf = s * INNER_HALF_BASE
        val innerTop = top + s * INNER_TOP_OFFSET
        val innerBottom = top + s * INNER_BOTTOM_OFFSET
        return EarGeometry(
            outerPath = floatArrayOf(cx, top, cx - outerHalf, top + s, cx + outerHalf, top + s),
            innerPath = floatArrayOf(cx, innerTop, cx - innerHalf, innerBottom, cx + innerHalf, innerBottom),
        )
    }

    /**
     * Draws [overlayPlacement] cat ears onto [frame] and returns the composited result.
     * Returns a copy of [frame] when [overlayPlacement] is null.
     * The caller is responsible for recycling the returned bitmap.
     */
    fun composite(frame: Bitmap, overlayPlacement: OverlayPlacement?): Bitmap {
        val result = frame.copy(Bitmap.Config.ARGB_8888, true)
        if (overlayPlacement == null) return result

        val canvas = Canvas(result)
        drawEarOnCanvas(canvas, overlayPlacement.leftEar)
        drawEarOnCanvas(canvas, overlayPlacement.rightEar)
        return result
    }

    private fun drawEarOnCanvas(canvas: Canvas, anchor: EarAnchor) {
        val geo = computeEarGeometry(anchor)
        val pivotX = anchor.x
        val pivotY = anchor.y + anchor.size / 2f

        val matrix = Matrix().apply {
            postRotate(anchor.tiltDegrees, pivotX, pivotY)
            postScale(anchor.xScale, 1f, pivotX, pivotY)
        }

        canvas.withMatrix(matrix) {
            drawPath(trianglePath(geo.outerPath), outerPaint)
            drawPath(trianglePath(geo.innerPath), innerPaint)
        }
    }

    private fun trianglePath(vertices: FloatArray): Path = Path().apply {
        moveTo(vertices[0], vertices[1])
        lineTo(vertices[2], vertices[3])
        lineTo(vertices[4], vertices[5])
        close()
    }

    // Computed with pure bit-ops to avoid android.graphics.Color at class-load time;
    // lazy so they're never initialized during JVM unit tests.
    private val outerPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = OUTER_COLOR }
    }
    private val innerPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INNER_COLOR }
    }

    private const val OUTER_HALF_BASE = 0.42f
    private const val INNER_HALF_BASE = 0.24f
    private const val INNER_TOP_OFFSET = 0.28f
    private const val INNER_BOTTOM_OFFSET = 0.78f

    // ARGB packed as (alpha shl 24) or (r shl 16) or (g shl 8) or b — pure arithmetic.
    private val OUTER_COLOR: Int = (0xFF shl 24) or (0x8B shl 16) or (0x5E shl 8) or 0x3C
    private val INNER_COLOR: Int = (0xFF shl 24) or (0xE8 shl 16) or (0xA0 shl 8) or 0xA0
}
