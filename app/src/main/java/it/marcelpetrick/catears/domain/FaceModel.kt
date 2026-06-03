// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/**
 * Framework-free representation of a detected face.
 *
 * Coordinates are in **image space** (pixels, origin top-left).
 * The caller is responsible for transforming to view/screen space before rendering.
 *
 * @param boundingBox Axis-aligned bounding box around the face.
 * @param leftEyePosition Landmark position of the left eye (as seen by the camera).
 * @param rightEyePosition Landmark position of the right eye (as seen by the camera).
 * @param headEulerAngleZ Roll angle of the head in degrees (positive = tilted right).
 */
data class FaceModel(
    val boundingBox: BoundingBox,
    val leftEyePosition: Point2D?,
    val rightEyePosition: Point2D?,
    val headEulerAngleZ: Float,
)

/** Axis-aligned rectangle in pixel coordinates. */
data class BoundingBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f
}

/** 2-D point in pixel coordinates. */
data class Point2D(val x: Float, val y: Float)
