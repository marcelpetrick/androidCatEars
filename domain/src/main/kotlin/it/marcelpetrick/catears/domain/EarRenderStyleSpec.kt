// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/**
 * Shared visual metadata for richer ear renderers.
 *
 * The values are ratios relative to the visible ear height, not raw bitmap bounds. Keeping this in
 * the domain module lets the live preview, still-photo compositor, and video overlay agree on
 * placement and material intent without sharing Android drawing APIs.
 */
data class EarRenderStyleSpec(
    val style: EarStyle,
    val material: EarMaterialSpec,
    val anchor: EarAssetAnchor,
    val furStrokeCount: Int,
    val supportsTufts: Boolean,
    val tintPolicy: EarTintPolicy,
    val rendererKind: EarRendererKind = EarRendererKind.Procedural,
) {
    init {
        require(furStrokeCount >= 0) { "furStrokeCount must be >= 0" }
    }
}

/**
 * Which renderer backs a style's visual fill.
 *
 * [Procedural] draws the ear with Canvas/DrawScope primitives (flat shapes, fur strokes).
 * [Sprite] draws a pre-rendered transparent bitmap, scaled and transformed onto the placement.
 * The domain only declares the kind; the app module resolves the actual drawable resource so
 * this module stays free of Android resource references and remains JVM-testable.
 */
enum class EarRendererKind {
    Procedural,
    Sprite,
}

/** Color/material intent for a style, expressed as ARGB ints for framework-neutral sharing. */
data class EarMaterialSpec(
    val outerBaseArgb: Int,
    val outerRimArgb: Int,
    val outerHighlightArgb: Int,
    val innerBaseArgb: Int,
    val innerHighlightArgb: Int,
    val shadowArgb: Int,
)

/**
 * Describes visible-content anchors for procedural or sprite-backed ears.
 *
 * [baseLineRatio] is the vertical point that should sit on the computed head/forehead line. For
 * the current top-origin ear anchors this is normally 1.0: the bottom of the visible ear.
 */
data class EarAssetAnchor(val baseLineRatio: Float, val tipRatio: Float, val visualPaddingRatio: Float) {
    init {
        require(baseLineRatio in MIN_BASE_LINE_RATIO..MAX_BASE_LINE_RATIO) {
            "baseLineRatio must be near the visible ear bounds"
        }
        require(tipRatio in MIN_TIP_RATIO..MAX_TIP_RATIO) { "tipRatio must be near the visible ear bounds" }
        require(tipRatio < baseLineRatio) { "tipRatio must be above baseLineRatio" }
        require(visualPaddingRatio >= 0f) { "visualPaddingRatio must be >= 0" }
    }
}

/** Which layers should receive the user-selected hue tint. */
enum class EarTintPolicy {
    /** Tint everything; matches the existing renderer behavior. */
    WholeEar,

    /** Tint only outer fur so the rosy inner ear stays warm and natural. */
    OuterFurOnly,
}

private val specCache: Map<EarStyle, EarRenderStyleSpec> by lazy {
    EarStyle.entries.associateWith { buildEarRenderStyleSpec(it) }
}

fun earRenderStyleSpec(style: EarStyle): EarRenderStyleSpec = specCache.getValue(style)

fun allEarRenderStyleSpecs(): List<EarRenderStyleSpec> = EarStyle.entries.map(::earRenderStyleSpec)

private fun buildEarRenderStyleSpec(style: EarStyle): EarRenderStyleSpec = when (style) {
    EarStyle.CLASSIC -> classicSpec(style)

    EarStyle.SHARP_FELINE -> naturalFelineSpec(
        style,
        furStrokeCount = 7,
        supportsTufts = true,
        rendererKind = EarRendererKind.Sprite,
    )

    EarStyle.ROUNDED_FELINE -> naturalFelineSpec(
        style,
        furStrokeCount = 8,
        supportsTufts = false,
        rendererKind = EarRendererKind.Sprite,
    )

    EarStyle.LYNX_TUFTED -> naturalFelineSpec(
        style,
        furStrokeCount = 10,
        supportsTufts = true,
        rendererKind = EarRendererKind.Sprite,
    )

    EarStyle.DENSE_FLUFFY -> naturalFelineSpec(
        style,
        furStrokeCount = 14,
        supportsTufts = true,
        rendererKind = EarRendererKind.Sprite,
    )

    EarStyle.FOX -> foxSpec(style)
}

private fun classicSpec(style: EarStyle): EarRenderStyleSpec = EarRenderStyleSpec(
    style = style,
    material = EarMaterialSpec(
        outerBaseArgb = 0xFF8B5E3C.toInt(),
        outerRimArgb = 0xFF4F3020.toInt(),
        outerHighlightArgb = 0xFFD29A61.toInt(),
        innerBaseArgb = 0xFFE8A0A0.toInt(),
        innerHighlightArgb = 0xFFFFC6BA.toInt(),
        shadowArgb = 0x33000000,
    ),
    anchor = defaultCatAnchor(),
    furStrokeCount = 9,
    supportsTufts = false,
    tintPolicy = EarTintPolicy.OuterFurOnly,
    rendererKind = EarRendererKind.Sprite,
)

private fun naturalFelineSpec(
    style: EarStyle,
    furStrokeCount: Int,
    supportsTufts: Boolean,
    rendererKind: EarRendererKind = EarRendererKind.Procedural,
): EarRenderStyleSpec = EarRenderStyleSpec(
    style = style,
    material = EarMaterialSpec(
        outerBaseArgb = 0xFF9A6A45.toInt(),
        outerRimArgb = 0xFF463026.toInt(),
        outerHighlightArgb = 0xFFD6AA7A.toInt(),
        innerBaseArgb = 0xFFEFA4A5.toInt(),
        innerHighlightArgb = 0xFFFFCEC2.toInt(),
        shadowArgb = 0x33000000,
    ),
    anchor = defaultCatAnchor(),
    furStrokeCount = furStrokeCount,
    supportsTufts = supportsTufts,
    tintPolicy = EarTintPolicy.OuterFurOnly,
    rendererKind = rendererKind,
)

private fun foxSpec(style: EarStyle): EarRenderStyleSpec = EarRenderStyleSpec(
    style = style,
    material = EarMaterialSpec(
        outerBaseArgb = 0xFFD07A2F.toInt(),
        outerRimArgb = 0xFF5E2C18.toInt(),
        outerHighlightArgb = 0xFFFFB36A.toInt(),
        innerBaseArgb = 0xFFFF9A92.toInt(),
        innerHighlightArgb = 0xFFFFD2C8.toInt(),
        shadowArgb = 0x33000000,
    ),
    anchor = defaultCatAnchor(),
    furStrokeCount = 9,
    supportsTufts = true,
    tintPolicy = EarTintPolicy.OuterFurOnly,
    rendererKind = EarRendererKind.Sprite,
)

private fun defaultCatAnchor() = EarAssetAnchor(
    baseLineRatio = 1f,
    tipRatio = 0f,
    visualPaddingRatio = 0.08f,
)

private const val MIN_BASE_LINE_RATIO = 0f
private const val MAX_BASE_LINE_RATIO = 1.2f
private const val MIN_TIP_RATIO = -0.2f
private const val MAX_TIP_RATIO = 1f
