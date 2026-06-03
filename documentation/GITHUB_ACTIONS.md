# GitHub Actions

This project has several workflows under [`.github/workflows/`](../.github/workflows/).

---

## 1. CI — `ci.yml`

**Trigger:** automatically on every push and pull request to `master` / `main`.

**What it does** (mirrors the local `scripts/ci.sh` gate):

1. `assembleDebug` — compiles the app
2. `spotlessCheck` — formatting
3. `detekt` — static analysis
4. `:app:lint` — Android Lint
5. `:app:test` — unit tests
6. `:app:koverVerify` — coverage ≥ 95% on the domain/logic scope
7. `cyclonedxBom` — aggregate CycloneDX SBOM generation

It uploads test, coverage, lint, and CycloneDX SBOM reports as build artefacts.
A red CI run blocks merging. The workflow invokes `./scripts/ci.sh` directly so
local and GitHub checks cannot drift silently.

---

## 2. Release — `release.yml`

**Trigger:** **manual only** — GitHub UI → **Actions → Release → Run workflow**.

**What it does:**

1. Reads the current version from `version.properties` (e.g. `0.1.25`).
2. Runs the full quality gate (same as CI).
3. Uses `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and
   `RELEASE_KEY_PASSWORD` repository secrets when present; otherwise the release AAB is unsigned
   and labelled as such.
4. Builds `androidCatEars-<version>-release.aab` — signed only when signing secrets are configured.
5. Generates CycloneDX SBOM release artifacts:
   - `androidCatEars-<version>.cdx.json`
   - `androidCatEars-<version>.cdx.xml`
   - SHA-256 checksum files for both SBOMs
6. Publishes a **GitHub Release**:
   - Tag: `v<version>` (e.g. `v0.1.25`)
   - Title: `androidCatEars <version>`
   - Body mentions the version number and what each download is.
   - The release AAB, debug APK, CycloneDX SBOMs, and SBOM checksums attached for download.

So an operator can download the `.aab` for Play Console upload and the
CycloneDX SBOMs for release supply-chain records.

### How to cut a release

1. Make sure the version in `version.properties` is what you want to release
   (the patch auto-increments per commit; bump minor/major manually if needed).
2. Push your branch to GitHub (this project commits locally by default — push when ready).
3. Go to **Actions → Release → Run workflow**, choose the branch, and run it.
4. When it finishes, the new release appears under **Releases** with the AAB and SBOM artifacts.

### Signing note

The release workflow decodes `RELEASE_KEYSTORE_BASE64` into a runner-local keystore file, passes
that path to Gradle as `RELEASE_STORE_FILE`, and signs the AAB when all signing secrets are present.
The debug APK is attached for sideload testing only and must not be used as a production artifact.

### SBOM note

The release workflow runs `./scripts/generate-sbom.sh release-artefacts`, which
wraps `./gradlew cyclonedxBom`. The same script is locally triggerable and
produces the same versioned CycloneDX JSON/XML files plus SHA-256 checksums.

---

## Best practices used

- `actions/checkout@v6`, `actions/setup-java@v5` (Temurin JDK 17), `gradle/actions/setup-gradle@v6`
  for Gradle caching.
- `softprops/action-gh-release@v2` for publishing releases.
- Least-privilege `permissions: contents: write` (only what's needed to create a release/tag).
- The release reuses the exact same quality gate as CI before building artefacts.
- Release artifacts include CycloneDX SBOM JSON/XML files and SHA-256 checksums.
- Workflows set explicit `timeout-minutes`; branch/PR workflows cancel superseded runs via
  `concurrency` so the newest signal stays visible.

---

## 3. Dependency Review — `dependency-review.yml`

**Trigger:** automatically on pull requests to `master` / `main`.

It uses GitHub's dependency review action to fail the PR when a changed dependency introduces a
known vulnerability at **moderate** severity or above.

---

## 4. CodeQL — `codeql.yml`

**Trigger:** automatically on pushes and pull requests to `master` / `main`, plus a weekly scheduled
scan.

It builds the debug APK and runs CodeQL's Java/Kotlin analysis, publishing findings to GitHub code
scanning.

---

## 5. Secret Scan — `secret-scan.yml`

**Trigger:** automatically on pushes and pull requests to `master` / `main`.

It runs Gitleaks across committed history so accidental keystores, tokens, passwords, and similar
secrets fail CI before they are treated as project artifacts.
