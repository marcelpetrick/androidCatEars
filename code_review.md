# Code Review

This review records concrete issues found in the current project state. The app
has no backend, login, or network permission, so several security items are
release, privacy, or supply-chain hardening failures rather than directly
exploitable remote vulnerabilities.

## Security / Privacy / Release Blunders

1. The manual release workflow publishes a debug-signed APK as a user-facing
   download. Debug keys are not a stable release identity and train users to
   install non-release artifacts. **Status: fixed.**
2. The manual release workflow builds `bundleRelease` without wiring
   `RELEASE_*` signing secrets or verifying the result, so it can publish an
   unsigned AAB while the docs imply release signing exists. **Status: fixed.**
3. The manifest does not explicitly disable Android backup. The
   `dataExtractionRules` file excludes data on Android 12+, but the app should
   make the no-backup policy explicit at the manifest level. **Status: fixed.**
4. The project has no dependency vulnerability gate. Dependency updates and CVE
   checks rely on manual review. **Status: fixed.**
5. The project has no CodeQL or equivalent static security analysis workflow.
   **Status: fixed.**
6. The project has no committed secret-scanning workflow. `.gitignore` blocks
   common keystore names, but CI does not detect accidentally committed secrets.
   **Status: fixed.**
7. The app allows screenshots and Recents thumbnails of the live camera preview.
   That is a privacy leak for a camera-first app. **Status: fixed for release builds.**
8. `ImageSaver.save()` returns success even if `Bitmap.compress()` returns
   `false`, which can expose a broken gallery/share URI as if it were a valid
   saved photo. **Status: fixed.**
9. Release docs still describe the AAB as unsigned after signing support landed,
   so operators can ship with a false understanding of artifact security.
   **Status: fixed.**
10. Play Store compliance work is only in backlog. There is no committed privacy
    policy template covering camera processing and gallery writes. **Status:
    fixed.**

## Software Correctness / Architecture Blunders

1. `imageToViewCoordinates()` independently scales X/Y to the whole
   `PreviewView` and ignores CameraX aspect-ratio preservation and crop/fit
   offsets, so overlay placement can be wrong on real screens. **Status: fixed.**
2. `CameraPreview` manually constructs `CameraXControllerImpl` and
   `MlKitFaceDetectorImpl`, bypassing the Hilt/seam architecture promised in the
   docs. **Status: fixed.**
3. `CameraXControllerImpl.capturePhoto()` and the bound `ImageCapture` use case
   are dead code for the current WYSIWYG capture path, increasing camera binding
   complexity without product value. **Status: fixed.**
4. `CaptureState.Success` is obsolete and still describes raw JPEG bytes even
   though the app now saves composited preview bitmaps. **Status: fixed.**
5. `CaptureState.Saved` stores `android.net.Uri` inside the domain package,
   violating the documented framework-free domain boundary. **Status: fixed.**
6. `CameraXControllerImpl` owns executors but never shuts them down, leaking
   threads across activity disposal/recreation. **Status: fixed.**
7. The capture button remains active while a capture/save is already in progress,
   so repeated taps can enqueue overlapping saves and inconsistent UI state.
   **Status: fixed.**
8. `MainViewModel` hardcodes `Dispatchers.IO`, `System.currentTimeMillis()`, and
   random suffix generation, making save behavior harder to test deterministically.
   **Status: fixed.**
9. `app/lint.xml` still suppresses `UnusedResources` with a comment from before
   the cat-ear asset was wired, hiding real resource cleanup problems. **Status:
   fixed.**
10. `TODO.md` and emulator docs are stale relative to `BACKLOG.md` and the local
    webcam-capable emulator setup, so agents get contradictory project state.
    **Status: fixed.**

## CI / Local Gate Findings

1. `release.yml` expects `RELEASE_STORE_FILE` to be a usable file path on the
   GitHub runner. That is not a stable secret model; the workflow should decode
   a base64 keystore secret into a runner-local file before building. **Status:
   fixed.**
2. `ci.yml` duplicates the local gate command-by-command instead of invoking
   `scripts/ci.sh`, so the claimed local/remote parity can drift. **Status:
   fixed.**
3. Workflows do not set job timeouts or concurrency cancellation, so stuck or
   superseded runs can waste minutes and obscure the latest signal.
4. `scripts/ci.sh` exits on failure but does not print which command failed,
   making local failure diagnosis less clear than it should be.
