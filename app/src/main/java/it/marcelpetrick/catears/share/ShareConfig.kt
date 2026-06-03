// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.share

import android.content.Intent
import android.net.Uri

/**
 * Pure parameters for an image share operation.
 * No Android deps — fully unit-testable on the JVM.
 */
data class ShareConfig(val uri: Uri, val mimeType: String)

/**
 * Builds a [ShareConfig] for the given [uri]. Pure function — tested on JVM.
 */
fun buildShareConfig(uri: Uri, mimeType: String = MIME_JPEG) = ShareConfig(uri, mimeType)

/**
 * Converts a [ShareConfig] to an Android chooser [Intent].
 * Excluded from unit tests (Intent is not mocked in JVM test runtime).
 */
fun ShareConfig.toChooserIntent(): Intent {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(shareIntent, null)
}

private const val MIME_JPEG = "image/jpeg"
