# Adding ads to Balloon Learn Kids

Everything needed to put AdMob into this app later, in the order it has to happen.
Written for the plan you described: an ad around **Animals mode**.

> **Read the warning in §1 first.** One half of that plan is fine, the other half
> is the single most common reason children's apps get suspended.

---

## 1. What a children's app is allowed to do

Balloon Learn Kids declares its target audience as **age 5 and under**, so it is
bound by the Google Play **Families policy**, not just the ordinary ads policy.
That is much stricter, and the rules are enforced by suspension rather than a
warning.

### Allowed

| | |
|---|---|
| Banner on the **menu** | ✅ Safest placement of all |
| Interstitial at a **genuine break** — the moment a set is finished, before the "Play again / Menu" card | ✅ Acceptable |
| Non-personalised ads only | ✅ Required |
| G-rated ad content only | ✅ Required |
| Google-certified ad SDK (AdMob qualifies) | ✅ Required |

### Forbidden — any one of these can get the app removed

| | |
|---|---|
| An ad **when a mode starts** | ❌ This is "an ad before content", the classic violation |
| An ad at app launch / on the splash | ❌ |
| Rewarded or incentivised ads ("watch to continue") | ❌ Not for under-13 |
| Ads **during** play | ❌ |
| Anything a child cannot dismiss immediately | ❌ |
| Personalised / interest-based ads | ❌ |
| Collecting the advertising ID | ❌ |

### So, about "start and end of Animals mode"

- **End of the set — do it.** The child has finished all 14 animals, the world is
  frozen, the celebration is over and they are about to choose "Play again" or
  "Menu". That is a real break in the experience, which is exactly what the
  policy asks for.
- **Start of the mode — do not.** An ad between tapping the Animals balloon and
  the game appearing is an ad shown before the content the child asked for. It
  is disruptive by definition, a three-year-old cannot read a close button, and
  it is the pattern reviewers look for in this category.

**Recommended design: one interstitial, at the end of a completed set, at most
once every few minutes.** Everything below assumes that.

---

## 2. Set up AdMob (15 minutes, do this first)

1. Go to <https://admob.google.com> and sign in as `learnkidsballoon@gmail.com`.
2. **Apps → Add app → Android → Yes, it's on Google Play** → search for
   `com.meetpatel.balloonlearnkids`.
3. Copy the **App ID**. It looks like `ca-app-pub-0000000000000000~0000000000`
   (note the `~`).
4. **Ad units → Add ad unit → Interstitial**, name it `animals-set-complete`.
   Copy the **Ad unit ID** — `ca-app-pub-0000000000000000/0000000000` (a `/`).
5. **App settings → COPPA / child-directed** → mark the app as directed to
   children. This is separate from the code flag and both are needed.
6. **Payments** → add PAN and bank details. Payout threshold is $100.

Keep both IDs out of the repository — see §4.

---

## 3. Code changes

### 3.1 Permissions

`app/src/main/AndroidManifest.xml`

```xml
<!-- Ads need the network. This is the one thing the app currently promises
     it does not have, so the privacy policy has to change with it. -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Children's apps must NOT collect the advertising ID. The Play services
     library adds this permission itself, so remove it explicitly. -->
<uses-permission
    android:name="com.google.android.gms.permission.AD_ID"
    tools:node="remove" />
```

Inside `<application>`:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="${admobAppId}" />
```

### 3.2 Dependency

`gradle/libs.versions.toml`

```toml
[versions]
playServicesAds = "24.0.0"   # check for the current version

[libraries]
play-services-ads = { module = "com.google.android.gms:play-services-ads", version.ref = "playServicesAds" }
```

`app/build.gradle.kts`

```kotlin
dependencies {
    implementation(libs.play.services.ads)
}
```

### 3.3 Initialise, child-directed, in `MainActivity.onCreate`

```kotlin
// Non-personalised, G-rated, child-directed. All three are required; setting
// only one of them is a policy violation, not a preference.
MobileAds.setRequestConfiguration(
    RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(
            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
        )
        .setTagForUnderAgeOfConsent(
            RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
        )
        .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
        .build()
)
MobileAds.initialize(this)
```

### 3.4 A small holder class

`app/src/main/java/com/meetpatel/balloonlearnkids/ads/Interstitial.kt`

```kotlin
/**
 * One interstitial, shown only when a whole set has been finished, and never
 * more than once every few minutes. Loads the next one straight after so the
 * child never waits on a spinner.
 */
class Interstitial(private val context: Context, private val unitId: String) {

    private var ad: InterstitialAd? = null
    private var lastShown = 0L

    /** Do not interrupt more often than this, however fast the child plays. */
    private val minGapMs = 4 * 60 * 1000L

