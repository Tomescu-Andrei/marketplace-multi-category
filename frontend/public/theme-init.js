// Apply the preferred palette before React/CSS paint, using an external CSP-compatible script.
(() => {
  let preference;
  try { preference = localStorage.getItem('piata-theme'); } catch { /* Storage may be unavailable. */ }
  const dark = preference === 'dark' || (preference !== 'light' && window.matchMedia?.('(prefers-color-scheme: dark)').matches);
  document.documentElement.dataset.theme = dark ? 'dark' : 'light';
})();
