# Publishing Balloon Pop & Learn on the Google Play Store

A complete, beginner-friendly walkthrough — from a signing key to a live store
listing. Follow the phases in order. Steps you do once ever are marked
**(once)**; steps you repeat for every update are marked **(every release)**.

---

## Phase 0 — What you need before starting

| Thing | Where |
|---|---|
| A Google account | any Gmail |
| **$25 (one-time)** Play Console registration fee | paid during sign-up |
| JDK 17 + Android SDK | already installed on this PC |
| A public URL for the privacy policy | see Phase 3 |
| ~1 hour for the forms | — |

> **Identity check:** Google verifies new developer accounts (ID document,
> sometimes an address postcard). Start the account early — verification can
> take a few days.

---

## Phase 1 — Create your signing key **(once)**

Play releases must be signed. The key is created once and reused forever —
**if you lose it, you lose the ability to update the app**, so back it up.

Open **Command Prompt** in this project folder and run:

```bat
"%ProgramFiles%\Eclipse Adoptium\jdk-17.0.20.8-hotspot\bin\keytool" -genkeypair -v -keystore popgrow-release.jks -alias popgrow -keyalg RSA -keysize 2048 -validity 10000
```

It asks for:
- A **keystore password** — invent one, write it down somewhere safe
- Your name/organisation/city — fill what you like (shown to nobody)
- A **key password** — press Enter to reuse the keystore password

This creates `popgrow-release.jks` in the project folder.

Now create a file named **`keystore.properties`** in the project folder
(next to `build.gradle.kts`) with exactly these four lines:

```properties
storeFile=popgrow-release.jks
storePassword=YOUR_PASSWORD_HERE
keyAlias=popgrow
keyPassword=YOUR_PASSWORD_HERE
```

The build is already wired to pick this up automatically (see
`app/build.gradle.kts`) — with the file present, release builds sign
themselves; without it they build unsigned.

> 🔒 Both `popgrow-release.jks` and `keystore.properties` are already in
> `.gitignore` — they will never be committed. **Copy both to a safe place**
> (USB stick, password manager) right now.

---

## Phase 2 — Build the release **(every release)**

1. **Bump the version** in [app/build.gradle.kts](app/build.gradle.kts):

   ```kotlin
   versionCode = 1        // +1 every upload (2, 3, 4…)
   versionName = "1.0.0"  // human-readable, your choice
   ```

   Google rejects an upload whose `versionCode` it has seen before.

2. **Build the App Bundle** (the format Play requires):

   ```bat
   env.bat
   gradlew.bat bundleRelease
   ```

   Output: `app\build\outputs\bundle\release\app-release.aab`

3. **Test the release build on your phone first** (it is minified, so worth a
   real check):

   ```bat
   gradlew.bat installRelease
   ```

   Play with every mode for a minute. If it behaves, the `.aab` is good.

---

## Phase 3 — Put the privacy policy at a public URL **(once)**

Google **requires** a link to a privacy policy for every children's app.
[PRIVACY.md](PRIVACY.md) is already written and compliant (no data collected,
no internet permission). It just needs to be reachable on the web.

**Easiest path — GitHub:**
1. Push this project to a public GitHub repository (or a private repo with just
   the PRIVACY.md in a public one).
2. Open PRIVACY.md on github.com and copy the URL, e.g.
   `https://github.com/YOURNAME/PopAndGrow/blob/main/PRIVACY.md`
3. That URL goes into the Play Console form later.

Any other public URL (Google Sites, your own site) works equally well.

---

## Phase 4 — Play Console: create the app **(once)**

1. Go to <https://play.google.com/console> → pay the $25 → complete identity
   verification.
2. **Create app**:
   - App name: **Balloon Pop & Learn**
   - Default language: English (United States)
   - App or game: **Game**
   - Free or paid: **Free** (a free app can never be made paid later — fine here)
   - Accept the declarations.

3. Work through **“Set up your app”** (the dashboard checklist). Answers for
   this app:

   | Form | Answer |
   |---|---|
   | Privacy policy | the URL from Phase 3 |
   | App access | All functionality is available without special access |
   | Ads | **No, my app does not contain ads** |
   | Content rating | fill the IARC questionnaire honestly → results in **Everyone / PEGI 3** |
   | Target audience | Age groups **“5 and under”** (you can add 6–8 too) |
   | “Designed for Families” / children | the app **complies**: no ads, no purchases, no data collection, no links out |
   | News app | No |
   | COVID-19 app | No |
   | Data safety | **No data collected**, **No data shared** — every question is “No” (the app has no INTERNET permission, which Google can verify) |
   | Government app | No |
   | Financial features | None |

   > Because the target audience includes children, Google reviews the app
   > against its **Families policy**. This app was built for exactly that:
   > zero data, zero ads, zero purchases, an on-device-only experience.

---

