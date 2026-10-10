import { WeeklyReportPrintView } from '@/features/reporting';

export default async function WeeklyReportPrintPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <WeeklyReportPrintView id={id} />;
}
