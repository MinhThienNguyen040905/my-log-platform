import { OnboardingWizard } from '@/features/onboarding';
import { AuthGate } from '@/app/_components/AuthGate';
import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';

export const metadata = {
  title: 'Thiết lập sổ tay - MyLog',
  description: 'Cá nhân hóa không gian ghi chép và nhịp điệu đồng hành của bạn.',
};

export default async function OnboardingPage() {
  if (!(await cookies()).has('mylog_refresh')) redirect('/auth/login');
  return <AuthGate showNavbar={false}><OnboardingWizard /></AuthGate>;
}
