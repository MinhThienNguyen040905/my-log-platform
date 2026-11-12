'use client';

import React, { useId, useState, useSyncExternalStore } from 'react';
import Link from 'next/link';
import { User, Mail, Shield, Globe, Clock, Check, ArrowLeft, AlertCircle, Lock, Database, MessageSquarePlus } from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { useSettings } from '../hooks/useSettings';
import { ConsentsPanel, SessionsPanel } from './AccountAccessPanel';
import { ChangePasswordForm } from './ChangePasswordForm';
import { StartOverCard } from './StartOverCard';
import { AccountDataExport } from '@/features/export';
import { UserFeedbackCard } from '@/features/feedback';
import { useAccount } from '../context/AccountContext';
import { useTranslation } from 'react-i18next';

const TABS = ['profile', 'security', 'data', 'feedback'] as const;
type Tab = (typeof TABS)[number];
const TAB_ICONS: Record<Tab, React.ComponentType<{ className?: string }>> = { profile: User, security: Lock, data: Database, feedback: MessageSquarePlus };
const isTab = (value: string | null): value is Tab => !!value && (TABS as readonly string[]).includes(value);
/** Address users write to when they want their account deleted; the line is hidden when it is not configured. */
const SUPPORT_EMAIL = process.env.NEXT_PUBLIC_SUPPORT_EMAIL?.trim() || '';
/** `?tab=` as an external store: read on the client only, so server and client markup stay identical. */
const subscribeToUrl = (onChange: () => void) => {
  window.addEventListener('popstate', onChange);
  return () => window.removeEventListener('popstate', onChange);
};
const tabFromUrl = () => new URLSearchParams(window.location.search).get('tab');

const card = 'bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm';
const input = 'w-full p-2.5 bg-paper-warm/30 border-2 border-black rounded-xl font-space text-sm font-bold shadow-neo-xs focus:outline-none focus:bg-white transition-all';

