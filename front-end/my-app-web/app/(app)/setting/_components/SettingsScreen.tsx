'use client';

import { useQuery } from '@tanstack/react-query';
import { getDashboard } from '@/features/dashboard';
import { SettingsView, useAccount } from '@/features/user';

export function SettingsScreen() {
  const { userId } = useAccount();
  const dashboard = useQuery({ queryKey: ['dashboard', userId, '90d'], queryFn: () => getDashboard('90d') });
  return <>
    {dashboard.isError && <p role="alert" className="mx-auto max-w-4xl px-4 text-sm text-red-700">Không thể tải số bài viết. <button type="button" className="underline" onClick={() => void dashboard.refetch()}>Thử lại</button></p>}
    <SettingsView entryCount={dashboard.data?.journalEntryCount ?? null} draftStorageKey={`mylog_${userId}_draft_journal`} />
  </>;
}
