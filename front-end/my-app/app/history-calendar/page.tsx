'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useJournal } from '@/lib/journal-context';
import { MoodType } from '@/lib/types';
import { WashiTape, PolaroidCard, StickerBadge } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';

export default function HistoryCalendarPage() {
  const { entries, deleteEntry } = useJournal();
  const [selectedMood, setSelectedMood] = useState<string>('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedDate, setSelectedDate] = useState<string>('2026-10-15');

  const filteredEntries = entries.filter((entry) => {
    const matchesMood = selectedMood === 'all' || entry.mood === selectedMood;
    const matchesSearch =
      entry.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      entry.content.toLowerCase().includes(searchTerm.toLowerCase()) ||
      entry.tags.some((t) => t.toLowerCase().includes(searchTerm.toLowerCase()));
    return matchesMood && matchesSearch;
  });

  const calendarDays = [
    { day: 28, inMonth: false, mood: null },
    { day: 29, inMonth: false, mood: null },
    { day: 30, inMonth: false, mood: null },
    { day: 1, inMonth: true, mood: 'neutral' },
    { day: 2, inMonth: true, mood: 'anxiety-stress' },
    { day: 3, inMonth: true, mood: 'calm-joy' },
    { day: 4, inMonth: true, mood: 'calm-joy' },
    { day: 5, inMonth: true, mood: 'neutral' },
    { day: 6, inMonth: true, mood: 'anxiety-stress' },
    { day: 7, inMonth: true, mood: 'anxiety-stress' },
    { day: 8, inMonth: true, mood: 'calm-joy' },
    { day: 9, inMonth: true, mood: 'hope-energy' },
    { day: 10, inMonth: true, mood: 'hope-energy' },
    { day: 11, inMonth: true, mood: 'calm-joy' },
    { day: 12, inMonth: true, mood: 'sadness-reflect' },
    { day: 13, inMonth: true, mood: 'calm-joy' },
    { day: 14, inMonth: true, mood: 'hope-energy' },
    { day: 15, inMonth: true, mood: 'calm-joy', active: true },
    { day: 16, inMonth: true, mood: null },
    { day: 17, inMonth: true, mood: null },
    { day: 18, inMonth: true, mood: null },
    { day: 19, inMonth: true, mood: null },
    { day: 20, inMonth: true, mood: null },
    { day: 21, inMonth: true, mood: null },
    { day: 22, inMonth: true, mood: null },
    { day: 23, inMonth: true, mood: null },
    { day: 24, inMonth: true, mood: null },
    { day: 25, inMonth: true, mood: null },
    { day: 26, inMonth: true, mood: null },
    { day: 27, inMonth: true, mood: null },
    { day: 28, inMonth: true, mood: null },
    { day: 29, inMonth: true, mood: null },
    { day: 30, inMonth: true, mood: null },
    { day: 31, inMonth: true, mood: null },
    { day: 1, inMonth: false, mood: null },
  ];

  const moodEmojiMap: Record<string, string> = {
    'calm-joy': '😊',
    'hope-energy': '⚡',
    'anxiety-stress': '😰',
    'sadness-reflect': '🌧️',
    neutral: '😐',
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col gap-8">
      {/* Top Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b-2 border-on-background">
        <div>
          <h1 className="font-space text-2xl sm:text-4xl font-extrabold text-on-surface">
            Lịch Ký Ức & Timeline Đa Tầng
          </h1>
          <p className="font-sans text-xs sm:text-sm text-on-surface-variant">
            Theo dõi hành trình nội tâm của bạn qua từng ngày và trang nhật ký scrapbook
          </p>
        </div>

        <Link href="/journal-editor">
          <NeoButton size="md" icon={<span className="material-symbols-outlined text-base">add_circle</span>}>
            Thêm trang mới
          </NeoButton>
        </Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* LEFT COLUMN: Calendar & Filters (4 cols) */}
        <div className="lg:col-span-4 flex flex-col gap-6">
          {/* CALENDAR CARD */}
          <div className="bg-surface-card border-neo rounded-2xl p-5 shadow-neo relative">
            <WashiTape color="lime" rotate={-2} className="absolute -top-3 left-8 w-28" />

            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-3">
              <span className="font-space text-base font-bold text-on-surface">Tháng 10, 2026</span>
              <div className="flex items-center gap-1">
                <button className="p-1 border border-black rounded hover:bg-paper-warm text-xs font-bold">‹</button>
                <button className="p-1 border border-black rounded hover:bg-paper-warm text-xs font-bold">›</button>
              </div>
            </div>

            {/* Weekdays */}
            <div className="grid grid-cols-7 text-center font-space text-[11px] font-extrabold text-on-surface-variant mb-2">
              <span>T2</span>
              <span>T3</span>
              <span>T4</span>
              <span>T5</span>
              <span>T6</span>
              <span>T7</span>
              <span>CN</span>
            </div>

            {/* Days Grid */}
            <div className="grid grid-cols-7 gap-1">
              {calendarDays.map((item, idx) => (
                <div
                  key={idx}
                  className={`aspect-square p-1 border rounded-lg flex flex-col items-center justify-between text-xs transition-all cursor-pointer ${
                    !item.inMonth
                      ? 'opacity-30 border-transparent'
                      : item.active
                      ? 'bg-primary-container border-neo-sm font-bold shadow-neo-sm'
                      : 'border-border-soft hover:border-black bg-surface-container-lowest'
                  }`}
                >
                  <span className="font-mono text-[10px] font-bold">{item.day}</span>
                  {item.mood && (
                    <span className="text-[12px] leading-none" title={item.mood}>
                      {moodEmojiMap[item.mood]}
                    </span>
                  )}
                </div>
              ))}
            </div>

            {/* Mood Legend */}
            <div className="mt-4 pt-3 border-t border-border-soft flex flex-wrap items-center justify-around text-[10px] font-space font-bold">
              <span className="flex items-center gap-0.5">😊 Bình an</span>
              <span className="flex items-center gap-0.5">⚡ Năng lượng</span>
              <span className="flex items-center gap-0.5">😰 Lo âu</span>
              <span className="flex items-center gap-0.5">🌧️ Suy ngẫm</span>
            </div>
          </div>

          {/* SEARCH & FILTERS */}
          <div className="bg-surface-card border-neo rounded-2xl p-5 shadow-neo">
            <h2 className="font-space text-sm font-bold text-on-surface mb-3 uppercase tracking-wider">
              Tìm kiếm & Lọc theo Tâm Trạng
            </h2>

            <div className="mb-4">
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Tìm từ khóa, thẻ hashtag..."
                className="w-full font-space text-xs p-2.5 rounded-xl border-neo-sm bg-white focus:outline-none"
              />
            </div>

            <div className="flex flex-wrap gap-2">
              {[
                { id: 'all', label: 'Tất cả' },
                { id: 'calm-joy', label: '😊 Bình an' },
                { id: 'hope-energy', label: '⚡ Năng lượng' },
                { id: 'anxiety-stress', label: '😰 Căng thẳng' },
                { id: 'sadness-reflect', label: '🌧️ Suy ngẫm' },
              ].map((filter) => (
                <button
                  key={filter.id}
                  onClick={() => setSelectedMood(filter.id)}
                  className={`font-space text-xs font-bold px-3 py-1.5 rounded-xl border-neo-sm transition-all ${
                    selectedMood === filter.id
                      ? 'bg-primary-container text-on-primary-container shadow-neo-sm translate-y-0.5'
                      : 'bg-paper-warm text-on-surface hover:bg-surface-container-high'
                  }`}
                >
                  {filter.label}
                </button>
              ))}
            </div>
          </div>

          {/* MONTH STATS BADGE */}
          <div className="p-5 bg-paper-warm border-neo rounded-2xl shadow-neo">
            <span className="font-space text-[10px] uppercase font-bold tracking-wider bg-black text-white px-2 py-0.5 rounded">
              Thống Kê Tháng 10
            </span>
            <div className="grid grid-cols-2 gap-3 mt-3">
              <div className="bg-white p-2.5 rounded-xl border border-black">
                <span className="font-space text-[10px] text-gray-500 font-bold block">Tổng trang viết</span>
                <span className="font-space text-xl font-extrabold">{entries.length} bài</span>
              </div>
              <div className="bg-white p-2.5 rounded-xl border border-black">
                <span className="font-space text-[10px] text-gray-500 font-bold block">Tâm trạng TB</span>
                <span className="font-space text-xl font-extrabold text-primary">7.8/10</span>
              </div>
            </div>
          </div>
        </div>

        {/* RIGHT COLUMN: Timeline stream of notebook pages (8 cols) */}
        <div className="lg:col-span-8 flex flex-col gap-6">
          <div className="flex items-center justify-between">
            <h2 className="font-space text-xl font-bold text-on-surface">
              Dòng Ký Ức & Trang Sổ ({filteredEntries.length})
            </h2>
            <span className="font-space text-xs text-on-surface-variant">Sắp xếp theo mới nhất</span>
          </div>

          {filteredEntries.length === 0 ? (
            <div className="bg-surface-card border-neo rounded-2xl p-12 text-center shadow-neo">
              <p className="font-space text-base font-bold text-on-surface">Không tìm thấy bài viết nào phù hợp.</p>
              <button
                onClick={() => { setSelectedMood('all'); setSearchTerm(''); }}
                className="mt-3 font-space text-xs font-bold text-primary underline"
              >
                Đặt lại bộ lọc
              </button>
            </div>
          ) : (
            filteredEntries.map((entry, idx) => (
              <article
                key={entry.id}
                className="bg-surface-card border-neo rounded-2xl p-6 sm:p-8 shadow-neo relative bg-notebook-lines transition-transform hover:-translate-y-1"
              >
                {/* Random Washi Tape */}
                <WashiTape
                  color={idx % 3 === 0 ? 'peach' : idx % 3 === 1 ? 'lavender' : 'lime'}
                  rotate={idx % 2 === 0 ? -2 : 2}
                  className="absolute -top-3 right-10 w-28"
                />

                {/* Header row */}
                <div className="flex flex-wrap items-center justify-between gap-2 mb-3 pb-2 border-b border-border-soft">
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-0.5 bg-paper-warm font-mono text-xs font-bold border border-black rounded">
                      {entry.date} • {entry.time}
                    </span>
                    <span className="px-2 py-0.5 bg-primary-container text-black font-space text-xs font-bold border border-black rounded">
                      Mood {entry.moodScore}/10
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="text-xl" title={entry.mood}>
                      {moodEmojiMap[entry.mood]}
                    </span>
                    <button
                      onClick={() => deleteEntry(entry.id)}
                      className="text-gray-400 hover:text-red-600 p-1 text-xs"
                      title="Xóa bài viết"
                    >
                      <span className="material-symbols-outlined text-base">delete</span>
                    </button>
                  </div>
                </div>

                {/* Title */}
                <h3 className="font-space text-xl sm:text-2xl font-extrabold text-on-surface mb-3 leading-snug">
                  {entry.title}
                </h3>

                {/* Polaroid photo if exists */}
                {entry.photoUrl && (
                  <div className="my-4 max-w-sm">
                    <PolaroidCard
                      imageUrl={entry.photoUrl}
                      caption={entry.photoCaption || entry.title}
                      rotate={idx % 2 === 0 ? 1 : -1}
                    />
                  </div>
                )}

                {/* Content */}
                <p className="font-serif text-base text-on-surface leading-relaxed whitespace-pre-line mb-4">
                  {entry.content}
                </p>

                {/* AI Reflection preview */}
                {entry.aiAnalysis && (
                  <div className="p-3.5 bg-paper-warm/80 border border-black rounded-xl mb-4">
                    <span className="font-space text-[10px] uppercase font-bold text-gray-700 block mb-1">
                      💡 AI Phản Hồi: {entry.aiAnalysis.sentiment}
                    </span>
                    <p className="font-sans text-xs italic text-on-surface">
                      "{entry.aiAnalysis.reflectionPrompt}"
                    </p>
                  </div>
                )}

                {/* Tags & Meta Footer */}
                <div className="flex flex-wrap items-center justify-between gap-2 pt-3 border-t border-border-soft">
                  <div className="flex flex-wrap gap-1.5">
                    {entry.tags.map((tag) => (
                      <span
                        key={tag}
                        className="font-space text-[11px] font-bold px-2 py-0.5 bg-surface-container-high border border-black rounded"
                      >
                        #{tag}
                      </span>
                    ))}
                  </div>

                  {entry.location && (
                    <span className="font-space text-xs text-gray-600 flex items-center gap-1 font-semibold">
                      <span className="material-symbols-outlined text-sm">location_on</span>
                      {entry.location}
                    </span>
                  )}
                </div>
              </article>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
