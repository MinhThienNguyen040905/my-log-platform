'use client';

import React, { useEffect, useState } from 'react';
import { X, Sparkles } from 'lucide-react';
import { createGoal, createHabit, listGoals, type Goal, type Habit, type HabitToolCode } from '../api/self-care';

export interface HabitTemplatePreset {
  templateCode: string | null;
  category: string | null;
  presetTitle: string | null;
  presetTargetValue: number | null;
  presetUnit: string | null;
  presetToolCode: HabitToolCode | null;
}

interface CreateHabitFromTemplateModalProps {
  isOpen: boolean;
  preset: HabitTemplatePreset | null;
  onClose: () => void;
  onCreated: (habit: Habit) => void;
}

const CATEGORY_LABELS: Record<string, string> = {
  MINDFULNESS: 'Chánh niệm & Thư giãn',
  SLEEP: 'Giấc ngủ',
  EXERCISE: 'Vận động nhẹ',
  SOCIAL: 'Kết nối',
  CUSTOM: 'Tự chăm sóc khác',
};

const TOOL_OPTIONS: Array<{ value: '' | HabitToolCode; label: string }> = [
  { value: '', label: 'Không gắn công cụ' },
  { value: 'BREATHING_2MIN', label: 'Thở chậm 2 phút' },
  { value: 'AMBIENT', label: 'Nghe âm thanh dịu' },
  { value: 'VENT_PROMPT', label: 'Viết xả ra trang' },
];

export function CreateHabitFromTemplateModal({
  isOpen,
  preset,
  onClose,
  onCreated,
}: CreateHabitFromTemplateModalProps) {
  if (!isOpen || !preset) return null;
  const modalKey = `${preset.templateCode ?? 'custom'}:${preset.presetTitle ?? ''}:${preset.presetToolCode ?? ''}`;
  return (
    <CreateHabitFromTemplateDialog
      key={modalKey}
      preset={preset}
      onClose={onClose}
      onCreated={onCreated}
    />
  );
}

