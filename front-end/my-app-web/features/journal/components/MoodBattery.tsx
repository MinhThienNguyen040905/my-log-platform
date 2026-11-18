'use client';

import { useRef, useState, type KeyboardEvent, type PointerEvent } from 'react';
import { motion, useReducedMotion } from 'motion/react';
import { Zap } from 'lucide-react';
import type { JournalEmotion } from '@/types';
import { JOURNAL_EMOTIONS, JOURNAL_EMOTION_ORDER } from '../utils/journal-emotions';

interface MoodBatteryProps {
  value: JournalEmotion | null;
  onChange: (emotion: JournalEmotion) => void;
  /** Tên của thanh kéo cho trình đọc màn hình. */
  label: string;
}

const LAST = JOURNAL_EMOTION_ORDER.length - 1;

/**
 * Thanh "pin cảm xúc": 5 ô màu từ cạn (rất tệ) đến đầy (vui). Kéo núm tia sét, bấm vào ô, hoặc dùng phím
 * mũi tên trên núm; núm chỉ dừng ở 5 ô cho sẵn. Mỗi ô vẫn là một nút có tên cảm xúc để trình đọc màn hình bấm được.
 */
export function MoodBattery({ value, onChange, label }: MoodBatteryProps) {
  const reduceMotion = useReducedMotion();
  const segmentRefs = useRef<(HTMLButtonElement | null)[]>([]);
  const thumbRef = useRef<HTMLSpanElement>(null);
  const [dragIndex, setDragIndex] = useState<number | null>(null);

  const valueIndex = value ? JOURNAL_EMOTION_ORDER.indexOf(value) : -1;
  const shownIndex = dragIndex ?? valueIndex;
  const thumbColumn = shownIndex >= 0 ? shownIndex : JOURNAL_EMOTION_ORDER.indexOf('neutral');

  const commit = (index: number) => {
    const emotion = JOURNAL_EMOTION_ORDER[index];
    if (emotion && emotion !== value) onChange(emotion);
  };

  /** Ô gần con trỏ nhất theo chiều ngang. */
  const nearestIndex = (clientX: number) => {
    let best = 0;
    let bestDistance = Infinity;
    segmentRefs.current.forEach((segment, index) => {
      if (!segment) return;
      const rect = segment.getBoundingClientRect();
      const distance = Math.abs(clientX - (rect.left + rect.width / 2));
      if (distance < bestDistance) { best = index; bestDistance = distance; }
    });
    return best;
  };

  const handlePointerDown = (event: PointerEvent<HTMLDivElement>) => {
    if (event.button !== 0) return;
    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    thumbRef.current?.focus({ preventScroll: true });
    setDragIndex(nearestIndex(event.clientX));
  };
  const handlePointerMove = (event: PointerEvent<HTMLDivElement>) => {
    if (dragIndex === null) return;
    const next = nearestIndex(event.clientX);
    if (next !== dragIndex) setDragIndex(next);
  };
  const handlePointerUp = (event: PointerEvent<HTMLDivElement>) => {
    if (dragIndex === null) return;
    const index = nearestIndex(event.clientX);
    setDragIndex(null);
    commit(index);
  };
  const handlePointerCancel = () => setDragIndex(null);

  const handleKeyDown = (event: KeyboardEvent<HTMLSpanElement>) => {
    const from = valueIndex >= 0 ? valueIndex : thumbColumn;
    let next: number | null = null;
    if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') next = valueIndex >= 0 ? Math.max(0, from - 1) : from;
    if (event.key === 'ArrowRight' || event.key === 'ArrowUp') next = valueIndex >= 0 ? Math.min(LAST, from + 1) : from;
    if (event.key === 'Home') next = 0;
    if (event.key === 'End') next = LAST;
    if (next === null) return;
    event.preventDefault();
    commit(next);
  };

  const shownLabel = shownIndex >= 0 ? JOURNAL_EMOTIONS[JOURNAL_EMOTION_ORDER[shownIndex]].label : null;

  return (
    <div className="flex w-full min-w-0 items-center">
      {/* Thân pin: 5 cột màu liền nhau, rãnh trượt nằm ngang bên trong các cột. */}
      <div
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerCancel}
        className="relative min-w-0 flex-1 touch-none select-none rounded-lg border-2 border-black shadow-neo-sm cursor-pointer"
      >
        <div className="grid grid-cols-5 overflow-hidden rounded-md">
          {JOURNAL_EMOTION_ORDER.map((id, index) => {
            const { label: name, icon: Face, swatch } = JOURNAL_EMOTIONS[id];
            const active = index === shownIndex;
            return (
              <button
                key={id}
                ref={(node) => { segmentRefs.current[index] = node; }}
                type="button"
                tabIndex={-1}
                aria-label={name}
                aria-pressed={id === value}
                title={name}
                // Chuột và cảm ứng được xử lý ở thân pin (kéo); chỉ nhận click từ bàn phím hoặc trình đọc màn hình.
                onClick={(event) => { if (event.detail === 0) commit(index); }}
                className={`relative flex h-10 flex-col items-center pt-1 ${swatch} ${index < LAST ? 'border-r border-black/30' : ''}`}
              >
                {/* Cột đang chọn sáng lên nhờ một lớp phủ trắng mờ. */}
                <span aria-hidden="true" className={`pointer-events-none absolute inset-0 bg-white/45 transition-opacity duration-150 ${active ? 'opacity-100' : 'opacity-0'}`} />
                <Face className={`relative h-3.5 w-3.5 transition-transform duration-150 ${active ? 'scale-125' : ''}`} strokeWidth={2.4} aria-hidden="true" />
              </button>
            );
          })}
        </div>

        {/* Rãnh trượt nối tâm cột đầu tới tâm cột cuối. */}
        <span aria-hidden="true" className="pointer-events-none absolute bottom-[5px] left-[10%] right-[10%] h-1.5 rounded-full bg-black/85" />
        <div className="pointer-events-none absolute inset-x-0 bottom-0 grid h-4 grid-cols-5">
          <motion.span
            ref={thumbRef}
            layout={!reduceMotion}
            transition={{ type: 'spring', stiffness: 500, damping: 34 }}
            role="slider"
            tabIndex={0}
            aria-label={label}
            aria-valuemin={1}
            aria-valuemax={JOURNAL_EMOTION_ORDER.length}
            aria-valuenow={valueIndex >= 0 ? valueIndex + 1 : undefined}
            aria-valuetext={shownLabel ?? 'Chưa chọn'}
            title={shownLabel ? `${label}: ${shownLabel}` : label}
            onKeyDown={handleKeyDown}
            style={{ gridColumnStart: thumbColumn + 1 }}
            className={`pointer-events-auto row-start-1 flex h-6 w-6 items-center justify-center justify-self-center self-center rounded-sm cursor-grab active:cursor-grabbing focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${
              shownIndex >= 0 ? '' : 'opacity-50'
            }`}
          >
            <Zap className="h-6 w-6 fill-yellow-300 text-black drop-shadow-[0_1px_0_rgba(0,0,0,0.35)]" strokeWidth={2} aria-hidden="true" />
          </motion.span>
        </div>
      </div>
      {/* Đầu pin. */}
      <span aria-hidden="true" className="h-4 w-1 shrink-0 rounded-r-sm border-2 border-l-0 border-black bg-black" />
    </div>
  );
}
