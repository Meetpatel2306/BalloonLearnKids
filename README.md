# Pop & Grow 🫧🌼

A bubble-popping music toy for children aged roughly **2 to 5**. Pop a bubble,
hear a note, grow a flower. That is the entire game.

Play alone, or hand the tablet to two children at once — the screen splits into
two gardens that never interfere with each other.

**Native Android · Kotlin · Jetpack Compose · 100% offline · no ads · no
analytics · no internet permission · ~790 KB**

---

## Table of contents

- [What it is](#what-it-is)
- [Design principles](#design-principles)
- [Requirements](#requirements)
- [Quick start — run it in three commands](#quick-start--run-it-in-three-commands)
- [Every command you need](#every-command-you-need)
- [Running on a device](#running-on-a-device)
- [Running on an emulator](#running-on-an-emulator)
- [Signing a release build](#signing-a-release-build)
- [Free distribution](#free-distribution)
- [Project layout](#project-layout)
- [How it works](#how-it-works)
- [Tests](#tests)
- [Tuning the game](#tuning-the-game)
- [Compliance notes](#compliance-notes)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)
- [License](#license)

---

## What it is

Bubbles with faces drift up a slowly shifting sky. Tapping one makes it burst
into confetti, play a note, buzz the device gently, and plant a flower in the
garden along the bottom. Every ten flowers, a butterfly wanders across.

There is no score, no timer, no level, no lose condition, no text to read and
no way to get anything wrong.

| Mode | What happens |
|---|---|
| **1 player** | Full screen, nine bubbles, one garden. |
| **2 players** | Screen splits down the middle. Each side has its own colours (warm vs cool) and its own garden. Both children can tap at the same instant and both pops register. |

---

## Design principles

Every one of these is a deliberate constraint, taken from research on how
2–5 year olds actually use touchscreens:

| Principle | How it shows up in the code |
|---|---|
| **No drag gestures.** Under-5s lose finger contact mid-drag. | Tap is the *only* input in the app. |
| **2 cm minimum touch target.** | Bubble radius is 44–74 dp plus 20 dp of tap slop — always over the 2 cm (~126 dp) target. Enforced by a unit test. |
| **Slow reaction times.** | Pops fire on finger-**down**, not on tap-up, so a press-and-hold still works. |
| **Never punish.** | A bubble that escapes off the top is silently replaced. Tapping empty sky does nothing — no buzzer, no red X, no shake. |
| **No reading required.** | The menu is two bubbles with faces: one face = solo, two faces = together. Text is there for the adult. |
| **Random input must sound good.** | Notes come from a C-major pentatonic scale, which has no dissonant intervals. Mashing bubbles produces melody, not noise. |
| **Cause and effect.** | Bigger bubble → lower note. Size and pitch move together, which is a real physical intuition a child absorbs without explanation. |
| **A child cannot exit by accident.** | Leaving a game needs an 800 ms press-and-hold on a small corner button. Settings need a 900 ms hold. The nav bar is hidden. |
| **Faces beat shapes.** | Every bubble has eyes and a smile. Toddlers reach for faces first. |

---

## Requirements

### To play

- Android **7.0 (API 24)** or newer
- A touchscreen
- ~10 MB free space
- **No internet connection, ever** — not at install, not at first run, not after

### To build

| Tool | Version | Notes |
|---|---|---|
| **JDK** | **17** | Required. JDK 11 and 21 will not work with this AGP. |
| **Android SDK** | Platform 36, Build-Tools 36.0.0 | Gradle installs Build-Tools automatically; you need the platform. |
| **Gradle** | 8.14.3 | **Do not install it** — `./gradlew` downloads it for you. |
| **Android Studio** | Optional | Ladybug or newer if you want the IDE. Not needed to build. |
| Disk | ~4 GB | For the SDK plus the Gradle cache. |

The build pins its own versions in `gradle/libs.versions.toml`:

```
Android Gradle Plugin  8.13.2
Kotlin                 2.2.0
Compose BOM            2025.09.00
compileSdk / targetSdk 36
minSdk                 24
```

---

## Quick start — run it in three commands

If you already have a JDK 17 and the Android SDK, and a phone plugged in with
USB debugging on:

```bash
cd PopAndGrow
./gradlew installDebug
adb shell am start -n com.meetpatel.popgrow.debug/com.meetpatel.popgrow.MainActivity
```

That is it. The app is now running on your phone.

### Starting from absolutely nothing (Linux, no sudo, no Android Studio)

Copy-paste this whole block. It downloads a JDK and the Android SDK into your
home folder, builds the APK, and leaves it in `app/build/outputs/`.

```bash
# 1. JDK 17
mkdir -p ~/toolchain && cd ~/toolchain
curl -L -o jdk17.tar.gz \
  "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_linux_hotspot_17.0.13_11.tar.gz"
tar xzf jdk17.tar.gz && mv jdk-17* jdk17
export JAVA_HOME=~/toolchain/jdk17
export PATH=$JAVA_HOME/bin:$PATH

# 2. Android SDK command-line tools
curl -L -o cmdtools.zip \
  "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
mkdir -p ~/Android/Sdk/cmdline-tools
unzip -q cmdtools.zip -d /tmp/cmdt
mv /tmp/cmdt/cmdline-tools ~/Android/Sdk/cmdline-tools/latest
export ANDROID_HOME=~/Android/Sdk
export PATH=$ANDROID_HOME/platform-tools:$PATH

# 3. SDK packages (accept the licences)
yes | ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager \
  "platform-tools" "platforms;android-36" "build-tools;36.0.0"

# 4. Build
cd /path/to/PopAndGrow
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
./gradlew assembleDebug
```

The installable APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

> **macOS:** identical, but swap the JDK URL for the `mac` build (or use
> `brew install --cask temurin@17`) and the SDK URL for
> `commandlinetools-mac-11076708_latest.zip`.
>
> **Windows:** use `gradlew.bat` instead of `./gradlew`, and install JDK 17 plus
> the Android SDK through Android Studio.

---

## Every command you need

Run all of these from the project root. Set `JAVA_HOME` and `ANDROID_HOME` first
if they are not already in your environment.

### Build

| Command | What it does |
|---|---|
| `./gradlew assembleDebug` | Installable debug APK → `app/build/outputs/apk/debug/app-debug.apk` |
| `./gradlew assembleRelease` | Optimised release APK → `app/build/outputs/apk/release/` (unsigned unless you set up signing) |
| `./gradlew bundleRelease` | Android App Bundle (`.aab`) for Google Play |
| `./gradlew build` | Everything: compiles, tests and lints all variants |
| `./gradlew clean` | Delete all build output |

### Install and run

| Command | What it does |
|---|---|
| `./gradlew installDebug` | Build and install onto the connected device |
| `./gradlew uninstallDebug` | Remove it again |
| `adb devices` | List connected devices (should show one, not `unauthorized`) |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | Install an APK by hand |
| `adb shell am start -n com.meetpatel.popgrow.debug/com.meetpatel.popgrow.MainActivity` | Launch it |
| `adb logcat -s PopAndGrow AndroidRuntime` | Watch for crashes |

> The debug build installs as `com.meetpatel.popgrow.debug` so it can sit
> side-by-side with a release install. Drop the `.debug` suffix in the launch
> command when running a release build.

### Test and check

| Command | What it does |
|---|---|
| `./gradlew testDebugUnitTest` | Run the 13 game-logic unit tests (no device needed) |
| `./gradlew test` | Unit tests for every variant |
| `./gradlew lintDebug` | Android lint → HTML report at `app/build/reports/lint-results-debug.html` |
| `./gradlew dependencies` | Print the full dependency tree |

### Inspect the APK

| Command | What it does |
|---|---|
| `unzip -l app/build/outputs/apk/release/*.apk` | List everything inside the APK |
| `$ANDROID_HOME/build-tools/36.0.0/aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk` | Show the manifest, permissions and SDK levels |

---

## Running on a device

1. On the phone or tablet: **Settings → About phone → tap "Build number" seven
   times** to unlock Developer options.
2. **Settings → Developer options → USB debugging → on.**
3. Plug it in over USB and accept the "Allow USB debugging?" prompt.
4. Check it is visible:
   ```bash
   adb devices
   ```
   You want `device`, not `unauthorized` (re-accept the prompt) and not
   `offline` (try `adb kill-server && adb devices`).
5. Install and launch:
   ```bash
   ./gradlew installDebug
   adb shell am start -n com.meetpatel.popgrow.debug/com.meetpatel.popgrow.MainActivity
   ```

**Without a cable:** build the APK, copy `app-debug.apk` onto the device however
you like, tap it in a file manager, and allow "install from unknown sources"
when prompted.

A tablet in landscape is by far the best way to experience two-player mode.

---

## Running on an emulator

An emulator needs hardware virtualisation (KVM on Linux, HAXM/Hypervisor on
Windows, native on Apple Silicon). Check Linux support with
`ls /dev/kvm` — if that file is missing, the emulator will be far too slow to
judge the game and you should use a real device.

```bash
# Install an image (x86_64 on Intel/AMD, arm64-v8a on Apple Silicon)
sdkmanager "system-images;android-36;google_apis;x86_64" "emulator"

# Create a tablet AVD — the two-player mode is designed for this shape
avdmanager create avd -n popgrow -k "system-images;android-36;google_apis;x86_64" \
  -d "pixel_tablet"

# Boot it
$ANDROID_HOME/emulator/emulator -avd popgrow &

# Then install as usual
./gradlew installDebug
```

Note that a mouse gives you one pointer, so you cannot test simultaneous
two-player tapping on an emulator. That needs a real touchscreen.

---

## Signing a release build

`assembleRelease` produces an **unsigned** APK by default, so a fresh clone
builds for anyone with no secrets. Android will not install an unsigned APK — to
get an installable release build, create a key once:

```bash
keytool -genkey -v \
  -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias popgrow
```

Then create `keystore.properties` in the project root:

```properties
storeFile=release.jks
storePassword=your-store-password
keyAlias=popgrow
keyPassword=your-key-password
```

`./gradlew assembleRelease` now signs automatically.

> ⚠️ `keystore.properties` and `*.jks` are already in `.gitignore`. **Never
> commit them, and back the keystore up somewhere safe** — losing it means you
> can never update the app on Google Play under the same listing.

---

## Free distribution

The app is free to build and free to give away. Your options, cheapest first:

### GitHub Releases — free, no account fees, works today

`.github/workflows/build.yml` is already set up. It runs tests and lint on every
push, and on a version tag it publishes the APKs as a downloadable GitHub
Release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Anyone can then download the APK from your Releases page and sideload it.

To have CI sign the release, add four repository secrets under
**Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 release.jks` |
| `KEYSTORE_PASSWORD` | your store password |
| `KEY_ALIAS` | `popgrow` |
| `KEY_PASSWORD` | your key password |

Without them the workflow still passes and publishes the debug APK.

### F-Droid — free, and a real store listing

F-Droid accepts open-source apps at no cost. This project already satisfies the
requirements: MIT licensed, no proprietary dependencies, no tracking, and a
reproducible Gradle build. Submit a metadata file to
[fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata).

### Google Play — **US$25, one time**

Not free, but it is a single lifetime fee with no renewal. Upload
`./gradlew bundleRelease` output (`.aab`, not `.apk`). You will need to:

- complete the **Data safety** form — answer "no data collected" for everything
- opt into the **Families** programme and set the target age to **Ages 5 and under**
- host `PRIVACY.md` at a public URL and link it in the listing

### Amazon Appstore / Samsung Galaxy Store

Both are free to register and both accept sideload-style APK uploads. Worth
doing after Play or F-Droid, not before.

---

## Project layout

```
PopAndGrow/
├── app/
│   ├── build.gradle.kts              Module config, signing, dependencies
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   One permission: VIBRATE
│       │   ├── java/com/meetpatel/popgrow/
│       │   │   ├── MainActivity.kt       Immersive mode, screen routing
│       │   │   ├── Prefs.kt              Two booleans, nothing else
│       │   │   ├── Haptics.kt            The buzz on each pop
│       │   │   ├── audio/
│       │   │   │   └── ToneEngine.kt     Synthesises the pentatonic scale
│       │   │   ├── game/
│       │   │   │   ├── Entities.kt       Bubble, Flower, Particle, Butterfly
│       │   │   │   └── GameWorld.kt      All the rules — no Android imports
│       │   │   └── ui/
│       │   │       ├── Palette.kt        Every colour in the app
│       │   │       ├── WorldRenderer.kt  All drawing, pure vector
│       │   │       ├── HomeScreen.kt     Menu + parental gate + settings
│       │   │       ├── GameScreen.kt     Game loop and multi-touch input
│       │   │       └── HoldToConfirm.kt  The press-and-hold gate
│       │   └── res/                  Icon, strings, theme — no bitmaps
│       └── test/                     13 JVM unit tests
├── .github/workflows/build.yml       CI: test, lint, build, release
├── gradle/libs.versions.toml         Every dependency version
├── PRIVACY.md
├── LICENSE                           MIT
└── README.md
```

---

## How it works

**No assets.** There is not one PNG, JPEG, WAV or MP3 in the repository.

- **Graphics** are Compose `Canvas` primitives — circles, arcs, gradients and
  bezier paths — so the art is pin-sharp at any density and costs nothing in
  APK size.
- **Sound** is synthesised on first launch. `ToneEngine` renders ten sine-plus-
  harmonics tones with a soft attack and an exponential decay, writes them to
  the cache directory as WAV, and loads them into a `SoundPool`. That is why the
  release APK is under 800 KB.
- **The launcher icon** is an adaptive vector icon.

**The game loop** runs on `withFrameNanos`, which is Compose's frame clock.
Each frame advances `GameWorld` and bumps a counter that is read *inside the
draw lambda* — so every frame re-runs the draw phase only, never recomposition.
Frame deltas are clamped, so returning from the background never teleports the
world (there is a test for this).

**Multi-touch** is handled by inspecting every `PointerInputChange` on the
`Initial` pass rather than using `detectTapGestures`, which only tracks a single
pointer. This is what makes genuinely simultaneous two-player tapping work.

**`GameWorld` has no Android dependencies at all**, which is why the rules can
be tested on a plain JVM in about a second.

---

## Tests

```bash
./gradlew testDebugUnitTest
```

13 tests, all passing. They cover the game rules and, more importantly, the
*age-appropriateness* rules:

```
✓ bubbles spawn up to the target count
✓ two player mode fills both lanes
✓ two player bubbles never cross the divider
✓ every bubble is at least a 2cm touch target
✓ tapping empty sky does nothing at all
✓ a pop removes the bubble, plants a flower and returns a note
✓ a near miss still pops, thanks to tap slop
✓ bigger bubbles play lower notes
✓ an escaping bubble is replaced, never punished
✓ the garden stops growing instead of overflowing
✓ a butterfly arrives every ten flowers
✓ a long pause cannot teleport the world
✓ flowers land inside their own lane
```

If one of these fails, the app has stopped being suitable for a two-year-old —
not merely stopped working. Report at
`app/build/reports/tests/testDebugUnitTest/index.html`.

---

## Tuning the game

Almost everything you would want to adjust is a constant in the `companion
object` at the bottom of `game/GameWorld.kt`:

| Constant | Effect |
|---|---|
| `MIN_RADIUS_DP` / `MAX_RADIUS_SOLO_DP` | Bubble size. Raise for younger children. |
| `TAP_SLOP_DP` | How far outside a bubble still counts as a hit. |
| `RISE_MIN_DP` / `RISE_RANGE_DP` | How fast bubbles float up. |
| `targetBubbles` | How many bubbles are on screen at once. |
| `MAX_FLOWERS_PER_LANE` | Garden capacity before the oldest flower wilts. |
| `BUTTERFLY_EVERY` | Flowers needed to earn a butterfly. |
| `GROUND_FRACTION` | Where the horizon sits. |

Colours are all in `ui/Palette.kt`. The musical scale is `SCALE` in
`audio/ToneEngine.kt` — swapping it for a different pentatonic set changes the
mood of the whole game. Bump `CACHE_VERSION` in the same file after editing the
synthesis, or the old cached samples will be reused.

---

## Compliance notes

Relevant if you publish this rather than just installing it at home.

**Apple's Kids Category and Google Play's Families policy both forbid
third-party advertising and third-party analytics in apps aimed at young
children.** This app ships neither, plus:

- ✅ No `INTERNET` permission — the app is physically incapable of sending data
- ✅ No third-party SDKs whatsoever (AndroidX and Compose only)
- ✅ No in-app purchases, no external links, no social features
- ✅ Both adult-facing controls sit behind a press-and-hold parental gate
- ✅ Backup and device-transfer are disabled in `data_extraction_rules.xml`
- ✅ `PRIVACY.md` is written and ready to host

For Google Play, set the **target audience to "Ages 5 and under"** and answer
"no data collected" throughout the Data safety form. Note that Play requires a
recent `targetSdk`; this project is on 36.

None of this is legal advice — check the current policy text before you submit.

---

## Troubleshooting

**`SDK location not found`**
Create `local.properties` in the project root:
```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

**`Unsupported class file major version` / `invalid source release: 17`**
You are not on JDK 17. Check with `java -version`, then:
```bash
export JAVA_HOME=/path/to/jdk17
```

**`Failed to install ... INSTALL_PARSE_FAILED_NO_CERTIFICATES`**
You tried to install the unsigned release APK. Use the debug APK, or set up
[signing](#signing-a-release-build).

**`adb: no devices/emulators found`**
USB debugging is off, the cable is charge-only, or you did not accept the
authorisation prompt. Try `adb kill-server && adb devices`.

**No sound**
Check the device is not on silent, and check the in-app setting behind the
grown-ups gate. The first one or two pops after a cold install can be silent
while the samples finish decoding — that is expected and only happens once.

**Build hangs on "Downloading gradle-8.14.3-bin.zip"**
First build only; it is ~130 MB. Later builds use the cache.

**Gradle runs out of memory**
Raise the heap in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m
```

---

## Roadmap

Ideas that fit the design without complicating it for the child:

- [ ] A second room — a **peekaboo doors** screen — reached from the same menu.
  A hub of small rooms is what drives retention in this category; a single
  mechanic gets deleted within a week.
- [ ] Landscape *and* portrait layouts (portrait would suit solo play on a phone)
- [ ] A "calm" mode: fewer bubbles, slower rise, quieter palette, for bedtime
- [ ] Let a grown-up pick the musical scale (major pentatonic, minor pentatonic,
  Japanese *hirajōshi*)
- [ ] TalkBack labels on the two adult-facing controls

---

## License

MIT — see [LICENSE](LICENSE). Do whatever you like with it.
