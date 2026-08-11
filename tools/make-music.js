/*
 * Writes the game's background music straight into app/src/main/res/raw as
 * 16-bit stereo WAV, so the app ships real recorded-quality audio and still
 * needs no network and no third-party library.
 *
 *   node tools/make-music.js
 *
 * Everything here is additive synthesis: sine partials shaped by an envelope,
 * a little detune for width, and a simple delay network for room. The tunes are
 * built from a pentatonic scale, which is the same trick the in-app tone engine
 * uses — a toddler mashing anything can never produce a sour note.
 */

const fs = require('fs');
const path = require('path');

const SR = 44100;
const OUT = path.join(__dirname, '..', 'app', 'src', 'main', 'res', 'raw');

// ---------------------------------------------------------------- note tables

const A4 = 440;
const NAMES = { C: -9, D: -7, E: -5, F: -4, G: -2, A: 0, B: 2 };

/** "C4" / "F#5" -> frequency in Hz. */
function hz(note) {
  const m = /^([A-G])(#?)(-?\d)$/.exec(note);
  const semis = NAMES[m[1]] + (m[2] ? 1 : 0) + (parseInt(m[3], 10) - 4) * 12;
  return A4 * Math.pow(2, semis / 12);
}

// ------------------------------------------------------------------ voices

/**
 * A soft mallet: a few partials that decay quickly, like a marimba or a music
 * box. Warm, and it never sounds harsh at toddler volume.
 */
function mallet(buf, tStart, dur, freq, gain, pan) {
  const n = Math.floor(dur * SR);
  const i0 = Math.floor(tStart * SR);
  const partials = [
    { m: 1, a: 1.0, d: 1.0 },
    { m: 2.0, a: 0.34, d: 1.7 },
    { m: 3.01, a: 0.14, d: 2.6 },
    { m: 4.98, a: 0.06, d: 3.4 },
  ];
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    // Quick attack, long natural decay.
    const atk = Math.min(1, t / 0.004);
    let s = 0;
    for (const p of partials) {
      s += p.a * Math.sin(2 * Math.PI * freq * p.m * t) * Math.exp(-p.d * t * 3.2);
    }
    const v = s * atk * gain;
    write(buf, i0 + i, v, pan);
  }
}

/**
 * A breathy pad: two slightly detuned saw-ish tones under a slow swell. This is
 * what gives the track its "room" and stops it sounding like ringtone bleeps.
 */
function pad(buf, tStart, dur, freq, gain, pan) {
  const n = Math.floor(dur * SR);
  const i0 = Math.floor(tStart * SR);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const env = Math.min(1, t / (dur * 0.35)) * Math.min(1, (dur - t) / (dur * 0.45));
    // Three partials, each a touch detuned, gives a slow natural beating.
    const a = Math.sin(2 * Math.PI * freq * t);
    const b = Math.sin(2 * Math.PI * freq * 1.003 * t);
    const c = Math.sin(2 * Math.PI * freq * 2 * t) * 0.28;
    const d = Math.sin(2 * Math.PI * freq * 0.5 * t) * 0.22;
    const wob = 1 + 0.04 * Math.sin(2 * Math.PI * 0.18 * t);
    write(buf, i0 + i, (a + b + c + d) * 0.25 * env * gain * wob, pan);
  }
}

/** A tiny glass bell for sparkle, used sparingly on off-beats. */
function bell(buf, tStart, dur, freq, gain, pan) {
  const n = Math.floor(dur * SR);
  const i0 = Math.floor(tStart * SR);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const env = Math.exp(-4.5 * t);
    const s =
      Math.sin(2 * Math.PI * freq * t) +
      0.5 * Math.sin(2 * Math.PI * freq * 2.76 * t) * Math.exp(-2 * t) +
      0.25 * Math.sin(2 * Math.PI * freq * 5.4 * t) * Math.exp(-3 * t);
    write(buf, i0 + i, s * 0.33 * env * gain, pan);
  }
}

