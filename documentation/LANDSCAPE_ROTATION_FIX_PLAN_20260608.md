# Landscape Rotation Fix Plan 2026-06-08

## Bug

When the device rotates by 90 degrees, the live preview continues but the ears can be painted far
away from the detected head. Ear geometry itself is not the likely cause: the renderer draws in
Compose view-space, so a large landscape-only offset points to the camera image-to-view transform or
stale placement state during rotation.

## Current Code Path

1. `CameraXControllerImpl.analyseFrame()` receives `ImageProxy` frames and asks CameraX for an
   `OutputTransform` via `ImageProxyTransformFactory`.
2. `CameraPreviewComposable` combines that transform with `PreviewView.outputTransform` using
   CameraX `CoordinateTransform`.
3. Face boxes and eye landmarks are mapped through the resulting matrix before
   `computeOverlayPlacement()` creates view-space ear anchors.
4. `CatEarOverlay` draws the current view-space placements.

There is still a fallback to the older `TransformContext` path when CameraX output transforms are
missing. That fallback approximates `PreviewView` as uniform fill-center scaling and front-camera
mirroring. It does not carry CameraX crop rect, sensor rotation, display rotation, or preview output
metadata, so it can be very wrong in landscape.

## Mitigation

1. Prefer no overlay over wrong overlay.
   If the CameraX `CoordinateTransform` is unavailable for live preview, clear the current
   placements instead of using the approximate fallback.
2. Reset smoothing and overlay state when the preview layout size or display rotation changes.
   This avoids interpolating portrait-space coordinates into landscape-space coordinates.
3. Bind camera use cases with the current display rotation.
   `Preview`, `ImageAnalysis`, and `VideoCapture` must be created/updated with the `PreviewView`
   display rotation. The bind-skip guard must include rotation so a same-lens rotation can refresh
   the session.
4. Add bounded debug telemetry.
   Log transform availability, frame rotation, image/view dimensions, and raw/mapped face boxes only
   when the transform state changes. A real-device test can then confirm whether landscape uses the
   CameraX matrix or falls into a no-overlay recovery path.

## Acceptance Criteria

- Rotating portrait to landscape no longer leaves ears far away from the head.
- During rotation, the overlay may disappear briefly while CameraX transforms settle, but it must not
  draw with the unsafe fallback transform.
- Front and rear camera sessions bind with the current display rotation.
- Smoother state is reset on view size or rotation changes.
- Incremental checks pass; full CI passes before commit.
