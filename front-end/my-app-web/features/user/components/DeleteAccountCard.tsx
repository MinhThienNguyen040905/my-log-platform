'use client';

import { useEffect, useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { UserX, X } from 'lucide-react';
import { AccountApiError, requestAccountDeletion } from '../api/client';
import { clearLocalJournalData } from '../utils/local-data';

const CONFIRM_PHRASE = 'XÓA TÀI KHOẢN';
const GRACE_DAYS = 7;

function errorMessage(error: unknown): string {
  if (error instanceof AccountApiError) {
    if (error.detail === 'PASSWORD_INCORRECT') return 'Mật khẩu không đúng.';
    if (error.detail === 'RECENT_LOGIN_REQUIRED') return 'Vì lý do an toàn, hãy đăng xuất, đăng nhập lại rồi thử trong vòng 10 phút.';
    if (error.status === 404) return 'Chức năng xóa tài khoản hiện chưa được bật.';
    if (error.status === 409) return 'Tài khoản này đã có yêu cầu xóa.';
    return error.message;
  }
  return 'Chưa thể gửi yêu cầu xóa tài khoản. Vui lòng thử lại.';
}

const formatDate = (iso: string) =>
  new Date(iso).toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });

/**
 * UC-23: asks for account deletion. The account is deleted for good after {@link GRACE_DAYS} days; signing in again
 * before then cancels it. Confirmed with a checkbox, a typed phrase and the password (or a recent sign-in for
 * Google accounts). On success the user is signed out on this device too.
 */
