// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.overlay

import it.marcelpetrick.catears.R
import it.marcelpetrick.catears.domain.EarStyle

internal fun earSpriteDrawableId(style: EarStyle): Int? = when (style) {
    EarStyle.CLASSIC -> R.drawable.ear_classic
    else -> null
}
