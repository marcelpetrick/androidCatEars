// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HelpContentProviderTest {

    @Test
    fun `REPO_URL starts with https`() {
        assertTrue(REPO_URL.startsWith("https://"))
    }

    @Test
    fun `FEEDBACK_URL starts with https`() {
        assertTrue(FEEDBACK_URL.startsWith("https://"))
    }

    @Test
    fun `FEEDBACK_URL contains REPO_URL as prefix`() {
        assertTrue(FEEDBACK_URL.startsWith(REPO_URL))
    }

    @Test
    fun `all languages produce non-blank appPitch`() {
        HelpLanguage.entries.forEach { lang ->
            assertFalse(helpContent(lang).appPitch.isBlank(), "appPitch blank for $lang")
        }
    }

    @Test
    fun `all languages produce non-blank authorLine`() {
        HelpLanguage.entries.forEach { lang ->
            assertTrue(helpContent(lang).authorLine.contains("Marcel Petrick"), "authorLine missing name for $lang")
        }
    }

    @Test
    fun `all languages produce non-empty features list`() {
        HelpLanguage.entries.forEach { lang ->
            assertTrue(helpContent(lang).features.isNotEmpty(), "features empty for $lang")
        }
    }

    @Test
    fun `all languages have the same number of features`() {
        val counts = HelpLanguage.entries.map { helpContent(it).features.size }.toSet()
        assertEquals(1, counts.size, "feature counts differ across languages: $counts")
    }

    @Test
    fun `all languages have the same number of how-to steps`() {
        val counts = HelpLanguage.entries.map { helpContent(it).howToUseSteps.size }.toSet()
        assertEquals(1, counts.size, "step counts differ across languages: $counts")
    }

    @Test
    fun `all languages produce non-blank closeLabel`() {
        HelpLanguage.entries.forEach { lang ->
            assertFalse(helpContent(lang).closeLabel.isBlank(), "closeLabel blank for $lang")
        }
    }

    @Test
    fun `all languages produce non-blank feedbackInvitation`() {
        HelpLanguage.entries.forEach { lang ->
            assertFalse(helpContent(lang).feedbackInvitation.isBlank(), "feedbackInvitation blank for $lang")
        }
    }

    @Test
    fun `English content contains English text`() {
        val content = helpContent(HelpLanguage.ENGLISH)
        assertTrue(content.closeLabel == "Close")
        assertTrue(content.featuresTitle == "Features")
        assertTrue(content.howToUseTitle == "How to use")
    }

    @Test
    fun `German content contains German text`() {
        val content = helpContent(HelpLanguage.GERMAN)
        assertTrue(content.closeLabel == "Schließen")
        assertTrue(content.featuresTitle == "Funktionen")
    }

    @Test
    fun `Mandarin content contains Chinese characters`() {
        val content = helpContent(HelpLanguage.MANDARIN)
        assertTrue(content.closeLabel == "关闭")
        assertTrue(content.featuresTitle == "功能特性")
        assertTrue(content.appPitch.any { it.code > 0x4E00 }, "Mandarin appPitch should contain CJK characters")
    }

    @Test
    fun `HelpContent data class equality works`() {
        val a = helpContent(HelpLanguage.ENGLISH)
        val b = helpContent(HelpLanguage.ENGLISH)
        assertEquals(a, b)
    }

    @Test
    fun `HelpContent copy produces distinct instance with changed field`() {
        val original = helpContent(HelpLanguage.ENGLISH)
        val copy = original.copy(closeLabel = "Done")
        assertTrue(copy.closeLabel == "Done")
        assertTrue(original.closeLabel == "Close")
    }
}
