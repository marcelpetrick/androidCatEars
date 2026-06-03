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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import it.marcelpetrick.catears.domain.EarAnchor
import it.marcelpetrick.catears.domain.EarStyle
import it.marcelpetrick.catears.domain.OverlayPlacement
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Transparent overlay that draws two animated, procedural cat ears at the positions described by
 * [placement]. Sits on top of the camera preview inside a [Box] with fillMaxSize.
 * When [placement] is null (no face detected) nothing is rendered.
 */
@Composable
fun CatEarOverlay(placement: OverlayPlacement?, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "earSway")
    val swayTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SWAY_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "swayTime",
    )
    val twitchTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = TWITCH_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "twitchTime",
    )

    val leftTilt by animateFloatAsState(
        targetValue = if (placement != null) placement.leftEar.tiltDegrees * LEFT_TILT_FACTOR else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "leftTilt",
    )
    val rightTilt by animateFloatAsState(
        targetValue = if (placement != null) placement.rightEar.tiltDegrees * RIGHT_TILT_FACTOR else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "rightTilt",
    )

    val style = placement?.earStyle ?: EarStyle.CLASSIC

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (placement != null) {
                    drawEar(placement.leftEar.copy(tiltDegrees = leftTilt), style, swayTime, twitchTime)
                    drawEar(placement.rightEar.copy(tiltDegrees = rightTilt), style, swayTime, twitchTime)
                }
            },
    )
}

private fun DrawScope.drawEar(anchor: EarAnchor, style: EarStyle, swayTime: Float, twitchTime: Float) {
    val cx = anchor.x
    val top = anchor.y
    val s = anchor.size
    val pivot = Offset(cx, top + s / 2f)
    val twitchAngle = sin(twitchTime) * TWITCH_AMPLITUDE * sin(twitchTime * TWITCH_FREQ_MOD)

    rotate(degrees = anchor.tiltDegrees + twitchAngle, pivot = pivot) {
        scale(scaleX = anchor.xScale, scaleY = 1f, pivot = pivot) {
            when (style) {
                EarStyle.CLASSIC -> drawClassicEar(cx, top, s, swayTime)
                EarStyle.SHARP_FELINE -> drawSharpFelineEar(cx, top, s, swayTime)
                EarStyle.LYNX_TUFTED -> drawLynxTuftedEar(cx, top, s, swayTime)
                EarStyle.CANINE_FLOPPY -> drawCanineFloppyEar(cx, top, s, swayTime)
                EarStyle.CANINE_PERKY -> drawCaninePerkyEar(cx, top, s, swayTime)
            }
        }
    }
}

// ─── CLASSIC ────────────────────────────────────────────────────────────────

private fun DrawScope.drawClassicEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    drawOuterEar(cx, top, s)
    drawInnerEar(cx, top, s)
    drawFurStrands(cx, top, s, swayTime)
}

private fun DrawScope.drawOuterEar(cx: Float, top: Float, s: Float) {
    val halfBase = s * OUTER_HALF_BASE
    val path = Path().apply {
        moveTo(cx, top)
        lineTo(cx - halfBase, top + s)
        lineTo(cx + halfBase, top + s)
        close()
    }
    drawPath(path, color = EAR_COLOR)
}

private fun DrawScope.drawInnerEar(cx: Float, top: Float, s: Float) {
    val halfBase = s * INNER_HALF_BASE
    val innerTop = top + s * INNER_TOP_OFFSET
    val innerBottom = top + s * INNER_BOTTOM_OFFSET
    val path = Path().apply {
        moveTo(cx, innerTop)
        lineTo(cx - halfBase, innerBottom)
        lineTo(cx + halfBase, innerBottom)
        close()
    }
    drawPath(path, color = INNER_EAR_COLOR)
}

