// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.overlay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import it.marcelpetrick.catears.domain.EarAnchor
import it.marcelpetrick.catears.domain.EarMaterialSpec
import it.marcelpetrick.catears.domain.EarRenderStyleSpec
import it.marcelpetrick.catears.domain.EarStyle
import it.marcelpetrick.catears.domain.EarTint
import it.marcelpetrick.catears.domain.OverlayPlacement
import it.marcelpetrick.catears.domain.earRenderStyleSpec
import it.marcelpetrick.catears.domain.hueRotationMatrix
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bundled animated values driven by time and expression probabilities. */
private data class EarAnimState(
    val swayTime: Float,
    val twitchTime: Float,
    val leftTilt: Float,
    val rightTilt: Float,
    val yShiftFraction: Float,
    val leftWinkScale: Float,
    val rightWinkScale: Float,
)

@Composable
private fun rememberEarAnimState(placement: OverlayPlacement?): EarAnimState {
    val transition = rememberInfiniteTransition(label = "earSway")
    val swayTime by transition.animateFloat(
        0f,
        TWO_PI,
        infiniteRepeatable(tween(SWAY_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "swayTime",
    )
    val twitchTime by transition.animateFloat(
        0f,
        TWO_PI,
        infiniteRepeatable(tween(TWITCH_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "twitchTime",
    )
    val leftTilt by animateFloatAsState(
        if (placement != null) placement.leftEar.tiltDegrees * LEFT_TILT_FACTOR else 0f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "leftTilt",
    )
    val rightTilt by animateFloatAsState(
        if (placement != null) placement.rightEar.tiltDegrees * RIGHT_TILT_FACTOR else 0f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "rightTilt",
    )
    val smiling = placement?.smilingProbability ?: 0f
    val eyeOpen = placement?.eyeOpennessMean ?: 1f
    val leftEyeOpen = placement?.leftEyeOpenness ?: eyeOpen
    val rightEyeOpen = placement?.rightEyeOpenness ?: eyeOpen
    val smileY by animateFloatAsState(
        if (smiling > SMILE_THRESHOLD) -SMILE_PERK_FRACTION else 0f,
        spring(stiffness = Spring.StiffnessMediumLow),
        label = "smileY",
    )
    val wideEyeY by animateFloatAsState(
        if (eyeOpen > WIDE_EYE_THRESHOLD) -WIDE_EYE_PERK_FRACTION else 0f,
        spring(stiffness = Spring.StiffnessHigh),
        label = "wideEyeY",
    )
    val leftWinkScale = if (leftEyeOpen < WINK_THRESHOLD) WINK_SCALE else 1f
    val rightWinkScale = if (rightEyeOpen < WINK_THRESHOLD) WINK_SCALE else 1f
    val leftWink by animateFloatAsState(leftWinkScale, spring(stiffness = Spring.StiffnessMedium), label = "leftWink")
    val rightWink by animateFloatAsState(
        rightWinkScale,
        spring(stiffness = Spring.StiffnessMedium),
        label = "rightWink",
    )
    return EarAnimState(swayTime, twitchTime, leftTilt, rightTilt, smileY + wideEyeY, leftWink, rightWink)
}

/**
 * Transparent overlay that draws animated procedural cat ears for every placement in [placements].
 * Each face gets independently spring-animated ears keyed by its tracking ID.
 * Renders nothing when [placements] is empty.
 */
@Composable
fun CatEarOverlay(placements: List<OverlayPlacement>, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        placements.forEachIndexed { index, placement ->
            key(placement.trackingId ?: index) {
                SingleFaceEarOverlay(placement)
            }
        }
    }
}

@Composable
private fun SingleFaceEarOverlay(placement: OverlayPlacement) {
    val anim = rememberEarAnimState(placement)
    val style = placement.earStyle
    val tintPaint: Paint? = tintPaintCache[placement.tint]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                val leftEar = placement.leftEar.copy(
                    tiltDegrees = anim.leftTilt,
                    y = placement.leftEar.y + placement.leftEar.size * anim.yShiftFraction,
                    xScale = placement.leftEar.xScale * anim.leftWinkScale,
                )
                val rightEar = placement.rightEar.copy(
                    tiltDegrees = anim.rightTilt,
                    y = placement.rightEar.y + placement.rightEar.size * anim.yShiftFraction,
                    xScale = placement.rightEar.xScale * anim.rightWinkScale,
                )
                val drawEars: DrawScope.() -> Unit = {
                    drawEar(leftEar, style, anim.swayTime, anim.twitchTime)
                    drawEar(rightEar, style, anim.swayTime, anim.twitchTime)
                }
                if (tintPaint == null) {
                    drawEars()
                } else {
                    val margin = leftEar.size.coerceAtLeast(rightEar.size)
                    val bounds = Rect(
                        left = minOf(leftEar.x, rightEar.x) - margin,
                        top = minOf(leftEar.y, rightEar.y) - margin * 0.1f,
                        right = maxOf(leftEar.x, rightEar.x) + margin,
                        bottom = maxOf(leftEar.y, rightEar.y) + margin * 2f,
                    )
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(bounds, tintPaint)
                        drawEars()
                        canvas.restore()
                    }
                }
            },
    )
}

