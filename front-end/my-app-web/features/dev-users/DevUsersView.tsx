'use client';

import { useState, type FormEvent } from 'react';
import Link from 'next/link';

type Account = { id: string; status: string; email: string };
const pageSize = 50;

export function DevUsersView() {
  const [key, setKey] = useState('');
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [hasMore, setHasMore] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [loaded, setLoaded] = useState(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  async function load(before?: string) {
    setBusy(true);
    setError('');
    try {
      const response = await fetch('/api/dev/users', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ key, before }),
        cache: 'no-store',
      });
      if (!response.ok) throw new Error(response.status === 404
        ? 'Không tìm thấy API. Kiểm tra profile local và khóa phát triển ở backend.'
        : 'Không tải được danh sách tài khoản.');
      const next = await response.json() as Account[];
      setAccounts(current => before ? [...current, ...next] : next);
      setHasMore(next.length === pageSize);
      setLoaded(true);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không tải được danh sách tài khoản.');
    } finally {
      setBusy(false);
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void load();
  }

  return <main className="min-h-screen bg-bg-canvas px-4 py-10 text-on-surface">
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <p className="font-mono text-xs uppercase tracking-widest">Local development</p>
        <h1 className="font-space text-3xl font-extrabold">Tài khoản thử nghiệm</h1>
        <p className="mt-2 text-sm">Chỉ tài khoản đang hoạt động. Khóa chỉ giữ trong bộ nhớ trang và mất khi tải lại.</p>
      </div>
      <form onSubmit={submit} className="flex flex-col gap-3 rounded-xl border border-black bg-white p-4 sm:flex-row">
        <label htmlFor="dev-users-key" className="sr-only">Khóa phát triển</label>
        <input id="dev-users-key" type="password" value={key} onChange={event => setKey(event.target.value)}
          placeholder="MYLOG_DEV_USERS_KEY" autoComplete="off" required minLength={16}
          className="min-w-0 flex-1 rounded-lg border border-black px-3 py-2" />
        <button type="submit" disabled={busy} className="rounded-lg border border-black bg-primary-container px-4 py-2 font-bold disabled:opacity-60">
          {busy ? 'Đang tải...' : 'Xem tài khoản'}
        </button>
      </form>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      {loaded && <section className="overflow-hidden rounded-xl border border-black bg-white">
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
