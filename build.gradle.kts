// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

import java.util.Properties

// Single source of truth for the app version.
// All submodules read versionName / versionCode from here via extra properties.
val versionProps =
    Properties().apply {
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
    // kotlin-android is intentionally absent: AGP 9.0+ provides Kotlin support built-in
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.kover) apply false
}

// ---------------------------------------------------------------------------
// Spotless — formatting gate for Kotlin, KTS, XML, TOML, Markdown, shell
// ---------------------------------------------------------------------------
apply(plugin = "com.diffplug.spotless")

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        // Allow PascalCase for @Composable functions (Compose convention)
        ktlint().editorConfigOverride(
            mapOf("ktlint_function_naming_ignore_when_annotated_with" to "Composable,Preview"),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**/*.kts")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("xml") {
        target("**/*.xml")
        targetExclude("**/build/**/*.xml", ".idea/**/*.xml")
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("misc") {
        target("**/*.toml", "**/*.md", "**/*.sh", "**/*.bash", "**/*.properties")
        targetExclude("**/build/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// ---------------------------------------------------------------------------
// detekt — static analysis
// ---------------------------------------------------------------------------
apply(plugin = "io.gitlab.arturbosch.detekt")

configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    source.setFrom(
        files(
            "$rootDir/app/src/main/java",
            "$rootDir/app/src/test/java",
        ),
    )
}

// ---------------------------------------------------------------------------
// Quality gate — single task that runs all checks
// ---------------------------------------------------------------------------
tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs spotlessCheck, detekt, and Android lint in one command."
    dependsOn("spotlessCheck", "detekt")
    // lint lives in the :app subproject; add it once the module is resolved
    dependsOn(":app:lint")
}

tasks.register("installHooks") {
    group = "setup"
    description = "Copies scripts/pre-commit into .git/hooks/ and makes it executable."
    doLast {
        val hooksDir = rootProject.file(".git/hooks")
        val src = rootProject.file("scripts/pre-commit")
        val dst = hooksDir.resolve("pre-commit")
        src.copyTo(dst, overwrite = true)
        dst.setExecutable(true)
        println("Installed pre-commit hook → ${dst.absolutePath}")
    }
}
