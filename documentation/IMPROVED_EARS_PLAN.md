# Improved Ears Implementation Plan (revised 2026-06-05)

This plan supersedes the original 2026-06-04 version. The original correctly
identified the target quality and the right architectural components; it
under-specified the rendering strategy. This revision fixes that.

The goal is spelled out in the proposal images in
[`media/proposals/improved_ears`](../media/proposals/improved_ears).
`01_plush_realistic_tabby`, `03_glossy_stylized_3d`, `09_plush_soft_toy_realistic_fur`,
and `10_game_ready_pbr_natural` show the quality bar: volumetric fur, subsurface
scattering in the inner ear, ambient occlusion at the base, directional
highlighting. These are photo-rendered. That quality is the goal.

---

## What went wrong in the first implementation attempt

Branch `mpe/improvedCatEars` added a "material finish pass" — rim lines,
inner glaze, and fur strokes drawn on top of the existing flat geometric triangles.
The result is still obviously hand-drawn cartoon ears. More Canvas line strokes
cannot produce subsurface scattering, fur texture, or volumetric depth.

**You cannot draw photo-realistic ears with procedural code alone.**
The plan itself says so: *"For the most realistic look, use pre-rendered texture
sprites."* The first implementation ignored this.

### What to keep from that branch

| Component | Keep? | Reason |
|-----------|-------|--------|
| `EarRenderStyleSpec` / `EarMaterialSpec` (domain) | Yes — restructure | The metadata model is correct; `EarAssetAnchor` is exactly what sprite placement needs |
| Per-eye openness on `OverlayPlacement` | Yes entirely | Correct feature, correct implementation |
| `EarTintPolicy` | Yes | Needed to protect inner-ear warmth from tinting |
| Material finish visual pass | **Remove** | Wrong approach; doesn't reach the target; adds hot-path allocations |
| Old flat ear shapes (triangles, cubics, line strokes) | **Remove for sprite styles** | Replaced by bitmap sprite; keep as fallback only |

---

## The correct rendering strategy

### Why sprites, not procedural drawing

The reference images were produced by 3D renderers with:
- Physically-based lighting (rim light, fill, ambient)
- Subsurface scattering in the skin/cartilage
- Per-strand fur simulation
- Ambient occlusion at the ear base
- Smooth volumetric gradients

None of this can be approximated satisfactorily with Canvas `drawLine` or
Compose `drawPath`. The visual gap between procedural ear shapes and the
reference images is not a matter of "more strokes" — it is a qualitative
difference that only pre-rendered imagery can close.

### The hybrid architecture

Keep **all existing logic** as-is:

- Face detection and tracking (ML Kit)
- `OverlayPlacement` — anchor positions, tilt, perspective xScale, expression values
- Placement smoothing (`MultiFaceSmoother`)
- Party Mode, tint cycling, style cycling
- `CameraXControllerImpl`, video overlay, photo capture pipeline

Replace only the **visual fill** of each ear: instead of drawing procedural
shapes, draw a pre-rendered bitmap that has been:
- Background-removed (transparent PNG/WebP)
- Scaled so its base aligns with `anchor.y + anchor.size`
- Rotated by `anchor.tiltDegrees`
- Squeezed horizontally by `anchor.xScale` for perspective
- Tinted via `Paint.colorFilter` (`ColorMatrixColorFilter`) on the outer layer

**Expression animation** becomes transform-only on the sprite:
- Smile: translate up ~12% of ear height, slight vertical scale up
- Wide eyes: translate up ~8%, scale up
- Per-eye wink/squint: horizontal scale to ~0.5 around the base center

This is simpler code than what was built and visually 4× better.

---

## Progress log

Newest first. Update this whenever a work package advances so another agent can pick up cleanly.

- **2026-06-05 — WP S-5c done.** Extracted `app/src/main/res/drawable-nodpi/ear_sharp_feline.png`
  from `08_wildcat_realistic.png` and `app/src/main/res/drawable-nodpi/ear_rounded_feline.png`
  from `01_plush_realistic_tabby.png`. `SHARP_FELINE` and `ROUNDED_FELINE` are now sprite-backed
  and use the shared live/capture/video sprite renderer.
