import type { Report } from '../api/reports';

function csvCell(value: unknown): string {
  const text = value === null || value === undefined ? '' : typeof value === 'object' ? JSON.stringify(value) : String(value);
  const safe = text.replace(/[\r\n]+/g, ' ');
  const protectedValue = /^\s*[=+\-@]/.test(safe) ? `'${safe}` : safe;
  return `"${protectedValue.replace(/"/g, '""')}"`;
}

export function downloadReportCsv(report: Report): void {
  const rows: unknown[][] = [
    ['Trường', 'Giá trị'],
    ['Loại báo cáo', report.type],
    ['Từ ngày', report.from],
    ['Đến ngày', report.to],
    ['Múi giờ', report.timezone],
    ['Cỡ mẫu', report.sampleSize],
    ['Nội dung', report.narrative],
    ...Object.entries(report.metrics).map(([key, value]) => [key, value]),
    [],
    ['Ngày', 'Nguồn', 'Chỉ số', 'Giá trị'],
    ...report.evidence.map((item) => [item.date, item.source, item.metric, item.value]),
  ];
  const blob = new Blob(['\uFEFF', rows.map((row) => row.map(csvCell).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `mylog-bao-cao-tuan-${report.from}-${report.to}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
