<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Human Developer Comparison

Snapshot reviewed: **version 0.1.206**.

Current repository size reviewed:

- 22 Android app source files.
- 14 pure `:domain` source files.
- 17 JVM unit-test files.
- About 5,300 lines across app, domain, and test Kotlin.
- 5 GitHub Actions workflows.
- 24 Markdown documents under `documentation/`.

Current project scope includes:

- Native Android app in Kotlin with Jetpack Compose, CameraX, ML Kit Face Detection, Hilt, and Gradle Kotlin DSL.
- Camera permission flow, front/rear switching, full-screen live preview, lifecycle-safe CameraX binding, and user-facing failure/status states.
- On-device face detection with transformed face boxes, landmarks, head pose, expression probabilities, coordinate mirroring, multi-face tracking, and smoothed placements.
- Photorealistic sprite-backed live overlay: 6 cat/fox ear styles, per-ear anchors, perspective squash, head-roll spring tilt, expression reactions, Party Mode, and up to 4 faces.
- WYSIWYG still capture: preview-frame grab, sprite overlay compositing, style propagation into saved images, MediaStore save, bitmap cleanup, gallery URI handling, and Android share sheet.
- Fixed 5-second video recording with baked-in ears: CameraX `VideoCapture`, `OverlayEffect` frame overlay, MediaStore MP4 output, start/stop state, record/share UI, and recording domain/ViewModel tests.
- In-app Help/About dialog with English, German, Croatian, and Mandarin content, runtime language switcher, author/repo/feedback links, feature overview, and how-to-use copy backed by pure domain tests.
- Product polish: app name, launcher icon, theme, light/dark handling, haptic capture feedback, version + git commit stamp, accessibility labels, and regression fixes for ear head attachment and saved tint mismatch.
- `:domain` JVM module with pure models and logic for permissions, lens selection, file naming, coordinate transforms, overlay placement, smoothing, ear styles, residual tint metadata, help content, recording state, and version parsing.
- Quality tooling: JUnit 5, MockK, Turbine, Kover 95% domain/logic gate, Spotless, detekt, Android Lint, and warning-free current domain test compilation.
- Local automation: `scripts/ci.sh` with ASCII wall-clock summary, pre-commit SemVer patch bump, SBOM generation script, and documented local quality gate.
- GitHub automation: CI, CodeQL, dependency review, Gitleaks, Dependabot, and manual release workflow with optional signing, AAB/debug APK artifacts, CycloneDX SBOMs, SHA-256 checksums, and release documentation.
- Security/release hardening: GPL/SPDX conventions, privacy policy, Play Store notes, release checklist, optional secret-based signing, secret scanning, dependency review, SBOM documentation, and code-review findings.
- Documentation system: architecture, development plan, backlog, agent guidelines, workflow, tooling, emulator, deploy, troubleshooting, release, SBOM, GitHub Actions, Play Store, animated ears, ear styles, overlay lab, project review, and review radar.

Best-effort estimate for one seasoned Android developer with about 10 years of experience, working carefully without AI assistance:

- Foundations, Gradle setup, app skeleton, Hilt, theme, permissions, basic Compose shell: 7-12 person-days.
- CameraX preview, front/rear rebinding, ML Kit analysis pipeline, lifecycle cleanup, and device debugging: 10-18 person-days.
- Coordinate transforms, face model, multi-face smoothing, ear anchoring, regression tuning against real screenshots: 8-14 person-days.
- Procedural-to-sprite ear renderer evolution, style variants, expression/head-pose placement, Party Mode assignments, and capture/video renderer parity: 18-32 person-days.
- Still capture, MediaStore save, share integration, WYSIWYG compositor, style propagation, bitmap cleanup, and user feedback: 8-13 person-days.
- Five-second video recording, baked-in `OverlayEffect` overlay, crop/rotation fixes, and share path: 8-14 person-days.
- Help/About dialog with four languages and testable content model: 4-7 person-days.
- Domain extraction, unit tests, coverage gate, warning cleanup, fixture tests, and regression tests: 10-18 person-days.
- CI, local CI script, GitHub workflows, CodeQL, dependency review, Gitleaks, Dependabot, release workflow, signing fallback, SBOM automation, and artifacts/checksums: 10-18 person-days.
- Documentation, backlog/process maintenance, release/security docs, reviews, troubleshooting, and README upkeep: 8-14 person-days.
- Integration churn, dependency/update triage, Android/Gradle/Kotlin warning cleanup, release-action fixes, and real-device validation loops: 8-16 person-days.

Human-readable total: **roughly 13-21 full-time weeks** for a strong 10-year Android developer. A realistic midpoint is **about 17 weeks**.

Important caveat: this is not a shipped Play Store product estimate. Remaining backlog items still include screenshots/demo assets, screenshot/instrumented/E2E tests, Play Store listing/compliance/upload work, SDK/toolchain upgrades when available, renderer consolidation, cache lifecycle hardening, and device validation for video/landscape behavior.
