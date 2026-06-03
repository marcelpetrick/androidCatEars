// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/** Which camera lens the user has selected. Pure domain — no Android/CameraX imports. */
enum class LensSelector {
    Front,
    Rear,
    ;

    fun toggled(): LensSelector = if (this == Front) Rear else Front
}
