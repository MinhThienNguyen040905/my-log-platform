'use client';

import type { ReactNode } from 'react';
import type { JournalEmotion } from '@/types';
import { JOURNAL_EMOTION_ORDER } from '../utils/journal-emotions';
import { MoodBattery } from './MoodBattery';

interface JournalMoodBarProps {
  isSelected: (emotion: JournalEmotion) => boolean;
  onSelect: (emotion: JournalEmotion) => void;
  /** Nội dung căn phải cùng hàng (chuỗi ngày viết, điều hướng khi sửa bài). */
  trailing?: ReactNode;
}

export function JournalMoodBar({ isSelected, onSelect, trailing }: JournalMoodBarProps) {
  const selected = JOURNAL_EMOTION_ORDER.find(isSelected) ?? null;

  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
      <div className="w-[254px] max-w-full shrink-0">
        <MoodBattery value={selected} onChange={onSelect} label="Cảm xúc hôm nay" />
      </div>
      {trailing && <div className="ml-auto flex shrink-0 flex-wrap items-center justify-end gap-2">{trailing}</div>}
    </div>
  );
}
