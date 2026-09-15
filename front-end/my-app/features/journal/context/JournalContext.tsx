'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import { JournalEntry, WellnessGoal, DailyStat, UserProfile } from '@/lib/types';
import { INITIAL_ENTRIES, MOCK_GOALS, MOCK_14_DAYS_STATS } from '@/lib/mock-data';

const DEFAULT_PROFILE: UserProfile = {
  name: 'Minh Anh',
  penName: 'Minh Anh',
  email: 'minhanh.journal@gmail.com',
  plan: 'FREE',
  timezone: 'Asia/Ho_Chi_Minh',
  avatarUrl: '/avatar.png',
  language: 'vi',
};

interface JournalContextType {
  entries: JournalEntry[];
  goals: WellnessGoal[];
  stats: DailyStat[];
  addEntry: (entry: Omit<JournalEntry, 'id'>) => JournalEntry;
  updateEntry: (id: string, entry: Partial<JournalEntry>) => void;
  deleteEntry: (id: string) => void;
  toggleGoal: (id: string) => void;
  getEntryById: (id: string) => JournalEntry | undefined;
  toggleFavorite: (id: string) => void;
  userProfile: UserProfile;
  updateProfile: (data: Partial<UserProfile>) => void;
  logout: () => void;
  streakCount: number;
}

const JournalContext = createContext<JournalContextType | undefined>(undefined);

export function JournalProvider({ children }: { children: React.ReactNode }) {
  const [entries, setEntries] = useState<JournalEntry[]>(INITIAL_ENTRIES);
  const [goals, setGoals] = useState<WellnessGoal[]>(MOCK_GOALS);
  const [stats] = useState<DailyStat[]>(MOCK_14_DAYS_STATS);
  const [userProfile, setUserProfile] = useState<UserProfile>(DEFAULT_PROFILE);
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    try {
      const stored = localStorage.getItem('mylog_entries');
      if (stored) {
        setEntries(JSON.parse(stored));
      }
      const storedGoals = localStorage.getItem('mylog_goals');
      if (storedGoals) {
        setGoals(JSON.parse(storedGoals));
      }
      const storedProfile = localStorage.getItem('mylog_user_profile');
      if (storedProfile) {
        setUserProfile(JSON.parse(storedProfile));
      }
    } catch (e) {
      console.error('Failed to load from localStorage:', e);
    }
    setIsLoaded(true);
  }, []);

  useEffect(() => {
    if (!isLoaded) return;
    try {
      localStorage.setItem('mylog_entries', JSON.stringify(entries));
    } catch (e) {
      console.error('Failed to save entries:', e);
    }
  }, [entries, isLoaded]);

  useEffect(() => {
    if (!isLoaded) return;
    try {
      localStorage.setItem('mylog_goals', JSON.stringify(goals));
    } catch (e) {
      console.error('Failed to save goals:', e);
    }
  }, [goals, isLoaded]);

  useEffect(() => {
    if (!isLoaded) return;
    try {
      localStorage.setItem('mylog_user_profile', JSON.stringify(userProfile));
    } catch (e) {
      console.error('Failed to save user profile:', e);
    }
  }, [userProfile, isLoaded]);

  const addEntry = (data: Omit<JournalEntry, 'id'>): JournalEntry => {
    const newEntry: JournalEntry = {
      ...data,
      id: 'entry-' + Date.now(),
      status: data.status || 'ANALYZED',
    };
    setEntries(prev => [newEntry, ...prev]);
    return newEntry;
  };

  const updateEntry = (id: string, updated: Partial<JournalEntry>) => {
    setEntries(prev => prev.map(e => (e.id === id ? { ...e, ...updated, updatedAt: new Date().toISOString() } : e)));
  };

  const deleteEntry = (id: string) => {
    setEntries(prev => prev.filter(e => e.id !== id));
  };

  const getEntryById = (id: string): JournalEntry | undefined => {
    return entries.find(e => e.id === id);
  };

  const toggleFavorite = (id: string) => {
    setEntries(prev => prev.map(e => (e.id === id ? { ...e, isFavorite: !e.isFavorite } : e)));
  };

  const updateProfile = (data: Partial<UserProfile>) => {
    setUserProfile(prev => ({ ...prev, ...data }));
  };

  const logout = () => {
    localStorage.removeItem('mylog_draft_journal');
    // Reserved for clearing tokens when backend is integrated
  };

  const toggleGoal = (id: string) => {
    setGoals(prev => prev.map(g => {
      if (g.id !== id) return g;
      const completed = !g.completed;
      return {
        ...g,
        completed,
        completedDays: completed ? g.targetDays : Math.max(0, g.completedDays - 1)
      };
    }));
  };

  return (
    <JournalContext.Provider
      value={{
        entries,
        goals,
        stats,
        addEntry,
        updateEntry,
        deleteEntry,
        toggleGoal,
        getEntryById,
        toggleFavorite,
        userProfile,
        updateProfile,
        logout,
        streakCount: 14 + (entries.length > INITIAL_ENTRIES.length ? 1 : 0),
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
