import { SettingsView } from '@/features/user';

export const metadata = {
  title: 'Hồ sơ & Thiết lập - MyLog',
  description: 'Tùy chỉnh thông tin tác giả, đổi mật khẩu và cài đặt không gian nhật ký cá nhân.',
};

export default function SettingPage() {
  return <SettingsView />;
}

