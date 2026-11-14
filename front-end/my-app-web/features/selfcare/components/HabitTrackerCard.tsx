'use client';

import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Apple,
  BookOpen,
  Check,
  Droplets,
  Dumbbell,
  Flame,
  Pencil,
  Plus,
  Sparkles,
  Star,
  Trash2,
  Wind,
} from 'lucide-react';
import { NotificationModal } from '@/components/ui/NotificationModal';
import { useJournal } from '@/features/journal';
import { getHabitTodayDate, useSelfCareGoals } from '../hooks/useSelfCareGoals';
import { CreateHabitFromTemplateModal, type HabitTemplatePreset } from './CreateHabitFromTemplateModal';
import { HabitEditForm } from './HabitEditForm';
import { HABIT_STATUS_LABELS } from '../constants/habit-options';
import type { Habit } from '../api/self-care';

interface DefaultHabitRow {
  id: string;
  title: string;
  iconKey: 'exercise' | 'study' | 'water' | 'food' | 'breath' | 'default';
  backendHabit?: Habit;
  /** False when the habit or its goal is not ACTIVE: the backend refuses new completions then. */
  trackable: boolean;
}

const WEEKDAY_SHORT = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'] as const;

/** Goal category used when a starter suggestion is turned into a real habit. */
const STARTER_CATEGORY: Record<DefaultHabitRow['iconKey'], string> = {
  exercise: 'EXERCISE',
  breath: 'MINDFULNESS',
  study: 'CUSTOM',
  water: 'CUSTOM',
  food: 'CUSTOM',
  default: 'CUSTOM',
};

