'use client';

import React, { createContext, useContext, useState, useCallback, useMemo, useRef } from 'react';
import { JournalEntry, DailyStat, UserProfile } from '@/types';
import { journalBody, journalJson, journalRequest, toEntry, type JournalDetail, type JournalPage } from '../api/client';
import { clearJournalDrafts } from '../utils/draft-storage';
import { useAccountProfile, type Profile } from '@/features/user';
import { useQuery, useQueryClient } from '@tanstack/react-query';

interface JournalContextType {
  userId: string;
  entries: JournalEntry[];
  stats: DailyStat[];
  addEntry: (entry: Record<string, unknown>, key: string) => Promise<JournalEntry>;
  updateEntry: (id: string, entry: Record<string, unknown>, version: number) => Promise<JournalEntry>;
  deleteEntry: (id: string, version: number) => Promise<void>;
  getEntryById: (id: string) => JournalEntry | undefined;
  toggleFavorite: (id: string) => Promise<JournalEntry>;
  loadEntry: (id: string) => Promise<JournalEntry>;
  reloadEntries: () => Promise<void>;
  entriesLoading: boolean;
  entriesError: string | null;
  userProfile: UserProfile;
  accountProfile: Profile;
  reloadProfile: () => Promise<Profile>;
  updateProfile: (data: Partial<UserProfile>) => Promise<Profile>;
  completeOnboarding: () => Promise<Profile>;
  logout: () => void;
  streakCount: number;
  draftStorageKey: string;
  registerJournalLeaveCheck: (check: (() => boolean) | null) => void;
  hasUnsavedJournalChanges: () => boolean;
}

const JournalContext = createContext<JournalContextType | undefined>(undefined);

function journalStreak(entries: JournalEntry[], timezone: string) {
  if (!entries.length) return 0;
  const parts = new Intl.DateTimeFormat('en-US', { timeZone: timezone, year: 'numeric', month: '2-digit', day: '2-digit' })
    .formatToParts(new Date());
  const value = (type: string) => parts.find((part) => part.type === type)?.value ?? '';
  const today = new Date(`${value('year')}-${value('month')}-${value('day')}T00:00:00Z`);
  const dates = new Set(entries.map((entry) => entry.date));
  if (!dates.has(today.toISOString().slice(0, 10))) today.setUTCDate(today.getUTCDate() - 1);
  let count = 0;
  while (dates.has(today.toISOString().slice(0, 10))) {
    count++;
    today.setUTCDate(today.getUTCDate() - 1);
  }
  return count;
}

