'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  User,
  Mail,
  Shield,
  Globe,
  Clock,
  Check,
  Key,
  Eye,
  EyeOff,
  ArrowLeft,
  Sparkles,
  Lock,
  BookOpen,
  FileText,
  AlertCircle,
} from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { useSettings, AVATAR_OPTIONS } from '../hooks/useSettings';

export function SettingsView() {
  const {
    userProfile,
    entries,
    name,
    setName,
    penName,
    setPenName,
    bio,
    setBio,
    avatarUrl,
    setAvatarUrl,
    selectedAvatarId,
    setSelectedAvatarId,
    timezone,
    setTimezone,
    language,
    setLanguage,
    currentPassword,
    setCurrentPassword,
    newPassword,
    setNewPassword,
    confirmPassword,
    setConfirmPassword,
    showCurrentPassword,
    setShowCurrentPassword,
    showNewPassword,
    setShowNewPassword,
    showConfirmPassword,
    setShowConfirmPassword,
    passwordLoading,
    passwordStrength,
    hasDraft,
    handleSaveProfile,
    handleChangePassword,
  } = useSettings();

  return (
    <div className="w-full min-h-screen bg-bg-canvas pb-20 selection:bg-brand-lime selection:text-black">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 pt-6 flex flex-col gap-6">
        {/* Top Back Navigation Bar */}
        <div className="flex items-center justify-between">
          <Link
            href="/dashboard"
            className="inline-flex items-center gap-2 text-xs font-space font-bold text-gray-600 hover:text-black hover:underline"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Quay lại Bảng điều khiển (Dashboard)</span>
          </Link>

          <span className="font-mono text-[11px] font-bold text-gray-500 uppercase px-2.5 py-1 bg-white border border-black/40 rounded-full shadow-neo-xs">
            FR-ACCOUNT-02 & 03 / Settings Ledger
          </span>
        </div>

        {/* Main Settings Scrapbook Ledger Card */}
        <div className="relative w-full bg-paper-warm border-[2.5px] border-black rounded-3xl p-6 sm:p-10 shadow-neo-lg flex flex-col gap-8 overflow-visible">
          {/* Top Scrapbook Washi Tape */}
          <WashiTape color="lavender" rotate={-1.5} className="absolute -top-3.5 left-10 w-36 z-20 pointer-events-none" />

          {/* Page Header */}
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b-2 border-black/80 pb-6">
            <div className="flex items-center gap-4">
              <div className="w-14 h-14 rounded-2xl bg-primary-container border-2 border-black shadow-neo flex items-center justify-center shrink-0">
                <User className="w-7 h-7 text-black stroke-[2.3]" />
              </div>
              <div>
                <h1 className="font-space text-2xl sm:text-3xl font-extrabold text-black">
                  Hồ Sơ & Thiết Lập Sổ Tay
                </h1>
                <p className="font-sans text-xs sm:text-sm text-gray-600 mt-0.5">
                  Tùy chỉnh bút danh, bảo mật chìa khóa và quản lý không gian nhật ký của bạn.
                </p>
              </div>
            </div>

            <div className="px-3 py-1 bg-white border-2 border-black rounded-xl font-space text-xs font-extrabold shadow-neo-xs flex items-center gap-1.5 self-start sm:self-auto">
              <span className="w-2 h-2 rounded-full bg-brand-lime border border-black animate-pulse" />
              <span>{userProfile.plan} TIER</span>
            </div>
          </div>

          {/* SECTION 1: PERSONAL INFORMATION */}
          <section className="flex flex-col gap-5">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-white border border-black flex items-center justify-center font-bold shadow-neo-xs">
                <BookOpen className="w-4 h-4 text-black" />
              </div>
              <h2 className="font-space text-lg font-extrabold text-black uppercase tracking-wider">
                1. Thông Tin Tác Giả Sổ Tay
              </h2>
            </div>

            <div className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm flex flex-col gap-5">
              {/* Avatar Selector */}
              <div className="flex flex-col gap-2">
                <label className="font-space text-xs font-extrabold text-black uppercase flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                  Ảnh đại diện / Linh vật sổ tay:
                </label>
                <div className="flex items-center gap-3 flex-wrap">
                  {/* Current Avatar Preview */}
                  <div className="relative w-14 h-14 rounded-2xl border-2 border-black overflow-hidden bg-paper-warm shadow-neo shrink-0">
                    <Image
                      src={avatarUrl}
                      alt="Avatar tác giả"
                      width={56}
                      height={56}
                      className="w-full h-full object-cover"
                    />
                  </div>

                  {/* Avatar Options */}
                  <div className="flex items-center gap-2 flex-wrap">
                    {AVATAR_OPTIONS.map((opt) => {
                      const isSelected = selectedAvatarId === opt.id;
                      return (
                        <button
                          key={opt.id}
                          type="button"
                          onClick={() => {
                            setSelectedAvatarId(opt.id);
                            setAvatarUrl(opt.url);
                          }}
                          className={`px-3 py-1.5 rounded-xl border-2 border-black font-space text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
                            isSelected
                              ? 'bg-primary-container text-black shadow-neo-xs -translate-y-0.5'
                              : 'bg-paper-warm/50 hover:bg-paper-warm text-gray-700'
                          }`}
                        >
                          <span>{opt.iconText}</span>
                          <span>{opt.label}</span>
                          {isSelected && <Check className="w-3.5 h-3.5 stroke-[3] text-black" />}
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>

              {/* Name & Pen Name Inputs */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="flex flex-col gap-1.5">
                  <label className="font-space text-xs font-bold text-black" htmlFor="author-name">
                    Họ và tên tác giả:
                  </label>
                  <input
                    id="author-name"
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Nguyễn Minh Anh"
                    className="w-full p-2.5 bg-paper-warm/30 border-2 border-black rounded-xl font-space text-sm font-bold shadow-neo-xs focus:outline-none focus:bg-white transition-all"
                  />
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="font-space text-xs font-bold text-black flex items-center justify-between" htmlFor="author-penname">
                    <span>Bút danh hiển thị (Pen Name):</span>
                    <span className="text-red-500 font-extrabold">*</span>
                  </label>
                  <input
                    id="author-penname"
                    type="text"
                    required
                    value={penName}
                    onChange={(e) => setPenName(e.target.value)}
                    placeholder="Minh Anh"
                    className="w-full p-2.5 bg-paper-warm/30 border-2 border-black rounded-xl font-space text-sm font-bold shadow-neo-xs focus:outline-none focus:bg-white transition-all"
                  />
                </div>
              </div>

              {/* Email (Readonly) */}
              <div className="flex flex-col gap-1.5">
                <label className="font-space text-xs font-bold text-gray-700 flex items-center gap-1.5" htmlFor="author-email">
                  <Mail className="w-3.5 h-3.5" />
                  Địa chỉ Email tài khoản:
                </label>
                <div className="p-2.5 bg-paper-warm/20 border border-black/40 rounded-xl font-mono text-xs text-gray-700 flex items-center justify-between">
                  <span>{userProfile.email}</span>
                  <span className="text-[10px] font-space font-bold uppercase bg-white px-2 py-0.5 rounded border border-black">
                    Chỉ đọc
                  </span>
                </div>
              </div>

              {/* Bio / Book Inscription */}
              <div className="flex flex-col gap-1.5">
                <label className="font-space text-xs font-bold text-black flex items-center gap-1.5" htmlFor="author-bio">
                  <FileText className="w-3.5 h-3.5" />
                  Lời đề tựa / Châm ngôn cho cuốn sổ (Bio):
                </label>
                <input
                  id="author-bio"
                  type="text"
                  value={bio}
                  onChange={(e) => setBio(e.target.value)}
                  placeholder="Mỗi ngày là một trang sách mới..."
                  className="w-full p-2.5 bg-paper-warm/30 border-2 border-black rounded-xl font-serif text-sm italic shadow-neo-xs focus:outline-none focus:bg-white transition-all"
                />
              </div>
            </div>
          </section>

          {/* SECTION 2: CHANGE PASSWORD */}
          <section className="flex flex-col gap-5">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-white border border-black flex items-center justify-center font-bold shadow-neo-xs">
                <Lock className="w-4 h-4 text-black" />
              </div>
              <h2 className="font-space text-lg font-extrabold text-black uppercase tracking-wider">
                2. Đổi Mật Khẩu Bảo Mật
              </h2>
            </div>

            <form
              onSubmit={handleChangePassword}
              className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm flex flex-col gap-4"
            >
              {/* Current Password */}
              <div className="flex flex-col gap-1.5">
                <label className="font-space text-xs font-bold text-black" htmlFor="curr-pass">
                  Mật khẩu hiện tại:
                </label>
                <div className="relative flex items-center">
                  <Key className="w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none" />
                  <input
                    id="curr-pass"
                    type={showCurrentPassword ? 'text' : 'password'}
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="Nhập mật khẩu hiện tại..."
                    className="w-full pl-10 pr-10 py-2.5 bg-paper-warm/30 border-2 border-black rounded-xl font-space text-xs sm:text-sm font-bold shadow-neo-xs focus:outline-none focus:bg-white transition-all"
                  />
                  <button
                    type="button"
                    onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    className="absolute right-3 text-gray-500 hover:text-black cursor-pointer"
                    aria-label={showCurrentPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showCurrentPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* New Password & Confirm */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* New Password */}
                <div className="flex flex-col gap-1.5">
                  <label className="font-space text-xs font-bold text-black" htmlFor="new-pass">
                    Mật khẩu mới (tối thiểu 6 ký tự):
                  </label>
                  <div className="relative flex items-center">
                    <Key className="w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none" />
                    <input
                      id="new-pass"
                      type={showNewPassword ? 'text' : 'password'}
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="Nhập mật khẩu mới..."
                      className="w-full pl-10 pr-10 py-2.5 bg-paper-warm/30 border-2 border-black rounded-xl font-space text-xs sm:text-sm font-bold shadow-neo-xs focus:outline-none focus:bg-white transition-all"
                    />
                    <button
                      type="button"
                      onClick={() => setShowNewPassword(!showNewPassword)}
                      className="absolute right-3 text-gray-500 hover:text-black cursor-pointer"
                      aria-label={showNewPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                    >
                      {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>

                  {/* Password Strength Meter */}
                  {newPassword && (
                    <div className="flex flex-col gap-1 mt-1">
                      <div className="w-full h-1.5 bg-gray-200 rounded-full overflow-hidden border border-black/20">
                        <div className={`h-full transition-all duration-300 ${passwordStrength.width} ${passwordStrength.color}`} />
                      </div>
                      <span className="font-space text-[10px] text-gray-600 font-bold">
                        Độ mạnh: <strong className="text-black">{passwordStrength.label}</strong>
                      </span>
                    </div>
                  )}
                </div>

                {/* Confirm New Password */}
                <div className="flex flex-col gap-1.5">
                  <label className="font-space text-xs font-bold text-black" htmlFor="confirm-pass">
                    Xác nhận mật khẩu mới:
                  </label>
                  <div className="relative flex items-center">
                    <Key className="w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none" />
                    <input
                      id="confirm-pass"
                      type={showConfirmPassword ? 'text' : 'password'}
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Nhập lại mật khẩu mới..."
                      className="w-full pl-10 pr-10 py-2.5 bg-paper-warm/30 border-2 border-black rounded-xl font-space text-xs sm:text-sm font-bold shadow-neo-xs focus:outline-none focus:bg-white transition-all"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="absolute right-3 text-gray-500 hover:text-black cursor-pointer"
                      aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                    >
                      {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>
              </div>

              <div className="flex justify-end pt-2">
                <NeoButton
                  type="submit"
                  variant="paper"
                  size="sm"
                  disabled={passwordLoading || !currentPassword || !newPassword || !confirmPassword}
                  className="font-space font-extrabold text-xs shadow-neo-sm cursor-pointer"
                  icon={<Key className="w-3.5 h-3.5" />}
                >
                  {passwordLoading ? 'Đang cập nhật...' : 'Cập nhật mật khẩu'}
                </NeoButton>
              </div>
            </form>
          </section>

          {/* SECTION 3: PREFERENCES & TIMEZONE */}
          <section className="flex flex-col gap-5">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-white border border-black flex items-center justify-center font-bold shadow-neo-xs">
                <Globe className="w-4 h-4 text-black" />
              </div>
              <h2 className="font-space text-lg font-extrabold text-black uppercase tracking-wider">
                3. Ngôn Ngữ & Múi Giờ
              </h2>
            </div>

            <div className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm grid grid-cols-1 sm:grid-cols-2 gap-5">
              {/* Language Selection */}
              <div className="flex flex-col gap-2">
                <label className="font-space text-xs font-bold text-black flex items-center gap-1.5">
                  <Globe className="w-3.5 h-3.5" />
                  Ngôn ngữ giao diện (Language):
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setLanguage('vi')}
                    className={`py-2 px-3 rounded-xl border-2 border-black font-space text-xs font-extrabold transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                      language === 'vi'
                        ? 'bg-primary-container text-black shadow-neo-xs'
                        : 'bg-paper-warm/40 hover:bg-paper-warm text-gray-700'
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
                        ? 'bg-primary-container text-black shadow-neo-xs'
                        : 'bg-paper-warm/40 hover:bg-paper-warm text-gray-700'
                    }`}
                  >
                    <span>English (EN)</span>
                    {language === 'en' && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                  </button>
                </div>
              </div>

              {/* Timezone Selection */}
              <div className="flex flex-col gap-2">
                <label className="font-space text-xs font-bold text-black flex items-center gap-1.5" htmlFor="timezone-select">
                  <Clock className="w-3.5 h-3.5" />
                  Múi giờ ghi nhật ký (Timezone):
                </label>
                <select
                  id="timezone-select"
                  value={timezone}
                  onChange={(e) => setTimezone(e.target.value)}
                  className="w-full p-2.5 bg-paper-warm/30 border-2 border-black rounded-xl font-space text-xs font-bold shadow-neo-xs focus:outline-none cursor-pointer"
                >
                  <option value="Asia/Ho_Chi_Minh">Asia/Ho_Chi_Minh (GMT+7)</option>
                  <option value="Asia/Bangkok">Asia/Bangkok (GMT+7)</option>
                  <option value="Asia/Tokyo">Asia/Tokyo (GMT+9)</option>
                  <option value="UTC">UTC (GMT+0)</option>
                </select>
              </div>
            </div>
          </section>

          {/* SECTION 4: SECURITY VAULT & METRICS */}
          <section className="flex flex-col gap-5">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-white border border-black flex items-center justify-center font-bold shadow-neo-xs">
                <Shield className="w-4 h-4 text-green-700" />
              </div>
              <h2 className="font-space text-lg font-extrabold text-black uppercase tracking-wider">
                4. Gói Tài Khoản & Két Bảo Mật Sổ Tay
              </h2>
            </div>

            <div className="bg-surface-card border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Shield className="w-5 h-5 text-green-700 stroke-[2.3]" />
                  <span className="font-space text-sm font-extrabold text-black">
                    Cơ Chế Bảo Mật & Lưu Trữ Riêng Tư
                  </span>
                </div>
                <span className="px-3 py-1 bg-primary-container text-black font-space text-xs font-extrabold rounded-lg border border-black uppercase">
                  {userProfile.plan} TIER
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs font-space">
                <div className="p-3 bg-white rounded-xl border border-black flex flex-col gap-1 shadow-neo-xs">
                  <span className="text-gray-500 font-bold">Chuẩn mã hóa:</span>
                  <span className="font-extrabold text-black flex items-center gap-1">
                    <Check className="w-3.5 h-3.5 text-green-700 stroke-[3]" /> AES-256 Vault Cục Bộ
                  </span>
                </div>
                <div className="p-3 bg-white rounded-xl border border-black flex flex-col gap-1 shadow-neo-xs">
                  <span className="text-gray-500 font-bold">Tổng bài viết đã cất giữ:</span>
                  <span className="font-extrabold text-black text-sm">
                    {entries.length} trang nhật ký
                  </span>
                </div>
              </div>

              {hasDraft && (
                <div className="flex items-center gap-2 text-xs font-space font-bold text-amber-800 bg-amber-100 p-3 rounded-xl border border-amber-300">
                  <AlertCircle className="w-4 h-4 text-amber-800 shrink-0" />
                  <span>Bạn đang có 1 bản nháp nhật ký tự động lưu trên trình duyệt này.</span>
                </div>
              )}
            </div>
          </section>

          {/* SECTION 5: ACTION FOOTER */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t-2 border-black">
            <Link
              href="/dashboard"
              className="w-full sm:w-auto px-5 py-2.5 bg-white text-black font-space text-xs font-bold border-2 border-black rounded-xl hover:bg-gray-100 transition-colors cursor-pointer shadow-neo-xs text-center"
            >
              ← Về Bảng điều khiển
            </Link>

            <NeoButton
              variant="primary"
              size="md"
              onClick={handleSaveProfile}
              className="w-full sm:w-auto font-space font-extrabold text-xs sm:text-sm shadow-neo cursor-pointer justify-center"
              icon={<Check className="w-4 h-4 stroke-[2.5]" />}
            >
              Lưu toàn bộ thay đổi
            </NeoButton>
          </div>
        </div>
      </div>
    </div>
  );
}

