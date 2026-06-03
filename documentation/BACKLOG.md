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
| 9.4 | DONE | Wire ImageAnalysis into camera pipeline | Add `ImageAnalysis` use case to `CameraXControllerImpl`; bind `MlKitFaceDetectorImpl` as the analyser; route results through `PlacementSmoother` into `MainViewModel.onFaceDetected()`; ears track faces live on device. |

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
| 11.0 | DONE | Capture use case + UI | Capture button requests a WYSIWYG `PreviewView` frame for compositing. |
| 11.1 | DONE | Composite overlay | Camera frame + overlay composited into one bitmap; testable parts factored and tested. |
| 11.2 | DONE | Capture → composite → save → share integration | Capture button grabs the view-space preview frame, composites the ear overlay (WYSIWYG, coords already in view space), saves via `ImageSaver`, and enables the Share FAB with the saved URI. |

### WP 12 — Save captured image

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 12.0 | DONE | Naming/metadata | Filename/metadata strategy in `domain`; unit-tested. |
| 12.1 | DONE | MediaStore save | Save to gallery via `MediaStore` (scoped storage); user feedback on success/failure. |

### WP 13 — Share captured image

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 13.0 | DONE | Share-intent builder | `FileProvider` configured; share `Intent` construction in `domain`; unit-tested. |
| 13.1 | DONE | Share UI | Share button opens the system share sheet delivering the saved image. |

---

## Milestone 0.5.x → 1.0.0 — Polish, release, docs

### WP 14 — Product polish

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 14.0 | DONE | Name + launcher icon | Display name `androidCatEars` (Q5); adaptive launcher icon added. |
| 14.1 | DONE | Theme refinement | Refined theme; correct light/dark handling. |
| 14.2 | DONE | States + a11y | Empty/error states; content descriptions for accessibility. |
| 14.3 | DONE | Version + commit stamp at startup | App displays current version (from `version.properties`, e.g. `0.1.15`) and the first 7 characters of the git commit hash on the main screen at startup. Useful during development to identify exactly what build is running. Version is read at build time via `BuildConfig`; commit hash is injected by Gradle at build time. Both are shown as a small non-intrusive label (e.g. bottom of screen or about overlay). |
| 14.4 | TODO | README screenshots | Screenshots of the working app added to README. |

### WP 15 — Release build & signing

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 15.0 | DONE | Release build type | R8/ProGuard rules + resource shrinking; release build compiles. |
| 15.1 | DONE | Signing config | Signing driven by `keystore.properties`/env; **no secrets committed**. |
| 15.2 | DONE | Deploy docs + verify | Release checklist + README "Deploy" section; `assembleRelease`/`bundleRelease` produce a signed artifact. |

### WP 16 — Documentation finalisation

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 16.0 | DONE | README pass | Overview/build/run/deploy all accurate against the shipped app. |
| 16.1 | DONE | Architecture notes | Architecture documentation under `documentation/`. |
| 16.2 | DONE | CHANGELOG + troubleshooting | CHANGELOG seeded from SemVer history; troubleshooting section added. |

---

## Post-MVP — hardening & distribution

### WP 17 — Overlay lab: desktop tuning of ear placement (requested)

Design and rationale in [`OVERLAY_LAB.md`](OVERLAY_LAB.md). Resolves TODO #3
(tune placement constants against real faces) on the desktop.

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 17.0 | DONE | Extract `:domain` module | Pure domain package moved to `domain/` JVM library module; `:app` depends on it via `project(":domain")`; package names unchanged; all CI gates green. |
| 17.1 | DONE | Golden-fixture placement tests | `ComputeOverlayPlacementFixtureTest`: 8 `@ParameterizedTest` cases (frontal/roll/yaw/extreme-yaw/landmarks/tilt/partial-landmarks) via `@MethodSource`; each asserts all 5 EarAnchor fields within 0.01 tolerance. |
| 17.2 | BLOCKED | Sample image set + annotations | Requires physical device capture or manual photo annotation — cannot be done without access to real face images. |
| 17.3 | BLOCKED | Desktop visualiser module | Depends on 17.2 sample images; also requires Compose Desktop runtime not yet in the project. |
| 17.4 | BLOCKED | Tune & promote constants | Depends on 17.2 and 17.3. |

