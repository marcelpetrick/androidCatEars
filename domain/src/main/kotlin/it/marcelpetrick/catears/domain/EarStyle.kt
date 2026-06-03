// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/** Visual style used to render both cat ears in [OverlayPlacement]. */
enum class EarStyle {
    /** Warm-brown triangle with pink inner accent — the original WP 20 design. */
    CLASSIC,

    /** Asymmetric pointed feline shape with gradient fill and animated tip-tufts. */
    SHARP_FELINE,

    /** Bezier-curved realistic feline silhouette with smooth edges and slight tufts. */
    ROUNDED_FELINE,

    /** Extra-tall ears with long dark tufts projecting from the tip. */
    LYNX_TUFTED,

    /** Wide-base ear with dense animated fur fringe along the outer edge. */
    DENSE_FLUFFY,

    /** Drooping teardrop flap resembling a floppy-eared dog. */
    CANINE_FLOPPY,

    /** Short wide triangles with a rounded cap — husky / perky dog style. */
    CANINE_PERKY,

    /** Tall narrow ovals with white outer and pink inner — rabbit style. */
    RABBIT,

    /** Small vivid-orange pointed triangle with white tip — fox style. */
    FOX,

    /** Tiny round semicircles close to the head — bear style. */
    BEAR,
}
