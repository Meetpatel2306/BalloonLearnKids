# Balloon Pop & Learn 🫧🌼

A bubble-popping music toy for children aged roughly **2 to 5**. Pop a bubble,
hear a note, grow a flower. That is the entire game.

Play alone, or hand the tablet to two children at once — the screen splits into
two gardens that never interfere with each other.

**Native Android · Kotlin · Jetpack Compose · 100% offline · no ads · no
analytics · no internet permission · ~790 KB**

---

## Table of contents

- [How to run it — simple steps](#how-to-run-it--simple-steps)
- [What it is](#what-it-is)
- [Design principles](#design-principles)
- [Requirements](#requirements)
- [Quick start — run it in three commands](#quick-start--run-it-in-three-commands)
- [Every command you need](#every-command-you-need)
- [Running on a device](#running-on-a-device)
- [I have no Android device](#i-have-no-android-device)
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

## How to run it — simple steps

Follow these in order. Five steps, about 15 minutes the first time.

> **An Android app cannot run inside a terminal.** The terminal only *builds*
> it. The game itself needs an Android screen — a phone, a tablet, or an
> emulator. Step 3 is where you choose one.

---

### Step 1 — Install two things

You need **JDK 17** and the **Android SDK**. Nothing else.

<details open>
<summary><b>🐧 Linux</b></summary>

```bash
# JDK 17
mkdir -p ~/toolchain && cd ~/toolchain
curl -L -o jdk17.tar.gz "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_linux_hotspot_17.0.13_11.tar.gz"
tar xzf jdk17.tar.gz && mv jdk-17* jdk17

# Android SDK
curl -L -o cmdtools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
mkdir -p ~/Android/Sdk/cmdline-tools
unzip -q cmdtools.zip -d /tmp/cmdt
mv /tmp/cmdt/cmdline-tools ~/Android/Sdk/cmdline-tools/latest

# SDK packages (say yes to the licences)
export JAVA_HOME=~/toolchain/jdk17
yes | ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

No `sudo` needed — everything goes in your home folder.
</details>

<details>
<summary><b>🪟 Windows</b></summary>

The easy way is one installer that brings both:

1. Download **Android Studio** from <https://developer.android.com/studio>
2. Run it, accept the defaults, let it finish downloading the SDK
3. Open Studio → **More Actions → SDK Manager** → tick **Android 16 (API 36)** → Apply

That gives you the SDK at `%LOCALAPPDATA%\Android\Sdk` and a bundled JDK 17.

Prefer no IDE? Install JDK 17 from <https://adoptium.net> and the
"Command line tools only" package from the Android Studio download page.
</details>

---

### Step 2 — Open a terminal in the project and set it up

<details open>
<summary><b>🐧 Linux</b></summary>

```bash
cd ~/Music/PopAndGrow
source env.sh
```
</details>

<details>
<summary><b>🪟 Windows</b></summary>

Open **Command Prompt** (not PowerShell) in the project folder, then:

```bat
cd C:\path\to\PopAndGrow
env.bat
```

If it says `java : NOT FOUND`, open `env.bat` in Notepad and correct the
`JAVA_HOME` line to wherever your JDK 17 actually is.
</details>

Either one should print three lines:

```
java : openjdk version "17.0.13" ...
adb  : Android Debug Bridge version 1.0.41
sdk  : /home/you/Android/Sdk
```

**If `java` shows 11 or 21, stop here and fix it** — the build will fail
otherwise. You must run this in *every new terminal window*, or add the same
lines to your `~/.bashrc` (Linux) or system environment variables (Windows) to
make them permanent.

---

### Step 3 — Get an Android screen

Pick **one**.

#### 3a. Phone by USB cable — easiest

1. On the phone: **Settings → About phone → tap "Build number" 7 times**
2. **Settings → Developer options → USB debugging → on**
3. Plug the phone into the computer
4. The phone shows *"Allow USB debugging?"* → tap **Allow**
5. Check the computer can see it:
   ```bash
   adb devices
   ```
   You want a line ending in `device`. If it says `unauthorized`, unplug,
   replug and accept the prompt. If nothing appears, your cable may be
   charge-only — try another one.

#### 3b. Phone over Wi-Fi — no cable needed

Needs Android 11+, with the phone on the **same Wi-Fi network** as the computer.

1. Enable Developer options as above
2. **Settings → Developer options → Wireless debugging → on**
3. Tap the words *"Wireless debugging"* to open its screen
4. Tap **"Pair device with pairing code"** — a popup shows two things:
   ```
   Wi-Fi pairing code:  483726
   IP address & Port:   192.168.1.77:37419
   ```
5. In your terminal, typing **your own numbers, not these**:
   ```bash
   adb pair 192.168.1.77:37419
   ```
   It asks `Enter pairing code:` → type `483726` → Enter
6. **Close the popup.** The main *Wireless debugging* screen shows a
   **different** IP address & Port. Connect to that one:
   ```bash
   adb connect 192.168.1.77:41235
   adb devices
   ```

> ⚠️ The **pair** port and the **connect** port are two different random
> numbers. Do not reuse the first one.

#### 3c. Emulator — no phone at all

See [I have no Android device](#i-have-no-android-device). It needs hardware
virtualisation switched on in your computer's BIOS, so it is the slowest option
to set up — but once done it needs no phone.

---

### Step 4 — Build and install

Same command on both systems:

```bash
./gradlew installDebug          # Linux
gradlew.bat installDebug        # Windows
```

First run downloads Gradle and the dependencies (a few minutes). Later runs take
seconds. When it prints `BUILD SUCCESSFUL`, the app is on your device.

---

### Step 5 — Play

Find the **Balloon Pop & Learn** icon in the phone's app drawer and tap it. Or launch it
from the terminal:

```bash
adb shell am start -n com.meetpatel.popgrow.debug/com.meetpatel.popgrow.MainActivity
```

Tap **1 Player** or **2 Players** and start popping.

To leave a game, **press and hold** the small house button in the top-left
corner for about a second — a ring fills up as you hold. A quick tap does
nothing, which is the point: a toddler cannot get out by accident.

---

### If something goes wrong

| Message | Meaning | Fix |
|---|---|---|
| `No connected devices!` | Nothing to install onto | Do Step 3 |
| `adb server version (41) doesn't match this client (39)` | Two adb versions installed | Re-run Step 2; on Ubuntu also `sudo apt remove android-tools-adb` |
| `invalid source release: 17` | Wrong Java | Re-run Step 2 and check it prints 17 |
| `SDK location not found` | Gradle cannot find the SDK | Re-run Step 2, or `echo "sdk.dir=$HOME/Android/Sdk" > local.properties` |
| `cd: PopAndGrow: No such file or directory` | You are already inside the folder | Skip the `cd`, or use the full path |
| `INSTALL_PARSE_FAILED_NO_CERTIFICATES` | You used the release APK, which is unsigned | Use `installDebug`, or set up [signing](#signing-a-release-build) |

More in [Troubleshooting](#troubleshooting).

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

From inside the project directory, with a phone connected and USB debugging on:

```bash
source env.sh                 # Windows: env.bat  — sets JAVA_HOME, ANDROID_HOME, PATH
./gradlew installDebug        # Windows: gradlew.bat installDebug
adb shell am start -n com.meetpatel.popgrow.debug/com.meetpatel.popgrow.MainActivity
```

That is it. The app is now running on your phone.

New to this? Use [How to run it — simple steps](#how-to-run-it--simple-steps)
instead, which walks through installing the tools and connecting a phone.

**No device connected?** `installDebug` fails with `No connected devices!`.
See [I have no Android device](#i-have-no-android-device) for the three ways to
get a screen to run on — including turning this machine into one.

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

## I have no Android device

Three ways to get a screen, best first.

### 1. Wireless debugging — your own phone, no cable (works in 2 minutes)

Needs Android 11 or newer, with the phone on the same Wi-Fi as this machine.

**On the phone:**

1. **Settings → Developer options → Wireless debugging → on**
   (no Developer options? Settings → About phone → tap **Build number** seven times)
2. Tap the words *"Wireless debugging"* to open its screen
3. Tap **"Pair device with pairing code"**

The popup shows two things — a 6-digit code and an `IP address & Port`:

```
Wi-Fi pairing code:  483726
IP address & Port:   192.168.1.77:37419
```

**In the terminal**, using your own numbers:

```bash
source env.sh
adb pair 192.168.1.77:37419      # then type 483726 at the prompt
```

Now **close the popup**. The main *Wireless debugging* screen shows a
**different** `IP address & Port` — that is the one to connect to:

```bash
adb connect 192.168.1.77:41235
adb devices                       # should now list the phone
./gradlew installDebug
```

> ⚠️ There are **two different ports**. The pairing port is random and only
> appears in the popup; the connection port is a different random number on the
> main screen. It is *not* 5555 on modern Android — that was the old
> `adb tcpip 5555` flow, which is a different thing entirely.

The phone must be on the same subnet as this machine. Check with
`ip -4 addr show scope global` here and compare against the phone's IP —
`192.168.1.43` and `192.168.1.77` match; `192.168.1.x` and `10.x.x.x` do not.

A USB cable works too, if you have one: enable **USB debugging**, plug in, accept
the prompt on the phone, then `adb devices`.

### 2. Emulator — needs VT-x turned on in this machine's BIOS

The emulator needs hardware virtualisation. Check:

```bash
ls /dev/kvm && echo OK || dmesg | grep -i "disabled by BIOS"
```

If that reports **`VMX (outside TXT) disabled by BIOS`**, the CPU supports it but
the firmware has it switched off. Fix it once:

1. Reboot and press <kbd>Del</kbd> or <kbd>F2</kbd> to enter BIOS/UEFI setup
2. **Advanced → CPU Configuration → Intel Virtualization Technology → Enabled**
   (sometimes under "Intel VT-x", "SVM Mode" on AMD, or a "Security" tab)
3. Save and exit

Then, back in Linux:

```bash
sudo modprobe kvm_intel          # usually loads automatically after the reboot
sudo usermod -aG kvm "$USER"     # log out and back in for this to take effect
ls -l /dev/kvm                   # should now exist
```

Now create and boot a tablet — two-player mode is designed for that shape:

```bash
source env.sh
sdkmanager "system-images;android-36;google_apis;x86_64" "emulator"
avdmanager create avd -n popgrow \
  -k "system-images;android-36;google_apis;x86_64" -d "pixel_tablet"
emulator -avd popgrow &
./gradlew installDebug
```

That needs roughly 10 GB of free disk and a graphical session (this project was
built on X11 `DISPLAY=:1`, which is fine).

> Without KVM the emulator still starts with `-no-accel`, but it software-emulates
> every instruction. Android takes 15+ minutes to boot and the game runs at a few
> frames per second — useless for judging a 60 fps animation. Don't bother.

### 3. Firebase Test Lab — real devices in the cloud, free tier

Upload the APK and it runs on physical hardware, recording video and a
screenshot. No local virtualisation needed. Free tier allows a handful of runs
per day. Requires a Google account and internet.

### A note on two-player mode

A mouse gives you exactly one pointer, so simultaneous two-player tapping
**cannot** be tested on an emulator or over scrcpy. That one needs a real
touchscreen with two real fingers on it.

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
├── env.sh                            Linux/macOS: sets JAVA_HOME, ANDROID_HOME, PATH
├── env.bat                           Windows: the same
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

**`adb: no devices/emulators found` or `No connected devices!`**
Nothing is plugged in and no emulator is running. See
[I have no Android device](#i-have-no-android-device). If a phone *is*
connected: USB debugging is off, the cable is charge-only, or you did not accept
the authorisation prompt. Try `adb kill-server && adb devices`.

**`adb server version (41) doesn't match this client (39); killing...`**
Two different `adb` binaries. Ubuntu's `android-tools-adb` package installs
1.0.39 at `/usr/bin/adb`, while the SDK ships 1.0.41. `source env.sh` puts the
SDK one first on `PATH` and fixes it. To confirm:
```bash
adb version          # want 1.0.41
which -a adb         # SDK platform-tools must come before /usr/bin
```
Optionally remove the stale one: `sudo apt remove android-tools-adb`.

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
