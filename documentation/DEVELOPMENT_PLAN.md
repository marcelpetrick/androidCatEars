# Development Plan — Cat Ears Camera

This document describes how the application defined in [`VISION.md`](VISION.md) is built,
under the engineering rules defined in [`agents.md`](agents.md).

It has two parts:

1. **Foundations** — the principles, architecture, and tooling decisions that apply to all work.
2. **Work Packages** — an enumerated, incremental backlog. Each package is sized so that an agent
   can complete it in isolation and leave the repository **buildable, documented, and working**.

> **Execution happens from [`BACKLOG.md`](BACKLOG.md), not from this plan.** Each work package below
> is broken into atomic, commit-sized tasks there. This document is the strategic *what and why*;
> the backlog is the actionable *next concrete step*. See [`agents.md`](agents.md) "Work Format".

> **How agents must use this plan**
> Implement work packages strictly in order. Do not start a package until the previous one is
> done. If a package contains an **open question** or you hit an unlisted decision, **stop and ask
> the user**, presenting 2–3 concrete proposals with a recommendation — do not guess on
> architecture, dependencies, or product behaviour.

---

## Part 1 — Foundations

### 1.1 Guiding principles (SDLC)

- **Vertical, always-green increments.** Every work package ends with a successful
  `./gradlew build`, passing tests, clean linters, and updated documentation. The `main`/`master`
  branch is always releasable.
- **Atomic, conventional commits.** One logical change per commit (see [`agents.md`](agents.md)).
  Multi-step packages produce multiple commits, never one giant commit.
- **Documentation travels with code.** A feature is not "done" until the `README.md` and any
  relevant doc under `documentation/` reflect it.
- **Tests precede or accompany code.** No production logic lands without tests covering it.
  Coverage gate (95%) is enforced from the moment the testing harness exists.
- **Commit locally only.** Never push to the remote; the user decides when to push.

### 1.2 Definition of Done (applies to every work package)

A work package is complete only when **all** of the following hold:

1. **Buildable** — `./gradlew assembleDebug` (and `build`) succeeds from a clean checkout.
2. **Tested** — new logic has unit tests; the suite passes; coverage stays **≥ 95%** of the
   instrumented scope — see decision Q3: 95% on `domain`/logic, UI and generated code excluded).
3. **Linted** — formatter and static analysis report zero violations.
4. **Documented** — `README.md` / relevant docs updated; public APIs have KDoc where non-trivial.
5. **Versioned** — patch version incremented (single source of truth, see 1.4).
6. **Committed** — atomic conventional commit(s), signed off, no AI attribution, **not pushed**.

### 1.3 Target architecture

A single-module Android app (modularisation into `:core` / `:feature` modules is a documented
future option, not MVP — decision Q2), organised **package-by-feature** with a clean separation:

```
it.marcelpetrick.catears
├── ui/            Jetpack Compose screens, components, theme (no business logic)
├── camera/        CameraX setup, lifecycle binding, capture use cases
├── facedetect/    ML Kit wrapper, face-to-overlay geometry mapping
├── overlay/       Cat-ear rendering, asset model, placement math
├── capture/       Compose-overlay → bitmap compositing, local save (MediaStore)
├── share/         Android share-intent plumbing
├── domain/        Pure, framework-free models and logic (highly testable, drives coverage)
└── di/            Dependency-injection wiring
```

**Patterns:** MVVM with unidirectional data flow (UI state exposed via `StateFlow`, events flow
down, state flows up). The `domain` layer is pure Kotlin with no Android dependencies so the bulk
of logic — especially overlay placement geometry — is unit-testable on the JVM without a device.

### 1.4 Versioning (single source of truth)

- A single `version.properties` file in the repo root holds `major`, `minor`, `patch`.
- `build.gradle.kts` reads it to derive `versionName` (`major.minor.patch`) and `versionCode`.
- **Every commit bumps `patch` by 1.** This is automated by a git hook (Work Package 1) so it is
  consistent and never forgotten; major/minor bumps are manual and deliberate.

### 1.5 Tooling decisions (with rationale)

