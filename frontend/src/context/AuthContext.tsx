import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { clearCredentials, setCredentials, setUnauthorizedHandler } from '../api/client';
import { login as fetchMe } from '../api/payments';
import type { CurrentUser } from '../api/types';

interface AuthContextValue {
  user: CurrentUser | null;
  sessionExpired: boolean;
  /** Operators may create and refund payments; viewers are read-only. */
  canWrite: boolean;
  signIn: (username: string, password: string) => Promise<void>;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [sessionExpired, setSessionExpired] = useState(false);

  const signOut = useCallback(() => {
    clearCredentials();
    setUser(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearCredentials();
      setUser(null);
      setSessionExpired(true);
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  const signIn = useCallback(async (username: string, password: string) => {
    setCredentials(username, password);
    try {
      const me = await fetchMe();
      setSessionExpired(false);
      setUser(me);
    } catch (e) {
      clearCredentials();
      throw e;
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, sessionExpired, canWrite: user?.roles.includes('OPERATOR') ?? false, signIn, signOut }),
    [user, sessionExpired, signIn, signOut],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
