// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.capture

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.withMatrix
import it.marcelpetrick.catears.domain.EarAnchor
import it.marcelpetrick.catears.domain.EarMaterialSpec
import it.marcelpetrick.catears.domain.EarRenderStyleSpec
import it.marcelpetrick.catears.domain.EarRendererKind
import it.marcelpetrick.catears.domain.EarStyle
import it.marcelpetrick.catears.domain.EarTint
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.domain.earRenderStyleSpec
import it.marcelpetrick.catears.domain.hueRotationMatrix
import it.marcelpetrick.catears.overlay.earSpriteDrawableId
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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

    private data class MaterialEarGeometry(
        val tipX: Float,
        val tipY: Float,
        val leftBaseX: Float,
        val leftBaseY: Float,
        val rightBaseX: Float,
        val rightBaseY: Float,
    )

    private data class CanvasEarFrame(val style: EarStyle, val resources: Resources?, val tintPaint: Paint?)

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
     * Draws cat ears for every placement in [placements] directly onto [canvas].
     * Used by the video overlay effect to bake ears into recorded frames.
     */
    fun drawEarsOnCanvas(canvas: Canvas, placements: List<OverlayPlacement>, resources: Resources? = null) {
        for (p in placements) {
            val frame = CanvasEarFrame(
                style = p.earStyle,
                resources = resources,
                tintPaint = tintPaints[p.tint],
            )
            drawEarOnCanvas(canvas, p.leftEar, isLeft = true, frame)
            drawEarOnCanvas(canvas, p.rightEar, isLeft = false, frame)
        }
    }

    /**
     * Draws cat ears for every placement in [placements] onto [frame] and returns the result.
     * Returns a copy of [frame] unchanged when [placements] is empty.
     */
    @Suppress("TooGenericExceptionCaught")
    fun composite(frame: Bitmap, placements: List<OverlayPlacement>, resources: Resources? = null): Bitmap {
        val result = frame.copy(Bitmap.Config.ARGB_8888, true)
        try {
            if (placements.isEmpty()) return result
            val canvas = Canvas(result)
            for (p in placements) {
                val frameState = CanvasEarFrame(
                    style = p.earStyle,
                    resources = resources,
                    tintPaint = tintPaints[p.tint],
                )
                drawEarOnCanvas(canvas, p.leftEar, isLeft = true, frameState)
                drawEarOnCanvas(canvas, p.rightEar, isLeft = false, frameState)
            }
        } catch (e: RuntimeException) {
            result.recycle()
            throw e
        }
        return result
    }

    private fun drawEarOnCanvas(canvas: Canvas, anchor: EarAnchor, isLeft: Boolean, frame: CanvasEarFrame) {
        val spec = earRenderStyleSpec(frame.style)
        val sprite = spriteBitmapFor(frame.style, frame.resources)
        if (spec.rendererKind == EarRendererKind.Sprite && sprite != null) {
            drawSpriteEar(canvas, anchor, sprite, isLeft)
        } else {
            drawProceduralEarOnCanvas(canvas, anchor, spec, frame)
        }
    }

    private fun drawSpriteEar(canvas: Canvas, anchor: EarAnchor, bitmap: Bitmap, isLeft: Boolean) {
        val spriteHeight = anchor.size
        val spriteWidth = spriteHeight * bitmap.width / bitmap.height
        val baseY = anchor.y + anchor.size
        val basePivotX = anchor.x
        val matrix = Matrix().apply {
            setScale(spriteWidth / bitmap.width, spriteHeight / bitmap.height)
            postTranslate(anchor.x - spriteWidth / 2f, baseY - spriteHeight)
            postScale(if (isLeft) -1f else 1f, 1f, basePivotX, baseY)
            postScale(anchor.xScale, 1f, basePivotX, baseY)
            postRotate(anchor.tiltDegrees, basePivotX, baseY)
        }
        canvas.drawBitmap(bitmap, matrix, null)
    }

    private fun drawProceduralEarOnCanvas(
        canvas: Canvas,
        anchor: EarAnchor,
        spec: EarRenderStyleSpec,
        frame: CanvasEarFrame,
    ) {
        if (frame.tintPaint != null) canvas.saveLayer(earBounds(anchor), frame.tintPaint)
        val pivotX = anchor.x
        val pivotY = anchor.y + anchor.size / 2f
        val matrix = Matrix().apply {
            postRotate(anchor.tiltDegrees, pivotX, pivotY)
            postScale(anchor.xScale, 1f, pivotX, pivotY)
        }
        canvas.withMatrix(matrix) {
            drawSoftEarShadow(this, anchor, spec.material)
            when (frame.style) {
                EarStyle.CLASSIC -> drawClassicEar(this, anchor)
                EarStyle.SHARP_FELINE -> drawSharpFelineEar(this, anchor)
                EarStyle.ROUNDED_FELINE -> drawRoundedFelineEar(this, anchor)
                EarStyle.LYNX_TUFTED -> drawLynxTuftedEar(this, anchor)
                EarStyle.DENSE_FLUFFY -> drawDenseFluffyEar(this, anchor)
                EarStyle.FOX -> drawFoxEar(this, anchor)
            }
            drawMaterialFinish(this, anchor, spec)
        }
        if (frame.tintPaint != null) canvas.restore()
    }

    private fun earBounds(anchor: EarAnchor): RectF = RectF(
        anchor.x - anchor.size,
        anchor.y - anchor.size * 0.25f,
        anchor.x + anchor.size,
        anchor.y + anchor.size * 1.5f,
    )

    private fun spriteBitmapFor(style: EarStyle, resources: Resources?): Bitmap? =
        earSpriteDrawableId(style)?.let { drawableId ->
            resources?.let {
                spriteBitmapCache.getOrPut(drawableId) {
                    BitmapFactory.decodeResource(it, drawableId)
                }
            }
        }

    private fun drawSoftEarShadow(canvas: Canvas, anchor: EarAnchor, material: EarMaterialSpec) {
        val s = anchor.size
        matFillPaint.color = colorWithAlpha(material.shadowArgb, SHADOW_ALPHA)
        canvas.drawOval(
            anchor.x - s * SHADOW_HALF_WIDTH,
            anchor.y + s * SHADOW_TOP_RATIO,
            anchor.x + s * SHADOW_HALF_WIDTH,
            anchor.y + s * (SHADOW_TOP_RATIO + SHADOW_HEIGHT),
            matFillPaint,
        )
    }

    private fun drawMaterialFinish(canvas: Canvas, anchor: EarAnchor, spec: EarRenderStyleSpec) {
        val s = anchor.size
        val geometry = MaterialEarGeometry(
            tipX = anchor.x + styleTipOffset(spec.style) * s,
            tipY = anchor.y,
            leftBaseX = anchor.x - styleLeftBase(spec.style) * s,
            leftBaseY = anchor.y + s,
            rightBaseX = anchor.x + styleRightBase(spec.style) * s,
            rightBaseY = anchor.y + s,
        )
        drawOuterRim(canvas, geometry, s, spec.material)
        drawInnerRosyGlaze(canvas, geometry, s, spec.material)
        drawFurTexture(canvas, geometry, s, spec)
        if (spec.supportsTufts) {
            drawMaterialTufts(canvas, geometry, s, spec.material)
        }
    }

    private fun drawOuterRim(canvas: Canvas, geometry: MaterialEarGeometry, s: Float, material: EarMaterialSpec) {
        matStrokePaint.color = colorWithAlpha(material.outerRimArgb, RIM_ALPHA)
        matStrokePaint.strokeWidth = s * MATERIAL_RIM_WIDTH_RATIO
        canvas.drawLine(geometry.leftBaseX, geometry.leftBaseY, geometry.tipX, geometry.tipY, matStrokePaint)
        canvas.drawLine(geometry.rightBaseX, geometry.rightBaseY, geometry.tipX, geometry.tipY, matStrokePaint)
        matStrokePaint.color = colorWithAlpha(material.outerHighlightArgb, HIGHLIGHT_ALPHA)
        matStrokePaint.strokeWidth = s * MATERIAL_HIGHLIGHT_WIDTH_RATIO
        canvas.drawLine(
            geometry.tipX - s * HIGHLIGHT_TIP_X_OFFSET,
            geometry.tipY + s * HIGHLIGHT_TIP_Y_OFFSET,
            geometry.leftBaseX + s * HIGHLIGHT_BASE_X_OFFSET,
            geometry.leftBaseY - s * HIGHLIGHT_BASE_Y_OFFSET,
            matStrokePaint,
        )
    }

    private fun drawInnerRosyGlaze(canvas: Canvas, geometry: MaterialEarGeometry, s: Float, material: EarMaterialSpec) {
        val innerTopX = geometry.tipX
        val innerTopY = geometry.tipY + s * MATERIAL_INNER_TOP_RATIO
        val tipBlend = 1f - MATERIAL_INNER_BASE_BLEND
        val innerLeftX =
            geometry.leftBaseX * MATERIAL_INNER_BASE_BLEND + geometry.tipX * tipBlend
        val innerRightX =
            geometry.rightBaseX * MATERIAL_INNER_BASE_BLEND + geometry.tipX * tipBlend
        val innerBaseY = geometry.leftBaseY - s * MATERIAL_INNER_BASE_LIFT_RATIO
        val inner = Path().apply {
            moveTo(innerTopX, innerTopY)
            lineTo(innerLeftX, innerBaseY)
            lineTo(innerRightX, innerBaseY)
            close()
        }
        matFillPaint.color = colorWithAlpha(material.innerBaseArgb, INNER_GLAZE_BOTTOM_ALPHA)
        canvas.drawPath(inner, matFillPaint)
        matStrokePaint.color = colorWithAlpha(material.innerHighlightArgb, INNER_GLAZE_TOP_ALPHA)
        matStrokePaint.strokeWidth = s * MATERIAL_HIGHLIGHT_WIDTH_RATIO
        canvas.drawLine(innerTopX, innerTopY, innerLeftX, innerBaseY, matStrokePaint)
    }

    private fun drawFurTexture(canvas: Canvas, geometry: MaterialEarGeometry, s: Float, spec: EarRenderStyleSpec) {
        matStrokePaint.color = colorWithAlpha(spec.material.outerHighlightArgb, MATERIAL_FUR_ALPHA)
        matStrokePaint.strokeWidth = s * MATERIAL_FUR_WIDTH_RATIO
        repeat(spec.furStrokeCount) { index ->
            val fraction = (index + 1f) / (spec.furStrokeCount + 1f)
            val leftEdge = index % 2 == 0
            val startX = if (leftEdge) geometry.leftBaseX else geometry.rightBaseX
            val startY = if (leftEdge) geometry.leftBaseY else geometry.rightBaseY
            val edgeX = lerp(startX, geometry.tipX, fraction)
            val edgeY = lerp(startY, geometry.tipY, fraction)
            val inwardX = lerp(edgeX, geometry.tipX, MATERIAL_FUR_INWARD_BLEND)
            val direction = if (leftEdge) 1f else -1f
            canvas.drawLine(
                edgeX,
                edgeY,
                inwardX + direction * s * MATERIAL_FUR_LENGTH_RATIO,
                edgeY - s * MATERIAL_FUR_LIFT_RATIO,
                matStrokePaint,
            )
        }
    }

    private fun drawMaterialTufts(canvas: Canvas, geometry: MaterialEarGeometry, s: Float, material: EarMaterialSpec) {
        matStrokePaint.color = material.outerRimArgb
        matStrokePaint.strokeWidth = s * MATERIAL_TUFT_WIDTH_RATIO
        for (i in -1..1) {
            val angle = Math.toRadians((MATERIAL_TUFT_FAN_DEG * i).toDouble()).toFloat()
            canvas.drawLine(
                geometry.tipX,
                geometry.tipY,
                geometry.tipX + sin(angle) * s * MATERIAL_TUFT_LENGTH_RATIO,
                geometry.tipY - cos(angle) * s * MATERIAL_TUFT_LENGTH_RATIO,
                matStrokePaint,
            )
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
        // swayTime=0 positions match CatEarOverlay.drawDenseFluffyEar at rest.
        for (i in 0..7) {
            val frac = i / 7f
            val bx = cx - halfBase * frac
            val by = top + s * frac
            val sway = sin(FLUFFY_PHASES[i].toDouble()).toFloat() * s * 0.07f
            canvas.drawLine(bx, by, bx - s * 0.10f + sway, by - s * 0.08f, furPaint)
        }
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

    // ─── helpers ─────────────────────────────────────────────────────────────

    private fun trianglePath(vertices: FloatArray): Path = Path().apply {
        moveTo(vertices[0], vertices[1])
        lineTo(vertices[2], vertices[3])
        lineTo(vertices[4], vertices[5])
        close()
    }

    // Two reusable Paints for the material finish pass. Lazy to avoid android.graphics.Paint
    // at class-load time in JVM tests. Drawing is sequential and mutually exclusive between
    // the video overlay thread and the photo capture thread (state machine prevents
    // simultaneous use), so shared mutable state is safe here.
    private val matFillPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }
    private val matStrokePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }

    private fun colorWithAlpha(argb: Int, alpha: Float): Int {
        val resolvedAlpha = ((argb ushr ALPHA_SHIFT) * alpha)
            .roundToInt()
            .coerceIn(MIN_COLOR_COMPONENT, MAX_COLOR_COMPONENT)
        return (argb and RGB_MASK) or (resolvedAlpha shl ALPHA_SHIFT)
    }

    private fun lerp(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction

    private fun styleTipOffset(style: EarStyle): Float = when (style) {
        EarStyle.SHARP_FELINE,
        EarStyle.LYNX_TUFTED,
        EarStyle.FOX,
        -> FELINE_TIP_OFFSET_X

        else -> 0f
    }

    private fun styleLeftBase(style: EarStyle): Float = when (style) {
        EarStyle.SHARP_FELINE -> FELINE_BASE_LEFT
        EarStyle.LYNX_TUFTED -> LYNX_BASE_LEFT
        EarStyle.DENSE_FLUFFY -> FLUFFY_LEFT_BASE
        EarStyle.FOX -> FOX_HALF_BASE
        else -> OUTER_HALF_BASE
    }

    private fun styleRightBase(style: EarStyle): Float = when (style) {
        EarStyle.SHARP_FELINE -> FELINE_BASE_RIGHT
        EarStyle.LYNX_TUFTED -> LYNX_BASE_RIGHT
        EarStyle.DENSE_FLUFFY -> FLUFFY_RIGHT_BASE
        EarStyle.FOX -> FOX_RIGHT_BASE
        else -> OUTER_HALF_BASE
    }

    // ─── tint paints — one per non-natural EarTint value, cached for hot-path reuse ─
    // saveLayer with a tight RectF avoids allocating a full-frame offscreen buffer.
    private val tintPaints: Map<EarTint, Paint> by lazy {
        EarTint.entries
            .filter { it != EarTint.NATURAL }
            .associateWith { tint ->
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(ColorMatrix(hueRotationMatrix(tint.hueDegrees)))
                }
            }
    }

    private val spriteBitmapCache = ConcurrentHashMap<Int, Bitmap>()

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
    private val roundedOuterPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ROUNDED_OUTER } }
    private val fluffyOuterPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FLUFFY_OUTER } }
    private val fluffyInnerPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FLUFFY_INNER } }
    private val foxOuterPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FOX_OUTER } }
    private val foxInnerPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FOX_INNER } }
    private val whitePaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE } }

    // ─── geometry constants ───────────────────────────────────────────────────
    private const val OUTER_HALF_BASE = 0.42f
    private const val INNER_HALF_BASE = 0.24f
    private const val INNER_TOP_OFFSET = 0.28f
    private const val INNER_BOTTOM_OFFSET = 0.78f
    private const val FELINE_TIP_OFFSET_X = 0.08f
    private const val FELINE_BASE_LEFT = 0.44f
    private const val FELINE_BASE_RIGHT = 0.30f
    private const val LYNX_BASE_LEFT = 0.48f
    private const val LYNX_BASE_RIGHT = 0.34f
    private const val FLUFFY_LEFT_BASE = 0.48f
    private const val FLUFFY_RIGHT_BASE = 0.408f
    private const val FOX_HALF_BASE = 0.32f
    private const val FOX_RIGHT_BASE = 0.224f
    private val FLUFFY_PHASES = floatArrayOf(0.0f, 0.8f, 1.6f, 2.4f, 0.4f, 1.2f, 2.0f, 0.6f)

    // ─── material finish constants ───────────────────────────────────────────
    private const val SHADOW_HALF_WIDTH = 0.46f
    private const val SHADOW_TOP_RATIO = 0.68f
    private const val SHADOW_HEIGHT = 0.36f
    private const val SHADOW_ALPHA = 1f
    private const val MATERIAL_RIM_WIDTH_RATIO = 0.035f
    private const val MATERIAL_HIGHLIGHT_WIDTH_RATIO = 0.022f
    private const val RIM_ALPHA = 0.72f
    private const val HIGHLIGHT_ALPHA = 0.62f
    private const val HIGHLIGHT_TIP_X_OFFSET = 0.07f
    private const val HIGHLIGHT_TIP_Y_OFFSET = 0.12f
    private const val HIGHLIGHT_BASE_X_OFFSET = 0.10f
    private const val HIGHLIGHT_BASE_Y_OFFSET = 0.12f
    private const val MATERIAL_INNER_TOP_RATIO = 0.22f
    private const val MATERIAL_INNER_BASE_BLEND = 0.58f
    private const val MATERIAL_INNER_BASE_LIFT_RATIO = 0.18f
    private const val INNER_GLAZE_TOP_ALPHA = 0.72f
    private const val INNER_GLAZE_BOTTOM_ALPHA = 0.52f
    private const val MATERIAL_FUR_WIDTH_RATIO = 0.016f
    private const val MATERIAL_FUR_INWARD_BLEND = 0.48f
    private const val MATERIAL_FUR_LENGTH_RATIO = 0.12f
    private const val MATERIAL_FUR_LIFT_RATIO = 0.035f
    private const val MATERIAL_FUR_ALPHA = 0.56f
    private const val MATERIAL_TUFT_FAN_DEG = 17f
    private const val MATERIAL_TUFT_LENGTH_RATIO = 0.18f
    private const val MATERIAL_TUFT_WIDTH_RATIO = 0.032f
    private const val ALPHA_SHIFT = 24
    private const val RGB_MASK = 0x00FFFFFF
    private const val MIN_COLOR_COMPONENT = 0
    private const val MAX_COLOR_COMPONENT = 255

    // ─── colours — pure bit-ops, no android.graphics.Color at load time ───────
    private val CLASSIC_OUTER: Int = (0xFF shl 24) or (0x8B shl 16) or (0x5E shl 8) or 0x3C
    private val CLASSIC_INNER: Int = (0xFF shl 24) or (0xE8 shl 16) or (0xA0 shl 8) or 0xA0
    private val FELINE_OUTER: Int = (0xFF shl 24) or (0xBF shl 16) or (0x8A shl 8) or 0x5A
    private val FELINE_INNER: Int = (0xFF shl 24) or (0xE8 shl 16) or (0xA8 shl 8) or 0xA0
    private val LYNX_OUTER: Int = (0xFF shl 24) or (0x9B shl 16) or (0x70 shl 8) or 0x40
    private val LYNX_TUFT: Int = (0xFF shl 24) or (0x2E shl 16) or (0x1A shl 8) or 0x08
    private val ROUNDED_OUTER: Int = (0xFF shl 24) or (0xBF shl 16) or (0x8A shl 8) or 0x5A
    private val FLUFFY_OUTER: Int = (0xFF shl 24) or (0x9E shl 16) or (0x6A shl 8) or 0x38
    private val FLUFFY_INNER: Int = (0xFF shl 24) or (0xE0 shl 16) or (0xB0 shl 8) or 0xA0
    private val FLUFFY_FUR: Int = (0xFF shl 24) or (0x6A shl 16) or (0x3E shl 8) or 0x18
    private val FOX_OUTER: Int = (0xFF shl 24) or (0xD4 shl 16) or (0x58 shl 8) or 0x00
    private val FOX_INNER: Int = (0xFF shl 24) or (0xE8 shl 16) or (0xC0 shl 8) or 0x90
    private val WHITE: Int = (0xFF shl 24) or (0xFF shl 16) or (0xFF shl 8) or 0xFF
}
