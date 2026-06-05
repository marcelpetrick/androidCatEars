# Source Review 2026-06-05

Whole-app source review after the video-overlay and landscape-placement fixes.

Severity scale: **CRITICAL** (normal user flow can break recording/camera correctness or crash) ·
**HIGH** (visible feature bug or high regression risk) · **MEDIUM** (performance, maintainability, or
edge-case reliability) · **LOW** (polish or hardening).

## Top Ten Findings

1. **CRITICAL — `AndroidView.update` rebinds CameraX on ordinary recomposition**
   - File: `app/src/main/java/it/marcelpetrick/catears/camera/CameraPreviewComposable.kt`
   - `update = { controller.bindPreview(lens) }` can run for recompositions that are not lens
     changes. `bindPreview()` calls `provider.unbindAll()`, rebuilds the video use case and overlay
     effect, and can interrupt preview or recording during unrelated state updates.
   - Status: fixed in this pass. `CameraXControllerImpl` now tracks the bound lens and returns early
     when recomposition asks for the already-bound lens.

2. **CRITICAL — Lens switching can unbind an active video recording**
   - Files: `CameraXControllerImpl.kt`, `MainViewModel.kt`, `MainScreen.kt`
   - `bindPreview()` has no active-recording guard, and the UI/ViewModel allow lens toggles while
     `RecordingState.Recording`. A camera switch during a 5 s clip can detach `VideoCapture` and
     produce a partial or failed clip.
   - Status: fixed in this pass. `MainViewModel` ignores lens toggles while recording, and
     `CameraXControllerImpl` defensively refuses rebinds while `activeRecording` is non-null.

3. **CRITICAL — Posted face callbacks can outlive camera teardown**
   - File: `CameraPreviewComposable.kt`
   - Face results are posted to `PreviewView` so transform work runs on the view thread. If the
     composable is disposed or the camera is rebound before the post runs, stale callbacks can still
     update overlay state.
   - Status: fixed in this pass. Posted face callbacks now check a disposed flag before touching
     overlay state, and teardown clears overlay placements/video overlay state.

4. **HIGH — Face smoothing is not reset on lens changes**
   - File: `CameraPreviewComposable.kt`
   - `MultiFaceSmoother` survives front/rear switches, so the first placements from the new camera
     can interpolate from the old camera's face positions and visibly jump.
   - Status: fixed in this pass. Lens changes reset `MultiFaceSmoother` and clear current overlay
     placements before new camera results arrive.

5. **HIGH — Video overlay still maps view-space placements to buffer space with custom math**
   - File: `CameraXControllerImpl.kt`
   - Live preview now uses CameraX output transforms, but recorded video still uses a handwritten
     `viewToBufferMatrix()`. This is a regression risk for devices with unusual crop/rotation output.
   - Status: fixed in this pass for the known crop/rotation risk. The video overlay now maps into
     CameraX's per-frame crop rect instead of assuming the whole buffer is visible. A later renderer
     consolidation can still replace the view-space video path entirely if CameraX exposes a direct
     source/target transform for `OverlayEffect` frames.

6. **MEDIUM — Captured preview bitmaps are never recycled**
   - Files: `CameraPreviewComposable.kt`, `MainViewModel.kt`, `ImageSaver.kt`
   - `PreviewView.bitmap` allocates native memory. The save path does not recycle the original
     preview bitmap or the composited copy after MediaStore write completes.
   - Status: fixed in this pass. The ViewModel now recycles the saved bitmap after the MediaStore
     write finishes, and the capture compositor recycles the original preview frame after producing
     a separate composited bitmap.

7. **MEDIUM — Still/video save paths do not catch non-IO MediaStore failures**
   - Files: `ImageSaver.kt`, `CameraXControllerImpl.kt`
   - `ImageSaver.writeBitmap()` catches `IOException` only; `insert`, `openOutputStream`, `compress`,
     and MediaStore updates can also throw `SecurityException`, `IllegalStateException`, or other
     runtime storage exceptions. Video `prepareRecording()` is now guarded in this pass and reports
     failure to the UI.
   - Status: fixed in this pass for still images. `ImageSaver` now catches MediaStore runtime
     failures around row creation and writing, and cleanup of pending rows is best-effort.

8. **MEDIUM — Live overlay and canvas compositor duplicate renderer geometry**
   - Files: `CatEarOverlay.kt`, `OverlayCompositor.kt`
   - Constants and material geometry are duplicated across Compose and Canvas renderers. Sprite-backed
     styles reduce the visible risk, but fallback paths can drift again.
   - Follow-up: extract shared renderer geometry/spec helpers into a common layer.

9. **MEDIUM — Sprite bitmap caches are unbounded and not lifecycle-aware**
   - Files: `CatEarOverlay.kt`, `OverlayCompositor.kt`
   - Decode caches keep bitmaps for all styles for the process lifetime and do not respond to memory
     pressure. The current asset set is small, but future high-resolution sprites can become costly.
   - Follow-up: use size-bounded/lifecycle-aware bitmap caching or Android resource decoding helpers.

10. **LOW — Help language selection is not saved across dialog recreation**
    - File: `HelpDialog.kt`
    - The selected help language uses `remember`, not `rememberSaveable`. Rotation or process
      recreation inside the dialog resets to English.
    - Follow-up: use `rememberSaveable` for the selected `HelpLanguage`.

## Critical Fix Plan

1. DONE — Prevent redundant CameraX rebinds and active-recording rebinds.
2. DONE — Block lens toggles while recording.
3. DONE — Guard posted face callbacks after dispose and clear overlay/smoothing on lens changes.
4. DONE — Fix the party-mode replacement slot collision from the older review list.
5. DONE — Recycle captured bitmaps and harden still-image MediaStore failure handling.
6. DONE — Full quality gate passed before commit.
