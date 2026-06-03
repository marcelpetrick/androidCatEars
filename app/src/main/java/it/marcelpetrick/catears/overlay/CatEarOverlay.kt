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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import it.marcelpetrick.catears.domain.EarAnchor
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

    // Per-ear tilt animated with a spring so rapid head snaps have a comical elastic lag.
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (placement != null) {
                    drawEar(placement.leftEar.copy(tiltDegrees = leftTilt), swayTime, twitchTime)
                    drawEar(placement.rightEar.copy(tiltDegrees = rightTilt), swayTime, twitchTime)
                }
            },
    )
}

private fun DrawScope.drawEar(anchor: EarAnchor, swayTime: Float, twitchTime: Float) {
    val cx = anchor.x
    val top = anchor.y
    val s = anchor.size
    val pivot = Offset(cx, top + s / 2f)
    val twitchAngle = sin(twitchTime) * TWITCH_AMPLITUDE * sin(twitchTime * TWITCH_FREQ_MOD)

    rotate(degrees = anchor.tiltDegrees + twitchAngle, pivot = pivot) {
        scale(scaleX = anchor.xScale, scaleY = 1f, pivot = pivot) {
            drawOuterEar(cx, top, s)
            drawInnerEar(cx, top, s)
            drawFurStrands(cx, top, s, swayTime)
        }
    }
}

/** Outer ear shape: a tall triangle in warm brown. */
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

/** Inner ear accent: a smaller triangle in pink. */
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

/**
 * Animated fur strands: sine-wave offset tip positions driven by [swayTime].
 * Each strand has a randomised phase so they move out of sync for a natural look.
 */
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

// ---- geometry constants ----
private const val OUTER_HALF_BASE = 0.42f
private const val INNER_HALF_BASE = 0.24f
private const val INNER_TOP_OFFSET = 0.28f
private const val INNER_BOTTOM_OFFSET = 0.78f

// ---- fur strand constants ----
private const val STRAND_WIDTH_RATIO = 0.04f
private const val STRAND_BASE_Y_RATIO = 0.15f
private const val STRAND_TIP_Y_RATIO = -0.08f
private const val STRAND_SWAY_RATIO = 0.06f
private const val STRAND_BOB_RATIO = 0.03f
private const val STRAND_BOB_FREQ = 0.7f
private val STRAND_X_FRACTIONS = floatArrayOf(0.22f, 0.38f, 0.50f, 0.62f, 0.78f)
private val STRAND_PHASES = floatArrayOf(0.0f, 1.2f, 2.4f, 0.7f, 1.9f)

// ---- animation timing ----
private const val SWAY_PERIOD_MS = 1250
private const val TWITCH_PERIOD_MS = 5000
private const val TWITCH_AMPLITUDE = 4f
private const val TWITCH_FREQ_MOD = 3f
private val TWO_PI = (2.0 * PI).toFloat()

// ---- spring tilt factors ----
// Right ear tilts slightly more to exaggerate the comical effect; both < 1.0
// so the ears have their own character rather than rigidly following the head.
private const val LEFT_TILT_FACTOR = 0.6f
private const val RIGHT_TILT_FACTOR = 1.0f

// ---- colours ----
private val EAR_COLOR = Color(0xFF8B5E3C)
private val INNER_EAR_COLOR = Color(0xFFE8A0A0)
private val FUR_COLOR = Color(0xFFD4A07A)
