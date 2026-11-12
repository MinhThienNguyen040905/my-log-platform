'use client';

import { useEffect, useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { RotateCcw, X } from 'lucide-react';
import { AccountApiError, resetAccountData } from '../api/client';
import { clearLocalJournalData } from '../utils/local-data';

const CONFIRM_PHRASE = 'BẮT ĐẦU LẠI';
const DELETED = [
  'Tất cả bài nhật ký, thẻ và ảnh đính kèm',
  'Check-in hằng ngày và hoạt động',
  'Phân tích cảm xúc, gợi ý, bản nhìn lại tuần/tháng',
  'Mục tiêu và thói quen tự chăm sóc',
  'Bản xuất dữ liệu đã tạo, góp ý đã gửi',
  'Bản nháp đang lưu trên trình duyệt này',
];
const KEPT = ['Tài khoản, email và cách đăng nhập', 'Tên, bút danh, linh vật, ngôn ngữ, múi giờ', 'Các quyền bạn đã cho phép'];

function errorMessage(error: unknown): string {
  if (error instanceof AccountApiError) {
    if (error.detail === 'PASSWORD_INCORRECT') return 'Mật khẩu không đúng.';
    if (error.detail === 'RECENT_LOGIN_REQUIRED') return 'Vì lý do an toàn, hãy đăng xuất, đăng nhập lại rồi thử trong vòng 10 phút.';
    return error.message;
  }
  return 'Chưa thể xóa dữ liệu. Vui lòng thử lại.';
}

/**
 * "Start a new journey": permanently deletes the user's journal data while keeping the account. Confirmed in a
 * dialog with a checkbox, a typed phrase and the password (or a recent sign-in for Google accounts).
 */
export function StartOverCard({ userId, hasPassword }: { userId: string; hasPassword: boolean }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const [open, setOpen] = useState(false);
  const [understood, setUnderstood] = useState(false);
  const [phrase, setPhrase] = useState('');
  const [password, setPassword] = useState('');
  const reset = useMutation({
    mutationFn: () => resetAccountData(hasPassword ? password : null),
    onSuccess: () => {
      clearLocalJournalData(userId);
      // A full load drops every cached list and in-memory journal state at once; router.push would keep them.
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination
      window.setTimeout(() => window.location.assign('/dashboard'), 1500);
    },
  });

  useEffect(() => {
    const element = dialog.current;
    if (!element) return;
    if (open && !element.open) element.showModal();
    if (!open && element.open) element.close();
  }, [open]);

  const close = () => {
    if (reset.isPending || reset.isSuccess) return;
    setOpen(false); setUnderstood(false); setPhrase(''); setPassword(''); reset.reset();
  };
  const ready = understood && phrase.trim().toUpperCase() === CONFIRM_PHRASE && (!hasPassword || password.length > 0);

  return (
    <section aria-labelledby="start-over-title" className="bg-white border-2 border-red-300 rounded-2xl p-5 sm:p-6 shadow-neo-sm flex flex-col gap-4">
      <div>
        <h3 id="start-over-title" className="font-space text-base font-extrabold flex items-center gap-2">
          <RotateCcw className="w-4 h-4 text-red-700" aria-hidden="true" />
          Bắt đầu hành trình mới
        </h3>
        <p className="mt-1 text-sm text-gray-700">Xóa vĩnh viễn toàn bộ dữ liệu nhật ký để bắt đầu lại từ đầu. Tài khoản của bạn vẫn giữ nguyên. Không thể hoàn tác.</p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
        <div className="rounded-xl border border-red-200 bg-red-50 p-3">
          <p className="font-space text-xs font-extrabold uppercase text-red-800">Sẽ bị xóa</p>
          <ul className="mt-1.5 list-disc pl-5 text-gray-800 space-y-0.5">{DELETED.map((item) => <li key={item}>{item}</li>)}</ul>
        </div>
        <div className="rounded-xl border border-black/20 bg-paper-warm/40 p-3">
          <p className="font-space text-xs font-extrabold uppercase text-gray-700">Được giữ lại</p>
          <ul className="mt-1.5 list-disc pl-5 text-gray-800 space-y-0.5">{KEPT.map((item) => <li key={item}>{item}</li>)}</ul>
        </div>
      </div>
      <p className="text-sm text-gray-700">Muốn giữ một bản sao? Hãy tạo bản xuất dữ liệu ở mục phía trên trước khi tiếp tục.</p>
      <button type="button" onClick={() => setOpen(true)} data-testid="start-over-open"
        className="self-start inline-flex items-center gap-2 rounded-xl border-2 border-red-700 bg-white px-4 py-2 font-space text-sm font-extrabold text-red-700 hover:bg-red-50 cursor-pointer">
        <RotateCcw className="w-4 h-4" aria-hidden="true" />
        Bắt đầu lại
      </button>

      <dialog
        ref={dialog}
        aria-labelledby="start-over-dialog-title"
        onClose={close}
        onCancel={(event) => { if (reset.isPending || reset.isSuccess) event.preventDefault(); }}
        className="m-auto w-[calc(100%-2rem)] max-w-lg rounded-3xl border-[2.5px] border-black bg-white p-0 shadow-neo-lg backdrop:bg-black/40"
      >
        <form className="flex flex-col gap-4 p-6" onSubmit={(event) => { event.preventDefault(); if (ready) reset.mutate(); }}>
          <div className="flex items-start justify-between gap-3">
            <h2 id="start-over-dialog-title" className="font-space text-lg font-extrabold">Xóa toàn bộ dữ liệu nhật ký?</h2>
            <button type="button" onClick={close} aria-label="Đóng" disabled={reset.isPending || reset.isSuccess}
              className="shrink-0 rounded-full border-2 border-black p-1.5 hover:bg-paper-warm cursor-pointer disabled:opacity-40">
              <X className="w-4 h-4" aria-hidden="true" />
            </button>
          </div>

          {reset.isSuccess ? (
            <p role="status" className="rounded-xl border border-green-700 bg-green-50 p-3 text-sm font-bold text-green-800">
              Đã xóa xong. Đang đưa bạn về trang chủ để bắt đầu trang đầu tiên…
            </p>
          ) : <>
            <p className="text-sm text-gray-700">Mọi bài viết, check-in, phân tích và bản nhìn lại sẽ bị xóa ngay và không khôi phục được.</p>
            <label className="flex items-start gap-2 text-sm">
              <input type="checkbox" checked={understood} onChange={(e) => setUnderstood(e.target.checked)} className="mt-0.5 h-4 w-4 accent-red-700" />
              <span>Tôi hiểu thao tác này không thể hoàn tác.</span>
            </label>
            <label className="flex flex-col gap-1 text-sm">
              <span>Gõ <strong className="font-space">{CONFIRM_PHRASE}</strong> để xác nhận</span>
              <input value={phrase} onChange={(e) => setPhrase(e.target.value)} autoComplete="off" aria-label="Cụm từ xác nhận"
                className="rounded-xl border-2 border-black bg-paper-warm/30 px-3 py-2 font-space text-sm font-bold focus:bg-white focus:outline-none" />
            </label>
            {hasPassword ? (
              <label className="flex flex-col gap-1 text-sm">
                <span>Mật khẩu hiện tại</span>
                <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password"
                  aria-label="Mật khẩu xác nhận xóa dữ liệu"
                  className="rounded-xl border-2 border-black bg-paper-warm/30 px-3 py-2 text-sm focus:bg-white focus:outline-none" />
              </label>
            ) : (
              <p className="text-xs text-gray-600">Tài khoản đăng nhập bằng Google: cần đăng nhập lại trong vòng 10 phút trước khi xóa.</p>
            )}
            {reset.isError && <p role="alert" className="text-sm font-bold text-red-700">{errorMessage(reset.error)}</p>}
            <div className="flex flex-wrap justify-end gap-2">
              <button type="button" onClick={close} disabled={reset.isPending}
                className="rounded-xl border-2 border-black bg-white px-4 py-2 font-space text-sm font-bold cursor-pointer hover:bg-paper-warm disabled:opacity-50">
                Hủy
              </button>
              <button type="submit" disabled={!ready || reset.isPending}
                className="rounded-xl border-2 border-black bg-red-700 px-4 py-2 font-space text-sm font-extrabold text-white shadow-neo-xs cursor-pointer disabled:cursor-not-allowed disabled:opacity-40">
                {reset.isPending ? 'Đang xóa…' : 'Xóa vĩnh viễn và bắt đầu lại'}
              </button>
            </div>
          </>}
        </form>
      </dialog>
    </section>
  );
}
