# Android Emulator Setup

How to run the app in the Android Emulator on Manjaro Linux.

---

## Prerequisites

- Android SDK at `~/Android/Sdk` (set up during project initialisation — see `TOOLING.md`)
- KVM hardware acceleration enabled (see "Troubleshooting" below)
- The `emulator` and `system-images;android-34;google_apis;x86_64` packages installed via `sdkmanager`

Install if not already present:
```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
sdkmanager "emulator" "system-images;android-34;google_apis;x86_64"
```

---

## Create the AVD (once)

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

echo "no" | avdmanager create avd \
  -n CatEars34 \
  -k "system-images;android-34;google_apis;x86_64" \
  --device "pixel_6" \
  --force
```

The AVD is named **CatEars34**, uses a Pixel 6 skin, and targets API 34 (Android 14) — matching the app's `minSdk`.

---

## Start the emulator

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

emulator -avd CatEars34 -accel on -gpu host -camera-front webcam0 -memory 4096 -no-audio &
```

Wait for it to fully boot (30–90 seconds with KVM; longer without):
```bash
until adb -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null | grep -q '1'; do
  sleep 3
done
echo "Emulator ready"
```

---

## Build and install the app

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew installDebug
```

Or build first, then install manually:
```bash
./gradlew assembleDebug
adb -s emulator-5554 install -r app/build/outputs/apk/debug/androidCatEars-debug.apk
```

---

## Camera and ML Kit behavior

- **Hardware acceleration**: use KVM (`-accel on`) for a stable local loop.
- **Host webcam**: pass `-camera-front webcam0` (or another webcam from
  `emulator -webcam-list`) to feed a real face into the front camera.
- **ML Kit**: runs on the emulator and can detect faces from a webcam feed, but
  camera orientation, mirroring, frame timing, and sensor metadata still differ
  from a real phone.
- **Performance**: depends heavily on KVM (see below).

Use a physical device running Android 14+ (API 34+) as the final authority for
camera behavior and overlay alignment. The target device is the Xiaomi Pro
(Android 16) specified in `VISION.md`.

---

## Troubleshooting

### KVM conflict with VirtualBox

**Symptom:** `ioctl(KVM_CREATE_VM) failed: 5 Input/output error` in emulator log.

**Cause:** VirtualBox kernel modules (`vboxdrv`, `vboxnetflt`) can conflict with KVM on the same system.

**Fix:** first stop any running VirtualBox VMs. If the conflict remains, reload
the KVM module:
```bash
sudo modprobe -r kvm_intel
sudo modprobe kvm_intel
```

If VirtualBox VMs need to keep running, start the Android emulator first, then
launch VirtualBox — they can coexist once KVM is initialised. Do not unload
`kvm_intel` while another KVM user is active; `emulator -accel-check` should
report whether KVM is usable.

### Crash with exit code 139 (segfault) when using `-gpu host`

On laptops with hybrid GPU (e.g. Intel Iris Xe + NVIDIA discrete), the emulator can segfault
when using `-gpu host` after a software-mode boot. Use `-gpu swiftshader_indirect` for stability:

```bash
emulator -avd CatEars34 -accel off -gpu swiftshader_indirect -memory 4096 -no-audio -no-boot-anim &
```

Once KVM is available (VirtualBox not conflicting), `-gpu host` works reliably with `-accel on`.

### Emulator starts but is very slow

Without KVM, software emulation can take 5–10 minutes to boot. Always ensure KVM is working before starting the emulator (see above).

### No devices in `adb devices`

```bash
adb kill-server && adb start-server
adb devices
```

### App not installing (INSTALL_FAILED_OLDER_SDK)

The app requires Android 14 (API 34). Only use AVDs or physical devices with API 34 or higher.

---

## Quick-start summary

```bash
# One-time setup
sdkmanager "emulator" "system-images;android-34;google_apis;x86_64"
avdmanager create avd -n CatEars34 -k "system-images;android-34;google_apis;x86_64" --device pixel_6 --force

# Every session
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"
emulator -avd CatEars34 -accel on -gpu host -camera-front webcam0 -memory 4096 -no-audio &
# wait for boot, then:
./gradlew installDebug
```
