'use client';

import React from 'react';
import Link from 'next/link';
import { useJournal } from '@/lib/journal-context';
import { WashiTape, PolaroidCard, StickerBadge } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';

export default function DashboardPage() {
  const { entries, stats, streakCount } = useJournal();
  const latestEntry = entries[0];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col gap-8">
      {/* HERO BANNER - Playful Neo-Brutalist Scrapbook */}
      <section className="relative w-full">
        {/* Washi Tape decoration */}
        <WashiTape color="lavender" rotate={-2} className="absolute -top-3 left-10 w-36 z-20" />

        <div className="bg-surface-card border-neo rounded-2xl p-6 sm:p-8 shadow-neo-lg relative overflow-hidden">
          {/* Badges bar */}
          <div className="flex flex-wrap items-center justify-between gap-4 mb-6 border-b-2 border-dashed border-border-soft pb-4">
            <div className="flex flex-wrap items-center gap-2">
              <span className="px-3 py-1 bg-paper-warm text-on-surface font-space text-xs sm:text-sm font-bold border-neo-sm rounded-lg shadow-neo-sm flex items-center gap-1.5">
                <span className="material-symbols-outlined text-base">calendar_today</span>
                Thứ Năm, 15 Tháng 10, 2026
              </span>
              <span className="px-3 py-1 bg-primary-container text-on-primary-container font-space text-xs sm:text-sm font-bold border-neo-sm rounded-lg shadow-neo-sm uppercase tracking-wider">
                Kỳ Phân Tích: Tuần 42
              </span>
            </div>

            {/* Streak Badge */}
            <div className="inline-flex items-center gap-2 bg-mood-hope-energy text-black px-4 py-1.5 border-neo-sm rounded-xl shadow-neo-sm rotate-1">
              <span className="text-xl">🔥</span>
              <span className="font-space text-sm font-extrabold">{streakCount} Ngày Viết Liên Tục</span>
              <span className="text-[11px] bg-white px-2 py-0.5 rounded border border-black uppercase font-bold text-on-surface">
                Huy hiệu Sổ Tay Vàng
              </span>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-center">
            <div className="lg:col-span-8 flex flex-col gap-3">
              <h1 className="font-space text-2xl sm:text-4xl lg:text-5xl font-extrabold text-on-surface tracking-tight leading-tight">
                Chào Minh Anh,{' '}
                <span className="bg-primary-container px-2.5 py-0.5 border-neo-sm rounded-lg shadow-neo-sm inline-block">
                  hôm nay
                </span>{' '}
                bạn cảm thấy thế nào?
              </h1>
              <p className="font-sans text-sm sm:text-base text-on-surface-variant max-w-2xl leading-relaxed">
                Không gian cá nhân an toàn để trút cạn suy tư. Nhịp sống chậm lại 5 phút để AI cùng bạn thấu cảm,
                nhận diện mẫu hình xúc cảm và tìm lại cân bằng nội tại.
              </p>
            </div>

            {/* Call To Action */}
            <div className="lg:col-span-4 flex flex-col sm:flex-row lg:justify-end items-stretch sm:items-center gap-3">
              <Link href="/journal-editor" className="w-full sm:w-auto">
                <NeoButton size="lg" className="w-full bg-primary-container text-on-primary-container font-extrabold" icon={<span className="material-symbols-outlined font-bold">edit_note</span>}>
                  Ghi lại nhật ký hôm nay [+]
                </NeoButton>
              </Link>
            </div>
          </div>

          {/* 4 Fast Status Cards */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mt-8 pt-6 border-t-2 border-on-background">
            <div className="bg-surface-container-lowest p-3.5 rounded-xl border-neo-sm shadow-neo-sm flex items-center justify-between hover:bg-paper-warm transition-colors">
              <div>
                <span className="font-space text-xs text-on-surface-variant uppercase tracking-wider block font-bold">Tâm Trạng</span>
                <span className="font-space text-xl font-bold text-on-surface">7.8<span className="text-xs font-normal text-on-surface-variant">/10</span></span>
              </div>
              <div className="w-10 h-10 rounded-full bg-mood-calm-joy/30 border-neo-sm flex items-center justify-center text-xl shadow-neo-sm">
                😊
              </div>
            </div>

            <div className="bg-surface-container-lowest p-3.5 rounded-xl border-neo-sm shadow-neo-sm flex items-center justify-between hover:bg-paper-warm transition-colors">
              <div>
                <span className="font-space text-xs text-on-surface-variant uppercase tracking-wider block font-bold">Mức Độ Stress</span>
                <span className="font-space text-xl font-bold text-on-surface">3<span className="text-xs font-normal text-on-surface-variant">/10 Thấp</span></span>
              </div>
              <div className="w-10 h-10 rounded-full bg-mood-calm-joy/30 border-neo-sm flex items-center justify-center text-xl shadow-neo-sm">
                🍃
              </div>
            </div>

            <div className="bg-surface-container-lowest p-3.5 rounded-xl border-neo-sm shadow-neo-sm flex items-center justify-between hover:bg-paper-warm transition-colors">
              <div>
                <span className="font-space text-xs text-on-surface-variant uppercase tracking-wider block font-bold">Năng Lượng</span>
                <span className="font-space text-xl font-bold text-on-surface">8.0<span className="text-xs font-normal text-on-surface-variant">/10</span></span>
              </div>
              <div className="w-10 h-10 rounded-full bg-mood-hope-energy/40 border-neo-sm flex items-center justify-center text-xl shadow-neo-sm">
                ⚡
              </div>
            </div>

            <div className="bg-surface-container-lowest p-3.5 rounded-xl border-neo-sm shadow-neo-sm flex items-center justify-between hover:bg-paper-warm transition-colors">
              <div>
                <span className="font-space text-xs text-on-surface-variant uppercase tracking-wider block font-bold">Giấc Ngủ Đêm Qua</span>
                <span className="font-space text-xl font-bold text-on-surface">7.5h<span className="text-xs font-normal text-on-surface-variant"> Sâu</span></span>
              </div>
              <div className="w-10 h-10 rounded-full bg-mood-sadness-reflect/20 border-neo-sm flex items-center justify-center text-xl shadow-neo-sm">
                🌙
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CORE COLLAGE BENTO GRID (FR-10, FR-06, FR-07) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* COLUMN 1 (7 cols): MOOD & STRESS 14-DAY DUAL TREND CHART (FR-10) */}
        <div className="lg:col-span-7 flex flex-col gap-6">
          <div className="bg-surface-card border-neo rounded-2xl p-6 shadow-neo relative">
            <WashiTape color="peach" rotate={2} className="absolute -top-3 right-8 w-28" />

            <div className="flex flex-wrap items-center justify-between gap-2 pb-4 border-b-2 border-on-background">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-full bg-primary-container border-neo-sm"></span>
                <h2 className="font-space text-lg font-bold text-on-surface">Xu Hướng 14 Ngày Qua</h2>
              </div>
              <span className="font-space text-xs bg-surface-container-high px-2.5 py-1 rounded border-neo-sm font-bold">
                FR-10 / Realtime Sync
              </span>
            </div>

            {/* Legend */}
            <div className="flex items-center gap-4 my-4 text-xs font-space font-bold">
              <div className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded-full bg-primary-container border border-black"></span>
                <span>Tâm trạng (Mood 1-10)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded-full bg-mood-anxiety-stress border border-black"></span>
                <span>Áp lực (Stress 1-10)</span>
              </div>
            </div>

            {/* Interactive Bar/Column Visualizer */}
            <div className="h-48 w-full flex items-end justify-between gap-1.5 sm:gap-2 pt-6 pb-2 px-2 bg-paper-warm/40 rounded-xl border-neo-sm">
              {stats.map((item, idx) => (
                <div key={idx} className="flex-1 flex flex-col items-center gap-1 h-full justify-end group relative">
                  {/* Tooltip */}
                  <div className="opacity-0 group-hover:opacity-100 transition-opacity absolute -top-10 bg-black text-white text-[10px] font-mono px-2 py-1 rounded whitespace-nowrap z-30 pointer-events-none">
                    {item.date}: Mood {item.moodScore} | Stress {item.stressScore}
                  </div>

                  <div className="w-full flex items-end justify-center gap-0.5 h-36">
                    {/* Mood bar */}
                    <div
                      style={{ height: `${(item.moodScore / 10) * 100}%` }}
                      className="w-1/2 bg-primary-container border border-black rounded-t shadow-neo-sm transition-all duration-300 group-hover:brightness-105"
                    />
                    {/* Stress bar */}
                    <div
                      style={{ height: `${(item.stressScore / 10) * 100}%` }}
                      className="w-1/2 bg-mood-anxiety-stress/80 border border-black rounded-t shadow-neo-sm transition-all duration-300 group-hover:brightness-105"
                    />
                  </div>
                  <span className="font-space text-[10px] font-bold text-on-surface-variant truncate">
                    {item.dayName}
                  </span>
                </div>
              ))}
            </div>

            <p className="mt-4 text-xs font-sans text-on-surface-variant bg-surface-container-low p-3 rounded-xl border border-border-soft">
              💡 <strong>AI Khái Quát:</strong> Tâm trạng có xu hướng bật tăng mạnh vào cuối tuần và những ngày sau khi hoàn thành các mục tiêu học tập quan trọng.
            </p>
          </div>

          {/* CYCLIC COMPARISON & THEMES (FR-07) */}
          <div className="bg-surface-card border-neo rounded-2xl p-6 shadow-neo">
            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-4">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-full bg-secondary-container border-neo-sm"></span>
                <h2 className="font-space text-lg font-bold text-on-surface">Chủ Đề Nổi Bật (FR-07)</h2>
              </div>
              <span className="font-space text-xs bg-paper-warm px-2 py-0.5 rounded border border-black font-bold">
                Trích xuất tuần
              </span>
            </div>

            <div className="flex flex-wrap gap-2.5">
              {[
                { tag: '#đồ án bách khoa', count: 8, bg: 'bg-primary-container' },
                { tag: '#chạy bộ hồ tây', count: 5, bg: 'bg-mood-hope-energy' },
                { tag: '#latte yến mạch', count: 4, bg: 'bg-paper-warm' },
                { tag: '#thức khuya sửa code', count: 3, bg: 'bg-mood-anxiety-stress text-white' },
                { tag: '#sách tâm lý học', count: 3, bg: 'bg-secondary-fixed' },
                { tag: '#buông bỏ cầu toàn', count: 2, bg: 'bg-mood-sadness-reflect text-white' }
              ].map((item, i) => (
                <span
                  key={i}
                  className={`font-space text-xs font-bold px-3 py-1.5 rounded-xl border-neo-sm shadow-neo-sm flex items-center gap-1.5 hover:scale-105 transition-transform cursor-pointer ${item.bg}`}
                >
                  <span>{item.tag}</span>
                  <span className="text-[10px] bg-black/15 px-1.5 py-0.5 rounded-full font-mono">{item.count}</span>
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* COLUMN 2 (5 cols): EMOTION SPECTRUM (FR-06) & RECENT POLAROID SHELF */}
        <div className="lg:col-span-5 flex flex-col gap-6">
          {/* EMOTION SPECTRUM (FR-06) */}
          <div className="bg-surface-card border-neo rounded-2xl p-6 shadow-neo relative">
            <WashiTape color="blue" rotate={-1} className="absolute -top-3 left-6 w-24" />

            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-4">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-full bg-mood-hope-energy border-neo-sm"></span>
                <h2 className="font-space text-lg font-bold text-on-surface">Phổ Cảm Xúc (FR-06)</h2>
              </div>
              <span className="font-space text-xs bg-paper-warm px-2 py-0.5 rounded border border-black font-bold">
                Tuần 42
              </span>
            </div>

            <div className="flex flex-col gap-3">
              {[
                { label: 'Bình an & Thư thái', percent: 45, color: 'bg-mood-calm-joy' },
                { label: 'Năng lượng & Tự hào', percent: 30, color: 'bg-mood-hope-energy' },
                { label: 'Suy tư & Tự vấn', percent: 15, color: 'bg-mood-sadness-reflect' },
                { label: 'Lo âu & Căng thẳng deadline', percent: 10, color: 'bg-mood-anxiety-stress' }
              ].map((em, idx) => (
                <div key={idx} className="flex flex-col gap-1">
                  <div className="flex justify-between text-xs font-space font-bold">
                    <span>{em.label}</span>
                    <span>{em.percent}%</span>
                  </div>
                  <div className="w-full h-3.5 bg-gray-100 rounded-full border-neo-sm overflow-hidden p-0.5">
                    <div
                      style={{ width: `${em.percent}%` }}
                      className={`h-full rounded-full ${em.color}`}
                    />
                  </div>
                </div>
              ))}
            </div>

            {/* AI Empathetic Quote */}
            <div className="mt-5 p-3.5 bg-paper-warm border-neo-sm rounded-xl shadow-neo-sm">
              <span className="font-space text-[10px] uppercase tracking-wider font-extrabold text-primary-container bg-black px-2 py-0.5 rounded inline-block mb-1">
                AI Phản Chiếu Thấu Cảm
              </span>
              <p className="font-serif italic text-xs sm:text-sm text-on-surface leading-relaxed">
                "Cảm xúc bình an đang chiếm ưu thế vượt trội sau khi Minh Anh hoàn thành bảo vệ đề cương. Hãy tiếp tục duy trì những khoảng dừng tĩnh lặng mỗi sớm."
              </p>
            </div>
          </div>

          {/* POLAROID SHELF */}
          <div className="bg-surface-card border-neo rounded-2xl p-6 shadow-neo">
            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-4">
              <h2 className="font-space text-lg font-bold text-on-surface">Kệ Ký Ức (Polaroid Shelf)</h2>
              <Link href="/history-calendar" className="text-xs font-space font-bold underline hover:text-primary">
                Xem tất cả ({entries.length}) →
              </Link>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {entries.slice(0, 2).map((entry, index) => (
                <Link key={entry.id} href="/history-calendar">
                  <PolaroidCard
                    imageUrl={entry.photoUrl || 'https://images.unsplash.com/photo-1517842645767-c639042777db?w=600&auto=format&fit=crop&q=80'}
                    caption={entry.title}
                    date={entry.date}
                    rotate={index % 2 === 0 ? -2 : 2}
                  />
                </Link>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