| Concern            | Choice (proposed)             | Rationale |
|--------------------|-------------------------------|-----------|
| Language           | Kotlin                        | Per VISION |
| UI                 | Jetpack Compose               | Per VISION |
| Camera             | CameraX                       | Per VISION |
| Face detection     | ML Kit Face Detection (on-device) | Per VISION |
| Build              | Gradle, Kotlin DSL, version catalog (`libs.versions.toml`) | Modern default; one place for dependency versions |
| Formatting         | ktlint (via spotless or ktlint-gradle) | De-facto Kotlin style enforcement |
| Static analysis    | detekt + Android Lint         | Catches smells and Android-specific issues |
| Unit testing       | JUnit5 + kotlin.test + Turbine (Flows) + MockK | Standard, Kotlin-friendly |
| Coverage           | Kover                         | Kotlin-native, Compose-aware, simpler than JaCoCo |
| Dependency injection | Hilt (decided — Q1)         | Standard Android DI; testability |
| CI                 | GitHub Actions workflow + a local `./gradlew check`-equivalent | Mirrors local gate; runnable offline |

> These are **proposals**. Where flagged with a Q-number, confirm with the user before adopting.

### 1.6 Resolved decisions

All initial open questions have been resolved by the user. These are now **binding**; agents do not
re-litigate them, but must still raise *new* decisions not covered here.

- **Q1 — Dependency injection: Hilt.** Use Google's Hilt from the start. Rationale: it is the
  industry-standard, officially recommended DI for Android and directly serves the project's
  "learn modern Android" goal. Define collaborators behind interfaces so they remain swappable with
  fakes in unit tests.
- **Q2 — Module structure: single module.** Build the MVP as a single `:app` module organised
  package-by-feature (see 1.3). Splitting into `:core`/`:feature` modules remains a documented
  future option, not MVP work.
- **Q3 — Coverage scope: domain/logic, exclude UI & generated code.** The 95% gate is enforced on
  the `domain` layer and other non-UI logic. Compose UI, DI wiring, and generated code are
  **excluded** via Kover rules rather than chasing 95% across the whole APK.
- **Q4 — Artwork: generic, self-generated, replaceable.** Start with simple generic cat-ear
  artwork — self-authored (or generated) so it is unambiguously GPLv3-compatible — committed to the
  repo. It is explicitly a **placeholder** that can be replaced later without affecting placement
  logic (logic depends on geometry, not on the specific asset).
- **Q5 — App display name: `androidCatEars`** for now. This is a working name to be changed/adapted
  later; do not block release polish on a final name.
- **Q7 — Version + commit stamp display:** The app must show the current version string (e.g.
  `0.1.15`) and the first 7 characters of the git commit hash at startup, as a small non-intrusive
  label. This identifies exactly which build is running — useful both during development and for
  users reporting issues. Implementation: Gradle injects both values into `BuildConfig` at build
  time; the UI reads them from `BuildConfig.VERSION_NAME` and a custom `BuildConfig.GIT_COMMIT`
  field. See backlog task 14.3.
- **Q6 — SDK levels: most recent stable.** minSdk 34 (Android 14, per VISION). compileSdk and
  targetSdk track the **latest stable** at implementation time. Goal: a stable but up-to-date
  codebase — prefer current stable releases of Gradle, AGP, Kotlin, Compose, and libraries (no
  alpha/beta unless unavoidable and justified).

---

## Part 2 — Work Packages

Each package lists its **Goal**, **Deliverables**, and **Done when** criteria (in addition to the
global Definition of Done in 1.2). Packages are intentionally small and ordered so the app is
demonstrable as early as possible and never broken between packages.

> Milestone mapping to SemVer minor versions is given at the end (Section 2.x "Milestones").

### WP 0 — Repository foundation & buildable skeleton
- **Goal:** A minimal Android app that compiles, installs, and shows a Compose screen.
- **Deliverables:**
  - `.gitignore` (Android/Gradle/Android Studio), `.editorconfig`.
  - `LICENSE` containing the full GPLv3 text; SPDX headers convention documented.
  - Gradle wrapper, `settings.gradle.kts`, root + `:app` `build.gradle.kts`, `libs.versions.toml`.
  - `version.properties` (`major=0, minor=1, patch=0`) wired into `versionName`/`versionCode`.
  - `:app` module with `it.marcelpetrick.catears`, a single `MainActivity` rendering a Compose
    placeholder ("Cat Ears — coming soon").
  - `AndroidManifest.xml`, minSdk 34, themes, basic resources.
  - `README.md` build section verified against the real Gradle tasks.
