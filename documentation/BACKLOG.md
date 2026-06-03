# Backlog — Cat Ears Camera

The actionable, **atomic** task list, derived from [`DEVELOPMENT_PLAN.md`](DEVELOPMENT_PLAN.md) and
governed by [`agents.md`](agents.md).

## How to use this backlog

- Each task is **one focused aspect** sized to **one atomic commit** (or a small, tightly related set).
- Work tasks **top to bottom, in order.** Do not start a task until its predecessors are `DONE`.
- A task gives a **Goal** and **Acceptance criteria** — the *what*, not a recipe. The *how* is the
  agent's choice, within `agents.md` and the project's binding decisions (Q1–Q6 in the plan).
- Every task must also satisfy the global **Definition of Done** (build, tests ≥95% on scope, lint,
  docs, version bump, signed atomic commit, **no push**).
- If a task is too big to stay atomic, **split it into sub-tasks here first**, then start.
- Task IDs are `<WP>.<seq>` and are stable. Update **Status** as you go.

**Status legend:** `TODO` · `IN PROGRESS` · `DONE` · `BLOCKED` (note the blocker) · `ASK` (needs a
user decision before proceeding).

---

## Milestone 0.1.x — Foundation, quality gate, CI

### WP 0 — Repository foundation & buildable skeleton

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 0.0 | DONE | Repo hygiene files | `.gitignore` (Android/Gradle/Studio) and `.editorconfig` present; junk paths ignored. |
| 0.1 | DONE | GPLv3 LICENSE + SPDX convention | Full `LICENSE` (GPLv3) in root; SPDX header convention documented for source files. |
| 0.2 | DONE | Gradle build foundation | Gradle wrapper, `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`; latest **stable** Gradle/AGP/Kotlin (Q6). `./gradlew tasks` runs. |
| 0.3 | DONE | Version single source of truth | `version.properties` (`major=0,minor=1,patch=0`) read by Gradle into `versionName`/`versionCode`. |
| 0.4 | DONE | `:app` module scaffolding | `:app` `build.gradle.kts`, `AndroidManifest.xml` (minSdk 34), base theme + resources; package `it.marcelpetrick.catears`. |
| 0.5 | DONE | Compose placeholder screen | `MainActivity` renders a Compose placeholder ("Cat Ears — coming soon"). |
| 0.6 | DONE | Verify build + README | `./gradlew assembleDebug` produces an installable APK that launches; README build section matches real tasks. |

### WP 1 — Versioning automation

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 1.0 | DONE | Patch-bump git hook | A committed `scripts/` pre-commit hook increments `patch` in `version.properties` and re-stages it. |
| 1.1 | DONE | Hook installer | `installHooks` Gradle task (or documented script) installs the hook into `.git/hooks`. |
| 1.2 | DONE | Document versioning | README explains auto patch-bump and how to do manual minor/major bumps. |

### WP 2 — Code quality tooling

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 2.0 | DONE | Formatter (ktlint/spotless) | Format tooling wired into Gradle; `spotlessApply` formats; `spotlessCheck` passes on existing code. |
| 2.1 | DONE | Static analysis (detekt) | detekt with a committed config; `./gradlew detekt` passes clean. |
| 2.2 | DONE | Android Lint + aggregation | Lint configured; `spotlessCheck`/`detekt`/`lint` aggregated under `check`; README documents commands. |
| 2.3 | TODO | Upgrade SDK/Kotlin/JUnit when available | Remove suppressed lint rules from `lint.xml`: upgrade compileSdk/targetSdk to 37 (needs `platforms;android-37` via sdkmanager), Kotlin 2.3.21→2.4.0 + matching KSP, JUnit 5.11.4→6.x. Re-enable `OldTargetApi`, `GradleDependency`, `NewerVersionAvailable` in lint.xml after upgrade. |

### WP 3 — Testing & coverage harness

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 3.0 | DONE | Unit-test stack | JUnit5 + MockK + Turbine wired; one trivial passing test; `./gradlew test` green. |
| 3.1 | DONE | Coverage gate (Kover) | Kover configured with a **95%** verification rule and Q3 exclusions (UI/DI/generated). |
| 3.2 | DONE | Prove the gate | A real `domain` test exists; verified that dropping coverage fails `koverVerify`; README documents it. |

### WP 4 — Continuous Integration

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 4.0 | DONE | CI workflow | GitHub Actions workflow: build → format/detekt/lint → test → `koverVerify`; file is valid. |
| 4.1 | DONE | Local gate + docs | A local equivalent (`./gradlew check` aggregation or `scripts/ci.sh`) runs the same gate offline; README "CI / quality gate" section added. |

---

## Milestone 0.2.x — Live preview & camera switching

### WP 5 — Architecture skeleton & Hilt (Q1)

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 5.0 | DONE | Hilt wiring | Hilt deps + `@HiltAndroidApp` Application + minimal module; app still builds and launches. |
| 5.1 | DONE | Package skeleton | Create `ui/camera/facedetect/overlay/capture/share/domain/di` packages (plan §1.3) with placeholders. |
| 5.2 | DONE | Compose theme | Theme: colour, typography, shapes; placeholder screen uses it. |
| 5.3 | DONE | Home screen + ViewModel | `MainViewModel` exposes `StateFlow` UI state; home screen renders it; ViewModel state logic unit-tested. |

