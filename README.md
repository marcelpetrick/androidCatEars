# Android Cat Ears 😼🐾

**Author: Marcel Petrick <mail@marcelpetrick.it>**

**Note: project is generated with AI.**

**License: GPLv3 or later. See `LICENSE`.**

---

A real-time augmented reality camera application for Android.
Point the camera at your face and get cat ears — instantly, on-device, no cloud required.

---

## What It Does

- Live camera preview with front/rear camera switching
- On-device face detection via ML Kit
- Real-time 2D cat-ear overlay positioned above the detected face
- Still photo capture with the overlay baked in
- Save to local storage and share via Android sharing intents

## Tech Stack

| Concern        | Technology                      |
|----------------|---------------------------------|
| Language       | Kotlin 2.3.21                   |
| UI             | Jetpack Compose (BOM 2026.05.01)|
| Camera         | CameraX 1.6.1                   |
| Face detection | ML Kit Face Detection 16.1.7    |
| DI             | Hilt 2.59.2                     |
| Build          | Gradle 9.5.1 (Kotlin DSL)       |
| Min SDK        | Android 14 (API 34)             |
| Target SDK     | Android 16 (API 36)             |

## Quality Gate

```bash
# Format (auto-fix)
./gradlew spotlessApply

# Check formatting + static analysis + Android lint in one command
./gradlew qualityCheck

# Coverage
./gradlew :app:koverVerify     # Fails if domain/logic coverage < 95%
./gradlew :app:koverHtmlReport # HTML report → app/build/reports/kover/html/

# Individual tools
./gradlew spotlessCheck   # ktlint via Spotless
./gradlew detekt          # detekt static analysis
./gradlew :app:lint       # Android Lint
```

## License

[GPL v3](LICENSE)

---

## Prerequisites

- JDK 17+ (tested with OpenJDK 26.0.1)
- Android SDK — set `sdk.dir` in `local.properties` (not committed):
  ```properties
  sdk.dir=/path/to/Android/Sdk
  ```
  Required SDK components: `platforms;android-36`, `build-tools;36.0.0`.
  Install via: `sdkmanager "platforms;android-36" "build-tools;36.0.0"`
- `./gradlew` is self-contained (downloads Gradle 9.5.1 on first run).

## Build

```bash
./gradlew assembleDebug
```

Install on a connected device or running emulator:

```bash
./gradlew installDebug
```

## Release Build

```bash
./gradlew assembleRelease
```

Signing configuration must be set up in `local.properties` or via environment variables before a release build will produce a signed APK.

---

## Versioning

The project uses [Semantic Versioning](https://semver.org/). The single source of truth is
`version.properties` in the repository root:

```properties
major=0
minor=1
patch=0
```

`build.gradle.kts` reads this file and exposes `appVersionName` (e.g. `0.1.0`) and
`appVersionCode` to the app module. **Never hardcode version strings elsewhere.**

The patch number is **auto-incremented on every commit** by a git pre-commit hook in `scripts/pre-commit`.

**First-time setup** — install the hook (required once per clone):
```bash
./gradlew installHooks
```

**Manual minor bump** (new backwards-compatible feature):
```bash
# Edit version.properties: increment minor, set patch=0
./gradlew installHooks  # hook is already installed; no-op if re-run
```

**Manual major bump** (breaking change):
```bash
# Edit version.properties: increment major, set minor=0, patch=0
```

Never edit `versionCode` or `versionName` anywhere else — they are derived from `version.properties`.

---

## Package

`it.marcelpetrick.catears`
