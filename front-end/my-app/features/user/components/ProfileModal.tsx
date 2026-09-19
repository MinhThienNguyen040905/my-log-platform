'use client';

import React, { useState, useEffect } from 'react';
import { User, Mail, Shield, Globe, Clock, Check, X } from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { useJournal } from '@/features/journal';
import { useToast } from '@/lib/toast-context';

export interface ProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function ProfileModal({ isOpen, onClose }: ProfileModalProps) {
  const { userProfile, updateProfile, entries } = useJournal();
  const { showToast } = useToast();

  const [penName, setPenName] = useState(userProfile.penName);
  const [timezone, setTimezone] = useState(userProfile.timezone);
  const [language, setLanguage] = useState<'vi' | 'en'>(userProfile.language || 'vi');

  useEffect(() => {
    if (isOpen) {
      setPenName(userProfile.penName);
      setTimezone(userProfile.timezone);
      setLanguage(userProfile.language || 'vi');
    }
  }, [isOpen, userProfile]);

  if (!isOpen) return null;

  const handleSave = () => {
    if (!penName.trim()) {
      showToast({
        title: 'Bút danh không được để trống',
        type: 'error',
      });
      return;
    }

    updateProfile({
      penName: penName.trim(),
      timezone,
      language,
    });

    showToast({
      title: 'Đã cập nhật hồ sơ cá nhân!',
      message: 'Thông tin bút danh và thiết lập đã được lưu trữ an toàn.',
      type: 'success',
    });

    onClose();
  };

  const hasDraft = typeof window !== 'undefined' && !!localStorage.getItem('mylog_draft_journal');

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200"
      role="dialog"
      aria-modal="true"
    >
      {/* Outer Card with overflow-visible so WashiTape floats proudly without clipping */}
      <div className="relative w-full max-w-lg max-h-[92vh] bg-paper-warm border-[2.5px] border-black rounded-3xl shadow-neo-lg flex flex-col overflow-visible">
        {/* Scrapbook Tape Decoration - Floating unclipped on top edge */}
        <WashiTape color="lavender" rotate={-1.5} className="absolute -top-3.5 left-10 w-36 z-30 pointer-events-none" />

