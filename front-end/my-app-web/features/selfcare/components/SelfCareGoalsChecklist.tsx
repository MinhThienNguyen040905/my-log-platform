'use client';

import React, { useState } from 'react';
import { CheckSquare, Lightbulb, Pencil, Plus, Sprout, Trash2, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { NotificationModal } from '@/components/ui/NotificationModal';
import { useSelfCareGoals } from '../hooks/useSelfCareGoals';
import { GoalEditForm } from './GoalEditForm';
import { GOAL_STATUS_LABELS } from '../constants/habit-options';
import type { Goal } from '../api/self-care';

const CATEGORY_OPTIONS = [
  { value: 'MINDFULNESS', label: 'Chánh niệm & Thư giãn' },
  { value: 'SLEEP', label: 'Giấc ngủ' },
  { value: 'EXERCISE', label: 'Vận động nhẹ' },
  { value: 'SOCIAL', label: 'Kết nối' },
  { value: 'CUSTOM', label: 'Tự chăm sóc khác' },
] as const;

/** Goal (task) card: goals only. Habits are created, edited and tracked in HabitTrackerCard. */
export function SelfCareGoalsChecklist() {
  const { t } = useTranslation();
  const { goals, loading, mutating, error, toggleGoal, createNewGoal, editGoal, removeGoal, refetch } =
    useSelfCareGoals();

  const [showGoalForm, setShowGoalForm] = useState(false);
  const [goalCategory, setGoalCategory] = useState<string>('MINDFULNESS');
  const [goalTitle, setGoalTitle] = useState('');
  const [goalDescription, setGoalDescription] = useState('');
  const [goalFormError, setGoalFormError] = useState<string | null>(null);
  const [editingGoalId, setEditingGoalId] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Goal | null>(null);

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

  const pendingHabitCount = pendingDelete?.habits?.length ?? 0;

  return (
    <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative">
      <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b-2 border-black mb-4">
        <div className="flex items-center gap-2">
          <Sprout className="w-5 h-5 text-primary" />
          <h2 className="font-space text-lg font-extrabold text-on-surface">{t('insight.goals')}</h2>
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

      {showGoalForm && (
        <form
          onSubmit={(e) => void handleCreateGoal(e)}
          aria-label="Tạo mục tiêu tự chăm sóc mới"
          className="mb-4 rounded-2xl border-2 border-black bg-paper-warm p-4 shadow-neo-sm flex flex-col gap-3 text-xs"
        >
          <h3 className="font-space text-xs font-extrabold uppercase text-black">Mục tiêu tự chăm sóc mới</h3>

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
        {!loading && !error && goals.length === 0 && <p className="text-sm">{t('status.noGoals')}</p>}

        {goals.map((goal) => {
          const isCompleted = goal.status === 'COMPLETED';
          const habitCount = goal.habits?.length ?? 0;
          if (editingGoalId === goal.id) {
            return (
              <GoalEditForm
                key={goal.id}
                goal={goal}
                saving={mutating}
                onCancel={() => setEditingGoalId(null)}
                onSave={async (input) => {
                  await editGoal(goal, input);
                  setEditingGoalId(null);
                }}
              />
            );
          }
          return (
            <div
              key={goal.id}
              className={`w-full rounded-2xl border-neo-sm shadow-neo-sm p-3.5 flex items-start gap-3 transition-all ${
                isCompleted ? 'bg-paper-warm opacity-85' : 'bg-surface-container-lowest'
              }`}
            >
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
                  <div className="flex items-center gap-1 shrink-0">
                    <span className="text-[10px] font-space px-1.5 py-0.5 rounded bg-black/10 font-bold">
                      {GOAL_STATUS_LABELS[goal.status] ?? goal.status}
                    </span>
                    <button
                      type="button"
                      onClick={() => setEditingGoalId(goal.id)}
                      disabled={mutating}
                      aria-label={`Sửa mục tiêu ${goal.title}`}
                      title="Sửa mục tiêu"
                      className="p-1 rounded-md border border-black/30 bg-white hover:bg-paper-warm cursor-pointer disabled:opacity-50"
                    >
                      <Pencil className="w-3 h-3 text-black" />
                    </button>
                    <button
                      type="button"
                      onClick={() => setPendingDelete(goal)}
                      disabled={mutating}
                      aria-label={`Xóa mục tiêu ${goal.title}`}
                      title="Xóa mục tiêu"
                      className="p-1 rounded-md border border-black/30 bg-white hover:bg-red-50 cursor-pointer disabled:opacity-50"
                    >
                      <Trash2 className="w-3 h-3 text-red-700" />
                    </button>
                  </div>
                </div>
                {goal.description && <p className="font-sans text-xs text-gray-600 mt-1">{goal.description}</p>}
                {habitCount > 0 && (
                  <p className="font-sans text-[11px] text-gray-500 mt-1">
                    {habitCount} thói quen gắn với mục tiêu này (theo dõi ở Bảng thói quen)
                  </p>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-5 p-3.5 bg-paper-warm rounded-2xl border-neo-sm">
        <span className="font-space text-xs font-extrabold uppercase tracking-wider text-black mb-1 flex items-center gap-1.5">
          <Lightbulb className="w-3.5 h-3.5 text-amber-600 fill-amber-300 stroke-[2.3]" />
          {t('insight.microAction')}
        </span>
        <p className="font-sans text-xs text-gray-700 leading-relaxed">{t('insight.microActionDescription')}</p>
      </div>

      <NotificationModal
        isOpen={!!pendingDelete}
        onClose={() => setPendingDelete(null)}
        type="danger"
        title="Xóa mục tiêu này?"
        description={
          pendingHabitCount > 0
            ? `"${pendingDelete?.title ?? ''}" cùng ${pendingHabitCount} thói quen gắn kèm và lịch sử đánh dấu của chúng sẽ bị xóa vĩnh viễn.`
            : `"${pendingDelete?.title ?? ''}" sẽ bị xóa vĩnh viễn.`
        }
        confirmLabel="Xóa vĩnh viễn"
        cancelLabel="Giữ lại"
        onConfirm={async () => {
          if (!pendingDelete) return;
          try {
            await removeGoal(pendingDelete.id);
          } catch {
            return false;
          }
        }}
      />
    </div>
  );
}
