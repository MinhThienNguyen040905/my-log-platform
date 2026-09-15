import { DashboardView } from '@/features/dashboard';

export const metadata = {
  title: 'Bảng điều khiển - MyLog',
  description: 'Tổng quan nhịp điệu cảm xúc, chuỗi ngày viết và các chỉ số sức khỏe tinh thần của bạn.',
};

export default function DashboardPage() {
  return <DashboardView />;
}
