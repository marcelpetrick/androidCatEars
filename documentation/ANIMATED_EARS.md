<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Animated 3D-Look Cat Ears — Design Document

Design rationale, rendering approach, anchoring strategy, and animation
architecture for **WP 20** (see [`BACKLOG.md`](BACKLOG.md)).

---

## What's wrong today (two separate bugs)

### Bug 1 — Ears fly above the face
Earlier versions placed the ear base too high above the visible head line and
scaled the ears too large. Real-device face examples showed that the visually
natural attachment point is a small overlap into the visible forehead/top-head
line, not a floating point above it.

**Fix**: keep horizontal placement from side landmarks when available, but use
the top-of-face/head box for vertical attachment. The current tuned production
constants are:

- ear bottom: `viewBox.top + viewBox.height * 0.065`
- ear size: `viewBox.width * 0.42`
- fallback half-spacing: `viewBox.width * 0.31`

### Bug 2 — One sprite, no life
The current overlay is a single static PNG/vector scaled and rotated in
unison. It has no perspective (both ears behave identically regardless of head
turn), no fur, no movement, and reads as a flat badge stuck to the screen.

**Fix**: replace the static sprite with a fully procedural Compose Canvas
renderer that applies per-ear geometry, animated hair strands, and a
head-pose-driven perspective squash — all within the existing Compose layer,
no extra runtime dependencies.

---

## Rendering approach: procedural Compose Canvas

**Why not Lottie?** Lottie files are third-party assets that are difficult to
source under GPL. A bespoke Lottie JSON also cannot react to live ML Kit pose
data (head yaw, roll speed). Custom Canvas gives full control and zero new
runtime dependencies.

**Why not a 3D engine (SceneView / ARCore)?** AR scene anchoring requires ARCore
depth API and a much heavier integration. The comical cartoon aesthetic we want
is better served by stylised 2D-plus-tricks than photorealistic 3D geometry.

**Why not Animated Vector Drawable (AVD)?** AVD cannot drive animation parameters
from live pose data; it's a looping timeline. Compose animation APIs (`rememberInfiniteTransition`,
`animateFloatAsState`) can be wired directly to `StateFlow` values from the ViewModel.

---

## 3D illusion techniques (no 3D engine)

| Technique | Compose API | Effect |
|-----------|-------------|--------|
| Gradient fill on ear shape | `drawPath` + `Brush.linearGradient` | Outer edges are dark tan/brown; inner face is lighter; gives a curved felt look |
| Inner-ear accent | `drawPath` (smaller, pink gradient) | Depth of the ear canal; standard cartoon shorthand |
| Drop shadow | `drawPath` with `BlurMaskFilter` or offset duplicate | Anchors ear visually to the head |
| Perspective X-squash | `graphicsLayer { scaleX = … }` per ear | When head turns (headEulerAngleY), the near ear scales to `1.0 + tilt×k`, far ear to `1.0 − tilt×k`; simulates a 3D object rotating |
| Hair strand highlights | Animated `Path` lines drawn on top | Fur light-catch lines that sway reinforce the 3D-soft texture illusion |

---

## Anchor strategy — human ear landmarks

ML Kit Face Detection (`LANDMARK_MODE_ALL`) returns:
- `FaceLandmark.LEFT_EAR` — where the human ear attaches to the side of the skull
- `FaceLandmark.RIGHT_EAR` — symmetrical right side
- `FaceLandmark.LEFT_EAR_TIP` — tip of the ear (helix), ~top of ear
- `FaceLandmark.RIGHT_EAR_TIP`

Cat ears are placed:
- Horizontally centred on `LEFT_EAR_TIP.x` and `RIGHT_EAR_TIP.x` respectively
- Vertically with bottom of the cat ear sitting at `LEFT_EAR_TIP.y`, so they
  visually "grow out of" the skull at the same height as the human ear tip

Fallback if ear tip landmarks are absent (ML Kit can miss them at oblique angles):
estimate from `LEFT_EAR.x + eyeLine offset` using the eye-to-ear distance ratio.

This replaces the bounding-box-top heuristic entirely. The bounding box is still
read (to scale ear size with face distance) but no longer used for Y-anchoring.

---