/** A round bass note to sit under the melody. */
function bass(buf, tStart, dur, freq, gain, pan) {
  const n = Math.floor(dur * SR);
  const i0 = Math.floor(tStart * SR);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const env = Math.min(1, t / 0.02) * Math.min(1, (dur - t) / (dur * 0.5));
    const s = Math.sin(2 * Math.PI * freq * t) + 0.25 * Math.sin(2 * Math.PI * freq * 2 * t);
    write(buf, i0 + i, s * 0.5 * env * gain, pan);
  }
}

/** Adds a sample to the stereo buffer, panned -1 (left) .. 1 (right). */
function write(buf, i, v, pan = 0) {
  if (i < 0 || i >= buf.left.length) return;
  const l = Math.cos((pan + 1) * Math.PI / 4);
  const r = Math.sin((pan + 1) * Math.PI / 4);
  buf.left[i] += v * l;
  buf.right[i] += v * r;
}

// ------------------------------------------------------------------- room

/** A small delay network, so the tunes sound like they are in a room rather
 *  than pasted onto silence. */
function reverb(buf, amount = 0.25) {
  const taps = [
    { d: 0.031, g: 0.36 }, { d: 0.047, g: 0.30 },
    { d: 0.071, g: 0.24 }, { d: 0.113, g: 0.18 },
    { d: 0.173, g: 0.12 }, { d: 0.241, g: 0.08 },
  ];
  const n = buf.left.length;
  const wetL = new Float64Array(n);
  const wetR = new Float64Array(n);
  for (const tap of taps) {
    const d = Math.floor(tap.d * SR);
    for (let i = d; i < n; i++) {
      wetL[i] += buf.left[i - d] * tap.g;
      wetR[i] += buf.right[i - d] * tap.g;
    }
  }
  for (let i = 0; i < n; i++) {
    buf.left[i] = buf.left[i] * (1 - amount * 0.35) + wetL[i] * amount;
    buf.right[i] = buf.right[i] * (1 - amount * 0.35) + wetR[i] * amount;
  }
}

/** Makes the ends match so the track loops without a click. */
function crossfadeLoop(buf, seconds = 1.5) {
  const n = Math.floor(seconds * SR);
  const len = buf.left.length;
  for (let i = 0; i < n; i++) {
    const f = i / n;
    const j = len - n + i;
    buf.left[i] = buf.left[i] * f + buf.left[j] * (1 - f);
    buf.right[i] = buf.right[i] * f + buf.right[j] * (1 - f);
  }
  buf.left = buf.left.slice(0, len - n);
  buf.right = buf.right.slice(0, len - n);
}

function normalise(buf, peak = 0.82) {
  let m = 0;
  for (let i = 0; i < buf.left.length; i++) {
    m = Math.max(m, Math.abs(buf.left[i]), Math.abs(buf.right[i]));
  }
  if (m === 0) return;
  const g = peak / m;
  for (let i = 0; i < buf.left.length; i++) {
    buf.left[i] *= g;
    buf.right[i] *= g;
  }
}

function writeWav(file, buf) {
  const n = buf.left.length;
  const data = Buffer.alloc(n * 4);
  for (let i = 0; i < n; i++) {
    const l = Math.max(-1, Math.min(1, buf.left[i]));
    const r = Math.max(-1, Math.min(1, buf.right[i]));
    data.writeInt16LE((l * 32767) | 0, i * 4);
    data.writeInt16LE((r * 32767) | 0, i * 4 + 2);
  }
  const head = Buffer.alloc(44);
  head.write('RIFF', 0);
  head.writeUInt32LE(36 + data.length, 4);
  head.write('WAVE', 8);
  head.write('fmt ', 12);
  head.writeUInt32LE(16, 16);
  head.writeUInt16LE(1, 20);        // PCM
  head.writeUInt16LE(2, 22);        // stereo
  head.writeUInt32LE(SR, 24);
  head.writeUInt32LE(SR * 4, 28);
  head.writeUInt16LE(4, 32);
  head.writeUInt16LE(16, 34);
  head.write('data', 36);
  head.writeUInt32LE(data.length, 40);
  fs.writeFileSync(file, Buffer.concat([head, data]));
  return head.length + data.length;
}

