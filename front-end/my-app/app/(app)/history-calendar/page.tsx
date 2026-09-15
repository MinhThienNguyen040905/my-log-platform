import { CalendarView } from '@/features/calendar';

export const metadata = {
  title: 'Lịch ký ức - MyLog',
  description: 'Nhìn lại dòng chảy cảm xúc và những trang nhật ký đã qua theo dòng thời gian.',
};

export default function HistoryCalendarPage() {
  return <CalendarView />;
}
