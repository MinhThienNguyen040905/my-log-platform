'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { usePathname, useRouter } from 'next/navigation';
import { useJournal } from '@/lib/journal-context';
import { useToast } from '@/lib/toast-context';
import { ProfileModal } from '@/components/ui/ProfileModal';
import {
  Flame,
  Menu,
  X,
  LayoutDashboard,
  PenTool,
  Calendar,
  BarChart3,
  ArrowRight,
  User,
  LogOut,
  Globe,
  Settings,
  Check,
  ChevronDown,
} from 'lucide-react';

export function Navbar() {
  const pathname = usePathname();
  const router = useRouter();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [profileModalOpen, setProfileModalOpen] = useState(false);

  const { streakCount, userProfile, updateProfile, logout } = useJournal();
  const { showToast } = useToast();

  const navItems = [
    { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
    { label: 'Journal Editor', path: '/journal-editor', icon: PenTool },
    { label: 'History & Calendar', path: '/history-calendar', icon: Calendar },
    { label: 'Insights & AI Reports', path: '/insight', icon: BarChart3 },
  ];

  const isActive = (itemPath: string) => {
    return pathname === itemPath || (itemPath !== '/' && pathname.startsWith(itemPath));
  };

  const handleLogout = () => {
    setUserMenuOpen(false);
    setMobileOpen(false);
    logout();
    showToast({
      title: 'Đã đăng xuất tài khoản',
      message: 'Hẹn gặp lại bạn trong trang nhật ký tiếp theo.',
      type: 'info',
    });
    router.push('/auth/login');
  };

  const handleToggleLanguage = (lang: 'vi' | 'en') => {
    updateProfile({ language: lang });
    showToast({
      title: lang === 'vi' ? 'Ngôn ngữ: Tiếng Việt' : 'Language: English',
      type: 'info',
    });
  };

  return (
    <>
      {/* Desktop & Top Sticky Header */}
      <header className="fixed top-0 left-0 right-0 w-full z-40 bg-bg-canvas/95 backdrop-blur-md border-b-2 border-on-background">
        <div className="h-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-3 sm:gap-4">
          {/* Brand Logo Container */}
          <Link
            href="/dashboard"
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
            {/* CTA Button */}
            <Link
              href="/journal-editor"
              className="hidden sm:inline-flex items-center justify-center bg-primary-container text-black font-space text-xs sm:text-sm font-extrabold px-3 sm:px-4 py-2 border-2 border-black rounded-xl shadow-[3px_3px_0px_#111111] hover:bg-[#a5f01e] hover:-translate-y-0.5 active:translate-y-0.5 active:shadow-[1px_1px_0px_#111111] transition-all shrink-0 gap-1.5"
            >
              <span>Viết nhật ký hôm nay</span>
              <ArrowRight className="w-4 h-4" />
            </Link>

            {/* Vertical Divider */}
            <div className="h-8 w-[1.5px] bg-border-soft hidden sm:block shrink-0" />

            {/* User Profile Block & Avatar with Dropdown */}
            <div className="relative">
              <button
                type="button"
                onClick={() => setUserMenuOpen(!userMenuOpen)}
                className="flex items-center gap-2 sm:gap-2.5 shrink-0 text-left cursor-pointer p-1 rounded-2xl hover:bg-paper-warm transition-colors"
                title="Tùy chọn tài khoản"
              >
                <div className="hidden sm:flex flex-col items-end leading-none">
                  <span className="font-space font-extrabold text-sm text-on-surface tracking-tight flex items-center gap-1">
                    {userProfile.penName}
                    <ChevronDown className="w-3 h-3 text-gray-500" />
                  </span>
                  <div className="mt-1 inline-flex items-center gap-1 bg-paper-warm px-2 py-0.5 rounded border border-black/80 shadow-[1px_1px_0px_#111111]">
                    <Flame className="w-3.5 h-3.5 text-orange-500 fill-orange-500 shrink-0" />
                    <span className="font-space font-bold text-[11px] text-on-surface tracking-tight">
                      {streakCount} ngày streak
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
                          {userProfile.penName}
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
                    <button
                      type="button"
                      onClick={() => {
                        setUserMenuOpen(false);
                        setProfileModalOpen(true);
                      }}
                      className="w-full px-3 py-2 bg-white hover:bg-surface-card text-left font-space text-xs font-bold rounded-xl border border-black/80 flex items-center gap-2 cursor-pointer shadow-neo-sm transition-all"
                    >
                      <Settings className="w-3.5 h-3.5 text-black" />
                      <span>Hồ sơ & Thiết lập</span>
                    </button>

                    {/* Language Switcher */}
                    <div className="p-2 bg-white rounded-xl border border-black/80 flex flex-col gap-1.5">
                      <span className="font-space text-[10px] font-bold text-gray-500 uppercase flex items-center gap-1">
                        <Globe className="w-3 h-3" /> Ngôn ngữ:
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
                      <span>Đăng xuất (FR-ACCOUNT-03)</span>
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
                    {userProfile.penName}
                  </span>
                  <span className="font-mono text-[10px] text-gray-500 truncate block">
                    {userProfile.email}
                  </span>
                </div>
              </div>
              <div className="inline-flex items-center gap-1 bg-paper-warm px-2.5 py-1 rounded-lg border border-black shadow-[1px_1px_0px_#111111]">
                <Flame className="w-3.5 h-3.5 text-orange-500 fill-orange-500 shrink-0" />
                <span className="font-space font-bold text-xs text-on-surface">
                  {streakCount} ngày
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
              <button
                type="button"
                onClick={() => {
                  setMobileOpen(false);
                  setProfileModalOpen(true);
                }}
                className="w-full px-4 py-2 bg-white text-black font-space text-xs font-bold rounded-xl border border-black flex items-center justify-center gap-2 cursor-pointer shadow-neo-sm"
              >
                <Settings className="w-3.5 h-3.5" />
                <span>Hồ sơ cá nhân & Cài đặt</span>
              </button>

              <button
                type="button"
                onClick={handleLogout}
                className="w-full px-4 py-2 bg-red-100 text-red-800 font-space text-xs font-bold rounded-xl border border-red-400 flex items-center justify-center gap-2 cursor-pointer"
              >
                <LogOut className="w-3.5 h-3.5 text-red-700" />
                <span>Đăng xuất (Logout)</span>
              </button>
            </div>
          </div>
        )}
      </header>

      {/* MOBILE BOTTOM NAVIGATION BAR (THUMB-FRIENDLY, STITCH COMPLIANT) */}
      <div className="lg:hidden fixed bottom-0 left-0 right-0 z-40 bg-surface-card/95 backdrop-blur-md border-t-2 border-black px-3 py-2 flex items-center justify-around shadow-[0px_-4px_10px_rgba(0,0,0,0.06)]">
        {navItems.map((item) => {
          const active = isActive(item.path);
          const IconComp = item.icon;
          const isEditor = item.path === '/journal-editor';

          if (isEditor) {
            return (
              <Link
                key={item.path}
                href={item.path}
                className="relative -top-3 flex flex-col items-center group"
              >
                <div className="w-12 h-12 rounded-2xl bg-primary-container border-2 border-black shadow-[3px_3px_0px_#111] flex items-center justify-center text-black group-hover:scale-105 transition-transform">
                  <IconComp className="w-5 h-5 stroke-[2.5]" />
                </div>
                <span className="font-space text-[10px] font-extrabold text-black mt-0.5">
                  Viết sổ
                </span>
              </Link>
            );
          }

          return (
            <Link
              key={item.path}
              href={item.path}
              className={`flex flex-col items-center gap-0.5 py-1 px-2.5 rounded-xl transition-all ${
                active ? 'text-black font-extrabold' : 'text-gray-500 hover:text-black font-semibold'
              }`}
            >
              <div
                className={`p-1 rounded-lg ${
                  active ? 'bg-paper-warm border border-black shadow-[1px_1px_0px_#111]' : ''
                }`}
              >
                <IconComp className="w-5 h-5 stroke-[2.2]" />
              </div>
              <span className="font-space text-[10px] tracking-tight truncate max-w-[70px]">
                {item.label === 'History & Calendar'
                  ? 'Lịch & Ký ức'
                  : item.label === 'Insights & AI Reports'
                  ? 'Báo cáo AI'
                  : item.label}
              </span>
            </Link>
          );
        })}
      </div>

      {/* User Profile Modal */}
      <ProfileModal
        isOpen={profileModalOpen}
        onClose={() => setProfileModalOpen(false)}
      />
    </>
  );
}