### WP 6 — Camera permission flow

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 6.0 | DONE | Permission state model | Camera-permission states modelled in `domain`; unit-tested without a device. |
| 6.1 | DONE | Permission UI | `CAMERA` in manifest; Compose flow for granted/denied/permanently-denied (with settings deep-link). |

### WP 7 — Live camera preview (CameraX)

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 7.0 | DONE | Camera controller seam | CameraX deps + a testable wrapper around the camera controller; non-UI logic unit-tested. |
| 7.1 | DONE | Preview composable | CameraX `Preview` use case bound to lifecycle, shown full-screen; clean start/stop. |
| 7.2 | DONE | Verify run + README | Preview shows live on emulator/device; README "Run" section verified. |

### WP 8 — Front/rear switching

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 8.0 | DONE | Lens-selector state | Lens selection modelled in `domain`; unit-tested. |
| 8.1 | DONE | Switch UI + rebind | UI toggle flips the live camera by rebinding CameraX use cases. |

---

## Milestone 0.3.x — Face-tracked cat-ear overlay

### WP 9 — On-device face detection (ML Kit)

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 9.0 | DONE | Analysis pipeline | ML Kit deps + CameraX `ImageAnalysis` use case feeding the detector. |
| 9.1 | DONE | Face model | Framework-free face model (box + key landmarks); single-face selection logic. |
| 9.2 | DONE | Coordinate transform | image→view transform incl. front-camera mirroring, in `domain`; thoroughly unit-tested across orientations. |
| 9.3 | DONE | Debug verification | A debug overlay/log confirms live face data updates in real time. |
| 9.4 | TODO | Wire ImageAnalysis into camera pipeline | Add `ImageAnalysis` use case to `CameraXControllerImpl`; bind `MlKitFaceDetectorImpl` as the analyser; route results through `PlacementSmoother` into `MainViewModel.onFaceDetected()`; ears track faces live on device. |

### WP 10 — Cat-ear overlay rendering

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 10.0 | DONE | Placeholder asset (Q4) | Generic, self-generated GPLv3-safe cat-ear asset committed; simple asset model. Replaceable later. |
| 10.1 | DONE | Placement math | Position/scale/rotation from face geometry, in `domain`; unit-tested. |
| 10.2 | DONE | Overlay layer | Compose `Canvas` overlay draws the ears over the preview using the placement math. |
| 10.3 | DONE | Jitter smoothing | Smoothing reduces overlay jitter; smoothing logic unit-tested. |

---

## Milestone 0.4.x — Capture, save, share

### WP 11 — Photo capture with overlay composited

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 11.0 | DONE | Capture use case + UI | Capture button + CameraX `ImageCapture` produces a still frame. |
| 11.1 | DONE | Composite overlay | Camera frame + overlay composited into one bitmap; testable parts factored and tested. |

### WP 12 — Save captured image

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 12.0 | DONE | Naming/metadata | Filename/metadata strategy in `domain`; unit-tested. |
| 12.1 | DONE | MediaStore save | Save to gallery via `MediaStore` (scoped storage); user feedback on success/failure. |

### WP 13 — Share captured image

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 13.0 | TODO | Share-intent builder | `FileProvider` configured; share `Intent` construction in `domain`; unit-tested. |
| 13.1 | TODO | Share UI | Share button opens the system share sheet delivering the saved image. |

---

## Milestone 0.5.x → 1.0.0 — Polish, release, docs

### WP 14 — Product polish

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 14.0 | TODO | Name + launcher icon | Display name `androidCatEars` (Q5); adaptive launcher icon added. |
| 14.1 | TODO | Theme refinement | Refined theme; correct light/dark handling. |
| 14.2 | TODO | States + a11y | Empty/error states; content descriptions for accessibility. |
| 14.3 | TODO | Version + commit stamp at startup | App displays current version (from `version.properties`, e.g. `0.1.15`) and the first 7 characters of the git commit hash on the main screen at startup. Useful during development to identify exactly what build is running. Version is read at build time via `BuildConfig`; commit hash is injected by Gradle at build time. Both are shown as a small non-intrusive label (e.g. bottom of screen or about overlay). |
| 14.4 | TODO | README screenshots | Screenshots of the working app added to README. |

### WP 15 — Release build & signing

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 15.0 | TODO | Release build type | R8/ProGuard rules + resource shrinking; release build compiles. |
| 15.1 | TODO | Signing config | Signing driven by `local.properties`/env; **no secrets committed**. |
| 15.2 | TODO | Deploy docs + verify | Release checklist + README "Deploy" section; `assembleRelease`/`bundleRelease` produce a signed artifact. |

### WP 16 — Documentation finalisation

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 16.0 | TODO | README pass | Overview/build/run/deploy all accurate against the shipped app. |
| 16.1 | TODO | Architecture notes | Architecture documentation under `documentation/`. |
| 16.2 | TODO | CHANGELOG + troubleshooting | CHANGELOG seeded from SemVer history; troubleshooting section added. |

---

## Optional / post-MVP (ask before starting)

### WP 17 — Desktop overlay-geometry simulator

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 17.0 | ASK | Desktop simulator | **Out of MVP scope.** Only if the user requests it: a JVM tool reusing the pure `domain` geometry to visualise overlay placement on sample landmark data. |

### Future backlog (not yet broken down)

Video recording · multi-face tracking · extra filters (dog ears, glasses, hats) · animated overlays ·
expression reactions · custom AI models (ONNX/TFLite) · overlay marketplace · social features.
Each becomes its own set of tasks when prioritised — see [`VISION.md`](VISION.md) "Future Ideas".