- **Done when:** `./gradlew assembleDebug` produces an installable APK; app launches to the placeholder.
- **Applies decision Q6:** minSdk 34; compileSdk/targetSdk = latest stable; current stable Gradle/AGP/Kotlin/Compose.

### WP 1 — Versioning automation
- **Goal:** Patch version auto-increments on every commit, from the single source of truth.
- **Deliverables:**
  - A `pre-commit` (or `prepare-commit-msg`) git hook that increments `patch` in
    `version.properties` and re-stages it.
  - A committed hook script under `scripts/` plus an `install-hooks` Gradle task (hooks aren't
    versioned by git themselves, so installation is explicit and documented).
  - README note on how versioning works and how to do manual minor/major bumps.
- **Done when:** Making a commit visibly bumps `patch`; build picks up the new version.

### WP 2 — Code quality tooling (format + static analysis)
- **Goal:** Automated formatting and linting wired into Gradle.
- **Deliverables:** ktlint (spotless), detekt with a committed config, Android Lint config;
  Gradle tasks (`spotlessCheck`, `detekt`, `lint`) aggregated under `check`.
- **Done when:** `./gradlew spotlessCheck detekt lint` passes clean on the existing code; README
  documents the commands.

### WP 3 — Testing & coverage harness
- **Goal:** Unit-test infrastructure with an enforced 95% coverage gate.
- **Deliverables:** JUnit5 + MockK + Turbine set up; Kover configured with a **95% verification
  rule** and exclusions per **Q3** (95% on `domain`/logic; exclude UI/DI/generated); at least one
  real `domain` test proving the pipeline.
  - **Applies decisions Q1 (Hilt) and Q3 (coverage scope)** — both shape testability.
- **Done when:** `./gradlew test koverVerify` passes; lowering coverage fails the build (verified).

### WP 4 — Continuous Integration pipeline
- **Goal:** One command / one workflow that runs the full gate, mirrored locally and in CI.
- **Deliverables:**
  - A GitHub Actions workflow running: build → spotless/detekt/lint → test → `koverVerify`.
  - A local equivalent (documented `./gradlew check` aggregation or a `scripts/ci.sh`) so the same
    gate runs offline before committing.
  - README "CI / quality gate" section.
- **Done when:** The aggregated gate runs green locally; the workflow file is valid. (Per user
  policy, nothing is pushed — the workflow is committed but not triggered remotely by us.)

### WP 5 — App architecture skeleton & DI
- **Goal:** The package structure, theme, navigation entry point, and DI wiring in place — still no
  camera, but the scaffolding all real features plug into.
- **Deliverables:** Compose theme (colour, typography), app entry/navigation, an empty
  `MainViewModel` exposing `StateFlow` UI state, Hilt wiring (per Q1), package skeleton from 1.3.
- **Done when:** App builds and shows the themed home screen driven by a ViewModel; tests cover the
  ViewModel state logic.

### WP 6 — Runtime camera permission flow
- **Goal:** Request and handle the `CAMERA` permission gracefully in Compose.
- **Deliverables:** Permission request UI (granted / denied / permanently-denied with
  settings deep-link), manifest permission, state modelled in `domain` and unit-tested.
- **Done when:** On first launch the app asks for camera permission and reflects all states; logic
  is tested without a device.

### WP 7 — Live camera preview (CameraX)
- **Goal:** Real-time preview rendered in Compose.
- **Deliverables:** CameraX `Preview` use case bound to the lifecycle, shown full-screen; clean
  start/stop with lifecycle; testable wrapper around the camera controller.
- **Done when:** Launching the app (with permission) shows a live preview; the non-UI control logic
  is unit-tested; README "Run" section verified on emulator/device.

### WP 8 — Front/rear camera switching
- **Goal:** Toggle between front and rear lenses.
- **Deliverables:** A switch control in the UI; lens-selector state in `domain`; rebinding the
  CameraX use cases on toggle.
- **Done when:** Toggling flips the camera live; selector logic is unit-tested.

### WP 9 — On-device face detection (ML Kit)
- **Goal:** Detect a single face from the camera stream and expose its geometry.
- **Deliverables:** CameraX `ImageAnalysis` use case feeding ML Kit Face Detection; a wrapper
  emitting a framework-free face model (bounding box + key landmarks) for the chosen single face;
  coordinate transform (image → view space, mirrored for front camera) implemented in `domain` and
  thoroughly unit-tested.
