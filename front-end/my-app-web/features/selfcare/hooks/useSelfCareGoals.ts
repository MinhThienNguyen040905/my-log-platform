'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useJournal } from '@/features/journal';
import {
  completeHabit,
  createGoal,
  createHabit,
  listGoals,
  undoHabitCompletion,
  updateGoal,
  type CreateGoal,
  type CreateHabit,
  type Goal,
  type Habit,
} from '../api/self-care';

export function getHabitTodayDate(timezone?: string): string {
  try {
    return new Intl.DateTimeFormat('en-CA', {
      timeZone: timezone || 'Asia/Ho_Chi_Minh',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(new Date());
  } catch {
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
}

export function useSelfCareGoals() {
  const { userId } = useJournal();
  const client = useQueryClient();
  const queryKey = ['selfCare', userId, 'goals'] as const;
  const goalsQuery = useQuery({ queryKey, queryFn: listGoals });

  const invalidateAll = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey }),
      client.invalidateQueries({ queryKey: ['insights', userId] }),
      client.invalidateQueries({ queryKey: ['dashboard', userId] }),
    ]);
  };

  const update = useMutation({
    mutationFn: (goal: Goal) =>
      updateGoal(goal.id, goal.version, {
        status: goal.status === 'COMPLETED' ? 'ACTIVE' : 'COMPLETED',
      }),
    onSuccess: invalidateAll,
  });

  const createGoalMutation = useMutation({
    mutationFn: (input: CreateGoal) => createGoal(input),
    onSuccess: invalidateAll,
  });

  const createHabitMutation = useMutation({
    mutationFn: ({ goalId, input }: { goalId: string; input: CreateHabit }) =>
      createHabit(goalId, {
        ...input,
        daysOfWeek: input.frequencyType === 'WEEKLY' ? input.daysOfWeek : undefined,
      }),
    onSuccess: invalidateAll,
  });

  const toggleHabitMutation = useMutation({
    mutationFn: async (habit: Habit) => {
      const today = getHabitTodayDate(habit.timezone);
      const isDoneToday = habit.completedDates?.includes(today) ?? false;
      if (isDoneToday) {
        await undoHabitCompletion(habit.id, today);
      } else {
        await completeHabit(habit.id, today, habit.targetValue > 0 ? habit.targetValue : 1);
      }
    },
    onSuccess: invalidateAll,
  });

  const toggleHabitDateMutation = useMutation({
    mutationFn: async ({ habit, date }: { habit: Habit; date: string }) => {
      const isDone = habit.completedDates?.includes(date) ?? false;
      if (isDone) {
        await undoHabitCompletion(habit.id, date);
      } else {
        await completeHabit(habit.id, date, habit.targetValue > 0 ? habit.targetValue : 1);
      }
    },
    onSuccess: invalidateAll,
  });

  const mutationError =
    update.error ??
    createGoalMutation.error ??
    createHabitMutation.error ??
    toggleHabitMutation.error ??
    toggleHabitDateMutation.error;

  return {
    goals: goalsQuery.data ?? [],
    loading: goalsQuery.isPending,
    mutating:
      update.isPending ||
      createGoalMutation.isPending ||
      createHabitMutation.isPending ||
      toggleHabitMutation.isPending,
    error:
      goalsQuery.error instanceof Error
        ? goalsQuery.error.message
        : mutationError instanceof Error
          ? mutationError.message
          : null,
    toggleGoal: (goal: Goal) => update.mutateAsync(goal),
    createNewGoal: (input: CreateGoal) => createGoalMutation.mutateAsync(input),
    createNewHabit: (goalId: string, input: CreateHabit) =>
      createHabitMutation.mutateAsync({ goalId, input }),
    toggleHabitToday: (habit: Habit) => toggleHabitMutation.mutateAsync(habit),
    toggleHabitOnDate: (habit: Habit, date: string) =>
      toggleHabitDateMutation.mutateAsync({ habit, date }),
    refetch: async () => {
      update.reset();
      createGoalMutation.reset();
      createHabitMutation.reset();
      toggleHabitMutation.reset();
      await goalsQuery.refetch();
    },
  };
}
