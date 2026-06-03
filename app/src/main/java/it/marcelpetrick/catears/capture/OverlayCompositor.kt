// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.capture

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import it.marcelpetrick.catears.domain.OverlayPlacement

/**
 * Composites a cat-ear overlay onto a captured camera frame.
 *
 * The pure transform math lives in [DrawTransform] / [computeDrawTransform] so it
 * can be unit-tested on the JVM without Android classes. The [composite] and
 * [applyToMatrix] functions require real Android Bitmap/Canvas and are excluded
 * from Kover coverage.
 */
object OverlayCompositor {

    /**
     * Pure data class holding all values needed to draw the overlay.
     * No Android dependencies — fully testable on the JVM.
     */
    data class DrawTransform(
        val scaleX: Float,
        val scaleY: Float,
        val rotateDegrees: Float,
        val rotatePivotX: Float,
        val rotatePivotY: Float,
        val translateX: Float,
        val translateY: Float,
    )

    /**
     * Computes the scale, rotation, and translation needed to draw the overlay
     * at the position described by [placement]. Pure Kotlin — no Android classes.
     */
    fun computeDrawTransform(placement: OverlayPlacement, overlayWidth: Int, overlayHeight: Int): DrawTransform {
        val scaleX = placement.width / overlayWidth.toFloat()
        val renderedHeight = placement.width * OVERLAY_ASPECT
        val scaleY = renderedHeight / overlayHeight.toFloat()

        return DrawTransform(
            scaleX = scaleX,
            scaleY = scaleY,
            rotateDegrees = placement.rotationDegrees,
            rotatePivotX = placement.width / 2f,
            rotatePivotY = renderedHeight / 2f,
            translateX = placement.centerX - placement.width / 2f,
            translateY = placement.topY,
        )
    }

    /** Builds an Android [Matrix] from a [DrawTransform]. */
    fun applyToMatrix(transform: DrawTransform): Matrix = Matrix().apply {
        postScale(transform.scaleX, transform.scaleY)
        postRotate(transform.rotateDegrees, transform.rotatePivotX, transform.rotatePivotY)
        postTranslate(transform.translateX, transform.translateY)
    }

    /**
     * Draws [overlayBitmap] onto [frame] according to [placement] and returns
     * the composited result as a new [Bitmap]. Returns a copy of [frame] if
     * [placement] is null. The caller must recycle the returned bitmap.
     */
    fun composite(frame: Bitmap, overlayBitmap: Bitmap, placement: OverlayPlacement?): Bitmap {
        val result = frame.copy(Bitmap.Config.ARGB_8888, true)
        if (placement == null) return result

        val transform = computeDrawTransform(placement, overlayBitmap.width, overlayBitmap.height)
        val matrix = applyToMatrix(transform)
        Canvas(result).drawBitmap(overlayBitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
        return result
    }

    private const val OVERLAY_ASPECT = 0.5f
}
