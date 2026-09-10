'use client';

import React from 'react';
import Link from 'next/link';
import { useJournal } from '@/lib/journal-context';
import { WashiTape, StickerBadge } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';

export default function InsightsAiReportsPage() {
  const { goals, toggleGoal, streakCount } = useJournal();

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col gap-8">
      {/* Top Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b-2 border-on-background">
        <div>
          <h1 className="font-space text-2xl sm:text-4xl font-extrabold text-on-surface">
            Báo Cáo Tự Phản Chiếu & Mục Tiêu Chăm Sóc
          </h1>
          <p className="font-sans text-xs sm:text-sm text-on-surface-variant">
            Phân tích tương quan tâm lý dài hạn & thiết lập thói quen nuôi dưỡng tinh thần
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span className="px-3 py-1.5 bg-paper-warm border-neo-sm rounded-xl font-space text-xs font-bold shadow-neo-sm">
            Kỳ Báo Cáo: 14 Ngày Vừa Qua
          </span>
        </div>
      </div>

      {/* CORE 1: CORRELATION DISCOVERY MATRIX */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* LEFT COLUMN: Correlations & Emotional frequency (7 cols) */}
        <div className="lg:col-span-7 flex flex-col gap-6">
          {/* CORRELATION MATRIX CARD */}
          <div className="bg-surface-card border-neo rounded-2xl p-6 shadow-neo relative">
            <WashiTape color="lime" rotate={-1} className="absolute -top-3 left-10 w-32" />

            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-4">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-full bg-primary-container border-neo-sm"></span>
                <h2 className="font-space text-lg font-bold text-on-surface">
                  Ma Trận Tương Quan & Quy Luật Hành Vi
                </h2>
              </div>
              <span className="font-space text-[10px] bg-paper-warm px-2 py-0.5 rounded border border-black font-bold">
                Correlation Discovery
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Correlation Item 1: Sleep vs Stress */}
              <div className="bg-surface-container-lowest p-4 rounded-xl border-neo-sm shadow-neo-sm flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <span className="font-space text-xs font-bold text-on-surface">Giấc ngủ & Căng thẳng</span>
                  <span className="text-xs bg-mood-calm-joy/30 text-black px-2 py-0.5 rounded border border-black font-mono font-bold">
                    -0.78 (Nghịch biến)
                  </span>
                </div>
                <p className="font-sans text-xs text-on-surface-variant leading-relaxed">
                  Khi bạn ngủ đủ trên <strong>7.5 tiếng</strong>, chỉ số căng thẳng ngày hôm sau giảm trung bình <strong>42%</strong>. Những đêm ngủ dưới 6 tiếng khiến mức độ nhạy cảm với deadline tăng cao.
                </p>
              </div>

              {/* Correlation Item 2: Physical activity vs Energy */}
              <div className="bg-surface-container-lowest p-4 rounded-xl border-neo-sm shadow-neo-sm flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <span className="font-space text-xs font-bold text-on-surface">Chạy bộ & Tích cực</span>
                  <span className="text-xs bg-mood-hope-energy/40 text-black px-2 py-0.5 rounded border border-black font-mono font-bold">
                    +0.85 (Đồng biến)
                  </span>
                </div>
                <p className="font-sans text-xs text-on-surface-variant leading-relaxed">
                  Hoạt động chạy bộ sáng sớm quanh Hồ Tây có tác động tích cực tức thì, mang lại điểm năng lượng đạt <strong>8.5 - 9.0/10</strong> suốt cả ngày.
                </p>
              </div>

              {/* Correlation Item 3: Perfectionism vs Delay */}
              <div className="bg-surface-container-lowest p-4 rounded-xl border-neo-sm shadow-neo-sm flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <span className="font-space text-xs font-bold text-on-surface">Kỳ vọng hoàn hảo</span>
                  <span className="text-xs bg-mood-anxiety-stress/30 text-black px-2 py-0.5 rounded border border-black font-mono font-bold">
                    Tạo tắc nghẽn
                  </span>
                </div>
                <p className="font-sans text-xs text-on-surface-variant leading-relaxed">
                  Những trang nhật ký nhắc đến "chỉnh sửa" hoặc "chưa hài lòng" thường đi kèm thời gian thức khuya và cảm giác bế tắc tạm thời.
                </p>
              </div>

              {/* Correlation Item 4: Weekly Rhythm */}
              <div className="bg-surface-container-lowest p-4 rounded-xl border-neo-sm shadow-neo-sm flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <span className="font-space text-xs font-bold text-on-surface">Nhịp Điệu Tuần</span>
                  <span className="text-xs bg-secondary-fixed text-black px-2 py-0.5 rounded border border-black font-mono font-bold">
                    Thứ 4 - Thứ 6
                  </span>
                </div>
                <p className="font-sans text-xs text-on-surface-variant leading-relaxed">
                  Giữa tuần là giai đoạn bạn đạt năng suất cao nhất nhưng cũng dễ tích tụ áp lực nếu không có những khoảng nghỉ ngắn (Micro-breaks).
                </p>
              </div>
            </div>
          </div>

          {/* EMOTION FREQUENCY BREAKDOWN */}
          <div className="bg-surface-card border-neo rounded-2xl p-6 shadow-neo">
            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-4">
              <h2 className="font-space text-lg font-bold text-on-surface">
                Tần Suất Xuất Hiện Cảm Xúc (14 Ngày qua)
              </h2>
              <span className="font-space text-xs text-gray-500">14 mẫu phân tích</span>
            </div>

            <div className="space-y-3">
              {[
                { label: 'Bình an & Thảnh thơi', count: 6, percent: 43, color: 'bg-mood-calm-joy' },
                { label: 'Hăng hái & Năng lượng cao', count: 4, percent: 29, color: 'bg-mood-hope-energy' },
                { label: 'Trầm tư suy xét & Tự vấn', count: 3, percent: 21, color: 'bg-mood-sadness-reflect' },
                { label: 'Áp lực đồ án & Thi cử', count: 1, percent: 7, color: 'bg-mood-anxiety-stress' },
              ].map((item, i) => (
                <div key={i} className="flex flex-col gap-1">
                  <div className="flex justify-between font-space text-xs font-bold">
                    <span>{item.label}</span>
                    <span>{item.count} ngày ({item.percent}%)</span>
                  </div>
                  <div className="w-full h-3 bg-gray-100 rounded-full border border-black overflow-hidden p-0.5">
                    <div
                      style={{ width: `${item.percent}%` }}
                      className={`h-full rounded-full ${item.color}`}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* RIGHT COLUMN: Goals & Habits & Safety Disclaimer (5 cols) */}
        <div className="lg:col-span-5 flex flex-col gap-6">
          {/* WELLNESS GOALS & HABITS */}
          <div className="bg-surface-card border-neo rounded-2xl p-6 shadow-neo relative">
            <WashiTape color="peach" rotate={2} className="absolute -top-3 right-8 w-28" />

            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-4">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-base">checklist</span>
                <h2 className="font-space text-base font-bold text-on-surface">Mục Tiêu & Thói Quen Chăm Sóc</h2>
              </div>
              <span className="text-[10px] font-space font-bold bg-primary-container px-2 py-0.5 rounded border border-black">
                Tuần 42
              </span>
            </div>

            <div className="flex flex-col gap-3">
              {goals.map((g) => (
                <div
                  key={g.id}
                  onClick={() => toggleGoal(g.id)}
                  className={`p-3.5 rounded-xl border-neo-sm shadow-neo-sm flex items-start gap-3 cursor-pointer transition-all ${
                    g.completed ? 'bg-paper-warm opacity-80' : 'bg-surface-container-lowest hover:bg-paper-warm'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={g.completed}
                    onChange={() => {}}
                    className="mt-0.5 w-4 h-4 accent-primary cursor-pointer"
                  />
                  <div className="flex-1">
                    <div className="flex items-center justify-between">
                      <span className={`font-space text-xs font-bold ${g.completed ? 'line-through text-gray-500' : 'text-on-surface'}`}>
                        {g.title}
                      </span>
                      <span className="font-mono text-[10px] font-bold bg-white px-1.5 py-0.5 rounded border border-black">
                        {g.completedDays}/{g.targetDays} {g.unit}
                      </span>
                    </div>
                    <p className="font-sans text-[11px] text-on-surface-variant mt-0.5">
                      {g.description}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            {/* Quick Micro Action */}
            <div className="mt-5 p-4 bg-primary-container/20 border-neo-sm rounded-xl">
              <span className="font-space text-[10px] uppercase font-bold text-on-primary-container block mb-1">
                🌱 Hành Động Nhỏ Cho Tuần Tới:
              </span>
              <p className="font-sans text-xs text-on-surface leading-relaxed">
                "Thử tắt thông báo mạng xã hội sau 21h30 và thay thế bằng 15 phút đọc sách giấy cùng một tách trà hoa cúc."
              </p>
            </div>
          </div>

          {/* SAFETY DISCLAIMER */}
          <div className="bg-paper-warm border-neo rounded-2xl p-5 shadow-neo">
            <div className="flex items-center gap-2 mb-2 text-on-surface">
              <span className="material-symbols-outlined text-base font-bold">verified_user</span>
              <h3 className="font-space text-xs font-bold uppercase tracking-wider">
                Tuyên Bố An Toàn Sức Khỏe Tinh Thần
              </h3>
            </div>
            <p className="font-sans text-[11px] text-on-surface-variant leading-relaxed">
              MyLog là một công cụ hỗ trợ ghi nhật ký và tự thấu cảm cá nhân thông qua AI, <strong>không thay thế cho các chẩn đoán hoặc tư vấn y khoa chuyên nghiệp</strong>. Nếu bạn đang trải qua những khủng hoảng tâm lý kéo dài, xin vui lòng tìm kiếm sự trợ giúp từ các chuyên gia tâm lý hoặc liên hệ đường dây nóng hỗ trợ.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
