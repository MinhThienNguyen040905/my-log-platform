'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Laptop, ShieldCheck } from 'lucide-react';
import { accountRequest } from '../api/client';
import { useAccount } from '../context/AccountContext';

type Consent = { type: string; documentVersion: string; granted: boolean; decidedAt: string };
type Session = { id: string; deviceName: string | null; createdAt: string; lastUsedAt: string | null; expiresAt: string; current?: boolean };

/** Plain-language names for consent codes; optional ones can be switched back on after a withdrawal. */
const CONSENTS: Record<string, { title: string; description: string; optional: boolean }> = {
  AI_PROCESSING: { title: 'Phân tích bài viết', description: 'Cho phép MyLog nhận ra cảm xúc và chủ đề trong bài viết để tạo gợi ý và bản nhìn lại.', optional: true },
  ANALYTICS: { title: 'Thống kê sử dụng', description: 'Gửi số liệu sử dụng ẩn danh để cải thiện ứng dụng. Không gồm nội dung nhật ký.', optional: true },
  MODEL_TRAINING: { title: 'Cải thiện mô hình', description: 'Cho phép dùng dữ liệu đã ẩn danh để huấn luyện mô hình.', optional: true },
  PRIVACY: { title: 'Chính sách quyền riêng tư', description: 'Bạn đã đồng ý khi tạo tài khoản.', optional: false },
  TERMS: { title: 'Điều khoản sử dụng', description: 'Bạn đã đồng ý khi tạo tài khoản.', optional: false },
};
const VISIBLE_DEVICES = 5;

function useAccess() {
  const { userId } = useAccount();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const access = useQuery({ queryKey: ['account', userId, 'access'], queryFn: async () => {
    const [consentResponse, sessionResponse] = await Promise.all([accountRequest('/consents'), accountRequest('/sessions')]);
    return { consents: await consentResponse.json() as Consent[], sessions: await sessionResponse.json() as Session[] };
  } });
  const action = useMutation({ mutationFn: async (perform: () => Promise<unknown>) => perform(), onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['account', userId, 'access'] });
  } });
  const perform = async (request: () => Promise<unknown>) => {
    setError(null);
    try { await action.mutateAsync(request); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể hoàn tất thao tác.'); }
  };
  const loadError = access.error instanceof Error ? access.error.message : access.error ? 'Không thể tải cài đặt.' : null;
  return { access, perform, busy: action.isPending, error: error || loadError };
}

const lastSeen = (session: Session) => session.lastUsedAt ?? session.createdAt;
const formatDay = (iso: string) => new Date(iso).toLocaleDateString('vi-VN', { day: 'numeric', month: 'numeric', year: 'numeric' });