// --- expression animation constants
private const val SMILE_THRESHOLD = 0.85f
private const val SMILE_PERK_FRACTION = 0.20f
private const val WIDE_EYE_THRESHOLD = 0.90f
private const val WIDE_EYE_PERK_FRACTION = 0.12f
private const val WINK_THRESHOLD = 0.20f
private const val WINK_SCALE = 0.5f

// --- material finish constants
private const val SHADOW_HALF_WIDTH = 0.46f
private const val SHADOW_WIDTH = 0.92f
private const val SHADOW_TOP_RATIO = 0.68f
private const val SHADOW_HEIGHT = 0.36f
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
private const val MATERIAL_FUR_PHASE_STEP = 0.63f
private const val MATERIAL_FUR_SWAY_RATIO = 0.018f
private const val MATERIAL_FUR_LENGTH_RATIO = 0.12f
private const val MATERIAL_FUR_LIFT_RATIO = 0.035f
private const val MATERIAL_FUR_ALPHA = 0.56f
private const val MATERIAL_TUFT_FAN_DEG = 17f
private const val MATERIAL_TUFT_SWAY_DEG = 4f
private const val MATERIAL_TUFT_LENGTH_RATIO = 0.18f
private const val MATERIAL_TUFT_WIDTH_RATIO = 0.032f

private fun DrawScope.drawEar(anchor: EarAnchor, style: EarStyle, swayTime: Float, twitchTime: Float) {
    val cx = anchor.x
    val top = anchor.y
    val s = anchor.size
    val pivot = Offset(cx, top + s / 2f)
    val twitchAngle = sin(twitchTime) * TWITCH_AMPLITUDE * sin(twitchTime * TWITCH_FREQ_MOD)
    val spec = earRenderStyleSpec(style)
    rotate(degrees = anchor.tiltDegrees + twitchAngle, pivot = pivot) {
        scale(scaleX = anchor.xScale, scaleY = 1f, pivot = pivot) {
            drawSoftEarShadow(cx, top, s, spec.material)
            when (style) {
                EarStyle.CLASSIC -> drawClassicEar(cx, top, s, swayTime)
                EarStyle.SHARP_FELINE -> drawSharpFelineEar(cx, top, s, swayTime)
                EarStyle.ROUNDED_FELINE -> drawRoundedFelineEar(cx, top, s, swayTime)
                EarStyle.LYNX_TUFTED -> drawLynxTuftedEar(cx, top, s, swayTime)
                EarStyle.DENSE_FLUFFY -> drawDenseFluffyEar(cx, top, s, swayTime)
                EarStyle.CANINE_FLOPPY -> drawCanineFloppyEar(cx, top, s, swayTime)
                EarStyle.CANINE_PERKY -> drawCaninePerkyEar(cx, top, s, swayTime)
                EarStyle.RABBIT -> drawRabbitEar(cx, top, s)
                EarStyle.FOX -> drawFoxEar(cx, top, s, swayTime)
                EarStyle.BEAR -> drawBearEar(cx, top, s)
            }
            drawMaterialFinish(cx, top, s, spec, swayTime)
        }
    }
}

private fun DrawScope.drawSoftEarShadow(cx: Float, top: Float, s: Float, material: EarMaterialSpec) {
    drawOval(
        color = Color(material.shadowArgb),
        topLeft = Offset(cx - s * SHADOW_HALF_WIDTH, top + s * SHADOW_TOP_RATIO),
        size = Size(s * SHADOW_WIDTH, s * SHADOW_HEIGHT),
    )
}

