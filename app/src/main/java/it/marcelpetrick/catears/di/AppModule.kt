// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Root Hilt module. Bindings for camera, face detection, and other
 * infrastructure are added here as the respective work packages land.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
