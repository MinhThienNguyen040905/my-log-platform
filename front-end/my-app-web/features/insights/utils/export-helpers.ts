import { JournalEntry, UserProfile } from '@/types';

export interface WeeklyReportExportData {
  userProfile?: UserProfile;
  streakCount: number;
  entries: JournalEntry[];
  reportWeek: string;
  reportStatus: string;
  metrics: {
    avgMood: number;
    avgStress: number;
    avgSleep: number;
  };
  highlights: {
    milestone: string;
    pattern: string;
    advice: string;
  };
}

/**
 * Xuất dữ liệu Báo cáo tuần ra tệp CSV (kèm BOM UTF-8 cho Microsoft Excel & Google Sheets)
 */
export function exportWeeklyReportCSV(data: WeeklyReportExportData) {
  const authorName = data.userProfile?.penName || data.userProfile?.name || 'Tác giả MyLog';
  const now = new Date().toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  const lines: string[] = [];

  // Tiêu đề & Metadata
  lines.push(`"MYLOG - BÁO CÁO TỔNG KẾT TUẦN (${data.reportWeek})"\n`);
  lines.push(`"Thời gian xuất","${now}"`);
  lines.push(`"Tác giả","${authorName}"`);
  lines.push(`"Trạng thái Snapshot","${data.reportStatus}"`);
  lines.push(`"Tổng số bài viết trong kỳ","${data.entries.length} trang"\n`);

  // Bảng chỉ số tổng hợp
  lines.push(`"--- CHỈ SỐ TỔNG HỢP TUẦN ---"`);
  lines.push(`"Chỉ số","Giá trị","So với tuần trước","Đánh giá"`);
  lines.push(`"Tâm trạng trung bình","${data.metrics.avgMood}/10","+0.8","Rất Tích Cực"`);
  lines.push(`"Áp lực trung bình","${data.metrics.avgStress}/10","-1.2","Giảm rõ rệt"`);
  lines.push(`"Chuỗi ngày liên tục (Streak)","${data.streakCount} ngày","100% duy trì","Sổ Tay Vàng"`);
  lines.push(`"Giấc ngủ trung bình","${data.metrics.avgSleep}h/đêm","+0.4h","Đủ giấc phục hồi"\n`);

  // Bảng chi tiết từng bài viết / ngày
  lines.push(`"--- CHI TIẾT TRANG NHẬT KÝ ĐÃ GHI NHẬN ---"`);
  lines.push(`"Ngày","Giờ","Tiêu đề","Tâm trạng","Điểm tâm trạng","Điểm áp lực","Giờ ngủ","Chủ đề / Tags"`);

  if (data.entries.length > 0) {
    data.entries.forEach((e) => {
      const tagsStr = (e.tags || []).join('; ');
      const cleanTitle = e.title.replace(/"/g, '""');
      lines.push(
        `"${e.date}","${e.time || '--:--'}","${cleanTitle}","${e.mood}","${e.moodScore}","${e.stressScore}","${e.sleepHours}h","${tagsStr}"`
      );
    });
  } else {
    lines.push(`"Chưa có bài viết nào được ghi nhận trong kỳ"`);
  }
  lines.push('');

  // 3 Điểm nhấn nổi bật
  lines.push(`"--- 3 ĐIỂM NHẤN TRỌNG TÂM CỦA TUẦN (AI SYNTHESIS) ---"`);
  lines.push(`"1. Cột mốc nổi bật","${data.highlights.milestone.replace(/"/g, '""')}"`);
  lines.push(`"2. Mẫu hình tâm trạng","${data.highlights.pattern.replace(/"/g, '""')}"`);
  lines.push(`"3. Lời khuyên tuần mới","${data.highlights.advice.replace(/"/g, '""')}"`);

  // Tạo Blob UTF-8 BOM
  const csvContent = '\uFEFF' + lines.join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `MyLog_Bao_Cao_Tuan_${data.reportWeek.replace(/\s+/g, '_')}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

/**
 * Kích hoạt hộp thoại in / xuất file PDF của trình duyệt
 */
export function exportWeeklyReportPDF() {
  if (typeof window !== 'undefined') {
    window.print();
  }
}

