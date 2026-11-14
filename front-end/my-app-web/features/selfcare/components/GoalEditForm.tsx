'use client';

import React, { useState } from 'react';
import type { Goal, UpdateGoal } from '../api/self-care';
import { GOAL_STATUS_LABELS } from '../constants/habit-options';

interface GoalEditFormProps {
  goal: Goal;
  saving: boolean;
  onSave: (input: UpdateGoal) => Promise<unknown>;
  onCancel: () => void;
}

export function GoalEditForm({ goal, saving, onSave, onCancel }: GoalEditFormProps) {
  const [title, setTitle] = useState(goal.title);
  const [description, setDescription] = useState(goal.description ?? '');
  const [status, setStatus] = useState(goal.status);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const cleanTitle = title.trim();
    if (!cleanTitle) {
      setError('Vui lòng nhập tên mục tiêu.');
      return;
    }
    const cleanDescription = description.trim();
    const input: UpdateGoal = {};
    if (cleanTitle !== goal.title) input.title = cleanTitle;
    // Backend treats a missing description as "keep"; it has no way to clear one yet.
    if (cleanDescription && cleanDescription !== (goal.description ?? '')) input.description = cleanDescription;
    if (status !== goal.status) input.status = status;
    if (Object.keys(input).length === 0) {
      onCancel();
      return;
    }
    setError(null);
    try {
      await onSave(input);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chưa thể lưu mục tiêu. Hãy thử lại.');
    }
  };

  return (
    <form
      onSubmit={(e) => void handleSubmit(e)}
      aria-label={`Sửa mục tiêu ${goal.title}`}
      className="rounded-xl border border-black bg-paper-warm p-3 flex flex-col gap-2.5 text-xs"
    >
      <span className="font-space text-[11px] font-extrabold uppercase text-black">Sửa mục tiêu</span>
      <input
        type="text"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        maxLength={160}
        required
        aria-label="Tên mục tiêu"
        className="rounded-lg border border-black bg-white px-2.5 py-1.5 text-xs text-black"
      />
      <input
        type="text"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        maxLength={300}
        placeholder="Mô tả ngắn (tùy chọn)"
        aria-label="Mô tả mục tiêu"
        className="rounded-lg border border-black bg-white px-2.5 py-1.5 text-xs text-black"
      />
      <select
        value={status}
        onChange={(e) => setStatus(e.target.value)}
        aria-label="Trạng thái mục tiêu"
        className="rounded-lg border border-black bg-white px-2.5 py-1.5 text-xs text-black"
      >
        {Object.entries(GOAL_STATUS_LABELS).map(([value, label]) => (
          <option key={value} value={value}>{label}</option>
        ))}
      </select>
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
