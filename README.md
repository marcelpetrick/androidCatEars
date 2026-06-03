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

| Concern        | Technology               |
|----------------|--------------------------|
| Language       | Kotlin                   |
| UI             | Jetpack Compose          |
| Camera         | CameraX                  |
| Face detection | ML Kit Face Detection    |
| Build          | Gradle (Kotlin DSL)      |
| Min SDK        | Android 14 (API 34)      |

## License

[GPL v3](LICENSE)

---

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

The patch number is auto-incremented by a git pre-commit hook (set up in task 1.0).
Manual minor/major bumps: edit `version.properties` directly, reset lower fields to `0`.

---

## Package

`it.marcelpetrick.catears`
