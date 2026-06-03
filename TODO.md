# TODO — Review Radar

Lightweight, one-line follow-ups from periodic reviews (every ~10 commits).
See [`agents.md`](documentation/agents.md) "Periodic review". Detailed work lives in
[`documentation/BACKLOG.md`](documentation/BACKLOG.md).

## Open

1. WP 16 docs finalisation is the main remaining device-free work: README pass (16.0), architecture notes (16.1), CHANGELOG + troubleshooting (16.2).
2. Verify live face-tracked overlay on a real device (emulator virtual camera has no real face) — **needs hardware**.
3. Tune overlay placement constants (width ratio, ear height) against real faces — **needs hardware**.
4. Task 2.3: upgrade compileSdk/targetSdk 37, Kotlin 2.4.0 + KSP, JUnit 6 once toolchain available — **blocked on SDK 37 release**.
5. Add README screenshots (14.4) once the app is captured on a device — **needs hardware**.
6. GitHub Actions release workflow exists (release.yml); verify it runs once pushed to GitHub.
7. Consider instrumented (androidTest) coverage for camera/ML Kit paths currently excluded from Kover.
8. Verify the GitHub Actions release workflow signs using the `RELEASE_*` env secrets (wired in 15.1).

## Resolved (recent reviews)

- ~~Wire full capture flow~~ DONE (11.2): capture → composite → save → share.
- ~~Version + commit hash at startup~~ DONE (14.3).
- ~~Designed launcher icon~~ DONE (14.0); ~~branded theme + light/dark~~ DONE (14.1).
- ~~States + a11y capture feedback~~ DONE (14.2).
- ~~Release build type / R8~~ DONE (15.0); ~~signing config~~ DONE (15.1); ~~deploy docs~~ DONE (15.2).
- ~~Emulator setup documented~~ DONE (EMULATOR.md).
