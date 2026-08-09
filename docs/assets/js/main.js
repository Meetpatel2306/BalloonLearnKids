document.addEventListener('DOMContentLoaded', () => {

  /* Footer year, so the copyright never goes stale. */
  const yearElement = document.getElementById('current-year');
  if (yearElement) yearElement.textContent = new Date().getFullYear();

  /* Mark the nav link for the page we are actually on. */
  document.querySelectorAll('.nav-link').forEach(link => {
    const linkPath = new URL(link.getAttribute('href'), window.location.href).pathname;
    const here = window.location.pathname.replace(/\/$/, '/index.html');
    if (linkPath === window.location.pathname || linkPath === here) {
      link.classList.add('active');
      link.setAttribute('aria-current', 'page');
    }
  });

  /* Reading-progress bar across the top. */
  const bar = document.getElementById('scroll-progress');
  const toTop = document.getElementById('to-top');

  function onScroll() {
    const max = document.documentElement.scrollHeight - window.innerHeight;
    const pct = max > 0 ? (window.scrollY / max) * 100 : 0;
    if (bar) bar.style.width = pct + '%';
    if (toTop) toTop.classList.toggle('show', window.scrollY > 500);
  }
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();

  if (toTop) {
    toTop.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
  }

  /* Fade sections in as they arrive. */
  const reveals = document.querySelectorAll('.reveal');
  if ('IntersectionObserver' in window && reveals.length) {
    const io = new IntersectionObserver((entries, obs) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('in');
          obs.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12 });
    reveals.forEach(el => io.observe(el));
  } else {
    reveals.forEach(el => el.classList.add('in'));
  }

  /* Count the stat numbers up the first time they are seen. */
  const stats = document.querySelectorAll('[data-count]');
  if ('IntersectionObserver' in window && stats.length) {
    const io2 = new IntersectionObserver((entries, obs) => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return;
        obs.unobserve(entry.target);
        const el = entry.target;
        const target = parseInt(el.dataset.count, 10);
        const suffix = el.dataset.suffix || '';
        const started = performance.now();
        const dur = 900;
        (function step(now) {
          const t = Math.min(1, (now - started) / dur);
          el.textContent = Math.round(target * (1 - Math.pow(1 - t, 3))) + suffix;
          if (t < 1) requestAnimationFrame(step);
        })(started);
      });
    }, { threshold: 0.4 });
    stats.forEach(el => io2.observe(el));
  }
});
