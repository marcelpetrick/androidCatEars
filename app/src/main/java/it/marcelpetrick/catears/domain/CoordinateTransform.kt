// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/**
 * Transforms a [Point2D] from **image space** to **view space**.
 *
 * ML Kit returns face landmarks in image pixel coordinates (origin = top-left
 * of the image buffer). PreviewView's default scale type preserves aspect ratio
 * and fills the view, cropping the overflowing axis. This function applies that
 * same uniform scale + crop offset and, for the front camera, mirrors
 * horizontally so points line up with what the user sees on screen.
 */
fun imageToViewCoordinates(point: Point2D, ctx: TransformContext): Point2D {
    val scale = maxOf(
        ctx.viewWidth.toFloat() / ctx.imageWidth.toFloat(),
        ctx.viewHeight.toFloat() / ctx.imageHeight.toFloat(),
    )
    val offsetX = (ctx.viewWidth - ctx.imageWidth * scale) / 2f
    val offsetY = (ctx.viewHeight - ctx.imageHeight * scale) / 2f

    val mappedX = point.x * scale + offsetX
    val mappedY = point.y * scale + offsetY

    val mirroredX = if (ctx.isFrontCamera) ctx.viewWidth - mappedX else mappedX
    return Point2D(x = mirroredX, y = mappedY)
}

/**
 * Transforms a [BoundingBox] from image space to view space.
 *
 * Applies the same scale and front-camera mirror as [imageToViewCoordinates].
 * The returned box always has left < right regardless of mirroring.
 */
fun imageToViewBoundingBox(box: BoundingBox, ctx: TransformContext): BoundingBox {
    val topLeft = imageToViewCoordinates(Point2D(box.left, box.top), ctx)
    val bottomRight = imageToViewCoordinates(Point2D(box.right, box.bottom), ctx)
    return BoundingBox(
        left = minOf(topLeft.x, bottomRight.x),
        top = topLeft.y,
        right = maxOf(topLeft.x, bottomRight.x),
        bottom = bottomRight.y,
    )
}