export function JournalProvider({ children, userId, profile }: {
  children: React.ReactNode;
  userId: string;
  profile: Profile;
}) {
  const queryClient = useQueryClient();
  const storageKey = useCallback((name: string) => `mylog_${userId}_${name}`, [userId]);
  const { userProfile, accountProfile, reloadProfile, updateProfile, completeOnboarding } = useAccountProfile(profile);
  const entriesKey = useMemo(() => ['journal', userId, 'entries'] as const, [userId]);
  const [stats] = useState<DailyStat[]>([]);
  const journalLeaveCheck = useRef<(() => boolean) | null>(null);
  const registerJournalLeaveCheck = useCallback((check: (() => boolean) | null) => {
    journalLeaveCheck.current = check;
  }, []);
  const hasUnsavedJournalChanges = useCallback(() => journalLeaveCheck.current?.() ?? false, []);

  const entriesQuery = useQuery({
    queryKey: entriesKey,
    queryFn: async () => {
      const all: JournalEntry[] = [];
      let cursor: string | null = null;
      do {
        const page: JournalPage = await journalJson<JournalPage>(`journal-entries?limit=100${cursor ? `&cursor=${encodeURIComponent(cursor)}` : ''}`);
        all.push(...page.items.map(toEntry));
        cursor = page.nextCursor;
      } while (cursor);
      return all;
    },
  });
  const entries = useMemo(() => entriesQuery.data ?? [], [entriesQuery.data]);
  const entriesLoading = entriesQuery.isPending;
  const entriesError = entriesQuery.error instanceof Error ? entriesQuery.error.message : null;
  const reloadEntries = async () => { await entriesQuery.refetch(); };
  const refreshDerived = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['dashboard', userId] }),
    queryClient.invalidateQueries({ queryKey: ['insights', userId] }),
    queryClient.invalidateQueries({ queryKey: ['reports', userId] }),
  ]);

  const addEntry = async (body: Record<string, unknown>, key: string) => {
    const saved = await journalJson<JournalDetail>('journal-entries', {
      method: 'POST', ...journalBody(body, { 'Idempotency-Key': key }),
    });
    const entry = toEntry(saved);
    queryClient.setQueryData<JournalEntry[]>(entriesKey, (previous) => previous ? [entry, ...previous.filter((old) => old.id !== entry.id)] : previous);
    void refreshDerived();
    return entry;
  };

  const updateEntry = async (id: string, body: Record<string, unknown>, version: number) => {
    const saved = await journalJson<JournalDetail>(`journal-entries/${id}`, {
      method: 'PATCH', ...journalBody(body, { 'If-Match': `"${version}"` }),
    });
    const entry = toEntry(saved);
    queryClient.setQueryData<JournalEntry[]>(entriesKey, (previous) => previous?.map((old) => old.id === id ? entry : old));
    queryClient.removeQueries({ queryKey: ['analysis', userId, id] });
    void refreshDerived();
    return entry;
  };

  const deleteEntry = async (id: string, version: number) => {
    await journalRequest(`journal-entries/${id}`, { method: 'DELETE', headers: { 'If-Match': `"${version}"` } });
    queryClient.setQueryData<JournalEntry[]>(entriesKey, (previous) => previous?.filter((entry) => entry.id !== id));
    queryClient.removeQueries({ queryKey: ['analysis', userId, id] });
    void refreshDerived();
  };

  const loadEntry = useCallback(async (id: string) => {
    const detail = await journalJson<JournalDetail>(`journal-entries/${id}`);
    const entry = toEntry(detail);
    queryClient.setQueryData<JournalEntry[]>(entriesKey, (previous) => previous?.map((old) => old.id === id ? entry : old));
    return entry;
  }, [queryClient, entriesKey]);

  const getEntryById = useCallback((id: string): JournalEntry | undefined => {
    return entries.find(e => e.id === id);
  }, [entries]);

  const toggleFavorite = async (id: string) => {
    const current = entries.find((entry) => entry.id === id);
    if (!current) throw new Error('Không tìm thấy bài viết.');
    const saved = await journalJson<JournalDetail>(`journal-entries/${id}/favorite`, {
      method: current.isFavorite ? 'DELETE' : 'PUT',
    });
    const entry = toEntry(saved);
    queryClient.setQueryData<JournalEntry[]>(entriesKey, (previous) => previous?.map((old) => old.id === id ? { ...entry, tags: old.tags } : old));
    return entry;
  };

  const logout = () => {
    clearJournalDrafts();
  };

  return (
    <JournalContext.Provider
      value={{
        userId,
        entries,
        stats,
        addEntry,
        updateEntry,
        deleteEntry,
        getEntryById,
        toggleFavorite,
        loadEntry,
        reloadEntries,
        entriesLoading,
        entriesError,
        userProfile,
        accountProfile,
        reloadProfile,
        updateProfile,
        completeOnboarding,
        logout,
        streakCount: journalStreak(entries, userProfile.timezone),
        draftStorageKey: storageKey('draft_journal'),
        registerJournalLeaveCheck,
        hasUnsavedJournalChanges,
      }}
    >
      {children}
    </JournalContext.Provider>
  );
}

export function useJournal() {
  const context = useContext(JournalContext);
  if (!context) {
    throw new Error('useJournal must be used within a JournalProvider');
  }
  return context;
}
