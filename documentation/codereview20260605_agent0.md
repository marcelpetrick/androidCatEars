# Code Review — branch `mpe/improvedCatEars` — 2026-06-05

**Scope:** changes on `mpe/improvedCatEars` relative to `master` (v0.1.172 → v0.1.182).
**Reviewer:** Claude Code (Sonnet 4.6) on behalf of Marcel Petrick.
**Files changed:** `EarRenderStyleSpec.kt` (new), `OverlayCompositor.kt`, `CatEarOverlay.kt`,
`OverlayPlacement.kt`, `EarRenderStyleSpecTest.kt`, `OverlayPlacementTest.kt`,
`CameraPreviewComposable.kt`, `README.md`.

---

## What the branch does

1. Adds `EarRenderStyleSpec` / `EarMaterialSpec` / `EarAssetAnchor` as shared domain types —
   a material-intent layer both renderers can consume without importing Android or Compose APIs.
2. Uses these specs to drive a new material finish pass (soft shadow, outer rim highlight,
   inner rosy glaze, fur-strand texture, optional tufts) drawn on top of every ear style in
   both `OverlayCompositor` (Canvas / photo+video path) and `CatEarOverlay` (Compose / live preview).
3. Adds `leftEyeOpenness` / `rightEyeOpenness` to `OverlayPlacement` for independent per-eye
   wink detection, replacing the averaged `eyeOpennessMean` wink check.
4. README improvements: AI workflow explanation, download badge, privacy statement, module
   structure description, JDK clarification, condensed CI section.

---

## The Good — 5 things done right

### 1. `EarRenderStyleSpec` in `:domain` — correct architectural direction

This is the geometry-sharing layer identified in the previous reviews as "the only correct fix"
for the drawing-logic duplication. Putting `EarMaterialSpec` and `EarAssetAnchor` in the pure JVM
module means both renderers consume the same material intent without importing Android or Compose
APIs. The concept is architecturally right.

### 2. `require()` validation on domain types

`EarAssetAnchor` and `EarRenderStyleSpec` validate their invariants at construction time.
A misconfigured spec (`baseLineRatio > 1.2`, `tipRatio >= baseLineRatio`) fails loudly at
object-creation time rather than silently producing wrong geometry at render time.

### 3. Per-eye openness on `OverlayPlacement`

`leftEyeOpenness` / `rightEyeOpenness` replaces the averaged `eyeOpennessMean` for the wink
check. The left eye now collapses only the left ear — distinctly more fun and more realistic.

### 4. `EarRenderStyleSpecTest` — domain tests without Android

68 lines verifying all 10 specs: valid invariants, non-negative fur counts, tip above base,
every style has a spec. Runs on the JVM in milliseconds.

### 5. Material finish is style-agnostic

Shadow, outer rim, inner glaze, fur strands, and tufts are driven by a single pass that reads
`EarMaterialSpec`. All 10 styles get depth and texture without 10 parallel implementations.
Adding a new style requires only a spec entry.

---

## The Bad — 10 problems

### 1. `earRenderStyleSpec()` allocates 3 new objects per ear per frame — **CRITICAL (performance)**

Both `OverlayCompositor.drawEarOnCanvas()` and `CatEarOverlay.drawEar()` call
`earRenderStyleSpec(style)` on the hot draw path. The function constructs a fresh
`EarRenderStyleSpec` + `EarMaterialSpec` + `EarAssetAnchor` on every invocation.

```
4 faces × 2 ears × 30 fps = 240 spec-triples/sec = 720 short-lived objects/sec
```

On a mid-range device recording at 1080p/30 this will produce intermittent GC pauses visible
as frame-rate drops in the recorded clip.

**Fix:** cache in a lazy map:
```kotlin
private val specCache: Map<EarStyle, EarRenderStyleSpec> by lazy {
    EarStyle.entries.associateWith(::earRenderStyleSpec)
}
```

### 2. `fillPaint()` / `strokePaint()` allocate a new `Paint` per sub-draw call — **CRITICAL (performance)**

`drawSoftEarShadow`, `drawOuterRim`, `drawInnerRosyGlaze`, `drawFurTexture`, and
`drawMaterialTufts` in `OverlayCompositor` all create new `Paint` objects via `fillPaint(argb)`
/ `strokePaint(argb, width)` on every invocation. For the video overlay at 30 fps with 4 faces
this is hundreds of fresh `Paint` instances per second — reversing the hot-path caching work
from the previous session and making the Canvas material pass more expensive than what it
replaced.

**Fix:** pre-compute and cache a `MaterialPaintSet` alongside each `EarRenderStyleSpec` in the
same lazy map, or add the `Paint` instances directly to the spec/material.

### 3. Canvas `drawFurTexture` is static; Compose version is animated — **MEDIUM (visual)**

`OverlayCompositor.drawFurTexture()` has no `swayTime` parameter. Fur strands are drawn at
their rest positions on every video frame. `CatEarOverlay.drawFurTexture()` takes `swayTime`
and swings each strand with `sin(swayTime + index * PHASE_STEP)`.

A user who records a clip while ears are mid-sway will see still fur in the video against the
animated fur they watched in the live preview. Every frame of the recorded clip is `swayTime = 0`.

**Fix:** pass `swayTime = 0f` to the Canvas call site (or the actual frame timestamp) so
captured media matches the last-visible preview state.

### 4. `styleTipOffset()`, `styleLeftBase()`, `styleRightBase()`, `styleTipYOffset()` duplicated in both files — **MEDIUM**

