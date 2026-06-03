<!--
  SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
# Publishing to the Google Play Store

End-to-end guide for getting **androidCatEars** onto Google Play: account
setup, the signing model, required assets, and the release flow. For *building*
a signed artifact locally, see [`RELEASE.md`](RELEASE.md).

> Numbers and policies below reflect Google Play at time of writing — always
> confirm against the [Play Console Help](https://support.google.com/googleplay/android-developer).

---

## 1. Create a Google Play Developer account

1. Go to <https://play.google.com/console> and sign in with the Google account
   that should **own** the app.
2. Pay the **one-time US$25** registration fee.
3. Choose account type:
   - **Personal** — quick, but see the testing requirement in §6.
   - **Organisation** — requires a **D-U-N-S number** and identity verification
     (allow several days).
4. Complete identity verification (legal name, address, phone). New accounts
   can take a few days to be approved.

---

## 2. Understand Play App Signing (the two-key model)

Google **manages your app signing key** for you; you never ship it. There are
two keys:

| Key | Who holds it | Purpose |
|-----|--------------|---------|
| **App signing key** | Google (Play App Signing) | Signs the APKs delivered to users. Permanent — defines the app identity. |
| **Upload key** | You | Signs the AAB you upload. Google verifies it, strips it, re-signs with the app signing key. |

This means: **the keystore from [`RELEASE.md`](RELEASE.md) is your *upload* key.**
If you ever lose it, Google can reset it — but losing the *app signing* key is
impossible because Google holds it. Recommended path: let Google **generate**
the app signing key on first upload, and register your existing keystore as the
upload key.

Keep your upload keystore + `keystore.properties` backed up securely and **out
of git** (already gitignored).

---

## 3. Build the artifact to upload

New apps **must** ship an **Android App Bundle (.aab)**, not an APK:

```bash
./gradlew bundleRelease
# -> app/build/outputs/bundle/release/androidCatEars-release.aab  (signed with the upload key)
```

`versionCode` must **strictly increase** on every upload — the auto patch-bump
hook handles this, but never reuse a code.

Target API level: Play requires new apps/updates to target a recent API
(currently API 34+). This app targets **API 36**, so it is compliant.

---

## 4. Create the app in Play Console

1. **Create app** → name `androidCatEars`, default language, app/game = App,
   free/paid = Free.
2. Work through **Dashboard → "Set up your app"**:
   - **App access** — all features available without special login (true here).
   - **Ads** — declare whether the app contains ads (no).
   - **Content rating** — complete the IARC questionnaire.
   - **Target audience & content** — set age groups (not directed at children).
   - **Data safety** — **declare camera usage and any photo storage.** This app
     processes the camera **on-device only**, takes no data off the device, and
     saves photos to the user's own gallery — state exactly that.
   - **Privacy policy** — a public URL is **required** (camera is a sensitive
     permission). Host a short policy and paste the link.
   - **Government apps / financial / health** — N/A.

---

## 5. Store listing assets

Prepare before submitting (see backlog 14.4 for capturing screenshots):

| Asset | Spec |
|-------|------|
| App icon | 512×512 PNG, 32-bit, ≤1 MB |
| Feature graphic | 1024×500 PNG/JPG |
| Phone screenshots | 2–8 images, 16:9 or 9:16, min side ≥320 px |
| Short description | ≤80 chars |
| Full description | ≤4000 chars |
| (optional) 7"/10" tablet shots, promo video | — |

Reuse the in-app launcher icon artwork as the basis for the 512×512 icon.

---

## 6. Testing tracks → production

Play has staged tracks; promote upward as confidence grows:

```
Internal testing  →  Closed testing (alpha/beta)  →  Open testing  →  Production
(≤100 testers,        (invite lists)                  (public opt-in)
 instant)
```

⚠️ **Personal accounts created after ~Nov 2023** must run a **closed test with
at least 20 testers opted-in for 14 consecutive days** before they can request
production access. Plan for this lead time. Organisation accounts are exempt.

Each upload triggers a **Pre-launch report** (Google runs your app on real
devices and reports crashes/accessibility/security findings) — a free smoke test
that complements the suite proposed in [`PROJECT_REVIEW.md`](PROJECT_REVIEW.md).

---

## 7. (Optional) Automate publishing from CI

Two common routes, both using a **Google Cloud service account** granted access
in *Play Console → Users & permissions* and the *Play Android Developer API*:

- **Gradle Play Publisher** — `id("com.github.triplet.play")`; then
  `./gradlew publishReleaseBundle` uploads the AAB to a chosen track.
- **fastlane `supply`** — Ruby-based, language-agnostic, popular for metadata +
  screenshots + binary in one run.

Store the service-account JSON as a CI secret (never in git). This slots into
the existing manual release workflow in [`GITHUB_ACTIONS.md`](GITHUB_ACTIONS.md).

---

## 8. Pre-submission checklist

- [ ] Developer account approved; US$25 paid.
- [ ] Upload keystore created, backed up, registered (or Google-generated app key).
- [ ] `bundleRelease` AAB built and signed; `versionCode` increased.
- [ ] Privacy policy URL live (covers camera + photo storage).
- [ ] Data safety, content rating, target audience, ads forms completed.
- [ ] Store listing: icon, feature graphic, ≥2 screenshots, descriptions.
- [ ] Closed test run (≥20 testers / 14 days) if a post-2023 personal account.
- [ ] Pre-launch report reviewed; crashes addressed.
- [ ] Promote to production.
