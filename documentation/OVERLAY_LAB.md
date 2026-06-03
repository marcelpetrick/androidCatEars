<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Overlay Lab — developing & tuning the cat-ear placement on the desktop

**Goal:** iterate on *how well the cat ears fit the face* without rebuilding and
deploying to a phone each time — feed in sample photos, run the real placement
algorithm on the desktop JVM, see the fit, and fine-tune the constants against a
fixed image set until it's right.

This is feasible **because the project was built for it.** The placement math
lives in the pure-Kotlin `domain` package (`computeOverlayPlacement`,
`CoordinateTransform`, `OverlayPlacement`, `BoundingBox`, `TransformContext`) —
**no Android imports.** The exact same code that positions the ears on the phone
runs unchanged on a laptop.

---

## The one wrinkle: detection vs. placement

Two separable concerns:

| Stage | On the phone | On the desktop |
|-------|--------------|----------------|
| **Detection** — find the face box + landmarks | ML Kit (Android-only) | needs a substitute (see below) |
| **Placement** — turn a face into ear position/scale/rotation | `domain` (pure) | **identical `domain` code** |

So we only need to *substitute the detector* on the desktop. Everything we
actually want to tune (placement) is shared. Three ways to get landmarks on the
desktop, cheapest first:

1. **Hand-annotated fixtures** — for each sample photo, record the face box and a
   few key points (eye centres, etc.) in a small JSON file. Zero extra
   dependencies. Perfect for a curated tuning set of ~10–20 images.
2. **Desktop face detector** — auto-generate landmarks from arbitrary photos with
   **OpenCV/JavaCV** (`org.bytedeco:javacv`, DNN or Haar face detector) or **dlib**
   via JavaCPP. JVM-native, no phone.
3. **Python sidecar** — run **MediaPipe Face Mesh** once offline (Python) to emit
   landmark JSON, then the Kotlin harness consumes it. Highest-fidelity landmarks.

---

## Recommended design — two layers

### Layer 1 — Golden-fixture regression tests *(do this first; pure JVM, lives in this repo)*

A normal JUnit5 test set that turns "does the fit look right" into an objective,
repeatable check:

```
app/src/test/resources/overlay-fixtures/
  alice.json   # { image: {w,h}, face: {box, landmarks}, expected: { earL:{x,y}, earR:{x,y}, scale, rotation }, tolerancePx }
  bob.json
  ...
```

A parameterized test loads each fixture, builds the `TransformContext`, calls
`computeOverlayPlacement(...)`, and asserts the result is within `tolerancePx`
of `expected`. The **tuning loop** becomes:

> tweak the placement constants → `./gradlew :app:test` → red/green tells you,
> per face, whether the fit improved — no device, runs in CI.

This is the highest-ROI step: it makes "fine-tune until it fits" measurable and
guards against regressions forever after. It needs **no new dependencies.**

### Layer 2 — Desktop visualiser *(when you want to *see* it)*

A small **separate Gradle JVM module** (`:overlay-lab`) — not part of the APK —
that depends only on `domain`:

- Loads each image in `samples/` plus its landmark JSON (hand-annotated, or
  produced by a desktop detector from Layer-1 option 2/3).
- Calls the **same** `computeOverlayPlacement` and draws the ear asset over the
  photo (Compose for Desktop, or plain Java2D/Swing).
- Shows the composite in a window and/or **exports a golden PNG** per sample.

Now you can eyeball the fit across the whole set at once, and the exported PNGs
double as **screenshot regression goldens** (same idea as Paparazzi, applied to
the lab). Optionally add a couple of sliders bound to the placement constants for
live, interactive tuning.

```
:app          (Android — uses domain via ML Kit)
:overlay-lab  (Desktop JVM — uses domain via fixtures/OpenCV)   ← new, optional
   └── depends on :domain
:domain       ← the shared, pure placement code
```

> Note: `domain` is currently a package inside `:app`. To share it with a desktop
> module cleanly, extract it into its own `:domain` (or `:geometry`) Gradle module
> — a mechanical refactor with no behaviour change, and good hygiene anyway.

---

## How we'd actually work together

1. **You drop sample photos** into `samples/` (varied: faces near/far, tilted,
   off-centre, front/rear, light/dark).
2. **We annotate them** — either I help you hand-label the face box + key points
   into JSON, or we wire OpenCV so it's automatic.
3. **You mark the desired ear position** on a few of them (where the ears *should*
   sit) — that becomes the `expected` in the fixtures.
4. **I tune the constants** (ear width ratio, vertical offset, scale-from-face,
   rotation-from-eye-line) and iterate against `:app:test` + the visualiser until
   the numeric error is small and the PNGs look right.
5. **Ship unchanged** — because placement is shared `domain` code, the tuned
   result is exactly what runs on the phone. No re-tuning after deploy.

This directly resolves backlog item **TODO #3** ("tune overlay placement
constants against real faces") and turns the previously hardware-gated tuning
into a fast desktop loop.

---

## Why not just test on the phone?

You still should, once, to confirm ML Kit's landmarks match the fixtures'
assumptions. But the *tuning* — the slow part — belongs on the desktop where the
loop is seconds, not a build-install-aim cycle, and where every change is checked
against the whole image set at once. The phone then only validates the detector,
not the geometry.
