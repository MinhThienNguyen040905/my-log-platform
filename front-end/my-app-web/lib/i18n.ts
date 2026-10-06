import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

export const resources = {
  vi: {
    translation: {
      appName: 'MyLog Platform',
      nav: {
        dashboard: 'Dashboard',
        journalEditor: 'Journal Editor',
        historyCalendar: 'History & Calendar',
        insights: 'Insights & AI Reports',
        writeToday: 'Viết nhật ký hôm nay',
      },
      account: {
        profile: 'Hồ sơ cá nhân & Cài đặt',
        logout: 'Đăng xuất',
        streak: 'ngày streak',
      },
    },
  },
  en: {
    translation: {
      appName: 'MyLog Platform',
      nav: {
        dashboard: 'Dashboard',
        journalEditor: 'Journal Editor',
        historyCalendar: 'History & Calendar',
        insights: 'Insights & AI Reports',
        writeToday: "Write Today's Journal",
      },
      account: {
        profile: 'Profile & Settings',
        logout: 'Log Out',
        streak: 'day streak',
      },
    },
  },
} as const;

if (!i18n.isInitialized) {
  i18n.use(initReactI18next).init({
    resources,
    lng: 'vi',
    fallbackLng: 'vi',
    interpolation: {
      escapeValue: false,
    },
  });
}

export default i18n;