- **2026-06-05 — WP S-5b done.** Extracted `app/src/main/res/drawable-nodpi/ear_dense_fluffy.png`
  from `07_fluffy_kitten.png` and `app/src/main/res/drawable-nodpi/ear_fox.png` from
  `06_neon_party_fox_cat.png`. `DENSE_FLUFFY` and `FOX` are now sprite-backed in domain and
  mapped in the app-layer drawable resolver. `RABBIT` deliberately stays procedural because the
  fluffy-kitten proposal is broad and cat-shaped rather than tall and oval; mapping it to rabbit
  would make the style picker misleading.
- **2026-06-05 — WP S-5a done.** Extracted `app/src/main/res/drawable-nodpi/ear_lynx_tufted.png`
  from `02_lynx_tufted_premium.png` using the same `rembg` + right-side connected-component
  pipeline as `CLASSIC`. `LYNX_TUFTED` is now marked `EarRendererKind.Sprite` in domain and mapped
  to the new drawable in the app layer. Live preview, still capture, and video inherit sprite
  rendering through the S-3/S-4 renderer paths. Tests updated for the second sprite-backed style.
- **2026-06-05 — WP S-4 done.** Still capture and CameraX video overlay now pass
  Android `Resources` into `OverlayCompositor`, which lazily decodes sprite-backed styles through
  the same app-layer `earSpriteDrawableId(EarStyle)` resolver used by live preview. The compositor
  caches decoded bitmaps in a `ConcurrentHashMap`, draws `CLASSIC` sprites with base alignment,
  screen-left mirroring, perspective scale, and rotation, and falls back to the existing procedural
  Canvas path when a style has no sprite or resources are unavailable. Sprite-backed capture avoids
  the procedural tint layer so the single-layer classic sprite keeps its warm inner ear.
- **2026-06-05 — WP S-3 done.** Live preview now resolves sprite resources in the
  app layer via `earSpriteDrawableId(EarStyle)`, loads the `CLASSIC` sprite once per style in
  Compose, and draws sprite-backed ears with transform-only placement: height-scaled to the
  anchor, base-aligned to `anchor.y + anchor.size`, mirrored for the screen-left ear, rotated,
  perspective-scaled, and wink/smile adjusted through the existing `EarAnchor` animation values.
  Procedural styles keep the fallback renderer. Sprite drawing deliberately bypasses the hue
  tint layer for now so the single-layer PNG keeps its rosy inner ear; outer-only sprite tinting
  requires a future split asset or mask. Added app-layer tests for the drawable resolver.
- **2026-06-05 — WP S-2 done.** `EarRendererKind { Procedural, Sprite }` added to
  `EarRenderStyleSpec` (domain) with a `Procedural` default so existing constructors are
  unaffected. `CLASSIC` is marked `Sprite`; all other styles remain `Procedural`. Domain stays
  Android-free — the actual `R.drawable` id is resolved in the app layer (see WP S-3). Tests
  added in `EarRenderStyleSpecTest`: CLASSIC is Sprite / others Procedural, and the field
  defaults to Procedural. `:domain:test` green.
- **2026-06-05 — WP S-1 done.** Extracted `app/src/main/res/drawable-nodpi/ear_classic.png`
  (435×512 RGBA, ~220KB) from `03_glossy_stylized_3d.png` using `rembg` (u2net) for background
  removal, then `scipy.ndimage` connected-component labelling to isolate the single right-side
  ear (the proposal image contains both ears; the left-ear remnant was dropped). Auto-cropped to
  visible bounds + 40px padding, scaled to 512px tall. Clean alpha, no fringe. The same pipeline
  works for other proposals; reusable steps: `rembg` → component-label → keep largest with
  centroid in target half → crop → resize.
- **2026-06-05 — Plan rewritten.** Sprite-first strategy replaces the failed procedural
  material-finish approach.

### Reproducing the asset extraction

