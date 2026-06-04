// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecordingStateTest {

    @Test
    fun `Idle is a RecordingState`() {
        assertTrue(RecordingState.Idle is RecordingState)
    }

    @Test
    fun `Recording is a RecordingState`() {
        assertTrue(RecordingState.Recording is RecordingState)
    }

    @Test
    fun `Saved holds its uriString`() {
        val state = RecordingState.Saved("content://media/video/1")
        assertEquals("content://media/video/1", state.uriString)
    }

    @Test
    fun `Failed is a RecordingState`() {
        assertTrue(RecordingState.Failed is RecordingState)
    }

    @Test
    fun `Saved equality matches on same uri`() {
        assertEquals(RecordingState.Saved("uri"), RecordingState.Saved("uri"))
    }

    @Test
    fun `Saved instances with different uris are not equal`() {
        assertNotEquals(RecordingState.Saved("a"), RecordingState.Saved("b"))
    }

    @Test
    fun `Idle and Recording are not equal`() {
        assertNotEquals(RecordingState.Idle as RecordingState, RecordingState.Recording as RecordingState)
    }
}