### WP 18 — Test depth & supply chain

Rationale in [`PROJECT_REVIEW.md`](PROJECT_REVIEW.md) §3–4.

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 18.0 | BLOCKED | Host-side Android tests | Robolectric 4.14.1 (ASM 9.7) cannot instrument classes compiled by JDK 26 (class version 70). Unblocks when: Robolectric ships with ASM 9.9+ supporting Java 26, OR the dev machine installs JDK 21 and the test toolchain is configured to use it via Gradle toolchains. |
| 18.1 | TODO | Screenshot tests | Paparazzi (or Roborazzi) snapshots for `MainScreen` states, overlay, and light/dark theme. |
| 18.2 | TODO | Instrumented happy path | `androidTest` source set: Compose UI Test happy path + UI Automator for the permission dialog. |
| 18.3 | TODO | Emulator E2E in CI | Gradle Managed Devices + a CI job running `connectedCheck` on a headless AVD. |
| 18.4 | DONE | Supply chain | `.github/dependabot.yml` added: weekly Gradle + GitHub Actions PRs. CodeQL (`codeql.yml`), dependency review (`dependency-review.yml`), and Gitleaks (`secret-scan.yml`) workflows already present. Branch protection rules must be set in the GitHub repository settings (not codeable). |
| 18.5 | DONE | CycloneDX SBOM automation | `org.cyclonedx.bom` generates aggregate JSON/XML SBOMs via `./gradlew cyclonedxBom`; `scripts/generate-sbom.sh` creates versioned release-style SBOM files and SHA-256 checksums; `scripts/ci.sh` includes SBOM generation in the local ASCII summary; `ci.yml` uploads SBOM artifacts; `release.yml` attaches CycloneDX SBOMs and checksums to GitHub Releases. |

### WP 19 — Google Play release

Full guide in [`PLAY_STORE.md`](PLAY_STORE.md).

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 19.0 | TODO | Listing assets | 512×512 icon, 1024×500 feature graphic, ≥2 screenshots (needs 14.4), short/full descriptions. |
| 19.1 | TODO | Compliance forms | Privacy policy URL (camera + photo storage), data safety, content rating, target audience. |
| 19.2 | TODO | Play App Signing + first upload | Register upload key, upload signed AAB to internal track; pass pre-launch report. |
| 19.3 | TODO | (optional) Automated publishing | Gradle Play Publisher or fastlane supply via a CI service account. |

### WP 20 — Animated, 3D-look cat ears with proper head anchoring

Full design rationale and rendering approach in [`ANIMATED_EARS.md`](ANIMATED_EARS.md).
Two distinct goals: fix the flying-ears anchoring bug, and replace the static sprite with
a procedural, animated, depth-illusory ear renderer — all within the existing Compose
layer, no new runtime dependencies.

