<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Project Review — androidCatEars

A software-development-lifecycle review of the project as of version `0.1.39`:
what exists, how healthy it is, and what is worth adding. Companion to
[`ARCHITECTURE.md`](ARCHITECTURE.md) (structure) and [`PLAY_STORE.md`](PLAY_STORE.md)
(publishing).

---

## 1. What was built — feature completeness

The MVP from [`VISION.md`](VISION.md) is **functionally complete**:

| Capability | Status |
|------------|--------|
| Live camera preview (CameraX) | ✅ |
| Front/rear lens switching | ✅ |
| On-device face detection (ML Kit) | ✅ |
| Real-time face-tracked cat-ear overlay (with jitter smoothing) | ✅ |
| Photo capture with overlay baked in (WYSIWYG) | ✅ |
| Save to gallery (MediaStore, scoped storage) | ✅ |
| Share via system share sheet | ✅ |
| Camera-permission flow (grant / deny / permanent-deny) | ✅ |
| Branded adaptive icon + Material 3 theme (light/dark) | ✅ |
| Startup version + git-commit build stamp | ✅ |
| Accessibility (content descriptions, live-region status) | ✅ partial |

**Verified-on-hardware items still open:** live tracking accuracy, overlay
constant tuning, README screenshots (backlog 14.4) — all need a physical device.

---

## 2. Engineering lifecycle — what exists

| Area | Tooling | State |
|------|---------|-------|
| Build | Gradle 9.5.1 (Kotlin DSL), AGP 9.2.1, version catalog, config cache | ✅ solid |
| Versioning | `version.properties` SSOT, auto patch-bump hook, SemVer | ✅ |
| Formatting | Spotless 8.6.0 (ktlint) | ✅ |
| Static analysis | detekt 1.23.8 + Android Lint (warnings-as-errors) | ✅ |
| Unit testing | JUnit5 + MockK + Turbine + coroutines-test — **11 files, 70 tests** | ✅ strong on domain |
| Coverage | Kover, **95% gate** on pure logic | ✅ |
| CI | GitHub Actions: build→spotless→detekt→lint→test→kover on push/PR | ✅ |
| Local gate | `scripts/ci.sh` mirrors CI | ✅ |
| Release | R8 + shrink, signing via `keystore.properties`/env, manual release workflow (APK+AAB) | ✅ |
| Docs | vision, plan, backlog, architecture, release, troubleshooting, emulator, deploy, changelog | ✅ |

**This is an unusually disciplined foundation for a hobby-scale app.** The
quality gate, automated versioning, and documentation are production-grade.

---

## 3. The testing pyramid — the main gap

Today the pyramid is **all base, no middle or top**:

```
        /\        E2E / UI flows ......... ❌ none
       /  \       Instrumented (device) .. ❌ none
      /----\      Screenshot / snapshot ... ❌ none
     /      \     Integration (Android) ... ❌ none (Robolectric absent)
    /--------\    Unit (pure JVM) ......... ✅ 70 tests, 95% on domain
```

By design, everything that touches the Android framework — CameraX, ML Kit, the
compositor, MediaStore save, share intent, **and the entire Compose UI** — is
*excluded* from coverage and exercised by **no automated test at all**. The pure
geometry/state layer is excellent; the glue and the UI are only ever verified by
hand. That is the single biggest quality risk.

### Recommended additions (priority order)

**A. Host-side Android tests with Robolectric** *(fastest ROI, runs in CI without an emulator)*
- Add `org.robolectric:robolectric` + `androidx.test:core`. Lets you unit-test
  `MainActivity` wiring, the permission launcher, and Compose screens on the JVM.
- Pairs with `androidx.compose.ui:ui-test-junit4` + `createComposeRule` to assert
  that the capture/share FABs appear in the right states (the logic currently
  only checked by reading `MainScreen.kt`).

**B. Screenshot / snapshot testing — strong fit for a visual app**
- **Paparazzi** (JVM, no device) renders Composables to PNGs and diffs them.
  Ideal for `MainScreen` states, the cat-ear overlay placement, and light/dark
  theme regressions. Or **Roborazzi** (Robolectric-based) if you prefer.
- Gives the overlay/theme work a regression net it currently lacks.

**C. Instrumented UI tests (`androidTest` source set)**
- Create `app/src/androidTest/...` with Compose UI Test for the happy path, and
  **UI Automator** to handle the system camera-permission dialog (Espresso/Compose
  can't tap OS dialogs).
- **Espresso** is the classic View-based driver; here the UI is Compose, so
  Compose UI Test is the primary tool, with Espresso only if any interop View
  (e.g. `PreviewView`) needs direct assertions.

**D. End-to-end on an emulator (in CI)**
- **Gradle Managed Devices (GMD):** declare an emulator in `build.gradle.kts`;
  `./gradlew <device>DebugAndroidTest` spins it up hermetically — reproducible
  locally and in CI.
- **CI emulator:** `reactivecircle/android-emulator-runner` (or
  `ReactiveCircus/android-emulator-runner`) runs `connectedCheck` on a headless
  AVD in GitHub Actions.
- **Firebase Test Lab:** run the instrumented suite on a matrix of real devices;
  also gives a free **Pre-launch report** when you upload to Play.
- The camera/face path can't see a real face on an emulator — feed a **virtual
  scene / injected image** or assert on UI state rather than detection output.

**E. Mutation testing (test-quality check)**
- **PITest** (`pitest`/`gradle-pitest-plugin`) mutates the `domain` code and
  checks the 70 tests actually catch the mutations — validates that 95% line
  coverage is *meaningful*, not just executed.

---

## 4. Static analysis & supply chain — what to add

- **Dependabot / Renovate** — automated dependency-update PRs; directly unblocks
  backlog **2.3** (SDK 37 / Kotlin 2.4 / JUnit 6) by surfacing releases.
- **GitHub CodeQL** — free SAST for the repo (security/quality queries).
- **OWASP `dependency-check`** (or GitHub Dependency Review) — flags vulnerable
  transitive libraries.
- **detekt type resolution** (`detektMain` with classpath) + the
  **`detekt-formatting`** ruleset — deeper rules than the current config.
- **`com.autonomousapps.dependency-analysis`** — finds unused/misplaced deps.
- **Branch protection** — require the CI check + ≥1 review before merge to `main`.
- **Codecov/Coveralls upload** — trend coverage over time (Kover XML already
  produced).

---

## 5. Performance & runtime health (post-MVP)

- **Baseline Profiles** + **Macrobenchmark** — measure and improve cold-start and
  scroll/jank; ships a profile in the release for faster startup.
- **StrictMode** in debug — catch disk/network on the main thread.
- **LeakCanary** (debugImplementation) — detect Activity/Bitmap leaks, relevant
  given the camera + bitmap compositing.

---

## 6. Suggested next backlog (see BACKLOG.md “WP 18 / WP 19”)

1. Robolectric + Compose UI Test harness (A) — biggest coverage gap.
2. Paparazzi screenshot tests for overlay + theme (B).
3. `androidTest` happy-path + UI Automator permission handling (C).
4. GMD + CI emulator job running `connectedCheck` (D).
5. Dependabot + CodeQL + branch protection (§4).
6. Play Store publishing prep (see [`PLAY_STORE.md`](PLAY_STORE.md)).

None of these block the MVP; they harden it for real distribution.
