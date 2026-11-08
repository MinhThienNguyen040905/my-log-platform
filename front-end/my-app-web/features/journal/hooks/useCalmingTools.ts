'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { closeAmbientAudioContext, createGentleRainAudioContext } from '../utils/ambient-sound';
import { useBreathingTimer } from './useBreathingTimer';

export type CalmingTool = 'BREATHING' | 'AMBIENT' | null;
/** MENU = nút "Công cụ dịu" trên thanh công cụ; CHAT = bấm hành động bên trong khung chat trợ lý. */
export type CalmingToolSource = 'MENU' | 'CHAT';

const BREATHING_SECONDS = 120;

/**
 * Runs the "Công cụ dịu" tools entirely on the device: no network request, no consent needed.
 * Only one tool runs at a time. Ambient audio always stops when the hook unmounts (e.g. leaving
 * the journal editor page).
 *
 * `onNaturalComplete` fires only when Thở chậm finishes its own countdown (not on manual Dừng) —
 * used by the companion chat to send a `TOOL_COMPLETED` turn, but only when the tool was started
 * from inside the chat (`source: 'CHAT'`). Âm thanh dịu has no natural end (loops until stopped),
 * so it never fires this.
 */
export function useCalmingTools(onNaturalComplete?: (tool: 'BREATHING', source: CalmingToolSource) => void) {
  const [activeTool, setActiveTool] = useState<CalmingTool>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const sourceRef = useRef<CalmingToolSource>('MENU');
  const onNaturalCompleteRef = useRef(onNaturalComplete);
  useEffect(() => {
    onNaturalCompleteRef.current = onNaturalComplete;
  }, [onNaturalComplete]);

  const breathing = useBreathingTimer(BREATHING_SECONDS, () => {
    setActiveTool(null);
    onNaturalCompleteRef.current?.('BREATHING', sourceRef.current);
  });

  const stopAmbient = useCallback(() => {
    closeAmbientAudioContext(audioCtxRef.current);
    audioCtxRef.current = null;
  }, []);

  const stop = useCallback(() => {
    breathing.stop();
    stopAmbient();
    setActiveTool(null);
  }, [breathing, stopAmbient]);

  const startBreathing = useCallback(
    (source: CalmingToolSource = 'MENU') => {
      sourceRef.current = source;
      stopAmbient();
      breathing.start();
      setActiveTool('BREATHING');
    },
    [breathing, stopAmbient],
  );

  const startAmbient = useCallback(
    (source: CalmingToolSource = 'MENU'): boolean => {
      sourceRef.current = source;
      breathing.stop();
      const ctx = createGentleRainAudioContext();
      if (!ctx) return false;
      audioCtxRef.current = ctx;
      setActiveTool('AMBIENT');
      return true;
    },
    [breathing],
  );

  // Leaving the page (or the editor unmounting) must always stop any playing audio.
  useEffect(() => () => stopAmbient(), [stopAmbient]);

  return { activeTool, breathing, startBreathing, startAmbient, stop };
}