export function DeleteAccountCard({ userId, hasPassword }: { userId: string; hasPassword: boolean }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const [open, setOpen] = useState(false);
  const [understood, setUnderstood] = useState(false);
  const [phrase, setPhrase] = useState('');
  const [password, setPassword] = useState('');
  const request = useMutation({
    mutationFn: () => requestAccountDeletion(hasPassword ? password : null),
    onSuccess: async () => {
      clearLocalJournalData(userId);
      // The backend already revoked every session; this clears this browser's cookies.
      try { await fetch('/api/auth/logout', { method: 'POST' }); } catch { /* Cookies expire on their own. */ }
    },
  });

  useEffect(() => {
    const element = dialog.current;
    if (!element) return;
    if (open && !element.open) element.showModal();
    if (!open && element.open) element.close();
  }, [open]);

  const close = () => {
    if (request.isPending || request.isSuccess) return;
    setOpen(false); setUnderstood(false); setPhrase(''); setPassword(''); request.reset();
  };
  const ready = understood && phrase.trim().toUpperCase() === CONFIRM_PHRASE && (!hasPassword || password.length > 0);

  return (
    <section aria-labelledby="delete-account-title" className="bg-white border-2 border-red-300 rounded-2xl p-5 sm:p-6 shadow-neo-sm flex flex-col gap-4">
      <div>
        <h3 id="delete-account-title" className="font-space text-base font-extrabold flex items-center gap-2">
          <UserX className="w-4 h-4 text-red-700" aria-hidden="true" />
          Xóa tài khoản
        </h3>
        <p className="mt-1 text-sm text-gray-700">
          Xóa vĩnh viễn tài khoản và toàn bộ dữ liệu sau {GRACE_DAYS} ngày. Trong thời gian đó, chỉ cần đăng nhập lại là yêu cầu xóa tự hủy.
        </p>
      </div>
      <p className="text-sm text-gray-700">Chỉ muốn xóa nhật ký mà giữ tài khoản? Dùng &ldquo;Bắt đầu lại&rdquo; ở mục phía trên.</p>
      <button type="button" onClick={() => setOpen(true)} data-testid="delete-account-open"
        className="self-start inline-flex items-center gap-2 rounded-xl border-2 border-red-700 bg-white px-4 py-2 font-space text-sm font-extrabold text-red-700 hover:bg-red-50 cursor-pointer">
        <UserX className="w-4 h-4" aria-hidden="true" />
        Xóa tài khoản
      </button>

      <dialog
        ref={dialog}
        aria-labelledby="delete-account-dialog-title"
        onClose={close}
        onCancel={(event) => { if (request.isPending || request.isSuccess) event.preventDefault(); }}
        className="m-auto w-[calc(100%-2rem)] max-w-lg rounded-3xl border-[2.5px] border-black bg-white p-0 shadow-neo-lg backdrop:bg-black/40"
      >
        <form className="flex flex-col gap-4 p-6" onSubmit={(event) => { event.preventDefault(); if (ready) request.mutate(); }}>
          <div className="flex items-start justify-between gap-3">
            <h2 id="delete-account-dialog-title" className="font-space text-lg font-extrabold">Xóa tài khoản MyLog?</h2>
            <button type="button" onClick={close} aria-label="Đóng" disabled={request.isPending || request.isSuccess}
              className="shrink-0 rounded-full border-2 border-black p-1.5 hover:bg-paper-warm cursor-pointer disabled:opacity-40">
              <X className="w-4 h-4" aria-hidden="true" />
            </button>
          </div>

          {request.isSuccess ? <>
            <p role="status" className="rounded-xl border border-green-700 bg-green-50 p-3 text-sm font-bold text-green-800">
              Đã nhận yêu cầu. Tài khoản sẽ bị xóa vào {formatDate(request.data.scheduledFor)}. Bạn đã được đăng xuất;
              đăng nhập lại trước thời điểm đó nếu muốn giữ tài khoản. Chúng tôi cũng đã gửi email xác nhận.
            </p>
            {/* A full load drops every cached query of the signed-out account. */}
            {/* eslint-disable-next-line @next/next/no-location-assign-relative-destination */}
            <button type="button" onClick={() => window.location.assign('/auth/login')}
              className="self-end rounded-xl border-2 border-black bg-white px-4 py-2 font-space text-sm font-bold cursor-pointer hover:bg-paper-warm">
              Về trang đăng nhập
            </button>
          </> : <>
            <p className="text-sm text-gray-700">
              Bạn sẽ bị đăng xuất trên mọi thiết bị. Sau {GRACE_DAYS} ngày, tài khoản, nhật ký, ảnh và mọi dữ liệu liên quan bị xóa vĩnh viễn, không khôi phục được.
              Muốn giữ một bản sao, hãy tạo bản xuất dữ liệu trước.
            </p>
            <label className="flex items-start gap-2 text-sm">
              <input type="checkbox" checked={understood} onChange={(e) => setUnderstood(e.target.checked)} className="mt-0.5 h-4 w-4 accent-red-700" />
              <span>Tôi hiểu sau {GRACE_DAYS} ngày tài khoản sẽ bị xóa vĩnh viễn.</span>
            </label>
            <label className="flex flex-col gap-1 text-sm">
              <span>Gõ <strong className="font-space">{CONFIRM_PHRASE}</strong> để xác nhận</span>
              <input value={phrase} onChange={(e) => setPhrase(e.target.value)} autoComplete="off" aria-label="Cụm từ xác nhận xóa tài khoản"
                className="rounded-xl border-2 border-black bg-paper-warm/30 px-3 py-2 font-space text-sm font-bold focus:bg-white focus:outline-none" />
            </label>
            {hasPassword ? (
              <label className="flex flex-col gap-1 text-sm">
                <span>Mật khẩu hiện tại</span>
                <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password"
                  aria-label="Mật khẩu xác nhận xóa tài khoản"
                  className="rounded-xl border-2 border-black bg-paper-warm/30 px-3 py-2 text-sm focus:bg-white focus:outline-none" />
              </label>
            ) : (
              <p className="text-xs text-gray-600">Tài khoản đăng nhập bằng Google: cần đăng nhập lại trong vòng 10 phút trước khi xóa.</p>
            )}
            {request.isError && <p role="alert" className="text-sm font-bold text-red-700">{errorMessage(request.error)}</p>}
            <div className="flex flex-wrap justify-end gap-2">
              <button type="button" onClick={close} disabled={request.isPending}
                className="rounded-xl border-2 border-black bg-white px-4 py-2 font-space text-sm font-bold cursor-pointer hover:bg-paper-warm disabled:opacity-50">
                Hủy
              </button>
              <button type="submit" disabled={!ready || request.isPending}
                className="rounded-xl border-2 border-black bg-red-700 px-4 py-2 font-space text-sm font-extrabold text-white shadow-neo-xs cursor-pointer disabled:cursor-not-allowed disabled:opacity-40">
                {request.isPending ? 'Đang gửi…' : `Xóa tài khoản sau ${GRACE_DAYS} ngày`}
              </button>
            </div>
          </>}
        </form>
      </dialog>
    </section>
  );
}
