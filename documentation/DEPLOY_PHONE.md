# Deploy to Phone — Quick Guide

How to build the APK and install it on your Android device (Android 14+, API 34+).

---

## One-time setup (per device)

### Enable Developer Options on MIUI (Xiaomi)

1. **Settings → About phone → MIUI version** — tap 7 times until "Developer mode enabled"
2. **Settings → Additional settings → Developer options**
   - **USB debugging** → ON
   - **USB debugging (Security settings)** → ON *(needed on MIUI)*
3. Plug in USB cable → choose **File Transfer (MTP)** on the phone
4. Accept the **RSA fingerprint** dialog that appears on the phone

Verify the phone is visible:
```bash
~/Android/Sdk/platform-tools/adb devices
```
Expected output:
```
List of devices attached
<serial>    device
```

---

## Build and deploy

```bash
cd ~/repos/androidCatEars
export ANDROID_HOME="$HOME/Android/Sdk"

# Build debug APK and install in one command:
./gradlew installDebug
```

That's it. The app launches automatically after install.

---

## Just build the APK (without installing)

```bash
./gradlew assembleDebug
```

APK lands at:
```
app/build/outputs/apk/debug/app-debug.apk
```

Transfer it to the phone manually (USB, local network, etc.) and tap to install.

---

## Update after a code change

```bash
./gradlew installDebug
```

No reboot needed — it replaces the existing install on the device.

---

## Check which version is running

```bash
adb shell dumpsys package it.marcelpetrick.catears | grep versionName
```

---

## If the phone isn't detected

```bash
adb kill-server && adb start-server
adb devices
```

If it shows `unauthorized`: look at the phone screen and tap **Allow** on the RSA fingerprint dialog.

---

## Requirements

- Android 14 or newer (API 34+) — the app will not install on older Android versions
- The Huawei HMA-L29 (Android 12) is **not compatible** — use the Xiaomi (Android 16) instead
