// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.capture

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import it.marcelpetrick.catears.domain.buildImageFileName
import java.io.IOException
import javax.inject.Inject

/**
 * Saves a composited [Bitmap] to the device gallery using scoped storage (MediaStore).
 *
 * No broad storage permissions are needed on Android 10+ (API 29+) when writing to
 * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] via [ContentResolver].
 *
 * Excluded from Kover (Android MediaStore APIs are device-only).
 */
class ImageSaver @Inject constructor(private val context: Context) {

    /**
     * Writes [bitmap] as a JPEG to the Pictures/CatEars gallery album.
     *
     * @param bitmap The composited frame to save.
     * @param epochMillis Capture timestamp (used in the filename).
     * @param randomSuffix 4-char hex string for filename uniqueness.
     * @return The [Uri] of the saved image, or null on failure.
     */
    fun save(bitmap: Bitmap, epochMillis: Long, randomSuffix: String): Uri? {
        val fileName = buildImageFileName(epochMillis, randomSuffix)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE)
            put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.Images.Media.IS_PENDING, PENDING_TRUE)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null

        return writeBitmap(resolver, uri, bitmap, values)
    }

    private fun writeBitmap(resolver: ContentResolver, uri: Uri, bitmap: Bitmap, values: ContentValues): Uri? = try {
        val saved = resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        } == true
        if (saved) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, PENDING_FALSE)
            resolver.update(uri, values, null, null)
            uri
        } else {
            resolver.delete(uri, null, null)
            null
        }
    } catch (e: IOException) {
        Log.e(TAG, "Failed to write image to MediaStore", e)
        resolver.delete(uri, null, null)
        null
    }

    companion object {
        private const val TAG = "ImageSaver"
        private const val MIME_TYPE = "image/jpeg"
        private const val RELATIVE_PATH = "Pictures/CatEars"
        private const val JPEG_QUALITY = 95
        private const val PENDING_TRUE = 1
        private const val PENDING_FALSE = 0
    }
}
