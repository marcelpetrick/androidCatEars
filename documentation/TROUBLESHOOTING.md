<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Troubleshooting

Common issues building, running, and testing **androidCatEars**, with fixes.
See also [`EMULATOR.md`](EMULATOR.md), [`DEPLOY_PHONE.md`](DEPLOY_PHONE.md), and
[`RELEASE.md`](RELEASE.md).

---

## Build

**"Deprecated Gradle features were used in this build, making it incompatible with Gradle 10."**
This one-line footer appears on every build. The single source is `ReportingExtension.file(String)`
called inside **detekt 1.23.8's own plugin code** (`DetektPlugin.kt:28`) — not our build scripts.
`1.23.8` is the latest stable detekt release; no fix is available yet. The build succeeds normally.
`gradle.properties` sets `org.gradle.warning.mode=summary` so the verbose warning text is suppressed
and only the footer remains. To investigate any NEW warnings: run `./gradlew <task> --warning-mode all`.
Fix: upgrade detekt once a release that uses the new `getBaseDirectory().file()` API is published
(tracked in backlog task 2.3 / WP 18 area).

**`./gradlew` fails with OutOfMemoryError / GC overhead during dex or merge.**
The daemon heap is set in `gradle.properties` (`-Xmx4g`). If it still thrashes
on a constrained machine, lower other memory use or raise the value.

**Configuration cache errors after editing `build.gradle.kts`.**
The build uses the configuration cache. A stale entry is rebuilt automatically;
to force it, run `./gradlew --no-configuration-cache <task>` once, or delete
`.gradle/configuration-cache`.

**Lint fails the build on warnings.**
Lint runs with `warningsAsErrors = true`. Fix the warning, or — only with a
documented reason — suppress the specific rule in `app/lint.xml`. A few rules
(`OldTargetApi`, `GradleDependency`, `NewerVersionAvailable`) are intentionally
suppressed pending the SDK 37 toolchain upgrade (backlog task 2.3).

**detekt `MagicNumber` on theme colours.**
The colour palette (`ui/theme/Color.kt`) is excluded from `MagicNumber` in
`config/detekt/detekt.yml`; other code must still name its constants.

---

## Coverage

**`koverVerify` fails after adding a class.**
The 95% gate measures pure logic. New Android-framework glue must be extracted
into a pure function (tested) plus a thin adapter (excluded). Add the adapter to
the Kover `excludes` block in `app/build.gradle.kts` **with a reason**, or — far
better — move the real logic into `domain` and test it. Do not lower the bound.

---

## Emulator

**Emulator won't start / KVM unavailable.**
VirtualBox kernel modules can hold `kvm_intel`. Shut down VirtualBox VMs, then
`sudo modprobe -r kvm_intel && sudo modprobe kvm_intel`. Full details in
[`EMULATOR.md`](EMULATOR.md).

**Emulator segfaults (exit 139) on a hybrid GPU.**
`-gpu host` can crash on Intel+NVIDIA setups. Use
`-gpu swiftshader_indirect` (software rendering — slower but stable).

**Ears don't track in the emulator.**
The emulator's virtual camera shows a synthetic scene with no real face, so face
detection has nothing to lock onto. Verify tracking on a physical device.

---

## Device

**App won't install on an older phone (e.g. Android 12 / HMA-L29).**
`minSdk` is 34; pre-Android-14 devices are unsupported by design and the code is
not adapted downward. Use a device on Android 14+ or the emulator.

**`adb` doesn't see the phone.**
Enable Developer options → USB debugging, accept the RSA prompt, and check
`adb devices`. See [`DEPLOY_PHONE.md`](DEPLOY_PHONE.md).

---

## Release & signing

**Release build produces `…-release-unsigned.apk`.**
No signing credentials were found. Provide a `keystore.properties` (copy
`keystore.properties.example`) or `RELEASE_*` environment variables — see
[`RELEASE.md`](RELEASE.md).

**Crash stack traces from a release build are obfuscated.**
Deobfuscate against `app/build/outputs/mapping/release/mapping.txt` (archived
per release). Line numbers are preserved by the ProGuard config.
