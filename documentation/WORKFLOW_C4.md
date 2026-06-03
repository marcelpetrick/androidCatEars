# Cat Ears Camera — Architecture & Workflow (C4 Model)

**Author:** Marcel Petrick \<mail@marcelpetrick.it\>
**License:** GPL-3.0-or-later
**Reference:** [C4 Model](https://c4model.com/) — Context → Container → Component → Code

---

## Level 1 — System Context

Who uses the system and what surrounds it.

```
                         ┌──────────────────────────────────┐
                         │         Cat Ears Camera           │
                         │  (Android application, on-device) │
                         └───┬──────────────────────────┬───┘
                             │                          │
             requests frames │                          │ writes / shares
                             ▼                          ▼
                  ┌─────────────────┐        ┌──────────────────────┐
                  │  Android Camera │        │  Android OS services  │
                  │  HAL / CameraX  │        │  MediaStore (gallery) │
                  │  (device HW)    │        │  Share Sheet (intents)│
                  └─────────────────┘        └──────────────────────┘

  ┌────────┐  opens app, sees preview,
  │  User  │  taps capture / share
  └────────┘
                  ┌────────────────────┐
                  │  ML Kit            │
                  │  Face Detection    │
                  │  (bundled in APK,  │
                  │   zero network)    │
                  └────────────────────┘
```

**Key constraints**

- All processing is **on-device** — no cloud, no server, no telemetry.
- No internet permission is declared.
- Storage access via scoped MediaStore (no `WRITE_EXTERNAL_STORAGE`).

---

## Level 2 — Container

One deployable unit: a single Android APK.

```
┌──────────────────────────────────────────────────────────────┐
│  androidCatEars.apk                                          │
│  Package: it.marcelpetrick.catears                           │
│  Min SDK: 34 (Android 14)  Compile SDK: 36 (Android 16)     │
│  Language: Kotlin 2.3.21   UI: Jetpack Compose (BOM 2026.05) │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  UI Layer  (it.marcelpetrick.catears.ui.*)           │   │
│  │  Compose screens + ViewModels + state                │   │
│  └───────────────────────┬──────────────────────────────┘   │
│                          │ reads/writes StateFlow            │
│  ┌───────────────────────▼──────────────────────────────┐   │
│  │  Domain Layer  (it.marcelpetrick.catears.domain.*)   │   │
│  │  Pure Kotlin — no Android/framework imports          │   │
│  │  FaceModel · BoundingBox · Point2D                   │   │
│  │  LensSelector · PermissionState · Version            │   │
│  │  imageToViewCoordinates · imageToViewBoundingBox      │   │
│  └───────────────────────┬──────────────────────────────┘   │
│                          │ interfaces only                   │
│  ┌───────────┬───────────┴──────────┬──────────────────┐    │
│  │  camera/  │      facedetect/     │  overlay/        │    │
│  │  CameraX  │      ML Kit          │  (WP 10+)        │    │
│  │  wrapper  │      face detect     │  canvas + ears   │    │
│  └───────────┴──────────────────────┴──────────────────┘    │
│                                                              │
│  ┌────────────────┐   ┌────────────────────────────────┐    │
│  │  capture/      │   │  share/                        │    │
│  │  (WP 11+)      │   │  (WP 13+)                      │    │
│  │  bitmap compo  │   │  FileProvider + share intent   │    │
│  └────────────────┘   └────────────────────────────────┘    │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  di/  — Hilt AppModule (Hilt 2.59.2 / KSP 2.3.9)     │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

## Level 3 — Component

How the packages fit together and what each is responsible for.

```
it.marcelpetrick.catears

├── ui/
│   ├── MainActivity              @AndroidEntryPoint; permission launcher;
│   │                             collectAsStateWithLifecycle; routes to MainScreen
│   ├── MainViewModel             @HiltViewModel; exposes uiState: StateFlow<MainUiState>
│   │                             and lens: StateFlow<LensSelector>
│   ├── MainScreen                stateless Composable; switches on MainUiState
│   ├── MainUiState               sealed interface: Initialising | PermissionRequired |
│   │                             PermissionPermanentlyDenied | Ready
│   └── theme/                    CatEarsTheme (Material3 + dynamic colour)
│
├── domain/                       ★ NO Android imports — fully unit-testable on JVM
│   ├── FaceModel                 data class: BoundingBox + eye landmarks + euler roll
│   ├── BoundingBox               left/top/right/bottom + derived width/height/center
│   ├── Point2D                   (x, y) in pixels
│   ├── LensSelector              enum: Front | Rear; toggled()
│   ├── PermissionState           sealed: Unknown | Granted | Denied | PermanentlyDenied
│   ├── CoordinateTransform       imageToViewCoordinates(); imageToViewBoundingBox()
│   │                             — scale + front-camera mirror
│   └── Version                   version parsing and comparison
│
├── camera/
│   ├── CameraControllerSeam      interface: bindPreview(lens); unbind(); switchLens()
│   ├── CameraXControllerImpl     ProcessCameraProvider; Preview use case; lifecycle bind
│   └── CameraPreviewComposable   AndroidView wrapping PreviewView; binds on composition
│
├── facedetect/
│   ├── FaceDetectorSeam          interface: process(ImageProxy, onResult); close()
│   └── MlKitFaceDetectorImpl     ML Kit FaceDetector (FAST mode, all landmarks);
│                                 picks largest face; maps to FaceModel
│
├── overlay/          (WP 10 — not yet implemented)
│   ├── OverlayPlacer             placement math: anchor + scale from face bbox
│   └── CatEarOverlay             Compose Canvas drawing ear assets
│
├── capture/          (WP 11 — not yet implemented)
│   └── CaptureUseCase            Compose → Bitmap compositing
│
├── share/            (WP 13 — not yet implemented)
│   └── ShareIntentBuilder        FileProvider URI + ACTION_SEND intent
│
└── di/
    └── AppModule                 @InstallIn(SingletonComponent); bindings added per WP
```

---

## Level 4 — Code: Core Workflows

### Workflow A — Get frame → detect face → place ears (real-time loop)

This is the hot path that runs on every camera frame (~30 fps).

```
Android Camera HAL
        │  YUV_420_888 frame
        ▼
CameraX ImageAnalysis use case
        │  ImageProxy
        ▼
FaceDetectorSeam.process()
  └─► MlKitFaceDetectorImpl
        │  InputImage (rotation-corrected)
        │  ML Kit detector: PERFORMANCE_MODE_FAST, LANDMARK_MODE_ALL
        │  minimum face size: 15 % of frame
        │  if multiple faces → pick largest by bounding box area
        │
        │  FaceModel (image-space coordinates)
        │    boundingBox: BoundingBox
        │    leftEyePosition: Point2D?
        │    rightEyePosition: Point2D?
        │    headEulerAngleZ: Float (head roll in degrees)
        ▼
imageToViewBoundingBox()               ← domain, unit-tested
  scale x: viewWidth  / imageWidth
  scale y: viewHeight / imageHeight
  mirror x if front camera: viewWidth - scaledX
        │
        │  FaceModel (view-space coordinates)
        ▼
OverlayPlacer.computePlacement()       ← domain, unit-tested (WP 10.1)
  ear anchor = top-center of bounding box
  ear scale  = face bounding box width
  ear rotation = headEulerAngleZ
        │
        │  EarPlacement(offsetX, offsetY, scale, rotationDeg)
        ▼
CatEarOverlay (Compose Canvas)         ← WP 10.2
  drawImage(leftEarAsset,  placement.left)
  drawImage(rightEarAsset, placement.right)
  — layer sits on top of CameraPreview composable
        │
        ▼
      Screen  (user sees live cat ears tracking their face)
```

### Workflow B — Capture → save → share

Triggered by a single user tap on the capture button.

```
User taps "Capture"
        │
        ▼
CaptureUseCase                          ← WP 11 (not yet implemented)
  renders current Compose tree including overlay into offscreen Bitmap
        │  Bitmap (camera frame + ears composited)
        ▼
ImageSaver                              ← WP 12
  filename: catears_<timestamp>.jpg
  inserts via MediaStore.Images.Media
  no WRITE_EXTERNAL_STORAGE needed (scoped storage, API 34+)
        │  content:// Uri
        ▼
ShareIntentBuilder                      ← WP 13
  FileProvider wraps URI (authorities: it.marcelpetrick.catears.fileprovider)
  Intent(ACTION_SEND, type = "image/jpeg")
        │
        ▼
Android Share Sheet  →  messaging / email / social / AirDrop
```

---

## State Machine — Permission & App Lifecycle

```
                  app launch
                      │
                      ▼
              ┌───────────────┐
              │  Initialising  │  (spinner shown)
              └───────┬───────┘
                      │ LaunchedEffect fires permission request
          ┌───────────┴───────────────────────┐
          │ granted = true                    │ granted = false
          ▼                                   ▼
      ┌───────┐              ┌─────────────────────────────┐
      │ Ready │              │ showRationale = true?        │
      └───┬───┘              └──────────┬───────────────────┘
          │                    yes      │    no
          │              ┌─────────────┘    └──────────────────┐
          │              ▼                                      ▼
          │   ┌────────────────────┐          ┌─────────────────────────────┐
          │   │ PermissionRequired │          │ PermissionPermanentlyDenied  │
          │   │ "Grant permission" │          │ "Open settings" button       │
          │   │  button shown      │          │ → ACTION_APP_SETTINGS        │
          │   └──────────┬─────────┘          └─────────────────────────────┘
          │     user     │ grants
          └──────────────┘
                 Ready
                   │
             ┌─────▼──────────────────────────────────────────┐
             │  CameraPreview (full-screen PreviewView)        │
             │  + CatEarOverlay (Canvas, tracks face in loop)  │
             │  + camera-switch FAB (bottom-right)             │
             └────────────────────────────────────────────────┘
```

---

## Data Flow: Coordinate Spaces

Three coordinate spaces are traversed per frame.

```
  Image space                View space              Screen space
  ┌────────────┐             ┌────────────┐          ┌────────────┐
  │ (0,0)      │  scale x    │ (0,0)      │  compose │            │
  │            │  scale y    │            │  layout  │            │
  │  W × H px  │  mirror x   │  vW × vH   │  ──────► │  display   │
  │  (camera   │  (front cam)│  (preview  │          │            │
  │   buffer)  │  ─────────► │   view)    │          │            │
  └────────────┘             └────────────┘          └────────────┘
  ML Kit output              domain transform         Compose draw
  FaceModel coords           imageToViewBoundingBox   CatEarOverlay
```

Front camera images are delivered by CameraX with the left/right axis flipped relative to what the user sees on screen. `imageToViewBoundingBox` un-mirrors the X axis so overlay positions match the preview.

---

## Technology Stack

| Concern | Library | Version |
|---------|---------|---------|
| Language | Kotlin | 2.3.21 |
| UI | Jetpack Compose BOM | 2026.05.01 |
| Camera | CameraX | 1.6.1 |
| Face detection | ML Kit Face Detection | 16.1.7 |
| Dependency injection | Hilt | 2.59.2 |
| Build system | Gradle (Kotlin DSL) | 9.5.1 |
| Android min SDK | API 34 (Android 14) | — |
| Android compile SDK | API 36 (Android 16) | — |
| License | GPLv3 | — |

---

## What Is Not in Scope (MVP)

The following appear in `VISION.md` "Future Ideas" and are explicitly deferred:

- Video recording with overlay
- Multiple simultaneous face tracking
- Custom AI models (ONNX Runtime, TensorFlow Lite)
- Animated overlays or expression reactions
- Overlay marketplace / downloadable packs
- Social publishing features
- Desktop simulation tool (Linux JVM utility)

---

## Related Documents

| Document | Purpose |
|----------|---------|
| [`VISION.md`](VISION.md) | Product vision, decisions, Q&A, future ideas |
| [`DEVELOPMENT_PLAN.md`](DEVELOPMENT_PLAN.md) | Strategic plan, architecture decisions (Q1–Q6), milestones |
| [`BACKLOG.md`](BACKLOG.md) | Atomic task backlog with status tracking |
| [`agents.md`](agents.md) | Agent rules: commits, versioning, execution loop |
| [`DEV_INTRO.md`](DEV_INTRO.md) | Developer setup guide (IDE, SDK, build) |
| [`TOOLING.md`](TOOLING.md) | Installed tools log |
| [`SPDX.md`](SPDX.md) | License header convention |
