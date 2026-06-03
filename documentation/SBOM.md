<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# SBOM Plan

This project should generate a Software Bill of Materials automatically from
Gradle, in CycloneDX format.

## Short answer

Yes, this is straightforward for this repo.

There is no Kotlin-specific SBOM standard that should be invented here. The
standard is the SBOM format: use **CycloneDX** for this project. Kotlin and
Android dependencies are resolved by Gradle, so the right integration point is a
Gradle plugin that reads the managed dependency graph.

Use the official CycloneDX Gradle plugin:

```kotlin
plugins {
    id("org.cyclonedx.bom") version "<pinned-version>"
}
```

The current plugin version checked during planning was `3.2.4`.

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

SBOM generation must be both:

1. **Locally triggerable** by a developer before pushing or cutting a release.
2. **Release-workflow automated** in GitHub Actions so every GitHub Release gets
   SBOM files generated from the exact dependency graph used for the signed AAB.

The local trigger should be a stable Gradle task:

```bash
./gradlew cyclonedxBom
```

Optionally add a small wrapper later if the release workflow needs renaming or
checksums locally too:

```bash
./scripts/generate-sbom.sh
```

The wrapper is not required for the first implementation because the managed
Gradle task is already the source of truth.

## Gradle integration plan

1. Add a pinned plugin version to `gradle/libs.versions.toml`.
2. Add `alias(libs.plugins.cyclonedx) apply false` to the root plugin block.
3. Apply `org.cyclonedx.bom` in the root project because this is a multi-project
   build (`:app` + `:domain`).
4. Configure the aggregate `cyclonedxBom` task as the main project SBOM:
   - `projectType = "application"`
   - `componentName = "androidCatEars"`
   - `componentVersion = rootProject.extra["appVersionName"].toString()`
   - include production classpaths only where possible
   - skip test, lint, detekt, kover, and build-tool-only configurations
   - keep `includeBomSerialNumber = true`
   - keep `includeMetadataResolution = true`
   - set `includeBuildSystem = true` in CI
5. Leave `cyclonedxDirectBom` available for debugging per-project dependency
   graphs, but do not make direct per-module SBOMs the release artifact.

Expected local commands after implementation:

```bash
./gradlew cyclonedxBom
./gradlew cyclonedxDirectBom
```

The aggregate task is the one that should be wired into CI and release
automation.

## CI integration plan

Add SBOM generation after the existing quality gate succeeds:

```bash
./gradlew cyclonedxBom
```

Then upload the generated SBOM files as CI artifacts. This keeps the source tree
clean while still making every CI run inspectable.

The local `scripts/ci.sh` should grow one extra measured step:

```text
sbom | ./gradlew cyclonedxBom | PASS/FAIL | duration
```

That keeps the local ASCII summary aligned with GitHub Actions.

## Release integration plan

The SBOM generation workflow belongs in the existing manual GitHub Actions
release workflow (`.github/workflows/release.yml`), after the quality gate and
before publishing the GitHub Release. That ensures the SBOM describes the same
source revision and resolved dependency graph as the signed release artifact.

The manual release workflow should:

1. Run the existing full quality gate.
2. Build and verify the signed AAB.
3. Run `./gradlew cyclonedxBom`.
4. Copy `build/reports/cyclonedx/bom.json` to
   `androidCatEars-<version>.cdx.json`.
5. Copy `build/reports/cyclonedx/bom.xml` to
   `androidCatEars-<version>.cdx.xml`.
6. Generate SHA-256 checksum files for both SBOM files.
7. Attach the SBOM files and checksums to the GitHub Release beside the signed
   AAB.

Do not commit generated SBOMs. They are build artifacts tied to a specific
resolved dependency graph and release version.

For local release rehearsal, the same steps should be runnable without GitHub:

```bash
./gradlew cyclonedxBom
VERSION="$(awk -F= '/major/{major=$2} /minor/{minor=$2} /patch/{patch=$2} END {print major "." minor "." patch}' version.properties)"
cp build/reports/cyclonedx/bom.json "androidCatEars-${VERSION}.cdx.json"
cp build/reports/cyclonedx/bom.xml "androidCatEars-${VERSION}.cdx.xml"
sha256sum "androidCatEars-${VERSION}.cdx.json" > "androidCatEars-${VERSION}.cdx.json.sha256"
sha256sum "androidCatEars-${VERSION}.cdx.xml" > "androidCatEars-${VERSION}.cdx.xml.sha256"
```

If these shell steps become part of regular release rehearsal, move them into a
tracked script so local and GitHub release behavior cannot drift.

## Validation

Minimum validation:

```bash
./gradlew cyclonedxBom
test -s build/reports/cyclonedx/bom.json
test -s build/reports/cyclonedx/bom.xml
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

## Backlog task proposal

Add one implementation task under WP 18:

```text
18.5 | TODO | CycloneDX SBOM automation | Add the CycloneDX Gradle plugin, generate aggregate CycloneDX JSON/XML SBOMs locally and in CI, upload CI artifacts, attach versioned SBOMs + SHA-256 checksums to GitHub Releases, and document the commands in README/GITHUB_ACTIONS.
```

Acceptance criteria:

1. `./gradlew cyclonedxBom` produces non-empty JSON and XML SBOMs.
2. `scripts/ci.sh` runs SBOM generation and includes it in the final ASCII
   summary.
3. `ci.yml` uploads SBOM artifacts after a green quality gate.
4. `release.yml` generates SBOMs during the manual release workflow and attaches
   versioned SBOM files plus SHA-256 checksums to GitHub Releases.
5. README and `documentation/GITHUB_ACTIONS.md` document where SBOMs are
   generated and published.
