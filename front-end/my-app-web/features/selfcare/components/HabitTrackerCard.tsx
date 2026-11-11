'use client';

import React, { useMemo, useState } from 'react';
import {
  Apple,
  BookOpen,
  Check,
  Droplets,
  Dumbbell,
  Flame,
  Plus,
  Sparkles,
  Star,
  Wind,
} from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { useJournal } from '@/features/journal';
import { getHabitTodayDate, useSelfCareGoals } from '../hooks/useSelfCareGoals';
import { CreateHabitFromTemplateModal, type HabitTemplatePreset } from './CreateHabitFromTemplateModal';
import type { Habit } from '../api/self-care';

interface DefaultHabitRow {
  id: string;
  title: string;
  iconKey: 'exercise' | 'study' | 'water' | 'food' | 'breath' | 'default';
  backendHabit?: Habit;
}

const DEFAULT_HABIT_ROWS: DefaultHabitRow[] = [
  { id: 'default-exercise', title: 'Tập thể dục', iconKey: 'exercise' },
  { id: 'default-study', title: 'Học tập / Làm việc hiệu quả', iconKey: 'study' },
  { id: 'default-water', title: 'Uống đủ nước', iconKey: 'water' },
  { id: 'default-food', title: 'Ăn uống lành mạnh', iconKey: 'food' },
];

function resolveIconKey(habit: Habit): DefaultHabitRow['iconKey'] {
  const lower = habit.title.toLowerCase();
  if (habit.toolCode === 'BREATHING_2MIN' || lower.includes('thở') || lower.includes('thiền')) {
    return 'breath';
  }
  if (lower.includes('thể dục') || lower.includes('đi bộ') || lower.includes('chạy') || lower.includes('vận động')) {
    return 'exercise';
  }
  if (lower.includes('học') || lower.includes('làm việc') || lower.includes('đọc')) {
    return 'study';
  }
  if (lower.includes('nước')) {
    return 'water';
  }
  if (lower.includes('ăn') || lower.includes('dinh dưỡng')) {
    return 'food';
  }
  return 'default';
}

function HabitRowIcon({ iconKey }: { iconKey: DefaultHabitRow['iconKey'] }) {
  switch (iconKey) {
    case 'exercise':
      return <Dumbbell className="w-4 h-4 text-black stroke-[2.2] shrink-0" />;
    case 'study':
      return <BookOpen className="w-4 h-4 text-black stroke-[2.2] shrink-0" />;
    case 'water':
      return <Droplets className="w-4 h-4 text-black stroke-[2.2] shrink-0" />;
    case 'food':
      return <Apple className="w-4 h-4 text-black stroke-[2.2] shrink-0" />;
    case 'breath':
      return <Wind className="w-4 h-4 text-black stroke-[2.2] shrink-0" />;
    default:
      return <Sparkles className="w-4 h-4 text-black stroke-[2.2] shrink-0" />;
  }
}