private fun DrawScope.drawMaterialFinish(cx: Float, top: Float, s: Float, spec: EarRenderStyleSpec, swayTime: Float) {
    val material = spec.material
    val tip = Offset(cx + styleTipOffset(spec.style) * s, top + styleTipYOffset(spec.style) * s)
    val leftBase = Offset(cx - styleLeftBase(spec.style) * s, top + s)
    val rightBase = Offset(cx + styleRightBase(spec.style) * s, top + s)
    drawOuterRim(tip, leftBase, rightBase, s, material)
    drawInnerRosyGlaze(tip, leftBase, rightBase, s, material)
    drawFurTexture(tip, leftBase, rightBase, s, spec, swayTime)
    if (spec.supportsTufts) {
        drawMaterialTufts(tip, s, material, swayTime)
    }
}

private fun DrawScope.drawOuterRim(
    tip: Offset,
    leftBase: Offset,
    rightBase: Offset,
    s: Float,
    material: EarMaterialSpec,
) {
    val rimWidth = s * MATERIAL_RIM_WIDTH_RATIO
    drawLine(Color(material.outerRimArgb).copy(alpha = RIM_ALPHA), leftBase, tip, rimWidth, StrokeCap.Round)
    drawLine(Color(material.outerRimArgb).copy(alpha = RIM_ALPHA), rightBase, tip, rimWidth, StrokeCap.Round)
    drawLine(
        Color(material.outerHighlightArgb).copy(alpha = HIGHLIGHT_ALPHA),
        Offset(tip.x - s * HIGHLIGHT_TIP_X_OFFSET, tip.y + s * HIGHLIGHT_TIP_Y_OFFSET),
        Offset(leftBase.x + s * HIGHLIGHT_BASE_X_OFFSET, leftBase.y - s * HIGHLIGHT_BASE_Y_OFFSET),
        s * MATERIAL_HIGHLIGHT_WIDTH_RATIO,
        StrokeCap.Round,
    )
}

private fun DrawScope.drawInnerRosyGlaze(
    tip: Offset,
    leftBase: Offset,
    rightBase: Offset,
    s: Float,
    material: EarMaterialSpec,
) {
    val innerTop = Offset(
        x = tip.x,
        y = tip.y + s * MATERIAL_INNER_TOP_RATIO,
    )
    val innerLeft = Offset(
        x = leftBase.x * MATERIAL_INNER_BASE_BLEND + tip.x * (1f - MATERIAL_INNER_BASE_BLEND),
        y = leftBase.y - s * MATERIAL_INNER_BASE_LIFT_RATIO,
    )
    val innerRight = Offset(
        x = rightBase.x * MATERIAL_INNER_BASE_BLEND + tip.x * (1f - MATERIAL_INNER_BASE_BLEND),
        y = rightBase.y - s * MATERIAL_INNER_BASE_LIFT_RATIO,
    )
    val inner = Path().apply {
        moveTo(innerTop.x, innerTop.y)
        lineTo(innerLeft.x, innerLeft.y)
        lineTo(innerRight.x, innerRight.y)
        close()
    }
    drawPath(
        inner,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(material.innerHighlightArgb).copy(alpha = INNER_GLAZE_TOP_ALPHA),
                Color(material.innerBaseArgb).copy(alpha = INNER_GLAZE_BOTTOM_ALPHA),
            ),
            start = Offset(tip.x, tip.y),
            end = Offset(tip.x, leftBase.y),
        ),
    )
}