/** Signed-in devices: this device first, the others grouped by name with their most recent use. */
export function SessionsPanel() {
  const { access, perform, busy, error } = useAccess();
  const [showAll, setShowAll] = useState(false);
  const sessions = access.data?.sessions ?? [];
  const current = sessions.find((session) => session.current);
  const groups = new Map<string, Session[]>();
  for (const session of sessions) {
    if (session === current) continue;
    const name = session.deviceName || 'Thiết bị không rõ tên';
    groups.set(name, [...(groups.get(name) ?? []), session]);
  }
  const others = [...groups.entries()]
    .map(([name, items]) => ({ name, items, last: items.map(lastSeen).sort().at(-1) as string }))
    .sort((a, b) => b.last.localeCompare(a.last));
  const shown = showAll ? others : others.slice(0, VISIBLE_DEVICES);
  const revokeAll = (items: Session[]) => perform(() => Promise.all(items.map((session) =>
    accountRequest(`/sessions/${encodeURIComponent(session.id)}`, { method: 'DELETE' }))));

  return <section aria-labelledby="sessions-title" className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm flex flex-col gap-4">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <h3 id="sessions-title" className="font-space text-base font-extrabold">Thiết bị đang đăng nhập</h3>
      <button type="button" disabled={busy || others.length === 0} className="text-sm underline disabled:opacity-50 disabled:no-underline"
        onClick={() => void perform(() => accountRequest('/sessions?exceptCurrent=true', { method: 'DELETE' }))}>
        Đăng xuất các thiết bị khác
      </button>
    </div>
    {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    {access.isPending && <p role="status" className="text-sm">Đang tải...</p>}
    <ul className="flex flex-col divide-y divide-black/10">
      {current && <li className="flex items-center justify-between gap-3 py-2.5 text-sm">
        <span className="flex items-center gap-2 min-w-0">
          <Laptop className="w-4 h-4 shrink-0" aria-hidden="true" />
          <span className="truncate font-bold">{current.deviceName || 'Thiết bị không rõ tên'}</span>
        </span>
        <span className="shrink-0 rounded-md border border-black bg-primary-container px-2 py-0.5 font-space text-xs font-bold">Thiết bị này</span>
      </li>}
      {shown.map((group) => <li key={group.name} className="flex items-center justify-between gap-3 py-2.5 text-sm">
        <span className="flex items-center gap-2 min-w-0">
          <Laptop className="w-4 h-4 shrink-0 text-gray-500" aria-hidden="true" />
          <span className="min-w-0">
            <span className="block truncate">{group.name}{group.items.length > 1 ? ` · ${group.items.length} phiên` : ''}</span>
            <span className="block text-xs text-gray-600">Dùng gần nhất {formatDay(group.last)}</span>
          </span>
        </span>
        <button type="button" disabled={busy} className="shrink-0 underline disabled:opacity-50" onClick={() => void revokeAll(group.items)}>
          Đăng xuất
        </button>
      </li>)}
    </ul>
    {others.length > VISIBLE_DEVICES && (
      <button type="button" className="self-start text-sm font-bold underline" onClick={() => setShowAll((value) => !value)}>
        {showAll ? 'Thu gọn' : `Xem tất cả (${others.length})`}
      </button>
    )}
  </section>;
}

/** Recorded consents in plain words; optional processing can be withdrawn and switched back on. */
export function ConsentsPanel() {
  const { access, perform, busy, error } = useAccess();
  const consents = access.data?.consents ?? [];
  const decide = (consent: Consent, granted: boolean) => perform(() => accountRequest(`/consents/${encodeURIComponent(consent.type)}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ documentVersion: consent.documentVersion, granted }),
  }));

  return <section aria-labelledby="consents-title" className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm flex flex-col gap-4">
    <h3 id="consents-title" className="font-space text-base font-extrabold flex items-center gap-2">
      <ShieldCheck className="w-4 h-4 text-green-700" aria-hidden="true" />
      Quyền bạn đã cho phép
    </h3>
    {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    {access.isPending && <p role="status" className="text-sm">Đang tải...</p>}
    {access.isSuccess && consents.length === 0 && <p className="text-sm">Chưa có lựa chọn nào.</p>}
    <ul className="flex flex-col divide-y divide-black/10">
      {consents.map((consent) => {
        const info = CONSENTS[consent.type] ?? { title: consent.type, description: '', optional: true };
        return <li key={consent.type} className="flex items-start justify-between gap-3 py-3 text-sm">
          <span className="min-w-0">
            <span className="block font-bold">{info.title}</span>
            {info.description && <span className="block text-xs text-gray-600">{info.description}</span>}
          </span>
          <span className="shrink-0 flex flex-col items-end gap-1">
            <span className={`rounded-md border px-2 py-0.5 font-space text-xs font-bold ${consent.granted ? 'border-green-700 text-green-800 bg-green-50' : 'border-gray-400 text-gray-600'}`}>
              {consent.granted ? 'Đang bật' : 'Đang tắt'}
            </span>
            {consent.granted
              ? <button type="button" disabled={busy} className="text-xs underline disabled:opacity-50" onClick={() => void decide(consent, false)}>Thu hồi</button>
              : info.optional && <button type="button" disabled={busy} className="text-xs underline disabled:opacity-50" onClick={() => void decide(consent, true)}>Cho phép</button>}
          </span>
        </li>;
      })}
    </ul>
  </section>;
}