export function HabitTrackerCard() {
  const { userId } = useJournal();
  const { goals, toggleHabitOnDate, refetch } = useSelfCareGoals();
  const [half, setHalf] = useState<'FIRST' | 'SECOND'>('FIRST');
  const [modalPreset, setModalPreset] = useState<HabitTemplatePreset | null>(null);

  const todayStr = useMemo(() => getHabitTodayDate('Asia/Ho_Chi_Minh'), []);
  const [yearStr, monthStr, dayStr] = todayStr.split('-');
  const currentYear = Number(yearStr) || new Date().getFullYear();
  const currentMonth = Number(monthStr) || new Date().getMonth() + 1;
  const currentDay = Number(dayStr) || new Date().getDate();
  const daysInMonth = new Date(currentYear, currentMonth, 0).getDate();

  const storageKey = `mylog_habit_checks_${userId}_${currentYear}-${String(currentMonth).padStart(2, '0')}`;

  const [localChecks, setLocalChecks] = useState<Record<string, boolean>>(() => {
    if (typeof window === 'undefined') return {};
    try {
      const raw = window.localStorage.getItem(storageKey);
      return raw ? (JSON.parse(raw) as Record<string, boolean>) : {};
    } catch {
      return {};
    }
  });

  const dayColumns = useMemo(() => {
    if (half === 'FIRST') {
      return Array.from({ length: 15 }, (_, i) => i + 1);
    }
    const count = Math.max(1, daysInMonth - 15);
    return Array.from({ length: count }, (_, i) => i + 16);
  }, [half, daysInMonth]);

  const rows: DefaultHabitRow[] = useMemo(() => {
    const backendHabits = goals.flatMap((g) => g.habits ?? []);
    const backendRows: DefaultHabitRow[] = backendHabits.map((h) => ({
      id: h.id,
      title: h.title,
      iconKey: resolveIconKey(h),
      backendHabit: h,
    }));

    const existingTitles = new Set(backendRows.map((r) => r.title.trim().toLowerCase()));
    const starters = DEFAULT_HABIT_ROWS.filter(
      (d) => !existingTitles.has(d.title.trim().toLowerCase()),
    );

    return [...backendRows, ...starters];
  }, [goals]);

  const formatDateForDay = (day: number): string => {
    const mm = String(currentMonth).padStart(2, '0');
    const dd = String(day).padStart(2, '0');
    return `${currentYear}-${mm}-${dd}`;
  };

  const isChecked = (row: DefaultHabitRow, day: number): boolean => {
    const dateIso = formatDateForDay(day);
    const key = `${row.id}:${dateIso}`;
    if (key in localChecks) {
      return localChecks[key];
    }
    if (row.backendHabit?.completedDates?.includes(dateIso)) {
      return true;
    }
    return false;
  };

  const handleToggleCell = async (row: DefaultHabitRow, day: number) => {
    const dateIso = formatDateForDay(day);
    const key = `${row.id}:${dateIso}`;
    const currentVal = isChecked(row, day);
    const nextVal = !currentVal;

    setLocalChecks((prev) => {
      const updated = { ...prev, [key]: nextVal };
      try {
        window.localStorage.setItem(storageKey, JSON.stringify(updated));
      } catch {
        // Ignore storage quota errors
      }
      return updated;
    });

    if (row.backendHabit && dateIso === getHabitTodayDate(row.backendHabit.timezone)) {
      try {
        await toggleHabitOnDate(row.backendHabit, dateIso);
      } catch {
        // Keep optimistic local check state if backend rejects
      }
    }
  };

  return (
    <section
      aria-label="Bảng theo dõi thói quen"
      className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative flex flex-col gap-4"
    >
      <WashiTape color="lime" rotate={-1.5} className="absolute -top-3 left-10 w-28" />

      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b-2 border-black">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-primary-container border-2 border-black shadow-neo-xs flex items-center justify-center">
            <Star className="w-4 h-4 fill-black text-black" />
          </div>
          <div>
            <h2 className="font-space text-lg font-extrabold text-on-surface flex items-center gap-2">
              <span>Bảng Theo Dõi Thói Quen</span>
              <span className="font-mono text-xs px-2 py-0.5 rounded-lg bg-paper-warm border border-black font-bold">
                Tháng {currentMonth}/{currentYear}
              </span>
            </h2>
            <p className="font-sans text-xs text-gray-600">
              Bấm vào vòng tròn từng ngày để đánh dấu thói quen bạn đã hoàn thành
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <div className="inline-flex items-center rounded-xl border-2 border-black bg-white p-0.5 shadow-neo-xs">
            <button
              type="button"
              onClick={() => setHalf('FIRST')}
              className={`px-2.5 py-1 rounded-lg font-space text-xs font-extrabold transition-all cursor-pointer ${
                half === 'FIRST'
                  ? 'bg-primary-container text-black border border-black'
                  : 'text-gray-600 hover:text-black'
              }`}
            >
              Ngày 1–15
            </button>
            <button
              type="button"
              onClick={() => setHalf('SECOND')}
              className={`px-2.5 py-1 rounded-lg font-space text-xs font-extrabold transition-all cursor-pointer ${
                half === 'SECOND'
                  ? 'bg-primary-container text-black border border-black'
                  : 'text-gray-600 hover:text-black'
              }`}
            >
              Ngày 16–{daysInMonth}
            </button>
          </div>

          <button
            type="button"
            onClick={() =>
              setModalPreset({
                templateCode: 'CUSTOM_HABIT',
                category: 'MINDFULNESS',
                presetTitle: '',
                presetTargetValue: 1,
                presetUnit: 'sessions',
                presetToolCode: null,
              })
            }
            className="inline-flex items-center gap-1 rounded-xl border-2 border-black bg-primary-container px-3 py-1.5 font-space text-xs font-extrabold text-black shadow-neo-xs hover:bg-lime-200 cursor-pointer"
          >
            <Plus className="w-3.5 h-3.5 stroke-[2.5]" />
            <span>Thêm vào bảng</span>
          </button>
        </div>
      </div>

      {/* Bảng lưới Thói quen theo ngày (giống mẫu sổ tay) */}
      <div className="w-full overflow-x-auto rounded-2xl border-2 border-black bg-white shadow-neo-sm">
        <table className="w-full border-collapse text-xs min-w-[680px]">
          <thead>
            <tr className="bg-paper-warm border-b-2 border-black">
              <th className="py-3 px-4 text-left font-space text-sm font-extrabold text-black border-r-2 border-black min-w-[220px]">
                <div className="flex items-center gap-2">
                  <Star className="w-4 h-4 fill-black text-black shrink-0" />
                  <span>Thói quen</span>
                </div>
              </th>
              {dayColumns.map((day) => {
                const isToday = day === currentDay;
                return (
                  <th
                    key={day}
                    className={`py-2.5 px-1 text-center font-space text-xs font-extrabold border-r last:border-r-0 border-black/60 w-9 ${
                      isToday ? 'bg-primary-container text-black' : 'text-black'
                    }`}
                  >
                    {day}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody className="divide-y divide-black/60">
            {rows.map((row) => (
              <tr key={row.id} className="hover:bg-paper-warm/30 transition-colors">
                <td className="py-3 px-4 border-r-2 border-black font-sans text-xs sm:text-sm font-semibold text-black">
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2.5">
                      <HabitRowIcon iconKey={row.iconKey} />
                      <span>{row.title}</span>
                    </div>
                    {row.backendHabit && row.backendHabit.streak > 0 && (
                      <span
                        title="Chuỗi ngày liên tiếp"
                        className="inline-flex items-center gap-0.5 text-[10px] font-space font-extrabold px-1.5 py-0.5 rounded bg-amber-100 border border-black text-black shrink-0"
                      >
                        <Flame className="w-3 h-3 text-amber-600" />
                        {row.backendHabit.streak}
                      </span>
                    )}
                  </div>
                </td>

                {dayColumns.map((day) => {
                  const checked = isChecked(row, day);
                  const isToday = day === currentDay;
                  return (
                    <td
                      key={day}
                      className={`py-2.5 px-1 text-center border-r last:border-r-0 border-black/60 ${
                        isToday ? 'bg-primary-container/20' : ''
                      }`}
                    >
                      <button
                        type="button"
                        onClick={() => void handleToggleCell(row, day)}
                        aria-pressed={checked}
                        aria-label={`${ checked ? 'Bỏ đánh dấu' : 'Đánh dấu' } ${row.title} ngày ${day}`}
                        className={`w-5 h-5 rounded-full border-[1.75px] border-black flex items-center justify-center mx-auto transition-all cursor-pointer ${
                          checked
                            ? 'bg-primary-container text-black shadow-neo-xs scale-105'
                            : 'bg-white hover:bg-paper-warm'
                        }`}
                      >
                        {checked && <Check className="w-3 h-3 stroke-[3] text-black" />}
                      </button>
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <CreateHabitFromTemplateModal
        isOpen={!!modalPreset}
        preset={modalPreset}
        onClose={() => setModalPreset(null)}
        onCreated={() => {
          setModalPreset(null);
          void refetch();
        }}
      />
    </section>
  );
}
