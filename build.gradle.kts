// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

import java.util.Properties

// Single source of truth for the app version.
// All submodules read versionName / versionCode from here via extra properties.
val versionProps = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val major = versionProps.getProperty("major").toInt()
val minor = versionProps.getProperty("minor").toInt()
val patch = versionProps.getProperty("patch").toInt()

extra["appVersionName"] = "$major.$minor.$patch"
// versionCode: major*100000 + minor*1000 + patch (supports up to 99 minor / 999 patch)
extra["appVersionCode"] = major * 100_000 + minor * 1_000 + patch

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.hilt)                apply false
    alias(libs.plugins.ksp)                 apply false
    alias(libs.plugins.detekt)              apply false
    alias(libs.plugins.spotless)            apply false
    alias(libs.plugins.kover)               apply false
}
