'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { JournalEmotion } from '@/types';
import { AmbientEngine } from '../utils/ambient/ambient-engine';
import {
  AMBIENT_TRACKS, DEFAULT_TRACK, MOOD_TRACK_SUGGESTIONS, trackById, type AmbientTrackId,
} from '../utils/ambient/ambient-tracks';

export type AmbientStatus = 'stopped' | 'playing' | 'paused';
/** null = không hẹn giờ (phát tới khi tắt). */
export type AmbientTimer = 15 | 30 | null;

const PREFS_KEY = 'mylog-ambient-player';
const TIMER_FADE_SECONDS = 5;

type Prefs = { volume: number; muted: boolean; trackId: AmbientTrackId };

// Per-browser convenience only (volume, last track); losing it is harmless.
function readPrefs(): Prefs {
  const fallback: Prefs = { volume: 0.8, muted: false, trackId: DEFAULT_TRACK };
  try {
    const raw: unknown = JSON.parse(localStorage.getItem(PREFS_KEY) ?? 'null');
    if (!raw || typeof raw !== 'object') return fallback;
    const value = raw as Partial<Prefs>;
    return {
      volume: typeof value.volume === 'number' && value.volume >= 0 && value.volume <= 1 ? value.volume : fallback.volume,
      muted: typeof value.muted === 'boolean' ? value.muted : fallback.muted,
      trackId: trackById(value.trackId).id,
    };
  } catch {
    return fallback;
  }
}

function writePrefs(prefs: Prefs) {
  try { localStorage.setItem(PREFS_KEY, JSON.stringify(prefs)); } catch { /* storage unavailable */ }
}

/**
 * Máy phát nhạc nền của trang viết. Chạy hoàn toàn trên máy (Web Audio), không gọi mạng, không cần
 * consent. Chạy song song được với "Thở chậm". Không bao giờ tự phát: chỉ phát khi người dùng bấm.
 * Rời trang (hook unmount) luôn dừng âm thanh.
 */
export function useAmbientPlayer(mood: JournalEmotion | null | undefined) {
  const engineRef = useRef<AmbientEngine | null>(null);
  const [open, setOpen] = useState(false);
  const [status, setStatus] = useState<AmbientStatus>('stopped');
  const [trackId, setTrackId] = useState<AmbientTrackId>(DEFAULT_TRACK);
  const [volume, setVolumeState] = useState(0.8);
  const [muted, setMuted] = useState(false);
  const [timer, setTimerState] = useState<AmbientTimer>(null);
  const [timerEndsAt, setTimerEndsAt] = useState<number | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [dismissedMood, setDismissedMood] = useState<JournalEmotion | null>(null);
  const [unavailable, setUnavailable] = useState(false);

  // Load per-browser preferences after mount (localStorage is not available during SSR).
  useEffect(() => {
    const prefs = readPrefs();
    const id = window.setTimeout(() => {
      setVolumeState(prefs.volume);
      setMuted(prefs.muted);
      setTrackId(prefs.trackId);
    }, 0);
    return () => window.clearTimeout(id);
  }, []);

  const engine = useCallback(() => {
    engineRef.current ??= new AmbientEngine();
    return engineRef.current;
  }, []);

  const persist = useCallback((next: Partial<Prefs>) => {
    writePrefs({ volume, muted, trackId, ...next });
  }, [volume, muted, trackId]);

  const playTrack = useCallback((id: AmbientTrackId): boolean => {
    const instance = engine();
    instance.setVolume(volume, muted);
    const ok = instance.play(trackById(id));
    if (!ok) { setUnavailable(true); return false; }
    setUnavailable(false);
    setTrackId(id);
    setStatus('playing');
    setOpen(true);
    setNotice(null);
    persist({ trackId: id });
    return true;
  }, [engine, muted, persist, volume]);

  /** Mở máy phát và phát (mặc định bài gần nhất). Trả về false nếu trình duyệt không hỗ trợ. */
  const openAndPlay = useCallback((id?: AmbientTrackId) => playTrack(id ?? trackId), [playTrack, trackId]);

  const togglePlay = useCallback(() => {
    if (status === 'playing') { engine().pause(); setStatus('paused'); return; }
    if (status === 'paused') { engine().resume(); setStatus('playing'); return; }
    playTrack(trackId);
  }, [engine, playTrack, status, trackId]);

  const step = useCallback((delta: number) => {
    const index = AMBIENT_TRACKS.findIndex((track) => track.id === trackId);
    const next = AMBIENT_TRACKS[(index + delta + AMBIENT_TRACKS.length) % AMBIENT_TRACKS.length].id;
    playTrack(next);
  }, [playTrack, trackId]);

  const setVolume = useCallback((value: number) => {
    const next = Math.min(1, Math.max(0, value));
    setVolumeState(next);
    const unmute = muted && next > 0;
    if (unmute) setMuted(false);
    engineRef.current?.setVolume(next, unmute ? false : muted);
    persist({ volume: next, muted: unmute ? false : muted });
  }, [muted, persist]);

  const toggleMute = useCallback(() => {
    const next = !muted;
    setMuted(next);
    engineRef.current?.setVolume(volume, next);
    persist({ muted: next });
  }, [muted, persist, volume]);

  const close = useCallback(() => {
    engineRef.current?.close();
    setStatus('stopped');
    setOpen(false);
    setTimerState(null);
    setTimerEndsAt(null);
  }, []);

  const setTimer = useCallback((minutes: AmbientTimer) => {
    setTimerState(minutes);
    setTimerEndsAt(minutes === null ? null : Date.now() + minutes * 60_000);
  }, []);

  // Sleep/focus timer: fade out, stop, and tell the user why the music stopped.
  useEffect(() => {
    if (timerEndsAt === null || status === 'stopped') return;
    const id = window.setTimeout(() => {
      void engine().fadeOutAndClose(TIMER_FADE_SECONDS).then(() => {
        setStatus('stopped');
        setTimerState(null);
        setTimerEndsAt(null);
        setNotice('Đã tắt nhạc nền theo hẹn giờ.');
      });
    }, Math.max(0, timerEndsAt - Date.now()));
    return () => window.clearTimeout(id);
  }, [engine, status, timerEndsAt]);

  // Leaving the editor must always stop the sound.
  useEffect(() => () => engineRef.current?.close(), []);

  const suggestion = useMemo(() => {
    if (!mood || dismissedMood === mood) return null;
    const ids = MOOD_TRACK_SUGGESTIONS[mood];
    if (!ids) return null;
    const id = ids.find((candidate) => !(status === 'playing' && candidate === trackId));
    if (!id || (status === 'playing' && ids.includes(trackId))) return null;
    return trackById(id);
  }, [dismissedMood, mood, status, trackId]);

  const dismissSuggestion = useCallback(() => setDismissedMood(mood ?? null), [mood]);

  return {
    open, status, track: trackById(trackId), tracks: AMBIENT_TRACKS, volume, muted, timer, timerEndsAt, notice, unavailable,
    suggestion, openAndPlay, togglePlay, next: () => step(1), previous: () => step(-1), select: playTrack,
    setVolume, toggleMute, setTimer, close, dismissSuggestion, dismissNotice: () => setNotice(null),
  };
}

export type AmbientPlayer = ReturnType<typeof useAmbientPlayer>;
