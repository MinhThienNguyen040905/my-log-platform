'use client';

import React, { createContext, useContext, useCallback, useMemo, useRef } from 'react';
import { JournalEntry } from '@/types';
import { journalBody, journalJson, journalRequest, toEntry, type JournalDetail, type JournalPage } from '../api/client';
import { useInfiniteQuery, useQueryClient } from '@tanstack/react-query';

interface JournalContextType {
  userId: string;
  entries: JournalEntry[];
  addEntry: (entry: Record<string, unknown>, key: string) => Promise<JournalEntry>;
  updateEntry: (id: string, entry: Record<string, unknown>, version: number) => Promise<JournalEntry>;
  deleteEntry: (id: string, version: number) => Promise<void>;
  getEntryById: (id: string) => JournalEntry | undefined;
  toggleFavorite: (id: string) => Promise<JournalEntry>;
  loadEntry: (id: string) => Promise<JournalEntry>;
  reloadEntries: () => Promise<void>;
  loadMoreEntries: () => Promise<void>;
  hasMoreEntries: boolean;
  entriesLoadingMore: boolean;
  entriesLoading: boolean;
  entriesError: string | null;
  streakCount: number;
  draftStorageKey: string;
  registerJournalLeaveCheck: (check: (() => boolean) | null) => void;
  hasUnsavedJournalChanges: () => boolean;
}

const JournalContext = createContext<JournalContextType | undefined>(undefined);

export function JournalProvider({ children, userId, streakCount, loadEntries }: {
  children: React.ReactNode;
  userId: string;
  streakCount: number;
  loadEntries: boolean;
}) {
  const queryClient = useQueryClient();
  const storageKey = useCallback((name: string) => `mylog_${userId}_${name}`, [userId]);
  const entriesKey = useMemo(() => ['journal', userId, 'entries'] as const, [userId]);
  const journalLeaveCheck = useRef<(() => boolean) | null>(null);
  const registerJournalLeaveCheck = useCallback((check: (() => boolean) | null) => {
    journalLeaveCheck.current = check;
  }, []);
  const hasUnsavedJournalChanges = useCallback(() => journalLeaveCheck.current?.() ?? false, []);

  const entriesQuery = useInfiniteQuery({
    queryKey: entriesKey,
    enabled: loadEntries,
    initialPageParam: null as string | null,
    queryFn: ({ pageParam }) => journalJson<JournalPage>(`journal-entries?limit=100${pageParam ? `&cursor=${encodeURIComponent(pageParam)}` : ''}`),
    getNextPageParam: (page) => page.nextCursor,
  });
  const entries = useMemo(() => entriesQuery.data?.pages.flatMap((page) => page.items.map(toEntry)) ?? [], [entriesQuery.data]);
  const entriesLoading = entriesQuery.isPending;
  const entriesError = entriesQuery.error instanceof Error ? entriesQuery.error.message : null;
  const reloadEntries = async () => { await entriesQuery.refetch(); };
  const loadMoreEntries = async () => { await entriesQuery.fetchNextPage(); };
  const refreshDerived = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['dashboard', userId] }),
    queryClient.invalidateQueries({ queryKey: ['insights', userId] }),
    queryClient.invalidateQueries({ queryKey: ['reports', userId] }),
    queryClient.invalidateQueries({ queryKey: ['journal', userId, 'recent'] }),
  ]);

  const addEntry = async (body: Record<string, unknown>, key: string) => {
    const saved = await journalJson<JournalDetail>('journal-entries', {
      method: 'POST', ...journalBody(body, { 'Idempotency-Key': key }),
    });
    const entry = toEntry(saved);
    await queryClient.invalidateQueries({ queryKey: entriesKey });
    void refreshDerived();
    return entry;
  };

  const updateEntry = async (id: string, body: Record<string, unknown>, version: number) => {
    const saved = await journalJson<JournalDetail>(`journal-entries/${id}`, {
      method: 'PATCH', ...journalBody(body, { 'If-Match': `"${version}"` }),
    });
    const entry = toEntry(saved);
    await queryClient.invalidateQueries({ queryKey: entriesKey });
    queryClient.removeQueries({ queryKey: ['analysis', userId, id] });
    void refreshDerived();
    return entry;
  };

  const deleteEntry = async (id: string, version: number) => {
    await journalRequest(`journal-entries/${id}`, { method: 'DELETE', headers: { 'If-Match': `"${version}"` } });
    await queryClient.invalidateQueries({ queryKey: entriesKey });
    queryClient.removeQueries({ queryKey: ['analysis', userId, id] });
    void refreshDerived();
  };

  const loadEntry = useCallback(async (id: string) => {
    const detail = await journalJson<JournalDetail>(`journal-entries/${id}`);
    const entry = toEntry(detail);
    return entry;
  }, []);

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
    await queryClient.invalidateQueries({ queryKey: entriesKey });
    return entry;
  };

  return (
    <JournalContext.Provider
      value={{
        userId,
        entries,
        addEntry,
        updateEntry,
        deleteEntry,
        getEntryById,
        toggleFavorite,
        loadEntry,
        reloadEntries,
        loadMoreEntries,
        hasMoreEntries: !!entriesQuery.hasNextPage,
        entriesLoadingMore: entriesQuery.isFetchingNextPage,
        entriesLoading,
        entriesError,
        streakCount,
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
