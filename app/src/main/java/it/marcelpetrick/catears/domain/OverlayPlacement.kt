// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/**
 * Describes how to position and scale the cat-ear overlay in **view space**.
 *
 * @param centerX X-coordinate of the midpoint between the two ears (view px).
 * @param topY Y-coordinate of the top of the ears (view px).
 * @param width Total width of the overlay spanning both ears (view px).
 * @param rotationDegrees Clockwise rotation to match head roll.
 */
data class OverlayPlacement(val centerX: Float, val topY: Float, val width: Float, val rotationDegrees: Float)

/**
 * Computes the cat-ear overlay placement from a face in view space.
 *
 * Anchoring strategy:
 * - When [leftEarAnchor] / [rightEarAnchor] (the ML Kit ear landmarks transformed to view space)
 *   are available, the cat-ear bottom is placed at those Y positions so it "grows out of" the
 *   skull at the natural ear attachment point — immune to bounding-box height noise.
 * - Fallback (landmarks absent): the old bounding-box-top heuristic is used so the function
 *   always returns a valid placement.
 * - Overlay width is always derived from the face bounding box for stable size scaling.
 * - Rotation matches [headEulerAngleZ].
 *
 * @param viewBox Face bounding box already transformed to view space.
 * @param headEulerAngleZ Head roll from ML Kit (degrees, positive = tilted right).
 * @param leftEarAnchor View-space position of the left ear landmark; null = use fallback.
 * @param rightEarAnchor View-space position of the right ear landmark; null = use fallback.
 * @param widthRatio How wide the overlay is relative to the face width. Default 1.3.
 * @param earHeightRatio Fallback only: how far above the box top the overlay bottom sits.
 */
fun computeOverlayPlacement(
    viewBox: BoundingBox,
    headEulerAngleZ: Float,
    leftEarAnchor: Point2D? = null,
    rightEarAnchor: Point2D? = null,
    widthRatio: Float = DEFAULT_WIDTH_RATIO,
    earHeightRatio: Float = DEFAULT_EAR_HEIGHT_RATIO,
): OverlayPlacement {
    val overlayWidth = viewBox.width * widthRatio
    val overlayHeight = overlayWidth * EAR_ASPECT_RATIO

    val topY = if (leftEarAnchor != null && rightEarAnchor != null) {
        // Anchor the cat-ear bottom to the average human-ear Y; ears then extend upward.
        val earAttachY = (leftEarAnchor.y + rightEarAnchor.y) / 2f
        earAttachY - overlayHeight
    } else {
        val earBottomY = viewBox.top - viewBox.height * earHeightRatio
        earBottomY - overlayHeight
    }

    val centerX = if (leftEarAnchor != null && rightEarAnchor != null) {
        (leftEarAnchor.x + rightEarAnchor.x) / 2f
    } else {
        viewBox.centerX
    }

    return OverlayPlacement(
        centerX = centerX,
        topY = topY,
        width = overlayWidth,
        rotationDegrees = headEulerAngleZ,
    )
}

/**
 * Exponential moving average smoother for [OverlayPlacement].
 *
 * Reduces jitter caused by per-frame ML Kit variation.
 * alpha ∈ (0, 1]: higher = more responsive, lower = smoother.
 */
class PlacementSmoother(private val alpha: Float = DEFAULT_ALPHA) {

    private var last: OverlayPlacement? = null

    fun smooth(next: OverlayPlacement): OverlayPlacement {
        val prev = last ?: return next.also { last = it }
        val smoothed = OverlayPlacement(
            centerX = lerp(prev.centerX, next.centerX, alpha),
            topY = lerp(prev.topY, next.topY, alpha),
            width = lerp(prev.width, next.width, alpha),
            rotationDegrees = lerpAngle(prev.rotationDegrees, next.rotationDegrees, alpha),
        )
        last = smoothed
        return smoothed
    }

    fun reset() {
        last = null
    }

    private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

    private fun lerpAngle(from: Float, to: Float, t: Float): Float {
        var diff = to - from
        // Normalise to [-180, 180] so the smoother takes the shortest arc
        while (diff > HALF_CIRCLE) diff -= FULL_CIRCLE
        while (diff < -HALF_CIRCLE) diff += FULL_CIRCLE
        return from + diff * t
    }

    companion object {
        private const val DEFAULT_ALPHA = 0.3f
        private const val HALF_CIRCLE = 180f
        private const val FULL_CIRCLE = 360f
    }
}

private const val DEFAULT_WIDTH_RATIO = 1.3f
private const val DEFAULT_EAR_HEIGHT_RATIO = 0.1f
private const val EAR_ASPECT_RATIO = 0.5f // ears are roughly half as tall as wide
