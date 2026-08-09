/*
 * The interactive balloon playground on the home page.
 *
 * Balloons rise, you tap them, they burst with a pop and a shower of confetti.
 * The mode chips swap what the balloons carry, mirroring the five modes in the
 * app itself. Everything is generated here - no images, no audio files.
 */
(function () {
  const field = document.getElementById('playground');
  if (!field) return;

  const counterEl = document.getElementById('pg-count');
  const soundBtn  = document.getElementById('pg-sound');
  const hintEl    = document.getElementById('pg-hint');

  const COLORS = ['#FF7EB3', '#4FA5F0', '#A573F5', '#3BC46B', '#FFB443', '#FF6B6B', '#2DD4BF'];

  // What each mode puts on a balloon, matching the app's own content.
  const MODES = {
    letters: { glyphs: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split(''), shape: null },
    numbers: { glyphs: Array.from({ length: 20 }, (_, i) => String(i + 1)), shape: null },
    colours: { glyphs: [''], shape: null },
    shapes:  { glyphs: ['●', '■', '▲', '★', '♥'], shape: 'auto' },
    animals: { glyphs: ['🐶', '🐱', '🐮', '🐷', '🦆',
                        '🐰', '🐻', '🦁', '🐒', '🐘',
                        '🐸', '🐟', '🐦', '🐎'], shape: null }
  };

  let mode      = 'letters';
  let popped    = 0;
  let soundOn   = true;
  let interacted = false;
  const live    = new Set();
  const MAX_LIVE = 7;

  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ---------------- sound ---------------- */
  let audioCtx = null;

  function ctx() {
    if (!audioCtx) {
      const AC = window.AudioContext || window.webkitAudioContext;
      if (AC) audioCtx = new AC();
    }
    return audioCtx;
  }

  // A short burst of filtered noise plus a falling tone - a balloon "pop".
  function playPop() {
    if (!soundOn) return;
    const ac = ctx();
    if (!ac) return;
    if (ac.state === 'suspended') ac.resume();

    const now = ac.currentTime;

    const len   = Math.floor(ac.sampleRate * 0.08);
    const buf   = ac.createBuffer(1, len, ac.sampleRate);
    const data  = buf.getChannelData(0);
    for (let i = 0; i < len; i++) {
      data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, 3);
    }
    const noise = ac.createBufferSource();
    noise.buffer = buf;

    const bp = ac.createBiquadFilter();
    bp.type = 'bandpass';
    bp.frequency.value = 900 + Math.random() * 500;
    bp.Q.value = 1.2;

    const ng = ac.createGain();
    ng.gain.setValueAtTime(0.35, now);
    ng.gain.exponentialRampToValueAtTime(0.001, now + 0.09);

    noise.connect(bp).connect(ng).connect(ac.destination);
    noise.start(now);
    noise.stop(now + 0.1);

    const osc = ac.createOscillator();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(620 + Math.random() * 180, now);
    osc.frequency.exponentialRampToValueAtTime(180, now + 0.12);

    const og = ac.createGain();
    og.gain.setValueAtTime(0.18, now);
    og.gain.exponentialRampToValueAtTime(0.001, now + 0.14);

    osc.connect(og).connect(ac.destination);
    osc.start(now);
    osc.stop(now + 0.15);
  }

  // A brighter twinkle every tenth pop.
  function playCheer() {
    if (!soundOn) return;
    const ac = ctx();
    if (!ac) return;
    if (ac.state === 'suspended') ac.resume();
    [523.25, 659.25, 783.99, 1046.5].forEach((f, i) => {
      const t = ac.currentTime + i * 0.09;
      const o = ac.createOscillator();
      const g = ac.createGain();
      o.type = 'triangle';
      o.frequency.value = f;
      g.gain.setValueAtTime(0.0001, t);
      g.gain.exponentialRampToValueAtTime(0.16, t + 0.02);
      g.gain.exponentialRampToValueAtTime(0.0001, t + 0.28);
      o.connect(g).connect(ac.destination);
      o.start(t);
      o.stop(t + 0.3);
    });
  }

  /* ---------------- balloons ---------------- */
  function pick(arr) { return arr[Math.floor(Math.random() * arr.length)]; }

  function spawn() {
    if (live.size >= MAX_LIVE) return;

    const cfg   = MODES[mode];
    const b     = document.createElement('div');
    const color = pick(COLORS);
    const glyph = pick(cfg.glyphs);

    b.className = 'pop-balloon';
    b.style.background = color;
    b.textContent = glyph;

    if (cfg.shape === 'auto') {
      if (glyph === '■') b.classList.add('shape-square');
      if (glyph === '▲') b.classList.add('shape-triangle');
    }

    const scale = 0.8 + Math.random() * 0.5;
    b.style.width  = (62 * scale) + 'px';
    b.style.height = (78 * scale) + 'px';
    b.style.fontSize = (1.5 * scale) + 'rem';
    b.style.left = (5 + Math.random() * 85) + '%';

    field.appendChild(b);
    live.add(b);

    const dur  = (reduced ? 30 : 9 + Math.random() * 7) * 1000;
    const sway = 20 + Math.random() * 40;
    const dir  = Math.random() < 0.5 ? -1 : 1;

    const anim = b.animate([
      { transform: 'translate(0, 0) rotate(-4deg)' },
      { transform: `translate(${sway * dir}px, -${field.clientHeight * 0.55}px) rotate(4deg)` },
      { transform: `translate(0, -${field.clientHeight + 200}px) rotate(-3deg)` }
    ], { duration: dur, easing: 'linear' });

    anim.onfinish = () => { remove(b); spawn(); };

    b.addEventListener('pointerdown', e => {
      e.preventDefault();
      e.stopPropagation();
      pop(b, color);
    });
  }

  function remove(b) {
    live.delete(b);
    if (b.parentNode) b.parentNode.removeChild(b);
  }

  function pop(b, color) {
    if (b.dataset.gone) return;
    b.dataset.gone = '1';

    if (!interacted) {
      interacted = true;
      if (hintEl) hintEl.textContent = 'Keep going — they never run out';
    }

    const rect  = b.getBoundingClientRect();
    const frame = field.getBoundingClientRect();
    burst(rect.left - frame.left + rect.width / 2, rect.top - frame.top + rect.height / 2, color);

    playPop();
    b.getAnimations().forEach(a => a.cancel());
    b.classList.add('popped');

    popped++;
    if (counterEl) counterEl.textContent = popped + (popped === 1 ? ' pop' : ' pops');
    if (popped % 10 === 0) { playCheer(); confetti(); }

    setTimeout(() => { remove(b); spawn(); }, 300);
  }

  function burst(x, y, color) {
    const n = reduced ? 4 : 12;
    for (let i = 0; i < n; i++) {
      const p = document.createElement('div');
      p.className = 'particle';
      p.style.background = Math.random() < 0.4 ? '#fff' : color;
      p.style.left = x + 'px';
      p.style.top  = y + 'px';
      field.appendChild(p);

      const angle = (Math.PI * 2 * i) / n + Math.random() * 0.4;
      const dist  = 40 + Math.random() * 70;

      p.animate([
        { transform: 'translate(-50%, -50%) scale(1)', opacity: 1 },
        { transform: `translate(${Math.cos(angle) * dist - 50}%, ${Math.sin(angle) * dist + 30}%) scale(0)`, opacity: 0 }
      ], { duration: 520 + Math.random() * 260, easing: 'cubic-bezier(.2,.7,.4,1)' })
       .onfinish = () => p.remove();
    }
  }

  function confetti() {
    const n = reduced ? 6 : 40;
    for (let i = 0; i < n; i++) {
      const p = document.createElement('div');
      p.className = 'particle';
      p.style.background = pick(COLORS);
      p.style.borderRadius = Math.random() < 0.5 ? '50%' : '2px';
      p.style.width  = (5 + Math.random() * 7) + 'px';
      p.style.height = (5 + Math.random() * 9) + 'px';
      p.style.left = (Math.random() * 100) + '%';
      p.style.top  = '-20px';
      field.appendChild(p);

      p.animate([
        { transform: 'translateY(0) rotate(0deg)', opacity: 1 },
        { transform: `translateY(${field.clientHeight + 60}px) rotate(${360 + Math.random() * 540}deg)`, opacity: .9 }
      ], { duration: 1600 + Math.random() * 1200, easing: 'cubic-bezier(.3,.6,.5,1)' })
       .onfinish = () => p.remove();
    }
  }

  /* ---------------- controls ---------------- */
  document.querySelectorAll('.mode-chip').forEach(chip => {
    chip.addEventListener('click', e => {
      e.stopPropagation();
      document.querySelectorAll('.mode-chip').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      mode = chip.dataset.mode;
      // Clear the sky so the new mode shows straight away.
      Array.from(live).forEach(b => { b.getAnimations().forEach(a => a.cancel()); remove(b); });
      for (let i = 0; i < 5; i++) setTimeout(spawn, i * 160);
    });
  });

  if (soundBtn) {
    soundBtn.addEventListener('click', e => {
      e.stopPropagation();
      soundOn = !soundOn;
      soundBtn.textContent = soundOn ? '🔊' : '🔇';
      soundBtn.setAttribute('aria-label', soundOn ? 'Mute pop sounds' : 'Unmute pop sounds');
    });
  }

  // Tapping empty sky sparkles, exactly like the app - never a penalty.
  field.addEventListener('pointerdown', e => {
    const frame = field.getBoundingClientRect();
    burst(e.clientX - frame.left, e.clientY - frame.top, '#FFFFFF');
  });

  // Start with a few balloons already on their way up.
  for (let i = 0; i < 5; i++) setTimeout(spawn, i * 500);

  // Keep the sky topped up.
  setInterval(() => { if (live.size < MAX_LIVE) spawn(); }, 2200);
})();
