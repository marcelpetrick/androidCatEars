<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Ear Styles — Design & Backlog

Design rationale, visual reference, and implementation plan for **WP 21:
Feline-quality ear styles with a live style switcher**.

---

## Why the current ears need an upgrade

The current renderer (WP 20) draws two solid brown triangles with a pink
inner triangle and 5 sine-wave fur strands. This reads as *geometric* rather
than *feline*. Real cat ears have:

- A **pointed, slightly curved tip** — not a perfectly sharp apex
- **Asymmetric outline**: the leading (outer) edge rises more steeply; the
  trailing (inner) edge has a gentler slope
- A **fur fringe** along the inner edge and at the tip (tufts visible in lynx,
  Maine Coon, etc.)
- **Gradient fill** — lighter at the centre of the outer ear, darker at the
  rim and at the base where it meets the head
- A **pink/salmon auricular interior** visible as an inset shape, not just a
  triangle overlay
- **Subtle shadowing** on the inside wall to give depth

The geometric shape also does not change across head poses beyond the existing
X-squash — a more realistic renderer would foreshorten the entire outline when
viewed from the side.

---

## Design mockups

Ten concept sketches live in [`../ear_design_ideas/`](../ear_design_ideas/)
(each 480 × 480 px, drawn on a minimal face silhouette for scale).

| File | Style | Key character |
|------|-------|---------------|
| `01_classic_triangle.png` | Classic Triangle | Current baseline — solid brown/pink triangles |
| `02_sharp_feline.png` | Sharp Feline | Asymmetric, strongly pointed, inner-ear offset |
| `03_rounded_feline.png` | Rounded Feline | Bezier-curved outline, realistic silhouette |
| `04_lynx_tufted.png` | Lynx Tufted | Long fur tufts projecting from the tip |
| `05_dense_fluffy.png` | Dense Fluffy | Wide base, fur fringe along entire edge |
| `06_canine_floppy.png` | Canine Floppy | Drooping teardrop — golden retriever / spaniel |
| `07_canine_perky.png` | Canine Perky | Short wide triangles with rounded tip — husky |
| `08_rabbit.png` | Rabbit | Tall narrow ovals with pink interior |
| `09_fox.png` | Fox | Small angled triangles, vivid orange, white tip |
| `10_bear.png` | Bear | Tiny rounded semicircles close to the head |

---

## Styles chosen for implementation (WP 21.3 – 21.7)

Five styles will be implemented as distinct `EarStyle` presets:

### Style A — Classic (baseline, already shipped)
Keep the existing WP 20 renderer as-is. Acts as the default / fallback.

### Style B — Sharp Feline ⭐ (priority upgrade)
**Visual goal**: pointy, asymmetric cat ears resembling a domestic shorthair.

Geometry changes from Classic:
- Outer edge: steep rise, tip offset 20% toward the outer side of the face
- Inner edge: gentler 65° slope
- Tip: `drawCircle` with radius ~4 dp to round the apex slightly
- Fill: `linearGradient` — dark tan (#8B6040) at the rim → sandy (#D4A87A) at
  the centre
- Inner ear: inset by 20% on each axis; salmon (#E8A8A0) gradient, slightly
  3D-shaded with a darker strip along the leading edge
- Fur at tip: 3 short strands (8–12 dp) fanning outward from the apex
- Base trim: short horizontal strokes along the bottom edge suggesting fur
  emerging from the head

### Style C — Lynx Tufted
Long prominent ear tufts (dark brown, 6–8 strands, 24–32 dp) projecting from
the tip. Same outer shape as Style B but wider base. Tufts animate with the
fur sway system (same `InfiniteTransition`, higher amplitude).

### Style D — Canine Floppy
Drooping rounded "ear flap" — teardrop shape anchored at the top, hanging to
the side rather than projecting upward. `EarAnchor.y` is used as the hinge
point; the bottom of the flap hangs `1.2 × earSize` below.  Sway animation
becomes a gentle pendulum swing (±4°) instead of tip-strand motion.

### Style E — Canine Perky
Short wide rounded triangles with a `drawArc` cap at the tip. Colouring:
warm cream (#D4B896) outer, coral interior. Fur strands are replaced by a
coarse cross-hatch texture on the outer surface.

---

## Architecture: `EarStyle` enum and renderer strategy

```kotlin
// domain module
enum class EarStyle { CLASSIC, SHARP_FELINE, LYNX_TUFTED, CANINE_FLOPPY, CANINE_PERKY }
```

Each style is a rendering strategy, not a data class. The Compose renderer
(`CatEarOverlay`) selects the correct draw functions based on `EarStyle`.
The bitmap renderer (`OverlayCompositor`) mirrors the same selection for
still-photo capture.

The active style lives in `MainViewModel`:
```kotlin
val earStyle: StateFlow<EarStyle>
fun onCycleEarStyle()           // called by the new FAB / button
```

A small "style" indicator FAB is added to the bottom row (between camera-switch
and capture). It shows the current style name as a short label and cycles on
tap.

---

## UI: style switcher

A `FloatingActionButton` with a palette/ear icon placed between the existing
FABs. On each tap it calls `MainViewModel.onCycleEarStyle()` which advances
`earStyle` through the enum values in order, wrapping around. The FAB label
(or content description) shows the current style name so accessibility users
can identify it.

No settings screen needed — the cycle button is sufficient for the 5-style
set.

---

## Acceptance criteria (per style)

1. Ears visually match the concept sketch in `ear design ideas/`.
2. All animations from WP 20 still apply (fur sway, twitch, spring tilt,
   perspective X-squash).
3. Live preview and still-photo capture produce identical geometry.
4. Style state survives camera-lens toggle but resets on app restart (no
   persistence needed for MVP).
5. Switching style has no visible flash — the spring tilt animation
   interpolates cleanly from whatever the current anchor position is.
6. All CI gates pass (build + detekt + lint + tests ≥ 95% + koverVerify).