- **Done when:** Face data updates in real time (verifiable via a debug overlay/log); the geometry
  math is unit-tested across orientations and front/back mirroring.

### WP 10 — Cat-ear overlay rendering
- **Goal:** Draw 2D cat ears correctly placed above the detected face, in real time.
- **Deliverables:** Cat-ear assets (per **Q4** — generic, self-generated, GPLv3-safe placeholder
  that can be swapped later), a Compose `Canvas`/overlay layer, placement math (position/scale/
  rotation from face geometry) in `domain` with unit tests, smoothing to reduce jitter.
- **Done when:** Ears track the face live and stay aligned when moving/switching cameras; placement
  math is unit-tested.

### WP 11 — Photo capture with overlay composited
- **Goal:** Capture a still image with the cat ears baked into the pixels.
- **Deliverables:** Capture trigger UI; compose the camera frame + overlay into a single bitmap;
  the compositing logic factored so its non-rendering parts are testable.
- **Done when:** Pressing capture yields an in-memory image showing the overlay; logic tested where
  feasible.

### WP 12 — Save captured image locally
- **Goal:** Persist the captured image to device storage.
- **Deliverables:** Save via `MediaStore` (scoped storage, no broad permissions) to the gallery;
  filename/metadata strategy in `domain`, unit-tested; user feedback on success/failure.
- **Done when:** Captured photos appear in the gallery; naming/path logic is unit-tested.

### WP 13 — Share captured image
- **Goal:** Share the saved image through Android's share sheet.
- **Deliverables:** `FileProvider` configured; share `Intent` built for the saved URI; share button
  in the UI; intent-construction logic unit-tested.
- **Done when:** Sharing opens the system sheet and the image is delivered to messaging/email/social
  targets.

### WP 14 — Product polish (name, icon, theming, UX)
- **Goal:** Make it feel like a finished app.
- **Deliverables:** App display name `androidCatEars` (per **Q5** — working name, changeable later),
  adaptive launcher icon, refined Compose theme, empty/error states, basic accessibility (content
  descriptions), light/dark handling.
- **Done when:** App presents a coherent, polished experience; screenshots added to README.

### WP 15 — Release build & signing
- **Goal:** Produce a shippable, signed release artifact.
- **Deliverables:** Release build type with R8/ProGuard rules, resource shrinking; signing config
  driven by `local.properties`/env (no secrets committed); a release checklist; README "Deploy"
  section with the real signing + `assembleRelease`/`bundleRelease` steps.
- **Done when:** `./gradlew assembleRelease` (and `bundleRelease`) yields a signed APK/AAB; deploy
  docs verified.

### WP 16 — Documentation finalisation
- **Goal:** Ensure every doc reflects the shipped app.
- **Deliverables:** README pass (overview, build, run, deploy), architecture notes under
  `documentation/`, CHANGELOG seeded from the SemVer history, troubleshooting section.
- **Done when:** A new contributor can clone, build, run, and deploy using only the docs.

### WP 17 (optional / post-MVP) — Desktop overlay-geometry simulator
- **Goal:** A lightweight Linux utility to iterate on face-landmark → overlay placement without
  deploying to Android. **Out of MVP scope** (per VISION); implement only if requested.
- **Deliverables:** A small JVM tool reusing the pure `domain` geometry to render placement against
  sample landmark data.
- **Done when:** Placement math can be visualised and tweaked on the desktop. **Ask before starting.**

### Future backlog (not planned here — see VISION "Future Ideas")
Video recording, multi-face tracking, additional filters (dog ears, glasses, hats), animated
overlays, expression reactions, custom AI models (ONNX/TFLite), overlay marketplace, social
features. Each becomes its own plan when prioritised.

---

## Milestones (SemVer minor versions)

| Version target | Delivered by | Demonstrable capability |
|----------------|--------------|--------------------------|
| `0.1.x` | WP 0–4   | Buildable skeleton with full quality gate and CI |
| `0.2.x` | WP 5–8   | Live preview with front/rear switching |
| `0.3.x` | WP 9–10  | Real-time face-tracked cat-ear overlay |
| `0.4.x` | WP 11–13 | Capture, save, and share with overlay |
| `0.5.x` | WP 14–16 | Polished, documented, signed release |
| `1.0.0` | —        | First public release after sign-off |

Patch versions increment per commit within each milestone (see 1.4).
