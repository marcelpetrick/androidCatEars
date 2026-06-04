# Product Ideas — androidCatEars — 2026-06-04

Ten feature ideas from a product + marketing perspective.
Audience: people who share selfies, make short-form content, and think AR filters are fun.
Tone: hip, fun, zero cringe.

---

## 1. Beat-reactive ears

The ears don't just sway — they **bounce to music**. Tap the microphone button to arm the audio
analyser; ears scale and wiggle proportionally to bass amplitude. Loud beat = big bop. Silence = lazy
twitch. Works for concerts, car karaoke, gym selfies, anything with sound. Every video becomes a tiny
music visualiser that lives on your head.

**Why it ships**: the ear animation is already parameterised; you'd feed `swayTime` from an FFT bin
instead of a linear timer. No new UI surface needed.

---

## 2. Seasonal & themed ear packs

The 10 built-in styles are great — extend them with themed drops:

| Drop | Styles |
|------|--------|
| Spooky Season | Bat wings, Frankenstein bolts, pointy witch hat |
| Winter | Reindeer antlers, Elf ears, Santa hat mini |
| Valentine | Heart ears, Cupid wings at temples |
| Galaxy | Glowing neon ears with sparkle particle trail |
| Y2K | Pixel-art ears rendered as chunky canvas polygons |

Release one pack per season as an in-app unlock (free or paid). Each drop generates a social moment
and gives existing users a reason to open the app again.

---

## 3. Catify My Gallery

You took 200 photos this year and none of them have ears. **Fix that retroactively.**

A "Catify" button in the help screen opens the photo picker. ML Kit runs face detection on every
selected photo. Each face gets ears placed and the composited image is saved alongside the original.
Batch process up to 10 photos at once. Progress bar. Done.

Users who discover this feature share it. "I catified my entire family album" is a perfect TikTok.

---

## 4. GIF export

Five-second clips are great. Looping GIFs are better for messaging apps.

After saving the video, offer **Export as GIF** (alongside Share Video). Downsample the MP4 frames,
palette-quantise to 256 colours, write a GIF. Target 480 × 480 px at 12fps — small enough for
Telegram/WhatsApp, good enough to look crisp. Drop it directly into iMessage, Messenger, or Discord.

GIFs stay in chats forever. Every GIF is a passive impression for the app.

---

## 5. Party Mode — one face, one style

When multiple faces are detected, each face gets its own **randomly assigned** ear style and tint.
The styles persist per tracking ID so they don't shuffle on every frame. First face = classic brown,
second = neon fox, third = galaxy lynx. Group selfies become a personality quiz: which ears are you?

Add a "re-roll" button to shuffle assignments. People will re-roll 10 times. That's 10 more seconds
of engagement per session.

---

## 6. Whiskers & nose kit

Ears are a start. Complete the transformation.

Add a **cat face kit** toggle: three procedural white whisker lines extending from the nose area
(derived from eye landmarks), plus a small pink triangle nose centered between the eyes. All Compose
Canvas, no texture assets, consistent with the existing procedural style. Toggleable independently of
the ears. The whiskers should wobble slightly — same sway animation, 1/4 amplitude.

This is the feature that makes someone say "oh it's a whole cat face" and immediately open the app
in front of someone else.

---

## 7. TikTok / Reels quick-send

After recording, the share sheet is generic. Instead, put a **TikTok** and **Instagram Reels** icon
directly on the save confirmation screen. One tap deep-links into TikTok's camera with the clip
pre-loaded via `ACTION_SEND` to `com.zhiliaoapp.musically` / `com.instagram.android`. The user lands
in TikTok's editor with the clip ready to caption and post. Zero extra steps.

Frictionless sharing is the most important growth mechanic for a creative app. Every clip shared
brings one new user back.

---

## 8. Live wallpaper / Lock Screen widget

Post a killer cat-ears selfie? **Set it as your lock screen** in one tap. On Android 14+, use
`WallpaperManager.setStream()` to write the composited photo as the lock screen wallpaper. The
result is a photo of yourself with ears that appears every time you unlock your phone. You will show
it to literally everyone.

Bonus stretch: animated live wallpaper using a short looping version of the 5-second clip. Requires
`WallpaperService`, but the clip is already produced.

---

## 9. Sound toggle preference

The shutter click and recording sounds are good — and legally required in some countries. But some
people record in meetings, libraries, or just hate the click. Add a **Sound on / off** preference
(persisted via DataStore) in the help/settings area. When off, skip `MediaActionSound.play()`.
Jurisdictions where the sound is mandatory (JP, KR) should ignore the setting and always play — this
is documented explicitly in `MediaActionSound`'s API contract.

Small feature, big quality signal. Users notice apps that respect their environment.

---

## 10. Pet Mode — reverse the premise

Detect cats and dogs. Give them **human ears**.

ML Kit's object detection (or a fine-tuned TFLite model) can identify a dog or cat's head bounding
box. Overlay a tiny pair of human-style headphones, human ears, or cartoon top-hat. The app already
draws arbitrary shapes onto arbitrary bounding boxes — this is a UX direction, not a new engine.

"I put glasses and human ears on my cat" is the most shareable content type on the internet. This
feature writes its own press release.