    fun preload() {
        if (ad != null) return
        InterstitialAd.load(
            context, unitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) { ad = loaded }
                override fun onAdFailedToLoad(error: LoadAdError) { ad = null }
            }
        )
    }

    /** True if an ad was actually shown. Offline, this simply returns false and
     *  the game carries on exactly as it does today. */
    fun showIfDue(activity: Activity): Boolean {
        val now = SystemClock.elapsedRealtime()
        val ready = ad ?: run { preload(); return false }
        if (now - lastShown < minGapMs) return false

        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                preload()
            }
        }
        ready.show(activity)
        lastShown = now
        return true
    }
}
```

### 3.5 Where to call it

In `GameScreen.kt`, the set-completion effect already exists:

```kotlin
LaunchedEffect(completePending) {
    if (completePending) {
        music.playFanfare()
        ...
        repeat(4) { i -> world.celebrate(); delay(1100) }

        // The party is over and the child is about to be offered
        // "Play again / Menu". This is the natural break.
        if (mode == GameMode.ANIMALS) interstitial.showIfDue(activity)

        showComplete = true
        completePending = false
    }
}
```

Call `interstitial.preload()` when the mode opens, so the ad is ready by the
time the set ends.

**Do not** add anything to the branch that starts a mode.

---

## 4. Keeping the IDs out of git

Same pattern as the signing keys. In `keystore.properties` (already gitignored):

```properties
admobAppId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
admobInterstitial=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
```

In `app/build.gradle.kts`:

```kotlin
defaultConfig {
    // Google's public test IDs are the fallback, so a fresh clone still builds
    // and never accidentally serves a real ad during development.
    manifestPlaceholders["admobAppId"] =
        keystoreProps.getProperty("admobAppId") ?: "ca-app-pub-3940256099942544~3347511713"
    buildConfigField(
        "String", "ADMOB_INTERSTITIAL",
        "\"${keystoreProps.getProperty("admobInterstitial") ?: "ca-app-pub-3940256099942544/1033173712"}\""
    )
}
```

---

## 5. Testing

Always test with Google's **test unit IDs** first:

| Type | Test ID |
|---|---|
| App ID | `ca-app-pub-3940256099942544~3347511713` |
| Interstitial | `ca-app-pub-3940256099942544/1033173712` |

⚠️ **Never tap your own live ads.** Google reads it as click fraud and closes
accounts for it. Add your phone as a test device:

```kotlin
RequestConfiguration.Builder()
    .setTestDeviceIds(listOf("YOUR_DEVICE_HASH"))   // printed in logcat
```

Check all of these before release:

- [ ] Ad shows **only** after a completed Animals set
- [ ] Never on launch, never mid-play, never on the menu balloons
- [ ] Closing it returns straight to the "Play again / Menu" card
- [ ] **Aeroplane mode**: no ad, no error, no crash, game plays normally
- [ ] Rapid replay: the 4-minute gap is respected
- [ ] Back button during the ad does not break the app

---

## 6. Declarations that must change in the same release

Shipping an ad build while these still say "no ads" is a policy violation.
All of it moves together, and the declaration must be updated **before** the
build rolls out.

| Where | Change to |
|---|---|
| Play Console → App content → **Ads** | Yes, my app contains ads |
| Play Console → **Data safety** | Now collects: Device or other IDs, App activity — purpose Advertising |
| Play Console → Target audience → ads questions | Yes, ads are shown to children |
| `docs/privacy.html` | Describe AdMob, what it collects, link to Google's policy |
| `docs/terms.html` | §5 currently says "contains no advertising" — rewrite |
| `docs/index.html` | The "What is not in it" card says no adverts |
| Store listing description | Currently says "No advertising of any kind" |
| `app/.../strings.xml` | `privacy_full` and `terms_full` say the same thing |

---

## 7. What to expect financially

Non-personalised, G-rated inventory aimed at under-fives is the lowest-paying
segment on the platform. One interstitial per completed set, capped at one per
four minutes, is a low-volume placement by design.

| Daily active children | Realistic monthly |
|---|---|
| 100 | ₹30 – ₹120 |
| 1,000 | ₹300 – ₹1,200 |
| 10,000 | ₹3,000 – ₹12,000 |

It is worth doing once the app has users. It is not worth risking the listing
for before then.

---

## 8. Order of work

1. Publish the current ad-free version and get through production review.
2. Create the AdMob account and app.
3. Implement §3 with **test IDs**, run the §5 checklist.
4. Update every item in §6.
5. Swap in the real IDs, upload, send for review.

Never do 4 and 5 in the wrong order.