private fun DrawScope.drawFurTexture(
    tip: Offset,
    leftBase: Offset,
    rightBase: Offset,
    s: Float,
    spec: EarRenderStyleSpec,
    swayTime: Float,
) {
    val stroke = s * MATERIAL_FUR_WIDTH_RATIO
    repeat(spec.furStrokeCount) { index ->
        val fraction = (index + 1f) / (spec.furStrokeCount + 1f)
        val edgeStart = if (index % 2 == 0) leftBase else rightBase
        val edgePoint = lerpOffset(edgeStart, tip, fraction)
        val inward = lerpOffset(edgePoint, Offset(tip.x, edgePoint.y), MATERIAL_FUR_INWARD_BLEND)
        val sway = sin(swayTime + index * MATERIAL_FUR_PHASE_STEP) * s * MATERIAL_FUR_SWAY_RATIO
        val end = Offset(
            x =
            inward.x +
                if (index % 2 == 0) s * MATERIAL_FUR_LENGTH_RATIO + sway else -s * MATERIAL_FUR_LENGTH_RATIO + sway,
            y = inward.y - s * MATERIAL_FUR_LIFT_RATIO,
        )
        drawLine(
            color = Color(spec.material.outerHighlightArgb).copy(alpha = MATERIAL_FUR_ALPHA),
            start = edgePoint,
            end = end,
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawMaterialTufts(tip: Offset, s: Float, material: EarMaterialSpec, swayTime: Float) {
    for (i in -1..1) {
        val angle = Math.toRadians((MATERIAL_TUFT_FAN_DEG * i + sin(swayTime + i) * MATERIAL_TUFT_SWAY_DEG).toDouble())
            .toFloat()
        drawLine(
            color = Color(material.outerRimArgb),
            start = tip,
            end = Offset(
                x = tip.x + sin(angle) * s * MATERIAL_TUFT_LENGTH_RATIO,
                y = tip.y - cos(angle) * s * MATERIAL_TUFT_LENGTH_RATIO,
            ),
            strokeWidth = s * MATERIAL_TUFT_WIDTH_RATIO,
            cap = StrokeCap.Round,
        )
    }
}

private fun lerpOffset(from: Offset, to: Offset, fraction: Float): Offset = Offset(
    x = from.x + (to.x - from.x) * fraction,
    y = from.y + (to.y - from.y) * fraction,
)

private fun styleTipOffset(style: EarStyle): Float = when (style) {
    EarStyle.SHARP_FELINE,
    EarStyle.LYNX_TUFTED,
    EarStyle.FOX,
    -> FELINE_TIP_OFFSET_X

    else -> 0f
}

private fun styleTipYOffset(style: EarStyle): Float = when (style) {
    EarStyle.CANINE_PERKY -> PERKY_TIP_Y
    else -> 0f
}

private fun styleLeftBase(style: EarStyle): Float = when (style) {
    EarStyle.SHARP_FELINE -> FELINE_BASE_LEFT
    EarStyle.LYNX_TUFTED -> LYNX_BASE_LEFT
    EarStyle.DENSE_FLUFFY -> FLUFFY_LEFT_BASE
    EarStyle.FOX -> FOX_HALF_BASE
    EarStyle.CANINE_PERKY -> PERKY_HALF_BASE
    EarStyle.RABBIT -> RABBIT_HALF_WIDTH
    EarStyle.BEAR -> BEAR_RADIUS_RATIO
    else -> OUTER_HALF_BASE
}

private fun styleRightBase(style: EarStyle): Float = when (style) {
    EarStyle.SHARP_FELINE -> FELINE_BASE_RIGHT
    EarStyle.LYNX_TUFTED -> LYNX_BASE_RIGHT
    EarStyle.DENSE_FLUFFY -> FLUFFY_RIGHT_BASE
    EarStyle.FOX -> FOX_RIGHT_BASE
    EarStyle.CANINE_PERKY -> PERKY_HALF_BASE
    EarStyle.RABBIT -> RABBIT_HALF_WIDTH
    EarStyle.BEAR -> BEAR_RADIUS_RATIO
    else -> OUTER_HALF_BASE
}

// --- 1 CLASSIC

private fun DrawScope.drawClassicEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    val hb = s * OUTER_HALF_BASE
    drawPath(
        Path().apply {
            moveTo(cx, top)
            lineTo(cx - hb, top + s)
            lineTo(cx + hb, top + s)
            close()
        },
        color = EAR_COLOR,
    )
    val ih = s * INNER_HALF_BASE
    val iTop = top + s * INNER_TOP_OFFSET
    val iBot = top + s * INNER_BOTTOM_OFFSET
    drawPath(
        Path().apply {
            moveTo(cx, iTop)
            lineTo(cx - ih, iBot)
            lineTo(cx + ih, iBot)
            close()
        },
        color = INNER_EAR_COLOR,
    )
    val sw = s * STRAND_WIDTH_RATIO
    STRAND_PHASES.forEachIndexed { i, phase ->
        val bx = cx + (STRAND_X_FRACTIONS[i] - 0.5f) * s * OUTER_HALF_BASE * 2f
        val by = top + s * STRAND_BASE_Y_RATIO
        val tx = bx + sin(swayTime + phase) * s * STRAND_SWAY_RATIO
        val ty = top + s * STRAND_TIP_Y_RATIO + cos(swayTime * STRAND_BOB_FREQ + phase) * s * STRAND_BOB_RATIO
        drawLine(FUR_COLOR, Offset(bx, by), Offset(tx, ty), sw, StrokeCap.Round)
    }
}

// --- 2 SHARP FELINE

private fun DrawScope.drawSharpFelineEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    val tipX = cx + s * FELINE_TIP_OFFSET_X
    val tipY = top
    val outer = Path().apply {
        moveTo(tipX, tipY)
        lineTo(cx - s * FELINE_BASE_LEFT, top + s)
        lineTo(cx + s * FELINE_BASE_RIGHT, top + s)
        close()
    }
    drawPath(
        outer,
        brush = Brush.radialGradient(
            listOf(FELINE_CENTER_COLOR, FELINE_RIM_COLOR),
            Offset(cx, top + s * 0.65f),
            s * 0.55f,
        ),
    )
    val inner = Path().apply {
        moveTo(tipX, top + s * FELINE_INNER_TOP)
        lineTo(cx - s * FELINE_INNER_HALF, top + s * FELINE_INNER_BOTTOM)
        lineTo(cx + s * FELINE_INNER_HALF, top + s * FELINE_INNER_BOTTOM)
        close()
    }
    drawPath(inner, color = FELINE_INNER_COLOR)
    val ts = s * TUFT_STROKE
    for (i in -1..1) {
        val angle = Math.toRadians((TUFT_FAN_DEG * i).toDouble()).toFloat()
        val sway = sin(swayTime + i * 1.2f) * TUFT_SWAY_DEG
        val rad = angle + Math.toRadians(sway.toDouble()).toFloat()
        val len = s * TUFT_LENGTH
        drawLine(
            FELINE_TUFT_COLOR,
            Offset(tipX, tipY),
            Offset(tipX + sin(rad) * len, tipY - cos(rad) * len),
            ts,
            StrokeCap.Round,
        )
    }
}

// --- 3 ROUNDED FELINE

private fun DrawScope.drawRoundedFelineEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    val outer = Path().apply {
        moveTo(cx - s * 0.38f, top + s)
        cubicTo(cx - s * 0.52f, top + s * 0.50f, cx - s * 0.08f, top + s * 0.05f, cx, top)
        cubicTo(cx + s * 0.32f, top + s * 0.08f, cx + s * 0.28f, top + s * 0.60f, cx + s * 0.24f, top + s)
        close()
    }
    drawPath(
        outer,
        brush = Brush.linearGradient(
            listOf(ROUNDED_CENTER_COLOR, ROUNDED_RIM_COLOR),
            Offset(cx, top + s * 0.15f),
            Offset(cx, top + s),
        ),
    )
    val inner = Path().apply {
        moveTo(cx - s * 0.18f, top + s * 0.92f)
        cubicTo(cx - s * 0.24f, top + s * 0.50f, cx - s * 0.04f, top + s * 0.18f, cx, top + s * 0.10f)
        cubicTo(cx + s * 0.14f, top + s * 0.18f, cx + s * 0.14f, top + s * 0.50f, cx + s * 0.12f, top + s * 0.92f)
        close()
    }
    drawPath(inner, color = FELINE_INNER_COLOR)
    val ts = s * TUFT_STROKE
    for (i in -1..0) {
        val sway = sin(swayTime + i * 1.5f) * s * 0.04f
        drawLine(
            FELINE_TUFT_COLOR,
            Offset(cx + i * s * 0.03f, top),
            Offset(cx + i * s * 0.03f + sway, top - s * 0.12f),
            ts,
            StrokeCap.Round,
        )
    }
}

