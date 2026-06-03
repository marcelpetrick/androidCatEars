// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LensSelectorTest {

    @Test
    fun `Front toggled returns Rear`() {
        assertEquals(LensSelector.Rear, LensSelector.Front.toggled())
    }

    @Test
    fun `Rear toggled returns Front`() {
        assertEquals(LensSelector.Front, LensSelector.Rear.toggled())
    }

    @Test
    fun `toggled twice returns original`() {
        assertEquals(LensSelector.Front, LensSelector.Front.toggled().toggled())
        assertEquals(LensSelector.Rear, LensSelector.Rear.toggled().toggled())
    }
}
