// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import it.marcelpetrick.catears.BuildConfig

/**
 * Small, non-intrusive build stamp: `v<version> (<commit>)`, e.g. `v0.1.27 (a1b2c3d)`.
 *
 * Lets developers and testers identify exactly which build is running.
 * Values come from BuildConfig, injected by Gradle at build time.
 */
@Composable
fun VersionLabel(modifier: Modifier = Modifier) {
    Text(
        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_COMMIT})",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = LABEL_ALPHA),
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

private const val LABEL_ALPHA = 0.7f
