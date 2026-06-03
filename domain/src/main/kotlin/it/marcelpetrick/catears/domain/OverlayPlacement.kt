// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/**
 * Position and display parameters for one cat ear in view space.
 *
 * @param x Horizontal centre of the ear (view px).
 * @param y Top of the ear (view px); ear is drawn downward from this point.
 * @param size Height (and implied width) of the ear in view px.
 * @param tiltDegrees Clockwise rotation of this individual ear.
 * @param xScale Horizontal squash factor for perspective illusion [0.4 .. 1.3].
 */
data class EarAnchor(val x: Float, val y: Float, val size: Float, val tiltDegrees: Float, val xScale: Float = 1f)

/**
 * Describes where and how to render both cat ears in **view space**.
 *
 * @param leftEar Anchor for the left-side cat ear (as seen on screen).
 * @param rightEar Anchor for the right-side cat ear.
 * @param headEulerAngleY Head yaw in degrees; positive = head turned right.
 * @param earStyle Visual rendering style to apply to both ears.
 * @param smilingProbability Smoothed smile probability [0..1]; 0 when unknown.
 * @param eyeOpennessMean Smoothed mean eye-openness [0..1]; 1 when unknown.
 * @param trackingId ML Kit face-tracking ID propagated from [FaceModel]; used as a stable Compose key.
 */
data class OverlayPlacement(
    val leftEar: EarAnchor,
    val rightEar: EarAnchor,
    val headEulerAngleY: Float = 0f,
    val earStyle: EarStyle = EarStyle.CLASSIC,
    val smilingProbability: Float = 0f,
    val eyeOpennessMean: Float = 1f,
    val trackingId: Int? = null,
)

/**
 * Computes independent [EarAnchor] positions for both cat ears.
 *
 * Anchoring strategy:
 * - When [leftEarAnchor] / [rightEarAnchor] are provided (ML Kit ear landmarks in view space)
 *   each cat ear is placed above its corresponding human ear — immune to bounding-box noise.
 * - Fallback (landmarks absent): positions derived from the bounding box centre so the ears
 *   are symmetrically placed above the face.
 * - Ear size is always derived from the face bounding box width for stable distance scaling.
 *
 * @param viewBox Face bounding box already transformed to view space.
 * @param headEulerAngleZ Head roll (degrees, positive = tilted right); applied to both ears.
 * @param headEulerAngleY Head yaw (degrees, positive = head turned right); stored for 20.4.
 * @param leftEarAnchor View-space position of the left ear landmark; null = use fallback.
 * @param rightEarAnchor View-space position of the right ear landmark; null = use fallback.
 * @param widthRatio Width of one ear relative to face width. Default 0.65.
 * @param earHeightRatio Fallback only: gap between box top and ear bottom (fraction of height).
 * @param smilingProbability Raw smile probability from ML Kit [0..1]; 0 when absent.
 * @param eyeOpennessMean Mean of left+right eye-open probabilities [0..1]; 1 when absent.
 * @param trackingId Stable face-tracking ID for Compose keying; null when unavailable.
 */
