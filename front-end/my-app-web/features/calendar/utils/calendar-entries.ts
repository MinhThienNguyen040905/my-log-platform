import type { JournalEmotion, JournalEntry } from '@/types';
import { normalizeJournalEmotion } from '@/features/journal';

export type HistoryFilters = {
  date: string | null;
  emotion: JournalEmotion | 'all';
  favorites: boolean;
  search: string;
};

export const EMPTY_FILTERS: HistoryFilters = { date: null, emotion: 'all', favorites: false, search: '' };

export function indexEntriesByDate(entries: JournalEntry[]): Record<string, JournalEntry[]> {
  const byDate: Record<string, JournalEntry[]> = {};
  for (const entry of entries) {
    if (!byDate[entry.date]) byDate[entry.date] = [];
    byDate[entry.date].push(entry);
  }
  return byDate;
}

export function averageMoodScore(entries: JournalEntry[]): string | null {
  const scored = entries.filter((entry) => entry.moodScore !== null);
  return scored.length
    ? (scored.reduce((sum, entry) => sum + (entry.moodScore ?? 0), 0) / scored.length).toFixed(1)
    : null;
}

export function filterEntries(entries: JournalEntry[], filters: HistoryFilters): JournalEntry[] {
  const search = filters.search.toLowerCase();
  return entries.filter((entry) => {
    const matchesMood = filters.emotion === 'all'
      || (entry.emotion ? normalizeJournalEmotion(entry.emotion) === filters.emotion
        : (filters.emotion === 'neutral' && entry.mood === 'neutral')
          || (filters.emotion === 'happy' && (entry.mood === 'calm-joy' || entry.mood === 'hope-energy'))
          || (filters.emotion === 'angry' && entry.mood === 'anxiety-stress')
          || (filters.emotion === 'sad' && entry.mood === 'sadness-reflect'));
    const matchesFavorite = !filters.favorites || !!entry.isFavorite;
    const matchesDate = !filters.date || entry.date === filters.date;
    const matchesSearch = entry.title.toLowerCase().includes(search)
      || entry.content.replace(/<[^>]*>/g, ' ').toLowerCase().includes(search)
      || entry.tags.some((tag) => tag.toLowerCase().includes(search));
    return matchesMood && matchesFavorite && matchesDate && matchesSearch;
  });
}
