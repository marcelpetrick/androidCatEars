// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.overlay

import it.marcelpetrick.catears.R
import it.marcelpetrick.catears.domain.EarStyle

internal fun earSpriteDrawableId(style: EarStyle): Int? = when (style) {
    EarStyle.CLASSIC -> R.drawable.ear_classic
    EarStyle.DENSE_FLUFFY -> R.drawable.ear_dense_fluffy
    EarStyle.FOX -> R.drawable.ear_fox
    EarStyle.LYNX_TUFTED -> R.drawable.ear_lynx_tufted
    EarStyle.ROUNDED_FELINE -> R.drawable.ear_rounded_feline
    EarStyle.SHARP_FELINE -> R.drawable.ear_sharp_feline
    else -> null
}
