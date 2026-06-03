<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Architecture

**androidCatEars** is a single-module Android app: a real-time AR camera that
detects faces and overlays cat ears, with capture, save, and share. This
document explains how the code is organised and the one rule that shapes every
design decision: **keep logic testable on the JVM, isolate the Android
framework behind thin seams.**

---

## High-level shape

- **Pattern:** MVVM. `MainViewModel` holds UI state as `StateFlow`s; Compose
  renders it; user intents flow back as method calls.
- **Module:** one `:app` module, **package-by-feature** (not by layer).
- **DI:** Hilt. The Application is annotated `@HiltAndroidApp`; `MainActivity`
  is an `@AndroidEntryPoint`; `di/AppModule` binds seam interfaces to their
  Android implementations.
- **Single source of truth for versioning:** `version.properties` → Gradle →
  `BuildConfig` → UI. Never hardcoded.

```
Camera frame ──► ImageAnalysis ──► FaceDetector (ML Kit) ──► FaceModel
                                                               │
                                          CoordinateTransform (image→view, mirror)
                                                               │
                                          OverlayPlacement + PlacementSmoother
                                                               │
   PreviewView ◄──────────── Compose Canvas overlay ◄──── MainViewModel state
                                                               │
   Capture ──► composite (view-space, WYSIWYG) ──► ImageSaver (MediaStore) ──► Share intent
```

---

## Packages

| Package | Responsibility | Tested on JVM? |
|---------|----------------|----------------|
| `domain` | Pure Kotlin: state models + all geometry/logic. No Android imports. | **Yes — carries the 95% gate** |
| `ui` | Compose screens, `MainActivity`, `MainViewModel`, theme. | ViewModel logic yes; Compose no |
| `camera` | CameraX seam + concrete controller + preview composable. | Seam yes; impl no (device-only) |
| `facedetect` | Face-detector seam + ML Kit implementation. | Seam yes; impl no (device-only) |
| `overlay` | Compose `Canvas` that draws the ears from a placement. | No (Compose) |
| `capture` | Overlay compositing, bitmap decode, MediaStore save. | Pure parts yes; Android glue no |
| `share` | Share-intent construction. | Pure `ShareConfig` yes; intent no |
| `di` | Hilt module wiring. | No (framework glue) |

---

## The seam pattern (why coverage is 95% without a device)

Android framework types — `Bitmap`, `Canvas`, `Matrix`, `Intent`, `MediaStore`,
CameraX, ML Kit — **cannot be exercised in plain JVM unit tests**. Rather than
reach for instrumented tests or Robolectric, we split every such feature in two:

1. **A pure function or data class** that does the real decision-making in terms
   of plain numbers/strings (e.g. `computeOverlayPlacement`, `DrawTransform` +
   `computeDrawTransform`, `buildShareConfig`, `ImageFileName`,
   `CoordinateTransform`). These are exhaustively unit-tested.
2. **A thin Android adapter** that only translates the pure result into a
   framework call (e.g. `applyToMatrix`, `toChooserIntent`, the MediaStore
   write). These contain no branching worth testing and are **excluded from
   Kover** with a documented reason in `app/build.gradle.kts`.

Device-dependent collaborators sit behind **seam interfaces**
(`CameraControllerSeam`, `FaceDetectorSeam`) so the ViewModel and tests depend
on an abstraction, and the real CameraX/ML Kit classes are swapped in via Hilt.

**Consequence:** the coverage gate measures the part that actually contains
logic; UI, DI, and framework glue are excluded, not gamed.

---

## Notable design decisions

- **WYSIWYG capture.** Instead of compositing onto a full-resolution
  `ImageCapture` frame (a different coordinate space that needs a complex
  transform), capture grabs the `PreviewView` bitmap (already in view space) and
  composites the overlay using the *same* view-space placement the user sees —
  so the saved photo matches the preview exactly.
- **Jitter smoothing.** `PlacementSmoother` low-passes successive placements so
  the ears don't twitch frame-to-frame. The smoothing math is pure and tested.
- **Front-camera mirroring** is handled in `CoordinateTransform`, unit-tested
  across orientations.
- **Branded theme by default.** Dynamic colour is opt-in so the cat-ear orange
  identity is consistent; light/dark follow the system, including the pre-Compose
  window backdrop (`values-night`).

---

## Build & quality gates

Every commit must pass the full gate (`./scripts/ci.sh`): build → Spotless
(ktlint) → detekt → Android Lint (warnings as errors) → unit tests → Kover 95%
verify. Versioning auto-bumps the patch via a pre-commit hook. See
[`agents.md`](agents.md) for the binding engineering rules and
[`RELEASE.md`](RELEASE.md) for release/signing.
