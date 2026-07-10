# Plan: User-Adjustable Ear Base-Point (2026-07-10)

> **Status (2026-07-10): implemented** — WP A–E landed (see BACKLOG WP 40); the
> real-device pass (40.5) is still open. Two deviations from the plan below:
> persistence commits on slider release / panel close instead of a debounced
> write (deterministic and fewer writes), and the adjustment state lives in
> dedicated `MainViewModel` flows rather than `MainUiState`, which is a sealed
> permission/camera phase interface, not a data holder.

## Goal

Let the user fine-tune where the ears attach to their head — shift the base point
**up/down** and move both ears **inward/outward** — because head shapes, hairstyles,
and hats make any single default imperfect. The adjustment must:

- stay **relative to the auto-fitted placement** (face tracking keeps working; the
  offsets ride on top of it and scale with face size/distance),
- be **persisted locally** on the device (no account, no network — consistent with
  the app's privacy stance),
- be **visually noticeable** when a non-default adjustment is active,
- apply identically to live preview, photo capture, and video recording.

## Current code path

1. `computeOverlayPlacement()` (`:domain`, `OverlayPlacement.kt`) derives both
   `EarAnchor`s from the view-space face box: ear size = `boxWidth * widthRatio`,
   horizontal spread = `earSize * EAR_HALF_SPACING_RATIO` around `centerX`, and the
   base line (`earBottomY`) from the eye line / box-top ratios.
2. Live preview (`CameraPreviewComposable.facePlacements()`), photo capture
   (`OverlayCompositor`), and video (`OverlayEffect`) all consume the placements
   produced by this single function — one insertion point covers every render path.
3. There is **no persistence layer** in the app yet (no DataStore, no
   SharedPreferences).

## Design

### 1. Domain model (`:domain`, pure JVM)

New value type, expressed in **face-box-relative fractions** so the adjustment
auto-scales with face size and distance:

```kotlin
data class EarAdjustment(
    /** Vertical shift of the ear base line, fraction of face-box height.
     *  Negative = up, positive = down. Range [-0.25 .. +0.25]. */
    val verticalOffset: Float = 0f,
    /** Horizontal spread delta, fraction of ear half-spacing.
     *  Negative = inward, positive = outward. Range [-0.5 .. +0.5]. */
    val spreadOffset: Float = 0f,
) {
    val isCustom: Boolean get() = verticalOffset != 0f || spreadOffset != 0f
    fun coerced(): EarAdjustment // clamps both axes into the documented ranges
    companion object { val DEFAULT = EarAdjustment() }
}
```

`computeOverlayPlacement()` gains an `adjustment: EarAdjustment = EarAdjustment.DEFAULT`
parameter applied after the auto-fit math:

- `topY += viewBox.height * adjustment.verticalOffset`
- `halfSpacing *= (1f + adjustment.spreadOffset)`

Default parameter keeps every existing call site and test compiling unchanged.

### 2. Persistence (`:app`, behind a seam)

- Add `androidx.datastore:datastore-preferences` (pinned in the version catalog).
- Seam interface in the app module, mirroring the existing `*Seam` pattern so the
  ViewModel stays testable with plain fakes:

```kotlin
interface EarAdjustmentStoreSeam {
    val adjustment: Flow<EarAdjustment>
    suspend fun save(adjustment: EarAdjustment)
    suspend fun reset()
}
```

- `DataStoreEarAdjustmentStore` implements it (two float preference keys), provided
  by Hilt in `AppModule`. Values are clamped through `EarAdjustment.coerced()` on
  read, so a corrupt or out-of-range stored value can never push ears off-screen.

### 3. ViewModel & state

- `MainUiState` gains `earAdjustment: EarAdjustment` and
  `isAdjustmentPanelVisible: Boolean`.
- `MainViewModel` collects the store flow at init, exposes
  `onAdjustmentChanged(EarAdjustment)` (updates state immediately for live feedback,
  debounced write ~300 ms to DataStore), `onAdjustmentReset()`, and panel
  show/hide intents.
- `facePlacements()` in `CameraPreviewComposable` passes the current adjustment into
  `computeOverlayPlacement()`. Because photo/video reuse the same placements, all
  three outputs stay in sync with zero extra plumbing.

### 4. Adjustment UI ("Fit" panel)

Not a separate navigation destination — the user must see the live effect while
adjusting, so it is a bottom-sheet-style panel over the running preview:

- New **Fit** button (tune/sliders icon) in the existing control column.
- Panel content:
  - **Height** slider: "Lower ↓ / ↑ Higher", mapped to `verticalOffset` (inverted:
    slider up = ears up).
  - **Width** slider: "Narrower ←→ Wider", mapped to `spreadOffset`.
  - **Reset to auto** button, enabled only when `isCustom`.
  - Small live readout of both values in percent (e.g. "Height +8 % · Width −12 %"),
    doubling as the accessibility description.
- Ears keep tracking the face live while sliders move — the whole point is tuning
  against your own head in real time.

### 5. "Custom setting active" indicator

Two layers, both driven by `earAdjustment.isCustom`:

- **Persistent badge**: a small accent-colored dot on the Fit button whenever a
  custom adjustment is stored (the pattern users know from "unread" markers). Content
  description announces "custom ear fit active" for TalkBack.
- **Transient confirmation**: the existing status-banner mechanism shows
  "Custom ear fit applied" once when a panel session ends with a non-default value,
  and "Ear fit reset to auto" after a reset.

The captured photo/video needs no watermark — the adjustment is a fit preference,
not a filter.

## Work packages

| ID | Task | Acceptance criteria |
|----|------|---------------------|
| A | Domain: `EarAdjustment` + placement math | `computeOverlayPlacement()` applies vertical/spread offsets relative to face-box size; clamping enforced; existing callers unaffected via default parameter; unit tests cover default (no-op), both extremes, clamping, and `isCustom`; `:domain` Kover stays ≥ 95 %. |
| B | Persistence: DataStore + seam | `EarAdjustmentStoreSeam` + DataStore impl wired through Hilt; values survive process restart; out-of-range stored values are clamped on read; ViewModel tests use an in-memory fake. |
| C | ViewModel/state wiring | `MainUiState` exposes adjustment + panel visibility; slider intents update placements live and persist debounced; reset intent restores `DEFAULT`; Turbine tests for the flow. |
| D | Fit panel UI | Panel opens over live preview; two sliders + reset; live ear movement while dragging; panel state survives rotation (state hoisted to ViewModel, not `remember`). |
| E | Custom-fit indicator | Badge on Fit button iff `isCustom`; status-banner confirmations on apply/reset; TalkBack descriptions present. |
| F | Docs & gate | README feature blurb + usage step; ARCHITECTURE.md seam list updated; full `scripts/ci.sh` green. |

Suggested order: A → B → C → D → E → F; A is independently shippable and the only
`:domain` change.

## Testing

- **Domain**: pure-JVM tests for the offset math (fractions scale with box size,
  clamping, no-op default) — keeps the 95 % gate green.
- **ViewModel**: fake store seam; assert live-update-then-debounced-persist,
  reset behavior, badge state derivation.
- **Manual device pass**: adjust in portrait, verify photo + video captures bake the
  adjusted ears; rotate to landscape (verify the adjustment survives the rotation
  path hardened in WP 39); restart the app and confirm persistence; multi-face party
  mode (adjustment applies to all faces — it is a device-level preference, v1
  explicitly does not do per-face adjustments).

## Risks & decisions

- **Per-face vs. global**: v1 is a single global adjustment. Per-face persistence is
  impossible anyway — ML Kit tracking IDs are session-scoped and there is no face
  recognition (and adding it would clash with the privacy story).
- **Range tuning**: the proposed ±25 % height / ±50 % spread ranges need a
  real-device check; wide enough to be useful, narrow enough that ears cannot detach
  absurdly from the head. Adjust constants after the first manual pass.
- **Smoothing interaction**: offsets are applied inside `computeOverlayPlacement()`
  *before* `MultiFaceSmoother`, so slider drags animate smoothly for free; no
  smoother reset is needed when the adjustment changes.
- **Capture consistency**: photo/video reuse live placements, so no divergence is
  expected; the manual pass explicitly verifies it.
