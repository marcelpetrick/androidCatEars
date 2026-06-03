# TODO — Review Radar

Lightweight, one-line follow-ups from periodic reviews (every ~10 commits).
See [`agents.md`](documentation/agents.md) "Periodic review". Detailed work lives in
[`documentation/BACKLOG.md`](documentation/BACKLOG.md).

## Open

1. ~~Wire full capture flow~~ DONE (task 11.2): capture → composite → save → share.
2. Verify live face-tracked overlay on a real device (emulator virtual camera has no real face).
3. Tune overlay placement constants (width ratio, ear height) against real faces.
4. Task 2.3: upgrade compileSdk/targetSdk 37, Kotlin 2.4.0 + KSP, JUnit 6 once toolchain available.
5. ~~Version + commit hash at startup~~ DONE (task 14.3).
6. GitHub Actions release workflow created (release.yml); verify it runs once pushed to GitHub.
7. Consider instrumented (androidTest) coverage for camera/ML Kit paths currently excluded from Kover.
8. Release signing config (WP 15) before any public APK distribution.
9. Add app icon / final artwork (replace placeholder ic_cat_ears + launcher icon) — WP 14.
10. Confirm KVM/VirtualBox emulator setup documented well enough for fast local runs (EMULATOR.md).
