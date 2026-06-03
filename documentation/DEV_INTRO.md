# Developer Introduction — Getting Started on Manjaro Linux

This guide covers how to open, build, and run the project from a fresh clone on Manjaro Linux.

---

## IDE: Android Studio

The only fully supported IDE for this stack (Gradle Kotlin DSL, Jetpack Compose, CameraX, Hilt,
KSP). IntelliJ IDEA Ultimate also works, but Android Studio is free and purpose-built for Android.

**Install via AUR:**

```bash
yay -S android-studio
# or:
paru -S android-studio
```

---

## Android SDK

The SDK is installed at `~/Android/Sdk` (set up during initial project bootstrapping — see
[`TOOLING.md`](TOOLING.md) for the full log).

On first Android Studio launch the setup wizard may offer to download a fresh SDK — **skip it**.
Instead, configure the existing location:

**File → Settings → Appearance & Behavior → System Settings → Android SDK**
→ set SDK location to `/home/mpetrick/Android/Sdk`

---

## Opening the project

**File → Open** → select the root directory:

```
/home/mpetrick/repos/androidCatEars
```

Open the **root directory** — not a subdirectory, not a single file. Android Studio detects
`settings.gradle.kts` and imports the Gradle project automatically.

Accept the Gradle sync when prompted. First sync downloads all dependencies (~500 MB); subsequent
syncs are fast.

---

## First-time setup after opening

Run once in the terminal (**Alt+F12** inside Android Studio):

```bash
./gradlew installHooks
```

This installs the pre-commit hook that auto-increments the patch version on every commit.
Without it, the version bump is skipped and the build will still work, but version tracking
breaks.

---

## Build and run

```bash
# Build a debug APK
./gradlew assembleDebug

# Build and install on a connected device or running emulator
./gradlew installDebug

# Full local CI gate (format + static analysis + lint + tests + coverage)
./scripts/ci.sh
```

Or use the IDE toolbar: **Run → Run 'app'** (`Shift+F10`).

---

## Key file locations

| What | Path |
|------|------|
| App entry point | `app/src/main/java/it/marcelpetrick/catears/CatEarsApplication.kt` |
| Main activity | `app/src/main/java/it/marcelpetrick/catears/ui/MainActivity.kt` |
| All app source | `app/src/main/java/it/marcelpetrick/catears/` |
| Unit tests | `app/src/test/java/it/marcelpetrick/catears/` |
| App build config | `app/build.gradle.kts` |
| All dependency versions | `gradle/libs.versions.toml` |
| App version | `version.properties` |
| Project backlog | `documentation/BACKLOG.md` |
| Architecture & decisions | `documentation/DEVELOPMENT_PLAN.md` |
| Agent / contributor rules | `documentation/agents.md` |
| Tooling install log | `documentation/TOOLING.md` |

---

## Useful IDE shortcuts

| Action | Shortcut |
|--------|----------|
| Project file tree | `Alt+1` |
| Build output | `Alt+0` |
| Logcat (device logs) | `Alt+6` |
| Terminal | `Alt+F12` |
| Run app | `Shift+F10` |
| Find any file | `Shift` `Shift` (double-tap) |
| Find in files | `Ctrl+Shift+F` |
| Go to declaration | `Ctrl+B` |
| Reformat file | `Ctrl+Alt+L` |

---

## Quality gate commands

Run these from the project root before committing or pushing:

```bash
# Auto-format all source files
./gradlew spotlessApply

# Full quality gate (mirrors CI exactly)
./scripts/ci.sh

# Individual tools
./gradlew spotlessCheck     # formatting
./gradlew detekt            # static analysis
./gradlew :app:lint         # Android lint
./gradlew :app:test         # unit tests
./gradlew :app:koverVerify  # coverage gate (≥ 95% on domain/logic)
```

---

## Versioning

The version is in `version.properties` (single source of truth). The pre-commit hook bumps
`patch` automatically on every commit. For manual bumps:

- **Minor** (new feature): increment `minor`, set `patch=0`
- **Major** (breaking change): increment `major`, set `minor=0`, `patch=0`

Never edit `versionName` or `versionCode` elsewhere — they are derived from `version.properties`
by `build.gradle.kts`.

---

## Further reading

- [`VISION.md`](VISION.md) — what the app is and why
- [`DEVELOPMENT_PLAN.md`](DEVELOPMENT_PLAN.md) — architecture decisions and milestones
- [`BACKLOG.md`](BACKLOG.md) — current task list and status
- [`agents.md`](agents.md) — contributor and agent rules (commit format, CI requirements, etc.)
