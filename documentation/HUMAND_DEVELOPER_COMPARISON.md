<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Human Developer Comparison

Current project scope includes:

- Android native app: Kotlin, Compose, CameraX, ML Kit, Hilt
- Camera permission flow, front/rear switching, live preview
- Face detection, coordinate transforms, smoothing, ear anchoring
- Procedural animated ears, 10 styles, expression-reactive behavior
- Capture with overlay compositing, MediaStore save, share sheet
- Version/build stamp, launcher icons, theme, accessibility/status states
- `:domain` JVM module with pure logic
- Unit tests, Kover 95% gate, Spotless, detekt, Android Lint
- Git hooks, SemVer patch automation
- GitHub Actions CI, CodeQL, dependency review, Gitleaks, release workflow
- Optional signing, SBOM generation, release artifacts/checksums
- Extensive documentation/backlog/review/process docs

Best-effort estimate for one seasoned Android developer, working carefully without AI assistance:

- MVP app features: 15-25 person-days
- Camera/ML Kit/overlay math and capture path: 15-25 person-days
- Procedural styles/animation/expression behavior: 10-18 person-days
- Testing, coverage, modularization: 8-14 person-days
- CI/release/security/SBOM/tooling/docs: 10-18 person-days
- Debugging, integration, polish, release hardening: 8-15 person-days

Human-readable total: **roughly 9-14 full-time weeks** for a strong 10-year developer. A realistic midpoint is **about 11 weeks**.
