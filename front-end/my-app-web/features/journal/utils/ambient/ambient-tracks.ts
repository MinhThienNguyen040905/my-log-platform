import type { JournalEmotion } from '@/types';
import type { TrackBuilder } from './audio-blocks';
import { cafe, fireplace, lofiPiano, ocean, rain } from './tracks';

export type AmbientTrackId = 'rain' | 'lofi-piano' | 'ocean' | 'fireplace' | 'cafe';

export type AmbientTrack = {
  id: AmbientTrackId;
  title: string;
  description: string;
  build: TrackBuilder;
  /** Per-track trim so every track plays at roughly the same loudness. */
  level: number;
};

export const AMBIENT_TRACKS: AmbientTrack[] = [
  // `level` balances the >300 Hz loudness (what small speakers play) around rain's −19 dB, measured
  // 2026-11-11 in Chromium: rain −19.4, ocean −21.1, fireplace −23.6, lofi −33.9, cafe −32.1 dB.
  { id: 'rain', title: 'Tiếng mưa rơi êm dịu', description: 'Mưa nhẹ ngoài hiên', build: rain, level: 1 },
  { id: 'lofi-piano', title: 'Piano lofi trầm lắng', description: 'Hợp âm chậm, tiếng đĩa than', build: lofiPiano, level: 4 },
  { id: 'ocean', title: 'Sóng biển & gió nhẹ', description: 'Sóng vỗ đều, gió thoảng', build: ocean, level: 1.2 },
  { id: 'fireplace', title: 'Lửa trại đêm', description: 'Lửa ấm, củi nổ lách tách', build: fireplace, level: 1.5 },
  { id: 'cafe', title: 'Quán cà phê yên tĩnh', description: 'Tiếng trò chuyện xa, tách chén', build: cafe, level: 3.5 },
];

export const DEFAULT_TRACK: AmbientTrackId = 'rain';

export function trackById(id: string | null | undefined): AmbientTrack {
  return AMBIENT_TRACKS.find((track) => track.id === id) ?? AMBIENT_TRACKS[0];
}

/**
 * Tracks suggested for the mood picked on the journal page. Suggestions are only shown as a chip —
 * nothing ever starts playing on its own (COMPANION_UI_PLAN.md §2.5 "không tự phát âm thanh").
 * `angry` is the closest mood to anxiety/stress in JournalMoodBar (it maps to `anxiety-stress`).
 */
export const MOOD_TRACK_SUGGESTIONS: Partial<Record<JournalEmotion, AmbientTrackId[]>> = {
  angry: ['rain', 'ocean'],
  sad: ['fireplace', 'lofi-piano'],
  very_bad: ['fireplace', 'lofi-piano'],
  happy: ['cafe', 'lofi-piano'],
};