`rembg` with CPU backend was installed via `pip3 install "rembg[cpu]" --break-system-packages`.
The extraction reads a proposal PNG, removes the gray background, labels connected components,
keeps the largest component whose centroid is in the right half, crops to content + padding, and
resizes to 512px tall. For a new style, change the source filename and (if the target ear is on
the left) the centroid-half test.

## Work packages

### WP S-1 — Asset extraction — **DONE (2026-06-05)**

**What:** Extract transparent-alpha PNG sprites from the proposal images using
AI background removal (`rembg`). One sprite per style, covering the right ear.
The left ear is produced at render time by horizontal mirroring.

**Deliverables:**
- `app/src/main/res/drawable-nodpi/ear_classic.png` (from `03_glossy_stylized_3d.png`)
- Additional style sprites added as implementation proceeds

**Acceptance:**
- No visible fringe or gray halo
- Ear fills ~90% of sprite canvas
- Real alpha channel, no rectangular background

### WP S-2 — Sprite model in `:domain` — **DONE (2026-06-05)**

**What was built (differs slightly from the original sketch):** the domain stays Android-free,
so it declares only *that* a style is sprite-backed — not which drawable. The `R.drawable` id is
resolved in the app layer (WP S-3) by a small `EarStyle -> Int?` mapping. This keeps `:domain`
free of generated `R` references and fully JVM-testable.

```kotlin
enum class EarRendererKind { Procedural, Sprite }

data class EarRenderStyleSpec(
    val style: EarStyle,
    val material: EarMaterialSpec,
    val anchor: EarAssetAnchor,
    val furStrokeCount: Int,
    val supportsTufts: Boolean,
    val tintPolicy: EarTintPolicy,
    val rendererKind: EarRendererKind = EarRendererKind.Procedural,  // new; default keeps old call sites valid
)
```

`CLASSIC.rendererKind == Sprite`; all others `Procedural`.
`EarAssetAnchor.baseLineRatio` is the fraction of sprite height where the ear base (the point
that sits on the forehead/head line) is located in the bitmap. For a well-cropped sprite this is
`1.0` (bottom edge). The app-layer `EarStyle -> R.drawable` map is the place to add new sprites.

**Acceptance (met):** all existing spec invariant tests pass; new tests assert CLASSIC is Sprite,
others Procedural, and the field defaults to Procedural. `:domain:test` green.

### WP S-3 — Sprite live renderer in `CatEarOverlay`

**What:** In `drawEar()`, detect `EarRendererKind.Sprite` and call
`drawSprite()` instead of the procedural when branch.

```kotlin
// Loaded once per composable lifecycle (remember keyed on style)
val bitmap: Bitmap? = remember(style) {
    val resId = earRenderStyleSpec(style).spriteDrawableId ?: return@remember null
    BitmapFactory.decodeResource(context.resources, resId)
}

// Per-frame — matrix math only, no allocation
fun DrawScope.drawSprite(anchor: EarAnchor, bitmap: Bitmap, isLeft: Boolean, tintPaint: Paint?) {
    val matrix = buildSpriteMatrix(anchor, bitmap.width, bitmap.height, isLeft)
    drawIntoCanvas { canvas ->
        if (tintPaint != null) canvas.saveLayer(spriteBounds(anchor), tintPaint)
        canvas.nativeCanvas.drawBitmap(bitmap, matrix, null)
        if (tintPaint != null) canvas.restore()
    }
}

fun buildSpriteMatrix(anchor: EarAnchor, bw: Int, bh: Int, isLeft: Boolean): Matrix {
    // 1. Scale: sprite height → anchor.size
    val scale = anchor.size / bh
    // 2. Translate: base (bottom of sprite) → anchor.y + anchor.size
    // 3. Center horizontally on anchor.x
    // 4. Mirror for left ear (scaleX = -1 around anchor.x)
    // 5. Rotate by anchor.tiltDegrees around base center
    // 6. Squeeze by anchor.xScale for perspective
}
```

**Expression animation:**
- `yShiftFraction` (smile/wide eyes) → `matrix.preTranslate(0, yShift)`
- `winkXScale` → `matrix.preScale(winkXScale, 1f, anchor.x, baseY)`

