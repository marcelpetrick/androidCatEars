// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    alias(libs.plugins.android.application)
    // kotlin-android no longer applied separately; built into AGP 9.0+
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
}

android {
    namespace = "it.marcelpetrick.catears"
    compileSdk = 36

    defaultConfig {
        applicationId = "it.marcelpetrick.catears"
        minSdk = 34
        targetSdk = 36
        versionCode = rootProject.extra["appVersionCode"] as Int
        versionName = rootProject.extra["appVersionName"] as String
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    buildFeatures {
        compose = true
    }

    lint {
        lintConfig = file("lint.xml")
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = true
        htmlReport = true
        xmlReport = false
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose (version from BOM)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ML Kit — on-device face detection
    implementation(libs.mlkit.face.detection)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Testing
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ---------------------------------------------------------------------------
// Kover — coverage gate: 95% on domain/logic; UI/DI/generated excluded (Q3)
// ---------------------------------------------------------------------------
kover {
    reports {
        filters {
            excludes {
                classes(
                    // Compose UI — Activities, Screens, themes, previews
                    "it.marcelpetrick.catears.ui.*",
                    // DI wiring — pure framework glue, no testable logic
                    "it.marcelpetrick.catears.di.*",
                    // Hilt-generated classes (all known patterns)
                    "*_HiltComponents*",
                    "*_MembersInjector*",
                    "*Hilt_*",
                    "*_GeneratedInjector*",
                    "dagger.hilt.*",
                    "hilt_aggregated_deps.*",
                    // Application class (framework entry point, no logic)
                    "it.marcelpetrick.catears.CatEarsApplication",
                    "it.marcelpetrick.catears.CatEarsApplication*",
                    // CameraX concrete implementation and Composable — lifecycle-bound, device-only
                    "it.marcelpetrick.catears.camera.CameraXControllerImpl",
                    "it.marcelpetrick.catears.camera.CameraXControllerImpl*",
                    "it.marcelpetrick.catears.camera.CameraPreviewComposableKt",
                    // Share intent builder — toChooserIntent() needs Android Intent (not mocked in JVM tests)
                    "it.marcelpetrick.catears.share.ShareConfigKt",
                    // Compositor and Saver — require real Android Bitmap/Canvas/MediaStore
                    "it.marcelpetrick.catears.capture.OverlayCompositor",
                    "it.marcelpetrick.catears.capture.ImageSaver",
                    "it.marcelpetrick.catears.capture.ImageSaver*",
                    // ML Kit face detector implementation — device-only
                    "it.marcelpetrick.catears.facedetect.MlKitFaceDetectorImpl",
                    "it.marcelpetrick.catears.facedetect.MlKitFaceDetectorImpl*",
                    "it.marcelpetrick.catears.facedetect.MlKitFaceDetectorImplKt",
                    // Hilt-generated factories
                    "*_Factory",
                    "*_Factory*",
                )
                annotatedBy(
                    "dagger.hilt.android.HiltAndroidApp",
                    "dagger.hilt.android.AndroidEntryPoint",
                    "dagger.Module",
                    "androidx.compose.runtime.Composable",
                    "androidx.compose.ui.tooling.preview.Preview",
                )
            }
        }
        verify {
            rule("Domain coverage gate — 95% line coverage required") {
                minBound(95)
            }
        }
    }
}
