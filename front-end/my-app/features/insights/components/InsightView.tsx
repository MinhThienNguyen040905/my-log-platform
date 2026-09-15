'use client';

import React, { useState, useMemo } from 'react';
import Link from 'next/link';
import { useJournal } from '@/lib/journal-context';
import { useToast } from '@/lib/toast-context';
import { WashiTape, StickerBadge } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import {
  Sparkles,
  CheckSquare,
  ShieldCheck,
  Sprout,
  BarChart3,
  Calendar,
  Layers,
  Lightbulb,
  FileText,
  ThumbsUp,
  ThumbsDown,
  Clock,
  Compass,
  CheckCircle2,
  AlertCircle,
  TrendingUp,
  Heart,
  Moon,
  Zap,
  Flame,
} from 'lucide-react';

export function InsightView() {
  const { goals, toggleGoal, streakCount, entries } = useJournal();
  const { showToast } = useToast();

  const [activeTab, setActiveTab] = useState<'weekly-report' | 'evidence-matrix'>('weekly-report');
  const [reportStatus, setReportStatus] = useState<'VALID' | 'STALE'>('VALID');
  const [feedbackState, setFeedbackState] = useState<{ [key: string]: 'HELPFUL' | 'NOT_HELPFUL' }>({});
  const [simulateLowData, setSimulateLowData] = useState(false);

  // [FR-INSIGHT-01] & [BR-08]: Minimum 3 distinct days required for insight engine
  const distinctDays = useMemo(() => {
    return new Set(entries.map((e) => e.date)).size;
  }, [entries]);

  const effectiveDistinctDays = simulateLowData ? 2 : distinctDays;

  const handleRateInsight = (id: string, val: 'HELPFUL' | 'NOT_HELPFUL') => {
    setFeedbackState((prev) => ({ ...prev, [id]: val }));
    showToast({
      title: val === 'HELPFUL' ? 'Đã ghi nhận hữu ích!' : 'Đã ghi nhận góp ý!',
      message: 'Hệ thống Statistical Engine sẽ tinh chỉnh độ ưu tiên cho các kỳ phân tích tiếp theo.',
      type: 'info',
    });
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col gap-8 selection:bg-primary-container selection:text-black">
      {/* Top Header & Tab Switcher */}
      <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b-2 border-on-background">
        <div>
          <div className="inline-flex items-center gap-2 mb-1">
            <span className="px-2.5 py-0.5 bg-paper-warm border border-black rounded text-[11px] font-space font-extrabold uppercase">
              FR-REPORT & FR-INSIGHT
            </span>
            <span className="text-xs font-space text-gray-500 font-bold">Kỳ Phân Tích: Tuần 42 / 2026</span>
          </div>
          <h1 className="font-space text-2xl sm:text-4xl font-extrabold text-on-surface">
            Báo Cáo Định Kỳ & Ma Trận Bằng Chứng AI
          </h1>
          <p className="font-sans text-xs sm:text-sm text-on-surface-variant">
            Tổng hợp dữ liệu định lượng, bằng chứng correlation và thói quen nuôi dưỡng tinh thần độc bản
          </p>
        </div>

        {/* Action Controls & Tab Navigation Switcher */}
        <div className="flex flex-wrap items-center gap-2">
          {/* BR-08 Demo Simulation Toggle */}
          <button
            type="button"
            onClick={() => {
              setSimulateLowData(!simulateLowData);
              showToast({
                title: !simulateLowData ? 'Đã BẬT mô phỏng < 3 ngày' : 'Đã TẮT mô phỏng',
                message: !simulateLowData ? 'Hiển thị Empty State theo BR-08 / FR-INSIGHT-01.' : 'Trở về dữ liệu thực tế.',
                type: 'info',
              });
            }}
            className={`px-3 py-1.5 rounded-xl border border-black font-space text-xs font-bold transition-all shadow-neo-sm cursor-pointer ${
              simulateLowData ? 'bg-red-300 text-red-900 border-red-700 font-extrabold' : 'bg-white text-gray-700 hover:bg-paper-warm'
            }`}
            title="Bật/tắt mô phỏng tài khoản chưa đủ 3 ngày ghi nhận để kiểm tra BR-08"
          >
            Test Ngưỡng &lt; 3 ngày (BR-08): {simulateLowData ? 'BẬT' : 'TẮT'}
          </button>

          <div className="flex items-center gap-1.5 p-1.5 bg-surface-card rounded-2xl border-neo shadow-neo-sm">
            <button
              type="button"
              onClick={() => setActiveTab('weekly-report')}
              className={`px-4 py-2 rounded-xl font-space text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
                activeTab === 'weekly-report'
                  ? 'bg-primary-container text-black border border-black shadow-neo-sm font-extrabold'
                  : 'text-gray-600 hover:text-black hover:bg-paper-warm'
              }`}
            >
              <FileText className="w-3.5 h-3.5" />
              Báo Cáo Tuần (Weekly Snapshot)
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('evidence-matrix')}
              className={`px-4 py-2 rounded-xl font-space text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
                activeTab === 'evidence-matrix'
                  ? 'bg-primary-container text-black border border-black shadow-neo-sm font-extrabold'
                  : 'text-gray-600 hover:text-black hover:bg-paper-warm'
              }`}
            >
              <Layers className="w-3.5 h-3.5" />
              Ma Trận Bằng Chứng & Thói Quen
            </button>
          </div>
        </div>
      </div>

      {/* [BR-08] & [FR-INSIGHT-01]: Insufficient Data Empty State */}
      {effectiveDistinctDays < 3 ? (
        <div className="bg-surface-card border-neo rounded-3xl p-8 sm:p-12 shadow-neo relative flex flex-col items-center text-center gap-6 max-w-3xl mx-auto my-6">
          <WashiTape color="lime" rotate={-2} className="absolute -top-3 left-12 w-36" />
          <div className="w-20 h-20 rounded-3xl bg-paper-warm border-2 border-black flex items-center justify-center shadow-neo">
            <Sprout className="w-10 h-10 text-green-700 stroke-[2.3]" />
          </div>

          <div className="flex flex-col gap-2">
            <span className="px-3 py-1 bg-amber-200 text-black border border-black rounded-lg font-space text-xs font-extrabold uppercase tracking-wide self-center shadow-neo-sm">
              BR-08 / FR-INSIGHT-01: Ngưỡng Dữ Liệu Tối Thiểu
            </span>
            <h2 className="font-space text-2xl sm:text-3xl font-extrabold text-on-surface">
              Cần tối thiểu 3 ngày ghi nhận để kích hoạt Insight
            </h2>
            <p className="font-sans text-sm text-gray-700 max-w-lg leading-relaxed">
              Mô hình Statistical &amp; Correlation Engine của MyLog cần quan sát tối thiểu <strong>3 ngày trải nghiệm khác nhau</strong> để phát hiện xu hướng cảm xúc, nguyên nhân gây stress và đề xuất thói quen chuẩn xác nhất cho bạn.
            </p>
          </div>

          {/* 3-day Progress visualization */}
          <div className="w-full max-w-md bg-paper-warm p-4 rounded-2xl border-2 border-black shadow-neo-sm flex flex-col gap-3">
            <div className="flex items-center justify-between text-xs font-space font-bold">
              <span>Tiến trình mở khóa:</span>
              <span className="font-extrabold font-mono text-sm">{effectiveDistinctDays}/3 Ngày ({Math.round((effectiveDistinctDays / 3) * 100)}%)</span>
            </div>
            <div className="w-full h-3.5 bg-white rounded-full border-2 border-black overflow-hidden p-0.5">
              <div
                style={{ width: `${Math.min(100, (effectiveDistinctDays / 3) * 100)}%` }}
                className="h-full bg-primary-container rounded-full transition-all duration-500 border border-black"
              />
            </div>

            <div className="grid grid-cols-3 gap-2 pt-1">
              {[1, 2, 3].map((d) => {
                const isDone = d <= effectiveDistinctDays;
                return (
                  <div
                    key={d}
                    className={`p-2 rounded-xl border flex flex-col items-center justify-center gap-1 text-xs font-space font-bold ${
                      isDone
                        ? 'bg-primary-container border-black text-black shadow-neo-sm'
                        : 'bg-white border-dashed border-gray-400 text-gray-400'
                    }`}
                  >
                    <span className="text-[10px] uppercase font-extrabold">Ngày {d}</span>
                    <span className="text-xs">{isDone ? '✓ Đã ghi' : '○ Còn thiếu'}</span>
                  </div>
                );
              })}
            </div>
          </div>

          <Link href="/journal-editor">
            <NeoButton size="md" className="font-space font-extrabold" icon={<Sparkles className="w-4 h-4" />}>
              Viết nhật ký để tiếp tục tiến trình
            </NeoButton>
          </Link>
        </div>
      ) : (
        <>

      {/* ========================================================== */}
      {/* TAB 1: WEEKLY SNAPSHOT REPORT (FR-REPORT-01..05)          */}
      {/* ========================================================== */}
      {activeTab === 'weekly-report' && (
        <div className="flex flex-col gap-8">
          {/* Status & Snapshot Banner */}
          <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative flex flex-wrap items-center justify-between gap-4">
            <WashiTape color="lime" rotate={-1} className="absolute -top-3 left-10 w-32" />

            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-paper-warm border-neo-sm flex items-center justify-center font-bold">
                <BarChart3 className="w-6 h-6 text-black" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-space text-lg font-extrabold text-black">
                    Báo Cáo Tổng Kết Tuần 42
                  </span>
                  <span
                    className={`px-2 py-0.5 rounded border border-black font-space text-[10px] font-extrabold uppercase ${
                      reportStatus === 'VALID' ? 'bg-primary-container text-black' : 'bg-amber-200 text-black'
                    }`}
                  >
                    Snapshot {reportStatus} (FR-REPORT-04)
                  </span>
                </div>
                <p className="font-sans text-xs text-gray-600 mt-0.5">
                  Tạo tự động vào Chủ Nhật lúc 23:59 • Tổng cộng <strong>{entries.length} trang nhật ký</strong> trong kỳ
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => {
                  setReportStatus(reportStatus === 'VALID' ? 'STALE' : 'VALID');
                  showToast({
                    title: `Chuyển trạng thái: Snapshot ${reportStatus === 'VALID' ? 'STALE' : 'VALID'}`,
                    message: 'Theo FR-REPORT-05, báo cáo là snapshot cố định và chuyển STALE khi dữ liệu nguồn thay đổi.',
                    type: 'info',
                  });
                }}
                className="text-xs font-space font-bold underline text-gray-600 hover:text-black cursor-pointer"
              >
                Giả lập thay đổi dữ liệu (Test STALE)
              </button>
            </div>
          </div>

          {/* Core Metrics Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-paper-warm border-neo-sm p-4 rounded-2xl shadow-neo-sm flex flex-col justify-between">
              <span className="font-space text-xs font-bold text-gray-600 uppercase">Tâm trạng trung bình tuần</span>
              <div className="flex items-baseline gap-2 mt-2">
                <span className="font-space text-3xl font-extrabold text-black">7.6</span>
                <span className="text-xs font-space font-bold text-green-700">▲ +0.8 so với tuần trước</span>
              </div>
              <span className="text-[11px] font-sans text-gray-500 mt-1">Đạt mức Rất Tích Cực</span>
            </div>

            <div className="bg-white border-neo-sm p-4 rounded-2xl shadow-neo-sm flex flex-col justify-between">
              <span className="font-space text-xs font-bold text-gray-600 uppercase">Áp lực trung bình</span>
              <div className="flex items-baseline gap-2 mt-2">
                <span className="font-space text-3xl font-extrabold text-mood-anxiety-stress">3.4</span>
                <span className="text-xs font-space font-bold text-mood-anxiety-stress">▼ -1.2 giảm rõ rệt</span>
              </div>
              <span className="text-[11px] font-sans text-gray-500 mt-1">Gỡ bỏ áp lực bảo vệ đề cương</span>
            </div>

            <div className="bg-white border-neo-sm p-4 rounded-2xl shadow-neo-sm flex flex-col justify-between">
              <span className="font-space text-xs font-bold text-gray-600 uppercase">Số ngày viết liên tục</span>
              <div className="flex items-baseline gap-2 mt-2">
                <span className="font-space text-3xl font-extrabold text-black">{streakCount} ngày</span>
                <span className="text-xs font-space font-bold text-amber-600 flex items-center gap-1">
                  <Flame className="w-3.5 h-3.5 text-amber-500 fill-amber-400 stroke-[2.3]" />
                  Sổ Tay Vàng
                </span>
              </div>
              <span className="text-[11px] font-sans text-gray-500 mt-1">Tỷ lệ duy trì 100% tuần</span>
            </div>

            <div className="bg-paper-warm border-neo-sm p-4 rounded-2xl shadow-neo-sm flex flex-col justify-between">
              <span className="font-space text-xs font-bold text-gray-600 uppercase">Giấc ngủ trung bình</span>
              <div className="flex items-baseline gap-2 mt-2">
                <span className="font-space text-3xl font-extrabold text-blue-700">7.2h</span>
                <span className="text-xs font-space font-bold text-green-700">▲ Đủ giấc</span>
              </div>
              <span className="text-[11px] font-sans text-gray-500 mt-1">Cải thiện chất lượng phục hồi</span>
            </div>
          </div>

          {/* AI Empathetic Synthesis Section (Editorial Scrapbook) */}
          <div className="bg-surface-card border-neo rounded-3xl p-6 sm:p-8 shadow-neo flex flex-col gap-6">
            <div className="flex items-center gap-3 border-b-2 border-black pb-3">
              <Sparkles className="w-6 h-6 text-primary" />
              <div>
                <h2 className="font-space text-xl font-extrabold text-black">
                  Khái Quát Thấu Cảm Của AI (Empathetic Synthesis)
                </h2>
                <span className="font-mono text-[10px] text-gray-500 font-bold">
                  Dựa trên 14 ngày lịch sử định lượng
                </span>
              </div>
            </div>

            <div className="p-5 bg-paper-warm rounded-2xl border-neo-sm leading-relaxed">
              <p className="font-serif italic text-base sm:text-lg text-on-surface">
                &ldquo;Tuần 42 đánh dấu một bước chuyển mình rõ nét trong tâm lý của Minh Anh. Sau những ngày thức khuya sửa code và hồi hộp chuẩn bị slide đề cương, biểu đồ tâm trạng đã bật tăng ấn tượng ngay sau buổi bảo vệ thành công ngày 15.10. Bạn đã học được cách cho phép bản thân nghỉ ngơi bằng một buổi dạo bộ hồ Tây và ly cà phê muối yêu thích.&rdquo;
              </p>
            </div>

            {/* Notable 3 Highlights of the Week */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="p-4 bg-white rounded-2xl border-neo-sm shadow-neo-sm flex flex-col gap-2">
                <span className="font-space text-xs font-extrabold text-primary uppercase">
                  1. Cột mốc nổi bật
                </span>
                <p className="font-sans text-xs text-on-surface-variant leading-relaxed">
                  Bảo vệ đề cương đồ án tốt nghiệp với phản hồi tích cực từ hội đồng về tính nhân văn của MyLog.
                </p>
              </div>

              <div className="p-4 bg-white rounded-2xl border-neo-sm shadow-neo-sm flex flex-col gap-2">
                <span className="font-space text-xs font-extrabold text-purple-700 uppercase">
                  2. Mẫu hình tâm trạng
                </span>
                <p className="font-sans text-xs text-on-surface-variant leading-relaxed">
                  Tâm trạng tăng mạnh vào các buổi chiều tối có thói quen ra ngoài hít thở không khí trong lành.
                </p>
              </div>

              <div className="p-4 bg-white rounded-2xl border-neo-sm shadow-neo-sm flex flex-col gap-2">
                <span className="font-space text-xs font-extrabold text-amber-700 uppercase">
                  3. Lời khuyên tuần mới
                </span>
                <p className="font-sans text-xs text-on-surface-variant leading-relaxed">
                  Tiếp tục duy trì nhịp viết 5 phút mỗi tối và giữ thói quen ngủ trước 23h30 để tái tạo năng lượng.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================== */}
      {/* TAB 2: EVIDENCE MATRIX & HABITS (FR-INSIGHT-01..07)        */}
      {/* ========================================================== */}
      {activeTab === 'evidence-matrix' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* LEFT COLUMN: Evidence-Based Insights (7 cols) */}
          <div className="lg:col-span-7 flex flex-col gap-6">
            {/* EVIDENCE MATRIX HEADER CARD */}
            <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative">
              <WashiTape color="peach" rotate={2} className="absolute -top-3 right-8 w-28" />

              <div className="flex items-center justify-between pb-3 border-b-2 border-black mb-4">
                <div className="flex items-center gap-2">
                  <span className="w-3.5 h-3.5 rounded-full bg-primary-container border-neo-sm"></span>
                  <h2 className="font-space text-lg font-extrabold text-on-surface">
                    Ma Trận Tương Quan Bằng Chứng (FR-INSIGHT-01..07)
                  </h2>
                </div>
                <span className="font-space text-[10px] bg-paper-warm px-2 py-0.5 rounded border border-black font-bold">
                  Evidence-Based Only
                </span>
              </div>

              {/* No Causation Disclaimer Alert (FR-INSIGHT-03) */}
              <div className="p-3 bg-blue-50 border border-blue-300 rounded-xl text-xs font-sans text-blue-900 mb-4 flex items-start gap-2">
                <ShieldCheck className="w-4 h-4 text-blue-700 shrink-0 mt-0.5" />
                <span>
                  <strong>Nguyên tắc khoa học (FR-INSIGHT-03):</strong> Mọi insight đều được tính toán từ dữ liệu thống kê định lượng. Hệ thống cam kết không khẳng định quan hệ nhân quả tuyệt đối mà thể hiện theo mức độ tương quan và tần suất xuất hiện cùng nhau.
                </span>
              </div>

              {/* Evidence Cards List */}
              <div className="flex flex-col gap-4">
                {/* Insight 1: Topic "deadline" vs low mood */}
                <div className="p-4 bg-paper-warm rounded-2xl border-neo-sm shadow-neo-sm flex flex-col gap-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="font-space text-xs font-extrabold text-black">
                        Chủ đề &ldquo;deadline&rdquo; ↔ Tâm trạng thấp
                      </span>
                      <span className="px-2 py-0.5 bg-primary-container text-black font-space text-[10px] font-extrabold rounded border border-black uppercase">
                        Độ tin cậy: STRONG (FR-INSIGHT-04)
                      </span>
                    </div>
                    <span className="text-[11px] font-mono font-bold bg-white px-2 py-0.5 rounded border border-black">
                      ACTIVE
                    </span>
                  </div>

                  <p className="font-sans text-xs sm:text-sm text-on-surface leading-relaxed">
                    Trong dữ liệu 14 ngày qua, những ngày bạn đề cập đến chủ đề &ldquo;deadline&rdquo; thường xuất hiện cùng mức tâm trạng thấp hơn mức trung bình (Mood ≤ 4.5/10).
                  </p>

                  {/* Quantitative Evidence Box (FR-INSIGHT-02) */}
                  <div className="p-2.5 bg-white rounded-xl border border-black text-xs font-mono flex flex-wrap items-center justify-between gap-2">
                    <span>Cỡ mẫu (sampleSize): <strong>8 ngày</strong></span>
                    <span>Khớp điều kiện (matchingCount): <strong>6 ngày</strong></span>
                    <span>Tần suất (frequency): <strong>75%</strong></span>
                  </div>

                  {/* Feedback rating */}
                  <div className="flex items-center justify-between pt-2 border-t border-black/10 text-xs font-space font-bold">
                    <span className="text-gray-600">Insight này có đúng với bạn?</span>
                    <div className="flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={() => handleRateInsight('insight-1', 'HELPFUL')}
                        className={`px-2 py-1 rounded border border-black transition-colors flex items-center gap-1 ${
                          feedbackState['insight-1'] === 'HELPFUL' ? 'bg-primary-container font-extrabold' : 'bg-white hover:bg-gray-100'
                        }`}
                      >
                        <ThumbsUp className="w-3.5 h-3.5 stroke-[2.3]" />
                        <span>Đúng & Hữu ích</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => handleRateInsight('insight-1', 'NOT_HELPFUL')}
                        className={`px-2 py-1 rounded border border-black transition-colors flex items-center gap-1 ${
                          feedbackState['insight-1'] === 'NOT_HELPFUL' ? 'bg-red-200 font-extrabold' : 'bg-white hover:bg-gray-100'
                        }`}
                      >
                        <ThumbsDown className="w-3.5 h-3.5 stroke-[2.3]" />
                        <span>Chưa chính xác</span>
                      </button>
                    </div>
                  </div>
                </div>

                {/* Insight 2: Sleep vs Stress (Section 16 Sleep Resolution) */}
                <div className="p-4 bg-white rounded-2xl border-neo-sm shadow-neo-sm flex flex-col gap-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="font-space text-xs font-extrabold text-black">
                        Thời lượng ngủ &ge; 7h ↔ Căng thẳng giảm rõ rệt
                      </span>
                      <span className="px-2 py-0.5 bg-mood-hope-energy text-black font-space text-[10px] font-extrabold rounded border border-black uppercase">
                        Độ tin cậy: MODERATE
                      </span>
                    </div>
                    <span className="text-[11px] font-mono font-bold bg-paper-warm px-2 py-0.5 rounded border border-black">
                      ACTIVE
                    </span>
                  </div>

                  <p className="font-sans text-xs sm:text-sm text-on-surface leading-relaxed">
                    Khi số giờ ngủ đạt trên 7.0 giờ, chỉ số stress ngày hôm sau có xu hướng giảm từ 6.8 xuống 3.2. (Section 16: Chỉ kích hoạt khi có dữ liệu giấc ngủ thực tế).
                  </p>

                  <div className="p-2.5 bg-paper-warm rounded-xl border border-black text-xs font-mono flex flex-wrap items-center justify-between gap-2">
                    <span>Cỡ mẫu: <strong>10 ngày có ghi nhận ngủ</strong></span>
                    <span>Tương quan: <strong>r = -0.74 (Nghịch biến mạnh)</strong></span>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-black/10 text-xs font-space font-bold">
                    <span className="text-gray-600">Insight này có đúng với bạn?</span>
                    <div className="flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={() => handleRateInsight('insight-2', 'HELPFUL')}
                        className={`px-2 py-1 rounded border border-black transition-colors flex items-center gap-1 ${
                          feedbackState['insight-2'] === 'HELPFUL' ? 'bg-primary-container font-extrabold' : 'bg-gray-50 hover:bg-gray-100'
                        }`}
                      >
                        <ThumbsUp className="w-3.5 h-3.5 stroke-[2.3]" />
                        <span>Đúng & Hữu ích</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => handleRateInsight('insight-2', 'NOT_HELPFUL')}
                        className={`px-2 py-1 rounded border border-black transition-colors flex items-center gap-1 ${
                          feedbackState['insight-2'] === 'NOT_HELPFUL' ? 'bg-red-200 font-extrabold' : 'bg-gray-50 hover:bg-gray-100'
                        }`}
                      >
                        <ThumbsDown className="w-3.5 h-3.5 stroke-[2.3]" />
                        <span>Chưa chính xác</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN: Micro-Action & Wellness Habit Checklist (5 cols) */}
          <div className="lg:col-span-5 flex flex-col gap-6">
            <div className="bg-surface-card border-neo rounded-3xl p-6 shadow-neo relative">
              <WashiTape color="lavender" rotate={-2} className="absolute -top-3 left-6 w-28" />

              <div className="flex items-center justify-between pb-3 border-b-2 border-black mb-4">
                <div className="flex items-center gap-2">
                  <Sprout className="w-5 h-5 text-primary" />
                  <h2 className="font-space text-lg font-extrabold text-on-surface">
                    Mục Tiêu Nuôi Dưỡng Tinh Thần
                  </h2>
                </div>
                <span className="font-space text-xs font-bold text-gray-500">
                  {goals.filter((g) => g.completed).length}/{goals.length} Đạt
                </span>
              </div>

              <div className="flex flex-col gap-3">
                {goals.map((goal) => (
                  <div
                    key={goal.id}
                    onClick={() => toggleGoal(goal.id)}
                    className={`p-3.5 rounded-2xl border-neo-sm shadow-neo-sm flex items-start gap-3 transition-all cursor-pointer ${
                      goal.completed ? 'bg-paper-warm opacity-85' : 'bg-surface-container-lowest hover:bg-white'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded border-2 border-black flex items-center justify-center shrink-0 mt-0.5 ${
                        goal.completed ? 'bg-primary-container' : 'bg-white'
                      }`}
                    >
                      {goal.completed && <CheckSquare className="w-3.5 h-3.5 text-black" />}
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center justify-between">
                        <h4
                          className={`font-space text-xs sm:text-sm font-bold text-black ${
                            goal.completed ? 'line-through opacity-70' : ''
                          }`}
                        >
                          {goal.title}
                        </h4>
                        <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-black/10 font-bold">
                          {goal.completedDays}/{goal.targetDays} ngày
                        </span>
                      </div>
                      <p className="font-sans text-xs text-gray-600 mt-1">
                        {goal.description}
                      </p>
                    </div>
                  </div>
                ))}
              </div>

              <div className="mt-5 p-3.5 bg-paper-warm rounded-2xl border-neo-sm">
                <span className="font-space text-xs font-extrabold uppercase tracking-wider text-black mb-1 flex items-center gap-1.5">
                  <Lightbulb className="w-3.5 h-3.5 text-amber-600 fill-amber-300 stroke-[2.3]" />
                  Nhắc nhở hành vi nhỏ (Micro-Action):
                </span>
                <p className="font-sans text-xs text-gray-700 leading-relaxed">
                  Thay vì đặt mục tiêu lớn gây áp lực, hãy duy trì các hành động dưới 5 phút mỗi ngày để tạo thói quen bền vững.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}
      </>
      )}
    </div>
  );
}