        {/* Close button */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 rounded-xl hover:bg-black/10 transition-colors border border-transparent hover:border-black cursor-pointer z-20"
          aria-label="Đóng modal"
        >
          <X className="w-5 h-5 text-black" />
        </button>

        {/* 1. Fixed Modal Header */}
        <div className="p-6 sm:p-7 pb-4 border-b-2 border-black shrink-0 flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-primary-container border-2 border-black shadow-neo-sm flex items-center justify-center shrink-0">
            <User className="w-6 h-6 text-black" />
          </div>
          <div>
            <h2 className="font-space text-xl font-extrabold text-black">
              Hồ Sơ Cá Nhân & Thiết Lập
            </h2>
            <span className="font-mono text-[10px] text-gray-600 font-bold block uppercase">
              FR-ACCOUNT-02 / User Configuration
            </span>
          </div>
        </div>

        {/* 2. Scrollable Body Container with custom-scrollbar */}
        <div className="p-6 sm:p-7 py-4 overflow-y-auto custom-scrollbar flex-1 flex flex-col gap-4">
          {/* Pen Name */}
          <div className="flex flex-col gap-1.5">
            <label className="font-space text-xs font-bold text-black flex items-center gap-1.5">
              <User className="w-3.5 h-3.5" />
              Bút danh hiển thị (Pen Name):
            </label>
            <input
              type="text"
              value={penName}
              onChange={(e) => setPenName(e.target.value)}
              placeholder="Nhập bút danh của bạn..."
              className="w-full p-2.5 bg-white border-2 border-black rounded-xl font-space text-sm font-bold shadow-neo-sm focus:outline-none focus:bg-amber-50"
            />
          </div>

          {/* Email (Readonly) */}
          <div className="flex flex-col gap-1.5">
            <label className="font-space text-xs font-bold text-gray-700 flex items-center gap-1.5">
              <Mail className="w-3.5 h-3.5" />
              Địa chỉ Email tài khoản:
            </label>
            <div className="p-2.5 bg-surface-container-lowest border border-black/40 rounded-xl font-mono text-xs text-gray-600 flex items-center justify-between">
              <span>{userProfile.email}</span>
              <span className="text-[10px] font-space font-bold uppercase bg-paper-warm px-1.5 py-0.5 rounded border border-black">
                Chỉ đọc
              </span>
            </div>
          </div>

          {/* Plan & Security Card */}
          <div className="p-3.5 bg-surface-card rounded-2xl border-2 border-black shadow-neo-sm flex flex-col gap-2.5">
            <div className="flex items-center justify-between">
              <span className="font-space text-xs font-extrabold uppercase text-black flex items-center gap-1.5">
                <Shield className="w-4 h-4 text-green-700 stroke-[2.3]" />
                Gói Tài Khoản & Bảo Mật:
              </span>
              <span className="px-2.5 py-0.5 bg-primary-container text-black font-space text-[10px] font-extrabold rounded-lg border border-black uppercase">
                {userProfile.plan} TIER
              </span>
            </div>
            <div className="grid grid-cols-2 gap-2 text-[11px] font-space">
              <div className="p-2 bg-white rounded-lg border border-black flex flex-col">
                <span className="text-gray-500 font-bold">Mã hóa dữ liệu:</span>
                <span className="font-extrabold text-black flex items-center gap-1">
                  <Check className="w-3 h-3 text-green-700 stroke-[3]" /> AES-256 Vault
                </span>
              </div>
              <div className="p-2 bg-white rounded-lg border border-black flex flex-col">
                <span className="text-gray-500 font-bold">Lưu trữ thiết bị:</span>
                <span className="font-extrabold text-black">
                  {entries.length} bài viết
                </span>
              </div>
            </div>
            {hasDraft && (
              <span className="text-[10px] font-space font-bold text-amber-800 bg-amber-100 p-1.5 rounded border border-amber-300">
                • Đang có 1 bản nháp tự động lưu trong máy
              </span>
            )}
          </div>

          {/* Timezone */}
          <div className="flex flex-col gap-1.5">
            <label className="font-space text-xs font-bold text-black flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5" />
              Múi giờ (Timezone):
            </label>
            <select
              value={timezone}
              onChange={(e) => setTimezone(e.target.value)}
              className="w-full p-2.5 bg-white border-2 border-black rounded-xl font-space text-xs font-bold shadow-neo-sm focus:outline-none cursor-pointer"
            >
              <option value="Asia/Ho_Chi_Minh">Asia/Ho_Chi_Minh (GMT+7)</option>
              <option value="Asia/Bangkok">Asia/Bangkok (GMT+7)</option>
              <option value="Asia/Tokyo">Asia/Tokyo (GMT+9)</option>
              <option value="UTC">UTC (GMT+0)</option>
            </select>
          </div>

          {/* Language Preference */}
          <div className="flex flex-col gap-1.5">
            <label className="font-space text-xs font-bold text-black flex items-center gap-1.5">
              <Globe className="w-3.5 h-3.5" />
              Ngôn ngữ giao diện (NFR-USABILITY-02):
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setLanguage('vi')}
                className={`py-2 px-3 rounded-xl border-2 border-black font-space text-xs font-extrabold transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                  language === 'vi'
                    ? 'bg-primary-container text-black shadow-neo-sm'
                    : 'bg-white hover:bg-paper-warm text-gray-700'
                }`}
              >
                <span>Tiếng Việt (VI)</span>
                {language === 'vi' && <Check className="w-3.5 h-3.5 stroke-[3]" />}
              </button>
              <button
                type="button"
                onClick={() => setLanguage('en')}
                className={`py-2 px-3 rounded-xl border-2 border-black font-space text-xs font-extrabold transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                  language === 'en'
                    ? 'bg-primary-container text-black shadow-neo-sm'
                    : 'bg-white hover:bg-paper-warm text-gray-700'
                }`}
              >
                <span>English (EN)</span>
                {language === 'en' && <Check className="w-3.5 h-3.5 stroke-[3]" />}
              </button>
            </div>
          </div>
        </div>

        {/* 3. Fixed Modal Footer */}
        <div className="p-5 sm:p-6 pt-3 border-t-2 border-black shrink-0 flex items-center justify-between bg-paper-warm rounded-b-3xl">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 bg-white text-black font-space text-xs font-bold border border-black rounded-xl hover:bg-gray-100 transition-colors cursor-pointer shadow-neo-sm"
          >
            Hủy
          </button>

          <NeoButton
            variant="primary"
            size="sm"
            onClick={handleSave}
            className="font-space font-extrabold text-xs"
            icon={<Check className="w-3.5 h-3.5 stroke-[2.5]" />}
          >
            Lưu thay đổi
          </NeoButton>
        </div>
      </div>
    </div>
  );
}

