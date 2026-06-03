# GitHub Actions

This project has two workflows under [`.github/workflows/`](../.github/workflows/).

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
3. Builds:
   - `androidCatEars-<version>-debug.apk` — installable directly on an Android 14+ device.
   - `androidCatEars-<version>-release.aab` — Play Store app bundle.
4. Publishes a **GitHub Release**:
   - Tag: `v<version>` (e.g. `v0.1.25`)
   - Title: `androidCatEars <version>`
   - Body mentions the version number and what each download is.
   - Both artefacts attached for download.

So a user can open the repo's **Releases** page, pick the latest, and download the `.apk` to install.

### How to cut a release

1. Make sure the version in `version.properties` is what you want to release
   (the patch auto-increments per commit; bump minor/major manually if needed).
2. Push your branch to GitHub (this project commits locally by default — push when ready).
3. Go to **Actions → Release → Run workflow**, choose the branch, and run it.
4. When it finishes, the new release appears under **Releases** with the APK and AAB.

### Signing note

The downloadable APK is **debug-signed** (the CI debug keystore). It installs fine for testing,
but:

- Debug keys differ between machines/runs, so updating over a previously-installed build from a
  different source may require uninstalling first.
- The `.aab` is **unsigned** until release signing is configured (backlog WP 15).

Once release signing lands, the workflow will produce a properly signed release APK/AAB suitable
for stable distribution and the Play Store.

---

## Best practices used

- `actions/checkout@v4`, `actions/setup-java@v4` (Temurin JDK 17), `gradle/actions/setup-gradle@v4`
  for Gradle caching.
- `softprops/action-gh-release@v2` for publishing releases.
- Least-privilege `permissions: contents: write` (only what's needed to create a release/tag).
- The release reuses the exact same quality gate as CI before building artefacts.
