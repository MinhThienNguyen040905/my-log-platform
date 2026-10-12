'use client';

import { useState } from 'react';
import Link from 'next/link';
import type { DevAccount } from './types';

const pageSize = 50;

export function DevUsersView({ initialAccounts, initialStatus }: { initialAccounts: DevAccount[]; initialStatus: number }) {
  const [accounts, setAccounts] = useState(initialAccounts);
  const [hasMore, setHasMore] = useState(initialAccounts.length === pageSize);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(initialStatus === 200 ? '' : 'Không tải được danh sách tài khoản. Kiểm tra backend local và cấu hình khóa.');
  const [copiedId, setCopiedId] = useState<string | null>(null);

  async function load(before?: string) {
    setBusy(true);
    setError('');
    try {
      const path = before ? `/api/dev/users?before=${encodeURIComponent(before)}` : '/api/dev/users';
      const response = await fetch(path, { cache: 'no-store' });
      if (!response.ok) throw new Error(response.status === 404
        ? 'Không tìm thấy API. Kiểm tra profile local ở backend.'
        : 'Không tải được danh sách tài khoản.');
      const next = await response.json() as DevAccount[];
      setAccounts(current => before ? [...current, ...next] : next);
      setHasMore(next.length === pageSize);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không tải được danh sách tài khoản.');
    } finally {
      setBusy(false);
    }
  }

  return <main className="min-h-screen bg-bg-canvas px-4 py-10 text-on-surface">
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <p className="font-mono text-xs uppercase tracking-widest">Local development</p>
        <h1 className="font-space text-3xl font-extrabold">Tài khoản thử nghiệm</h1>
        <p className="mt-2 text-sm">Chỉ tài khoản đang hoạt động trong môi trường phát triển local.</p>
      </div>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      {initialStatus === 200 && <section className="overflow-hidden rounded-xl border border-black bg-white">
        <div className="border-b border-black p-4 text-sm font-bold">{accounts.length} tài khoản đã tải</div>
        {accounts.length === 0 ? <p className="p-4 text-sm">Chưa có tài khoản đang hoạt động.</p> :
          <ul className="divide-y divide-gray-200">{accounts.map(account => <li key={account.id} className="flex flex-wrap items-center justify-between gap-2 p-4">
            <div className="min-w-0"><p className="break-all font-semibold">{account.email}</p><p className="break-all font-mono text-xs text-gray-500">{account.id}</p></div>
            <div className="flex items-center gap-3">
              <button type="button" onClick={async () => {
                try { await navigator.clipboard.writeText(account.email); setCopiedId(account.id); }
                catch { setError('Không thể sao chép email.'); }
              }} className="text-sm font-bold underline">{copiedId === account.id ? 'Đã sao chép' : 'Sao chép email'}</button>
              <Link href="/auth/login" className="text-sm font-bold underline">Đăng nhập</Link>
            </div>
          </li>)}</ul>}
      </section>}
      {hasMore && <button type="button" disabled={busy} onClick={() => void load(accounts.at(-1)?.id)}
        className="rounded-lg border border-black bg-white px-4 py-2 font-bold disabled:opacity-60">Tải thêm</button>}
    </div>
  </main>;
}
