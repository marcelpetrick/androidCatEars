<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# SBOM Automation

This project generates a Software Bill of Materials automatically from Gradle,
in CycloneDX format.

## Short answer

Yes, this is straightforward for this repo.

There is no Kotlin-specific SBOM standard that should be invented here. The
standard is the SBOM format: use **CycloneDX** for this project. Kotlin and
Android dependencies are resolved by Gradle, so the right integration point is a
Gradle plugin that reads the managed dependency graph.

The project uses the official CycloneDX Gradle plugin:

```kotlin
plugins {
    id("org.cyclonedx.bom") version "<pinned-version>"
}
```

The pinned plugin version is `3.2.4`.

## Target output

Primary artifact:

```text
build/reports/cyclonedx/bom.json
```

Secondary artifact:

```text
build/reports/cyclonedx/bom.xml
```

Use JSON as the release/default artifact because it is easier for GitHub
artifacts, scanners, Dependency-Track, and humans to inspect. Keep XML enabled
because CycloneDX supports both and some downstream tooling still prefers XML.

Suggested release names:

```text
androidCatEars-<version>.cdx.json
androidCatEars-<version>.cdx.xml
androidCatEars-<version>.cdx.json.sha256
androidCatEars-<version>.cdx.xml.sha256
```

CycloneDX convention recognises `bom.json`, `bom.xml`, `*.cdx.json`, and
`*.cdx.xml`.

## Automation shape

SBOM generation is both:

1. **Locally triggerable** by a developer before pushing or cutting a release.
2. **Release-workflow automated** in GitHub Actions so every GitHub Release gets
   SBOM files generated from the exact dependency graph used for the signed AAB.

The raw local trigger is the stable Gradle task:

```bash
./gradlew cyclonedxBom
```

The release-style local trigger is the wrapper used by GitHub Actions:

```bash
./scripts/generate-sbom.sh [output-dir]
```

The wrapper runs `./gradlew cyclonedxBom`, checks the raw JSON/XML files are
non-empty, copies versioned `*.cdx.json` and `*.cdx.xml` files into the output
directory, and writes SHA-256 checksum files.

## Gradle integration

The plugin is pinned in `gradle/libs.versions.toml` and applied at the root
project because this is a multi-project build (`:app` + `:domain`).

The root aggregate `cyclonedxBom` task is the main project SBOM:

- `projectType = "application"`
- `componentName = "androidCatEars"`
- `componentGroup = "it.marcelpetrick.catears"`
- `componentVersion = rootProject.extra["appVersionName"].toString()`
- JSON output: `build/reports/cyclonedx/bom.json`
- XML output: `build/reports/cyclonedx/bom.xml`

Direct per-project SBOMs remain available through `cyclonedxDirectBom` for
debugging dependency graphs, but the release artifact is always the aggregate
root SBOM.

Local commands:

```bash
./gradlew cyclonedxBom
./gradlew cyclonedxDirectBom
```

The aggregate task is the one that should be wired into CI and release
automation.

## CI integration

`scripts/ci.sh` runs SBOM generation after the coverage gate:

```text
SBOM (CycloneDX) | ./gradlew cyclonedxBom | PASS/FAIL | duration
```

`ci.yml` invokes `scripts/ci.sh` and uploads the raw aggregate SBOM files as CI
artifacts.

## Release integration

The SBOM generation workflow belongs in the existing manual GitHub Actions
release workflow (`.github/workflows/release.yml`), after the quality gate and
before publishing the GitHub Release. That ensures the SBOM describes the same
source revision and resolved dependency graph as the signed release artifact.

The manual release workflow:

1. Run the existing full quality gate.
2. Build the release AAB.
3. Run `./scripts/generate-sbom.sh release-artefacts`.
4. Attach the SBOM files and checksums to the GitHub Release beside the AAB and
   debug APK.

Do not commit generated SBOMs. They are build artifacts tied to a specific
resolved dependency graph and release version.

For local release rehearsal, the same release-style SBOM generation is available
without GitHub:

```bash
./scripts/generate-sbom.sh
```

## Validation

Minimum validation:

```bash
./scripts/generate-sbom.sh
test -s build/reports/cyclonedx/bom.json
test -s build/reports/cyclonedx/bom.xml
test -s build/reports/cyclonedx-release/androidCatEars-$(awk -F= '/major/{major=$2} /minor/{minor=$2} /patch/{patch=$2} END {print major "." minor "." patch}' version.properties).cdx.json
test -s build/reports/cyclonedx-release/androidCatEars-$(awk -F= '/major/{major=$2} /minor/{minor=$2} /patch/{patch=$2} END {print major "." minor "." patch}' version.properties).cdx.xml
```

Better validation, if adding one more tool is acceptable later:

```bash
cyclonedx validate --input-file build/reports/cyclonedx/bom.json
cyclonedx validate --input-file build/reports/cyclonedx/bom.xml
```

The Gradle plugin should already emit valid CycloneDX documents, but explicit
validation is useful in CI because it fails early if a plugin upgrade changes
output shape or a task misconfiguration creates an empty SBOM.

## Security notes

- SBOMs are not secrets, but they reveal the dependency graph and versions.
  Publishing them with releases is still the right tradeoff for a distributed
  app because it supports vulnerability analysis and supply-chain transparency.
- Generated SBOMs must stay out of git; use CI artifacts and GitHub Release
  assets.
- Dependency Review, Dependabot, CodeQL, Gitleaks, and SBOM generation are
  complementary. SBOM generation documents what was shipped; the other workflows
  prevent or detect known bad changes.

## Status

Implemented as WP 18.5. The release workflow publishes the CycloneDX SBOM files
as GitHub Release artifacts.
