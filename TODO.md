# TODO — Review Radar

Lightweight, one-line follow-ups from periodic reviews (every ~10 commits).
See [`agents.md`](documentation/agents.md) "Periodic review". Detailed work lives in
[`documentation/BACKLOG.md`](documentation/BACKLOG.md).

## Open

1. Verify live face-tracked overlay on a real device; emulator webcam testing is useful but not final hardware validation.
2. Tune overlay placement constants (width ratio, ear height) against real faces — use WP 17 overlay lab once sample images are available.
3. Task 2.3: upgrade compileSdk/targetSdk 37, Kotlin 2.4.0 + KSP, JUnit 6 once toolchain available — **blocked on SDK 37 release**.
4. Add README screenshots (14.4) once the app is captured on a device or webcam-backed emulator.
5. GitHub Actions release workflow exists (release.yml); verify it runs once pushed to GitHub.
6. WP 18.0: Robolectric host-side tests — **blocked on JDK 26** (Robolectric 4.14.1 / ASM 9.7 only supports up to Java 24 class files). Install JDK 21 and configure Gradle toolchains, or wait for Robolectric with ASM 9.9+.

## Resolved (recent reviews)

- ~~Wire full capture flow~~ DONE (11.2): capture → composite → save → share.
- ~~Version + commit hash at startup~~ DONE (14.3).
- ~~Designed launcher icon~~ DONE (14.0); ~~branded theme + light/dark~~ DONE (14.1).
- ~~States + a11y capture feedback~~ DONE (14.2).
- ~~Release build type / R8~~ DONE (15.0); ~~signing config~~ DONE (15.1); ~~deploy docs~~ DONE (15.2).
- ~~Emulator setup documented~~ DONE (EMULATOR.md).
- ~~WP 16 docs finalisation~~ DONE: README pass, architecture notes, CHANGELOG, and troubleshooting.
- ~~WP 20 animated 3D ears~~ DONE (0.1.76–0.1.82): ear anchoring, procedural Canvas ears, fur strands, perspective X-squash, spring tilt.
- ~~WP 17.0 :domain module extraction~~ DONE (0.1.84): pure domain moved to standalone JVM module.
- ~~WP 17.1 golden-fixture tests~~ DONE (0.1.85): 8 parameterized cases for computeOverlayPlacement.
- ~~WP 18.4 supply chain~~ DONE (0.1.87): Dependabot weekly Gradle + Actions PRs configured.
