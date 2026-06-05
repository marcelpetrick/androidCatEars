<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Ear Styles

Current user-facing ear styles are photorealistic, sprite-backed cat/fox ears.
The older procedural dog, rabbit, and bear styles were removed from the app
surface because they did not meet the visual quality bar.

## Style Set

| Style | Sprite asset | Source proposal |
|-------|--------------|-----------------|
| `CLASSIC` | `ear_classic.png` | `03_glossy_stylized_3d.png` |
| `SHARP_FELINE` | `ear_sharp_feline.png` | `08_wildcat_realistic.png` |
| `ROUNDED_FELINE` | `ear_rounded_feline.png` | `01_plush_realistic_tabby.png` |
| `LYNX_TUFTED` | `ear_lynx_tufted.png` | `02_lynx_tufted_premium.png` |
| `DENSE_FLUFFY` | `ear_dense_fluffy.png` | `07_fluffy_kitten.png` |
| `FOX` | `ear_fox.png` | `06_neon_party_fox_cat.png` |

## Rendering Strategy

`EarStyle` lives in the domain module and contains only the six styles above.
Every entry is marked `EarRendererKind.Sprite` in `EarRenderStyleSpec`; the app
module maps each style to a `drawable-nodpi` transparent PNG via
`earSpriteDrawableId(EarStyle)`.

Live preview, still-photo capture, and video recording all use the same visual
strategy:

- decode each sprite lazily and cache it;
- scale to the computed ear anchor height;
- align the sprite base to the forehead attachment line;
- mirror the right-ear sprite for the screen-left ear;
- apply head-roll rotation, yaw perspective squeeze, smile/wide-eye lift, and
  wink squashing as transforms.

The style switcher cycles through the six photorealistic styles. Party Mode
assigns stable per-face styles and re-rolls those assignments. User-facing tint
cycling was removed so the sprites keep their authored fur and inner-ear colour.

## Acceptance Criteria

1. Style cycling exposes only sprite-backed styles.
2. Party Mode never assigns removed procedural styles.
3. The palette/tint button is not shown in the camera UI.
4. Live preview, photo capture, and video overlay render matching sprite ears.
5. All CI gates pass.