function CreateHabitFromTemplateDialog({
  preset,
  onClose,
  onCreated,
}: {
  preset: HabitTemplatePreset;
  onClose: () => void;
  onCreated: (habit: Habit) => void;
}) {
  const initialCategory = preset.category ?? 'MINDFULNESS';
  const [goals, setGoals] = useState<Goal[]>([]);
  const [selectedGoalId, setSelectedGoalId] = useState<string>('NEW');
  const [newGoalCategory] = useState<string>(initialCategory);
  const [newGoalTitle, setNewGoalTitle] = useState<string>(
    CATEGORY_LABELS[initialCategory] ?? 'Chăm sóc nhịp sống mỗi ngày',
  );
  const [title, setTitle] = useState<string>(preset.presetTitle ?? 'Thói quen tự chăm sóc');
  const [targetValue, setTargetValue] = useState<number>(preset.presetTargetValue ?? 1);
  const [unit, setUnit] = useState<string>(preset.presetUnit ?? 'minutes');
  const [frequencyType] = useState<'DAILY' | 'WEEKLY'>('DAILY');
  const [toolCode, setToolCode] = useState<'' | HabitToolCode>(preset.presetToolCode ?? '');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (process.env.NODE_ENV !== 'production' && process.env.NEXT_PUBLIC_COMPANION_MOCK === '1') {
      return;
    }

    let cancelled = false;
    void listGoals()
      .then((loaded) => {
        if (cancelled) return;
        const active = loaded.filter((g) => g.status === 'ACTIVE');
        setGoals(active);
        const matching = active.find((g) => g.category === initialCategory) ?? active[0];
        setSelectedGoalId(matching ? matching.id : 'NEW');
      })
      .catch(() => {
        if (!cancelled) {
          setGoals([]);
          setSelectedGoalId('NEW');
        }
      });
    return () => {
      cancelled = true;
    };
  }, [initialCategory]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const cleanTitle = title.trim();
    if (!cleanTitle || targetValue <= 0) {
      setErrorMessage('Vui lòng nhập tên thói quen và mục tiêu lớn hơn 0.');
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      if (process.env.NODE_ENV !== 'production' && process.env.NEXT_PUBLIC_COMPANION_MOCK === '1') {
        onCreated({
          id: 'mock-habit-id',
          goalId: 'mock-goal-id',
          title: cleanTitle,
          targetValue,
          unit,
          frequencyType,
          daysOfWeek: [1, 2, 3, 4, 5, 6, 7],
          timezone: 'Asia/Ho_Chi_Minh',
          toolCode: toolCode || null,
          status: 'ACTIVE',
          version: 0,
          streak: 0,
          completionCount: 0,
          completedDates: [],
          moodAssociation: null,
        });
        onClose();
        return;
      }

      let goalId = selectedGoalId;
      if (goalId === 'NEW') {
        const createdGoal = await createGoal({
          category: newGoalCategory,
          title: newGoalTitle.trim() || 'Chăm sóc nhịp sống mỗi ngày',
        });
        goalId = createdGoal.id;
      }
      const createdHabit = await createHabit(goalId, {
        title: cleanTitle,
        targetValue,
        unit,
        frequencyType,
        daysOfWeek: frequencyType === 'WEEKLY' ? [1, 3, 5] : undefined,
        toolCode: toolCode || null,
      });
      onCreated(createdHabit);
      onClose();
    } catch {
      setErrorMessage('Chưa thể lưu thói quen lúc này. Bạn kiểm tra lại thông tin và thử lại nhé.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 animate-in fade-in"
      role="dialog"
      aria-modal="true"
      aria-label="Tạo thói quen tự chăm sóc"
    >
      <div className="w-full max-w-md rounded-2xl border-2 border-black bg-paper-warm p-5 shadow-neo-lg">
        <div className="flex items-center justify-between border-b border-black/15 pb-3">
          <div className="flex items-center gap-2">
            <span className="inline-flex h-8 w-8 items-center justify-center rounded-xl border border-black bg-primary-container shadow-neo-xs">
              <Sparkles className="h-4 w-4 text-black" />
            </span>
            <div>
              <h2 className="font-space text-sm font-extrabold text-black">Tạo thói quen từ gợi ý</h2>
              <p className="text-[11px] text-gray-600">Bạn có thể chỉnh lại trước khi lưu</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng cửa sổ tạo thói quen"
            className="rounded-lg p-1 text-gray-700 hover:bg-white"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <form onSubmit={(e) => void handleSubmit(e)} className="mt-4 flex flex-col gap-3 text-xs">
          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Tên thói quen</span>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={160}
              required
              aria-label="Tên thói quen"
              className="rounded-xl border border-black bg-white px-3 py-2 text-xs font-normal text-black focus:outline-2 focus:outline-black"
            />
          </label>

          <div className="grid grid-cols-2 gap-2.5">
            <label className="flex flex-col gap-1 font-space font-bold text-black">
              <span>Mức mục tiêu / ngày</span>
              <input
                type="number"
                min={1}
                max={600}
                step="1"
                value={targetValue}
                onChange={(e) => setTargetValue(Number(e.target.value) || 1)}
                required
                aria-label="Mức mục tiêu mỗi ngày"
                className="rounded-xl border border-black bg-white px-3 py-2 text-xs font-normal text-black focus:outline-2 focus:outline-black"
              />
            </label>
            <label className="flex flex-col gap-1 font-space font-bold text-black">
              <span>Đơn vị</span>
              <select
                value={unit}
                onChange={(e) => setUnit(e.target.value)}
                aria-label="Đơn vị thói quen"
                className="rounded-xl border border-black bg-white px-3 py-2 text-xs font-normal text-black focus:outline-2 focus:outline-black"
              >
                <option value="minutes">Phút (minutes)</option>
                <option value="sessions">Lần (sessions)</option>
                <option value="steps">Bước (steps)</option>
              </select>
            </label>
          </div>

          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Gắn với công cụ dịu trên trang viết</span>
            <select
              value={toolCode}
              onChange={(e) => setToolCode(e.target.value as '' | HabitToolCode)}
              aria-label="Gắn với công cụ dịu"
              className="rounded-xl border border-black bg-white px-3 py-2 text-xs font-normal text-black focus:outline-2 focus:outline-black"
            >
              {TOOL_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Thuộc mục tiêu tự chăm sóc</span>
            <select
              value={selectedGoalId}
              onChange={(e) => setSelectedGoalId(e.target.value)}
              aria-label="Chọn mục tiêu tự chăm sóc"
              className="rounded-xl border border-black bg-white px-3 py-2 text-xs font-normal text-black focus:outline-2 focus:outline-black"
            >
              {goals.map((g) => (
                <option key={g.id} value={g.id}>
                  {g.title} ({CATEGORY_LABELS[g.category] ?? g.category})
                </option>
              ))}
              <option value="NEW">+ Tạo mục tiêu mới ({CATEGORY_LABELS[newGoalCategory] ?? newGoalCategory})</option>
            </select>
          </label>

          {selectedGoalId === 'NEW' && (
            <label className="flex flex-col gap-1 font-space font-bold text-black">
              <span>Tên mục tiêu mới</span>
              <input
                type="text"
                value={newGoalTitle}
                onChange={(e) => setNewGoalTitle(e.target.value)}
                maxLength={160}
                required
                aria-label="Tên mục tiêu mới"
                className="rounded-xl border border-black bg-white px-3 py-2 text-xs font-normal text-black focus:outline-2 focus:outline-black"
              />
            </label>
          )}

          {errorMessage && (
            <p role="alert" className="rounded-xl border border-red-500 bg-red-50 px-3 py-2 text-[11px] font-medium text-red-700">
              {errorMessage}
            </p>
          )}

          <div className="mt-2 flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              aria-label="Hủy tạo thói quen"
              className="rounded-xl border border-black bg-white px-3 py-1.5 font-space text-xs font-bold hover:bg-gray-100"
            >
              Để sau
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              aria-label="Lưu thói quen"
              className="rounded-xl border border-black bg-primary-container px-3.5 py-1.5 font-space text-xs font-extrabold shadow-neo-xs hover:bg-lime-200 disabled:opacity-50"
            >
              {isSubmitting ? 'Đang lưu...' : 'Lưu thói quen'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
