// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EarRenderStyleSpecTest {

    @Test
    fun `all ear styles have one render spec`() {
        val specs = allEarRenderStyleSpecs()

        assertEquals(EarStyle.entries.size, specs.size)
        assertEquals(EarStyle.entries.toSet(), specs.map { it.style }.toSet())
    }

    @Test
    fun `all render specs have valid visible anchors`() {
        allEarRenderStyleSpecs().forEach { spec ->
            assertTrue(spec.anchor.tipRatio < spec.anchor.baseLineRatio, "${spec.style} tip must be above base")
            assertTrue(spec.anchor.visualPaddingRatio >= 0f, "${spec.style} padding must be non-negative")
        }
    }

    @Test
    fun `all render specs keep inner ear material visually distinct from outer fur`() {
        allEarRenderStyleSpecs().forEach { spec ->
            assertNotEquals(spec.material.outerBaseArgb, spec.material.innerBaseArgb, "${spec.style} inner ear")
            assertNotEquals(spec.material.outerRimArgb, spec.material.innerHighlightArgb, "${spec.style} highlight")
        }
    }

    @Test
    fun `default tint policy protects rosy inner ears`() {
        allEarRenderStyleSpecs().forEach { spec ->
            assertEquals(EarTintPolicy.OuterFurOnly, spec.tintPolicy, "${spec.style} tint policy")
        }
    }

    @Test
    fun `extracted sprite styles are sprite-backed and others default to procedural`() {
        val spriteStyles = setOf(
            EarStyle.CLASSIC,
            EarStyle.SHARP_FELINE,
            EarStyle.ROUNDED_FELINE,
            EarStyle.LYNX_TUFTED,
            EarStyle.DENSE_FLUFFY,
            EarStyle.FOX,
        )
        spriteStyles.forEach { style ->
            assertEquals(EarRendererKind.Sprite, earRenderStyleSpec(style).rendererKind, "$style")
        }
        EarStyle.entries.filterNot { it in spriteStyles }.forEach { style ->
            assertEquals(EarRendererKind.Procedural, earRenderStyleSpec(style).rendererKind, "$style")
        }
    }

    @Test
    fun `render kind defaults to procedural when unspecified`() {
        val spec = EarRenderStyleSpec(
            style = EarStyle.CLASSIC,
            material = earRenderStyleSpec(EarStyle.CLASSIC).material,
            anchor = earRenderStyleSpec(EarStyle.CLASSIC).anchor,
            furStrokeCount = 1,
            supportsTufts = false,
            tintPolicy = EarTintPolicy.OuterFurOnly,
        )
        assertEquals(EarRendererKind.Procedural, spec.rendererKind)
    }

    @Test
    fun `anchor rejects impossible visible bounds`() {
        assertThrows(IllegalArgumentException::class.java) {
            EarAssetAnchor(baseLineRatio = 0.2f, tipRatio = 0.8f, visualPaddingRatio = 0f)
        }
    }

    @Test
    fun `style spec rejects negative fur count`() {
        val material = earRenderStyleSpec(EarStyle.CLASSIC).material
        val anchor = earRenderStyleSpec(EarStyle.CLASSIC).anchor

        assertThrows(IllegalArgumentException::class.java) {
            EarRenderStyleSpec(
                style = EarStyle.CLASSIC,
                material = material,
                anchor = anchor,
                furStrokeCount = -1,
                supportsTufts = false,
                tintPolicy = EarTintPolicy.OuterFurOnly,
            )
        }
    }
}
