import { useEffect, useRef, useState } from 'react';
import { Moon, Sun } from 'lucide-react';

const storageKey = 'piata-theme';

function savedTheme() {
  try {
    const value = localStorage.getItem(storageKey);
    return value === 'dark' || value === 'light' ? value : null;
  } catch { return null; }
}

function preferredTheme() {
  return savedTheme() || (window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
}

export default function ThemeToggle() {
  const [theme, setTheme] = useState(preferredTheme);
  const manuallySelected = useRef(savedTheme() !== null);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#101a16' : '#f7f8f4');
  }, [theme]);

  useEffect(() => {
    const media = window.matchMedia?.('(prefers-color-scheme: dark)');
    const systemChanged = () => { if (!manuallySelected.current) setTheme(preferredTheme()); };
    const storageChanged = (event) => {
      if (event.key === storageKey || event.key === null) {
        manuallySelected.current = savedTheme() !== null;
        setTheme(preferredTheme());
      }
    };
    media?.addEventListener('change', systemChanged);
    window.addEventListener('storage', storageChanged);
    return () => {
      media?.removeEventListener('change', systemChanged);
      window.removeEventListener('storage', storageChanged);
    };
  }, []);

  const toggle = () => {
    const next = theme === 'dark' ? 'light' : 'dark';
    manuallySelected.current = true;
    try { localStorage.setItem(storageKey, next); } catch { /* Keep the toggle usable without storage. */ }
    setTheme(next);
  };

  return <button className="theme-toggle" type="button" onClick={toggle} aria-label="Mod întunecat"
    aria-pressed={theme === 'dark'} title={theme === 'dark' ? 'Activează modul luminos' : 'Activează modul întunecat'}>
    {theme === 'dark' ? <Sun size={19} /> : <Moon size={19} />}
  </button>;
}
