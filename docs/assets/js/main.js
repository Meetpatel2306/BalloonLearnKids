document.addEventListener('DOMContentLoaded', () => {
  // Footer year, so the copyright never goes stale.
  const yearElement = document.getElementById('current-year');
  if (yearElement) {
    yearElement.textContent = new Date().getFullYear();
  }

  // Mark the nav link for the page we are actually on.
  const here = window.location.pathname.replace(/\/index\.html$/, '/').replace(/\/$/, '/index.html');
  document.querySelectorAll('.nav-link').forEach(link => {
    const linkPath = new URL(link.getAttribute('href'), window.location.href).pathname;
    const isCurrent = linkPath === window.location.pathname || linkPath === here;
    link.classList.toggle('active', isCurrent);
    if (isCurrent) link.setAttribute('aria-current', 'page');
  });
});
