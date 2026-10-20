import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { ApiError, refreshSession, clearSession, setSessionClearedListener } from '@/lib/api/client';
import { getProfile, type Profile } from '@/features/user/api';
import { login, logout } from '@/features/auth/api';
import { clearDrafts } from '@/features/journal/drafts';

type SessionState = { status: 'loading' | 'guest' | 'authenticated' | 'error'; profile: Profile | null; error: string | null };
type SessionContextValue = SessionState & {
  reload: () => Promise<void>; signIn: (email: string, password: string) => Promise<Profile>;
  signOut: () => Promise<void>; setProfile: (profile: Profile) => void;
};
const SessionContext = createContext<SessionContextValue | null>(null);

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<SessionState>({ status: 'loading', profile: null, error: null });
  useEffect(() => {
    setSessionClearedListener(() => { void clearDrafts(); setState({ status: 'guest', profile: null, error: null }); });
    return () => setSessionClearedListener(null);
  }, []);
  const reload = useCallback(async () => {
    setState((current) => ({ ...current, status: 'loading', error: null }));
    try {
      const token = await refreshSession();
      if (!token) { setState({ status: 'guest', profile: null, error: null }); return; }
      const profile = await getProfile();
      setState({ status: 'authenticated', profile, error: null });
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        await clearSession();
        setState({ status: 'guest', profile: null, error: null });
      } else {
        setState({ status: 'error', profile: null, error: error instanceof Error ? error.message : 'Không thể tải phiên.' });
      }
    }
  }, []);
  useEffect(() => {
    const timer = setTimeout(() => void reload(), 0);
    return () => clearTimeout(timer);
  }, [reload]);

  const signIn = async (email: string, password: string) => {
    await login(email, password);
    const profile = await getProfile();
    setState({ status: 'authenticated', profile, error: null });
    return profile;
  };
  const signOut = async () => {
    try { await logout(); }
    finally { try { await clearDrafts(); }
      finally { setState({ status: 'guest', profile: null, error: null }); } }
  };
  const value = useMemo(() => ({ ...state, reload, signIn, signOut,
    setProfile: (profile: Profile) => setState({ status: 'authenticated', profile, error: null }) }), [state, reload]);
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const session = useContext(SessionContext);
  if (!session) throw new Error('SessionProvider is missing');
  return session;
}
