# Tooling Log

A traceable record of every tool, SDK, or dependency installed (or system change made) while working
on this project. See [`agents.md`](agents.md) "Local Environment & Tooling" for the rules.

Append one row per install. Prefer **project-local** scope; flag anything system-wide.

| Date | Tool | Version | Purpose | How installed | Scope (project / system) |
|------|------|---------|---------|---------------|--------------------------|
| 2026-06-03 | OpenJDK | 26.0.1 | JVM for running Gradle and building the project | Pre-installed on host (Manjaro) | system |
| 2026-06-03 | Gradle | 9.5.1 | Build system; bootstrapped via `gradle wrapper`; self-managed by wrapper at `~/.gradle/wrapper/dists` | Bootstrapped via temporary download of `gradle-9.5.1-bin.zip` to `/tmp` (deleted after wrapper generation); subsequent runs use `./gradlew` self-download | project (wrapper in repo, distribution cached in `~/.gradle`) |
| 2026-06-03 | Android SDK Command-Line Tools | 14742923 (latest) | Android SDK manager; installs platforms and build-tools | Downloaded `commandlinetools-linux-14742923_latest.zip` from dl.google.com; extracted to `~/Android/Sdk/cmdline-tools/latest` | system (`~/Android/Sdk`) |
| 2026-06-03 | Android SDK Platform | android-36 (API 36) | Compile target for the app (compileSdk 36 / Android 16) | Installed via `sdkmanager "platforms;android-36"` | system (`~/Android/Sdk/platforms`) |
| 2026-06-03 | Android SDK Build-Tools | 36.0.0 | APK packaging, aapt2, d8/r8 | Installed via `sdkmanager "build-tools;36.0.0"` | system (`~/Android/Sdk/build-tools`) |
| 2026-06-03 | Android SDK Platform-Tools | 37.0.0 | adb and related tools; auto-installed by Gradle during first build | Auto-downloaded by AGP during first `assembleDebug` | system (`~/Android/Sdk/platform-tools`) |