// --- 4 LYNX TUFTED

private fun DrawScope.drawLynxTuftedEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    val tipX = cx + s * FELINE_TIP_OFFSET_X
    val tipY = top
    val outer = Path().apply {
        moveTo(tipX, tipY)
        lineTo(cx - s * LYNX_BASE_LEFT, top + s)
        lineTo(cx + s * LYNX_BASE_RIGHT, top + s)
        close()
    }
    drawPath(outer, color = LYNX_OUTER_COLOR)
    val inner = Path().apply {
        moveTo(tipX, top + s * FELINE_INNER_TOP)
        lineTo(cx - s * FELINE_INNER_HALF, top + s * FELINE_INNER_BOTTOM)
        lineTo(cx + s * FELINE_INNER_HALF, top + s * FELINE_INNER_BOTTOM)
        close()
    }
    drawPath(inner, color = FELINE_INNER_COLOR)
    val ts = s * TUFT_STROKE * 1.3f
    val fanAngles = floatArrayOf(-24f, -16f, -8f, 0f, 8f, 16f, 24f)
    LYNX_TUFT_PHASES.forEachIndexed { i, phase ->
        val rad = Math.toRadians(
            (fanAngles[i] + sin(swayTime + phase) * LYNX_SWAY_DEG).toDouble(),
        ).toFloat()
        val len = s * LYNX_TUFT_LENGTH
        drawLine(
            LYNX_TUFT_COLOR,
            Offset(tipX, tipY),
            Offset(tipX + sin(rad) * len, tipY - cos(rad) * len),
            ts,
            StrokeCap.Round,
        )
    }
}

