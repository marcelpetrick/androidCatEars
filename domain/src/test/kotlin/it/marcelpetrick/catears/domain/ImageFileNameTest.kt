// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImageFileNameTest {

    @Test
    fun `filename starts with CatEars prefix`() {
        val name = buildImageFileName(0L, "abcd")
        assertTrue(name.startsWith("CatEars_")) { "Expected CatEars_ prefix, got: $name" }
    }

    @Test
    fun `filename ends with jpg extension`() {
        val name = buildImageFileName(0L, "abcd")
        assertTrue(name.endsWith(".jpg")) { "Expected .jpg suffix, got: $name" }
    }

    @Test
    fun `suffix is embedded in filename`() {
        val name = buildImageFileName(0L, "a3f1")
        assertTrue(name.contains("a3f1")) { "Expected suffix 'a3f1' in: $name" }
    }

    @Test
    fun `suffix longer than 4 chars is truncated`() {
        val name = buildImageFileName(0L, "abcdefgh")
        assertTrue(name.contains("abcd")) { "Expected first 4 chars of suffix in: $name" }
        val suffixPart = name.substringBeforeLast(".jpg").substringAfterLast("_")
        assertEquals(4, suffixPart.length) { "Suffix part should be 4 chars, was: $suffixPart" }
    }

    @Test
    fun `filename format matches expected pattern`() {
        // epoch 0 = 1970-01-01 00:00:00
        val name = buildImageFileName(0L, "0000")
        // Format: CatEars_YYYYMMDD_HHMMSS_xxxx.jpg
        val regex = Regex("^CatEars_\\d{8}_\\d{6}_[a-zA-Z0-9]{4}\\.jpg$")
        assertTrue(regex.matches(name)) { "Name '$name' does not match expected pattern" }
    }

    @Test
    fun `different timestamps produce different filenames`() {
        val a = buildImageFileName(1_000L, "aaaa")
        val b = buildImageFileName(2_000L, "aaaa")
        assertTrue(a != b) { "Different timestamps must yield different filenames" }
    }

    @Test
    fun `different suffixes produce different filenames`() {
        val a = buildImageFileName(1_000L, "aaaa")
        val b = buildImageFileName(1_000L, "bbbb")
        assertTrue(a != b) { "Different suffixes must yield different filenames" }
    }
}
