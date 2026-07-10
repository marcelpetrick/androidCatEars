// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.settings

import it.marcelpetrick.catears.domain.EarAdjustment
import kotlinx.coroutines.flow.Flow

/**
 * Seam for persisting the user's [EarAdjustment] locally on the device.
 *
 * Mirrors the other `*Seam` interfaces: the ViewModel depends only on this
 * abstraction so tests can substitute an in-memory fake.
 */
interface EarAdjustmentStoreSeam {

    /** The stored adjustment; emits [EarAdjustment.DEFAULT] when nothing is saved. */
    val adjustment: Flow<EarAdjustment>

    /** Persists [adjustment]; values are clamped on read, so any input is safe to store. */
    suspend fun save(adjustment: EarAdjustment)

    /** Removes the stored adjustment, returning to the auto-fit default. */
    suspend fun reset()
}
