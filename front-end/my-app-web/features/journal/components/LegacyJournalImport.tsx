'use client';

import { useEffect, useState } from 'react';
import { generateJSON } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import type { JournalEntry } from '@/types';
import { useJournal } from '../context/JournalContext';
import { journalBody, journalJson, journalRequest, prepareContent, type JournalTag } from '../api/client';

type Source = { key: string; label: string; entries: JournalEntry[] };

export function LegacyJournalImport() {
  const { userId, userProfile, addEntry, reloadEntries } = useJournal();
  const [sources, setSources] = useState<Source[]>([]);
  const [sourceKey, setSourceKey] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    const timer = setTimeout(() => {
      const found: Source[] = [];
      for (const [key, label] of [
        [`mylog_${userId}_entries`, 'Dữ liệu cũ của tài khoản này'],
      ]) {
        try {
          const value = localStorage.getItem(key);
          const entries: unknown = value ? JSON.parse(value) : null;
          if (Array.isArray(entries) && entries.length) found.push({ key, label, entries: entries as JournalEntry[] });
        } catch { /* Ignore invalid local data. */ }
      }
      setSources(found);
      setSourceKey(found[0]?.key ?? '');
    }, 0);
    return () => clearTimeout(timer);
  }, [userId]);

  const selected = sources.find((source) => source.key === sourceKey);
  if (!selected) return null;

  const importEntries = async () => {
    if (!window.confirm(`Nhập ${selected.entries.length} bài từ "${selected.label}" vào tài khoản hiện tại? Dữ liệu cũ sẽ được giữ nguyên.`)) return;
    setBusy(true);
    setMessage('');
    let imported = 0;
    let skipped = 0;
    try {
      for (const [index, entry] of selected.entries.entries()) {
        if (!entry.content || /<\s*(img|mark)\b/i.test(entry.content)) { skipped++; continue; }
        try {
          const contentJson = prepareContent(generateJSON(entry.content, [StarterKit, Underline]));
          const time = entry.time || '12:00';
          const occurredAt = new Date(`${entry.date}T${time.length === 5 ? `${time}:00` : time}`).toISOString();
          const key = `legacy-account-${index}-${String(entry.id || entry.date).replace(/[^A-Za-z0-9._:-]/g, '').slice(0, 110)}`;
          const saved = await addEntry({
            title: entry.title?.slice(0, 160) || 'Trang nhật ký không tên', contentJson, occurredAt,
            timezone: userProfile.timezone, moodCode: entry.emotion === 'very_bad' ? 'very-bad' : entry.emotion || 'neutral',
            moodScore: entry.moodScore, stressScore: entry.stressScore, energyScore: entry.energyScore,
            sleepMinutes: Math.round((entry.sleepHours || 0) * 60),
          }, key);
          const tags = await journalJson<JournalTag[]>('journal-tags');
          for (const name of (entry.tags || []).slice(0, 20)) {
            let tag = tags.find((item) => item.name === name);
            if (!tag) tag = await journalJson<JournalTag>('journal-tags', { method: 'POST', ...journalBody({ name }) });
            await journalRequest(`journal-entries/${saved.id}/tags/${tag.id}`, { method: 'PUT' });
          }
          imported++;
        } catch (error) {
          if (error instanceof Error && /kết nối|Phiên đăng nhập|giới hạn ghi/.test(error.message)) throw error;
          skipped++;
        }
      }
      await reloadEntries();
      setMessage(`Đã nhập ${imported} bài. ${skipped ? `${skipped} bài chưa nhập được; dữ liệu gốc vẫn còn trên trình duyệt.` : 'Dữ liệu gốc vẫn còn trên trình duyệt.'}`);
    } catch (error) {
      setMessage(`Đã nhập ${imported} bài, còn ${selected.entries.length - imported - skipped} bài chưa xử lý. ${error instanceof Error ? error.message : 'Vui lòng thử lại.'} Dữ liệu cũ vẫn còn trên trình duyệt.`);
      await reloadEntries();
    }
    finally { setBusy(false); }
  };

  return <section className="rounded-2xl border border-border-soft bg-surface-card p-4 font-space text-sm">
    <h2 className="font-bold">Nhập nhật ký cũ từ trình duyệt</h2>
    <p className="mt-1 text-gray-600">Xem trước và xác nhận trước khi nhập. Dữ liệu dùng chung có thể thuộc người từng dùng trình duyệt này.</p>
    <select aria-label="Nguồn dữ liệu cũ" value={sourceKey} onChange={(event) => setSourceKey(event.target.value)} className="mt-3 max-w-full rounded-lg border border-border-soft bg-white p-2">
      {sources.map((source) => <option key={source.key} value={source.key}>{source.label} ({source.entries.length} bài)</option>)}
    </select>
    <ul className="mt-2 list-inside list-disc text-gray-700">{selected.entries.slice(0, 3).map((entry, index) => <li key={`${entry.id}-${index}`}>{entry.title || 'Không có tiêu đề'}</li>)}</ul>
    <button type="button" disabled={busy} onClick={() => void importEntries()} className="mt-3 min-h-11 rounded-xl border border-black bg-primary-container px-4 font-bold disabled:opacity-60">{busy ? 'Đang nhập...' : 'Xác nhận nhập'}</button>
    {message && <p className="mt-2" role="status">{message}</p>}
  </section>;
}
