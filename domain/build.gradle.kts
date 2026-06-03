// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    id("com.android.lint")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit5.params)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ---------------------------------------------------------------------------
// Kover — 95% line coverage gate for all domain logic
// ---------------------------------------------------------------------------
kover {
    reports {
        verify {
            rule("Domain coverage gate — 95% line coverage required") {
                minBound(95)
            }
        }
    }
}
