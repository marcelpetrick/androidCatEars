// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.marcelpetrick.catears.ui.CaptureRuntime
import it.marcelpetrick.catears.ui.DefaultCaptureRuntime
import javax.inject.Singleton

/**
 * Root Hilt module. Infrastructure bindings wired here as features land.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCaptureRuntime(): CaptureRuntime = DefaultCaptureRuntime()
}