// --- 5 DENSE FLUFFY

private fun DrawScope.drawDenseFluffyEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    val halfBase = s * 0.48f
    val outer = Path().apply {
        moveTo(cx, top)
        lineTo(cx - halfBase, top + s)
        lineTo(cx + halfBase * 0.85f, top + s)
        close()
    }
    drawPath(outer, color = FLUFFY_OUTER_COLOR)
    val ih = s * 0.26f
    val inner = Path().apply {
        moveTo(cx, top + s * 0.20f)
        lineTo(cx - ih, top + s * 0.88f)
        lineTo(cx + ih, top + s * 0.88f)
        close()
    }
    drawPath(inner, color = FLUFFY_INNER_COLOR)
    val sw = s * 0.045f
    for (i in 0..7) {
        val frac = i / 7f
        val ex = cx - halfBase * frac
        val ey = top + s * frac
        val sway = sin(swayTime + FLUFFY_PHASES[i]) * s * 0.07f
        drawLine(FLUFFY_FUR_COLOR, Offset(ex, ey), Offset(ex - s * 0.10f + sway, ey - s * 0.08f), sw, StrokeCap.Round)
    }
}

// --- 6 CANINE FLOPPY

private fun DrawScope.drawCanineFloppyEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    val flapDx = s * FLOPPY_FLAP_DX
    val flapDy = s * FLOPPY_FLAP_DY
    val swayAngle = sin(swayTime * FLOPPY_FREQ) * FLOPPY_SWAY_DEG
    val outer = Path().apply {
        moveTo(cx - s * 0.15f, top)
        cubicTo(cx - flapDx, top, cx - flapDx, top + flapDy * 0.6f, cx - flapDx * 0.5f, top + flapDy)
        cubicTo(cx, top + flapDy * 1.05f, cx + s * 0.1f, top + flapDy * 0.4f, cx + s * 0.08f, top)
        close()
    }
    val inner = Path().apply {
        moveTo(cx - s * 0.12f, top + s * 0.15f)
        cubicTo(
            cx - flapDx * 0.8f,
            top + s * 0.20f,
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
    rotate(degrees = swayAngle, pivot = Offset(cx, top)) {
        drawPath(outer, color = FLOPPY_OUTER_COLOR)
        drawPath(inner, color = FLOPPY_INNER_COLOR)
    }
}

// --- 7 CANINE PERKY

private fun DrawScope.drawCaninePerkyEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    val halfBase = s * PERKY_HALF_BASE
    val tipY = top + s * PERKY_TIP_Y
    val outer = Path().apply {
        moveTo(cx - halfBase, top + s)
        lineTo(cx + halfBase, top + s)
        lineTo(cx + halfBase * 0.4f, tipY + s * 0.08f)
        arcTo(
            Rect(cx - halfBase * 0.4f, tipY - s * 0.10f, cx + halfBase * 0.4f, tipY + s * 0.18f),
            0f,
            -180f,
            false,
        )
        lineTo(cx - halfBase * 0.4f, tipY + s * 0.08f)
        close()
    }
    drawPath(outer, color = PERKY_OUTER_COLOR)
    val inner = Path().apply {
        moveTo(cx - halfBase * 0.55f, top + s * 0.95f)
        lineTo(cx + halfBase * 0.55f, top + s * 0.95f)
        lineTo(cx + halfBase * 0.2f, tipY + s * 0.12f)
        lineTo(cx - halfBase * 0.2f, tipY + s * 0.12f)
        close()
    }
    drawPath(inner, color = PERKY_INNER_COLOR)
    val hs = s * 0.018f
    for (i in 0..2) {
        val yOff = top + s * (0.35f + i * 0.18f)
        val xSpan = halfBase * (0.7f - i * 0.15f)
        drawLine(PERKY_HATCH_COLOR, Offset(cx - xSpan, yOff), Offset(cx + xSpan, yOff + s * 0.12f), hs)
    }
    val baseSway = sin(swayTime * 0.8f) * s * 0.015f
    drawLine(
        PERKY_HATCH_COLOR,
        Offset(cx - halfBase + baseSway, top + s),
        Offset(cx + halfBase + baseSway, top + s),
        s * 0.03f,
        StrokeCap.Round,
    )
}

