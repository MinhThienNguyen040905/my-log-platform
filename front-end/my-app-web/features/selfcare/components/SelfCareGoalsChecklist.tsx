'use client';

import React, { useState } from 'react';
import {
  CheckSquare,
  Flame,
  Lightbulb,
  Plus,
  Sprout,
  Wind,
  X,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { getHabitTodayDate, useSelfCareGoals } from '../hooks/useSelfCareGoals';
import { CreateHabitFromTemplateModal, type HabitTemplatePreset } from './CreateHabitFromTemplateModal';
import type { Habit, HabitToolCode } from '../api/self-care';

const CATEGORY_OPTIONS = [
  { value: 'MINDFULNESS', label: 'Chánh niệm & Thư giãn' },
  { value: 'SLEEP', label: 'Giấc ngủ' },
  { value: 'EXERCISE', label: 'Vận động nhẹ' },
  { value: 'SOCIAL', label: 'Kết nối' },
  { value: 'CUSTOM', label: 'Tự chăm sóc khác' },
] as const;

const TOOL_LABELS: Record<HabitToolCode, string> = {
  BREATHING_2MIN: 'Thở chậm 2 phút',
  AMBIENT: 'Âm thanh dịu',
  VENT_PROMPT: 'Viết xả ra trang',
};

const UNIT_LABELS: Record<string, string> = {
  minutes: 'phút',
  sessions: 'lần',
  steps: 'bước',
};

const WEEKDAY_OPTIONS = [
  { value: 1, label: 'T2' },
  { value: 2, label: 'T3' },
  { value: 3, label: 'T4' },
  { value: 4, label: 'T5' },
  { value: 5, label: 'T6' },
  { value: 6, label: 'T7' },
  { value: 7, label: 'CN' },
];

export function SelfCareGoalsChecklist() {
  const { t } = useTranslation();
  const {
    goals,
    loading,
    mutating,
    error,
    toggleGoal,
    createNewGoal,
    createNewHabit,
    toggleHabitToday,
    refetch,
  } = useSelfCareGoals();

  const [showGoalForm, setShowGoalForm] = useState(false);
  const [goalCategory, setGoalCategory] = useState<string>('MINDFULNESS');
  const [goalTitle, setGoalTitle] = useState('');
  const [goalDescription, setGoalDescription] = useState('');
  const [goalFormError, setGoalFormError] = useState<string | null>(null);

  const [addingHabitForGoalId, setAddingHabitForGoalId] = useState<string | null>(null);
  const [habitTitle, setHabitTitle] = useState('');
  const [habitTargetValue, setHabitTargetValue] = useState<number>(5);
  const [habitUnit, setHabitUnit] = useState<string>('minutes');
  const [habitFrequency, setHabitFrequency] = useState<'DAILY' | 'WEEKLY'>('DAILY');
  const [habitDaysOfWeek, setHabitDaysOfWeek] = useState<number[]>([1, 3, 5]);
  const [habitToolCode, setHabitToolCode] = useState<'' | HabitToolCode>('');
  const [habitFormError, setHabitFormError] = useState<string | null>(null);
  const [quickHabitPreset, setQuickHabitPreset] = useState<HabitTemplatePreset | null>(null);

  const handleCreateGoal = async (event: React.FormEvent) => {
    event.preventDefault();
    const cleanTitle = goalTitle.trim();
    if (!cleanTitle) {
      setGoalFormError('Vui lòng nhập tên mục tiêu.');
      return;
    }
    setGoalFormError(null);
    try {
      await createNewGoal({
        category: goalCategory,
        title: cleanTitle,
        description: goalDescription.trim() || null,
      });
      setGoalTitle('');
      setGoalDescription('');
      setShowGoalForm(false);
    } catch (err) {
      setGoalFormError(
        err instanceof Error ? err.message : 'Chưa thể tạo mục tiêu. Hãy kiểm tra lại nội dung.',
      );
    }
  };

  const handleOpenHabitForm = (goalId: string) => {
    setAddingHabitForGoalId(goalId);
    setHabitTitle('');
    setHabitTargetValue(5);
    setHabitUnit('minutes');
    setHabitFrequency('DAILY');
    setHabitDaysOfWeek([1, 3, 5]);
    setHabitToolCode('');
    setHabitFormError(null);
  };

  const toggleWeekday = (day: number) => {
    setHabitDaysOfWeek((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day].sort((a, b) => a - b),
    );
  };

  const handleCreateHabit = async (event: React.FormEvent, goalId: string) => {
    event.preventDefault();
    const cleanTitle = habitTitle.trim();
    if (!cleanTitle || habitTargetValue <= 0) {
      setHabitFormError('Vui lòng nhập tên thói quen và mức mục tiêu lớn hơn 0.');
      return;
    }
    if (habitFrequency === 'WEEKLY' && habitDaysOfWeek.length === 0) {
      setHabitFormError('Vui lòng chọn ít nhất 1 ngày trong tuần.');
      return;
    }
    setHabitFormError(null);
    try {
      await createNewHabit(goalId, {
        title: cleanTitle,
        targetValue: habitTargetValue,
        unit: habitUnit,
        frequencyType: habitFrequency,
        daysOfWeek: habitFrequency === 'WEEKLY' ? habitDaysOfWeek : undefined,
        toolCode: habitToolCode || null,
      });
      setAddingHabitForGoalId(null);
    } catch (err) {
      setHabitFormError(
        err instanceof Error ? err.message : 'Chưa thể tạo thói quen lúc này.',
      );
    }
  };

  return (
    <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative">

      <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b-2 border-black mb-4">
        <div className="flex items-center gap-2">
          <Sprout className="w-5 h-5 text-primary" />
          <h2 className="font-space text-lg font-extrabold text-on-surface">
            {t('insight.goals')}
          </h2>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <span className="font-space text-xs font-bold text-gray-500">
            {t('insight.achieved', {
              done: goals.filter((g) => g.status === 'COMPLETED').length,
              total: goals.length,
            })}
          </span>
          <button
            type="button"
            onClick={() =>
              setQuickHabitPreset({
                templateCode: 'CUSTOM_HABIT',
                category: 'MINDFULNESS',
                presetTitle: 'Thở chậm 2 phút mỗi tối',
                presetTargetValue: 2,
                presetUnit: 'minutes',
                presetToolCode: 'BREATHING_2MIN',
              })
            }
            className="inline-flex items-center gap-1 rounded-xl border-2 border-black bg-white px-2.5 py-1 font-space text-xs font-extrabold text-black shadow-neo-xs hover:bg-paper-warm cursor-pointer"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Tạo thói quen</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setShowGoalForm((v) => !v);
              setGoalFormError(null);
            }}
            className="inline-flex items-center gap-1 rounded-xl border-2 border-black bg-primary-container px-2.5 py-1 font-space text-xs font-extrabold text-black shadow-neo-xs hover:bg-lime-200 cursor-pointer"
          >
            {showGoalForm ? <X className="w-3.5 h-3.5" /> : <Plus className="w-3.5 h-3.5" />}
            <span>{showGoalForm ? 'Đóng' : 'Thêm mục tiêu'}</span>
          </button>
        </div>
      </div>

      {/* Form Tạo Mục Tiêu Mới */}
      {showGoalForm && (
        <form
          onSubmit={(e) => void handleCreateGoal(e)}
          aria-label="Tạo mục tiêu tự chăm sóc mới"
          className="mb-4 rounded-2xl border-2 border-black bg-paper-warm p-4 shadow-neo-sm flex flex-col gap-3 text-xs"
        >
          <h3 className="font-space text-xs font-extrabold uppercase text-black">
            Mục tiêu tự chăm sóc mới
          </h3>

          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Nhóm mục tiêu</span>
            <select
              value={goalCategory}
              onChange={(e) => setGoalCategory(e.target.value)}
              aria-label="Nhóm mục tiêu"
              className="rounded-xl border border-black bg-white px-3 py-2 text-xs font-normal text-black"
            >
              {CATEGORY_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Tên mục tiêu</span>
            <input
              type="text"
              value={goalTitle}
              onChange={(e) => setGoalTitle(e.target.value)}
              maxLength={160}
              required
              placeholder="Ví dụ: Ngủ đủ giấc và thư giãn cuối ngày"
              aria-label="Tên mục tiêu"
              className="rounded-xl border border-black bg-white px-3 py-2 text-xs font-normal text-black"
            />
          </label>

          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Mô tả ngắn (tùy chọn)</span>
            <input
              type="text"
              value={goalDescription}
              onChange={(e) => setGoalDescription(e.target.value)}
              maxLength={300}
              placeholder="Ghi chú nhẹ nhàng cho bản thân..."
              aria-label="Mô tả mục tiêu"
              className="rounded-xl border border-black bg-white px-3 py-2 text-xs font-normal text-black"
            />
          </label>

          {goalFormError && (
            <p role="alert" className="text-xs text-red-700 font-bold">
              {goalFormError}
            </p>
          )}

          <div className="flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowGoalForm(false)}
              className="rounded-xl border border-black bg-white px-3 py-1.5 font-space text-xs font-bold cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={mutating}
              className="rounded-xl border-2 border-black bg-primary-container px-3.5 py-1.5 font-space text-xs font-extrabold shadow-neo-xs cursor-pointer disabled:opacity-50"
            >
              Lưu mục tiêu
            </button>
          </div>
        </form>
      )}

      {/* Danh sách mục tiêu & checklist thói quen */}
      <div className="flex flex-col gap-3">
        {loading && <p role="status" className="text-sm">{t('status.loadingGoals')}</p>}
        {error && (
          <p role="alert" className="text-sm text-red-700">
            {t('status.goalsError')}{' '}
            <button type="button" className="underline" onClick={() => void refetch()}>
              {t('common.retry')}
            </button>
          </p>
        )}
        {!loading && !error && goals.length === 0 && (
          <p className="text-sm">{t('status.noGoals')}</p>
        )}

        {goals.map((goal) => {
          const isCompleted = goal.status === 'COMPLETED';
          const habits = goal.habits ?? [];
          return (
            <div
              key={goal.id}
              className={`w-full rounded-2xl border-neo-sm shadow-neo-sm p-3.5 flex flex-col gap-3 transition-all ${
                isCompleted ? 'bg-paper-warm opacity-85' : 'bg-surface-container-lowest'
              }`}
            >
              {/* Hàng tiêu đề mục tiêu */}
              <div className="flex items-start gap-3">
                <button
                  type="button"
                  onClick={() => void toggleGoal(goal)}
                  disabled={mutating}
                  aria-label={`Đánh dấu mục tiêu ${goal.title}`}
                  className={`w-5 h-5 rounded border-2 border-black flex items-center justify-center shrink-0 mt-0.5 cursor-pointer ${
                    isCompleted ? 'bg-primary-container' : 'bg-white'
                  }`}
                >
                  {isCompleted && <CheckSquare className="w-3.5 h-3.5 text-black" />}
                </button>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <h4
                      className={`font-space text-xs sm:text-sm font-bold text-black ${
                        isCompleted ? 'line-through opacity-70' : ''
                      }`}
                    >
                      {goal.title}
                    </h4>
                    <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-black/10 font-bold shrink-0">
                      {goal.status}
                    </span>
                  </div>
                  {goal.description && (
                    <p className="font-sans text-xs text-gray-600 mt-1">{goal.description}</p>
                  )}
                </div>
              </div>

              {/* Checklist Thói quen thuộc mục tiêu */}
              {habits.length > 0 && (
                <ul className="flex flex-col gap-2 pl-2 border-l-2 border-black/20" aria-label={`Thói quen của ${goal.title}`}>
                  {habits.map((habit: Habit) => {
                    const today = getHabitTodayDate(habit.timezone);
                    const doneToday = habit.completedDates?.includes(today) ?? false;
                    return (
                      <li
                        key={habit.id}
                        className="rounded-xl border border-black bg-white p-2.5 flex flex-col gap-1.5 shadow-neo-xs"
                      >
                        <div className="flex items-center justify-between gap-2">
                          <button
                            type="button"
                            disabled={mutating || isCompleted}
                            onClick={() => void toggleHabitToday(habit)}
                            aria-pressed={doneToday}
                            aria-label={
                              doneToday
                                ? `Bỏ đánh dấu hoàn thành hôm nay cho ${habit.title}`
                                : `Đánh dấu hoàn thành hôm nay cho ${habit.title}`
                            }
                            className="flex items-center gap-2 text-left cursor-pointer disabled:opacity-50"
                          >
                            <span
                              className={`w-4 h-4 rounded border border-black flex items-center justify-center shrink-0 ${
                                doneToday ? 'bg-primary-container' : 'bg-paper-warm'
                              }`}
                            >
                              {doneToday && <CheckSquare className="w-3 h-3 text-black" />}
                            </span>
                            <span
                              className={`font-space text-xs font-bold text-black ${
                                doneToday ? 'line-through opacity-75' : ''
                              }`}
                            >
                              {habit.title}
                            </span>
                          </button>

                          <div className="flex items-center gap-1.5 shrink-0">
                            <span className="text-[10px] font-mono px-1.5 py-0.5 rounded border border-black/20 bg-paper-warm">
                              {habit.targetValue} {UNIT_LABELS[habit.unit] ?? habit.unit}
                            </span>
                            {habit.streak > 0 && (
                              <span
                                title="Chuỗi ngày liên tiếp"
                                className="inline-flex items-center gap-0.5 text-[10px] font-space font-extrabold px-1.5 py-0.5 rounded bg-amber-100 border border-black text-black"
                              >
                                <Flame className="w-3 h-3 text-amber-600" />
                                {habit.streak}
                              </span>
                            )}
                          </div>
                        </div>

                        <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] text-gray-600">
                          <span>
                            {doneToday ? 'Đã làm hôm nay (bấm để hoàn tác)' : 'Chưa ghi nhận hôm nay'} · Tổng: {habit.completionCount} lần
                          </span>
                          {habit.toolCode && (
                            <span className="inline-flex items-center gap-1 rounded-md border border-black/30 bg-paper-warm px-1.5 py-0.5 text-[10px] font-space font-bold text-black">
                              <Wind className="w-2.5 h-2.5" />
                              {TOOL_LABELS[habit.toolCode] ?? habit.toolCode}
                            </span>
                          )}
                        </div>

                        {habit.moodAssociation?.interpretation && (
                          <p className="text-[11px] text-emerald-900 bg-emerald-50 border border-emerald-300 rounded-lg px-2 py-1">
                            {habit.moodAssociation.interpretation}
                          </p>
                        )}
                      </li>
                    );
                  })}
                </ul>
              )}

              {/* Nút & Form Thêm Thói Quen vào Mục Tiêu ACTIVE */}
              {!isCompleted && (
                <div>
                  {addingHabitForGoalId === goal.id ? (
                    <form
                      onSubmit={(e) => void handleCreateHabit(e, goal.id)}
                      aria-label={`Thêm thói quen cho ${goal.title}`}
                      className="rounded-xl border border-black bg-paper-warm p-3 flex flex-col gap-2.5 text-xs"
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-space text-[11px] font-extrabold uppercase text-black">
                          Thêm thói quen mới
                        </span>
                        <button
                          type="button"
                          onClick={() => setAddingHabitForGoalId(null)}
                          className="text-gray-600 hover:text-black"
                          aria-label="Đóng biểu mẫu thêm thói quen"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      <input
                        type="text"
                        value={habitTitle}
                        onChange={(e) => setHabitTitle(e.target.value)}
                        maxLength={160}
                        required
                        placeholder="Tên thói quen (VD: Thở chậm 2 phút tối)"
                        aria-label="Tên thói quen mới"
                        className="rounded-lg border border-black bg-white px-2.5 py-1.5 text-xs text-black"
                      />

                      <div className="grid grid-cols-2 gap-2">
                        <input
                          type="number"
                          min={1}
                          max={600}
                          value={habitTargetValue}
                          onChange={(e) => setHabitTargetValue(Number(e.target.value) || 1)}
                          aria-label="Mức mục tiêu thói quen"
                          className="rounded-lg border border-black bg-white px-2.5 py-1.5 text-xs text-black"
                        />
                        <select
                          value={habitUnit}
                          onChange={(e) => setHabitUnit(e.target.value)}
                          aria-label="Đơn vị thói quen"
                          className="rounded-lg border border-black bg-white px-2.5 py-1.5 text-xs text-black"
                        >
                          <option value="minutes">Phút (minutes)</option>
                          <option value="sessions">Lần (sessions)</option>
                          <option value="steps">Bước (steps)</option>
                        </select>
                      </div>

                      <div className="grid grid-cols-2 gap-2">
                        <select
                          value={habitFrequency}
                          onChange={(e) => setHabitFrequency(e.target.value as 'DAILY' | 'WEEKLY')}
                          aria-label="Tần suất thói quen"
                          className="rounded-lg border border-black bg-white px-2.5 py-1.5 text-xs text-black"
                        >
                          <option value="DAILY">Hằng ngày (DAILY)</option>
                          <option value="WEEKLY">Theo tuần (WEEKLY)</option>
                        </select>

                        <select
                          value={habitToolCode}
                          onChange={(e) => setHabitToolCode(e.target.value as '' | HabitToolCode)}
                          aria-label="Công cụ dịu liên kết"
                          className="rounded-lg border border-black bg-white px-2.5 py-1.5 text-xs text-black"
                        >
                          <option value="">Không gắn công cụ</option>
                          <option value="BREATHING_2MIN">Thở chậm 2 phút</option>
                          <option value="AMBIENT">Âm thanh dịu</option>
                          <option value="VENT_PROMPT">Viết xả ra trang</option>
                        </select>
                      </div>

                      {habitFrequency === 'WEEKLY' && (
                        <div className="flex flex-wrap items-center gap-1">
                          {WEEKDAY_OPTIONS.map((day) => {
                            const active = habitDaysOfWeek.includes(day.value);
                            return (
                              <button
                                key={day.value}
                                type="button"
                                onClick={() => toggleWeekday(day.value)}
                                className={`px-2 py-0.5 rounded border border-black font-space text-[10px] font-bold cursor-pointer ${
                                  active ? 'bg-primary-container text-black' : 'bg-white text-gray-600'
                                }`}
                              >
                                {day.label}
                              </button>
                            );
                          })}
                        </div>
                      )}

                      {habitFormError && (
                        <p role="alert" className="text-[11px] text-red-700 font-bold">
                          {habitFormError}
                        </p>
                      )}

                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => setAddingHabitForGoalId(null)}
                          className="rounded-lg border border-black bg-white px-2.5 py-1 font-space text-[11px] font-bold"
                        >
                          Hủy
                        </button>
                        <button
                          type="submit"
                          disabled={mutating}
                          className="rounded-lg border border-black bg-primary-container px-3 py-1 font-space text-[11px] font-extrabold shadow-neo-xs disabled:opacity-50"
                        >
                          Lưu thói quen
                        </button>
                      </div>
                    </form>
                  ) : (
                    <button
                      type="button"
                      onClick={() => handleOpenHabitForm(goal.id)}
                      className="inline-flex items-center gap-1 text-xs font-space font-bold text-gray-700 hover:text-black underline cursor-pointer"
                    >
                      <Plus className="w-3.5 h-3.5" />
                      <span>Thêm thói quen</span>
                    </button>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>

      <div className="mt-5 p-3.5 bg-paper-warm rounded-2xl border-neo-sm">
        <span className="font-space text-xs font-extrabold uppercase tracking-wider text-black mb-1 flex items-center gap-1.5">
          <Lightbulb className="w-3.5 h-3.5 text-amber-600 fill-amber-300 stroke-[2.3]" />
          {t('insight.microAction')}
        </span>
        <p className="font-sans text-xs text-gray-700 leading-relaxed">
          {t('insight.microActionDescription')}
        </p>
      </div>

      <CreateHabitFromTemplateModal
        isOpen={!!quickHabitPreset}
        preset={quickHabitPreset}
        onClose={() => setQuickHabitPreset(null)}
        onCreated={() => {
          setQuickHabitPreset(null);
          void refetch();
        }}
      />
    </div>
  );
}
