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
import { AccountAccessPanel } from './AccountAccessPanel';
import { AccountDataExport } from '@/features/export';
import { useAccount } from '../context/AccountContext';
import { useTranslation } from 'react-i18next';

export function SettingsView({ entryCount, draftStorageKey }: { entryCount: number | null; draftStorageKey: string }) {
  const { t } = useTranslation();
  const { userId } = useAccount();
  const {
    userProfile,
    entryCount: savedEntryCount,
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
    fieldErrors,
    hasDraft,
    handleSaveProfile,
  } = useSettings({ entryCount, draftStorageKey });

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
            <span>{t('settingsExtra.back')}</span>
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
                  {t('settingsExtra.header')}
                </h1>
                <p className="font-sans text-xs sm:text-sm text-gray-600 mt-0.5">
                  {t('settingsExtra.description')}
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
                {t('settings.authorSection')}
              </h2>
            </div>

            <div className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm flex flex-col gap-5">
              {/* Avatar Selector */}
              <div className="flex flex-col gap-2">
                <label className="font-space text-xs font-extrabold text-black uppercase flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                  {t('settingsExtra.avatar')}:
                </label>
                <div className="flex items-center gap-3 flex-wrap">
                  {/* Current Avatar Preview */}
                  <div className="relative w-14 h-14 rounded-2xl border-2 border-black overflow-hidden bg-paper-warm shadow-neo shrink-0">
                    <Image
                      src={avatarUrl}
                      alt={t('settingsExtra.avatarAlt')}
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
                    {t('settings.name')}:
                  </label>
                  <input
                    id="author-name"
                    type="text"
                    value={name}
                    aria-invalid={!!fieldErrors.name}
                    aria-describedby={fieldErrors.name ? 'author-name-error' : undefined}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Nguyễn Minh Anh"
                    className="w-full p-2.5 bg-paper-warm/30 border-2 border-black rounded-xl font-space text-sm font-bold shadow-neo-xs focus:outline-none focus:bg-white transition-all"
                  />
                  {fieldErrors.name && <p id="author-name-error" role="alert" className="text-xs text-red-700">{fieldErrors.name}</p>}
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="font-space text-xs font-bold text-black flex items-center justify-between" htmlFor="author-penname">
                    <span>{t('settings.penName')}:</span>
                    <span className="text-red-500 font-extrabold">*</span>
                  </label>
                  <input
                    id="author-penname"
                    type="text"
                    required
                    value={penName}
                    aria-invalid={!!fieldErrors.penName}
                    aria-describedby={fieldErrors.penName ? 'author-penname-error' : undefined}
                    onChange={(e) => setPenName(e.target.value)}
                    placeholder="Minh Anh"
                    className="w-full p-2.5 bg-paper-warm/30 border-2 border-black rounded-xl font-space text-sm font-bold shadow-neo-xs focus:outline-none focus:bg-white transition-all"
                  />
                  {fieldErrors.penName && <p id="author-penname-error" role="alert" className="text-xs text-red-700">{fieldErrors.penName}</p>}
                </div>
              </div>

              {/* Email (Readonly) */}
              <div className="flex flex-col gap-1.5">
                <label className="font-space text-xs font-bold text-gray-700 flex items-center gap-1.5" htmlFor="author-email">
                  <Mail className="w-3.5 h-3.5" />
                  {t('settingsExtra.email')}:
                </label>
                <div className="p-2.5 bg-paper-warm/20 border border-black/40 rounded-xl font-mono text-xs text-gray-700 flex items-center justify-between">
                  <span>{userProfile.email}</span>
                  <span className="text-[10px] font-space font-bold uppercase bg-white px-2 py-0.5 rounded border border-black">
                    {t('settingsExtra.readOnly')}
                  </span>
                </div>
              </div>

              {/* Bio / Book Inscription */}
              <div className="flex flex-col gap-1.5">
                <label className="font-space text-xs font-bold text-black flex items-center gap-1.5" htmlFor="author-bio">
                  <FileText className="w-3.5 h-3.5" />
                  {t('settingsExtra.bio')}:
                </label>
                <input
                  id="author-bio"
                  type="text"
                  value={bio}
                  onChange={(e) => setBio(e.target.value)}
                  disabled
                  title={t('settingsExtra.bioTitle')}
                  placeholder={t('settingsExtra.bioPlaceholder')}
                  className="w-full p-2.5 bg-paper-warm/30 border-2 border-black rounded-xl font-serif text-sm italic shadow-neo-xs focus:outline-none focus:bg-white transition-all"
                />
                <p className="text-xs text-gray-600">{t('settingsExtra.bioUnavailable')}</p>
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
                {t('settingsExtra.password')}
              </h2>
            </div>
            <div className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm font-space text-sm text-gray-700">
              {t('settingsExtra.passwordUnavailable')}
            </div>
          </section>

          {/* SECTION 3: PREFERENCES & TIMEZONE */}
          <section className="flex flex-col gap-5">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-white border border-black flex items-center justify-center font-bold shadow-neo-xs">
                <Globe className="w-4 h-4 text-black" />
              </div>
              <h2 className="font-space text-lg font-extrabold text-black uppercase tracking-wider">
                {t('settings.languageSection')}
              </h2>
            </div>

            <div className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm grid grid-cols-1 sm:grid-cols-2 gap-5">
              {/* Language Selection */}
              <div className="flex flex-col gap-2">
                <label className="font-space text-xs font-bold text-black flex items-center gap-1.5">
                  <Globe className="w-3.5 h-3.5" />
                  {t('settings.language')}:
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
                  {t('settings.timezone')}:
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
                {t('settingsExtra.security')}
              </h2>
            </div>

            <div className="bg-surface-card border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Shield className="w-5 h-5 text-green-700 stroke-[2.3]" />
                  <span className="font-space text-sm font-extrabold text-black">
                    {t('settingsExtra.securityTitle')}
                  </span>
                </div>
                <span className="px-3 py-1 bg-primary-container text-black font-space text-xs font-extrabold rounded-lg border border-black uppercase">
                  {userProfile.plan} TIER
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs font-space">
                <div className="p-3 bg-white rounded-xl border border-black flex flex-col gap-1 shadow-neo-xs">
                  <span className="text-gray-500 font-bold">{t('settingsExtra.encryption')}</span>
                  <span className="font-extrabold text-black flex items-center gap-1">
                    <Check className="w-3.5 h-3.5 text-green-700 stroke-[3]" /> {t('settingsExtra.encrypted')}
                  </span>
                </div>
                <div className="p-3 bg-white rounded-xl border border-black flex flex-col gap-1 shadow-neo-xs">
                  <span className="text-gray-500 font-bold">{t('settingsExtra.entries')}</span>
                  <span className="font-extrabold text-black text-sm">
                    {savedEntryCount === null ? '—' : t('settingsExtra.entryCount', { count: savedEntryCount })}
                  </span>
                </div>
              </div>

              {hasDraft && (
                <div className="flex items-center gap-2 text-xs font-space font-bold text-amber-800 bg-amber-100 p-3 rounded-xl border border-amber-300">
                  <AlertCircle className="w-4 h-4 text-amber-800 shrink-0" />
                  <span>{t('settingsExtra.draft')}</span>
                </div>
              )}
            </div>
          </section>

          <AccountAccessPanel />
          <AccountDataExport userId={userId} />

          {/* SECTION 5: ACTION FOOTER */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t-2 border-black">
            <Link
              href="/dashboard"
              className="w-full sm:w-auto px-5 py-2.5 bg-white text-black font-space text-xs font-bold border-2 border-black rounded-xl hover:bg-gray-100 transition-colors cursor-pointer shadow-neo-xs text-center"
            >
              ← {t('settingsExtra.back')}
            </Link>

            <NeoButton
              variant="primary"
              size="md"
              onClick={handleSaveProfile}
              className="w-full sm:w-auto font-space font-extrabold text-xs sm:text-sm shadow-neo cursor-pointer justify-center"
              icon={<Check className="w-4 h-4 stroke-[2.5]" />}
            >
              {t('settings.save')}
            </NeoButton>
          </div>
        </div>
      </div>
    </div>
  );
}

