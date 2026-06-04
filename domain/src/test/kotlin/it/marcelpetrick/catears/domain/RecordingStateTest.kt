// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class RecordingStateTest {

    @Test
    fun `state labels cover all variants`() {
        assertEquals("idle", RecordingState.Idle.label())
        assertEquals("recording", RecordingState.Recording.label())
        assertEquals("saved", RecordingState.Saved("content://media/video/1").label())
        assertEquals("failed", RecordingState.Failed.label())
    }

    @Test
    fun `Saved holds its uriString`() {
        val state = RecordingState.Saved("content://media/video/1")
        assertEquals("content://media/video/1", state.uriString)
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

    private fun RecordingState.label(): String = when (this) {
        RecordingState.Idle -> "idle"
        RecordingState.Recording -> "recording"
        is RecordingState.Saved -> "saved"
        RecordingState.Failed -> "failed"
    }
}