const DEFAULT_HABIT_ROWS: DefaultHabitRow[] = [
  { id: 'default-exercise', title: 'Tập thể dục', iconKey: 'exercise', trackable: true },
  { id: 'default-study', title: 'Học tập / Làm việc hiệu quả', iconKey: 'study', trackable: true },
  { id: 'default-water', title: 'Uống đủ nước', iconKey: 'water', trackable: true },
  { id: 'default-food', title: 'Ăn uống lành mạnh', iconKey: 'food', trackable: true },
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
  const { goals, mutating, toggleHabitOnDate, editHabit, removeHabit, refetch } = useSelfCareGoals();
  const [modalPreset, setModalPreset] = useState<HabitTemplatePreset | null>(null);
  const [editingHabitId, setEditingHabitId] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Habit | null>(null);

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

  // Starter suggestions are frontend-only; hiding one is a per-device preference, not backend data.
  const hiddenStartersKey = `mylog_hidden_habit_starters_${userId}`;
  const [hiddenStarters, setHiddenStarters] = useState<string[]>(() => {
    if (typeof window === 'undefined') return [];
    try {
      const raw = window.localStorage.getItem(hiddenStartersKey);
      const parsed: unknown = raw ? JSON.parse(raw) : [];
      return Array.isArray(parsed) ? parsed.filter((id): id is string => typeof id === 'string') : [];
    } catch {
      return [];
    }
  });
  const saveHiddenStarters = (next: string[]) => {
    setHiddenStarters(next);
    try {
      window.localStorage.setItem(hiddenStartersKey, JSON.stringify(next));
    } catch {
      // Ignore storage errors; the choice just won't persist.
    }
  };

  const [starterSourceId, setStarterSourceId] = useState<string | null>(null);
  const editStarter = (row: DefaultHabitRow) => {
    setStarterSourceId(row.id);
    setModalPreset({
      templateCode: 'CUSTOM_HABIT',
      category: STARTER_CATEGORY[row.iconKey],
      presetTitle: row.title,
      presetTargetValue: 1,
      presetUnit: 'sessions',
      presetToolCode: null,
    });
  };

  // Every real day of the current month, with its weekday; the day area scrolls on its own.
  const dayColumns = useMemo(
    () =>
      Array.from({ length: daysInMonth }, (_, i) => {
        const day = i + 1;
        const weekday = new Date(currentYear, currentMonth - 1, day).getDay(); // 0 = Sunday
        return { day, weekdayLabel: WEEKDAY_SHORT[weekday], weekend: weekday === 0 || weekday === 6 };
      }),
    [currentYear, currentMonth, daysInMonth],
  );

  const scrollAreaRef = useRef<HTMLDivElement>(null);
  const todayHeaderRef = useRef<HTMLTableCellElement>(null);
  const habitColumnRef = useRef<HTMLTableCellElement>(null);
  useEffect(() => {
    // Open with today in view: put it just right of the sticky habit column, a few days of context before it.
    const area = scrollAreaRef.current;
    const todayCell = todayHeaderRef.current;
    if (!area || !todayCell) return;
    const stickyWidth = habitColumnRef.current?.offsetWidth ?? 0;
    area.scrollLeft = Math.max(0, todayCell.offsetLeft - stickyWidth - todayCell.offsetWidth * 3);
  }, []);

  const rows: DefaultHabitRow[] = useMemo(() => {
    const backendRows: DefaultHabitRow[] = goals.flatMap((g) =>
      (g.habits ?? []).map((h) => ({
        id: h.id,
        title: h.title,
        iconKey: resolveIconKey(h),
        backendHabit: h,
        trackable: g.status === 'ACTIVE' && h.status === 'ACTIVE',
      })),
    );

    const existingTitles = new Set(backendRows.map((r) => r.title.trim().toLowerCase()));
    const starters = DEFAULT_HABIT_ROWS.filter(
      (d) => !existingTitles.has(d.title.trim().toLowerCase()) && !hiddenStarters.includes(d.id),
    );

    return [...backendRows, ...starters];
  }, [goals, hiddenStarters]);

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
    if (!row.trackable || day > currentDay) return;
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
          {hiddenStarters.length > 0 && (
            <button
              type="button"
              onClick={() => saveHiddenStarters([])}
              className="font-space text-xs font-bold text-gray-700 underline hover:text-black cursor-pointer"
            >
              Hiện lại gợi ý mẫu
            </button>
          )}
          <button
            type="button"
            onClick={() => {
              setStarterSourceId(null);
              setModalPreset({
                templateCode: 'CUSTOM_HABIT',
                category: 'MINDFULNESS',
                presetTitle: '',
                presetTargetValue: 1,
                presetUnit: 'sessions',
                presetToolCode: null,
              });
            }}
            className="inline-flex items-center gap-1 rounded-xl border-2 border-black bg-primary-container px-3 py-1.5 font-space text-xs font-extrabold text-black shadow-neo-xs hover:bg-lime-200 cursor-pointer"
          >
            <Plus className="w-3.5 h-3.5 stroke-[2.5]" />
            <span>Thêm vào bảng</span>
          </button>
        </div>
      </div>

      {/* Bảng lưới Thói quen theo ngày: cột tên dính bên trái, phần ngày cuộn ngang riêng */}
      {/* The border lives on this wrapper; its bottom padding keeps the scrollbar off the rounded border. */}
      <div className="w-full overflow-hidden rounded-2xl border-2 border-black bg-white shadow-neo-sm pb-3">
      <div
        ref={scrollAreaRef}
        role="region"
        aria-label={`Các ngày trong tháng ${currentMonth}/${currentYear}, cuộn ngang để xem thêm`}
        tabIndex={0}
        className="w-full overflow-x-auto pb-1 [scrollbar-width:thin] [scrollbar-color:rgb(0_0_0/0.45)_transparent] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-black"
      >
        <table className="w-max min-w-full border-separate border-spacing-0 text-xs">
          <thead>
            <tr className="bg-paper-warm">
              <th
                ref={habitColumnRef}
                className="sticky left-0 z-20 bg-paper-warm py-3 px-4 text-left font-space text-sm font-extrabold text-black border-b-2 border-r-2 border-black min-w-[200px] sm:min-w-[260px]"
              >
                <div className="flex items-center gap-2">
                  <Star className="w-4 h-4 fill-black text-black shrink-0" />
                  <span>Thói quen</span>
                </div>
              </th>
              {dayColumns.map(({ day, weekdayLabel, weekend }) => {
                const isToday = day === currentDay;
                return (
                  <th
                    key={day}
                    ref={isToday ? todayHeaderRef : undefined}
                    aria-current={isToday ? 'date' : undefined}
                    className={`py-1.5 px-1 text-center font-space border-b-2 border-r last:border-r-0 border-black border-r-black/40 min-w-[2.5rem] ${
                      isToday ? 'bg-primary-container text-black' : weekend ? 'bg-paper-warm text-gray-700' : 'text-black'
                    }`}
                  >
                    <span className="block text-[10px] font-bold opacity-70">{weekdayLabel}</span>
                    <span className="block text-xs font-extrabold">{day}</span>
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, rowIndex) => {
              const habit = row.backendHabit;
              // The last row closes the grid with a solid line above the scrollbar gap.
              const rowBorder = rowIndex < rows.length - 1 ? 'border-b border-b-black/60' : 'border-b-2 border-b-black';
              if (habit && editingHabitId === habit.id) {
                return (
                  <tr key={row.id}>
                    <td colSpan={dayColumns.length + 1} className={`p-3 ${rowBorder}`}>
                      {/* Keep the form pinned to the visible area while the day grid is scrolled. */}
                      <div className="sticky left-3 max-w-xl">
                      <HabitEditForm
                        habit={habit}
                        saving={mutating}
                        onCancel={() => setEditingHabitId(null)}
                        onSave={async (input) => {
                          await editHabit(habit, input);
                          setEditingHabitId(null);
                        }}
                      />
                      </div>
                    </td>
                  </tr>
                );
              }
              return (
              <tr key={row.id} className={`group transition-colors ${row.trackable ? '' : 'opacity-60'}`}>
                <td className={`sticky left-0 z-10 bg-white group-hover:bg-paper-warm py-3 px-4 border-r-2 border-r-black ${rowBorder} font-sans text-xs sm:text-sm font-semibold text-black`}>
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2.5 min-w-0">
                      <HabitRowIcon iconKey={row.iconKey} />
                      <div className="flex flex-col min-w-0">
                        <span className="truncate">{row.title}</span>
                        {habit?.moodAssociation?.interpretation && (
                          <span className="text-[10px] font-normal text-emerald-900">
                            {habit.moodAssociation.interpretation}
                          </span>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-1 shrink-0">
                      {habit && habit.streak > 0 && (
                        <span
                          title="Chuỗi ngày liên tiếp"
                          className="inline-flex items-center gap-0.5 text-[10px] font-space font-extrabold px-1.5 py-0.5 rounded bg-amber-100 border border-black text-black"
                        >
                          <Flame className="w-3 h-3 text-amber-600" />
                          {habit.streak}
                        </span>
                      )}
                      {habit && !row.trackable && (
                        <span className="text-[10px] font-space font-bold px-1.5 py-0.5 rounded bg-black/10">
                          {habit.status === 'ACTIVE' ? 'Mục tiêu tạm dừng' : HABIT_STATUS_LABELS[habit.status] ?? habit.status}
                        </span>
                      )}
                      {!habit && (
                        <>
                          <button
                            type="button"
                            onClick={() => editStarter(row)}
                            aria-label={`Sửa gợi ý ${row.title}`}
                            title="Sửa và lưu thành thói quen của bạn"
                            className="p-1 rounded-md border border-black/30 bg-white hover:bg-paper-warm cursor-pointer"
                          >
                            <Pencil className="w-3 h-3 text-black" />
                          </button>
                          <button
                            type="button"
                            onClick={() => saveHiddenStarters([...hiddenStarters, row.id])}
                            aria-label={`Ẩn gợi ý ${row.title}`}
                            title="Ẩn gợi ý này"
                            className="p-1 rounded-md border border-black/30 bg-white hover:bg-red-50 cursor-pointer"
                          >
                            <Trash2 className="w-3 h-3 text-red-700" />
                          </button>
                        </>
                      )}
                      {habit && (
                        <>
                          <button
                            type="button"
                            onClick={() => setEditingHabitId(habit.id)}
                            disabled={mutating}
                            aria-label={`Sửa thói quen ${habit.title}`}
                            title="Sửa thói quen"
                            className="p-1 rounded-md border border-black/30 bg-white hover:bg-paper-warm cursor-pointer disabled:opacity-50"
                          >
                            <Pencil className="w-3 h-3 text-black" />
                          </button>
                          <button
                            type="button"
                            onClick={() => setPendingDelete(habit)}
                            disabled={mutating}
                            aria-label={`Xóa thói quen ${habit.title}`}
                            title="Xóa thói quen"
                            className="p-1 rounded-md border border-black/30 bg-white hover:bg-red-50 cursor-pointer disabled:opacity-50"
                          >
                            <Trash2 className="w-3 h-3 text-red-700" />
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                </td>

                {dayColumns.map(({ day, weekend }) => {
                  const checked = isChecked(row, day);
                  const isToday = day === currentDay;
                  const isFuture = day > currentDay;
                  return (
                    <td
                      key={day}
                      className={`py-2.5 px-1 text-center border-r last:border-r-0 border-r-black/40 ${rowBorder} group-hover:bg-paper-warm/40 ${
                        isToday ? 'bg-primary-container/20' : weekend ? 'bg-paper-warm/40' : ''
                      }`}
                    >
                      <button
                        type="button"
                        onClick={() => void handleToggleCell(row, day)}
                        disabled={!row.trackable || isFuture}
                        aria-pressed={checked}
                        aria-label={`${ checked ? 'Bỏ đánh dấu' : 'Đánh dấu' } ${row.title} ngày ${day}`}
                        title={isFuture ? 'Ngày này chưa tới' : undefined}
                        className={`w-5 h-5 rounded-full border-[1.75px] border-black flex items-center justify-center mx-auto transition-all cursor-pointer disabled:cursor-not-allowed ${isFuture ? 'opacity-30' : ''} ${
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
              );
            })}
          </tbody>
        </table>
      </div>
      </div>

      <NotificationModal
        isOpen={!!pendingDelete}
        onClose={() => setPendingDelete(null)}
        type="danger"
        title="Xóa thói quen này?"
        description={`"${pendingDelete?.title ?? ''}" và lịch sử đánh dấu của nó sẽ bị xóa vĩnh viễn.`}
        confirmLabel="Xóa vĩnh viễn"
        cancelLabel="Giữ lại"
        onConfirm={async () => {
          if (!pendingDelete) return;
          try {
            await removeHabit(pendingDelete.id);
          } catch {
            return false;
          }
        }}
      />

      <CreateHabitFromTemplateModal
        isOpen={!!modalPreset}
        preset={modalPreset}
        onClose={() => {
          setModalPreset(null);
          setStarterSourceId(null);
        }}
        onCreated={() => {
          // A starter turned into a real habit is replaced by it, even if the title was changed.
          if (starterSourceId && !hiddenStarters.includes(starterSourceId)) {
            saveHiddenStarters([...hiddenStarters, starterSourceId]);
          }
          setStarterSourceId(null);
          setModalPreset(null);
          void refetch();
        }}
      />
    </section>
  );
}
