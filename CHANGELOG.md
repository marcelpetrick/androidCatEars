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
- **Animated 3D-look cat ears (WP 20)** — replaced the static PNG sprite with a
  procedural Compose Canvas renderer. Cat ears are now anchored to ML Kit's
  human-ear landmarks (no more flying ears from bounding-box noise), drawn as
  warm-brown outer + pink inner triangle shapes, animated with 5 swaying fur
  strands per ear (1.25 Hz sine-wave sway + cosine bobble) and a periodic
  ear-tip twitch at 0.2 Hz. Per-ear perspective X-squash driven by head yaw
  gives a 3D depth illusion. Spring-animated tilt (left ear 0.6×, right ear
  1.0× head roll) adds comical elastic lag on rapid head snaps. Both live
  preview and still-photo capture use the same procedural geometry.
- **Title bar & nav-bar-aware FABs** — semi-transparent "AndroidCatEars vX.Y.Z"
  pill at the top of the camera screen (status-bar-inset). Capture, switch, and
  share FABs wrapped in `navigationBarsPadding()` Box so they are never hidden
  behind the Android gesture navigation bar.
- **Project logo** — SVG cat-face logo added under `media/logo.svg` and
  embedded in the README header.
- **`:domain` JVM module (WP 17.0)** — all pure geometry/state classes moved
  out of `:app` into a standalone `:domain` Kotlin JVM library. `:app` depends
  on it via `project(":domain")`; package names unchanged.
- **Golden-fixture placement tests (WP 17.1)** — 8 `@ParameterizedTest` cases
  cover frontal/roll/yaw/extreme-yaw/landmark/partial-landmark scenarios for
  `computeOverlayPlacement`.
- **Supply chain (WP 18.4)** — `.github/dependabot.yml` for weekly Gradle and
  GitHub Actions version PRs; CodeQL, dependency-review, and Gitleaks workflows
  were already in place.

### Notes
- `minSdk 34`; older devices (e.g. Android 12) are intentionally unsupported.
- Live face-tracking accuracy and overlay-constant tuning are pending
  verification on real hardware (emulators have no real face input).
- Host-side Compose UI tests (Robolectric 4.14.1) are blocked on JDK 26;
  Robolectric's ASM 9.7 only supports up to Java 24 class files.

[Unreleased]: https://github.com/marcelpetrick/androidCatEars/commits/main
