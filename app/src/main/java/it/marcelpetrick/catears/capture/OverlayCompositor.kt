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
import it.marcelpetrick.catears.domain.EarStyle
import it.marcelpetrick.catears.domain.OverlayPlacement

/**
 * Composites procedurally-drawn cat ears onto a captured camera frame.
 *
 * The pure geometry helpers ([EarGeometry], [computeEarGeometry]) are
 * JVM-testable. The [composite] function requires Android [Bitmap]/[Canvas] and is
 * excluded from Kover coverage.
 */
object OverlayCompositor {

    /**
     * Pure geometry for one ear — all values in canvas pixel coordinates.
     * No Android dependencies; fully testable on the JVM.
     */
    data class EarGeometry(val outerPath: FloatArray, val innerPath: FloatArray)

    /** Computes the six vertices for the outer and inner classic-ear triangles. */
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
     */
    fun composite(frame: Bitmap, overlayPlacement: OverlayPlacement?): Bitmap {
        val result = frame.copy(Bitmap.Config.ARGB_8888, true)
        if (overlayPlacement == null) return result
        val canvas = Canvas(result)
        val style = overlayPlacement.earStyle
        drawEarOnCanvas(canvas, overlayPlacement.leftEar, style)
        drawEarOnCanvas(canvas, overlayPlacement.rightEar, style)
        return result
    }

    private fun drawEarOnCanvas(canvas: Canvas, anchor: EarAnchor, style: EarStyle) {
        val pivotX = anchor.x
        val pivotY = anchor.y + anchor.size / 2f
        val matrix = Matrix().apply {
            postRotate(anchor.tiltDegrees, pivotX, pivotY)
            postScale(anchor.xScale, 1f, pivotX, pivotY)
        }
        canvas.withMatrix(matrix) {
            when (style) {
                EarStyle.CLASSIC -> drawClassicEar(this, anchor)
                EarStyle.SHARP_FELINE -> drawSharpFelineEar(this, anchor)
                EarStyle.LYNX_TUFTED -> drawLynxTuftedEar(this, anchor)
                EarStyle.CANINE_FLOPPY -> drawCanineFloppyEar(this, anchor)
                EarStyle.CANINE_PERKY -> drawCaninePerkyEar(this, anchor)
            }
        }
    }

    // ─── CLASSIC ─────────────────────────────────────────────────────────────

    private fun drawClassicEar(canvas: Canvas, anchor: EarAnchor) {
        val geo = computeEarGeometry(anchor)
        canvas.drawPath(trianglePath(geo.outerPath), outerPaint)
        canvas.drawPath(trianglePath(geo.innerPath), innerPaint)
    }

    // ─── SHARP FELINE ────────────────────────────────────────────────────────

    private fun drawSharpFelineEar(canvas: Canvas, anchor: EarAnchor) {
        val cx = anchor.x
        val top = anchor.y
        val s = anchor.size
        val tipX = cx + s * 0.08f
        val outerVerts = floatArrayOf(tipX, top, cx - s * 0.44f, top + s, cx + s * 0.30f, top + s)
        canvas.drawPath(trianglePath(outerVerts), felineOuterPaint)
        val innerVerts = floatArrayOf(
            tipX,
            top + s * 0.26f,
            cx - s * 0.22f,
            top + s * 0.76f,
            cx + s * 0.22f,
            top + s * 0.76f,
        )
        canvas.drawPath(trianglePath(innerVerts), felineInnerPaint)
    }

    // ─── LYNX TUFTED ─────────────────────────────────────────────────────────

    private fun drawLynxTuftedEar(canvas: Canvas, anchor: EarAnchor) {
        val cx = anchor.x
        val top = anchor.y
        val s = anchor.size
        val tipX = cx + s * 0.08f
        val outerVerts = floatArrayOf(tipX, top, cx - s * 0.48f, top + s, cx + s * 0.34f, top + s)
        canvas.drawPath(trianglePath(outerVerts), lynxOuterPaint)
        val innerVerts = floatArrayOf(
            tipX,
            top + s * 0.26f,
            cx - s * 0.22f,
            top + s * 0.76f,
            cx + s * 0.22f,
            top + s * 0.76f,
        )
        canvas.drawPath(trianglePath(innerVerts), felineInnerPaint)
        // static tufts (no animation in capture path)
        val tuftPaint = lynxTuftPaint
        val fanAngles = floatArrayOf(-24f, -8f, 8f, 24f)
        for (deg in fanAngles) {
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val len = s * 0.30f
            canvas.drawLine(tipX, top, tipX + kotlin.math.sin(rad) * len, top - kotlin.math.cos(rad) * len, tuftPaint)
        }
    }

    // ─── CANINE FLOPPY ────────────────────────────────────────────────────────

