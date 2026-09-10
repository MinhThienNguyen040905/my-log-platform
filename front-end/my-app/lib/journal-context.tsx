'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import { JournalEntry, WellnessGoal, DailyStat } from './types';
import { INITIAL_ENTRIES, MOCK_GOALS, MOCK_14_DAYS_STATS } from './mock-data';

interface JournalContextType {
  entries: JournalEntry[];
  goals: WellnessGoal[];
  stats: DailyStat[];
  addEntry: (entry: Omit<JournalEntry, 'id'>) => JournalEntry;
  updateEntry: (id: string, entry: Partial<JournalEntry>) => void;
  deleteEntry: (id: string) => void;
  toggleGoal: (id: string) => void;
  streakCount: number;
}

const JournalContext = createContext<JournalContextType | undefined>(undefined);

export function JournalProvider({ children }: { children: React.ReactNode }) {
  const [entries, setEntries] = useState<JournalEntry[]>(INITIAL_ENTRIES);
  const [goals, setGoals] = useState<WellnessGoal[]>(MOCK_GOALS);
  const [stats] = useState<DailyStat[]>(MOCK_14_DAYS_STATS);
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

  const addEntry = (data: Omit<JournalEntry, 'id'>): JournalEntry => {
    const newEntry: JournalEntry = {
      ...data,
      id: 'entry-' + Date.now(),
    };
    setEntries(prev => [newEntry, ...prev]);
    return newEntry;
  };

  const updateEntry = (id: string, updated: Partial<JournalEntry>) => {
    setEntries(prev => prev.map(e => (e.id === id ? { ...e, ...updated } : e)));
  };

  const deleteEntry = (id: string) => {
    setEntries(prev => prev.filter(e => e.id !== id));
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
