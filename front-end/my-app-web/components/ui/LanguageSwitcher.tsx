'use client';

import { useAppLanguage } from '@/app/_components/AppLanguageProvider';

export function LanguageSwitcher() {
  const { locale, setLocale } = useAppLanguage();
  return <div className="inline-flex rounded-xl border border-black bg-white p-1 text-xs font-bold" role="group" aria-label="Language">
    <button type="button" onClick={() => void setLocale('vi')} aria-pressed={locale === 'vi'} className={`rounded-lg px-2 py-1 ${locale === 'vi' ? 'bg-primary-container' : ''}`}>VI</button>
    <button type="button" onClick={() => void setLocale('en')} aria-pressed={locale === 'en'} className={`rounded-lg px-2 py-1 ${locale === 'en' ? 'bg-primary-container' : ''}`}>EN</button>
  </div>;
}
