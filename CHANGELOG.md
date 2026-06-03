<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Changelog

All notable changes to **androidCatEars** are documented here. The format is
based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the
project follows [Semantic Versioning](https://semver.org/).

> **Versioning note:** the patch number auto-increments on every commit (see
> `scripts/pre-commit`), so `version.properties` advances faster than the
> milestones below. This changelog groups work by feature milestone rather than
> per-commit. Minor/major bumps are made manually at release boundaries.

## [Unreleased] — 0.1.x development

The full pre-1.0 development series. No public release has been tagged yet.

### Added
- **Foundation & quality gate** — Gradle/Kotlin DSL build, `version.properties`
  single source of truth with auto patch-bump hook, GPLv3 licensing + SPDX
  headers, Spotless (ktlint), detekt, Android Lint, JUnit5 + Kover 95% coverage
  gate, GitHub Actions CI, and a local `scripts/ci.sh` equivalent.
- **Live camera** — Hilt-wired MVVM skeleton, camera permission flow
  (granted/denied/permanently-denied with settings deep-link), CameraX preview
  bound to the lifecycle, and front/rear lens switching.
- **Face-tracked overlay** — ML Kit on-device face detection via CameraX
  `ImageAnalysis`, framework-free face model, image→view coordinate transform
  with front-camera mirroring, cat-ear placement math, Compose `Canvas`
  overlay, and jitter smoothing.
- **Capture, save, share** — WYSIWYG capture compositing the overlay in view
  space, MediaStore (scoped-storage) save with success/failure feedback, and a
  system share-sheet intent. Accessibility live-region status banner.
- **Polish** — designed adaptive launcher icon (foreground + monochrome),
  branded cat-ear-orange Material 3 theme with light/dark handling, and a
  startup version + git-commit build stamp.
- **Release engineering** — R8 minification + resource shrinking, ProGuard
  rules preserving deobfuscatable crash traces, signing config driven by a
  gitignored `keystore.properties` or `RELEASE_*` env vars (no secrets
  committed), a manual GitHub Actions release workflow (tagged release with
  signed APK + AAB), and a release guide/checklist.
- **Documentation** — vision, development plan, atomic backlog, agent rules,
  architecture overview, emulator and device-deploy guides, and this changelog.

### Notes
- `minSdk 34`; older devices (e.g. Android 12) are intentionally unsupported.
- Live face-tracking accuracy and overlay-constant tuning are pending
  verification on real hardware (emulators have no real face input).

[Unreleased]: https://github.com/marcelpetrick/androidCatEars/commits/main
