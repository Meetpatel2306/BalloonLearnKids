/*
 * Renders the celebration sounds into app/src/main/res/raw as 16-bit stereo WAV.
 *
 *   node tools/make-sfx.js
 *
 * These are the moments worth spending real audio on: a room full of children
 * clapping when a whole set is finished, a cheer over the top of it, and a warm
 * chime for each correct answer. Everything else in the game is still
 * synthesised live, which keeps the common sounds instant.
 */

const fs = require('fs');
const path = require('path');

const SR = 44100;
const OUT = path.join(__dirname, '..', 'app', 'src', 'main', 'res', 'raw');

// A deterministic pseudo-random source, so re-running gives the same files.
let seed = 20260811;
function rnd() {
  seed = (seed * 1664525 + 1013904223) >>> 0;
  return seed / 4294967296;
}

function blank(seconds) {
  const n = Math.floor(seconds * SR);
  return { left: new Float64Array(n), right: new Float64Array(n) };
}

function add(buf, i, v, pan = 0) {
  if (i < 0 || i >= buf.left.length) return;
  const l = Math.cos((pan + 1) * Math.PI / 4);
  const r = Math.sin((pan + 1) * Math.PI / 4);
  buf.left[i] += v * l;
  buf.right[i] += v * r;
}

/**
 * One pair of hands. A clap is a very short burst of noise shaped by a couple of
 * resonances — get the decay right and the ear fills in the rest.
 */
function clap(buf, t0, gain, pan, bright) {
  const n = Math.floor(0.16 * SR);
  const i0 = Math.floor(t0 * SR);
  let lp = 0;
  let bp = 0;
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const noise = rnd() * 2 - 1;
    // A one-pole low-pass plus a resonant band gives the body of a hand clap.
    lp += (noise - lp) * (0.35 + bright * 0.4);
    bp += (lp - bp) * 0.28;
    const env = Math.exp(-38 * t) + 0.35 * Math.exp(-12 * t);
    add(buf, i0 + i, (lp - bp * 0.6) * env * gain, pan);
  }
}

/** A crowd: many hands, slightly out of time with each other, swelling then easing. */
function applause(seconds) {
  const buf = blank(seconds);
  const hands = 260;
  for (let h = 0; h < hands; h++) {
    const pan = (rnd() * 2 - 1) * 0.9;
    const bright = rnd();
    const gain = 0.10 + rnd() * 0.16;
    // Each pair of hands claps at its own tempo, drifting over time.
    let t = rnd() * 0.5;
    const period = 0.20 + rnd() * 0.22;
    while (t < seconds - 0.2) {
      // The crowd builds for the first second and thins out at the end.
      const swell = Math.min(1, t / 0.9) * Math.min(1, (seconds - t) / 1.6);
      if (rnd() < 0.86 * swell + 0.1) clap(buf, t, gain * (0.5 + swell * 0.7), pan, bright);
      t += period * (0.82 + rnd() * 0.36);
    }
  }
  normalise(buf, 0.62);
  return buf;
}

/** A bright three-note chime, the sound of getting something right. */
function correct() {
  const buf = blank(1.6);
  const notes = [523.25, 659.25, 783.99];
  notes.forEach((f, k) => {
    const t0 = k * 0.075;
    const n = Math.floor(1.4 * SR);
    const i0 = Math.floor(t0 * SR);
    for (let i = 0; i < n; i++) {
      const t = i / SR;
      const env = Math.exp(-3.4 * t) * Math.min(1, t / 0.004);
      const s =
        Math.sin(2 * Math.PI * f * t) +
        0.45 * Math.sin(2 * Math.PI * f * 2 * t) * Math.exp(-2.2 * t) +
        0.18 * Math.sin(2 * Math.PI * f * 3.01 * t) * Math.exp(-3.4 * t);
      add(buf, i0 + i, s * 0.24 * env, (k - 1) * 0.35);
    }
  });
  normalise(buf, 0.8);
  return buf;
}

/** A rising sparkle sweep, for the moment the confetti goes up. */
function cheer(seconds) {
  const buf = blank(seconds);
  // A shower of little bells climbing the scale.
  const scale = [523.25, 587.33, 659.25, 783.99, 880, 1046.5, 1174.66, 1318.5];
  for (let k = 0; k < 90; k++) {
    const t0 = (k / 90) * (seconds * 0.7) + rnd() * 0.08;
    const f = scale[Math.floor((k / 90) * scale.length) % scale.length] * (rnd() < 0.4 ? 2 : 1);
    const n = Math.floor(0.9 * SR);
    const i0 = Math.floor(t0 * SR);
    const pan = (rnd() * 2 - 1) * 0.8;
    const gain = 0.10 + rnd() * 0.08;
    for (let i = 0; i < n; i++) {
      const t = i / SR;
      const env = Math.exp(-6.5 * t);
      const s = Math.sin(2 * Math.PI * f * t) + 0.3 * Math.sin(2 * Math.PI * f * 2.76 * t) * Math.exp(-3 * t);
      add(buf, i0 + i, s * gain * env, pan);
    }
  }
  normalise(buf, 0.75);
  return buf;
}

function normalise(buf, peak) {
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
  head.writeUInt16LE(1, 20);
  head.writeUInt16LE(2, 22);
  head.writeUInt32LE(SR, 24);
  head.writeUInt32LE(SR * 4, 28);
  head.writeUInt16LE(4, 32);
  head.writeUInt16LE(16, 34);
  head.write('data', 36);
  head.writeUInt32LE(data.length, 40);
  fs.writeFileSync(file, Buffer.concat([head, data]));
  return head.length + data.length;
}

const jobs = [
  ['sfx_applause.wav', () => applause(7.5)],
  ['sfx_cheer.wav', () => cheer(3.2)],
  ['sfx_correct.wav', () => correct()],
];

fs.mkdirSync(OUT, { recursive: true });
let total = 0;
for (const [name, make] of jobs) {
  const buf = make();
  const bytes = writeWav(path.join(OUT, name), buf);
  total += bytes;
  console.log(`${name.padEnd(20)} ${(bytes / 1048576).toFixed(2)} MB  ${(buf.left.length / SR).toFixed(1)}s`);
}
console.log(`total ${(total / 1048576).toFixed(2)} MB`);
