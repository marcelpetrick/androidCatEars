// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/** Dimensions of the camera image buffer and on-screen view area used for coordinate transforms. */
data class TransformContext(
    val imageWidth: Int,
    val imageHeight: Int,
    val viewWidth: Int,
    val viewHeight: Int,
    val isFrontCamera: Boolean,
)
