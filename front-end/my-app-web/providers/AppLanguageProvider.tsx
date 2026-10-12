'use client';

import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { createInstance } from 'i18next';
import { I18nextProvider, initReactI18next } from 'react-i18next';
import { resources, type AppLocale } from '@/lib/i18n';
import { MotionConfig } from 'motion/react';

function createAppI18n(locale: AppLocale) {
  const instance = createInstance();
  void instance.use(initReactI18next).init({
    resources,
    lng: locale,
    fallbackLng: 'vi',
    supportedLngs: ['vi', 'en'],
    interpolation: { escapeValue: false },
    initAsync: false,
  });
  return instance;
}

type LanguageContextValue = { locale: AppLocale; setLocale: (locale: AppLocale) => Promise<void> };
const LanguageContext = createContext<LanguageContextValue | null>(null);

export function AppLanguageProvider({ initialLocale, children }: { initialLocale: AppLocale; children: React.ReactNode }) {
  const router = useRouter();
  const [instance] = useState(() => createAppI18n(initialLocale));
  const [locale, updateLocale] = useState<AppLocale>(initialLocale);

  const setLocale = useCallback(async (next: AppLocale) => {
    if (locale === next && instance.language === next) return;
    if (instance.language !== next) await instance.changeLanguage(next);
    updateLocale(next);
    document.documentElement.lang = next;
    document.cookie = `mylog_locale=${next}; Path=/; Max-Age=31536000; SameSite=Lax`;
    router.refresh();
  }, [instance, locale, router]);

  const value = useMemo(() => ({ locale, setLocale }), [locale, setLocale]);
  return <LanguageContext.Provider value={value}><I18nextProvider i18n={instance}><MotionConfig reducedMotion="user">{children}</MotionConfig></I18nextProvider></LanguageContext.Provider>;
}

export function useAppLanguage() {
  const context = useContext(LanguageContext);
  if (!context) throw new Error('useAppLanguage must be used within AppLanguageProvider');
  return context;
}
