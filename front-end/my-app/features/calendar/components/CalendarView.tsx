'use client';

import React, { useState, useMemo } from 'react';
import Link from 'next/link';
import { DayPicker, type DayButtonProps } from 'react-day-picker';
import { useJournal } from '@/features/journal';
import { useToast } from '@/lib/toast-context';
import { JournalEntry, MoodType } from '@/types';
import { WashiTape, PolaroidCard, StickerBadge } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { NotificationModal } from '@/components/ui/NotificationModal';
import {
  PlusCircle,
  Trash2,
  MapPin,
  ChevronLeft,
  ChevronRight,
  Lightbulb,
  Calendar,
  Clock,
  Sparkles,
  Bot,
  X,
  Compass,
  Tag,
  Search,
  BookOpen,
  Smile,
  Zap,
  Frown,
  CloudRain,
  Meh,
  Star,
  Edit3,
  Filter,
  RotateCcw,
} from 'lucide-react';

export function CalendarView() {
  const { entries, deleteEntry, toggleFavorite } = useJournal();
  const { showToast } = useToast();

  const [selectedEntry, setSelectedEntry] = useState<JournalEntry | null>(null);
  const [entryToDelete, setEntryToDelete] = useState<string | null>(null);
  const [selectedMood, setSelectedMood] = useState<string>('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedDate, setSelectedDate] = useState<string>('2026-10-15');
  const [filterByDate, setFilterByDate] = useState(false);
  const [onlyFavorites, setOnlyFavorites] = useState(false);
  const [currentMonth, setCurrentMonth] = useState<Date>(new Date(2026, 9, 1));

  // Index entries by date for fast calendar queries and stats [FR-JOURNAL-07]
  const entriesByDate = useMemo(() => {
    const map: Record<string, JournalEntry[]> = {};
    for (const entry of entries) {
      if (!map[entry.date]) map[entry.date] = [];
      map[entry.date].push(entry);
    }
    return map;
  }, [entries]);

  const selectedDateEntries = entriesByDate[selectedDate] || [];
  const selectedDateAvgScore = selectedDateEntries.length > 0
    ? (selectedDateEntries.reduce((sum, e) => sum + e.moodScore, 0) / selectedDateEntries.length).toFixed(1)
    : null;

  const filteredEntries = entries.filter((entry) => {
    const matchesMood = selectedMood === 'all' || entry.mood === selectedMood;
    const matchesFavorite = !onlyFavorites || !!entry.isFavorite;
    const matchesDate = !filterByDate || entry.date === selectedDate;
    const matchesSearch =
      entry.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      entry.content.toLowerCase().includes(searchTerm.toLowerCase()) ||
      entry.tags.some((t) => t.toLowerCase().includes(searchTerm.toLowerCase()));
    return matchesMood && matchesFavorite && matchesDate && matchesSearch;
  });

  const monthNames = [
    'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
    'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'
  ];
  const formattedMonthYear = `${monthNames[currentMonth.getMonth()]}, ${currentMonth.getFullYear()}`;

  const renderMoodIcon = (mood: MoodType | string | null, className: string = 'w-4 h-4') => {
    switch (mood) {
      case 'calm-joy':
        return <Smile className={`${className} text-green-700 stroke-[2.3]`} />;
      case 'hope-energy':
        return <Zap className={`${className} text-yellow-700 fill-yellow-400 stroke-[2.3]`} />;
      case 'anxiety-stress':
        return <Frown className={`${className} text-mood-anxiety-stress stroke-[2.3]`} />;
      case 'sadness-reflect':
        return <CloudRain className={`${className} text-blue-700 stroke-[2.3]`} />;
      case 'neutral':
        return <Meh className={`${className} text-gray-600 stroke-[2.3]`} />;
      default:
        return null;
    }
  };

  const handleDeleteConfirm = () => {
    if (entryToDelete) {
      deleteEntry(entryToDelete);
      setEntryToDelete(null);
      if (selectedEntry?.id === entryToDelete) {
        setSelectedEntry(null);
      }
      showToast({
        title: 'Đã xóa trang nhật ký!',
        message: 'Dữ liệu đã được gỡ bỏ và cập nhật vào thống kê.',
        type: 'info',
      });
    }
  };

  // Custom Day Button for Scrapbook Neo-brutalism aesthetics
  function CustomDayButton(props: DayButtonProps) {
    const { day, modifiers, ...buttonProps } = props;
    const isoDate = day.isoDate;
    const dayNum = day.date.getDate();
    const dayEntries = entriesByDate[isoDate] || [];
    const hasEntries = dayEntries.length > 0;
    const repMood = hasEntries ? dayEntries[0].mood : null;
    const isSelected = selectedDate === isoDate;
    const isOutside = day.outside;

    return (
      <button
        {...buttonProps}
        type="button"
        onClick={(e) => {
          buttonProps.onClick?.(e);
          setSelectedDate(isoDate);
          setFilterByDate(true);
        }}
        className={`w-full aspect-square p-1 border rounded-xl flex flex-col items-center justify-between text-xs transition-all cursor-pointer ${
          isOutside
            ? 'opacity-25 border-transparent text-gray-400'
            : isSelected
            ? 'bg-primary-container border-2 border-black font-extrabold shadow-neo-sm scale-105 z-10'
            : hasEntries
            ? 'bg-white border-2 border-black/80 hover:border-black shadow-neo-sm text-black font-bold'
            : 'border-border-soft hover:border-black bg-surface-container-lowest text-gray-700'
        }`}
      >
        <span className="font-mono text-[10px] font-bold">{dayNum}</span>
        {hasEntries && (
          <span className="leading-none flex items-center justify-center relative" title={`${dayEntries.length} bài viết • Tâm trạng: ${repMood}`}>
            {renderMoodIcon(repMood, 'w-3.5 h-3.5')}
            {dayEntries.length > 1 && (
              <span className="absolute -top-1 -right-2 text-[8px] bg-black text-white rounded-full w-3.5 h-3.5 flex items-center justify-center font-mono font-bold">
                {dayEntries.length}
              </span>
            )}
          </span>
        )}
      </button>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col gap-8 selection:bg-primary-container selection:text-black">
      {/* Top Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b-2 border-on-background">
        <div>
          <h1 className="font-space text-2xl sm:text-4xl font-extrabold text-on-surface">
            Lịch Ký Ức & Timeline Đa Tầng
          </h1>
          <p className="font-sans text-xs sm:text-sm text-on-surface-variant">
            Theo dõi dòng xúc cảm, lật mở từng trang nhật ký vật lý và xem chi tiết phân tích AI đã lưu
          </p>
        </div>

        <Link href="/journal-editor">
          <NeoButton size="md">
            Thêm trang mới
          </NeoButton>
        </Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* ========================================================== */}
        {/* LEFT COLUMN: Calendar & Filters (4 cols on lg)             */}
        {/* ========================================================== */}
        <div className="lg:col-span-4 flex flex-col gap-6">
          {/* CALENDAR CARD */}
          <div className="bg-surface-card border-neo rounded-3xl p-5 shadow-neo relative">
            <WashiTape color="lime" rotate={-2} className="absolute -top-3 left-8 w-28" />

            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-3">
              <div>
                <span className="font-space text-base font-extrabold text-on-surface block capitalize">
                  {formattedMonthYear}
                </span>
                <span className="font-mono text-[10px] text-gray-500 font-bold">FR-JOURNAL-07 / Daily Average</span>
              </div>
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1))}
                  className="p-1 border border-black rounded hover:bg-paper-warm text-xs font-bold flex items-center justify-center cursor-pointer shadow-neo-sm"
                  title="Tháng trước"
                >
                  <ChevronLeft className="w-3.5 h-3.5" />
                </button>
                <button
                  type="button"
                  onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1))}
                  className="p-1 border border-black rounded hover:bg-paper-warm text-xs font-bold flex items-center justify-center cursor-pointer shadow-neo-sm"
                  title="Tháng sau"
                >
                  <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* Interactive DayPicker Calendar */}
            <div className="daypicker-neo-wrapper">
              <DayPicker
                mode="single"
                month={currentMonth}
                onMonthChange={setCurrentMonth}
                showOutsideDays
                hideNavigation
                weekStartsOn={1}
                formatters={{
                  formatWeekdayName: (date) => ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'][date.getDay()],
                }}
                components={{
                  MonthCaption: () => <span className="hidden" />,
                  DayButton: CustomDayButton,
                }}
                classNames={{
                  root: 'w-full',
                  months: 'w-full',
                  month: 'w-full',
                  month_grid: 'w-full border-collapse table-fixed',
                  weekdays: 'w-full',
                  weekday: 'p-1 text-center font-space text-[11px] font-extrabold text-on-surface-variant pb-2',
                  weeks: 'w-full',
                  week: 'w-full',
                  day: 'p-0.5 text-center',
                }}
              />
            </div>

            {/* Daily Mood Average Indicator for Selected Date (FR-JOURNAL-07) */}
            <div className="mt-4 p-3 bg-paper-warm rounded-xl border border-black text-xs font-space flex items-center justify-between shadow-neo-sm">
              <div>
                <span className="text-[10px] text-gray-600 block uppercase font-bold">
                  Ngày {selectedDate}:
                </span>
                {selectedDateAvgScore ? (
                  <span className="font-extrabold text-black">
                    Trung bình ngày: {selectedDateAvgScore}/10 ({selectedDateEntries.length} bài viết)
                  </span>
                ) : (
                  <span className="font-bold text-gray-600">
                    Chưa có bài viết nhật ký trong ngày này
                  </span>
                )}
              </div>
              <span className="bg-primary-container px-2 py-0.5 rounded border border-black font-extrabold text-[11px] shadow-neo-sm">
                FR-07
              </span>
            </div>

            {/* Mood Legend */}
            <div className="mt-3 pt-3 border-t border-border-soft flex flex-wrap items-center justify-around text-[10px] font-space font-bold">
              <span className="flex items-center gap-1">
                <Smile className="w-3.5 h-3.5 text-green-700 stroke-[2.3]" /> Bình an
              </span>
              <span className="flex items-center gap-1">
                <Zap className="w-3.5 h-3.5 text-yellow-700 fill-yellow-400 stroke-[2.3]" /> Năng lượng
              </span>
              <span className="flex items-center gap-1">
                <Frown className="w-3.5 h-3.5 text-mood-anxiety-stress stroke-[2.3]" /> Căng thẳng
              </span>
              <span className="flex items-center gap-1">
                <CloudRain className="w-3.5 h-3.5 text-blue-700 stroke-[2.3]" /> Suy ngẫm
              </span>
            </div>
          </div>

          {/* SEARCH & FILTERS CARD */}
          <div className="bg-surface-card border-neo rounded-3xl p-5 shadow-neo flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <h2 className="font-space text-sm font-extrabold text-on-surface uppercase tracking-wider flex items-center gap-2">
                <Search className="w-4 h-4" />
                Tìm kiếm & Bộ Lọc
              </h2>

              {(filterByDate || onlyFavorites || selectedMood !== 'all' || searchTerm) && (
                <button
                  type="button"
                  onClick={() => {
                    setFilterByDate(false);
                    setOnlyFavorites(false);
                    setSelectedMood('all');
                    setSearchTerm('');
                  }}
                  className="text-[10px] font-space font-bold text-gray-500 hover:text-black flex items-center gap-0.5 cursor-pointer underline"
                >
                  <RotateCcw className="w-3 h-3" /> Đặt lại
                </button>
              )}
            </div>

            <div>
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Tìm từ khóa, thẻ hashtag..."
                className="w-full font-space text-xs p-2.5 rounded-xl border-neo-sm bg-white focus:outline-none shadow-sm"
              />
            </div>

            {/* Date filter active chip */}
            {filterByDate && (
              <div className="flex items-center justify-between bg-primary-container px-2.5 py-1.5 rounded-xl border border-black shadow-neo-sm text-xs font-space font-extrabold">
                <span className="flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5" />
                  Đang lọc theo ngày: {selectedDate}
                </span>
                <button
                  type="button"
                  onClick={() => setFilterByDate(false)}
                  className="p-0.5 hover:bg-black/10 rounded cursor-pointer"
                  title="Bỏ lọc ngày"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            )}

            {/* Mood Filters and Favorites Filter */}
            <div className="flex flex-wrap gap-2 pt-1">
              <button
                type="button"
                onClick={() => setOnlyFavorites(!onlyFavorites)}
                className={`text-xs font-space font-bold px-2.5 py-1 rounded-lg border transition-all cursor-pointer flex items-center gap-1.5 ${
                  onlyFavorites
                    ? 'bg-amber-300 text-black border-black shadow-neo-sm font-extrabold'
                    : 'bg-surface-container-low text-on-surface border-border-soft hover:border-black'
                }`}
              >
                <Star className={`w-3.5 h-3.5 stroke-[2.3] ${onlyFavorites ? 'fill-black text-black' : 'text-amber-500'}`} />
                <span>Yêu thích ({entries.filter((e) => e.isFavorite).length})</span>
              </button>

              {[
                { id: 'all', label: 'Tất cả' },
                { id: 'calm-joy', label: 'Bình an', icon: <Smile className="w-3.5 h-3.5 text-green-700 stroke-[2.3]" /> },
                { id: 'hope-energy', label: 'Năng lượng', icon: <Zap className="w-3.5 h-3.5 text-yellow-700 fill-yellow-400 stroke-[2.3]" /> },
                { id: 'anxiety-stress', label: 'Căng thẳng', icon: <Frown className="w-3.5 h-3.5 text-mood-anxiety-stress stroke-[2.3]" /> },
                { id: 'sadness-reflect', label: 'Suy ngẫm', icon: <CloudRain className="w-3.5 h-3.5 text-blue-700 stroke-[2.3]" /> },
              ].map((moodItem) => (
                <button
                  key={moodItem.id}
                  onClick={() => setSelectedMood(moodItem.id)}
                  className={`text-xs font-space font-bold px-2.5 py-1 rounded-lg border transition-all cursor-pointer flex items-center gap-1.5 ${
                    selectedMood === moodItem.id
                      ? 'bg-primary-container text-on-primary-container border-black shadow-neo-sm font-extrabold'
                      : 'bg-surface-container-low text-on-surface border-border-soft hover:border-black'
                  }`}
                >
                  {moodItem.icon}
                  <span>{moodItem.label}</span>
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* ========================================================== */}
        {/* RIGHT COLUMN: Timeline Stream & Polaroid Cards (8 cols)    */}
        {/* ========================================================== */}
        <div className="lg:col-span-8 flex flex-col gap-6">
          <div className="flex items-center justify-between">
            <span className="font-space text-xs font-extrabold uppercase tracking-wider text-gray-600">
              Hiển thị {filteredEntries.length} trang nhật ký phù hợp
            </span>
          </div>

          {filteredEntries.length === 0 ? (
            <div className="bg-surface-card border-neo rounded-3xl p-10 text-center flex flex-col items-center justify-center gap-3 shadow-neo">
              <Calendar className="w-10 h-10 text-gray-400 stroke-[1.5]" />
              <p className="font-space text-base font-extrabold text-black">
                Không tìm thấy bài viết nào phù hợp
              </p>
              <p className="text-xs text-gray-600 max-w-sm">
                Thử đổi điều kiện lọc hoặc tạo trang nhật ký mới cho ngày hôm nay.
              </p>
              <Link href="/journal-editor" className="mt-2">
                <NeoButton size="sm">Viết nhật ký ngay</NeoButton>
              </Link>
            </div>
          ) : (
            <div className="flex flex-col gap-6">
              {filteredEntries.map((entry, idx) => (
                <article
                  key={entry.id}
                  onClick={() => setSelectedEntry(entry)}
                  className="bg-surface-card border-neo rounded-3xl p-6 sm:p-7 shadow-neo hover:shadow-neo-lg transition-all relative flex flex-col gap-4 cursor-pointer group hover:-translate-y-0.5"
                >
                  {/* Scrapbook Tape decoration on alternating items */}
                  {idx % 2 === 0 ? (
                    <WashiTape color="peach" rotate={2} className="absolute -top-3 right-10 w-28" />
                  ) : (
                    <WashiTape color="blue" rotate={-1.5} className="absolute -top-3 left-10 w-28" />
                  )}

                  {/* Entry Header */}
                  <div className="flex items-center justify-between gap-3 border-b-2 border-border-soft pb-3">
                    <div className="flex items-center gap-3 min-w-0 flex-1">
                      <span className="w-10 h-10 rounded-2xl bg-paper-warm border-neo-sm flex items-center justify-center shadow-neo-sm shrink-0">
                        {renderMoodIcon(entry.mood, 'w-5 h-5')}
                      </span>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <h2 className="font-space text-lg sm:text-xl font-extrabold text-on-surface group-hover:text-primary transition-colors break-words leading-snug">
                            {entry.title}
                          </h2>
                          {entry.status === 'ANALYSIS_OUTDATED' && (
                            <span className="px-2 py-0.5 bg-amber-200 text-amber-900 border border-black rounded text-[10px] font-space font-extrabold shadow-neo-sm shrink-0">
                              Chờ cập nhật AI
                            </span>
                          )}
                          {entry.status === 'ANALYSIS_FAILED' && (
                            <span className="px-2 py-0.5 bg-red-200 text-red-900 border border-black rounded text-[10px] font-space font-extrabold shadow-neo-sm shrink-0">
                              AI Lỗi kết nối
                            </span>
                          )}
                        </div>

                        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs font-space font-bold text-on-surface-variant mt-1">
                          <span className="flex items-center gap-1 shrink-0">
                            <Calendar className="w-3.5 h-3.5" />
                            {entry.date}
                          </span>
                          <span>•</span>
                          <span className="flex items-center gap-1 shrink-0">
                            <Clock className="w-3.5 h-3.5" />
                            {entry.time}
                          </span>
                          {entry.location && (
                            <>
                              <span>•</span>
                              <span className="flex items-center gap-1 truncate">
                                <MapPin className="w-3.5 h-3.5 shrink-0" />
                                {entry.location}
                              </span>
                            </>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Action Buttons & Quantitative Chips */}
                    <div className="flex items-center gap-1.5 shrink-0">
                      {/* Favorite Button (FR-JOURNAL-09) */}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleFavorite(entry.id);
                          showToast({
                            title: entry.isFavorite ? 'Đã bỏ khỏi danh sách yêu thích' : 'Đã thêm vào mục yêu thích!',
                            type: 'info',
                          });
                        }}
                        className={`p-1.5 rounded-lg border transition-all cursor-pointer ${
                          entry.isFavorite
                            ? 'bg-amber-100 border-amber-500 text-amber-600 shadow-neo-sm'
                            : 'bg-white border-black/30 hover:border-black text-gray-400 hover:text-amber-500'
                        }`}
                        title={entry.isFavorite ? 'Bỏ yêu thích' : 'Đánh dấu yêu thích (FR-JOURNAL-09)'}
                      >
                        <Star className={`w-4 h-4 stroke-[2.3] ${entry.isFavorite ? 'fill-amber-400 text-amber-500' : ''}`} />
                      </button>

                      {/* Edit Button (FR-JOURNAL-03) */}
                      <Link
                        href={`/journal-editor?id=${entry.id}`}
                        onClick={(e) => e.stopPropagation()}
                        className="inline-flex items-center gap-1 px-2.5 py-1 bg-white hover:bg-paper-warm text-black border border-black rounded-lg text-xs font-space font-bold shadow-neo-sm transition-all cursor-pointer"
                        title="Chỉnh sửa bài viết (FR-JOURNAL-03)"
                      >
                        <Edit3 className="w-3.5 h-3.5 stroke-[2.3]" />
                        <span className="hidden sm:inline">Sửa</span>
                      </Link>

                      <span className="px-2.5 py-1 bg-primary-container text-black border border-black rounded-lg text-xs font-space font-extrabold whitespace-nowrap shadow-neo-sm">
                        Mood {entry.moodScore}/10
                      </span>

                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          setEntryToDelete(entry.id);
                        }}
                        className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors border border-transparent hover:border-red-300 ml-1 cursor-pointer"
                        title="Xóa trang nhật ký này (FR-JOURNAL-11)"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  {/* Entry Snippet Content */}
                  <p className="font-serif text-sm sm:text-base text-on-surface leading-relaxed line-clamp-3 italic">
                    &ldquo;{entry.content.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()}&rdquo;
                  </p>

                  {/* Optional Polaroid thumbnail preview */}
                  {entry.photoUrl && (
                    <div className="flex items-center gap-4 p-3 bg-paper-warm rounded-2xl border-neo-sm">
                      <img
                        src={entry.photoUrl}
                        alt={entry.title}
                        className="w-16 h-16 object-cover rounded-xl border border-black shadow-neo-sm shrink-0"
                      />
                      <div className="flex flex-col">
                        <span className="font-space text-xs font-bold text-black">
                          Ảnh Polaroid đính kèm
                        </span>
                        <span className="font-serif italic text-xs text-gray-600">
                          {entry.photoCaption || 'Kỷ niệm trang nhật ký'}
                        </span>
                      </div>
                    </div>
                  )}

                  {/* Footer tags and AI badge */}
                  <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-border-soft">
                    <div className="flex flex-wrap gap-1.5">
                      {entry.tags.map((tag, tIdx) => (
                        <span
                          key={tIdx}
                          className="font-space text-[11px] font-bold bg-surface-container-lowest px-2.5 py-0.5 rounded-md border border-black"
                        >
                          #{tag}
                        </span>
                      ))}
                    </div>

                    <span className="text-xs font-space font-extrabold text-primary flex items-center gap-1 group-hover:translate-x-1 transition-transform">
                      <BookOpen className="w-3.5 h-3.5" /> Xem chi tiết sổ tay & AI →
                    </span>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* ========================================================== */}
      {/* JOURNAL DETAIL MODAL (FR-JOURNAL-09)                       */}
      {/* ========================================================== */}
      {selectedEntry && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200"
          role="dialog"
          aria-modal="true"
        >
          {/* Outer Card with overflow-visible so WashiTape floats proudly without clipping */}
          <div className="relative w-full max-w-2xl max-h-[92vh] bg-paper-warm border-[2.5px] border-black rounded-3xl shadow-neo-lg flex flex-col overflow-visible">
            {/* WashiTape floats unclipped on top edge */}
            <WashiTape color="lime" rotate={-1} className="absolute -top-3.5 left-10 w-36 z-30 pointer-events-none" />

            {/* Close Button */}
            <button
              type="button"
              onClick={() => setSelectedEntry(null)}
              className="absolute top-4 right-4 p-1.5 rounded-xl hover:bg-black/10 transition-colors border border-transparent hover:border-black cursor-pointer z-20"
              aria-label="Đóng sổ tay"
            >
              <X className="w-5 h-5 text-black" />
            </button>

            {/* 1. Fixed Modal Header */}
            <div className="p-6 sm:p-7 pb-4 border-b-2 border-black shrink-0 flex flex-col gap-2">
              <div className="flex items-center justify-between pr-8">
                <div className="flex items-center gap-2 text-xs font-space font-bold text-gray-700">
                  <span className="bg-white px-2 py-0.5 rounded border border-black">{selectedEntry.date}</span>
                  <span>•</span>
                  <span>{selectedEntry.time}</span>
                  {selectedEntry.location && <span>• {selectedEntry.location}</span>}
                </div>

                <div className="flex items-center gap-2">
                  {/* Favorite Toggle Button inside Modal */}
                  <button
                    type="button"
                    onClick={() => {
                      toggleFavorite(selectedEntry.id);
                      setSelectedEntry({ ...selectedEntry, isFavorite: !selectedEntry.isFavorite });
                      showToast({
                        title: !selectedEntry.isFavorite ? 'Đã thêm vào mục yêu thích!' : 'Đã bỏ khỏi danh sách yêu thích',
                        type: 'info',
                      });
                    }}
                    className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-xl border border-black font-space text-xs font-bold transition-all shadow-neo-sm cursor-pointer ${
                      selectedEntry.isFavorite ? 'bg-amber-300 text-black' : 'bg-white text-gray-700 hover:bg-gray-100'
                    }`}
                  >
                    <Star className={`w-3.5 h-3.5 stroke-[2.3] ${selectedEntry.isFavorite ? 'fill-amber-500 text-amber-600' : ''}`} />
                    <span>{selectedEntry.isFavorite ? 'Đã yêu thích' : 'Yêu thích'}</span>
                  </button>
                </div>
              </div>

              <h2 className="font-space text-2xl sm:text-3xl font-extrabold text-black leading-tight mt-1">
                {selectedEntry.title}
              </h2>

              <div className="flex flex-wrap items-center gap-2 mt-1">
                <span className="px-3 py-1 bg-primary-container text-black font-space text-xs font-extrabold rounded-lg border border-black shadow-neo-sm">
                  Tâm trạng: {selectedEntry.moodScore}/10
                </span>
                <span className="px-3 py-1 bg-white text-black font-space text-xs font-bold rounded-lg border border-black shadow-neo-sm">
                  Áp lực: {selectedEntry.stressScore}/10
                </span>
                <span className="px-3 py-1 bg-white text-black font-space text-xs font-bold rounded-lg border border-black shadow-neo-sm">
                  Năng lượng: {selectedEntry.energyScore}/10
                </span>
                {selectedEntry.status === 'ANALYSIS_OUTDATED' && (
                  <span className="px-3 py-1 bg-amber-200 text-amber-900 font-space text-xs font-extrabold rounded-lg border border-black shadow-neo-sm">
                    Chờ cập nhật AI (Outdated)
                  </span>
                )}
                {selectedEntry.status === 'ANALYSIS_FAILED' && (
                  <span className="px-3 py-1 bg-red-200 text-red-900 font-space text-xs font-extrabold rounded-lg border border-black shadow-neo-sm">
                    Lỗi kết nối AI
                  </span>
                )}
              </div>
            </div>

            {/* 2. Scrollable Modal Body */}
            <div className="p-6 sm:p-7 py-5 overflow-y-auto custom-scrollbar flex-1 flex flex-col gap-6">
              {/* Full Content */}
              <div className="p-4 sm:p-6 bg-white rounded-2xl border-2 border-black shadow-inner">
                {selectedEntry.content.includes('<') ? (
                  <div
                    className="font-serif text-base sm:text-lg text-on-surface leading-loose tiptap"
                    dangerouslySetInnerHTML={{ __html: selectedEntry.content }}
                  />
                ) : (
                  <p className="font-serif text-base sm:text-lg text-on-surface leading-loose italic whitespace-pre-line">
                    {selectedEntry.content}
                  </p>
                )}
              </div>

              {/* Polaroid Attachment if present */}
              {selectedEntry.photoUrl && (
                <div className="flex justify-center">
                  <PolaroidCard
                    imageUrl={selectedEntry.photoUrl}
                    caption={selectedEntry.photoCaption || selectedEntry.title}
                    date={selectedEntry.date}
                    rotate={1}
                    className="w-72"
                  />
                </div>
              )}

              {/* Associated AI Analysis (FR-JOURNAL-09) */}
              {selectedEntry.aiAnalysis && (
                <div className="p-5 bg-surface-card rounded-2xl border-2 border-black shadow-neo-sm flex flex-col gap-4">
                  <div className="flex items-center justify-between border-b border-black pb-2">
                    <div className="flex items-center gap-2">
                      <Bot className="w-5 h-5 text-black" />
                      <span className="font-space text-sm font-extrabold uppercase text-black">
                        Phân Tích AI Đã Lưu (Structured Analysis)
                      </span>
                    </div>
                    {selectedEntry.aiAnalysis.userCorrected && (
                      <span className="px-2 py-0.5 bg-lime-300 text-black border border-black rounded text-[10px] font-space font-extrabold">
                        ✓ Đã hiệu chỉnh bởi bạn
                      </span>
                    )}
                  </div>

                  <div>
                    <span className="font-space text-xs font-bold text-gray-500 uppercase block">Sắc thái tổng quan:</span>
                    <span className="font-space text-sm font-extrabold text-primary">
                      {selectedEntry.aiAnalysis.sentiment}
                    </span>
                  </div>

                  <div className="flex flex-col gap-2">
                    <span className="font-space text-xs font-bold text-gray-500 uppercase">Phổ cảm xúc:</span>
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                      {selectedEntry.aiAnalysis.emotions.map((em, i) => (
                        <div key={i} className="p-2 bg-paper-warm rounded-lg border border-black text-xs font-space">
                          <div className="flex justify-between font-bold">
                            <span>{em.label}</span>
                            <span>{em.percentage}%</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {selectedEntry.aiAnalysis.reflectionQuestions && (
                    <div className="flex flex-col gap-1.5">
                      <span className="font-space text-xs font-bold text-gray-500 uppercase">Câu hỏi phản tư:</span>
                      <ul className="list-disc list-inside text-xs font-serif italic text-gray-800 space-y-1">
                        {selectedEntry.aiAnalysis.reflectionQuestions.map((q, qi) => (
                          <li key={qi}>&ldquo;{q}&rdquo;</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* 3. Fixed Modal Footer */}
            <div className="p-5 sm:p-6 pt-3 border-t-2 border-black shrink-0 flex flex-wrap items-center justify-between gap-3 bg-paper-warm rounded-b-3xl">
              <button
                type="button"
                onClick={() => {
                  setEntryToDelete(selectedEntry.id);
                }}
                className="text-xs font-space font-bold text-red-600 hover:text-red-800 underline flex items-center gap-1 cursor-pointer"
              >
                <Trash2 className="w-3.5 h-3.5" /> Xóa trang này (FR-JOURNAL-11)
              </button>

              <div className="flex items-center gap-2">
                <Link
                  href={`/journal-editor?id=${selectedEntry.id}`}
                  className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-paper-warm hover:bg-amber-100 text-black border border-black rounded-xl font-space font-bold text-xs shadow-neo-sm transition-all cursor-pointer"
                >
                  <Edit3 className="w-3.5 h-3.5 stroke-[2.3]" />
                  <span>Chỉnh sửa bài viết (FR-JOURNAL-03)</span>
                </Link>

                <NeoButton
                  variant="primary"
                  size="sm"
                  onClick={() => setSelectedEntry(null)}
                  className="font-space font-bold text-xs"
                >
                  Đóng sổ tay
                </NeoButton>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Delete Modal with Recalculation Warning (FR-JOURNAL-11) */}
      <NotificationModal
        isOpen={!!entryToDelete}
        onClose={() => setEntryToDelete(null)}
        onConfirm={handleDeleteConfirm}
        type="danger"
        title="Xóa trang nhật ký này?"
        description="Việc xóa nhật ký có thể làm thay đổi các thống kê lịch sử và insight hiện tại của bạn. Hành động này không thể hoàn tác."
        confirmLabel="Xác nhận xóa vĩnh viễn"
        cancelLabel="Giữ lại trang này"
        washiColor="peach"
      />
    </div>
  );
}
