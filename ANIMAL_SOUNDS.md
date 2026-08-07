# Adding real animal sounds

The Animals game already teaches every animal by speaking it clearly — *"Cow.
The cow says moo."* — using the phone's own voice. That works offline and needs
no files.

If you want **real recordings** (an actual moo, an actual woof), the app is
ready for them: drop the files in and it plays them automatically. Nothing in
the code needs changing.

---

## 1. Where to get sounds you're allowed to ship

Use only **public-domain / CC0** clips, or ones you record yourself. Do **not**
grab audio from YouTube, ringtone sites or Google Images — publishing those in
a Play Store app is copyright infringement, and Google removes apps for it.

Safe sources, all free:

| Site | What to know |
|---|---|
| **freesound.org** | Huge library. Filter licence → **"Creative Commons 0"**. Free account needed to download. |
| **pixabay.com/sound-effects** | Free for commercial use, no attribution needed. Search "cow moo". |
| **mixkit.co/free-sound-effects/animals** | Free licence, no account required. |
| **BBC Sound Effects** (sound-effects.bbcrewind.co.uk) | Free for personal/education use — check terms before commercial release. |
| **Your own phone** | Best of all if you can visit a farm or a pet — record with the voice recorder app. |

> Tip: pick clips that are **short (0.5–2 seconds)** and start immediately.
> A toddler loses the connection if the sound arrives late.

## 2. Name the files exactly like this

Put them in **`app/src/main/res/raw/`** (create the `raw` folder if it isn't
there). Lower-case names, no spaces, no capital letters, no dashes:

```
animal_cat.ogg        animal_lion.ogg
animal_dog.ogg        animal_monkey.ogg
animal_cow.ogg        animal_elephant.ogg
animal_pig.ogg        animal_frog.ogg
animal_duck.ogg       animal_fish.ogg
animal_rabbit.ogg     animal_bird.ogg
animal_bear.ogg       animal_horse.ogg
```

`.ogg`, `.mp3` and `.wav` all work. `.ogg` is smallest — free converters like
Audacity (audacityteam.org) export it.

**You don't need all fourteen.** Add whichever you have; any animal without a
file simply uses the spoken version instead.

## 3. Build and enjoy

```bat
update.bat
```

Find the animal in the game — you'll hear the real recording, then the app
says the animal's name after it.

## 4. Keep a note of the licences

Before publishing, list where each sound came from — either in this file or a
`CREDITS.md`. CC0 needs no attribution, but a record protects you if anyone
ever asks.

### Sounds currently shipping

These fourteen were downloaded from **Pixabay** (their file names carry the
uploader and the Pixabay ID). Pixabay's Content Licence allows commercial use
with no attribution required — but **verify each one on pixabay.com before you
publish**, since a contributor can change or withdraw a file.

| Animal | File | Uploader / Pixabay ID |
|---|---|---|
| Cat | animal_cat.mp3 | dragon-studio — 401729 |
| Dog | animal_dog.mp3 | dragon-studio — 419014 |
| Cow | animal_cow.mp3 | u_jd81cxyq22 — 343423 |
| Pig | animal_pig.mp3 | freesound_community — 40664 |
| Duck | animal_duck.mp3 | freesound_community — 96140 |
| Rabbit | animal_rabbit.mp3 | freesound_community — 39519 |
| Bear | animal_bear.mp3 | universfield — 191995 |
| Lion | animal_lion.mp3 | ribhavagrawal — 293295 |
| Monkey | animal_monkey.mp3 | koiroylers — 351933 |
| Elephant | animal_elephant.mp3 | u_tococonino969 — 225994 |
| Frog | animal_frog.mp3 | dragon-studio — 322956 |
| Fish | animal_fish.mp3 | dragon-studio — 406647 (bubbles) |
| Bird | animal_bird.mp3 | yuliana-yurukova — 343624 |
| Horse | animal_horse.mp3 | dragon-studio — 390297 |

> Playback is capped at **2 seconds** in code (`AnimalVoices.MAX_MS`), so long
> clips like the bird chirps are cut short automatically — the files themselves
> are untouched.

---

**Why the app doesn't download them itself:** the app has no internet
permission at all — that's the promise made to parents in PRIVACY.md, and it's
what makes it safe for children. Sounds ship inside the app or not at all.
