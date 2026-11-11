'use client';

import { useEffect, useRef, useState, type MouseEvent, type ReactNode } from 'react';
import { animate, useReducedMotion } from 'motion/react';
import type { JournalEmotion } from '@/types';
import { JOURNAL_EMOTIONS, JOURNAL_EMOTION_ORDER } from '../utils/journal-emotions';
import { MOOD_BURST_DURATION_MS, MOOD_BUTTON_FEEDBACK, MoodBurstLayer, type MoodBurstOrigin } from './MoodBurst';

interface JournalMoodBarProps {
  isSelected: (emotion: JournalEmotion) => boolean;
  onSelect: (emotion: JournalEmotion) => void;
  /** Nội dung căn phải cùng hàng (chuỗi ngày viết, điều hướng khi sửa bài). */
  trailing?: ReactNode;
}

export function JournalMoodBar({ isSelected, onSelect, trailing }: JournalMoodBarProps) {
  const reduceMotion = useReducedMotion();
  const [bursts, setBursts] = useState<MoodBurstOrigin[]>([]);
  const nextBurstId = useRef(0);
  const timers = useRef<number[]>([]);

  useEffect(() => () => timers.current.forEach((t) => window.clearTimeout(t)), []);

  const handleClick = (emotion: JournalEmotion, event: MouseEvent<HTMLButtonElement>) => {
    onSelect(emotion);
    if (reduceMotion) return;

    const button = event.currentTarget;
    animate(button, MOOD_BUTTON_FEEDBACK[emotion], { duration: 0.45, ease: 'easeOut' });

    const rect = button.getBoundingClientRect();
    const id = nextBurstId.current++;
    setBursts((prev) => [
      ...prev,
      { id, emotion, x: rect.left + rect.width / 2, y: rect.top + rect.height / 2, width: rect.width, height: rect.height },
    ]);
    timers.current.push(
      window.setTimeout(() => setBursts((prev) => prev.filter((b) => b.id !== id)), MOOD_BURST_DURATION_MS),
    );
  };

  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
      <span id="journal-mood-label" className="order-1 shrink-0 font-space text-xs font-extrabold text-on-surface">
        Cảm xúc hôm nay
      </span>
      <div className="order-3 w-full min-w-0 sm:order-2 sm:w-auto sm:flex-1">
        <div
          role="group"
          aria-labelledby="journal-mood-label"
          className="-my-1 flex min-w-0 items-center gap-2 overflow-x-auto py-1 pr-1"
        >
          {JOURNAL_EMOTION_ORDER.map((id) => {
            const selected = isSelected(id);
            return (
              <button
                key={id}
                type="button"
                onClick={(event) => handleClick(id, event)}
                aria-pressed={selected}
                className={`shrink-0 rounded-xl border-2 border-black px-3.5 py-1.5 font-space text-sm font-bold transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${
                  selected ? 'bg-primary-container shadow-neo-sm' : 'bg-white'
                }`}
              >
                {JOURNAL_EMOTIONS[id].label}
              </button>
            );
          })}
        </div>
      </div>
      {trailing && <div className="order-2 ml-auto flex shrink-0 flex-wrap items-center justify-end gap-2 sm:order-3">{trailing}</div>}
      <MoodBurstLayer bursts={bursts} />
    </div>
  );
}
