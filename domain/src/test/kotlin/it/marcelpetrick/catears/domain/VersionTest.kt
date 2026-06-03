// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class VersionTest {

    @Test
    fun `toString returns major dot minor dot patch`() {
        assertEquals("1.2.3", Version(1, 2, 3).toString())
    }

    @Test
    fun `parse creates version from valid string`() {
        assertEquals(Version(0, 1, 3), Version.parse("0.1.3"))
    }

    @Test
    fun `parse throws on malformed string`() {
        assertThrows<IllegalArgumentException> { Version.parse("1.2") }
    }

    data class ComparisonCase(val a: Version, val b: Version, val expectNewer: Boolean)

    @ParameterizedTest
    @MethodSource("comparisonCases")
    fun `isNewerThan compares versions correctly`(case: ComparisonCase) {
        if (case.expectNewer) {
            assertTrue(case.a.isNewerThan(case.b))
        } else {
            assertFalse(case.a.isNewerThan(case.b))
        }
    }

    companion object {
        @JvmStatic
        fun comparisonCases(): Stream<ComparisonCase> = Stream.of(
            ComparisonCase(Version(2, 0, 0), Version(1, 9, 9), expectNewer = true),
            ComparisonCase(Version(1, 1, 0), Version(1, 0, 9), expectNewer = true),
            ComparisonCase(Version(1, 0, 1), Version(1, 0, 0), expectNewer = true),
            ComparisonCase(Version(1, 0, 0), Version(1, 0, 0), expectNewer = false),
            ComparisonCase(Version(0, 9, 9), Version(1, 0, 0), expectNewer = false),
        )
    }
}
