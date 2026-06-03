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

It uploads test, coverage, and lint reports as build artefacts. A red CI run blocks merging.

---

## 2. Release — `release.yml`

**Trigger:** **manual only** — GitHub UI → **Actions → Release → Run workflow**.

**What it does:**

1. Reads the current version from `version.properties` (e.g. `0.1.25`).
2. Runs the full quality gate (same as CI).
3. Requires `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and
   `RELEASE_KEY_PASSWORD` repository secrets.
4. Builds `androidCatEars-<version>-release.aab` — the signed Play Store app bundle.
5. Verifies the AAB signature with `jarsigner -verify -strict`.
6. Publishes a **GitHub Release**:
   - Tag: `v<version>` (e.g. `v0.1.25`)
   - Title: `androidCatEars <version>`
   - Body mentions the version number and what each download is.
   - The release AAB attached for download.

So an operator can download the `.aab` for Play Console upload.

### How to cut a release

1. Make sure the version in `version.properties` is what you want to release
   (the patch auto-increments per commit; bump minor/major manually if needed).
2. Push your branch to GitHub (this project commits locally by default — push when ready).
3. Go to **Actions → Release → Run workflow**, choose the branch, and run it.
4. When it finishes, the new release appears under **Releases** with the signed AAB.

### Signing note

The release workflow decodes `RELEASE_KEYSTORE_BASE64` into a runner-local keystore file, passes
that path to Gradle as `RELEASE_STORE_FILE`, and verifies the signed AAB before publishing. Debug
APKs are development artifacts and are not attached to GitHub Releases.

---

## Best practices used

- `actions/checkout@v4`, `actions/setup-java@v4` (Temurin JDK 17), `gradle/actions/setup-gradle@v4`
  for Gradle caching.
- `softprops/action-gh-release@v2` for publishing releases.
- Least-privilege `permissions: contents: write` (only what's needed to create a release/tag).
- The release reuses the exact same quality gate as CI before building artefacts.

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
