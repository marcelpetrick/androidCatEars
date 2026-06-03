// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/**
 * Transforms a [Point2D] from **image space** to **view space**.
 *
 * ML Kit returns face landmarks in image pixel coordinates (origin = top-left
 * of the image buffer). The PreviewView renders the image scaled and, for the
 * front camera, mirrored horizontally. This function converts a point so it
 * lines up with what the user sees on screen.
 */
fun imageToViewCoordinates(point: Point2D, ctx: TransformContext): Point2D {
    val scaleX = ctx.viewWidth.toFloat() / ctx.imageWidth.toFloat()
    val scaleY = ctx.viewHeight.toFloat() / ctx.imageHeight.toFloat()

    val scaledX = point.x * scaleX
    val scaledY = point.y * scaleY

    val mirroredX = if (ctx.isFrontCamera) ctx.viewWidth - scaledX else scaledX
    return Point2D(x = mirroredX, y = scaledY)
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