## OverlayPlacement data model changes

Current:
```kotlin
data class OverlayPlacement(
    val centerX: Float, val topY: Float,
    val width: Float, val rotationDegrees: Float
)
```

New (two independent ear anchors):
```kotlin
data class EarAnchor(
    val x: Float,           // horizontal centre of the cat ear in view-space px
    val y: Float,           // top of the cat ear in view-space px
    val size: Float,        // diameter / reference size in view-space px
    val tiltDegrees: Float, // per-ear rotation (not just global head roll)
    val xScale: Float,      // perspective squash [0.5 .. 1.0]
)

data class OverlayPlacement(
    val leftEar: EarAnchor,
    val rightEar: EarAnchor,
    val headEulerAngleY: Float,  // head yaw, for occlusion awareness
)
```

`PlacementSmoother` smooths each `EarAnchor` independently with the same EMA.

---

## Fur animation design

Each ear has ~5 hair strands drawn as cubic Bezier curves with a randomised
phase offset. A single `rememberInfiniteTransition` drives one global time
variable `t ∈ [0, 2π]` at ~0.8 Hz. Each strand's tip position:

```
tipX = anchorX + strand.offsetX + sin(t + strand.phase) * strand.swayAmplitude
tipY = anchorY - strand.length + cos(t * 0.7 + strand.phase) * strand.swayAmplitude * 0.3
```

A secondary `InfiniteTransition` at ~0.2 Hz with a random start delay produces
the occasional ear-tip twitch (a fast ±8° rotation eased in/out over 120 ms).

Both transitions run continuously as long as `placement != null`. When a face
disappears and reappears, the transitions continue uninterrupted — no cold-start
animation pop.

---

## Pose-reactive ear tilt

Head roll (`headEulerAngleZ`) currently rotates the entire overlay as a unit.
With the new model:

- Left ear tilt = `headEulerAngleZ * LEFT_TILT_FACTOR` (e.g. 0.6)
- Right ear tilt = `headEulerAngleZ * RIGHT_TILT_FACTOR` (e.g. 1.0)
- Both driven through `animateFloatAsState(spring(stiffness=Spring.StiffnessMedium))`
  so a sudden head snap makes the ears follow with a slight elastic lag — a classic
  comical overshoot.

---

## Photo capture compatibility

`OverlayCompositor` currently draws a Bitmap (the static PNG) onto the camera
frame. With a Canvas-drawn overlay there is no bitmap to pass in directly.

Solution: render the `CatEarOverlay` composable into an offscreen `Picture`
using `ComposeView` + `DrawToBitmap`, or — simpler and already available in the
codebase — keep a thin `EarBitmapRenderer` that draws the same ear shapes using
`android.graphics.Canvas` (not Compose Canvas) reusing the same geometry
constants. The live Compose path and the capture path share the constants but
have separate drawing implementations. This avoids a compositing dependency on
Compose internals for the saved photo.

---

## Migration path (incremental, all gates green after each step)

1. **20.0** — Richer ML Kit landmarks: add ear tips + headEulerAngleY to `FaceModel`
2. **20.1** — New anchor algorithm in `computeOverlayPlacement`: ear-landmark-based, bounding box for scale only
3. **20.2** — New `OverlayPlacement` data model with two `EarAnchor`s; update `PlacementSmoother`
4. **20.3** — Compose Canvas ear shape renderer (shapes + gradients, no animation yet)
5. **20.4** — Per-ear perspective X-squash from headEulerAngleY
6. **20.5** — Animated fur strands via `InfiniteTransition`
7. **20.6** — Pose-reactive ear tilt with spring animation
8. **20.7** — Capture path: `EarBitmapRenderer` for `OverlayCompositor`

Each step is independently shippable and keeps the full gate green.

---

## Out of scope (not in WP 20)

- Head-yaw occlusion (far ear hidden when head turns 90°) — needs depth or
  confidence scoring; post-MVP
- Mouth-open ear-perk reaction — needs `FaceContour.UPPER_LIP_BOTTOM` from
  contour mode; deferred
- Colour/style selection UI — deferred to a personalisation WP
- 3D engine (SceneView/ARCore) — not needed for the cartoon aesthetic
