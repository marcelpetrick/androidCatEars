// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import it.marcelpetrick.catears.domain.EarAnchor
import it.marcelpetrick.catears.domain.OverlayPlacement

/**
 * Transparent overlay that draws two procedural cat ears at the positions described by
 * [placement]. Sits on top of the camera preview inside a [Box] with fillMaxSize.
 * When [placement] is null (no face detected) nothing is rendered.
 */
@Composable
fun CatEarOverlay(placement: OverlayPlacement?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (placement != null) {
                    drawEar(placement.leftEar)
                    drawEar(placement.rightEar)
                }
            },
    )
}

private fun DrawScope.drawEar(anchor: EarAnchor) {
    val cx = anchor.x
    val top = anchor.y
    val s = anchor.size

    rotate(degrees = anchor.tiltDegrees, pivot = Offset(cx, top + s / 2f)) {
        scale(scaleX = anchor.xScale, scaleY = 1f, pivot = Offset(cx, top + s / 2f)) {
            drawOuterEar(cx, top, s)
            drawInnerEar(cx, top, s)
        }
    }
}

/** Outer ear shape: a tall triangle with slightly curved sides drawn in warm brown. */
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

/** Inner ear accent: a smaller triangle in pink offset slightly above the base. */
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

private val EAR_COLOR = Color(0xFF8B5E3C)
private val INNER_EAR_COLOR = Color(0xFFE8A0A0)

private const val OUTER_HALF_BASE = 0.42f
private const val INNER_HALF_BASE = 0.24f
private const val INNER_TOP_OFFSET = 0.28f
private const val INNER_BOTTOM_OFFSET = 0.78f
