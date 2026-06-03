// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.marcelpetrick.catears.R
import it.marcelpetrick.catears.domain.OverlayPlacement

/**
 * Transparent overlay that draws the cat-ear asset at the position and
 * scale described by [placement].
 *
 * Sits on top of [CameraPreview] inside a [Box] with fillMaxSize.
 * When [placement] is null (no face detected) nothing is rendered.
 */
@Composable
fun CatEarOverlay(placement: OverlayPlacement?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        if (placement != null) {
            val density = LocalDensity.current
            val widthDp = with(density) { placement.width.toDp() }
            val heightDp = widthDp * EAR_ASPECT_RATIO

            val leftDp = with(density) { (placement.centerX - placement.width / 2f).toDp() }
            val topDp = with(density) { placement.topY.toDp() }

            Image(
                painter = painterResource(id = R.drawable.ic_cat_ears),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = leftDp, y = topDp)
                    .size(width = widthDp, height = heightDp)
                    .graphicsLayer { rotationZ = placement.rotationDegrees },
            )
        }
    }
}

private const val EAR_ASPECT_RATIO = 0.5f
