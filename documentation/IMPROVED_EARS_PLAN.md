# Improved Ears Implementation Plan

This plan turns the generated concept previews in
[`media/proposals/improved_ears`](../media/proposals/improved_ears) into a
production path for prettier, more realistic cat ears without making the camera
feel laggy.

The goal is not "full 3D engine first". The right product step is a hybrid
renderer: keep the current fast face tracking, placement, tinting, Party Mode,
and expression logic, but replace the flat painted ear shapes with richer
texture-aware ears that have depth, rosy inner material, fur rims, and more
natural deformation.

## Product Target

Users should feel that the ears are attached to their head, not pasted on top of
the camera preview.

The improved ears should:

- sit on the visible forehead/top-head line without covering eyes or drifting
  upward;
- look more dimensional than the current flat triangles;
- have believable rosy inner ears and darker/lighter fur along the outside;
- keep the playful expression reactions users already have;
- work in photos and videos, not only in the live preview;
- stay smooth on mid-range Android phones.

## Recommended Visual Direction

Use the proposal set as art direction, not literal shipped assets yet.

Best baseline: `03_glossy_stylized_3d.png` plus `10_game_ready_pbr_natural.png`.

Reasoning:

- `03_glossy_stylized_3d` has the cleanest product-readability at small sizes.
- `10_game_ready_pbr_natural` points toward a realistic but implementable
  material model.
- `01_plush_realistic_tabby` is the best default color/material reference.
- `02_lynx_tufted_premium` and `06_neon_party_fox_cat` are better as style
  variants after the base renderer is stable.

Avoid using the watercolor option as the first implementation target. It looks
nice, but it is harder to animate, tint, mirror, and keep consistent across
preview/photo/video.

## Renderer Strategy

### Phase 1: Rich Procedural 2.5D Ears

Replace the current flat ear fill with a layered procedural material:

1. Outer silhouette path per `EarStyle`.
2. Outer fur base gradient: darker rim, lighter center, slight tip shadow.
3. Rosy inner-ear shape: soft pink/peach gradient, smaller than the outer
   silhouette, never reaching the outer rim.
4. Rim fur strokes: short dark and light strokes along the outer edge.
5. Directional fur strokes: low-count curved strokes following the ear shape.
6. Tip/edge tufts for lynx, fluffy, fox, and kitten-like styles.
7. Optional small ambient shadow under the base, clipped so it does not dirty
   the user's forehead.

This keeps tinting and animation flexible. It also avoids shipping a large
bitmap for every style/tint/state combination.

Expected implementation size: 350-700 changed LOC.

### Phase 2: Texture Assisted Renderer

After the 2.5D renderer lands, add tiny reusable texture overlays:

- grayscale fur-noise tile, 128x128 or 256x256;
- inner-ear soft tissue/noise tile;
- optional rim-mask texture for fluffy edges.

These should be small PNG/WebP assets with alpha, not full ear images. The
renderer clips/tints them inside procedural paths.

Expected implementation size: 150-350 changed LOC plus 2-5 small assets.

### Phase 3: Optional Sprite-Based Premium Styles

If the procedural version still feels too flat, add pre-rendered transparent ear
sprites for a small number of premium styles.

This is more expensive technically:

- every sprite must be transparent PNG/WebP with only visible ear pixels;
- left/right versions must mirror cleanly or be rendered separately;
- tinting must be constrained so rosy interiors do not become unnatural;
- animation becomes transform-based rather than shape-based.

Use this only for selected styles, not all ten styles initially.

Expected implementation size: 250-600 changed LOC plus optimized assets.

## Asset Pipeline

Generated proposal images are concept references. They are not ready for runtime.

Runtime assets must be prepared as follows:

1. Choose one target style from the proposal set.
2. Create transparent source art for left and right ear or a single neutral ear
   that mirrors cleanly.
3. Trim all transparent padding while preserving a known anchor point:
   - base center;
   - tip;
   - inner-ear bounds.
4. Export at multiple densities or use one high-resolution source with runtime
   scaling:
   - recommended source: 512-768 px ear height;
   - runtime target: usually 80-260 px on screen.
5. Convert to WebP lossless or PNG after checking alpha quality.
6. Add an asset manifest describing:
   - style;
   - anchor point ratios;
   - default scale;
   - whether mirroring is allowed;
   - tint mask behavior.

Transparent areas must really be transparent. Chroma-key backgrounds from image
generation are acceptable only as an intermediate step; final app assets must
have alpha and no visible fringe.

## Animation Model

Current behavior already reacts to smile, wide eyes, eye openness, head roll,
and wink/blink. Keep that, but make the motion more natural for richer ears.

### Neutral Motion

