# Code Review 2026-06-05 - Agent 1

The patch builds, but the new material renderer introduces visible rendering bugs for bear ears and does not honor the newly added outer-fur-only tint policy. These are user-visible issues in the overlay behavior.

## Findings

### P2 - Keep material geometry inside round bear ears

File: `app/src/main/java/it/marcelpetrick/catears/overlay/CatEarOverlay.kt`

When `EarStyle.BEAR` is selected, the generic material finish uses `top + s` as the base line, but `drawBearEar` only draws a circle with radius `0.32 * s`, so the visible bear ear ends around `top + 0.64 * s`. The new rim, glaze, and fur therefore extend well below the round ear and onto the forehead in both live preview and the duplicated capture compositor geometry.

Recommended fix: use a style-specific baseline for round ears or skip the triangular material finish for bear ears.

### P2 - Honor outer-only tinting for inner ear layers

File: `app/src/main/java/it/marcelpetrick/catears/overlay/CatEarOverlay.kt`

For any non-natural tint, the inner glaze is drawn inside the existing tinted `saveLayer`, so it is hue-rotated together with the fur even though every new render spec sets `EarTintPolicy.OuterFurOnly`. This makes the rosy inner ear turn blue, green, or other tint colors and leaves the new tint policy and tests disconnected from the actual renderer.

Recommended fix: draw inner layers outside the tint layer or branch on `tintPolicy` so only the outer fur is recolored.
