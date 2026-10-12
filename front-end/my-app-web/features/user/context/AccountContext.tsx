'use client';

import { createContext, useContext } from 'react';
import { useAccountProfile } from '../hooks/useAccountProfile';
import type { Profile } from '../api/client';

type AccountContextValue = ReturnType<typeof useAccountProfile> & { userId: string };
const AccountContext = createContext<AccountContextValue | null>(null);

export function AccountProvider({ profile, children }: { profile: Profile; children: React.ReactNode }) {
  const account = useAccountProfile(profile);
  return <AccountContext.Provider value={{ ...account, userId: profile.userId }}>{children}</AccountContext.Provider>;
}

export function useAccount() {
  const account = useContext(AccountContext);
  if (!account) throw new Error('useAccount must be used within AccountProvider');
  return account;
}
