import { OnboardingWizard } from '@/features/onboarding';

export const metadata = {
  title: 'Thiết lập sổ tay - MyLog',
  description: 'Cá nhân hóa không gian ghi chép và nhịp điệu đồng hành của bạn.',
};

export default function OnboardingPage() {
  return <OnboardingWizard />;
}
