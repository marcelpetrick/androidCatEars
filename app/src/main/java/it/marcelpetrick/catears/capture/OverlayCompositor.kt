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
     * Draws cat ears for every placement in [placements] onto [frame] and returns the result.
     * Returns a copy of [frame] unchanged when [placements] is empty.
     */
    fun composite(frame: Bitmap, placements: List<OverlayPlacement>): Bitmap {
        val result = frame.copy(Bitmap.Config.ARGB_8888, true)
        if (placements.isEmpty()) return result
        val canvas = Canvas(result)
        for (p in placements) {
            drawEarOnCanvas(canvas, p.leftEar, p.earStyle)
            drawEarOnCanvas(canvas, p.rightEar, p.earStyle)
        }
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
                EarStyle.ROUNDED_FELINE -> drawRoundedFelineEar(this, anchor)
                EarStyle.LYNX_TUFTED -> drawLynxTuftedEar(this, anchor)
                EarStyle.DENSE_FLUFFY -> drawDenseFluffyEar(this, anchor)
                EarStyle.CANINE_FLOPPY -> drawCanineFloppyEar(this, anchor)
                EarStyle.CANINE_PERKY -> drawCaninePerkyEar(this, anchor)
                EarStyle.RABBIT -> drawRabbitEar(this, anchor)
                EarStyle.FOX -> drawFoxEar(this, anchor)
                EarStyle.BEAR -> drawBearEar(this, anchor)
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

    // ─── ROUNDED FELINE ──────────────────────────────────────────────────────

    private fun drawRoundedFelineEar(canvas: Canvas, anchor: EarAnchor) {
        val cx = anchor.x
        val top = anchor.y
        val s = anchor.size
        val outer = android.graphics.Path().apply {
            moveTo(cx - s * 0.38f, top + s)
            cubicTo(cx - s * 0.52f, top + s * 0.50f, cx - s * 0.08f, top + s * 0.05f, cx, top)
            cubicTo(cx + s * 0.32f, top + s * 0.08f, cx + s * 0.28f, top + s * 0.60f, cx + s * 0.24f, top + s)
            close()
        }
        canvas.drawPath(outer, roundedOuterPaint)
        val inner = android.graphics.Path().apply {
            moveTo(cx - s * 0.18f, top + s * 0.92f)
            cubicTo(cx - s * 0.24f, top + s * 0.50f, cx - s * 0.04f, top + s * 0.18f, cx, top + s * 0.10f)
            cubicTo(cx + s * 0.14f, top + s * 0.18f, cx + s * 0.14f, top + s * 0.50f, cx + s * 0.12f, top + s * 0.92f)
            close()
        }
        canvas.drawPath(inner, felineInnerPaint)
    }

    // ─── DENSE FLUFFY ─────────────────────────────────────────────────────────

    private fun drawDenseFluffyEar(canvas: Canvas, anchor: EarAnchor) {
        val cx = anchor.x
        val top = anchor.y
        val s = anchor.size
        val halfBase = s * 0.48f
        canvas.drawPath(
            trianglePath(floatArrayOf(cx, top, cx - halfBase, top + s, cx + halfBase * 0.85f, top + s)),
            fluffyOuterPaint,
        )
        val ih = s * 0.26f
        canvas.drawPath(
            trianglePath(
                floatArrayOf(
                    cx,
                    top + s * 0.20f,
                    cx - ih,
                    top + s * 0.88f,
                    cx + ih,
                    top + s * 0.88f,
                ),
            ),
            fluffyInnerPaint,
        )
        val furPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FLUFFY_FUR
            style = Paint.Style.STROKE
            strokeWidth = s * 0.045f
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0..7) {
            val frac = i / 7f
            canvas.drawLine(
                cx - halfBase * frac,
                top + s * frac,
                cx - halfBase * frac - s * 0.10f,
                top + s * frac - s * 0.08f,
                furPaint,
            )
        }
    }

    // ─── RABBIT ───────────────────────────────────────────────────────────────

    private fun drawRabbitEar(canvas: Canvas, anchor: EarAnchor) {
        val cx = anchor.x
        val top = anchor.y
        val s = anchor.size
        val halfW = s * 0.18f
        canvas.drawOval(cx - halfW, top, cx + halfW, top + s, rabbitOuterPaint)
        canvas.drawOval(cx - halfW * 0.55f, top + s * 0.06f, cx + halfW * 0.55f, top + s * 0.92f, rabbitInnerPaint)
    }

    // ─── FOX ──────────────────────────────────────────────────────────────────

    private fun drawFoxEar(canvas: Canvas, anchor: EarAnchor) {
        val cx = anchor.x
        val top = anchor.y
        val s = anchor.size
        val halfBase = s * 0.32f
        val tipX = cx + s * 0.05f
        canvas.drawPath(
            trianglePath(floatArrayOf(tipX, top, cx - halfBase, top + s, cx + halfBase * 0.7f, top + s)),
            foxOuterPaint,
        )
        canvas.drawPath(
            trianglePath(
                floatArrayOf(
                    tipX,
                    top + s * 0.14f,
                    cx - halfBase * 0.52f,
                    top + s * 0.88f,
                    cx + halfBase * 0.4f,
                    top + s * 0.88f,
                ),
            ),
            foxInnerPaint,
        )
        canvas.drawCircle(tipX, top, s * 0.07f, whitePaint)
    }

    // ─── BEAR ─────────────────────────────────────────────────────────────────

    private fun drawBearEar(canvas: Canvas, anchor: EarAnchor) {
        val cx = anchor.x
        val top = anchor.y
        val s = anchor.size
        val r = s * 0.32f
        val cy = top + r
        canvas.drawCircle(cx, cy, r, bearOuterPaint)
        canvas.drawCircle(cx, cy, r * 0.55f, bearInnerPaint)
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
    private val roundedOuterPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ROUNDED_OUTER } }
    private val fluffyOuterPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FLUFFY_OUTER } }
    private val fluffyInnerPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FLUFFY_INNER } }
    private val rabbitOuterPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = RABBIT_OUTER } }
    private val rabbitInnerPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = RABBIT_INNER } }
    private val foxOuterPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FOX_OUTER } }
    private val foxInnerPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FOX_INNER } }
    private val whitePaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE } }
    private val bearOuterPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BEAR_OUTER } }
    private val bearInnerPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BEAR_INNER } }

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
    private val ROUNDED_OUTER: Int = (0xFF shl 24) or (0xBF shl 16) or (0x8A shl 8) or 0x5A
    private val FLUFFY_OUTER: Int = (0xFF shl 24) or (0x9E shl 16) or (0x6A shl 8) or 0x38
    private val FLUFFY_INNER: Int = (0xFF shl 24) or (0xE0 shl 16) or (0xB0 shl 8) or 0xA0
    private val FLUFFY_FUR: Int = (0xFF shl 24) or (0x6A shl 16) or (0x3E shl 8) or 0x18
    private val RABBIT_OUTER: Int = (0xFF shl 24) or (0xF0 shl 16) or (0xEC shl 8) or 0xEC
    private val RABBIT_INNER: Int = (0xFF shl 24) or (0xE8 shl 16) or (0x90 shl 8) or 0xB0
    private val FOX_OUTER: Int = (0xFF shl 24) or (0xD4 shl 16) or (0x58 shl 8) or 0x00
    private val FOX_INNER: Int = (0xFF shl 24) or (0xE8 shl 16) or (0xC0 shl 8) or 0x90
    private val WHITE: Int = (0xFF shl 24) or (0xFF shl 16) or (0xFF shl 8) or 0xFF
    private val BEAR_OUTER: Int = (0xFF shl 24) or (0x3A shl 16) or (0x20 shl 8) or 0x10
    private val BEAR_INNER: Int = (0xFF shl 24) or (0x7A shl 16) or (0x40 shl 8) or 0x20
}
