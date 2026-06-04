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
) {
    init {
        require(furStrokeCount >= 0) { "furStrokeCount must be >= 0" }
    }
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
    EarStyle.CLASSIC -> EarRenderStyleSpec(
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
    )

    EarStyle.SHARP_FELINE -> naturalFelineSpec(style, furStrokeCount = 7, supportsTufts = true)

    EarStyle.ROUNDED_FELINE -> naturalFelineSpec(style, furStrokeCount = 8, supportsTufts = false)

    EarStyle.LYNX_TUFTED -> naturalFelineSpec(style, furStrokeCount = 10, supportsTufts = true)

    EarStyle.DENSE_FLUFFY -> naturalFelineSpec(style, furStrokeCount = 14, supportsTufts = true)

    EarStyle.CANINE_FLOPPY -> warmCanineSpec(style, furStrokeCount = 6)

    EarStyle.CANINE_PERKY -> warmCanineSpec(style, furStrokeCount = 6)

    EarStyle.RABBIT -> paleSoftSpec(style, furStrokeCount = 8)

    EarStyle.FOX -> EarRenderStyleSpec(
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
    )

    EarStyle.BEAR -> warmCanineSpec(style, furStrokeCount = 5)
}

private fun naturalFelineSpec(style: EarStyle, furStrokeCount: Int, supportsTufts: Boolean): EarRenderStyleSpec =
    EarRenderStyleSpec(
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
    )

private fun warmCanineSpec(style: EarStyle, furStrokeCount: Int): EarRenderStyleSpec = EarRenderStyleSpec(
    style = style,
    material = EarMaterialSpec(
        outerBaseArgb = 0xFFD0AA76.toInt(),
        outerRimArgb = 0xFF6E4B32.toInt(),
        outerHighlightArgb = 0xFFF0D0A2.toInt(),
        innerBaseArgb = 0xFFEFA09A.toInt(),
        innerHighlightArgb = 0xFFFFCEC4.toInt(),
        shadowArgb = 0x33000000,
    ),
    anchor = defaultCatAnchor(),
    furStrokeCount = furStrokeCount,
    supportsTufts = false,
    tintPolicy = EarTintPolicy.OuterFurOnly,
)

private fun paleSoftSpec(style: EarStyle, furStrokeCount: Int): EarRenderStyleSpec = EarRenderStyleSpec(
    style = style,
    material = EarMaterialSpec(
        outerBaseArgb = 0xFFE8D8C4.toInt(),
        outerRimArgb = 0xFF8C7664.toInt(),
        outerHighlightArgb = 0xFFFFF2E2.toInt(),
        innerBaseArgb = 0xFFF0A8AE.toInt(),
        innerHighlightArgb = 0xFFFFD5D2.toInt(),
        shadowArgb = 0x33000000,
    ),
    anchor = defaultCatAnchor(),
    furStrokeCount = furStrokeCount,
    supportsTufts = false,
    tintPolicy = EarTintPolicy.OuterFurOnly,
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
