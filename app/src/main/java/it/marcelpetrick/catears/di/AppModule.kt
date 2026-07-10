// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import it.marcelpetrick.catears.capture.CaptureRuntime
import it.marcelpetrick.catears.capture.DefaultCaptureRuntime
import it.marcelpetrick.catears.settings.DataStoreEarAdjustmentStore
import it.marcelpetrick.catears.settings.EarAdjustmentStoreSeam
import javax.inject.Singleton

private val Context.earFitDataStore by preferencesDataStore(name = "ear_fit")

/**
 * Root Hilt module. Infrastructure bindings wired here as features land.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCaptureRuntime(): CaptureRuntime = DefaultCaptureRuntime()

    @Provides
    @Singleton
    fun provideEarAdjustmentStore(@ApplicationContext context: Context): EarAdjustmentStoreSeam =
        DataStoreEarAdjustmentStore(context.earFitDataStore)
}
