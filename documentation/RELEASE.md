<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Release Guide

How to produce a signed release build of **androidCatEars** locally, and the
checklist to follow before tagging a release. CI-driven releases are documented
separately in [`GITHUB_ACTIONS.md`](GITHUB_ACTIONS.md).

---

## 1. One-time: create a signing keystore

A release keystore signs the APK/AAB so Android (and the Play Store) accept
updates. Create one once and keep it safe — losing it means you can no longer
publish updates under the same app identity.

```bash
keytool -genkeypair -v -keystore release.jks -alias catears \
  -keyalg RSA -keysize 2048 -validity 10000
```

Store `release.jks` **outside** the repo, or anywhere — `*.jks`/`*.keystore`
are gitignored and must never be committed.

## 2. Provide credentials (no secrets in git)

The build reads signing credentials from **either** source, checked in this order:

1. A gitignored `keystore.properties` in the repo root. Copy the template:

   ```bash
   cp keystore.properties.example keystore.properties
   # then edit storeFile / storePassword / keyAlias / keyPassword
   ```

2. Environment variables (handy for CI):

   ```bash
   export RELEASE_STORE_FILE=/secure/path/release.jks
   export RELEASE_STORE_PASSWORD=…
   export RELEASE_KEY_ALIAS=catears
   export RELEASE_KEY_PASSWORD=…
   ```

   In GitHub Actions, store the keystore contents as a base64 secret instead of
   a path:

   ```bash
   base64 -w 0 release.jks
   # save output as RELEASE_KEYSTORE_BASE64
   ```

   The release workflow decodes it into a runner-local file and exports
   `RELEASE_STORE_FILE` for Gradle.

If neither is present, the release build still succeeds but emits an **unsigned**
artifact (`androidCatEars-release-unsigned.apk`).

## 3. Build the release artifacts

```bash
./gradlew assembleRelease   # signed APK  -> app/build/outputs/apk/release/androidCatEars-release.apk
./gradlew bundleRelease     # signed AAB  -> app/build/outputs/bundle/release/androidCatEars-release.aab
```

Both are R8-minified with resource shrinking. The ProGuard mapping for
deobfuscating crash traces is written to
`app/build/outputs/mapping/release/mapping.txt` — archive it per release.

## 4. Verify the signature

```bash
"$ANDROID_HOME"/build-tools/*/apksigner verify --print-certs \
  app/build/outputs/apk/release/androidCatEars-release.apk
```

A signed build prints `Signer #1 certificate DN: …`; the output filename also
drops the `-unsigned` suffix.

## 5. Install a signed build on a device

```bash
adb install -r app/build/outputs/apk/release/androidCatEars-release.apk
```

(For day-to-day debug installs see [`DEPLOY_PHONE.md`](DEPLOY_PHONE.md).)

---

## Release checklist

- [ ] Working tree clean; all quality gates green (`./scripts/ci.sh`).
- [ ] Version bumped appropriately in `version.properties` (minor/major are
      manual; patch auto-bumps per commit).
- [ ] `keystore.properties` or `RELEASE_*` env vars configured.
- [ ] `./gradlew assembleRelease bundleRelease` succeeds.
- [ ] APK signature verified (`apksigner verify`).
- [ ] `mapping.txt` archived for this version.
- [ ] Release notes / changelog updated.
- [ ] Tag created and pushed (or trigger the GitHub Actions release workflow).