## Phase 5 — Store listing **(once, editable later)**

### Text (copy-paste, edit as you like)

- **App name** (max 30 chars):
  `Balloon Pop & Learn` (19 chars — room to add e.g. `: ABC 123` if you like)

- **Short description** (max 80 chars):
  `Pop balloons, learn ABC, 123, colors & shapes. Calm, offline, ad‑free. 2–5.`

- **Full description** (max 4000 chars):

  ```
  Balloon Pop & Learn is a calm, cheerful balloon-popping playground for toddlers and
  preschoolers (ages 2–5). Tap friendly balloons with smiling faces to pop
  them into confetti, grow a flower garden, meet butterflies, bees, puppies
  and bunnies — and gently learn along the way.

  FIVE WAYS TO PLAY
  🎈 Play — pure popping fun: musical notes, confetti, gardens and fireworks
  🌈 Colors — find and pop the balloon of the spoken color
  🔤 A–Z — pop letters in order and hear “A for Apple” with a picture
  🔢 1–20 — find each number, watch it fly into place
  ⭐ Shapes — circle, square, triangle, star and heart

  MADE FOR LITTLE HANDS
  • Tap is the only gesture — no drags, no menus in the way
  • Big targets, forgiving taps, and nothing to get wrong — ever
  • No score pressure, no timers, no “game over”
  • Exit and settings are protected so a child can’t leave by accident
  • The correct answer glows so pre-readers always know what to find

  A LIVING WORLD
  • The sky drifts through meadow, sunset, night, candy, ocean and space
  • A smiling sun becomes a sleepy moon; fireflies and shooting stars at dusk
  • Gentle wind, birds by day, crickets and owls at night — all generated on
    your device

  PRIVATE BY DESIGN
  • 100% offline — the app does not even have the INTERNET permission
  • No ads, no in-app purchases, no accounts, no data collection — nothing
  • Small download, works on phones and tablets

  Made with love for curious little poppers.
  ```

### Graphics you must upload

| Asset | Size | How to make it |
|---|---|---|
| App icon | 512×512 PNG | In Android Studio: **File → New → Image Asset**, pick the existing launcher foreground/background, and export; or screenshot the icon from the phone's app drawer and crop to 512×512 |
| Feature graphic | 1024×500 PNG/JPG | A simple banner: sky-blue background, the title, and a few balloons. Any image editor (even PowerPoint) works |
| Phone screenshots | at least 2 (up to 8) | With the phone connected, capture straight from the app: |

```bat
adb shell screencap -p /sdcard/shot1.png
adb pull /sdcard/shot1.png
```

Take one of the menu (rainbow arch), one of each learning mode, one mid-pop
with confetti. Tablet screenshots are optional but help.

- Category: **Educational** · Tags: kids, toddler, preschool
- Contact email: required — shown publicly on the listing.

---

## Phase 6 — Release! **(every release)**

1. Play Console → **Production** → **Create new release**.
2. First time: accept **Play App Signing** (Google keeps the final signing key;
   your `.jks` becomes the upload key — recommended, click through the default).
3. Upload `app-release.aab`.
4. Release notes, e.g. `First release 🎈`.
5. **Next → Save → Review release → Start rollout to production.**

**What happens next:** the app goes to Google review. For a brand-new developer
account with a children's app, expect **2 days to ~2 weeks**. You'll get an
email when it's live (or if anything needs fixing — usually the data-safety
form or the privacy URL).

---

## Updating the app later (the whole loop)

```
1. Make changes  →  2. bump versionCode  →  3. gradlew.bat bundleRelease
→  4. Play Console → Production → New release → upload new .aab → rollout
```

Users get the update automatically within a day or so.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `bundleRelease` builds but Play says “unsigned” | `keystore.properties` missing or wrong path/passwords — Phase 1 |
| “Version code already used” | bump `versionCode` in app/build.gradle.kts |
| Rejected: privacy policy | URL must open publicly (test in an incognito window) |
| Rejected: data safety mismatch | re-check every answer is “No collection / No sharing” |
| Families policy questions | this app has no ads/purchases/links/data — answer accordingly and, if asked, point to the privacy policy |
| Lost keystore | with Play App Signing you can ask Google to reset the upload key; without it, the app can never be updated — **back the file up now** |

---

## Quick checklist

- [ ] Play Console account created & verified ($25)
- [ ] `popgrow-release.jks` created and **backed up**
- [ ] `keystore.properties` created (never committed)
- [ ] `versionCode` correct
- [ ] `gradlew.bat bundleRelease` → `.aab` built
- [ ] Release build tested on a real phone (`installRelease`)
- [ ] PRIVACY.md live at a public URL
- [ ] All “App content” forms done (ads: no, data: none, audience: 5 & under)
- [ ] Store listing text + icon 512 + feature graphic 1024×500 + 2 screenshots
- [ ] Production release rolled out 🎉
