// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class CaptureStateTest {

    @Test
    fun `Idle is a distinct singleton`() {
        assertEquals(CaptureState.Idle, CaptureState.Idle)
    }

    @Test
    fun `Capturing is a distinct singleton`() {
        assertEquals(CaptureState.Capturing, CaptureState.Capturing)
    }

    @Test
    fun `Saved holds its uri string`() {
        val state = CaptureState.Saved("content://media/external/images/1234")
        assertEquals("content://media/external/images/1234", state.uriString)
    }

    @Test
    fun `Saved equality is based on uri string`() {
        val a = CaptureState.Saved("content://a")
        val b = CaptureState.Saved("content://a")
        val c = CaptureState.Saved("content://b")
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `Failed is a distinct singleton`() {
        assertEquals(CaptureState.Failed, CaptureState.Failed)
    }

    @Test
    fun `sealed when is exhaustive over all states`() {
        val states: List<CaptureState> = listOf(
            CaptureState.Idle,
            CaptureState.Capturing,
            CaptureState.Saved("uri"),
            CaptureState.Failed,
        )
        val labels = states.map { state ->
            when (state) {
                CaptureState.Idle -> "idle"
                CaptureState.Capturing -> "capturing"
                is CaptureState.Saved -> "saved"
                CaptureState.Failed -> "failed"
            }
        }
        assertEquals(listOf("idle", "capturing", "saved", "failed"), labels)
    }
}