private fun DrawScope.drawFurStrands(cx: Float, top: Float, s: Float, swayTime: Float) {
    val strokeWidth = s * STRAND_WIDTH_RATIO
    STRAND_PHASES.forEachIndexed { i, phase ->
        val fractionAlongBase = STRAND_X_FRACTIONS[i]
        val baseX = cx + (fractionAlongBase - 0.5f) * s * OUTER_HALF_BASE * 2f
        val baseY = top + s * STRAND_BASE_Y_RATIO
        val tipSway = sin(swayTime + phase).toFloat() * s * STRAND_SWAY_RATIO
        val tipBobble = cos(swayTime * STRAND_BOB_FREQ + phase).toFloat() * s * STRAND_BOB_RATIO
        val tipX = baseX + tipSway
        val tipY = top + s * STRAND_TIP_Y_RATIO + tipBobble
        drawLine(
            color = FUR_COLOR,
            start = Offset(baseX, baseY),
            end = Offset(tipX, tipY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

// ─── SHARP FELINE ────────────────────────────────────────────────────────────

private fun DrawScope.drawSharpFelineEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    val tipX = cx + s * FELINE_TIP_OFFSET_X
    val tipY = top
    val baseLeft = Offset(cx - s * FELINE_BASE_LEFT, top + s)
    val baseRight = Offset(cx + s * FELINE_BASE_RIGHT, top + s)

    // Outer ear — gradient fill via solid + lighter centre overlay
    val outerPath = Path().apply {
        moveTo(tipX, tipY)
        lineTo(baseLeft.x, baseLeft.y)
        lineTo(baseRight.x, baseRight.y)
        close()
    }
    drawPath(
        outerPath,
        brush = Brush.radialGradient(
            colors = listOf(FELINE_CENTER_COLOR, FELINE_RIM_COLOR),
            center = Offset(cx, top + s * 0.65f),
            radius = s * 0.55f,
        ),
    )

    // Inner ear — inset pink
    val innerPath = Path().apply {
        moveTo(tipX, top + s * FELINE_INNER_TOP)
        lineTo(cx - s * FELINE_INNER_HALF, top + s * FELINE_INNER_BOTTOM)
        lineTo(cx + s * FELINE_INNER_HALF, top + s * FELINE_INNER_BOTTOM)
        close()
    }
    drawPath(innerPath, color = FELINE_INNER_COLOR)

    // Tip tufts (3 short animated strands fanning from apex)
    val tuftStroke = s * TUFT_STROKE
    for (i in -1..1) {
        val angle = Math.toRadians((TUFT_FAN_DEG * i).toDouble()).toFloat()
        val sway = sin(swayTime + i * 1.2f).toFloat() * TUFT_SWAY_DEG
        val totalAngle = angle + Math.toRadians(sway.toDouble()).toFloat()
        val len = s * TUFT_LENGTH
        drawLine(
            color = FELINE_TUFT_COLOR,
            start = Offset(tipX, tipY),
            end = Offset(tipX + sin(totalAngle) * len, tipY - cos(totalAngle) * len),
            strokeWidth = tuftStroke,
            cap = StrokeCap.Round,
        )
    }
}

// ─── LYNX TUFTED ─────────────────────────────────────────────────────────────

private fun DrawScope.drawLynxTuftedEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    // Same outer shape as sharp feline but wider base
    val tipX = cx + s * FELINE_TIP_OFFSET_X
    val tipY = top
    val outerPath = Path().apply {
        moveTo(tipX, tipY)
        lineTo(cx - s * LYNX_BASE_LEFT, top + s)
        lineTo(cx + s * LYNX_BASE_RIGHT, top + s)
        close()
    }
    drawPath(outerPath, color = LYNX_OUTER_COLOR)

    val innerPath = Path().apply {
        moveTo(tipX, top + s * FELINE_INNER_TOP)
        lineTo(cx - s * FELINE_INNER_HALF, top + s * FELINE_INNER_BOTTOM)
        lineTo(cx + s * FELINE_INNER_HALF, top + s * FELINE_INNER_BOTTOM)
        close()
    }
    drawPath(innerPath, color = FELINE_INNER_COLOR)

    // 7 long dark tufts projecting from the tip with high sway amplitude
    val tuftStroke = s * TUFT_STROKE * 1.3f
    val fanAngles = floatArrayOf(-24f, -16f, -8f, 0f, 8f, 16f, 24f)
    LYNX_TUFT_PHASES.forEachIndexed { i, phase ->
        val baseDeg = fanAngles[i]
        val sway = sin(swayTime + phase).toFloat() * LYNX_SWAY_DEG
        val rad = Math.toRadians((baseDeg + sway).toDouble()).toFloat()
        val len = s * LYNX_TUFT_LENGTH
        drawLine(
            color = LYNX_TUFT_COLOR,
            start = Offset(tipX, tipY),
            end = Offset(tipX + sin(rad) * len, tipY - cos(rad) * len),
            strokeWidth = tuftStroke,
            cap = StrokeCap.Round,
        )
    }
}

// ─── CANINE FLOPPY ───────────────────────────────────────────────────────────

private fun DrawScope.drawCanineFloppyEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    // Teardrop hanging to the outer-left: hinge at anchor top, flap extends down-left.
    val flapDx = s * FLOPPY_FLAP_DX
    val flapDy = s * FLOPPY_FLAP_DY
    // Pendulum sway on the whole flap
    val swayAngle = sin(swayTime * FLOPPY_FREQ) * FLOPPY_SWAY_DEG

    val outerPath = Path().apply {
        moveTo(cx - s * 0.15f, top) // hinge top-left
        cubicTo(
            cx - flapDx,
            top, // control: left
            cx - flapDx,
            top + flapDy * 0.6f, // control: lower-left
            cx - flapDx * 0.5f,
            top + flapDy, // flap bottom
        )
        cubicTo(
            cx,
            top + flapDy * 1.05f, // control: bottom-right
            cx + s * 0.1f,
            top + flapDy * 0.4f,
            cx + s * 0.08f,
            top, // back to hinge right
        )
        close()
    }

    rotate(degrees = swayAngle, pivot = Offset(cx, top)) {
        drawPath(outerPath, color = FLOPPY_OUTER_COLOR)
        // inner ear crescent
        val innerPath = Path().apply {
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
        drawPath(innerPath, color = FLOPPY_INNER_COLOR)
    }
}

// ─── CANINE PERKY ────────────────────────────────────────────────────────────

private fun DrawScope.drawCaninePerkyEar(cx: Float, top: Float, s: Float, swayTime: Float) {
    // Short wide triangle with rounded arc cap at the top
    val halfBase = s * PERKY_HALF_BASE
    val tipY = top + s * PERKY_TIP_Y // tip is lower than full height

    val outerPath = Path().apply {
        moveTo(cx - halfBase, top + s)
        lineTo(cx + halfBase, top + s)
        lineTo(cx + halfBase * 0.4f, tipY + s * 0.08f)
        arcTo(
            androidx.compose.ui.geometry.Rect(
                cx - halfBase * 0.4f,
                tipY - s * 0.10f,
                cx + halfBase * 0.4f,
                tipY + s * 0.18f,
            ),
            startAngleDegrees = 0f,
            sweepAngleDegrees = -180f,
            forceMoveTo = false,
        )
        lineTo(cx - halfBase * 0.4f, tipY + s * 0.08f)
        close()
    }
    drawPath(outerPath, color = PERKY_OUTER_COLOR)

    // Inner fill
    val innerPath = Path().apply {
        moveTo(cx - halfBase * 0.55f, top + s * 0.95f)
        lineTo(cx + halfBase * 0.55f, top + s * 0.95f)
        lineTo(cx + halfBase * 0.2f, tipY + s * 0.12f)
        lineTo(cx - halfBase * 0.2f, tipY + s * 0.12f)
        close()
    }
    drawPath(innerPath, color = PERKY_INNER_COLOR)

    // Cross-hatch texture strokes on outer surface
    val hatchStroke = s * 0.018f
    for (i in 0..2) {
        val yOff = top + s * (0.35f + i * 0.18f)
        val xSpan = halfBase * (0.7f - i * 0.15f)
        drawLine(
            color = PERKY_HATCH_COLOR,
            start = Offset(cx - xSpan, yOff),
            end = Offset(cx + xSpan, yOff + s * 0.12f),
            strokeWidth = hatchStroke,
        )
    }

    // Subtle fur sway on the base edge
    val baseFurSway = sin(swayTime * 0.8f).toFloat() * s * 0.015f
    drawLine(
        color = PERKY_HATCH_COLOR,
        start = Offset(cx - halfBase + baseFurSway, top + s),
        end = Offset(cx + halfBase + baseFurSway, top + s),
        strokeWidth = s * 0.03f,
        cap = StrokeCap.Round,
    )
}

// ─── animation timing ────────────────────────────────────────────────────────
private const val SWAY_PERIOD_MS = 1250
private const val TWITCH_PERIOD_MS = 5000
private const val TWITCH_AMPLITUDE = 4f
private const val TWITCH_FREQ_MOD = 3f
private val TWO_PI = (2.0 * PI).toFloat()

// ─── spring tilt factors ─────────────────────────────────────────────────────
private const val LEFT_TILT_FACTOR = 0.6f
private const val RIGHT_TILT_FACTOR = 1.0f

// ─── CLASSIC geometry constants ───────────────────────────────────────────────
private const val OUTER_HALF_BASE = 0.42f
private const val INNER_HALF_BASE = 0.24f
private const val INNER_TOP_OFFSET = 0.28f
private const val INNER_BOTTOM_OFFSET = 0.78f

// ─── CLASSIC fur strand constants ─────────────────────────────────────────────
private const val STRAND_WIDTH_RATIO = 0.04f
private const val STRAND_BASE_Y_RATIO = 0.15f
private const val STRAND_TIP_Y_RATIO = -0.08f
private const val STRAND_SWAY_RATIO = 0.06f
private const val STRAND_BOB_RATIO = 0.03f
private const val STRAND_BOB_FREQ = 0.7f
private val STRAND_X_FRACTIONS = floatArrayOf(0.22f, 0.38f, 0.50f, 0.62f, 0.78f)
private val STRAND_PHASES = floatArrayOf(0.0f, 1.2f, 2.4f, 0.7f, 1.9f)

// ─── SHARP FELINE constants ───────────────────────────────────────────────────
private const val FELINE_TIP_OFFSET_X = 0.08f // tip leans slightly outward
private const val FELINE_BASE_LEFT = 0.44f
private const val FELINE_BASE_RIGHT = 0.30f
private const val FELINE_INNER_TOP = 0.26f
private const val FELINE_INNER_BOTTOM = 0.76f
private const val FELINE_INNER_HALF = 0.22f
private const val TUFT_STROKE = 0.05f
private const val TUFT_FAN_DEG = 18f
private const val TUFT_SWAY_DEG = 6f
private const val TUFT_LENGTH = 0.22f

// ─── LYNX TUFTED constants ────────────────────────────────────────────────────
private const val LYNX_BASE_LEFT = 0.48f
private const val LYNX_BASE_RIGHT = 0.34f
private const val LYNX_TUFT_LENGTH = 0.36f
private const val LYNX_SWAY_DEG = 10f
private val LYNX_TUFT_PHASES = floatArrayOf(0.0f, 0.9f, 1.8f, 2.7f, 0.4f, 1.3f, 2.2f)

// ─── CANINE FLOPPY constants ──────────────────────────────────────────────────
private const val FLOPPY_FLAP_DX = 0.55f
private const val FLOPPY_FLAP_DY = 1.15f
private const val FLOPPY_FREQ = 0.5f
private const val FLOPPY_SWAY_DEG = 4f

// ─── CANINE PERKY constants ───────────────────────────────────────────────────
private const val PERKY_HALF_BASE = 0.38f
private const val PERKY_TIP_Y = 0.20f

// ─── colours ─────────────────────────────────────────────────────────────────
private val EAR_COLOR = Color(0xFF8B5E3C)
private val INNER_EAR_COLOR = Color(0xFFE8A0A0)
private val FUR_COLOR = Color(0xFFD4A07A)

private val FELINE_RIM_COLOR = Color(0xFF7A4E28)
private val FELINE_CENTER_COLOR = Color(0xFFBF8A5A)
private val FELINE_INNER_COLOR = Color(0xFFE8A8A0)
private val FELINE_TUFT_COLOR = Color(0xFF4A2C10)

private val LYNX_OUTER_COLOR = Color(0xFF9B7040)
private val LYNX_TUFT_COLOR = Color(0xFF2E1A08)

private val FLOPPY_OUTER_COLOR = Color(0xFFB8864A)
private val FLOPPY_INNER_COLOR = Color(0xFFE8C8A8)

private val PERKY_OUTER_COLOR = Color(0xFFD4B896)
private val PERKY_INNER_COLOR = Color(0xFFE8A090)
private val PERKY_HATCH_COLOR = Color(0xFF9A7848)
