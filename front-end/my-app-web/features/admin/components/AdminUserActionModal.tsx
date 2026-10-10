'use client';

import React, { useState } from 'react';
import { Ban, CheckCircle2, AlertTriangle, X } from 'lucide-react';
import { NeoButton } from '@/components/ui/NeoButton';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';

interface AdminUserActionModalProps {
  isOpen: boolean;
  actionType: 'SUSPEND' | 'RESTORE';
  userId: string;
  onClose: () => void;
  onConfirm: (reasonCode: string) => Promise<void>;
}

const SUSPEND_REASONS = [
  { code: 'SECURITY_VIOLATION', label: 'Vi phạm chính sách an ninh / Đăng nhập bất thường' },
  { code: 'SUSPICIOUS_LOGIN_BURST', label: 'Tần suất đăng nhập quá giới hạn (Rate-limit burst)' },
  { code: 'USER_ACCOUNT_LOCKED_REQUEST', label: 'Yêu cầu tạm khóa tài khoản từ người dùng' },
  { code: 'MALICIOUS_INPUT_PATTERN', label: 'Phát hiện hành vi gửi payload bất thường' },
];

const RESTORE_REASONS = [
  { code: 'APPEAL_APPROVED', label: 'Đã xác minh và chấp thuận khiếu nại của người dùng' },
  { code: 'VERIFICATION_COMPLETED', label: 'Xác thực bảo mật tài khoản thành công' },
  { code: 'MISTAKEN_SUSPENSION_RESOLVED', label: 'Khôi phục do đình chỉ nhầm lẫn' },
];

export function AdminUserActionModal({
  isOpen,
  actionType,
  userId,
  onClose,
  onConfirm,
}: AdminUserActionModalProps) {
  const [selectedReason, setSelectedReason] = useState<string>(
    actionType === 'SUSPEND' ? SUSPEND_REASONS[0].code : RESTORE_REASONS[0].code
  );
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const isSuspend = actionType === 'SUSPEND';
  const reasonList = isSuspend ? SUSPEND_REASONS : RESTORE_REASONS;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await onConfirm(selectedReason);
      onClose();
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="w-full max-w-lg bg-paper-warm border-[2.5px] border-black rounded-3xl p-6 sm:p-8 shadow-neo-lg relative flex flex-col gap-5 animate-in zoom-in-95 duration-200">
        
        {/* Washi Tape Header Decoration */}
        <WashiTape
          color={isSuspend ? 'peach' : 'lime'}
          rotate={isSuspend ? -2 : 1.5}
          className="absolute -top-3.5 left-10 w-32"
        />

        {/* Close Button */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 rounded-xl border border-black bg-white hover:bg-gray-100 transition-colors cursor-pointer shadow-neo-xs"
          title="Đóng hộp thoại"
        >
          <X className="w-4 h-4 text-black stroke-[2.5]" />
        </button>

        {/* Modal Header */}
        <div className="flex items-start gap-3 mt-1">
          <div
            className={`w-12 h-12 rounded-2xl border-2 border-black flex items-center justify-center shrink-0 shadow-neo-sm ${
              isSuspend ? 'bg-rose-200' : 'bg-primary-container'
            }`}
          >
            {isSuspend ? (
              <Ban className="w-6 h-6 text-rose-700 stroke-[2.3]" />
            ) : (
              <CheckCircle2 className="w-6 h-6 text-black stroke-[2.3]" />
            )}
          </div>
          <div>
            <h2 className="font-space text-lg sm:text-xl font-extrabold text-black">
              {isSuspend ? 'Đình chỉ tài khoản người dùng' : 'Khôi phục quyền truy cập'}
            </h2>
            <p className="font-mono text-xs text-gray-700 mt-0.5 break-all">
              UUID: {userId}
            </p>
          </div>
        </div>

        {/* Warning Note */}
        <div
          className={`p-3.5 rounded-2xl border border-black flex items-start gap-2.5 text-xs font-space ${
            isSuspend ? 'bg-rose-50 text-rose-900' : 'bg-white text-gray-800'
          }`}
        >
          <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
          <p className="leading-relaxed">
            {isSuspend
              ? 'Tài khoản sau khi đình chỉ sẽ bị thu hồi toàn bộ token đăng nhập và phiên làm việc, không thể thực hiện hành động ghi chép hay đồng bộ AI.'
              : 'Sau khi khôi phục, người dùng có thể đăng nhập bình thường và truy cập lại dữ liệu nhật ký mã hóa của họ.'}
          </p>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="font-space text-xs font-extrabold uppercase text-gray-800">
              Chọn lý do thực hiện (reasonCode bắt buộc):
            </label>
            <div className="flex flex-col gap-2">
              {reasonList.map(item => (
                <label
                  key={item.code}
                  className={`flex items-start gap-2.5 p-3 rounded-xl border cursor-pointer transition-all ${
                    selectedReason === item.code
                      ? 'bg-white border-2 border-black shadow-neo-xs font-bold'
                      : 'bg-white/60 border-black/30 hover:border-black hover:bg-white'
                  }`}
                >
                  <input
                    type="radio"
                    name="reasonCode"
                    value={item.code}
                    checked={selectedReason === item.code}
                    onChange={() => setSelectedReason(item.code)}
                    className="mt-0.5 accent-black cursor-pointer"
                  />
                  <div className="flex flex-col">
                    <span className="font-space text-xs text-black">{item.label}</span>
                    <span className="font-mono text-[10px] text-gray-500 font-bold">{item.code}</span>
                  </div>
                </label>
              ))}
            </div>
          </div>

          {/* Modal Actions */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2 bg-white text-black font-space text-xs font-bold border border-black rounded-xl hover:bg-gray-100 transition-all cursor-pointer shadow-neo-xs disabled:opacity-50"
            >
              Hủy bỏ
            </button>
            <NeoButton
              type="submit"
              variant={isSuspend ? 'danger' : 'primary'}
              size="sm"
              disabled={loading}
              className="font-space font-extrabold text-xs shadow-neo-sm"
            >
              {loading
                ? 'Đang xử lý...'
                : isSuspend
                ? 'Xác nhận đình chỉ'
                : 'Xác nhận khôi phục'}
            </NeoButton>
          </div>
        </form>
      </div>
    </div>
  );
}

