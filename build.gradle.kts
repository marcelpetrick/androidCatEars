// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

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