function blank(seconds) {
  const n = Math.floor(seconds * SR);
  return { left: new Float64Array(n), right: new Float64Array(n) };
}

// ------------------------------------------------------------------ tracks

/** Bright, strolling daytime tune in C major pentatonic. */
function trackDay(seconds) {
  const buf = blank(seconds);
  const beat = 60 / 96;
  const scale = ['C5', 'D5', 'E5', 'G5', 'A5', 'C6', 'D6', 'E6'];
  const chords = [
    ['C3', 'C4', 'E4', 'G4'],
    ['A2', 'A3', 'C4', 'E4'],
    ['F2', 'F3', 'A3', 'C4'],
    ['G2', 'G3', 'B3', 'D4'],
  ];
  const bars = Math.floor(seconds / (beat * 4));
  // A simple melody that rises and falls, so it feels sung rather than random.
  const shape = [0, 2, 4, 3, 2, 4, 5, 4, 3, 1, 2, 0, 1, 3, 2, 1];

  for (let bar = 0; bar < bars; bar++) {
    const t0 = bar * beat * 4;
    const chord = chords[bar % chords.length];
    pad(buf, t0, beat * 4.1, hz(chord[1]), 0.30, -0.25);
    pad(buf, t0, beat * 4.1, hz(chord[2]), 0.26, 0.25);
    pad(buf, t0, beat * 4.1, hz(chord[3]), 0.22, 0.05);
    bass(buf, t0, beat * 1.9, hz(chord[0]), 0.55, 0);
    bass(buf, t0 + beat * 2, beat * 1.9, hz(chord[0]), 0.42, 0);

    for (let step = 0; step < 8; step++) {
      const t = t0 + step * beat * 0.5;
      const idx = shape[(bar * 8 + step) % shape.length];
      const note = scale[idx % scale.length];
      const p = ((step % 4) - 1.5) * 0.22;
      mallet(buf, t, beat * 0.9, hz(note), 0.42, p);
      if (step % 4 === 2) bell(buf, t + beat * 0.25, beat * 1.6, hz(scale[(idx + 2) % scale.length]) * 2, 0.20, -p);
    }
  }
  reverb(buf, 0.30);
  crossfadeLoop(buf, 1.6);
  normalise(buf);
  return buf;
}

/** Slow, soft lullaby for the night scenes. */
function trackNight(seconds) {
  const buf = blank(seconds);
  const beat = 60 / 62;
  const scale = ['A4', 'C5', 'D5', 'E5', 'G5', 'A5'];
  const chords = [
    ['A2', 'A3', 'C4', 'E4'],
    ['F2', 'F3', 'A3', 'C4'],
    ['C3', 'C4', 'E4', 'G4'],
    ['G2', 'G3', 'B3', 'D4'],
  ];
  const bars = Math.floor(seconds / (beat * 4));
  const shape = [0, 1, 2, 1, 3, 2, 1, 0, 2, 3, 4, 3, 2, 1, 0, 1];

  for (let bar = 0; bar < bars; bar++) {
    const t0 = bar * beat * 4;
    const chord = chords[bar % chords.length];
    pad(buf, t0, beat * 4.2, hz(chord[1]), 0.34, -0.3);
    pad(buf, t0, beat * 4.2, hz(chord[2]), 0.30, 0.3);
    pad(buf, t0, beat * 4.2, hz(chord[3]), 0.24, 0);
    bass(buf, t0, beat * 3.8, hz(chord[0]), 0.5, 0);

    for (let step = 0; step < 4; step++) {
      const t = t0 + step * beat;
      const idx = shape[(bar * 4 + step) % shape.length];
      bell(buf, t, beat * 2.4, hz(scale[idx % scale.length]), 0.32, ((step % 2) - 0.5) * 0.4);
      if (step === 1) mallet(buf, t + beat * 0.5, beat * 1.2, hz(scale[(idx + 1) % scale.length]), 0.22, 0.2);
    }
  }
  reverb(buf, 0.42);
  crossfadeLoop(buf, 2.0);
  normalise(buf, 0.7);
  return buf;
}