**Prerequisite order**: 20.0 → 20.1 → 20.2 → 20.3 → 20.4 → 20.5 → 20.6 → 20.7
Each task keeps all quality gates green (build + detekt + lint + tests ≥ 95% + koverVerify).

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 20.0 | DONE | Expose richer ML Kit landmarks | Added `leftEarPosition`, `rightEarPosition`, `headEulerAngleY` to `FaceModel`. `MlKitFaceDetectorImpl` extracts `FaceLandmark.LEFT_EAR`, `FaceLandmark.RIGHT_EAR` (TIP landmarks don't exist in ML Kit API), and `headEulerAngleY`. FaceModel unit tests updated; all gates green. |
| 20.1 | DONE | Anchor ears to human ear-tip landmarks | Rewrote `computeOverlayPlacement` to place each cat ear above the corresponding `LEFT_EAR`/`RIGHT_EAR` ML Kit landmark. Bounding box still used for size scaling. Fallback to bounding-box estimate when landmarks absent. Ears no longer fly above the face. |
| 20.2 | DONE | Two-anchor `OverlayPlacement` model | `OverlayPlacement(leftEar: EarAnchor, rightEar: EarAnchor, headEulerAngleY: Float)` with `EarAnchor(x, y, size, tiltDegrees, xScale)`. `PlacementSmoother` smooths each `EarAnchor` field independently. All placement/smoother tests updated and green. |
| 20.3 | DONE | Compose Canvas ear shape renderer | `CatEarOverlay` draws two independent ears as triangle paths: outer brown (`#8B5E3C`) + inner pink (`#E8A0A0`) accent shape. Static PNG asset (`ic_cat_ears.xml`) deleted. `OverlayCompositor` also rewritten to draw procedural ears on `android.graphics.Canvas` for the capture path. |
| 20.4 | DONE | Per-ear perspective X-squash | `computeOverlayPlacement` computes `xScale` from `headEulerAngleY`: near ear widens (>1), far ear narrows (<1), clamped to [0.4, 1.6]. `CatEarOverlay` applies `scale(scaleX = anchor.xScale)` per ear. xScale computation covered by unit tests. |
| 20.5 | DONE | Animated fur strands | `CatEarOverlay` drives two `InfiniteTransition`s: sway at 1.25 Hz and twitch at 0.2 Hz. Five fur strands per ear with randomised phases, sine-wave tip sway (`sin(t + phase) × swayAmplitude`) and cosine bobble. Periodic ear-tip twitch via `sin(twitchTime) * sin(twitchTime * 3) * 4°`. |
| 20.6 | DONE | Pose-reactive ear tilt with spring | `headEulerAngleZ` mapped to per-ear tilt: left at `0.6 × roll`, right at `1.0 × roll`. Both driven through `animateFloatAsState(spring(StiffnessMedium))` for elastic overshoot on rapid head snaps. |
| 20.7 | DONE | Capture path: procedural ears composited | `OverlayCompositor.composite(frame, OverlayPlacement?)` draws both ears procedurally on `android.graphics.Canvas` using the same geometry constants. `by lazy {}` Paint initialisation avoids Android class-load in JVM tests. `CameraPreviewComposable.captureComposited()` no longer decodes a PNG asset. |

### WP 21 — Feline-quality ear styles with live style switcher

Full design rationale, visual references, and style definitions in
[`EAR_STYLES.md`](EAR_STYLES.md). Ten concept mockups in
[`../ear_design_ideas/`](../ear_design_ideas/).

**Goal 1**: Make the default ears look like real cat ears (feline silhouette,
gradient fill, fur fringe, rounded tip) rather than plain triangles.
**Goal 2**: Let the user cycle through 5 distinct ear styles (cat variants +
dog + other) via a new button in the camera UI.

**Prerequisite order**: 21.0 → 21.1 → 21.2 → 21.3 → 21.4 → 21.5 → 21.6
Each task keeps all quality gates green.

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 21.0 | DONE | `EarStyle` enum in `:domain` | Add `enum class EarStyle { CLASSIC, SHARP_FELINE, LYNX_TUFTED, CANINE_FLOPPY, CANINE_PERKY }` to `:domain`. Add `earStyle: EarStyle = EarStyle.CLASSIC` to `OverlayPlacement` (default-valued, no breaking change). Unit-test enum values and default; all gates green. |
| 21.1 | DONE | Style-dispatch in `CatEarOverlay` | Refactor `drawEar()` in `CatEarOverlay` to branch on `anchor.style` (or pass `EarStyle` alongside). Extract current triangle logic into `drawClassicEar()`. Add a stub `drawSharpFelineEar()` that delegates to classic for now. All gates green; no visible change yet. |
| 21.2 | DONE | Style switcher button in `MainScreen` | Add `earStyle: EarStyle` + `onCycleEarStyle: () → Unit` parameters to `MainScreen` and `CameraContent`. In `CameraContent`, add a small `ExtendedFloatingActionButton` (palette icon + current style name label) at `Alignment.BottomEnd` offset above the camera-switch FAB. Wire to `MainViewModel.onCycleEarStyle()`. ViewModel cycles `EarStyle.entries` with wrap-around. Unit-test the cycle logic; UI compiles. |
| 21.3 | DONE | Style B — Sharp Feline renderer | Implement `drawSharpFelineEar()` in `CatEarOverlay`: asymmetric outline (outer edge steep, inner edge gentle), `linearGradient` fill (dark tan rim → sandy centre), salmon inner-ear inset with shadowed leading strip, 3 animated tip-tufts using the existing `InfiniteTransition`. Mirror same geometry in `OverlayCompositor` for capture. Visual check against `02_sharp_feline.png`. |
| 21.4 | DONE | Style C — Lynx Tufted renderer | Implement `drawLynxTuftedEar()`: same outline as Sharp Feline but wider base + 6–8 long dark-brown tufts (24–32 dp) projecting from the tip. Tufts animate with higher sway amplitude (`TUFT_SWAY_RATIO = 0.12f`). Visual check against `04_lynx_tufted.png`. |
| 21.5 | DONE | Style D — Canine Floppy renderer | Implement `drawCanineFloppyEar()`: teardrop path anchored at `anchor.y`, bottom of flap at `anchor.y + 1.2 × anchor.size`, hanging to the outer side. Sway becomes a pendulum swing (±4°) on a single `animateFloatAsState` with `spring(stiffness = StiffnessLow)`. Visual check against `06_canine_floppy.png`. |
| 21.6 | DONE | Style E — Canine Perky renderer | Implement `drawCaninePerkyEar()`: short wide triangle with `drawArc` rounded cap at the tip, warm cream (#D4B896) outer fill, coral interior, cross-hatch texture on outer surface (4 diagonal lines). Visual check against `07_canine_perky.png`. |

### WP 22 — Expression-reactive ears

ML Kit's face detector (already running) returns `smilingProbability` and
`leftEyeOpenProbability` / `rightEyeOpenProbability` every frame when
`CLASSIFICATION_MODE_ALL` is set. Mapping these to ear animations makes the
overlay feel alive and responsive in a way that pure pose-tracking doesn't.

**Prerequisite order**: 22.0 → 22.1 → 22.2
Each task keeps all quality gates green.

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 22.0 | DONE | Expose expression probabilities in `FaceModel` | Add `smilingProbability: Float?`, `leftEyeOpenProbability: Float?`, `rightEyeOpenProbability: Float?` to `FaceModel` (default `null` for backward compat). Enable `CLASSIFICATION_MODE_ALL` in `MlKitFaceDetectorImpl` and populate the new fields. Unit-test new fields; all gates green. |
| 22.1 | DONE | Surface expressions in `OverlayPlacement` | Add `smilingProbability: Float = 0f` and `eyeOpennessMean: Float = 1f` to `OverlayPlacement`. `computeOverlayPlacement` derives them from the new `FaceModel` fields. `PlacementSmoother` smooths both. Unit-tested; all gates green. |
| 22.2 | DONE | Expression-driven ear animations in `CatEarOverlay` | Map expressions to ear behaviour: broad smile (>0.85) → both ears perk upward (tip-Y offset −20% with spring); wide eyes (>0.90) → ears shoot up briefly via `animateFloatAsState`; low eye-openness (<0.20, wink or blink) → ipsilateral ear flattens (xScale → 0.5 with spring). All animations layered on top of existing tilt/sway; no visible change when face is neutral. |

---

### WP 23 — Multi-face tracking

Currently only one face gets ears (first face selected in `MlKitFaceDetectorImpl`).
Supporting multiple faces is the main social/party use case.

**Prerequisite**: WP 22 complete (or at least 22.0, since the model and pipeline
are already clean enough to extend).

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 23.0 | DONE | Multi-face data model | Replace single `FaceModel?` with `List<FaceModel>` throughout the analysis pipeline. `OverlayPlacement` becomes `List<OverlayPlacement>`. Update `PlacementSmoother` to smooth a list keyed by face-tracking ID. `computeOverlayPlacement` is called once per face. All unit tests updated; all gates green. |
| 23.1 | DONE | Multi-face rendering | `CatEarOverlay` iterates `List<OverlayPlacement>` and draws one pair of ears per entry. `OverlayCompositor.composite` does the same for the capture path. Each face inherits the active `EarStyle`. Up to 4 simultaneous faces supported (ML Kit limit); no visible change when only one face is present. |
| 23.2 | TODO | Per-face style assignment (optional stretch) | Allow each detected face to independently cycle its `EarStyle` — e.g., tapping near a face rotates that face's style. Requires mapping tap coordinates to the nearest face anchor. |

---

### WP 24 — Video recording with ears baked in

A shareable MP4 with animated ears is significantly more viral than a still photo.
CameraX provides a `VideoCapture` use case; the compositing challenge is applying
the procedural ear overlay to every recorded frame.

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 24.0 | TODO | CameraX `VideoCapture` integration | Add `VideoCapture` use case to `CameraXControllerImpl`; bind it alongside `Preview` and `ImageAnalysis`. Add start/stop recording API to the camera controller seam. Unit-testable parts covered; all gates green. |
| 24.1 | TODO | Overlay compositor for video frames | Extend `OverlayCompositor` (or create `VideoFrameCompositor`) to composite ears onto each `Surface`-delivered `Bitmap` frame during recording. The current `OverlayPlacement` from the face detector is used for each frame; ears are rendered at the last known placement when no face is detected. |
| 24.2 | TODO | Record button UI | Add a record/stop toggle FAB (or long-press on the capture button). Show a recording-time counter. On stop, save the MP4 to MediaStore and enable sharing via the existing share sheet. |
| 24.3 | TODO | Animated GIF export (stretch) | Post-process a short recording segment into an animated GIF using a bundled encoder (e.g., gifencoder). Share via the existing share intent. |

---

### WP 25 — Product polish

Individually small improvements that collectively lift the "first launch" and
"daily use" quality significantly.

| ID | Status | Task | Acceptance criteria |
|----|--------|------|---------------------|
| 25.0 | TODO | README screenshots / demo GIF (WP 14.4) | Capture at least two screenshots (ears visible, different styles) plus one animated GIF on a real device or webcam-backed emulator. Add to `media/` and embed in README. Supersedes the existing WP 14.4 TODO entry. |
| 25.1 | DONE | Haptic feedback on capture | Single `HapticFeedbackConstants.CONFIRM` vibration on shutter tap via Compose `LocalHapticFeedback`. Costs one line; noticeably improves the capture feel. |
| 25.2 | TODO | Ear colour customisation | Let users pick a base hue for the active ear style via a compact colour-wheel or preset swatches (bottom-sheet or in-camera overlay). `EarAnchor` gains an optional `tintColor: Color`; renderers apply it as a `BlendMode.Modulate` or tint parameter. |
| 25.3 | TODO | First-launch onboarding screen | Single illustrated screen shown once (persisted via `DataStore<Preferences>`): shows an example of the ear overlay and the three main controls (capture, switch, style). Dismisses to the normal camera view. |

---

### Future backlog (not yet broken down)

Extra filters (glasses, hats) · custom AI models (ONNX/TFLite) ·
overlay marketplace · social features.
Each becomes its own set of tasks when prioritised — see [`VISION.md`](VISION.md) "Future Ideas".
