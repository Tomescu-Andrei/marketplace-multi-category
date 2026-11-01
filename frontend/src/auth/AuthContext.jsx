import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { authApi, describeError } from './api.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const userRef = useRef(null);
  const revision = useRef(0);
  const [status, setStatus] = useState('loading');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const refresh = useCallback(async ({ signal, background = false } = {}) => {
    const requestRevision = ++revision.current;
    if (!background) setStatus('loading');
    setError('');
    try {
      const account = await authApi.me({ signal });
      if (signal?.aborted || requestRevision !== revision.current) return;
      if (!account && userRef.current) setNotice('Sesiunea ta a expirat. Autentifică-te din nou.');
      userRef.current = account;
      setUser(account);
      setStatus('ready');
    } catch (failure) {
      if (signal?.aborted || requestRevision !== revision.current) return;
      setError(describeError(failure));
      if (!background) setStatus('error');
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    refresh({ signal: controller.signal });
    return () => controller.abort();
  }, [refresh]);

  useEffect(() => {
    if (!user) return;
    const focus = () => refresh({ background: true });
    window.addEventListener('focus', focus);
    return () => window.removeEventListener('focus', focus);
  }, [user, refresh]);

  const login = async (values) => {
    const account = await authApi.login(values);
    revision.current++;
    userRef.current = account;
    setUser(account);
    setStatus('ready');
    setNotice('');
    setError('');
  };
  const logout = async () => {
    revision.current++;
    await authApi.logout();
    revision.current++;
    userRef.current = null;
    setUser(null);
    setNotice('Te-ai deconectat cu succes. Pe curând!');
    setError('');
  };

  return <AuthContext.Provider value={{ user, status, error, notice, refresh, login, logout, register: authApi.register }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