// --- 8 RABBIT

private fun DrawScope.drawRabbitEar(cx: Float, top: Float, s: Float) {
    val halfW = s * 0.18f
    drawOval(RABBIT_OUTER_COLOR, Offset(cx - halfW, top), Size(halfW * 2f, s))
    drawOval(RABBIT_INNER_COLOR, Offset(cx - halfW * 0.55f, top + s * 0.06f), Size(halfW * 1.1f, s * 0.86f))
}

// --- 9 FOX

private fun DrawScope.drawFoxEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    val halfBase = s * 0.32f
    val tipX = cx + s * 0.05f
    val outer = Path().apply {
        moveTo(tipX, top)
        lineTo(cx - halfBase, top + s)
        lineTo(cx + halfBase * 0.7f, top + s)
        close()
    }
    drawPath(outer, color = FOX_OUTER_COLOR)
    val inner = Path().apply {
        moveTo(tipX, top + s * 0.14f)
        lineTo(cx - halfBase * 0.52f, top + s * 0.88f)
        lineTo(cx + halfBase * 0.4f, top + s * 0.88f)
        close()
    }
    drawPath(inner, color = FOX_INNER_COLOR)
    drawCircle(Color.White, s * 0.07f, Offset(tipX, top))
    val ts = s * 0.04f
    for (i in -1..0) {
        val sway = sin(swayTime + i * 1.1f) * s * 0.03f
        drawLine(FOX_TUFT_COLOR, Offset(tipX, top), Offset(tipX + sway, top - s * 0.10f), ts, StrokeCap.Round)
    }
}

// --- 10 BEAR

private fun DrawScope.drawBearEar(cx: Float, top: Float, s: Float) {
    val radius = s * 0.32f
    val centerY = top + radius
    drawCircle(BEAR_OUTER_COLOR, radius, Offset(cx, centerY))
    drawCircle(BEAR_INNER_COLOR, radius * 0.55f, Offset(cx, centerY))
}

// --- animation timing
private const val SWAY_PERIOD_MS = 1250
private const val TWITCH_PERIOD_MS = 5000
private const val TWITCH_AMPLITUDE = 4f
private const val TWITCH_FREQ_MOD = 3f
private val TWO_PI = (2.0 * PI).toFloat()

// --- spring tilt factors
private const val LEFT_TILT_FACTOR = 0.6f
private const val RIGHT_TILT_FACTOR = 1.0f

// --- CLASSIC constants
private const val OUTER_HALF_BASE = 0.42f
private const val INNER_HALF_BASE = 0.24f
private const val INNER_TOP_OFFSET = 0.28f
private const val INNER_BOTTOM_OFFSET = 0.78f
private const val STRAND_WIDTH_RATIO = 0.04f
private const val STRAND_BASE_Y_RATIO = 0.15f
private const val STRAND_TIP_Y_RATIO = -0.08f
private const val STRAND_SWAY_RATIO = 0.06f
private const val STRAND_BOB_RATIO = 0.03f
private const val STRAND_BOB_FREQ = 0.7f
private val STRAND_X_FRACTIONS = floatArrayOf(0.22f, 0.38f, 0.50f, 0.62f, 0.78f)
private val STRAND_PHASES = floatArrayOf(0.0f, 1.2f, 2.4f, 0.7f, 1.9f)