These four `when(style)` functions appear with identical cases and return values in both
`OverlayCompositor.kt` and `CatEarOverlay.kt`. They are exactly the per-style geometry that
should live on `EarRenderStyleSpec` as fields (`tipXOffsetRatio`, `leftBaseRatio`,
`rightBaseRatio`, `tipYOffsetRatio`). The spec exists but doesn't carry these values, so both
renderers re-derive them independently. The duplication problem this branch was meant to address
has been partially — not fully — resolved.

### 5. `EarTintPolicy.OuterFurOnly` is dead code — **MEDIUM**

The policy is declared, assigned `OuterFurOnly` in every spec, and never read. Both renderers
check `p.tint != EarTint.NATURAL` and apply `saveLayer` to the whole ear regardless. The inner
rosy glaze gets tinted alongside the outer fur — the ear turns lavender or sky-blue inner-ear
instead of staying warm pink. This is a visible quality regression on all non-Natural tints.

**Fix:** in the `saveLayer` / `drawIntoCanvas` block, apply tint only to the outer-fur draw
calls when `spec.tintPolicy == EarTintPolicy.OuterFurOnly`, keeping the inner glaze pass
outside the tint layer.

### 6. `MaterialEarGeometry` is two separate private classes, one per file — **MEDIUM**

`OverlayCompositor` has `private data class MaterialEarGeometry(tipX, tipY, leftBaseX,
leftBaseY, rightBaseX, rightBaseY)` with six raw floats. `CatEarOverlay` has `private data class
MaterialEarGeometry(tip: Offset, leftBase: Offset, rightBase: Offset)` with Compose `Offset`s.
Two parallel classes for the same concept in the same branch intended to address duplication.
The domain-layer geometry sharing was started but not finished.

### 7. Straight outer-rim lines don't match curved silhouettes — **MEDIUM (visual)**

`drawOuterRim` draws straight lines from `leftBase` to `tip` and `rightBase` to `tip`.
The base coordinates come from `styleLeftBase()` / `styleRightBase()` which return the
triangle-ear base ratios even for CANINE_FLOPPY (a drooping Bézier curve) and ROUNDED_FELINE
(a cubic spline). The rim lines will be visibly detached from the actual ear edge on those two
styles — floating inside or crossing the curve.

### 8. BEAR shadow oval lands inside the ear body — **LOW (visual)**

`SHADOW_TOP_RATIO = 0.68` places the shadow oval at 68% of the ear's bounding-box height.
The BEAR ear is a circle of radius `0.32 × size` centered at `top + 0.32 × size`. The shadow
therefore sits entirely inside the bear circle and paints a dark smudge through the middle of
the fur rather than a soft shadow beneath it.

### 9. `drawMaterialFinish` in Canvas allocates a `MaterialEarGeometry` data class per call — **LOW**

A new 6-field data class instance is heap-allocated on every `drawMaterialFinish()` call
(per face, per ear, per frame). Replace with local `val tipX`, `val tipY`, etc. inside the
function. The same allocation exists in the Compose version (`MaterialEarGeometry` with
`Offset` objects).

### 10. `SHADOW_WIDTH = 0.92f` is `2 × SHADOW_HALF_WIDTH` expressed as a magic constant — **LOW**

`CatEarOverlay.kt` defines both `SHADOW_HALF_WIDTH = 0.46f` and `SHADOW_WIDTH = 0.92f` side by
side. The relationship between them is implicit. Either derive one from the other
(`SHADOW_WIDTH = SHADOW_HALF_WIDTH * 2f`) or remove `SHADOW_WIDTH` and update the `Size()`
call to `s * SHADOW_HALF_WIDTH * 2f`.

---

## Will it run?

**Yes, but with a performance regression.** Issues 1 and 2 together put several hundred object
allocations per second on the render thread during video recording, reversing the hot-path work
from the previous session. On a mid-range device at 1080p/30 this will cause intermittent GC
pauses visible as frame-rate drops.

## Will it look good?

**Mostly, with two visible exceptions:**

- Issue 5 (inner ear tinted the wrong colour when any non-Natural tint is selected) is
  visible at normal viewing distance on all tinted placements.
- Issue 7 (rim lines float off CANINE_FLOPPY and ROUNDED_FELINE silhouettes) is reproducible
  on two of the ten styles.
- Issue 3 (static fur in video vs animated fur in preview) is subtle but noticeable
  frame-by-frame.

The material finish layer itself — when viewed on CLASSIC, SHARP_FELINE, LYNX_TUFTED, FOX,
RABBIT, BEAR — adds genuine depth and warmth to the ears. The direction is right; the
performance and visual edge-cases need a follow-up pass.

---

## Priority fix order

| # | Issue | Severity |
|---|-------|----------|
| 1 | `earRenderStyleSpec()` allocates per frame | CRITICAL |
| 2 | `fillPaint()`/`strokePaint()` allocates per sub-draw | CRITICAL |
| 5 | `EarTintPolicy.OuterFurOnly` ignored — inner ear wrong colour | MEDIUM |
| 3 | Canvas fur static, Compose fur animated | MEDIUM |
| 4 | Geometry helpers duplicated in both files | MEDIUM |
| 6 | `MaterialEarGeometry` duplicated in both files | MEDIUM |
| 7 | Rim lines don't follow curved ear silhouettes | MEDIUM |
| 8 | BEAR shadow inside ear body | LOW |
| 9 | `MaterialEarGeometry` data class allocation per call | LOW |
| 10 | `SHADOW_WIDTH` magic constant | LOW |
