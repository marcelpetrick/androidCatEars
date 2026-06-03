// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap

/**
 * Renders a (possibly vector) drawable resource into a software [Bitmap].
 *
 * Needed because [android.graphics.BitmapFactory] cannot decode vector
 * drawables. Excluded from Kover (Android drawable/canvas, device-only).
 */
fun decodeDrawableToBitmap(context: Context, @DrawableRes resId: Int, width: Int, height: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    val safeWidth = width.coerceAtLeast(1)
    val safeHeight = height.coerceAtLeast(1)
    val bitmap = createBitmap(safeWidth, safeHeight)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