    private fun drawCanineFloppyEar(canvas: Canvas, anchor: EarAnchor) {
        val cx = anchor.x
        val top = anchor.y
        val s = anchor.size
        val flapDx = s * 0.55f
        val flapDy = s * 1.15f
        val outer = Path().apply {
            moveTo(cx - s * 0.15f, top)
            cubicTo(cx - flapDx, top, cx - flapDx, top + flapDy * 0.6f, cx - flapDx * 0.5f, top + flapDy)
            cubicTo(cx, top + flapDy * 1.05f, cx + s * 0.1f, top + flapDy * 0.4f, cx + s * 0.08f, top)
            close()
        }
        canvas.drawPath(outer, floppyOuterPaint)
        val inner = Path().apply {
            moveTo(cx - s * 0.12f, top + s * 0.15f)
            cubicTo(
                cx - flapDx * 0.8f,
                top + s * 0.2f,
                cx - flapDx * 0.8f,
                top + flapDy * 0.55f,
                cx - flapDx * 0.4f,
                top + flapDy * 0.88f,
            )
            cubicTo(
                cx,
                top + flapDy * 0.95f,
                cx + s * 0.05f,
                top + flapDy * 0.35f,
                cx + s * 0.04f,
                top + s * 0.15f,
            )
            close()
        }
        canvas.drawPath(inner, floppyInnerPaint)
    }

    // ─── CANINE PERKY ─────────────────────────────────────────────────────────

    private fun drawCaninePerkyEar(canvas: Canvas, anchor: EarAnchor) {
        val cx = anchor.x
        val top = anchor.y
        val s = anchor.size
        val halfBase = s * 0.38f
        val tipY = top + s * 0.20f
        val outer = floatArrayOf(cx - halfBase, top + s, cx + halfBase, top + s, cx, tipY)
        canvas.drawPath(trianglePath(outer), perkyOuterPaint)
        // round cap via circle at tip
        canvas.drawCircle(cx, tipY, halfBase * 0.4f, perkyOuterPaint)
        val inner = floatArrayOf(
            cx - halfBase * 0.55f,
            top + s * 0.95f,
            cx + halfBase * 0.55f,
            top + s * 0.95f,
            cx,
            tipY + s * 0.12f,
        )
        canvas.drawPath(trianglePath(inner), perkyInnerPaint)
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private fun trianglePath(vertices: FloatArray): Path = Path().apply {
        moveTo(vertices[0], vertices[1])
        lineTo(vertices[2], vertices[3])
        lineTo(vertices[4], vertices[5])
        close()
    }

    // ─── paints — lazy to avoid android.graphics.Color at class-load in JVM tests ──

    private val outerPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CLASSIC_OUTER } }
    private val innerPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CLASSIC_INNER } }
    private val felineOuterPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FELINE_OUTER } }
    private val felineInnerPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FELINE_INNER } }
    private val lynxOuterPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LYNX_OUTER } }
    private val lynxTuftPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LYNX_TUFT
            strokeWidth = 4f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }
    private val floppyOuterPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FLOPPY_OUTER } }
    private val floppyInnerPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FLOPPY_INNER } }
    private val perkyOuterPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PERKY_OUTER } }
    private val perkyInnerPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PERKY_INNER } }

    // ─── geometry constants ───────────────────────────────────────────────────
    private const val OUTER_HALF_BASE = 0.42f
    private const val INNER_HALF_BASE = 0.24f
    private const val INNER_TOP_OFFSET = 0.28f
    private const val INNER_BOTTOM_OFFSET = 0.78f

    // ─── colours — pure bit-ops, no android.graphics.Color at load time ───────
    private val CLASSIC_OUTER: Int = (0xFF shl 24) or (0x8B shl 16) or (0x5E shl 8) or 0x3C
    private val CLASSIC_INNER: Int = (0xFF shl 24) or (0xE8 shl 16) or (0xA0 shl 8) or 0xA0
    private val FELINE_OUTER: Int = (0xFF shl 24) or (0xBF shl 16) or (0x8A shl 8) or 0x5A
    private val FELINE_INNER: Int = (0xFF shl 24) or (0xE8 shl 16) or (0xA8 shl 8) or 0xA0
    private val LYNX_OUTER: Int = (0xFF shl 24) or (0x9B shl 16) or (0x70 shl 8) or 0x40
    private val LYNX_TUFT: Int = (0xFF shl 24) or (0x2E shl 16) or (0x1A shl 8) or 0x08
    private val FLOPPY_OUTER: Int = (0xFF shl 24) or (0xB8 shl 16) or (0x86 shl 8) or 0x4A
    private val FLOPPY_INNER: Int = (0xFF shl 24) or (0xE8 shl 16) or (0xC8 shl 8) or 0xA8
    private val PERKY_OUTER: Int = (0xFF shl 24) or (0xD4 shl 16) or (0xB8 shl 8) or 0x96
    private val PERKY_INNER: Int = (0xFF shl 24) or (0xE8 shl 16) or (0xA0 shl 8) or 0x90
}
