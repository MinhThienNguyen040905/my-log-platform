'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useJournal } from '@/features/journal';
import { listGoals, updateGoal, type Goal } from '../api/self-care';

export function useSelfCareGoals() {
  const { userId } = useJournal();
  const client = useQueryClient();
  const queryKey = ['selfCare', userId, 'goals'] as const;
  const goalsQuery = useQuery({ queryKey, queryFn: listGoals });
  const update = useMutation({
    mutationFn: (goal: Goal) => updateGoal(goal.id, goal.version, {
      status: goal.status === 'COMPLETED' ? 'ACTIVE' : 'COMPLETED',
    }),
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey }),
        client.invalidateQueries({ queryKey: ['insights', userId] }),
        client.invalidateQueries({ queryKey: ['dashboard', userId] }),
      ]);
    },
  });

  return {
    goals: goalsQuery.data ?? [],
    loading: goalsQuery.isPending,
    error: goalsQuery.error instanceof Error ? goalsQuery.error.message : update.error instanceof Error ? update.error.message : null,
    toggleGoal: (goal: Goal) => update.mutateAsync(goal),
    refetch: async () => { update.reset(); await goalsQuery.refetch(); },
  };
}