**Tinting:**
- `EarTintPolicy.OuterFurOnly`: apply `saveLayer` with `ColorMatrixColorFilter`
  over the outer draw, then overdraw the inner ear region untinted

**Acceptance:**
- CLASSIC style shows the realistic sprite in live preview
- All expression reactions work
- No per-frame Bitmap allocation
- 30fps with 4 faces on a mid-range device

### WP S-4 — Sprite capture and video renderer in `OverlayCompositor`

**What:** Same sprite approach for the Canvas path. Bitmaps decoded lazily,
cached at object level.

```kotlin
private val spriteBitmaps: Map<EarStyle, Bitmap?> by lazy {
    EarStyle.entries.associateWith { style ->
        earRenderStyleSpec(style).spriteDrawableId?.let { resId ->
            BitmapFactory.decodeResource(resources, resId)
        }
    }
}
```

Video overlay uses rest-state animation (no `swayTime`). Still capture and
video ear quality matches the live preview.

**Acceptance:**
- Saved photo contains the realistic sprite
- Video clip contains the realistic sprite
- No per-frame decode

### WP S-5 — Style rollout

Apply sprites to additional styles as assets become available, in order:

1. `CLASSIC` — default, highest impact
2. `LYNX_TUFTED` (ref: `02_lynx_tufted_premium.png`)
3. `DENSE_FLUFFY` / `RABBIT` (ref: `07_fluffy_kitten.png`)
4. `FOX` (ref: `06_neon_party_fox_cat.png`)
5. Remaining styles — keep procedural fallback until sprites exist

---

## Performance budget (unchanged from original plan)

- No `Path`, `Paint`, `Bitmap`, or color matrix allocations per frame
- Sprites decoded once, stored in `remember` / lazy companion map
- No per-frame `saveLayer` larger than the sprite bounds
- Tint via `Paint.colorFilter` on `drawBitmap`, not full-frame offscreen

### Anti-patterns to avoid

- More Canvas strokes ≠ photo-realistic — do not try to approximate the
  reference images procedurally
- Never decode a Bitmap inside a draw callback or `DrawScope` lambda
- Do not use full-frame `saveLayer` for tinting — scope it to the sprite bounds
- Do not use the proposal images directly as runtime assets without alpha extraction

---

## Architecture at end state

```
EarRenderStyleSpec (domain)
  ├── rendererKind: Sprite | Procedural
  ├── spriteDrawableId: R.drawable.ear_classic | null
  ├── anchor: baseLineRatio=1.0, tipRatio=0.05
  └── tintPolicy: OuterFurOnly

CatEarOverlay (live preview)
  └── drawEar()
        ├── Sprite → drawBitmap(cachedBitmap, spriteMatrix, tintPaint)
        └── Procedural → existing flat draw (fallback while no sprite exists)

OverlayCompositor (photo + video)
  └── drawEarOnCanvas()
        ├── Sprite → canvas.drawBitmap(cachedBitmap, spriteMatrix, tintPaint)
        └── Procedural → existing flat draw (fallback)
```

---

## Proposal image reference

| File | Best use |
|------|----------|
| `01_plush_realistic_tabby.png` | Color/material baseline for CLASSIC |
| `02_lynx_tufted_premium.png` | Shape reference for LYNX_TUFTED |
| `03_glossy_stylized_3d.png` | **First prototype sprite — cleanest edges** |
| `07_fluffy_kitten.png` | Reference for RABBIT / DENSE_FLUFFY |
| `09_plush_soft_toy_realistic_fur.png` | Fur density reference |
| `10_game_ready_pbr_natural.png` | Material target for realistic default |

---

## Test plan (retained from original)

Automated:
- Domain tests for spec invariants (rendererKind ↔ spriteDrawableId consistency)
- Compositor geometry tests: visible base aligns at anchor line
- Unit tests: tint does not corrupt inner-ear warmth (tintPolicy respected)

Manual/device:
- Neutral face, smile, wide eyes, left wink, right wink, both-eyes squint
- Front and rear camera (mirroring correct)
- One, two, four faces in Party Mode
- Photo capture; five-second video capture