- Gentle sway: keep the existing low-frequency sway, but apply it mostly to tip
  and fur/tuft strokes, not the entire base.
- Tip twitch: keep periodic twitch, but reduce amplitude for realistic styles.
- Head roll: preserve spring tilt from `headEulerAngleZ`.
- Head yaw: preserve perspective `xScale`; near ear slightly wider, far ear
  narrower.

### Wide Eyes

When eyes open wide:

- ears move up by about 8-12 percent of ear height;
- ears scale vertically by 1.05-1.10;
- tips rotate outward slightly, 2-5 degrees;
- animation should be quick in, springy out.

This reads as alert/curious without covering too much forehead.

### Smile

When smiling strongly:

- ears perk up by about 10-16 percent of ear height;
- tips rotate inward slightly, 2-4 degrees;
- fur/tuft sway amplitude increases subtly.

This should feel delighted, not startled.

### Squint, Blink, or Wink

The user specifically asked for squint behavior. Current code uses mean eye
openness, which can flatten both ears. Improve the model to keep per-eye
openness:

- left eye squint/wink lowers and horizontally compresses the left-side ear;
- right eye squint/wink lowers and horizontally compresses the right-side ear;
- both eyes squint compress both ears horizontally to 0.70-0.85;
- a full blink should be brief and subtle, not a dramatic collapse.

Recommended behavior:

| Expression | Ear response |
| --- | --- |
| Wide eyes | taller, perked, slightly outward tip rotation |
| Smile | warmer perk, slight inward tip rotation |
| Left wink | left ear flattens horizontally and dips; right ear stays alert |
| Right wink | right ear flattens horizontally and dips; left ear stays alert |
| Squint both eyes | both ears become a little wider/flatter, like relaxed focus |
| Neutral | stable base, only tiny fur motion |

## Placement and Head Fit

The current placement work should remain the foundation:

- use face-center-based X spacing rather than human-ear landmarks;
- use eye landmarks to estimate the forehead/top-head line;
- clamp vertical placement so ears do not float too high;
- preserve tracking IDs so Party Mode assignments and animation state do not
  shuffle.

For improved ears, add asset-aware placement:

- each renderer/style declares `baseLineRatio`, `tipRatio`, and
  `visualPaddingRatio`;
- base of the ear should land on the computed head line;
- transparent padding must never affect placement;
- sprite or texture assets must be measured from visible bounds, not bitmap
  bounds;
- renderer tests should assert that the visible base sits at the anchor line.

If using full sprites, store visible content bounds and anchor ratios in code or
an asset manifest. Do not guess from PNG dimensions at draw time.

## Performance Budget

Target: no visible camera lag.

Preview budget:

- 30 fps target on modern phones;
- do not allocate new `Path`, `Paint`, `Bitmap`, shader, or color matrix objects
  per frame when avoidable;
- cache per-style geometry and paints;
- cache texture bitmaps once via `remember`/resource decode;
- keep fur strokes low-count and deterministic;
- cap rendered faces at the existing ML Kit max of four.

Capture/video budget:

- still capture may spend more time, but should remain under a perceptible pause;
- video overlay must avoid per-frame bitmap decoding;
- any texture-assisted video renderer must reuse decoded bitmaps and paints;
- prefer Canvas drawing and cached shaders before adding GPU/GL complexity.

Avoid first:

- full 3D mesh renderer;
- per-frame generated images;
- large transparent PNGs for every color/tint;
- runtime alpha extraction;
- high-resolution full-frame offscreen compositing for preview.

## Architecture Plan

### 1. Create a Shared Renderer Model

Add a small shared model, probably in `:domain` or a lightweight app package:

- `EarRenderStyleSpec`
- `EarMaterialSpec`
- `EarExpressionPose`
- `EarAssetAnchor`

The live renderer (`CatEarOverlay`) and capture/video renderer
(`OverlayCompositor`) must consume the same specs. This prevents the current
duplication from growing worse.

### 2. Split Shape From Material

Each style should define:

- silhouette path;
- inner-ear path;
- tuft/rim zones;
- material colors;
- fur stroke pattern seed.

The renderer then applies:

- expression pose;
- tint;
- perspective scale;
- head-roll tilt;
- texture overlay if enabled.

### 3. Add Per-Eye Expression to Placement

`OverlayPlacement` currently has `eyeOpennessMean`. Add:

- `leftEyeOpenness`
- `rightEyeOpenness`

Keep `eyeOpennessMean` for compatibility or derive it.

This unlocks proper wink/squint behavior.

### 4. Implement One New Baseline Style First

Do not convert all ten styles in one commit.

First target:

- `EarStyle.CLASSIC` becomes the new rich 2.5D baseline, or
- add an internal feature flag for `ImprovedClassic`.

