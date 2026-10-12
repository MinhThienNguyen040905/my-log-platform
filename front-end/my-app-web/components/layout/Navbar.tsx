'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { usePathname, useRouter } from 'next/navigation';
import type { UserProfile } from '@/types';
import { useToast } from '@/providers/ToastProvider';
import { useTranslation } from 'react-i18next';
import {
  Flame,
  Menu,
  X,
  LayoutDashboard,
  PenTool,
  Calendar,
  BarChart3,
  LogOut,
  Globe,
  Settings,
  Check,
  ChevronDown,
  Shield,
} from 'lucide-react';

export function Navbar({ onLogout, penName, streakCount, userProfile, updateProfile, hasUnsavedJournalChanges }: {
  onLogout: () => Promise<void>;
  penName: string;
  streakCount: number;
  userProfile: UserProfile;
  updateProfile: (data: Partial<UserProfile>) => Promise<void>;
  hasUnsavedJournalChanges: () => boolean;
}) {
  const pathname = usePathname();
  const router = useRouter();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [pendingAction, setPendingAction] = useState<{ kind: 'navigate'; destination: string } | { kind: 'logout' } | null>(null);

  const { showToast } = useToast();
  const { t } = useTranslation();

  const navItems = [
    { label: t('nav.dashboard'), path: '/dashboard', icon: LayoutDashboard },
    { label: t('nav.journal'), path: '/journal-editor', icon: PenTool },
    { label: t('nav.history'), path: '/history-calendar', icon: Calendar },
    { label: t('nav.insights'), path: '/insight', icon: BarChart3 },
  ];

  const isActive = (itemPath: string) => {
    return pathname === itemPath || (itemPath !== '/' && pathname.startsWith(itemPath));
  };

  const confirmLeave = (destination?: string) => {
    if (destination && `${window.location.pathname}${window.location.search}` === destination) return true;
    if (!hasUnsavedJournalChanges()) return true;
    setPendingAction(destination ? { kind: 'navigate', destination } : { kind: 'logout' });
    return false;
  };

  const handleLogout = async () => {
    if (!confirmLeave()) return;
    await performLogout();
  };

  const performLogout = async () => {
    setUserMenuOpen(false);
    setMobileOpen(false);
    try {
      await onLogout();
      showToast({ title: 'Đã đăng xuất tài khoản', type: 'info' });
    } catch {
      showToast({ title: 'Không thể đăng xuất', message: 'Vui lòng thử lại.', type: 'error' });
    }
  };

  const continuePendingAction = () => {
    if (!pendingAction) return;
    const action = pendingAction;
    setPendingAction(null);
    if (action.kind === 'logout') void performLogout();
    else router.push(action.destination);
  };

  const handleToggleLanguage = async (lang: 'vi' | 'en') => {
    try {
      await updateProfile({ language: lang });
      showToast({ title: lang === 'vi' ? 'Ngôn ngữ: Tiếng Việt' : 'Language: English', type: 'info' });
    } catch {
      showToast({ title: 'Không thể đổi ngôn ngữ', message: 'Vui lòng thử lại.', type: 'error' });
    }
  };

  return (
    <>
      {/* Desktop & Top Sticky Header */}
      <header className="fixed top-0 left-0 right-0 w-full z-40 bg-bg-canvas/95 backdrop-blur-md border-b-2 border-on-background">
        <div className="h-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-3 sm:gap-4">
          {/* Brand Logo Container */}
          <Link
            href="/dashboard"
            onNavigate={(event) => { if (!confirmLeave('/dashboard')) event.preventDefault(); }}
            className="flex items-center shrink-0 group focus:outline-none"
          >
            <div className="relative flex items-center justify-center h-10 sm:h-11 w-auto max-w-[130px] sm:max-w-[160px] overflow-hidden">
              <Image
                src="/logo.png"
                alt="MyLog Logo"
                width={140}
                height={44}
                priority
                className="h-10 sm:h-11 w-auto object-contain transition-transform duration-150 group-hover:scale-105"
              />
            </div>
          </Link>

          {/* Desktop Navigation Tabs */}
          <nav className="hidden lg:flex items-center gap-2">
            {navItems.map((item) => {
              const active = isActive(item.path);
              return (
                <Link
                  key={item.path}
                  href={item.path}
                  onNavigate={(event) => { if (!confirmLeave(item.path)) event.preventDefault(); }}
                  className={`px-3.5 py-2 font-space text-sm font-semibold rounded-xl transition-all duration-150 ${
                    active
                      ? 'bg-paper-warm text-on-surface border-neo-sm shadow-neo-sm font-bold'
                      : 'text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high'
                  }`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>

          {/* Right Section: CTA Button + Divider + User Profile Info */}
          <div className="flex items-center gap-2.5 sm:gap-3.5">

            {/* Vertical Divider */}
            <div className="h-8 w-[1.5px] bg-border-soft hidden sm:block shrink-0" />

            {/* User Profile Block & Avatar with Dropdown */}
            <div className="relative">
              <button
                type="button"
                onClick={() => setUserMenuOpen(!userMenuOpen)}
                className="flex items-center gap-2 sm:gap-2.5 shrink-0 text-left cursor-pointer p-1 rounded-2xl hover:bg-paper-warm transition-colors"
                title={t('nav.accountOptions')}
              >
                <div className="hidden sm:flex flex-col items-end leading-none">
                  <span className="font-space font-extrabold text-sm text-on-surface tracking-tight flex items-center gap-1">
                    {penName}
                    <ChevronDown className="w-3 h-3 text-gray-500" />
                  </span>
                  <div className="mt-1 inline-flex items-center gap-1 bg-paper-warm px-2 py-0.5 rounded border border-black/80 shadow-[1px_1px_0px_#111111]">
                    <Flame className="w-3.5 h-3.5 text-orange-500 fill-orange-500 shrink-0" />
                    <span className="font-space font-bold text-[11px] text-on-surface tracking-tight">
                      {streakCount} {t('nav.streak')}
                    </span>
                  </div>
                </div>

                {/* Avatar */}
                <div className="relative w-9 h-9 sm:w-10 sm:h-10 rounded-2xl border-2 border-black shadow-[2px_2px_0px_#111111] overflow-hidden shrink-0 bg-paper-warm transition-transform hover:scale-105">
                  <Image
                    src={userProfile.avatarUrl || '/avatar.png'}
                    alt="User Avatar"
                    width={40}
                    height={40}
                    priority
                    className="w-full h-full object-cover"
                  />
                </div>
              </button>

              {/* Neo-brutalist User Menu Dropdown (FR-ACCOUNT-02 & 03) */}
              {userMenuOpen && (
                <>
                  <div
                    className="fixed inset-0 z-40"
                    onClick={() => setUserMenuOpen(false)}
                  />
                  <div className="absolute right-0 mt-2 w-64 bg-paper-warm border-2 border-black rounded-2xl p-3 shadow-neo-lg z-50 animate-in fade-in slide-in-from-top-2 flex flex-col gap-2">
                    {/* User info snippet */}
                    <div className="p-2.5 bg-white rounded-xl border border-black flex flex-col gap-0.5">
                      <div className="flex items-center justify-between">
                        <span className="font-space text-xs font-extrabold text-black">
                          {penName}
                        </span>
                        <span className="px-1.5 py-0.2 bg-primary-container text-black font-space text-[9px] font-extrabold rounded border border-black uppercase">
                          {userProfile.plan}
                        </span>
                      </div>
                      <span className="font-mono text-[10px] text-gray-500 truncate">
                        {userProfile.email}
                      </span>
                    </div>

                    {/* Menu Items */}
                    <Link
                      href="/setting"
                      onNavigate={(event) => { if (!confirmLeave('/setting')) event.preventDefault(); }}
                      onClick={() => setUserMenuOpen(false)}
                      className="w-full px-3 py-2 bg-white hover:bg-surface-card text-left font-space text-xs font-bold rounded-xl border border-black/80 flex items-center gap-2 cursor-pointer shadow-neo-sm transition-all"
                    >
                      <Settings className="w-3.5 h-3.5 text-black" />
                      <span>{t('nav.settings')}</span>
                    </Link>

                    {/* Admin Console Shortcut */}
                    <Link
                      href="/admin"
                      onNavigate={(event) => { if (!confirmLeave('/admin')) event.preventDefault(); }}
                      onClick={() => setUserMenuOpen(false)}
                      className="w-full px-3 py-2 bg-paper-warm hover:bg-white text-left font-space text-xs font-extrabold rounded-xl border border-black flex items-center gap-2 cursor-pointer shadow-neo-sm transition-all"
                    >
                      <Shield className="w-3.5 h-3.5 text-black stroke-[2.3]" />
                      <span>{t('nav.admin')}</span>
                    </Link>

                    {/* Language Switcher */}
                    <div className="p-2 bg-white rounded-xl border border-black/80 flex flex-col gap-1.5">
                      <span className="font-space text-[10px] font-bold text-gray-500 uppercase flex items-center gap-1">
                        <Globe className="w-3 h-3" /> {t('common.language')}:
                      </span>
                      <div className="grid grid-cols-2 gap-1 text-[11px] font-space font-bold">
                        <button
                          type="button"
                          onClick={() => handleToggleLanguage('vi')}
                          className={`px-2 py-1 rounded-lg border transition-all cursor-pointer flex items-center justify-center gap-1 ${
                            userProfile.language === 'vi'
                              ? 'bg-primary-container text-black border-black font-extrabold shadow-sm'
                              : 'bg-paper-warm text-gray-600 border-transparent hover:border-black'
                          }`}
                        >
                          <span>Tiếng Việt</span>
                          {userProfile.language === 'vi' && <Check className="w-3 h-3 stroke-[3]" />}
                        </button>
                        <button
                          type="button"
                          onClick={() => handleToggleLanguage('en')}
                          className={`px-2 py-1 rounded-lg border transition-all cursor-pointer flex items-center justify-center gap-1 ${
                            userProfile.language === 'en'
                              ? 'bg-primary-container text-black border-black font-extrabold shadow-sm'
                              : 'bg-paper-warm text-gray-600 border-transparent hover:border-black'
                          }`}
                        >
                          <span>English</span>
                          {userProfile.language === 'en' && <Check className="w-3 h-3 stroke-[3]" />}
                        </button>
                      </div>
                    </div>

                    <div className="h-[1px] bg-black/20 my-0.5" />

                    {/* Logout Button */}
                    <button
                      type="button"
                      onClick={handleLogout}
                      className="w-full px-3 py-2 bg-red-100 hover:bg-red-200 text-red-800 text-left font-space text-xs font-bold rounded-xl border border-red-400 flex items-center gap-2 cursor-pointer transition-all"
                    >
                      <LogOut className="w-3.5 h-3.5 text-red-700" />
                      <span>{t('nav.logout')}</span>
                    </button>
                  </div>
                </>
              )}
            </div>

            {/* Mobile Menu Toggle */}
            <button
              onClick={() => setMobileOpen(!mobileOpen)}
              className="lg:hidden p-2 rounded-lg border-neo-sm bg-white shadow-neo-sm text-on-surface flex items-center justify-center cursor-pointer"
              aria-label="Menu"
            >
              {mobileOpen ? (
                <X className="w-5 h-5 stroke-[2.5]" />
              ) : (
                <Menu className="w-5 h-5 stroke-[2.5]" />
              )}
            </button>
          </div>
        </div>

        {/* Mobile Drawer Dropdown */}
        {mobileOpen && (
          <div className="lg:hidden bg-surface-card border-b-2 border-on-background px-4 py-4 flex flex-col gap-2 animate-in fade-in slide-in-from-top-2">
            {/* Mobile User Profile Header */}
            <div className="flex items-center justify-between pb-3 mb-1 border-b border-border-soft">
              <div className="flex items-center gap-3">
                <div className="relative w-10 h-10 rounded-2xl border-2 border-black shadow-[2px_2px_0px_#111111] overflow-hidden bg-paper-warm">
                  <Image
                    src={userProfile.avatarUrl || '/avatar.png'}
                    alt="User Avatar"
                    width={40}
                    height={40}
                    className="w-full h-full object-cover"
                  />
                </div>
                <div>
                  <span className="font-space font-extrabold text-sm text-on-surface block">
                    {penName}
                  </span>
                  <span className="font-mono text-[10px] text-gray-500 truncate block">
                    {userProfile.email}
                  </span>
                </div>
              </div>
              <div className="inline-flex items-center gap-1 bg-paper-warm px-2.5 py-1 rounded-lg border border-black shadow-[1px_1px_0px_#111111]">
                <Flame className="w-3.5 h-3.5 text-orange-500 fill-orange-500 shrink-0" />
                <span className="font-space font-bold text-xs text-on-surface">
                  {streakCount} {t('nav.streak')}
                </span>
              </div>
            </div>

            {/* Nav Items */}
            {navItems.map((item) => {
              const active = isActive(item.path);
              const IconComp = item.icon;
              return (
                <Link
                  key={item.path}
                  href={item.path}
                  onNavigate={(event) => { if (!confirmLeave(item.path)) event.preventDefault(); }}
                  onClick={() => setMobileOpen(false)}
                  className={`px-4 py-2.5 font-space text-sm font-bold rounded-xl border-neo-sm transition-all flex items-center gap-2.5 ${
                    active
                      ? 'bg-primary-container text-black shadow-neo-sm'
                      : 'bg-paper-warm text-on-surface hover:bg-surface-container-high'
                  }`}
                >
                  <IconComp className="w-4 h-4" />
                  <span>{item.label}</span>
                </Link>
              );
            })}

            {/* Mobile Profile & Logout */}
            <div className="pt-2 border-t border-border-soft flex flex-col gap-2">
              <Link
                href="/setting"
                onNavigate={(event) => { if (!confirmLeave('/setting')) event.preventDefault(); }}
                onClick={() => setMobileOpen(false)}
                className="w-full px-4 py-2 bg-white text-black font-space text-xs font-bold rounded-xl border border-black flex items-center justify-center gap-2 cursor-pointer shadow-neo-sm"
              >
                <Settings className="w-3.5 h-3.5" />
                <span>{t('nav.settings')}</span>
              </Link>

              <button
                type="button"
                onClick={handleLogout}
                className="w-full px-4 py-2 bg-red-100 text-red-800 font-space text-xs font-bold rounded-xl border border-red-400 flex items-center justify-center gap-2 cursor-pointer"
              >
                <LogOut className="w-3.5 h-3.5 text-red-700" />
                <span>{t('nav.logout')}</span>
              </button>
            </div>
          </div>
        )}
      </header>

      {/* Mobile Bottom Navigation Bar */}
      <div className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-surface-card border-t-2 border-black px-2 py-2 flex items-center justify-around shadow-neo">
        {navItems.map((item) => {
          const IconComp = item.icon;
          const isActive = pathname === item.path;

          return (
            <Link
              key={item.path}
              href={item.path}
              onNavigate={(event) => { if (!confirmLeave(item.path)) event.preventDefault(); }}
              className={`flex flex-col items-center gap-1 p-2 rounded-xl transition-all ${
                isActive
                  ? 'bg-primary-container text-black border border-black shadow-neo-xs font-extrabold'
                  : 'text-gray-600 hover:text-black'
              }`}
            >
              <IconComp className="w-4 h-4" />
              <span className="text-[10px] font-space">
                {item.label}
              </span>
            </Link>
          );
        })}
      </div>
      {pendingAction && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/55 px-4">
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="unsaved-journal-title"
            aria-describedby="unsaved-journal-description"
            onKeyDown={(event) => { if (event.key === 'Escape') setPendingAction(null); }}
            className="w-full max-w-md rounded-3xl border-neo bg-surface-card p-6 shadow-neo-lg"
          >
            <h2 id="unsaved-journal-title" className="font-space text-xl font-extrabold text-on-surface">{t('nav.unsavedTitle')}</h2>
            <p id="unsaved-journal-description" className="mt-3 text-sm leading-relaxed text-on-surface-variant">
              {t('nav.unsavedDescription')}
            </p>
            <div className="mt-6 flex flex-wrap justify-end gap-3">
              <button type="button" autoFocus onClick={() => setPendingAction(null)} className="rounded-xl border-neo bg-white px-5 py-2 font-space font-bold shadow-neo-sm">{t('nav.stay')}</button>
              <button type="button" onClick={continuePendingAction} className="rounded-xl border-neo bg-primary-container px-5 py-2 font-space font-bold shadow-neo-sm">
                {pendingAction.kind === 'logout' ? t('nav.logout') : t('nav.leave')}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
