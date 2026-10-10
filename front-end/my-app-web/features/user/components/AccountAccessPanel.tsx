'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { accountRequest } from '../api/client';
import { useJournal } from '@/features/journal';

type Consent = { type: string; documentVersion: string; granted: boolean; decidedAt: string };
type Session = { id: string; deviceName: string | null; createdAt: string; lastUsedAt: string | null; expiresAt: string };

export function AccountAccessPanel() {
  const { userId } = useJournal();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const access = useQuery({ queryKey: ['account', userId, 'access'], queryFn: async () => {
    const [consentResponse, sessionResponse] = await Promise.all([
      accountRequest('/consents'), accountRequest('/sessions'),
    ]);
    return { consents: await consentResponse.json() as Consent[], sessions: await sessionResponse.json() as Session[] };
  } });
  const action = useMutation({ mutationFn: async (perform: () => Promise<unknown>) => perform(), onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['account', userId, 'access'] });
  } });
  const consents = access.data?.consents ?? [];
  const sessions = access.data?.sessions ?? [];
  const busy = action.isPending;

  const perform = async (request: () => Promise<unknown>) => {
    setError(null);
    try { await action.mutateAsync(request); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể hoàn tất thao tác.'); }
  };

  return <section className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm space-y-5">
    <h2 className="font-space text-lg font-extrabold">Quyền dữ liệu và phiên đăng nhập</h2>
    {(error || access.error) && <p role="alert" className="text-sm text-red-700">{error || (access.error instanceof Error ? access.error.message : 'Không thể tải cài đặt.')}</p>}
    {access.isPending && <p role="status" className="text-sm">Đang tải quyền dữ liệu và phiên đăng nhập...</p>}
    <div className="space-y-2">
      <h3 className="font-space font-bold">Đồng ý đã ghi nhận</h3>
      {consents.length === 0 && <p className="text-sm">Chưa có lựa chọn nào.</p>}
      {consents.map((consent) => <div key={consent.type} className="flex items-center justify-between gap-3 text-sm">
        <span>{consent.type} · phiên bản {consent.documentVersion} · {consent.granted ? 'Đã đồng ý' : 'Đã từ chối'}</span>
        {consent.granted && <button type="button" disabled={busy} className="underline disabled:opacity-50" onClick={() => void perform(() => accountRequest(`/consents/${encodeURIComponent(consent.type)}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ documentVersion: consent.documentVersion, granted: false }) }))}>Thu hồi</button>}
      </div>)}
    </div>
    <div className="space-y-2">
      <div className="flex items-center justify-between gap-3">
        <h3 className="font-space font-bold">Phiên đăng nhập</h3>
        <button type="button" disabled={busy || sessions.length < 2} className="underline text-sm disabled:opacity-50" onClick={() => void perform(() => accountRequest('/sessions?exceptCurrent=true', { method: 'DELETE' }))}>Đăng xuất thiết bị khác</button>
      </div>
      {sessions.map((session) => <div key={session.id} className="flex items-center justify-between gap-3 text-sm">
        <span>{session.deviceName || 'Thiết bị không rõ tên'} · {new Date(session.createdAt).toLocaleDateString('vi-VN')}</span>
        <button type="button" disabled={busy} className="underline disabled:opacity-50" onClick={() => void perform(() => accountRequest(`/sessions/${encodeURIComponent(session.id)}`, { method: 'DELETE' }))}>Thu hồi</button>
      </div>)}
    </div>
  </section>;
}