Recommendation: update `CLASSIC` after visual validation, because users expect
the default to improve.

### 5. Port to Capture and Video

After live preview looks right:

- port the same spec to `OverlayCompositor`;
- verify saved photos match preview;
- verify video overlay matches preview at least at neutral animation state;
- decide whether video uses full animation or sampled/rest-state expression.

## Work Packages

### WP IE-1: Renderer Design Spec

Deliverables:

- Add shared style/material data structures.
- Document which proposal image maps to which implementation target.
- Add tests for style spec invariants: valid anchor ratios, positive sizes,
  tint behavior.

Acceptance:

- No visual behavior change.
- Full CI green.

### WP IE-2: Per-Eye Expression Model

Deliverables:

- Add left/right eye openness to `OverlayPlacement`.
- Populate from `FaceModel`.
- Smooth left/right values independently.
- Keep mean compatibility.

Acceptance:

- Unit tests cover neutral, wink, blink, and both-eyes squint values.
- Existing expression behavior remains neutral-compatible.

### WP IE-3: Improved Classic Live Renderer

Deliverables:

- Implement rich 2.5D Classic in `CatEarOverlay`.
- Add outer gradient, rosy inner gradient, rim strokes, soft fur strokes.
- Add per-eye expression pose:
  - wide eyes perk/taller;
  - smile perk/inward;
  - wink/squint horizontal compression and small dip.

Acceptance:

- Live preview remains smooth.
- Default style visibly improves.
- Screenshot/manual visual check against proposal `03`/`10`.

### WP IE-4: Capture Renderer Parity

Deliverables:

- Port Improved Classic to `OverlayCompositor`.
- Share constants/specs with the live renderer where practical.
- Keep saved photos visually close to preview.

Acceptance:

- Still capture contains improved ears.
- No full-frame or per-ear bitmap decode in the hot path.
- Existing compositor tests updated.

### WP IE-5: Texture Overlay Spike

Deliverables:

- Add one small fur/noise texture asset.
- Clip it into the outer ear path.
- Benchmark preview and video path.

Acceptance:

- Measurable no-lag behavior on emulator and at least one physical device.
- If texture adds cost or poor quality, document and stop.

### WP IE-6: Style Rollout

Deliverables:

- Apply the improved material system to 2-3 more high-value styles:
  - Lynx Tufted;
  - Fox/Party;
  - Fluffy Kitten or Wildcat.

Acceptance:

- Party Mode still assigns stable, distinct looks.
- Tints remain pleasant and do not corrupt rosy inner ears.

### WP IE-7: Asset/Sprite Decision

Deliverables:

- Decide whether full transparent sprite ears are needed.
- If yes, create one optimized alpha asset and manifest.
- Compare quality/performance against procedural 2.5D.

Acceptance:

- Documented decision.
- No runtime asset path that depends on non-transparent proposal images.

## Test Plan

Automated:

- domain tests for per-eye expression values and smoothing;
- renderer-spec tests for valid ratios and style mappings;
- compositor geometry tests for visible base alignment;
- screenshot/golden-lite tests if Compose test infra can capture stable output;
- unit tests that tint preserves inner-ear warmth or at least does not run on
  the inner-ear layer unless intended.

Manual/device:

- neutral face, smile, wide eyes, left wink, right wink, both-eyes squint;
- front and rear camera;
- one, two, and four faces;
- Party Mode reroll;
- photo capture;
- five-second video capture;
- Android 10 downport device if that work proceeds.

## Risks and Mitigations

| Risk | Mitigation |
| --- | --- |
| Ears look pasted-on despite better art | Keep base locked to head line, add subtle base shadow, tune scale by face width. |
| Transparent sprite padding shifts placement | Store visible bounds and anchor ratios; never place by raw bitmap bounds. |
| Preview gets laggy | Cache all assets/paints/paths; avoid per-frame allocation; keep texture optional. |
| Live/photo/video diverge | Shared specs; implement live first, then capture parity immediately. |
| Tints ruin rosy inner ears | Apply tint only to outer fur layer, or use separate tint masks. |
| Generated assets are too inconsistent | Use proposal images only for direction; produce controlled runtime assets separately. |
| Too large for one PR/commit | Ship work packages one at a time, starting with Improved Classic only. |

## Recommendation

Implement the next big visual step as procedural 2.5D first, not full sprites or
full 3D.

That gives the best balance:

- much prettier than flat painted ears;
- still flexible for animation and Party Mode;
- no large asset/tint matrix;
- easier capture/video parity;
- safer performance profile.

The first user-visible milestone should be: Improved Classic ears in live
preview and still capture, with per-eye squint/wink behavior. Only after that is
stable should texture overlays or sprite-based premium styles be added.
