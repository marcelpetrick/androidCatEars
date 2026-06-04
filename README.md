# Android Cat Ears 😼🐾

> **The camera app that makes your face furry** — point it at yourself and watch
> animated cat ears spring to life in real time, on-device, no cloud, no lag.

<p align="center">
  <img src="media/logo.svg" alt="androidCatEars logo" width="160"/>
</p>

---

**Author: Marcel Petrick <mail@marcelpetrick.it>**

**Note: project is generated with AI.**

**License: GPLv3 or later. See `LICENSE`.**

This project is developed using agentic AI workflows — autonomous coding agents
that plan, implement, test, and iterate across the full development cycle.
Marcel Petrick is the human author: setting direction, reviewing every change,
making architectural decisions, and owning the final product. All commits are
signed off by the human author. "AI-generated" means the agents do the typing;
the human does the thinking.

---

## CI Status

[![CI](https://github.com/marcelpetrick/androidCatEars/actions/workflows/ci.yml/badge.svg)](https://github.com/marcelpetrick/androidCatEars/actions/workflows/ci.yml) [![CodeQL](https://github.com/marcelpetrick/androidCatEars/actions/workflows/codeql.yml/badge.svg)](https://github.com/marcelpetrick/androidCatEars/actions/workflows/codeql.yml) [![Dependabot Updates](https://github.com/marcelpetrick/androidCatEars/actions/workflows/dependabot/dependabot-updates/badge.svg)](https://github.com/marcelpetrick/androidCatEars/actions/workflows/dependabot/dependabot-updates) [![Dependency Review](https://github.com/marcelpetrick/androidCatEars/actions/workflows/dependency-review.yml/badge.svg)](https://github.com/marcelpetrick/androidCatEars/actions/workflows/dependency-review.yml) [![Release](https://github.com/marcelpetrick/androidCatEars/actions/workflows/release.yml/badge.svg)](https://github.com/marcelpetrick/androidCatEars/actions/workflows/release.yml) [![Secret Scan](https://github.com/marcelpetrick/androidCatEars/actions/workflows/secret-scan.yml/badge.svg)](https://github.com/marcelpetrick/androidCatEars/actions/workflows/secret-scan.yml) [![Download APK](https://img.shields.io/github/v/release/marcelpetrick/androidCatEars?label=Download%20APK&color=brightgreen)](https://github.com/marcelpetrick/androidCatEars/releases/latest)

---

## ✨ What makes it special

### Everything runs on-device — zero network access

All face detection, rendering, and compositing happens locally on the device.
No image ever leaves the phone. No account. No permissions for storage or
internet. `INTERNET` is not in the manifest — the OS enforces this at runtime.

### 10 ear styles, one tap away
Cycle through **Classic Cat · Sharp Feline · Rounded Feline · Lynx Tufted ·
Dense Fluffy · Canine Floppy · Canine Perky · Rabbit · Fox · Bear** with a
single tap on the paw-icon button. Every style renders in real time — no
loading, no lag. All styles are drawn in code: there are **no image assets** in
the APK; every triangle, curve, and strand is computed on the fly.

### Ears that actually feel alive
Each ear style is procedurally animated on every frame:
- **Fur strands sway** at 1.25 Hz with randomised phases — no two strands move the same.
- **Ear-tip twitches** fire periodically, like a real cat listening to something.
- **Spring tilt** follows your head roll with elastic overshoot — rapid head snaps make the ears wobble comically.
- **Perspective squeeze** narrows the far ear and widens the near one as you turn, giving genuine 3-D depth.

### Ears that react to your expressions
ML Kit reads your face every frame and the ears respond:
- **Broad smile** → both ears perk upward with a spring pop.
- **Wide eyes** → ears shoot up for a surprised look.
- **Wink or blink** → the ear on the closing-eye side flattens and recovers.

### Up to 4 faces at once
Point the camera at a group — every detected face gets its own independently
animated ears, each smoothed and tracked separately so they don't jump when
faces overlap.

### Party Mode for group selfies
Turn on Party Mode and every tracked face gets a stable style/tint pairing.
The first face starts classic brown, the next faces get their own looks, and
the re-roll button reshuffles the group without changing face positions.

### Baked-in capture
Tap the shutter to save a photo with the ears composited directly onto the
frame. The same procedural geometry used in the live preview is applied at
full resolution — what you see is exactly what you get. Share via any Android
app with one more tap.

For video, five-second MP4 clips are recorded with the overlay baked into
every frame at record time via CameraX `OverlayEffect` (`camera-effects:1.6.1`).
The ears are drawn directly into the video buffer — no post-processing step.

---

## What It Does

- Live camera preview with front/rear camera switching
- On-device face detection via ML Kit (landmarks, head pose, per-eye expression)
- Animated procedural ears — 10 styles, 6 tints, expression-reactive, tracks up to 4 faces
- Party Mode with stable per-face style/tint assignments and manual re-roll
- Still photo capture with the overlay baked in at full resolution
- Five-second MP4 recording with the overlay baked in via `CameraEffect`
- Save to gallery and share photos/videos via the Android share sheet
- No internet permission, no cloud, no account — fully private by design

## Tech Stack

| Concern | Technology |
|---------|------------|
| Language | Kotlin 2.3.21 |
| UI | Jetpack Compose (BOM 2026.05.01) |
| Camera | CameraX 1.6.1 |
| Video overlay | `camera-effects:1.6.1` — `OverlayEffect` bakes ears into frames at record time |
| Face detection | ML Kit Face Detection 16.1.7 — fully on-device, no network |
| DI | Hilt 2.59.2 |
| Build | Gradle 9.5.1 (Kotlin DSL) |
| Min SDK | Android 14 (API 34) |
| Target SDK | Android 16 (API 36) |
| Drawing | 100% procedural — no PNG/WebP/SVG assets |

## Module structure and testability

The project is split into two Gradle modules:

- **`:domain`** — pure JVM, zero Android dependencies. All geometry
  (`computeOverlayPlacement`, `imageToViewCoordinates`), state machines
  (`CaptureState`, `RecordingState`, `MainUiState`), and face-smoothing
  (`MultiFaceSmoother`) live here. 40+ tests run in milliseconds on any JVM
  — no Robolectric, no emulator.
- **`:app`** — Android module. CameraX, Compose UI, ML Kit, Hilt wiring.
  The boundary is enforced at compile time: `:domain` cannot import anything
  from `:app`.

Camera, ML Kit, and I/O are hidden behind seam interfaces
(`CameraControllerSeam`, `FaceDetectorSeam`, `CaptureRuntime`) injected via
Hilt, so the ViewModel and all domain logic are exercised with plain Kotlin
test doubles. A **95% Kover line-coverage gate** on `:domain` runs in CI
and blocks merges when it drops.

See [`documentation/ARCHITECTURE.md`](documentation/ARCHITECTURE.md) for the
full module map and MVVM wiring.

## Getting started (contributors)

```bash
git clone https://github.com/marcelpetrick/androidCatEars
cd androidCatEars
./gradlew installHooks   # required once per clone — installs the pre-commit version bumper
./gradlew assembleDebug  # downloads Gradle 9.5.1 and all deps on first run
./gradlew installDebug   # push to a connected device or running emulator
```

Read [`documentation/agents.md`](documentation/agents.md) before opening a PR.
It contains the binding engineering rules: commit format (Conventional Commits),
quality gates that must pass, what must never be bypassed (hooks, coverage).

---

## CI / Quality Gate

The required local gate is [`scripts/ci.sh`](scripts/ci.sh). Run it before
pushing; GitHub Actions invokes the same script so local and remote CI cannot
drift.

```bash
./scripts/ci.sh            # build + format + detekt + lint + tests + Kover (≥95%)
./gradlew spotlessApply    # auto-fix formatting
./gradlew qualityCheck     # format + detekt + lint in one command
./gradlew :app:koverVerify # domain coverage gate (fails if < 95%)
```

If a local gate step fails, `scripts/ci.sh` prints the failed step and command.
GitHub CI uploads test, coverage, lint, and CycloneDX SBOM reports as workflow
artifacts.

Example summary from a successful local run:

```text
+----+----------------------------------+----------+----------+
| #  | Step                             | Status   | Wall     |
+----+----------------------------------+----------+----------+
| 1  | Build (debug)                    | PASSED   | 00:51    |
| 2  | Format check (Spotless)          | PASSED   | 00:01    |
| 3  | Static analysis (detekt)         | PASSED   | 00:01    |
| 4  | Android Lint                     | PASSED   | 00:18    |
| 5  | Unit tests                       | PASSED   | 00:08    |
| 6  | Coverage gate (Kover >= 95%)     | PASSED   | 00:01    |
| 7  | SBOM (CycloneDX)                 | PASSED   | 00:06    |
+----+----------------------------------+----------+----------+
| Total                                 | PASSED   | 01:26    |
+---------------------------------------+----------+----------+
All checks passed.
```

GitHub Actions also runs security/release workflows:

- `ci.yml` — push/PR quality gate using `scripts/ci.sh`.
- `dependency-review.yml` — blocks vulnerable dependency changes on PRs.
- `codeql.yml` — Java/Kotlin code scanning on push/PR and weekly schedule.
- `secret-scan.yml` — Gitleaks scan for committed credentials.
- `release.yml` — manual release publishing with AAB, debug APK, CycloneDX SBOMs, and checksums.

CycloneDX SBOMs can also be generated locally:

```bash
./gradlew cyclonedxBom              # raw aggregate SBOM under build/reports/cyclonedx/
./scripts/generate-sbom.sh          # versioned release-style SBOM files + SHA-256 checksums
```

See [`documentation/GITHUB_ACTIONS.md`](documentation/GITHUB_ACTIONS.md) for
workflow triggers, secrets, timeouts, and release behavior.

## License

[GPL v3](LICENSE) — copyleft. Modifications and derived works must be
distributed under the same licence.

---

## Prerequisites

- **JDK 21+** (JDK 17 is the compile-time minimum but Gradle 9.5 recommends
  JDK 21+; using JDK 17 is unsupported and may produce unexpected errors)
- **Android SDK** — set `sdk.dir` in `local.properties` (not committed):
  ```properties
  sdk.dir=/path/to/Android/Sdk
  ```
  Required SDK components: `platforms;android-36`, `build-tools;36.0.0`.
  ```bash
  sdkmanager "platforms;android-36" "build-tools;36.0.0"
  ```
- `./gradlew` downloads Gradle 9.5.1 automatically on first run.

> **Why Android 14 (API 34) minimum?** The `CameraEffect` / `OverlayEffect`
> API used to bake the overlay into recorded video frames was introduced in
> CameraX 1.4 targeting API 34. Supporting lower APIs would require a
> significantly more complex off-device compositing approach.

## Build

```bash
./gradlew assembleDebug
```

Install on a connected device or running emulator:

```bash
./gradlew installDebug
```

## Usage

1. On first launch, grant the **camera** permission when prompted (if denied
   permanently, the app links you to system settings to re-enable it).
2. The live preview fills the screen. A title bar at the top shows the version
   and git commit stamp so you can always identify the running build.
3. Tap the **style** button (bottom-right, labelled with the current style name
   and a paw icon) to cycle through all 10 ear styles — Classic, Sharp Feline,
   Rounded Feline, Lynx Tufted, Dense Fluffy, Canine Floppy, Canine Perky,
   Rabbit, Fox, Bear. Each tap advances to the next; it wraps back to Classic.
4. Tap the **colour** button (palette icon) to cycle the ear tint.
5. Tap the **Party Mode** button (celebration icon) for group selfies. Each
   tracked face gets its own stable style/tint pairing; while Party Mode is on,
   tap the **re-roll** button to reshuffle those pairings.
6. Tap the **switch** button (bottom-right, below the style controls) to flip
   between front and rear cameras. Cat ears track detected faces in real time.
7. Tap the **record** button to save a five-second MP4 clip. Tap the red stop
   button to end the clip early.
8. Tap the **capture** button (bottom-centre) to take a photo with the ears
   baked in; it is saved to your gallery and a status banner confirms it.
9. Tap the **share** buttons (bottom-left, appearing after a capture or
   recording) to send the saved photo or video via the Android share sheet.

### Make the ears react

The ears don't just sit there — they respond to your face. Try it:

- **Tilt your head** → the ears tilt with it, lagging slightly for a springy, comical wobble.
- **Turn left or right** → the ears squeeze in perspective, the far one narrowing as if seen at an angle.
- **Smile big** → both ears perk up.
- **Open your eyes wide** → the ears shoot up in surprise.
- **Wink** → the ear on that side flattens, then springs back.
- **Bring in friends** → up to four faces each get animated ears at the same time.
- **Turn on Party Mode** → each face keeps its own style/tint until you re-roll.

> Face tracking needs a real face in view. The emulator can use a host webcam
> (`-camera-front webcam0`) for local testing, but a physical Android 14+
> device remains the final check for camera behavior and overlay alignment.

## Release Build & Deploy

Build a minified, resource-shrunk release APK or an App Bundle for the Play Store:

```bash
./gradlew assembleRelease   # -> app/build/outputs/apk/release/androidCatEars-release.apk
./gradlew bundleRelease     # -> app/build/outputs/bundle/release/androidCatEars-release.aab
```

Release **signing** is driven by a gitignored `keystore.properties` (copy
`keystore.properties.example`) or by `RELEASE_*` environment variables — **no
secrets are committed**. In GitHub Actions, the release keystore is stored as
`RELEASE_KEYSTORE_BASE64`; the workflow decodes it into a runner-local file,
builds a signed AAB when all signing secrets are configured, and publishes the
AAB, debug APK, CycloneDX SBOMs, and SBOM SHA-256 checksums to GitHub Releases.

When no credentials are present, local release builds still succeed but produce
an *unsigned* artifact (`…-release-unsigned.apk`).

See [`documentation/RELEASE.md`](documentation/RELEASE.md) for the full release
checklist and keystore setup. See
[`documentation/GITHUB_ACTIONS.md`](documentation/GITHUB_ACTIONS.md) for the
manual GitHub release workflow.

---

## Versioning

The project uses [Semantic Versioning](https://semver.org/). The single source of truth is
`version.properties` in the repository root:

```properties
major=0
minor=1
patch=0
```

`build.gradle.kts` reads this file and exposes `appVersionName` (e.g. `0.1.0`) and
`appVersionCode` to the app module. **Never hardcode version strings elsewhere.**

The patch number is **auto-incremented on every commit** by a git pre-commit hook in `scripts/pre-commit`.

**First-time setup** — install the hook (required once per clone):
```bash
./gradlew installHooks
```

**Manual minor bump** (new backwards-compatible feature):
```bash
# Edit version.properties: increment minor, set patch=0
./gradlew installHooks  # hook is already installed; no-op if re-run
```

**Manual major bump** (breaking change):
```bash
# Edit version.properties: increment major, set minor=0, patch=0
```

Never edit `versionCode` or `versionName` anywhere else — they are derived from `version.properties`.

---

## Documentation

Project documentation lives in [`documentation/`](documentation/).

| Document | What it covers |
|----------|----------------|
| [`documentation/DEV_INTRO.md`](documentation/DEV_INTRO.md) | Fresh-clone setup and Android Studio workflow |
| [`documentation/ARCHITECTURE.md`](documentation/ARCHITECTURE.md) | Module structure, MVVM, seam/testability pattern |
| [`documentation/BACKLOG.md`](documentation/BACKLOG.md) | Ordered task list for contributors and agents |
| [`documentation/agents.md`](documentation/agents.md) | Binding engineering rules (commit format, gates, what not to bypass) |
| [`documentation/RELEASE.md`](documentation/RELEASE.md) | Keystore setup, signed builds, release checklist |
| [`documentation/TROUBLESHOOTING.md`](documentation/TROUBLESHOOTING.md) | Build, coverage, emulator, device, signing issues |
| [`documentation/GITHUB_ACTIONS.md`](documentation/GITHUB_ACTIONS.md) | CI workflows, secrets, timeouts, release publishing |

See [`documentation/`](documentation/) for the full index — CI, SBOM, privacy policy,
overlay lab, ear style design docs, Play Store guide, and more.
