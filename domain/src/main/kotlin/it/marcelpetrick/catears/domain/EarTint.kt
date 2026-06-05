// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ear colour treatment applied by renderers.
 *
 * Photorealistic sprites keep their authored colours, so only [NATURAL] is user-facing.
 */
enum class EarTint(val hueDegrees: Float) {
    NATURAL(0f),
}

/**
 * Builds the 4×5 (20-element, row-major) colour matrix that rotates hue by [degrees].
 *
 * Luminance-preserving rotation (Rec. 709 weights). At 0° this is the identity
 * matrix, so [EarTint.NATURAL] is a no-op. The array layout matches both
 * `androidx.compose.ui.graphics.ColorMatrix` and `android.graphics.ColorMatrix`.
 */
fun hueRotationMatrix(degrees: Float): FloatArray {
    val rad = degrees * PI.toFloat() / HALF_TURN_DEGREES
    val c = cos(rad)
    val s = sin(rad)
    return floatArrayOf(
        LR + c * (1f - LR) + s * (-LR), LG + c * (-LG) + s * (-LG), LB + c * (-LB) + s * (1f - LB), 0f, 0f,
        LR + c * (-LR) + s * (0.143f), LG + c * (1f - LG) + s * (0.140f), LB + c * (-LB) + s * (-0.283f), 0f, 0f,
        LR + c * (-LR) + s * (-(1f - LR)), LG + c * (-LG) + s * (LG), LB + c * (1f - LB) + s * (LB), 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

private const val HALF_TURN_DEGREES = 180f

// Rec. 709 luminance weights.
private const val LR = 0.213f
private const val LG = 0.715f
private const val LB = 0.072f
