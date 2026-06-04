// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.di

import androidx.camera.core.ExperimentalGetImage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import it.marcelpetrick.catears.facedetect.FaceDetectorSeam
import it.marcelpetrick.catears.facedetect.MlKitFaceDetectorImpl

@Module
@InstallIn(ActivityComponent::class)
@androidx.annotation.OptIn(ExperimentalGetImage::class)
abstract class FaceDetectModule {

    @Binds
    abstract fun bindFaceDetector(impl: MlKitFaceDetectorImpl): FaceDetectorSeam
}
