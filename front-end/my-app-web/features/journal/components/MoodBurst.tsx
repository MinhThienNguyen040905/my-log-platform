'use client';

import { createPortal } from 'react-dom';
import { motion } from 'motion/react';
import type { JournalEmotion } from '@/types';
import { JOURNAL_EMOTIONS } from '../utils/journal-emotions';

export interface MoodBurstOrigin {
  id: number;
  emotion: JournalEmotion;
  /** Tâm và kích thước của nút cảm xúc lúc bấm (toạ độ viewport). */
  x: number;
  y: number;
  width: number;
  height: number;
}

/** Thời gian dài nhất của một hiệu ứng, dùng để dọn burst khỏi state. */
export const MOOD_BURST_DURATION_MS = 1400;

type Keyframes = { x: number[]; y: number[]; opacity: number[]; scale: number[]; rotate?: number[] };
interface Particle {
  keyframes: Keyframes;
  duration: number;
  delay: number;
  size: number;
}

const TINT: Record<JournalEmotion, string> = {
  neutral: 'text-slate-500',
  happy: 'text-amber-500',
  angry: 'text-orange-500 fill-yellow-300',
  sad: 'text-blue-500',
  very_bad: 'text-indigo-700',
};

/** Tia sét chạy vòng quanh nút theo hình elip. */
function orbit(rx: number, ry: number): Particle[] {
  const count = 5;
  const steps = 10;
  return Array.from({ length: count }, (_, i) => {
    const start = (i / count) * Math.PI * 2;
    const angles = Array.from({ length: steps + 1 }, (_, k) => start + (k / steps) * Math.PI * 1.6);
    return {
      keyframes: {
        x: angles.map((a) => Math.cos(a) * rx),
        y: angles.map((a) => Math.sin(a) * ry),
        opacity: angles.map((_, k) => (k === 0 || k === steps ? 0 : 1)),
        scale: angles.map((_, k) => (k === 0 ? 0.4 : k === steps ? 0.5 : 1)),
        rotate: angles.map((_, k) => (k % 2 === 0 ? -12 : 12)),
      },
      duration: 0.95,
      delay: 0,
      size: 17,
    };
  });
}

/** Mặt trời nhỏ bung ra tỏa tròn. */
function radiate(rx: number, ry: number): Particle[] {
  const count = 8;
  return Array.from({ length: count }, (_, i) => {
    const a = (i / count) * Math.PI * 2;
    return {
      keyframes: {
        x: [0, Math.cos(a) * (rx + 10)],
        y: [0, Math.sin(a) * (ry + 10)],
        opacity: [0, 1, 0],
        scale: [0.3, 1, 0.6],
        rotate: [0, 120],
      },
      duration: 0.8,
      delay: 0,
      size: 16,
    };
  });
}

/** Mây trôi chậm sang hai bên rồi tan. */
function drift(rx: number): Particle[] {
  return [-1, 1, -1, 1].map((side, i) => ({
    keyframes: {
      x: [side * (rx - 18), side * (rx + 22 + i * 6)],
      y: [i < 2 ? -6 : 6, i < 2 ? -14 : 10],
      opacity: [0, 1, 0],
      scale: [0.6, 1, 0.9],
    },
    duration: 1.2,
    delay: i * 0.08,
    size: 19,
  }));
}

/** Rơi từ cạnh dưới nút xuống, như mưa. */
function fall(width: number, height: number, count: number, distance: number, duration: number): Particle[] {
  return Array.from({ length: count }, (_, i) => {
    const x = ((i + 0.5) / count - 0.5) * width * 0.8;
    return {
      keyframes: {
        x: [x, x + 2],
        y: [height / 2 - 4, height / 2 + distance],
        opacity: [0, 1, 0],
        scale: [0.6, 1, 0.8],
      },
      duration,
      delay: (i % 3) * 0.1 + Math.floor(i / 3) * 0.05,
      size: 15,
    };
  });
}

function particlesFor({ emotion, width, height }: MoodBurstOrigin): Particle[] {
  const rx = width / 2 + 14;
  const ry = height / 2 + 12;
  switch (emotion) {
    case 'angry':
      return orbit(rx, ry);
    case 'happy':
      return radiate(rx, ry);
    case 'neutral':
      return drift(rx);
    case 'sad':
      return fall(width, height, 4, 28, 1.1);
    case 'very_bad':
      return fall(width, height, 7, 36, 0.9);
  }
}

/** Hiệu ứng vẽ trên lớp phủ toàn trang để không bị khung cuộn ngang của hàng cảm xúc cắt mất. */
export function MoodBurstLayer({ bursts }: { bursts: MoodBurstOrigin[] }) {
  if (bursts.length === 0 || typeof document === 'undefined') return null;
  return createPortal(
    <div aria-hidden="true" className="pointer-events-none fixed inset-0 z-[60]">
      {bursts.map((burst) => {
        const Icon = JOURNAL_EMOTIONS[burst.emotion].icon;
        return (
          <div key={burst.id} className="absolute" style={{ left: burst.x, top: burst.y }}>
            {particlesFor(burst).map((p, i) => (
              <motion.span
                key={i}
                className={`absolute ${TINT[burst.emotion]}`}
                style={{ left: -p.size / 2, top: -p.size / 2, width: p.size, height: p.size }}
                initial={{ opacity: 0 }}
                animate={p.keyframes}
                transition={{ duration: p.duration, delay: p.delay, ease: 'easeOut' }}
              >
                <Icon style={{ width: p.size, height: p.size }} strokeWidth={2.4} />
              </motion.span>
            ))}
          </div>
        );
      })}
    </div>,
    document.body,
  );
}

/** Chuyển động nhỏ của chính nút khi bấm, đi kèm hiệu ứng. */
export const MOOD_BUTTON_FEEDBACK: Record<JournalEmotion, Record<string, number[]>> = {
  neutral: { scale: [1, 1.05, 1] },
  happy: { y: [0, -4, 0], scale: [1, 1.08, 1] },
  angry: { x: [0, -3, 3, -3, 3, 0] },
  sad: { y: [0, 2, 0] },
  very_bad: { y: [0, 4, 0], scale: [1, 0.96, 1] },
};
