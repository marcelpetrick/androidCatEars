// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/** Visual style used to render both cat ears in [OverlayPlacement]. */
enum class EarStyle {
    /** Warm-brown triangle with pink inner accent — the original WP 20 design. */
    CLASSIC,

    /** Asymmetric pointed feline shape with gradient fill and animated tip-tufts. */
    SHARP_FELINE,

    /** Extra-tall ears with long dark tufts projecting from the tip. */
    LYNX_TUFTED,

    /** Drooping teardrop flap resembling a floppy-eared dog. */
    CANINE_FLOPPY,

    /** Short wide triangles with a rounded cap — husky / perky dog style. */
    CANINE_PERKY,
}
