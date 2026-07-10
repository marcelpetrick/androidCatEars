// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import it.marcelpetrick.catears.domain.EarAdjustment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreEarAdjustmentStoreTest {

    @TempDir
    lateinit var tempDir: File

    private fun TestScopeContext.store(): DataStoreEarAdjustmentStore = DataStoreEarAdjustmentStore(dataStore)

    private class TestScopeContext(val dataStore: DataStore<Preferences>, val scope: CoroutineScope)

    private fun runStoreTest(body: suspend TestScopeContext.() -> Unit) = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(tempDir, "ear_fit_test.preferences_pb")
        }
        val context = TestScopeContext(dataStore, scope)
        try {
            context.body()
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `empty store emits the default adjustment`() = runStoreTest {
        assertEquals(EarAdjustment.DEFAULT, store().adjustment.first())
    }

    @Test
    fun `saved adjustment is read back`() = runStoreTest {
        val store = store()
        val saved = EarAdjustment(verticalOffset = -0.1f, spreadOffset = 0.25f)
        store.save(saved)
        assertEquals(saved, store.adjustment.first())
    }

    @Test
    fun `out-of-range stored values are clamped on read`() = runStoreTest {
        val store = store()
        store.save(EarAdjustment(verticalOffset = 9f, spreadOffset = -9f))
        val read = store.adjustment.first()
        assertEquals(EarAdjustment.MAX_VERTICAL_OFFSET, read.verticalOffset)
        assertEquals(-EarAdjustment.MAX_SPREAD_OFFSET, read.spreadOffset)
    }

    @Test
    fun `reset returns to the default adjustment`() = runStoreTest {
        val store = store()
        store.save(EarAdjustment(verticalOffset = 0.2f, spreadOffset = 0.3f))
        store.reset()
        assertEquals(EarAdjustment.DEFAULT, store.adjustment.first())
    }

    @Test
    fun `values survive a new store instance over the same file`() = runStoreTest {
        // Same DataStore instance shared by two seam wrappers — mirrors process-internal reuse.
        val saved = EarAdjustment(verticalOffset = 0.05f, spreadOffset = -0.15f)
        store().save(saved)
        assertEquals(saved, store().adjustment.first())
    }
}
