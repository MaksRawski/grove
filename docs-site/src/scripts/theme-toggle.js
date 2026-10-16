// Shared read/write/toggle for Bloom's theme state. Used by the landing
// page's buffer-bar pill and the Starlight docs ThemeSelect override, so
// both toggles manipulate the exact same localStorage key + attribute set
// by theme-boot.js.
export const BLOOM_THEME_KEY = 'bloom-theme';

export function getBloomTheme() {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light';
}

export function setBloomTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  try {
    localStorage.setItem(BLOOM_THEME_KEY, theme);
  } catch (e) {
    // localStorage unavailable (private browsing, etc.) — theme still applies for this page view.
  }
}

export function toggleBloomTheme() {
  const next = getBloomTheme() === 'light' ? 'dark' : 'light';
  setBloomTheme(next);
  return next;
}

// Wires up every `[data-bloom-theme-toggle]` button on the page: label reads
// "bloom-<destination>-theme", click flips the shared theme. Used by both
// the landing page's buffer bar and the Starlight ThemeSelect override.
export function wireThemeTogglePills(selector = '[data-bloom-theme-toggle]') {
  function syncLabel(button) {
    const label = button.querySelector('[data-bloom-theme-label]');
    const destination = getBloomTheme() === 'light' ? 'dark' : 'light';
    if (label) label.textContent = `bloom-${destination}-theme`;
  }

  document.querySelectorAll(selector).forEach((button) => {
    syncLabel(button);
    button.addEventListener('click', () => {
      toggleBloomTheme();
      syncLabel(button);
    });
  });
}
