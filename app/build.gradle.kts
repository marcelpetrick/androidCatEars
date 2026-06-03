// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // kotlin-android no longer applied separately; built into AGP 9.0+
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
}

// APK output naming: produces androidCatEars-debug.apk / androidCatEars-release.apk
base {
    archivesName = "androidCatEars"
}

// Short git commit hash (first 7 chars) injected into BuildConfig; "unknown" if git is unavailable.
val gitCommitHash: String =
    providers
        .exec {
            commandLine("git", "rev-parse", "--short=7", "HEAD")
        }.standardOutput.asText
        .map { it.trim() }
        .orElse("unknown")
        .get()

// Release signing — credentials come from a gitignored keystore.properties or
// environment variables; never from committed sources. When neither is present
// (e.g. a contributor or CI without the keystore), signing is skipped and the
// release build produces an unsigned APK so the build still succeeds.
val keystorePropsFile = rootProject.file("keystore.properties")
val signingProps: Map<String, String?> =
    if (keystorePropsFile.exists()) {
        val props = Properties()
        keystorePropsFile.inputStream().use { props.load(it) }
        mapOf(
            "storeFile" to props.getProperty("storeFile"),
            "storePassword" to props.getProperty("storePassword"),
            "keyAlias" to props.getProperty("keyAlias"),
            "keyPassword" to props.getProperty("keyPassword"),
        )
    } else {
        mapOf(
            "storeFile" to System.getenv("RELEASE_STORE_FILE"),
            "storePassword" to System.getenv("RELEASE_STORE_PASSWORD"),
            "keyAlias" to System.getenv("RELEASE_KEY_ALIAS"),
            "keyPassword" to System.getenv("RELEASE_KEY_PASSWORD"),
        )
    }
// Treat blank strings (empty env vars from CI) the same as absent; file("") crashes Gradle.
val hasReleaseSigning =
    signingProps["storeFile"]
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { file(it).exists() }.getOrDefault(false) } == true

android {
    namespace = "it.marcelpetrick.catears"
    compileSdk = 36

    defaultConfig {
        applicationId = "it.marcelpetrick.catears"
        minSdk = 34
        targetSdk = 36
        versionCode = rootProject.extra["appVersionCode"] as Int
        versionName = rootProject.extra["appVersionName"] as String
        buildConfigField("String", "GIT_COMMIT", "\"$gitCommitHash\"")
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(signingProps.getValue("storeFile")!!)
                storePassword = signingProps["storePassword"]
                keyAlias = signingProps["keyAlias"]
                keyPassword = signingProps["keyPassword"]
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Attach the release signing config only when credentials are available;
            // otherwise the build emits an unsigned release APK.
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else null
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
        buildConfig = true
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
                    // Generated BuildConfig — no logic
                    "it.marcelpetrick.catears.BuildConfig",
                    // Application class (framework entry point, no logic)
                    "it.marcelpetrick.catears.CatEarsApplication",
                    "it.marcelpetrick.catears.CatEarsApplication*",
                    // CameraX concrete implementation and Composable — lifecycle-bound, device-only
                    "it.marcelpetrick.catears.camera.CameraXControllerImpl",
                    "it.marcelpetrick.catears.camera.CameraXControllerImpl*",
                    "it.marcelpetrick.catears.camera.CameraPreviewComposableKt",
                    // Share intent builder — toChooserIntent() needs Android Intent (not mocked in JVM tests)
                    "it.marcelpetrick.catears.share.ShareConfigKt",
                    // Canvas-based overlay renderer — DrawScope + graphicsLayer are Android-only
                    "it.marcelpetrick.catears.overlay.CatEarOverlayKt",
                    // Compositor, Saver, and drawable decoder — require real Android Bitmap/Canvas/MediaStore
                    "it.marcelpetrick.catears.capture.OverlayCompositor",
                    "it.marcelpetrick.catears.capture.ImageSaver",
                    "it.marcelpetrick.catears.capture.ImageSaver*",
                    "it.marcelpetrick.catears.capture.DrawableBitmapKt",
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