// --- SHARP / ROUNDED FELINE constants
private const val FELINE_TIP_OFFSET_X = 0.08f
private const val FELINE_BASE_LEFT = 0.44f
private const val FELINE_BASE_RIGHT = 0.30f
private const val FELINE_INNER_TOP = 0.26f
private const val FELINE_INNER_BOTTOM = 0.76f
private const val FELINE_INNER_HALF = 0.22f
private const val TUFT_STROKE = 0.05f
private const val TUFT_FAN_DEG = 18f
private const val TUFT_SWAY_DEG = 6f
private const val TUFT_LENGTH = 0.22f

// --- LYNX constants
private const val LYNX_BASE_LEFT = 0.48f
private const val LYNX_BASE_RIGHT = 0.34f
private const val LYNX_TUFT_LENGTH = 0.36f
private const val LYNX_SWAY_DEG = 10f
private val LYNX_TUFT_PHASES = floatArrayOf(0.0f, 0.9f, 1.8f, 2.7f, 0.4f, 1.3f, 2.2f)

// --- DENSE FLUFFY constants
private val FLUFFY_PHASES = floatArrayOf(0.0f, 0.8f, 1.6f, 2.4f, 0.4f, 1.2f, 2.0f, 0.6f)

// --- CANINE FLOPPY constants
private const val FLOPPY_FLAP_DX = 0.55f
private const val FLOPPY_FLAP_DY = 1.15f
private const val FLOPPY_FREQ = 0.5f
private const val FLOPPY_SWAY_DEG = 4f

// --- CANINE PERKY constants
private const val PERKY_HALF_BASE = 0.38f
private const val PERKY_TIP_Y = 0.20f

// --- colours
private val EAR_COLOR = Color(0xFF8B5E3C)
private val INNER_EAR_COLOR = Color(0xFFE8A0A0)
private val FUR_COLOR = Color(0xFFD4A07A)

private val FELINE_RIM_COLOR = Color(0xFF7A4E28)
private val FELINE_CENTER_COLOR = Color(0xFFBF8A5A)
private val FELINE_INNER_COLOR = Color(0xFFE8A8A0)
private val FELINE_TUFT_COLOR = Color(0xFF4A2C10)

private val ROUNDED_RIM_COLOR = Color(0xFF7A4E28)
private val ROUNDED_CENTER_COLOR = Color(0xFFBF8A5A)

private val LYNX_OUTER_COLOR = Color(0xFF9B7040)
private val LYNX_TUFT_COLOR = Color(0xFF2E1A08)

private val FLUFFY_OUTER_COLOR = Color(0xFF9E6A38)
private val FLUFFY_INNER_COLOR = Color(0xFFE0B0A0)
private val FLUFFY_FUR_COLOR = Color(0xFF6A3E18)

private val FLOPPY_OUTER_COLOR = Color(0xFFB8864A)
private val FLOPPY_INNER_COLOR = Color(0xFFE8C8A8)

private val PERKY_OUTER_COLOR = Color(0xFFD4B896)
private val PERKY_INNER_COLOR = Color(0xFFE8A090)
private val PERKY_HATCH_COLOR = Color(0xFF9A7848)

private val RABBIT_OUTER_COLOR = Color(0xFFF0ECEC)
private val RABBIT_INNER_COLOR = Color(0xFFE890B0)

private val FOX_OUTER_COLOR = Color(0xFFD45800)
private val FOX_INNER_COLOR = Color(0xFFE8C090)
private val FOX_TUFT_COLOR = Color(0xFF3A1800)

private val BEAR_OUTER_COLOR = Color(0xFF3A2010)
private val BEAR_INNER_COLOR = Color(0xFF7A4020)

// One Paint per non-natural tint; created once so the hot draw path allocates nothing.
private val tintPaintCache: Map<EarTint, Paint> by lazy {
    EarTint.entries
        .filter { it != EarTint.NATURAL }
        .associateWith { tint ->
            Paint().apply {
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix(hueRotationMatrix(tint.hueDegrees)),
                )
            }
        }
}