/** Bouncier tune for busy play. */
function trackPlay(seconds) {
  const buf = blank(seconds);
  const beat = 60 / 116;
  const scale = ['G4', 'A4', 'B4', 'D5', 'E5', 'G5', 'A5', 'B5'];
  const chords = [
    ['G2', 'G3', 'B3', 'D4'],
    ['E2', 'E3', 'G3', 'B3'],
    ['C3', 'C4', 'E4', 'G4'],
    ['D3', 'D4', 'F#4', 'A4'],
  ];
  const bars = Math.floor(seconds / (beat * 4));
  const shape = [0, 3, 2, 4, 5, 4, 2, 3, 1, 4, 3, 5, 6, 5, 3, 2];

  for (let bar = 0; bar < bars; bar++) {
    const t0 = bar * beat * 4;
    const chord = chords[bar % chords.length];
    pad(buf, t0, beat * 4.1, hz(chord[1]), 0.24, -0.2);
    pad(buf, t0, beat * 4.1, hz(chord[2]), 0.20, 0.2);
    for (let b = 0; b < 4; b++) bass(buf, t0 + b * beat, beat * 0.8, hz(chord[0]), 0.46, 0);

    for (let step = 0; step < 8; step++) {
      const t = t0 + step * beat * 0.5;
      const idx = shape[(bar * 8 + step) % shape.length];
      mallet(buf, t, beat * 0.75, hz(scale[idx % scale.length]), 0.46, ((step % 3) - 1) * 0.3);
      if (step % 2 === 1) {
        mallet(buf, t + beat * 0.25, beat * 0.4, hz(scale[(idx + 2) % scale.length]) * 2, 0.16, 0.35);
      }
    }
  }
  reverb(buf, 0.26);
  crossfadeLoop(buf, 1.2);
  normalise(buf);
  return buf;
}

/** A short, warm fanfare for finishing a whole set. */
function trackWin(seconds) {
  const buf = blank(seconds);
  const beat = 0.26;
  const rise = ['C5', 'E5', 'G5', 'C6', 'E6', 'G6', 'C7'];
  rise.forEach((n, i) => {
    mallet(buf, i * beat * 0.75, 1.4, hz(n), 0.55, ((i % 3) - 1) * 0.3);
    bell(buf, i * beat * 0.75 + 0.05, 2.2, hz(n) * 2, 0.28, -((i % 3) - 1) * 0.3);
  });
  pad(buf, 0, seconds * 0.9, hz('C4'), 0.34, -0.2);
  pad(buf, 0, seconds * 0.9, hz('E4'), 0.30, 0.2);
  pad(buf, 0, seconds * 0.9, hz('G4'), 0.28, 0);
  bass(buf, 0, seconds * 0.85, hz('C3'), 0.6, 0);
  // A final shimmering cluster.
  ['C6', 'E6', 'G6', 'C7', 'E7'].forEach((n, i) => {
    bell(buf, beta(i), 2.6, hz(n), 0.24, (i % 2 ? 1 : -1) * 0.35);
  });
  function beta(i) { return 5 * beat + i * 0.09; }
  reverb(buf, 0.34);
  normalise(buf);
  return buf;
}

// -------------------------------------------------------------------- main

const jobs = [
  ['music_day.wav', () => trackDay(76)],
  ['music_night.wav', () => trackNight(80)],
  ['music_play.wav', () => trackPlay(72)],
  ['music_win.wav', () => trackWin(7)],
];

fs.mkdirSync(OUT, { recursive: true });
let total = 0;
for (const [name, make] of jobs) {
  const started = Date.now();
  const buf = make();
  const bytes = writeWav(path.join(OUT, name), buf);
  total += bytes;
  console.log(
    `${name.padEnd(18)} ${(bytes / 1048576).toFixed(1)} MB` +
    `  ${(buf.left.length / SR).toFixed(1)}s  (${Date.now() - started}ms)`
  );
}
console.log(`total ${(total / 1048576).toFixed(1)} MB written to res/raw`);
