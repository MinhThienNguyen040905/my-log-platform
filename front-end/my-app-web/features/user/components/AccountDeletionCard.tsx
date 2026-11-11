'use client';

import React, { useState } from 'react';
import { AlertTriangle, RotateCcw, Search, Trash2 } from 'lucide-react';
import {
  cancelAccountDeletion,
  getAccountDeletionStatus,
  requestAccountDeletion,
  type DeletionRequest,
} from '../api/account-deletion';

export function AccountDeletionCard({ userEmail }: { userEmail: string }) {
  const [password, setPassword] = useState('');
  const [confirmChecked, setConfirmChecked] = useState(false);
  const [requesting, setRequesting] = useState(false);
  const [requestError, setRequestError] = useState<string | null>(null);
  const [deletionInfo, setDeletionInfo] = useState<DeletionRequest | null>(null);

  const [requestId, setRequestId] = useState('');
  const [reauthEmail, setReauthEmail] = useState(userEmail || '');
  const [reauthPassword, setReauthPassword] = useState('');
  const [managing, setManaging] = useState(false);
  const [manageMessage, setManageMessage] = useState<string | null>(null);
  const [manageError, setManageError] = useState<string | null>(null);

  const handleRequestDeletion = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!password || !confirmChecked) return;
    setRequesting(true);
    setRequestError(null);
    setManageMessage(null);
    try {
      const res = await requestAccountDeletion(password);
      setDeletionInfo(res);
      setRequestId(res.id);
      setReauthEmail(userEmail);
      setPassword('');
    } catch (err) {
      setRequestError(
        err instanceof Error
          ? err.message
          : 'Không thể gửi yêu cầu xóa tài khoản. Kiểm tra lại mật khẩu hiện tại.',
      );
    } finally {
      setRequesting(false);
    }
  };

  const effectiveEmail = (reauthEmail || userEmail || '').trim();

  const handleCheckStatus = async () => {
    const cleanId = requestId.trim();
    if (!cleanId || !effectiveEmail || !reauthPassword) return;
    setManaging(true);
    setManageError(null);
    setManageMessage(null);
    try {
      const status = await getAccountDeletionStatus(cleanId, {
        email: effectiveEmail,
        password: reauthPassword,
      });
      setDeletionInfo(status);
      setManageMessage(`Trạng thái hiện tại: ${status.status}`);
    } catch (err) {
      setManageError(
        err instanceof Error ? err.message : 'Không thể kiểm tra trạng thái yêu cầu xóa.',
      );
    } finally {
      setManaging(false);
    }
  };

  const handleCancelDeletion = async () => {
    const cleanId = requestId.trim();
    if (!cleanId || !effectiveEmail || !reauthPassword) return;
    setManaging(true);
    setManageError(null);
    setManageMessage(null);
    try {
      await cancelAccountDeletion(cleanId, {
        email: effectiveEmail,
        password: reauthPassword,
      });
      setDeletionInfo((prev) => (prev ? { ...prev, status: 'CANCELED' } : null));
      setReauthPassword('');
      setManageMessage('Đã hủy yêu cầu xóa tài khoản thành công.');
    } catch (err) {
      setManageError(
        err instanceof Error ? err.message : 'Không thể hủy yêu cầu xóa tài khoản.',
      );
    } finally {
      setManaging(false);
    }
  };

  return (
    <section
      aria-label="Xóa tài khoản vĩnh viễn"
      className="bg-rose-50/70 border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm flex flex-col gap-5"
    >
      <div className="flex items-start gap-3 border-b border-black/15 pb-3">
        <div className="w-8 h-8 rounded-xl bg-rose-200 border border-black flex items-center justify-center shrink-0 shadow-neo-xs">
          <AlertTriangle className="w-4 h-4 text-rose-950" />
        </div>
        <div>
          <h2 className="font-space text-lg font-extrabold text-rose-950">
            Vùng Nguy Hiểm: Xóa Tài Khoản
          </h2>
          <p className="text-xs text-gray-700 leading-relaxed">
            Khi gửi yêu cầu xóa, tài khoản sẽ được lên lịch xóa vĩnh viễn sau <strong>7 ngày ân hạn</strong> và phiên đăng nhập hiện tại sẽ khép lại. Bạn có thể dùng mã yêu cầu để kiểm tra trạng thái hoặc hủy yêu cầu xóa trong thời gian ân hạn.
          </p>
        </div>
      </div>

      {/* Form Gửi Yêu Cầu Xóa */}
      <form onSubmit={(e) => void handleRequestDeletion(e)} className="flex flex-col gap-3 text-xs">
        <label className="flex items-center gap-2 font-space font-bold text-black cursor-pointer">
          <input
            type="checkbox"
            checked={confirmChecked}
            onChange={(e) => setConfirmChecked(e.target.checked)}
            className="w-4 h-4 accent-black"
          />
          <span>
            Tôi hiểu rằng sau thời gian ân hạn 7 ngày, toàn bộ nhật ký và dữ liệu cá nhân sẽ bị xóa không thể khôi phục.
          </span>
        </label>

        <div className="flex flex-wrap items-end gap-3">
          <label className="flex-1 min-w-[220px] flex flex-col gap-1 font-space font-bold text-black">
            <span>Xác nhận mật khẩu hiện tại để yêu cầu xóa</span>
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Nhập mật khẩu hiện tại..."
              aria-label="Mật khẩu xác nhận xóa tài khoản"
              className="rounded-xl border-2 border-black bg-white px-3 py-2 text-xs text-black"
            />
          </label>

          <button
            type="submit"
            disabled={requesting || !confirmChecked || !password}
            className="inline-flex items-center gap-1.5 rounded-xl border-2 border-black bg-rose-300 hover:bg-rose-400 px-4 py-2 font-space text-xs font-extrabold text-black shadow-neo-xs cursor-pointer disabled:opacity-50"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>{requesting ? 'Đang gửi yêu cầu...' : 'Yêu cầu xóa tài khoản'}</span>
          </button>
        </div>

        {requestError && (
          <p role="alert" className="text-xs font-bold text-red-700">
            {requestError}
          </p>
        )}
      </form>

      {deletionInfo && (
        <div
          role="status"
          className="rounded-xl border-2 border-black bg-white p-4 text-xs flex flex-col gap-1.5 shadow-neo-xs"
        >
          <div className="flex flex-wrap items-center justify-between gap-2">
            <span className="font-space font-extrabold text-black">
              Mã yêu cầu xóa: <code className="font-mono">{deletionInfo.id}</code>
            </span>
            <span className="px-2 py-0.5 rounded border border-black bg-amber-100 font-space font-bold text-black">
              {deletionInfo.status}
            </span>
          </div>
          {deletionInfo.scheduledFor && (
            <p className="text-gray-700">
              Dự kiến xóa vĩnh viễn vào:{' '}
              <strong>{new Date(deletionInfo.scheduledFor).toLocaleString('vi-VN')}</strong>
            </p>
          )}
        </div>
      )}

      {/* Tra cứu trạng thái hoặc Hủy yêu cầu xóa */}
      <div className="border-t border-black/15 pt-4 flex flex-col gap-3 text-xs">
        <h3 className="font-space text-xs font-extrabold uppercase text-black">
          Kiểm tra trạng thái hoặc Hủy yêu cầu xóa đang chờ
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Mã yêu cầu xóa (UUID)</span>
            <input
              type="text"
              value={requestId}
              onChange={(e) => setRequestId(e.target.value)}
              placeholder="UUID yêu cầu xóa..."
              aria-label="Mã UUID yêu cầu xóa"
              className="rounded-xl border border-black bg-white px-3 py-2 text-xs font-mono text-black"
            />
          </label>

          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Email tài khoản</span>
            <input
              type="email"
              value={reauthEmail || userEmail || ''}
              onChange={(e) => setReauthEmail(e.target.value)}
              placeholder="Email đăng nhập..."
              aria-label="Email xác thực yêu cầu xóa"
              className="rounded-xl border border-black bg-white px-3 py-2 text-xs text-black"
            />
          </label>

          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Mật khẩu xác thực</span>
            <input
              type="password"
              value={reauthPassword}
              onChange={(e) => setReauthPassword(e.target.value)}
              placeholder="Mật khẩu..."
              aria-label="Mật khẩu quản lý yêu cầu xóa"
              className="rounded-xl border border-black bg-white px-3 py-2 text-xs text-black"
            />
          </label>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            disabled={managing || !requestId.trim() || !effectiveEmail || !reauthPassword}
            onClick={() => void handleCheckStatus()}
            className="inline-flex items-center gap-1 rounded-xl border border-black bg-white px-3.5 py-1.5 font-space text-xs font-bold text-black hover:bg-paper-warm cursor-pointer disabled:opacity-50"
          >
            <Search className="w-3.5 h-3.5" />
            <span>Kiểm tra trạng thái</span>
          </button>

          <button
            type="button"
            disabled={managing || !requestId.trim() || !effectiveEmail || !reauthPassword}
            onClick={() => void handleCancelDeletion()}
            className="inline-flex items-center gap-1 rounded-xl border-2 border-black bg-primary-container px-3.5 py-1.5 font-space text-xs font-extrabold text-black shadow-neo-xs hover:bg-lime-200 cursor-pointer disabled:opacity-50"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Hủy yêu cầu xóa</span>
          </button>
        </div>

        {manageError && (
          <p role="alert" className="text-xs font-bold text-red-700">
            {manageError}
          </p>
        )}
        {manageMessage && (
          <p role="status" className="text-xs font-bold text-emerald-800">
            {manageMessage}
          </p>
        )}
      </div>
    </section>
  );
}