/** `mascotPicker` is composed by the route (it belongs to the insights feature). The open tab is kept in `?tab=`. */
export function SettingsView({ entryCount, draftStorageKey, mascotPicker }: { entryCount: number | null; draftStorageKey: string; mascotPicker?: React.ReactNode }) {
  const { t } = useTranslation();
  const { userId } = useAccount();
  const settings = useSettings({ entryCount, draftStorageKey });
  const urlTab = useSyncExternalStore(subscribeToUrl, tabFromUrl, () => null);
  const [chosen, setChosen] = useState<Tab | null>(null);
  const tab: Tab = chosen ?? (isTab(urlTab) ? urlTab : 'profile');
  const baseId = useId();

  const select = (next: Tab) => {
    setChosen(next);
    const url = new URL(window.location.href);
    url.searchParams.set('tab', next);
    window.history.replaceState(window.history.state, '', url);
  };
  const onTabKey = (event: React.KeyboardEvent) => {
    const step = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0;
    if (!step) return;
    event.preventDefault();
    const next = TABS[(TABS.indexOf(tab) + step + TABS.length) % TABS.length];
    select(next);
    document.getElementById(`${baseId}-tab-${next}`)?.focus();
  };

  return (
    <div className="w-full min-h-screen bg-bg-canvas pb-20 selection:bg-brand-lime selection:text-black">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 pt-6 flex flex-col gap-6">
        <Link href="/dashboard" className="inline-flex items-center gap-2 self-start text-xs font-space font-bold text-gray-600 hover:text-black hover:underline">
          <ArrowLeft className="w-4 h-4" aria-hidden="true" />
          <span>{t('settingsExtra.back')}</span>
        </Link>

        <div className="relative w-full bg-paper-warm border-[2.5px] border-black rounded-3xl p-5 sm:p-8 shadow-neo-lg flex flex-col gap-6 overflow-visible">
          <WashiTape color="lavender" rotate={-1.5} className="absolute -top-3.5 left-10 w-36 z-20 pointer-events-none" />

          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-primary-container border-2 border-black shadow-neo flex items-center justify-center shrink-0">
              <User className="w-6 h-6 text-black stroke-[2.3]" aria-hidden="true" />
            </div>
            <div>
              <h1 className="font-space text-2xl sm:text-3xl font-extrabold text-black">{t('settingsExtra.header')}</h1>
              <p className="font-sans text-sm text-gray-600 mt-0.5">{t('settingsExtra.description')}</p>
            </div>
          </div>

          <div role="tablist" aria-label={t('settingsExtra.header')} onKeyDown={onTabKey}
            className="flex gap-2 overflow-x-auto pb-1 -mx-1 px-1 border-b-2 border-black/80">
            {TABS.map((id) => {
              const Icon = TAB_ICONS[id];
              const selected = tab === id;
              return (
                <button
                  key={id}
                  id={`${baseId}-tab-${id}`}
                  type="button"
                  role="tab"
                  aria-selected={selected}
                  aria-controls={`${baseId}-panel-${id}`}
                  tabIndex={selected ? 0 : -1}
                  onClick={() => select(id)}
                  className={`shrink-0 inline-flex items-center gap-1.5 rounded-t-xl border-2 border-b-0 px-3.5 py-2 font-space text-sm font-bold transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${
                    selected ? 'border-black bg-white text-black' : 'border-transparent text-gray-600 hover:text-black'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  {t(`settingsExtra.tabs.${id}`)}
                </button>
              );
            })}
          </div>

          <div role="tabpanel" id={`${baseId}-panel-${tab}`} aria-labelledby={`${baseId}-tab-${tab}`} className="flex flex-col gap-6">
            {tab === 'profile' && <ProfileTab settings={settings} mascotPicker={mascotPicker} />}
            {tab === 'security' && <>
              <ChangePasswordForm canChangePassword={settings.userProfile.canChangePassword} userEmail={settings.userProfile.email} />
              <SessionsPanel />
            </>}
            {tab === 'data' && <DataTab entryCount={settings.entryCount} hasDraft={settings.hasDraft} userId={userId} hasPassword={settings.userProfile.canChangePassword !== false} />}
            {tab === 'feedback' && <UserFeedbackCard userId={userId} />}
          </div>
        </div>
      </div>
    </div>
  );
}

function ProfileTab({ settings, mascotPicker }: { settings: ReturnType<typeof useSettings>; mascotPicker?: React.ReactNode }) {
  const { t } = useTranslation();
  const { userProfile, name, setName, penName, setPenName, timezone, setTimezone, language, setLanguage, fieldErrors, dirty, saving, handleSaveProfile } = settings;
  const languageButton = (value: 'vi' | 'en', label: string) => (
    <button
      type="button"
      onClick={() => setLanguage(value)}
      aria-pressed={language === value}
      className={`py-2 px-3 rounded-xl border-2 border-black font-space text-xs font-extrabold transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
        language === value ? 'bg-primary-container text-black shadow-neo-xs' : 'bg-paper-warm/40 hover:bg-paper-warm text-gray-700'
      }`}
    >
      <span>{label}</span>
      {language === value && <Check className="w-3.5 h-3.5 stroke-[3]" aria-hidden="true" />}
    </button>
  );

  return <>
    <section className={`${card} flex flex-col gap-5`}>
      {mascotPicker}
    </section>

    <form className={`${card} flex flex-col gap-5`} onSubmit={(event) => { event.preventDefault(); void handleSaveProfile(); }}>
      <h2 className="font-space text-base font-extrabold text-black">{t('settings.authorSection')}</h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="flex flex-col gap-1.5">
          <label className="font-space text-xs font-bold text-black" htmlFor="author-name">{t('settings.name')}</label>
          <input id="author-name" type="text" value={name} aria-invalid={!!fieldErrors.name}
            aria-describedby={fieldErrors.name ? 'author-name-error' : undefined}
            onChange={(e) => setName(e.target.value)} placeholder="Nguyễn Minh Anh" className={input} />
          {fieldErrors.name && <p id="author-name-error" role="alert" className="text-xs text-red-700">{fieldErrors.name}</p>}
        </div>
        <div className="flex flex-col gap-1.5">
          <label className="font-space text-xs font-bold text-black flex items-center justify-between" htmlFor="author-penname">
            <span>{t('settings.penName')}</span>
            <span className="text-red-500 font-extrabold" aria-hidden="true">*</span>
          </label>
          <input id="author-penname" type="text" required value={penName} aria-invalid={!!fieldErrors.penName}
            aria-describedby={fieldErrors.penName ? 'author-penname-error' : undefined}
            onChange={(e) => setPenName(e.target.value)} placeholder="Minh Anh" className={input} />
          {fieldErrors.penName && <p id="author-penname-error" role="alert" className="text-xs text-red-700">{fieldErrors.penName}</p>}
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <span className="font-space text-xs font-bold text-gray-700 flex items-center gap-1.5">
          <Mail className="w-3.5 h-3.5" aria-hidden="true" />
          {t('settingsExtra.email')}
        </span>
        <p className="p-2.5 bg-paper-warm/20 border border-black/40 rounded-xl font-mono text-sm text-gray-800 break-all" data-testid="account-email">
          {userProfile.email || '—'}
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-5 pt-1">
        <div className="flex flex-col gap-2">
          <span className="font-space text-xs font-bold text-black flex items-center gap-1.5">
            <Globe className="w-3.5 h-3.5" aria-hidden="true" />
            {t('settings.language')}
          </span>
          <div className="grid grid-cols-2 gap-2">
            {languageButton('vi', 'Tiếng Việt')}
            {languageButton('en', 'English')}
          </div>
        </div>
        <div className="flex flex-col gap-2">
          <label className="font-space text-xs font-bold text-black flex items-center gap-1.5" htmlFor="timezone-select">
            <Clock className="w-3.5 h-3.5" aria-hidden="true" />
            {t('settings.timezone')}
          </label>
          <select id="timezone-select" value={timezone} onChange={(e) => setTimezone(e.target.value)}
            className="w-full p-2.5 bg-paper-warm/30 border-2 border-black rounded-xl font-space text-xs font-bold shadow-neo-xs focus:outline-none cursor-pointer">
            <option value="Asia/Ho_Chi_Minh">Asia/Ho_Chi_Minh (GMT+7)</option>
            <option value="Asia/Bangkok">Asia/Bangkok (GMT+7)</option>
            <option value="Asia/Tokyo">Asia/Tokyo (GMT+9)</option>
            <option value="UTC">UTC (GMT+0)</option>
          </select>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-end gap-3 border-t border-black/15 pt-4">
        {dirty && <span role="status" className="font-sans text-sm text-amber-800">{t('settingsExtra.unsaved')}</span>}
        <NeoButton type="submit" variant="primary" size="md" disabled={!dirty || saving}
          className="font-space font-extrabold text-sm justify-center disabled:opacity-50 disabled:cursor-not-allowed"
          icon={<Check className="w-4 h-4 stroke-[2.5]" />}>
          {saving ? t('settingsExtra.saving') : t('settings.save')}
        </NeoButton>
      </div>
    </form>
  </>;
}

function DataTab({ entryCount, hasDraft, userId, hasPassword }: { entryCount: number | null; hasDraft: boolean; userId: string; hasPassword: boolean }) {
  const { t } = useTranslation();
  return <>
    <ConsentsPanel />

    <section className={`${card} flex flex-col gap-4`}>
      <h3 className="font-space text-base font-extrabold flex items-center gap-2">
        <Shield className="w-4 h-4 text-green-700" aria-hidden="true" />
        {t('settingsExtra.securityTitle')}
      </h3>
      <dl className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
        <div className="p-3 bg-paper-warm/40 rounded-xl border border-black/30">
          <dt className="text-gray-600">{t('settingsExtra.encryption')}</dt>
          <dd className="font-bold flex items-center gap-1"><Check className="w-4 h-4 text-green-700 stroke-[3]" aria-hidden="true" /> {t('settingsExtra.encrypted')}</dd>
        </div>
        <div className="p-3 bg-paper-warm/40 rounded-xl border border-black/30">
          <dt className="text-gray-600">{t('settingsExtra.entries')}</dt>
          <dd className="font-bold">{entryCount === null ? '—' : t('settingsExtra.entryCount', { count: entryCount })}</dd>
        </div>
      </dl>
      {hasDraft && (
        <p className="flex items-center gap-2 text-sm font-bold text-amber-800 bg-amber-100 p-3 rounded-xl border border-amber-300">
          <AlertCircle className="w-4 h-4 shrink-0" aria-hidden="true" />
          {t('settingsExtra.draft')}
        </p>
      )}
    </section>

    <AccountDataExport userId={userId} />
    <StartOverCard userId={userId} hasPassword={hasPassword} />

    {SUPPORT_EMAIL && (
      <p className="font-sans text-sm text-gray-700" data-testid="account-deletion-contact">
        {t('settingsExtra.deleteByEmail')}{' '}
        <a className="font-bold underline" href={`mailto:${SUPPORT_EMAIL}?subject=${encodeURIComponent(t('settingsExtra.deleteEmailSubject'))}`}>{SUPPORT_EMAIL}</a>
      </p>
    )}
  </>;
}
