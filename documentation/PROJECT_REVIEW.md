<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Project Review — androidCatEars

A software-development-lifecycle review of the current documented project state:
what exists, how healthy it is, and what is worth adding next. Companion to
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
| Landmark-aware ear anchoring and perspective X-squash | ✅ |
| Animated procedural ears with fur, wobble, and capture compositor parity | ✅ |
| Photo capture with overlay baked in (WYSIWYG) | ✅ |
| Save to gallery (MediaStore, scoped storage) | ✅ |
| Share via system share sheet | ✅ |
| Camera-permission flow (grant / deny / permanent-deny) | ✅ |
| Branded adaptive icon + Material 3 theme (light/dark) | ✅ |
| Startup version + git-commit build stamp | ✅ |
| Accessibility (content descriptions, live-region status) | ✅ partial |
| Pure `:domain` module with golden overlay fixtures | ✅ |

**Verified-on-hardware items still open:** live tracking accuracy on a physical
device, overlay constant tuning against real face samples, and README screenshots
(backlog 14.4). A webcam-backed emulator is useful for local workflow checks,
but it does not replace final device validation.

---

## 2. Engineering lifecycle — what exists

| Area | Tooling | State |
|------|---------|-------|
| Build | Gradle 9.5.1 (Kotlin DSL), AGP 9.2.1, version catalog, config cache | ✅ solid |
| Versioning | `version.properties` SSOT, auto patch-bump hook, SemVer | ✅ |
| Formatting | Spotless 8.6.0 (ktlint) | ✅ |
| Static analysis | detekt 1.23.8 + Android Lint (warnings-as-errors) | ✅ |
| Unit testing | JUnit5 + MockK + Turbine + coroutines-test across app logic and `:domain` | ✅ strong on domain |
| Coverage | Kover, **95% gate** on pure logic | ✅ |
| CI | GitHub Actions: build→spotless→detekt→lint→test→kover on push/PR | ✅ |
| Local gate | `scripts/ci.sh` mirrors CI and prints a timed ASCII summary | ✅ |
| Release | R8 + shrink, signing via `keystore.properties`/env, manual signed-AAB release workflow | ✅ |
| Supply chain | Dependabot, CodeQL, Dependency Review, Gitleaks | ✅ |
| Docs | vision, plan, backlog, architecture, release, troubleshooting, emulator, deploy, review logs | ✅ |

**This is an unusually disciplined foundation for a hobby-scale app.** The
quality gate, automated versioning, and documentation are production-grade.

---

## 3. The testing pyramid — the main gap

The project has a strong JVM base, especially after extracting `:domain`, but
automated Android/UI coverage is still thin:

```
        /\        E2E / UI flows ......... ❌ TODO
       /  \       Instrumented (device) .. ❌ TODO
      /----\      Screenshot / snapshot ... ❌ TODO
     /      \     Integration (Android) ... ⚠️ BLOCKED (Robolectric/JDK 26)
    /--------\    Unit (pure JVM) ......... ✅ 95% gate on pure logic
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
- Current blocker: Robolectric 4.14.1 uses ASM 9.7 and cannot instrument classes
  compiled by JDK 26. Unblock by using a JDK 21 Gradle test toolchain or by
  waiting for a Robolectric/ASM release that supports Java 26 class files.

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
  For local manual work, a webcam-backed emulator can still exercise the live
  preview and face pipeline.

**E. Mutation testing (test-quality check)**
- **PITest** (`pitest`/`gradle-pitest-plugin`) mutates the `domain` code and
  checks the 70 tests actually catch the mutations — validates that 95% line
  coverage is *meaningful*, not just executed.

---

## 4. Static analysis & supply chain

Implemented:

- **Dependabot** — weekly Gradle and GitHub Actions update PRs.
- **GitHub CodeQL** — SAST for security and quality queries.
- **GitHub Dependency Review** — blocks vulnerable dependency additions in PRs.
- **Gitleaks** — secret scanning in CI.

Worth adding later:

- **detekt type resolution** (`detektMain` with classpath) + the
  **`detekt-formatting`** ruleset — deeper rules than the current config.
- **`com.autonomousapps.dependency-analysis`** — finds unused/misplaced deps.
- **Branch protection** — require CI + security workflows before merge to `main`
  in GitHub repository settings. This is not codeable from the repo itself.
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

## 6. Suggested next backlog

1. Add README screenshots and Play listing assets (14.4, 19.0).
2. Capture or annotate real face samples, then tune overlay constants (17.2–17.4).
3. Add screenshot tests for `MainScreen`, overlay states, and light/dark theme (18.1).
4. Add `androidTest` happy-path coverage with UI Automator for permissions (18.2).
5. Add emulator E2E in CI with Gradle Managed Devices or an emulator-runner job (18.3).
6. Configure branch protection in GitHub repository settings.
7. Prepare Play Store compliance and first signed AAB upload (19.1–19.2).
8. Upgrade compile/target SDK, Kotlin, and JUnit when SDK 37/tooling are available (2.3).
9. Add Robolectric host-side Android tests once the JDK 26/ASM blocker is resolved (18.0).

None of these block the MVP; they harden it for real distribution.
