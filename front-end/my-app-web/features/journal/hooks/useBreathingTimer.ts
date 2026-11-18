'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

export type BreathPhase = 'Hít vào chậm...' | 'Giữ nhẹ...' | 'Thở ra từ từ...';

const PHASES: BreathPhase[] = ['Hít vào chậm...', 'Giữ nhẹ...', 'Thở ra từ từ...'];
/** Nhịp 4-4-4: mỗi pha 4 giây. */
export const PHASE_SECONDS = 4;

/** A generic countdown with a cycling 3-phase breathing cadence (hít vào / giữ / thở ra). */
export function useBreathingTimer(totalSeconds: number, onComplete?: () => void) {
  const [secondsLeft, setSecondsLeft] = useState(totalSeconds);
  const [running, setRunning] = useState(false);
  const onCompleteRef = useRef(onComplete);

  useEffect(() => {
    onCompleteRef.current = onComplete;
  }, [onComplete]);

  useEffect(() => {
    if (!running) return undefined;
    const interval = setInterval(() => {
      setSecondsLeft((previous) => {
        if (previous <= 1) {
          setRunning(false);
          onCompleteRef.current?.();
          return 0;
        }
        return previous - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [running]);

  const start = useCallback(() => {
    setSecondsLeft(totalSeconds);
    setRunning(true);
  }, [totalSeconds]);

  const stop = useCallback(() => setRunning(false), []);

  const elapsed = totalSeconds - secondsLeft;
  const phase = PHASES[Math.floor(elapsed / PHASE_SECONDS) % PHASES.length];
  /** Giây thứ mấy trong pha hiện tại (1..PHASE_SECONDS), để đếm theo nhịp cho người dùng. */
  const count = (elapsed % PHASE_SECONDS) + 1;
  const minutes = Math.floor(secondsLeft / 60);
  const seconds = secondsLeft % 60;
  const label = `${minutes}:${String(seconds).padStart(2, '0')}`;

  return { running, secondsLeft, phase, count, label, start, stop };
}
