'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { useBreathingTimer } from './useBreathingTimer';

export type CalmingTool = 'BREATHING' | null;
/** MENU = nút "Công cụ dịu" trên thanh công cụ; CHAT = bấm hành động bên trong khung chat trợ lý. */
export type CalmingToolSource = 'MENU' | 'CHAT';

const BREATHING_SECONDS = 120;

/**
 * Runs "Thở chậm 2 phút" entirely on the device: no network request, no consent needed.
 * "Nghe âm thanh dịu" now lives in the background-music player (useAmbientPlayer), which can play
 * alongside breathing.
 *
 * `onNaturalComplete` fires only when Thở chậm finishes its own countdown (not on manual Dừng) —
 * used by the companion chat to send a `TOOL_COMPLETED` turn, but only when the tool was started
 * from inside the chat (`source: 'CHAT'`).
 */
export function useCalmingTools(onNaturalComplete?: (tool: 'BREATHING', source: CalmingToolSource) => void) {
  const [activeTool, setActiveTool] = useState<CalmingTool>(null);
  const sourceRef = useRef<CalmingToolSource>('MENU');
  const onNaturalCompleteRef = useRef(onNaturalComplete);
  useEffect(() => {
    onNaturalCompleteRef.current = onNaturalComplete;
  }, [onNaturalComplete]);

  const breathing = useBreathingTimer(BREATHING_SECONDS, () => {
    setActiveTool(null);
    onNaturalCompleteRef.current?.('BREATHING', sourceRef.current);
  });

  const stop = useCallback(() => {
    breathing.stop();
    setActiveTool(null);
  }, [breathing]);

  const startBreathing = useCallback(
    (source: CalmingToolSource = 'MENU') => {
      sourceRef.current = source;
      breathing.start();
      setActiveTool('BREATHING');
    },
    [breathing],
  );

  return { activeTool, breathing, startBreathing, stop };
}
