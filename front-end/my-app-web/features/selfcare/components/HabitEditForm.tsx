'use client';

import React, { useState } from 'react';
import type { Habit, HabitToolCode, UpdateHabit } from '../api/self-care';
import { HABIT_STATUS_LABELS, TOOL_LABELS, UNIT_LABELS, WEEKDAY_OPTIONS } from '../constants/habit-options';

interface HabitEditFormProps {
  habit: Habit;
  saving: boolean;
  onSave: (input: UpdateHabit) => Promise<unknown>;
  onCancel: () => void;
}

function sameDays(a: number[], b: number[]): boolean {
  return a.length === b.length && a.every((day, i) => day === b[i]);
}

export function HabitEditForm({ habit, saving, onSave, onCancel }: HabitEditFormProps) {
  const [title, setTitle] = useState(habit.title);
  const [targetValue, setTargetValue] = useState<number>(habit.targetValue);
  const [unit, setUnit] = useState(habit.unit);
  const [frequency, setFrequency] = useState<'DAILY' | 'WEEKLY'>(habit.frequencyType === 'WEEKLY' ? 'WEEKLY' : 'DAILY');
  const [days, setDays] = useState<number[]>(habit.daysOfWeek?.length ? habit.daysOfWeek : [1, 3, 5]);
  const [toolCode, setToolCode] = useState<'' | HabitToolCode>(habit.toolCode ?? '');
  const [status, setStatus] = useState(habit.status);
  const [error, setError] = useState<string | null>(null);

  const toggleDay = (day: number) =>
    setDays((prev) => (prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day].sort((a, b) => a - b)));

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const cleanTitle = title.trim();
    if (!cleanTitle || !(targetValue > 0)) {
      setError('Vui lòng nhập tên thói quen và mức mục tiêu lớn hơn 0.');
      return;
    }
    if (frequency === 'WEEKLY' && days.length === 0) {
      setError('Vui lòng chọn ít nhất 1 ngày trong tuần.');
      return;
    }
    const input: UpdateHabit = {};
    if (cleanTitle !== habit.title) input.title = cleanTitle;
    if (targetValue !== habit.targetValue) input.targetValue = targetValue;
    if (unit !== habit.unit) input.unit = unit;
    const scheduleChanged =
      frequency !== habit.frequencyType || (frequency === 'WEEKLY' && !sameDays(days, habit.daysOfWeek ?? []));
    if (scheduleChanged) {
      input.frequencyType = frequency;
      if (frequency === 'WEEKLY') input.daysOfWeek = days;
    }
    if (toolCode !== (habit.toolCode ?? '')) input.toolCode = toolCode;
    if (status !== habit.status) input.status = status;
    if (Object.keys(input).length === 0) {
      onCancel();
      return;
    }
    setError(null);
    try {
      await onSave(input);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chưa thể lưu thói quen. Hãy thử lại.');
    }
  };

  const fieldClass = 'rounded-lg border border-black bg-white px-2.5 py-1.5 text-xs text-black';

  return (
    <form
      onSubmit={(e) => void handleSubmit(e)}
      aria-label={`Sửa thói quen ${habit.title}`}
      className="rounded-xl border border-black bg-paper-warm p-3 flex flex-col gap-2.5 text-xs"
    >
      <span className="font-space text-[11px] font-extrabold uppercase text-black">Sửa thói quen</span>
      <input
        type="text"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        maxLength={160}
        required
        aria-label="Tên thói quen"
        className={fieldClass}
      />
      <div className="grid grid-cols-2 gap-2">
        <input
          type="number"
          min={1}
          max={600}
          value={targetValue}
          onChange={(e) => setTargetValue(Number(e.target.value) || 1)}
          aria-label="Mức mục tiêu thói quen"
          className={fieldClass}
        />
        <select value={unit} onChange={(e) => setUnit(e.target.value)} aria-label="Đơn vị thói quen" className={fieldClass}>
          {Object.entries(UNIT_LABELS).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
          {!(habit.unit in UNIT_LABELS) && <option value={habit.unit}>{habit.unit}</option>}
        </select>
      </div>
      <div className="grid grid-cols-2 gap-2">
        <select
          value={frequency}
          onChange={(e) => setFrequency(e.target.value as 'DAILY' | 'WEEKLY')}
          aria-label="Tần suất thói quen"
          className={fieldClass}
        >
          <option value="DAILY">Hằng ngày</option>
          <option value="WEEKLY">Theo tuần</option>
        </select>
        <select
          value={toolCode}
          onChange={(e) => setToolCode(e.target.value as '' | HabitToolCode)}
          aria-label="Công cụ dịu liên kết"
          className={fieldClass}
        >
          <option value="">Không gắn công cụ</option>
          {(Object.keys(TOOL_LABELS) as HabitToolCode[]).map((code) => (
            <option key={code} value={code}>{TOOL_LABELS[code]}</option>
          ))}
        </select>
      </div>
      {frequency === 'WEEKLY' && (
        <div className="flex flex-wrap items-center gap-1">
          {WEEKDAY_OPTIONS.map((day) => (
            <button
              key={day.value}
              type="button"
              onClick={() => toggleDay(day.value)}
              aria-pressed={days.includes(day.value)}
              className={`px-2 py-0.5 rounded border border-black font-space text-[10px] font-bold cursor-pointer ${
                days.includes(day.value) ? 'bg-primary-container text-black' : 'bg-white text-gray-600'
              }`}
            >
              {day.label}
            </button>
          ))}
        </div>
      )}
      <select value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Trạng thái thói quen" className={fieldClass}>
        {Object.entries(HABIT_STATUS_LABELS).map(([value, label]) => (
          <option key={value} value={value}>{label}</option>
        ))}
      </select>
      <p className="text-[11px] text-gray-600">Các ngày đã đánh dấu trước đây vẫn được giữ nguyên.</p>
      {error && <p role="alert" className="text-[11px] text-red-700 font-bold">{error}</p>}
      <div className="flex justify-end gap-2">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-lg border border-black bg-white px-2.5 py-1 font-space text-[11px] font-bold cursor-pointer"
        >
          Hủy
        </button>
        <button
          type="submit"
          disabled={saving}
          className="rounded-lg border border-black bg-primary-container px-3 py-1 font-space text-[11px] font-extrabold shadow-neo-xs cursor-pointer disabled:opacity-50"
        >
          Lưu thay đổi
        </button>
      </div>
    </form>
  );
}
