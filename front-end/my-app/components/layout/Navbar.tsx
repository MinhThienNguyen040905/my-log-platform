'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

export function Navbar() {
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);

  const navItems = [
    { label: 'Dashboard', path: '/' },
    { label: 'Journal Editor', path: '/journal-editor' },
    { label: 'History & Calendar', path: '/history-calendar' },
    { label: 'Insights & AI Reports', path: '/insights-ai-reports' },
  ];

  const isActive = (itemPath: string) => {
    if (itemPath === '/') return pathname === '/' || pathname === '/dashboard';
    return pathname.startsWith(itemPath);
  };

  return (
    <header className="fixed top-0 left-0 right-0 w-full z-50 bg-bg-canvas/95 backdrop-blur-md border-b-2 border-on-background">
      <div className="h-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        {/* Brand Logo */}
        <Link href="/" className="flex items-center gap-3 shrink-0 group">
          <div className="w-10 h-10 rounded-xl bg-primary-container border-neo shadow-neo-sm flex items-center justify-center font-space font-extrabold text-xl group-hover:rotate-3 transition-transform">
            📔
          </div>
          <span className="px-2.5 py-1 bg-primary-container text-on-primary-container font-space text-lg font-bold border-neo-sm rounded-lg shadow-neo-sm tracking-tight">
            MY LOG
          </span>
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

        {/* CTA Button & Mobile Toggle */}
        <div className="flex items-center gap-3">
          <Link
            href="/journal-editor"
            className="inline-flex items-center gap-1.5 bg-paper-warm text-on-surface font-space text-xs sm:text-sm font-bold px-3.5 py-2 border-neo-sm rounded-xl shadow-neo-sm hover:bg-primary-container hover:-translate-y-0.5 transition-all"
          >
            <span className="material-symbols-outlined text-base">add_circle</span>
            <span className="hidden sm:inline">Viết nhật ký</span>
          </Link>

          <button
            onClick={() => setMobileOpen(!mobileOpen)}
            className="lg:hidden p-2 rounded-lg border-neo-sm bg-white shadow-neo-sm text-on-surface"
            aria-label="Menu"
          >
            <span className="material-symbols-outlined text-xl">
              {mobileOpen ? 'close' : 'menu'}
            </span>
          </button>
        </div>
      </div>

      {/* Mobile Drawer */}
      {mobileOpen && (
        <div className="lg:hidden bg-surface-card border-b-2 border-on-background px-4 py-4 flex flex-col gap-2">
          {navItems.map((item) => {
            const active = isActive(item.path);
            return (
              <Link
                key={item.path}
                href={item.path}
                onClick={() => setMobileOpen(false)}
                className={`px-4 py-2.5 font-space text-sm font-bold rounded-xl border-neo-sm transition-all ${
                  active
                    ? 'bg-primary-container text-on-primary-container shadow-neo-sm'
                    : 'bg-paper-warm text-on-surface hover:bg-surface-container-high'
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </div>
      )}
    </header>
  );
}
