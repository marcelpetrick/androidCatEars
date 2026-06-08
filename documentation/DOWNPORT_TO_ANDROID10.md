# Downport to Android 10

This note estimates what would have to change to let androidCatEars install and
run on Android 10 phones.

Android 10 is API 29. The current app is configured for:

- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 34`

So the product currently requires Android 14 or newer. A true Android 10
downport means lowering only `minSdk` from 34 to 29 while keeping
`compileSdk`/`targetSdk` modern for Play compliance.

## Short Answer

This is probably a small-to-medium change, not a rewrite.

Estimated implementation effort: 40-120 changed lines of code.

Estimated validation effort: 1-2 days if an Android 10 physical device is
available, 2-4 days if validation depends on an emulator plus later physical
device confirmation.

The big risk is not Kotlin code volume. The risk is device behavior: older
camera HALs, weaker CPUs/GPUs, OEM MediaStore behavior, and CameraX/ML Kit
performance on real Android 10 hardware.

## Required Code Changes

| Area | Expected change | Estimated changed LOC |
| --- | --- | ---: |
| Gradle SDK config | Change `minSdk = 34` to `minSdk = 29`. | 1 |
| Lint config | Remove or adjust the `ObsoleteSdkInt` suppression because checks below API 34 become meaningful again. | 5-15 |
| Manifest backup metadata | Keep `dataExtractionRules`; optionally add an API-aware `fullBackupContent` rule if backup behavior should be explicit below Android 12. | 0-10 |
| Runtime API guards | Audit all Android framework calls and keep guards such as `Build.VERSION.SDK_INT >= S` for dynamic color. Add guards if lint finds any API 30+ calls. | 5-30 |
| MediaStore image save | Current still-photo save path already uses Android 10 scoped storage (`RELATIVE_PATH`, `IS_PENDING`) and should work on API 29. Mainly test and add comments if needed. | 0-10 |
| MediaStore video save | Current `MediaStoreOutputOptions` path should work on API 29, but needs real-device validation for file visibility and finalization. | 0-15 |
| CameraX effects/video overlay | Verify `OverlayEffect`, `SurfaceProcessor`, and video overlay behavior on API 29. Add fallback if the effect pipeline is unsupported or unstable on old hardware. | 10-40 |
| Tests/docs | Add an Android 10 compatibility note and, if possible, an emulator/device smoke-test checklist. | 15-30 |

Likely total: 40-120 changed LOC.

Worst-case if CameraX video overlay is unreliable on Android 10 hardware:
150-300 changed LOC, because the app may need a capability gate that disables
baked-in video ears while keeping preview, photos, and plain video stable.

## Feature-by-Feature Impact

### Live Camera Preview

CameraX itself is intended to support older Android versions than API 29, so
the preview pipeline should not need a redesign.

Expected work: mostly validation.

Risk: medium. Old phones often have weaker camera HAL behavior. Front/rear
switching, lifecycle rebinding, and preview sizing need real-device testing.

### ML Kit Face Detection

The on-device ML Kit face detector should run on Android 10, but performance
may be worse on an old CPU.

Expected work: no code change unless performance is poor.

Risk: medium. If frame processing lags, reduce analysis resolution, throttle
analysis, or skip frames more aggressively.

### Still Photo Capture

This is the easiest part of the downport. Android 10 introduced scoped storage,
and the app already saves via `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`
with `RELATIVE_PATH` and `IS_PENDING`.

Expected work: no major code change.

Risk: low-to-medium. Test that saved photos appear in the gallery and that
sharing the returned URI works on the old phone.

### Five-Second Video Capture

The CameraX `VideoCapture` flow should be compatible in principle, but this is
more sensitive than still photos.

Expected work: validation first, small fixes if MediaStore finalization behaves
differently.

Risk: medium. Test start/stop sounds, auto-stop, early stop, URI return, gallery
visibility, and share sheet behavior.

### Baked-In Video Ears

This is the highest-risk feature. The overlay path relies on CameraX effects and
surface processing. API 29 is not automatically a blocker, but older devices may
have weaker GPU/encoder combinations.

Expected work: validation first.

Fallback option: if the effect path is unstable, gate baked-in video ears behind
a runtime capability check and keep still-photo capture fully supported.

Risk: medium-to-high.

### Party Mode and Multi-Face Tracking

This is mostly domain/UI logic and should not care about Android 10.

Expected work: no code change.

Risk: low, except for performance when tracking several faces on old hardware.

### Shutter and Recording Sounds

`MediaActionSound` exists far below API 29, so Android 10 does not require a
downport-specific change.

Expected work: no code change.

Risk: low. Device/OEM audio policy can still make it silent, but that is not an
Android 10 compatibility issue.

## Dependency Risks

The current dependency set is modern:

- AGP 9.x
- Kotlin 2.3.x
- Compose BOM 2026.05
- CameraX 1.6.x
- AndroidX Core 1.18.x
- Lifecycle 2.10.x
- Activity Compose 1.13.x
- ML Kit Face Detection 16.1.7

Before changing `minSdk`, run `./gradlew :app:checkDebugAarMetadata`. If any
dependency declares `minCompileSdk`/`minSdk` constraints above API 29, the
downport becomes a dependency-version task instead of a one-line Gradle change.

Expected risk: low-to-medium. Most of these libraries historically support API
29 or lower, but the current future-dated versions should be verified by Gradle,
not assumed.

## Testing Matrix

Minimum acceptable Android 10 validation:

1. App installs on API 29.
2. Camera permission flow works.
3. Preview opens on front and rear cameras.
4. Face detection places ears correctly.
5. Style switching works and only sprite-backed photorealistic styles are exposed.
6. Party Mode assigns stable styles and re-rolls.
7. Still photo saves to gallery and shares.
8. Five-second video records, auto-stops, appears in gallery, and shares.
9. Baked-in video ears are present, or the feature is clearly disabled on that
   device with no crash.
10. Lifecycle survives background/foreground and lens switching.

Recommended devices:

- API 29 emulator for first pass.
- One real Android 10 phone for camera/video truth.
- If possible, one low-end Android 10 phone to expose performance issues.

## Product Implications

Supporting Android 10 increases the install base and makes the app usable on
older phones, but it also raises QA cost. Camera apps age badly across OEMs:
every extra Android generation increases the number of camera HAL, encoder, and
gallery behaviors the app must tolerate.

The most pragmatic product position would be:

- Support Android 10+ for live preview, photos, Party Mode, and sharing.
- Treat baked-in video ears as best-effort on Android 10 until validated on real
  hardware, especially across older camera HAL and encoder combinations.
- Keep target SDK modern so Play requirements stay satisfied.

## Recommendation

Do the downport as a short spike first:

1. Change `minSdk` to 29.
2. Run `./scripts/ci.sh`.
3. Install on an API 29 emulator.
4. Install on a real Android 10 phone.
5. Decide whether baked-in video ears are supported, gated, or disabled on API
   29 based on observed behavior.

If the spike passes, the production downport is likely under 120 changed LOC. If
video effects fail on real hardware, budget up to 300 changed LOC for capability
gating and user-facing fallback behavior.