fun computeOverlayPlacement(
    viewBox: BoundingBox,
    headEulerAngleZ: Float,
    headEulerAngleY: Float = 0f,
    leftEarAnchor: Point2D? = null,
    rightEarAnchor: Point2D? = null,
    widthRatio: Float = DEFAULT_EAR_WIDTH_RATIO,
    earHeightRatio: Float = DEFAULT_EAR_HEIGHT_RATIO,
    smilingProbability: Float = 0f,
    eyeOpennessMean: Float = 1f,
    trackingId: Int? = null,
): OverlayPlacement {
    val earSize = viewBox.width * widthRatio
    // Positive yaw (head turning right) → right ear nearer, left ear farther.
    val yawFraction = (headEulerAngleY / MAX_YAW_DEGREES).coerceIn(-1f, 1f)
    val leftXScale = (1f - yawFraction * PERSPECTIVE_STRENGTH).coerceIn(MIN_SCALE, MAX_SCALE)
    val rightXScale = (1f + yawFraction * PERSPECTIVE_STRENGTH).coerceIn(MIN_SCALE, MAX_SCALE)

    return if (leftEarAnchor != null && rightEarAnchor != null) {
        OverlayPlacement(
            leftEar = EarAnchor(
                x = leftEarAnchor.x,
                y = leftEarAnchor.y - earSize,
                size = earSize,
                tiltDegrees = headEulerAngleZ,
                xScale = leftXScale,
            ),
            rightEar = EarAnchor(
                x = rightEarAnchor.x,
                y = rightEarAnchor.y - earSize,
                size = earSize,
                tiltDegrees = headEulerAngleZ,
                xScale = rightXScale,
            ),
            headEulerAngleY = headEulerAngleY,
            smilingProbability = smilingProbability,
            eyeOpennessMean = eyeOpennessMean,
            trackingId = trackingId,
        )
    } else {
        val earBottomY = viewBox.top - viewBox.height * earHeightRatio
        val topY = earBottomY - earSize
        val halfSpacing = earSize * EAR_HALF_SPACING_RATIO
        OverlayPlacement(
            leftEar = EarAnchor(
                x = viewBox.centerX - halfSpacing,
                y = topY,
                size = earSize,
                tiltDegrees = headEulerAngleZ,
                xScale = leftXScale,
            ),
            rightEar = EarAnchor(
                x = viewBox.centerX + halfSpacing,
                y = topY,
                size = earSize,
                tiltDegrees = headEulerAngleZ,
                xScale = rightXScale,
            ),
            headEulerAngleY = headEulerAngleY,
            smilingProbability = smilingProbability,
            eyeOpennessMean = eyeOpennessMean,
            trackingId = trackingId,
        )
    }
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
            leftEar = smoothAnchor(prev.leftEar, next.leftEar),
            rightEar = smoothAnchor(prev.rightEar, next.rightEar),
            headEulerAngleY = lerp(prev.headEulerAngleY, next.headEulerAngleY, alpha),
            smilingProbability = lerp(prev.smilingProbability, next.smilingProbability, alpha),
            eyeOpennessMean = lerp(prev.eyeOpennessMean, next.eyeOpennessMean, alpha),
        )
        last = smoothed
        return smoothed
    }

    fun reset() {
        last = null
    }

    private fun smoothAnchor(prev: EarAnchor, next: EarAnchor): EarAnchor = EarAnchor(
        x = lerp(prev.x, next.x, alpha),
        y = lerp(prev.y, next.y, alpha),
        size = lerp(prev.size, next.size, alpha),
        tiltDegrees = lerpAngle(prev.tiltDegrees, next.tiltDegrees, alpha),
        xScale = lerp(prev.xScale, next.xScale, alpha),
    )

    private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

    private fun lerpAngle(from: Float, to: Float, t: Float): Float {
        var diff = to - from
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

/**
 * Smooths a list of per-face [OverlayPlacement]s using one [PlacementSmoother] per tracking ID.
 *
 * Smoothers for faces no longer present are discarded each frame so memory is bounded
 * by the number of simultaneously visible faces.
 */
class MultiFaceSmoother(private val alpha: Float = DEFAULT_MULTI_ALPHA) {

    private val smoothers = LinkedHashMap<Int, PlacementSmoother>()

    /** Smooth [entries]: pairs of (trackingId, placement). Returns the smoothed placements in order. */
    fun smooth(entries: List<Pair<Int?, OverlayPlacement>>): List<OverlayPlacement> {
        val activeIds = entries.mapNotNull { it.first }.toSet()
        smoothers.keys.retainAll(activeIds)
        return entries.map { (id, placement) ->
            if (id != null) {
                smoothers.getOrPut(id) { PlacementSmoother(alpha) }.smooth(placement)
            } else {
                placement
            }
        }
    }

    fun reset() {
        smoothers.clear()
    }

    companion object {
        private const val DEFAULT_MULTI_ALPHA = 0.3f
    }
}

private const val DEFAULT_EAR_WIDTH_RATIO = 0.65f
private const val DEFAULT_EAR_HEIGHT_RATIO = 0.1f
private const val EAR_HALF_SPACING_RATIO = 0.35f
private const val MAX_YAW_DEGREES = 45f
private const val PERSPECTIVE_STRENGTH = 0.5f
private const val MIN_SCALE = 0.4f
private const val MAX_SCALE = 1.6f
