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
 * @param headEulerAngleY Yaw angle in degrees (positive = head turned right); used for perspective scaling.
 * @param leftEarPosition Side-of-skull anchor where the left ear attaches; null if not detected.
 * @param rightEarPosition Side-of-skull anchor where the right ear attaches; null if not detected.
 * @param smilingProbability Probability [0..1] that the face is smiling; null if classification disabled.
 * @param leftEyeOpenProbability Probability [0..1] that the left eye is open; null if classification disabled.
 * @param rightEyeOpenProbability Probability [0..1] that the right eye is open; null if classification disabled.
 * @param trackingId ML Kit face-tracking ID; stable across frames for the same physical face; null if not available.
 */
data class FaceModel(
    val boundingBox: BoundingBox,
    val leftEyePosition: Point2D?,
    val rightEyePosition: Point2D?,
    val headEulerAngleZ: Float,
    val headEulerAngleY: Float = 0f,
    val leftEarPosition: Point2D? = null,
    val rightEarPosition: Point2D? = null,
    val smilingProbability: Float? = null,
    val leftEyeOpenProbability: Float? = null,
    val rightEyeOpenProbability: Float? = null,
    val trackingId: Int? = null,
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
