// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import it.marcelpetrick.catears.domain.EarAdjustment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Preferences-DataStore implementation of [EarAdjustmentStoreSeam].
 *
 * Two float keys hold the offsets. Reads run through [EarAdjustment.coerced] so a
 * corrupt or out-of-range stored value can never push the ears off-screen.
 */
class DataStoreEarAdjustmentStore(private val dataStore: DataStore<Preferences>) : EarAdjustmentStoreSeam {

    override val adjustment: Flow<EarAdjustment> = dataStore.data.map { preferences ->
        EarAdjustment(
            verticalOffset = preferences[VERTICAL_OFFSET_KEY] ?: 0f,
            spreadOffset = preferences[SPREAD_OFFSET_KEY] ?: 0f,
        ).coerced()
    }

    override suspend fun save(adjustment: EarAdjustment) {
        dataStore.edit { preferences ->
            preferences[VERTICAL_OFFSET_KEY] = adjustment.verticalOffset
            preferences[SPREAD_OFFSET_KEY] = adjustment.spreadOffset
        }
    }

    override suspend fun reset() {
        dataStore.edit { preferences ->
            preferences.remove(VERTICAL_OFFSET_KEY)
            preferences.remove(SPREAD_OFFSET_KEY)
        }
    }

    companion object {
        private val VERTICAL_OFFSET_KEY = floatPreferencesKey("ear_fit_vertical_offset")
        private val SPREAD_OFFSET_KEY = floatPreferencesKey("ear_fit_spread_offset")
    }
}
