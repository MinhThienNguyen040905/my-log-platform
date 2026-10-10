'use client';

import React, { useState, useMemo, useEffect, useRef } from 'react';
import Link from 'next/link';
import { DayPicker, type DayButtonProps } from 'react-day-picker';
import { useJournal, LegacyJournalImport, journalEmotionLabel, JOURNAL_EMOTIONS, JOURNAL_EMOTION_ORDER, normalizeJournalEmotion } from '@/features/journal';
import { useToast } from '@/lib/toast-context';
import { JournalEntry, JournalEmotion, MoodType } from '@/types';
import { WashiTape, PolaroidCard } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { NotificationModal } from '@/components/ui/NotificationModal';
import {
  Trash2,
  ChevronLeft,
  ChevronRight,
  Calendar,
  Clock,
  Bot,
  X,
  Search,
  BookOpen,
  Star,
  Edit3,
  RotateCcw,
  SlidersHorizontal,
} from 'lucide-react';

type HistoryFilters = {
  date: string | null;
  emotion: JournalEmotion | 'all';
  favorites: boolean;
  search: string;
};

const EMPTY_FILTERS: HistoryFilters = { date: null, emotion: 'all', favorites: false, search: '' };

export function CalendarView() {
  const { entries, deleteEntry, toggleFavorite, loadEntry, entriesLoading, entriesError, reloadEntries } = useJournal();
  const { showToast } = useToast();

  const [selectedEntry, setSelectedEntry] = useState<JournalEntry | null>(null);
  const [entryToDelete, setEntryToDelete] = useState<string | null>(null);
  const [selectedMood, setSelectedMood] = useState<JournalEmotion | 'all'>('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedDate, setSelectedDate] = useState<string>(() => {
    const today = new Date();
    return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  });
  const [filterByDate, setFilterByDate] = useState(false);
  const selectedDateLabel = selectedDate.split('-').reverse().join('/');
  const [onlyFavorites, setOnlyFavorites] = useState(false);
  const [appliedFilters, setAppliedFilters] = useState<HistoryFilters>(EMPTY_FILTERS);
  const [showMoreFilters, setShowMoreFilters] = useState(false);
  const [showPreviews, setShowPreviews] = useState(false);
  const hasActiveFilters = !!appliedFilters.date || appliedFilters.favorites || appliedFilters.emotion !== 'all' || !!appliedFilters.search;
  const hasPendingFilters = (filterByDate ? selectedDate : null) !== appliedFilters.date
    || onlyFavorites !== appliedFilters.favorites
    || selectedMood !== appliedFilters.emotion
    || searchTerm.trim() !== appliedFilters.search;
  const applyFilters = () => setAppliedFilters({
    date: filterByDate ? selectedDate : null,
    emotion: selectedMood,
    favorites: onlyFavorites,
    search: searchTerm.trim(),
  });
  const clearFilters = () => {
    setFilterByDate(false);
    setOnlyFavorites(false);
    setSelectedMood('all');
    setSearchTerm('');
    setAppliedFilters(EMPTY_FILTERS);
  };
  const [currentMonth, setCurrentMonth] = useState<Date>(() => {
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth(), 1);
  });
  const detailDialogRef = useRef<HTMLDivElement>(null);
  const entryTriggerRef = useRef<HTMLButtonElement | null>(null);
  const isDetailOpen = selectedEntry !== null;

  useEffect(() => {
    if (!isDetailOpen) return;
    detailDialogRef.current?.focus();
    return () => entryTriggerRef.current?.focus();
  }, [isDetailOpen]);

  const handleDetailKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape') {
      event.stopPropagation();
      setSelectedEntry(null);
      return;
    }
    if (event.key !== 'Tab') return;
    const controls = detailDialogRef.current?.querySelectorAll<HTMLElement>('button, a[href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
    if (!controls?.length) return;
    const first = controls[0];
    const last = controls[controls.length - 1];
    if (event.shiftKey && (document.activeElement === first || document.activeElement === detailDialogRef.current)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  };

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
  const scoredDateEntries = selectedDateEntries.filter((entry) => entry.moodScore !== null);
  const selectedDateAvgScore = scoredDateEntries.length > 0
    ? (scoredDateEntries.reduce((sum, e) => sum + (e.moodScore ?? 0), 0) / scoredDateEntries.length).toFixed(1)
    : null;

  const filteredEntries = entries.filter((entry) => {
    const matchesMood = appliedFilters.emotion === 'all'
      || (entry.emotion ? normalizeJournalEmotion(entry.emotion) === appliedFilters.emotion
        : (appliedFilters.emotion === 'neutral' && entry.mood === 'neutral')
          || (appliedFilters.emotion === 'happy' && (entry.mood === 'calm-joy' || entry.mood === 'hope-energy'))
          || (appliedFilters.emotion === 'angry' && entry.mood === 'anxiety-stress')
          || (appliedFilters.emotion === 'sad' && entry.mood === 'sadness-reflect'));
    const matchesFavorite = !appliedFilters.favorites || !!entry.isFavorite;
    const matchesDate = !appliedFilters.date || entry.date === appliedFilters.date;
    const matchesSearch =
      entry.title.toLowerCase().includes(appliedFilters.search.toLowerCase()) ||
      entry.content.replace(/<[^>]*>/g, ' ').toLowerCase().includes(appliedFilters.search.toLowerCase()) ||
      entry.tags.some((t) => t.toLowerCase().includes(appliedFilters.search.toLowerCase()));
    return matchesMood && matchesFavorite && matchesDate && matchesSearch;
  });

  const monthNames = [
    'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
    'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'
  ];
  const formattedMonthYear = `${monthNames[currentMonth.getMonth()]}, ${currentMonth.getFullYear()}`;

  const renderMoodIcon = (mood: MoodType | string | null, className: string = 'w-4 h-4', emotion?: JournalEntry['emotion']) => {
    const resolved = normalizeJournalEmotion(emotion) ?? (
      mood === 'calm-joy' || mood === 'hope-energy' ? 'happy'
        : mood === 'anxiety-stress' ? 'angry'
          : mood === 'sadness-reflect' ? 'sad'
            : 'neutral'
    );
    const Icon = JOURNAL_EMOTIONS[resolved].icon;
    return <Icon className={`${className} stroke-[2.2]`} aria-hidden="true" />;
  };

  const handleDeleteConfirm = async () => {
    if (entryToDelete) {
      const entry = entries.find((item) => item.id === entryToDelete);
      if (!entry || entry.rowVersion === undefined) return false;
      try { await deleteEntry(entryToDelete, entry.rowVersion); }
      catch (error) {
        showToast({ title: 'Chưa thể xóa bài viết', message: error instanceof Error ? error.message : 'Vui lòng thử lại.', type: 'error' });
        return false;
      }
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
    return true;
  };

  const openEntry = async (entry: JournalEntry) => {
    try { setSelectedEntry(await loadEntry(entry.id)); }
    catch (error) { showToast({ title: 'Không thể mở bài viết', message: error instanceof Error ? error.message : 'Vui lòng thử lại.', type: 'error' }); }
  };

  const favoriteEntry = async (entry: JournalEntry) => {
    try {
      const saved = await toggleFavorite(entry.id);
      setSelectedEntry((current) => current?.id === saved.id ? { ...current, isFavorite: saved.isFavorite, rowVersion: saved.rowVersion } : current);
      showToast({ title: saved.isFavorite ? 'Đã thêm vào mục yêu thích' : 'Đã bỏ khỏi danh sách yêu thích', type: 'info' });
    } catch (error) { showToast({ title: 'Chưa thể cập nhật yêu thích', message: error instanceof Error ? error.message : 'Vui lòng thử lại.', type: 'error' }); }
  };

  // Custom Day Button for Scrapbook Neo-brutalism aesthetics
  function CustomDayButton(props: DayButtonProps) {
    const { day, modifiers, ...buttonProps } = props;
    const isoDate = day.isoDate;
    const dayNum = day.date.getDate();
    const dayEntries = entriesByDate[isoDate] || [];
    const hasEntries = dayEntries.length > 0;
    const isSelected = filterByDate && selectedDate === isoDate;
    const isOutside = modifiers.outside;

    return (
      <button
        {...buttonProps}
        type="button"
        onClick={(e) => {
          buttonProps.onClick?.(e);
          setSelectedDate(isoDate);
          setFilterByDate(true);
        }}
        aria-label={`${dayNum}/${day.date.getMonth() + 1}/${day.date.getFullYear()}: ${dayEntries.length} bài viết`}
        aria-pressed={isSelected}
        className={`w-full aspect-square p-1 border rounded-xl flex flex-col items-center justify-between text-xs transition-all cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${
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
          <span className="leading-none flex items-center justify-center w-5 h-5 rounded-full bg-black text-white text-[10px] font-mono font-bold" aria-hidden="true">
            {dayEntries.length}
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
            History & Calendar
          </h1>
          <p className="font-sans text-xs sm:text-sm text-on-surface-variant">
            Xem lại những ngày bạn đã viết và tìm trang nhật ký theo cảm xúc.
          </p>
        </div>

        <Link href="/journal-editor">
          <NeoButton size="md">
            Thêm trang mới
          </NeoButton>
        </Link>
      </div>

      {/* Search and filters appear before both the calendar and the history list. */}
      <LegacyJournalImport />
      <div className="bg-surface-card border border-border-soft rounded-2xl p-4 shadow-sm flex flex-col gap-3" role="search" aria-label="Tìm và lọc nhật ký">
            {(hasActiveFilters || hasPendingFilters) && <div className="flex items-center justify-end">
                <button
                  type="button"
                  onClick={clearFilters}
                  className="min-h-11 px-2 text-xs font-space font-bold text-gray-700 hover:text-black flex items-center gap-1 cursor-pointer underline"
                >
                  <RotateCcw className="w-3 h-3" /> Đặt lại
                </button>
            </div>}

            <form onSubmit={(event) => { event.preventDefault(); applyFilters(); }} className="flex items-stretch gap-2">
              <input
                type="text"
                aria-label="Tìm trong nhật ký"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Tìm kiếm sẽ khả dụng khi API hỗ trợ"
                disabled
                className="min-w-0 flex-1 min-h-11 font-space text-sm p-2.5 rounded-xl border border-border-soft bg-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
              />
              <button type="submit" aria-label="Tìm và lọc" title="Tìm và lọc" className="w-11 h-11 shrink-0 flex items-center justify-center rounded-xl border border-black bg-primary-container text-black cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black">
                <Search className="w-5 h-5" />
              </button>
            </form>
            <p className="text-xs font-space text-gray-600">Tìm nội dung chưa khả dụng. Các bộ lọc ngày, cảm xúc và yêu thích áp dụng sau khi tải xong danh sách.</p>

            {/* Date filter active chip */}
            {filterByDate && (
              <div className="flex items-center justify-between bg-primary-container px-2.5 py-1.5 rounded-xl border border-black text-xs font-space font-extrabold">
                <span className="flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5" />
                  Ngày đã chọn: {selectedDateLabel}
                </span>
                <button
                  type="button"
                  onClick={() => setFilterByDate(false)}
                  className="w-11 h-11 flex items-center justify-center hover:bg-black/10 rounded cursor-pointer"
                  aria-label="Bỏ lọc ngày"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            )}

            <button type="button" onClick={() => setShowMoreFilters(!showMoreFilters)} aria-expanded={showMoreFilters} aria-controls="history-extra-filters" className="sm:hidden min-h-11 inline-flex items-center gap-2 self-start px-3 rounded-lg border border-black font-space text-sm font-bold">
              <SlidersHorizontal className="w-4 h-4" /> {showMoreFilters ? 'Ẩn bộ lọc' : 'Lọc cảm xúc và yêu thích'}
            </button>
            <div id="history-extra-filters" className={`${showMoreFilters ? 'flex' : 'hidden'} sm:flex flex-wrap gap-2 pt-1`}>
              <button
                type="button"
                onClick={() => setOnlyFavorites(!onlyFavorites)}
                aria-pressed={onlyFavorites}
                className={`min-h-11 text-xs font-space font-bold px-3 py-2 rounded-lg border transition-all cursor-pointer flex items-center gap-1.5 ${
                  onlyFavorites
                    ? 'bg-amber-300 text-black border-black font-extrabold'
                    : 'bg-surface-container-low text-on-surface border-border-soft hover:border-black'
                }`}
              >
                <Star className={`w-3.5 h-3.5 stroke-[2.3] ${onlyFavorites ? 'fill-black text-black' : 'text-amber-500'}`} />
                <span>Yêu thích ({entries.filter((e) => e.isFavorite).length})</span>
              </button>

              {(['all', ...JOURNAL_EMOTION_ORDER] as const).map((emotion) => {
                const Icon = emotion === 'all' ? null : JOURNAL_EMOTIONS[emotion].icon;
                return (
                <button
                  key={emotion}
                  type="button"
                  onClick={() => setSelectedMood(emotion)}
                  aria-pressed={selectedMood === emotion}
                  className={`min-h-11 text-xs font-space font-bold px-3 py-2 rounded-lg border transition-all cursor-pointer flex items-center gap-1.5 ${
                    selectedMood === emotion
                      ? 'bg-primary-container text-on-primary-container border-black font-extrabold'
                      : 'bg-surface-container-low text-on-surface border-border-soft hover:border-black'
                  }`}
                >
                  {Icon && <Icon className="w-4 h-4" aria-hidden="true" />}
                  <span>{emotion === 'all' ? 'Tất cả' : JOURNAL_EMOTIONS[emotion].label}</span>
                </button>
              ); })}
            </div>
            {hasPendingFilters && <p className="text-xs font-space text-gray-600">Bấm kính lúp hoặc Enter để áp dụng lựa chọn.</p>}
          </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {entriesLoading && <p className="lg:col-span-12 font-space text-sm" role="status">Đang tải nhật ký...</p>}
        {entriesError && <div className="lg:col-span-12 font-space text-sm text-red-700">{entriesError} <button type="button" className="underline" onClick={() => void reloadEntries()}>Thử lại</button></div>}
        {/* ========================================================== */}
        {/* LEFT COLUMN: Calendar (4 cols on lg)                       */}
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
              </div>
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => {
                    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1));
                    setFilterByDate(false);
                  }}
                  className="w-11 h-11 border border-black rounded-lg hover:bg-paper-warm flex items-center justify-center cursor-pointer shadow-neo-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
                  aria-label="Tháng trước"
                >
                  <ChevronLeft className="w-3.5 h-3.5" />
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1));
                    setFilterByDate(false);
                  }}
                  className="w-11 h-11 border border-black rounded-lg hover:bg-paper-warm flex items-center justify-center cursor-pointer shadow-neo-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
                  aria-label="Tháng sau"
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
                onMonthChange={(month) => {
                  setCurrentMonth(month);
                  setFilterByDate(false);
                }}
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
            <div className="mt-4 p-3 bg-paper-warm rounded-xl border border-black text-xs font-space shadow-neo-sm">
              <div>
                {!filterByDate ? (
                  <span className="font-bold text-gray-600">Chọn một ngày để xem bài viết và cảm xúc đã ghi.</span>
                ) : selectedDateAvgScore ? (
                  <span className="font-extrabold text-black">
                    Ngày {selectedDateLabel}: {selectedDateEntries.length} bài viết · Điểm cảm xúc trung bình từ các bài viết: {selectedDateAvgScore}/10
                  </span>
                ) : (
                  <span className="font-bold text-gray-600">
                    Ngày {selectedDateLabel}: Chưa có bài viết nhật ký.
                  </span>
                )}
              </div>
            </div>

            {hasPendingFilters && filterByDate && <p className="mt-2 text-xs font-space text-gray-600">Ngày đã chọn. Bấm kính lúp ở bộ lọc phía trên để xem kết quả.</p>}

            <p className="mt-3 text-xs font-space text-gray-600">Số trong ô ngày là số bài viết đã ghi.</p>
          </div>


        </div>

        {/* ========================================================== */}
        {/* RIGHT COLUMN: Timeline Stream & Polaroid Cards (8 cols)    */}
        {/* ========================================================== */}
        <div className="lg:col-span-8 flex flex-col gap-6">
          <div className="flex items-center justify-between">
            <span className="font-space text-xs font-extrabold text-gray-700">
              {filteredEntries.length} bài viết{appliedFilters.date ? ` · Ngày ${appliedFilters.date.split('-').reverse().join('/')}` : ''}
              {appliedFilters.favorites ? ' · Yêu thích' : ''}
              {appliedFilters.emotion !== 'all' ? ` · ${JOURNAL_EMOTIONS[appliedFilters.emotion].label}` : ''}
              {appliedFilters.search ? ' · Kết quả tìm kiếm' : ''}
            </span>
            {hasActiveFilters && (
              <button type="button" onClick={clearFilters} className="min-h-11 px-2 text-xs font-space font-bold underline text-gray-700 hover:text-black cursor-pointer">
                Bỏ lọc
              </button>
            )}
          </div>

          {entries.length > 0 && (
            <label className="inline-flex min-h-11 items-center gap-2 self-start font-space text-xs font-bold text-gray-700 cursor-pointer">
              <input type="checkbox" checked={showPreviews} onChange={(event) => setShowPreviews(event.target.checked)} className="h-4 w-4 accent-black" />
              Hiện cảm xúc và chỉ số
            </label>
          )}

          {!entriesLoading && !entriesError && filteredEntries.length === 0 ? (
            <div className="bg-surface-card border-neo rounded-3xl p-10 text-center flex flex-col items-center justify-center gap-3 shadow-neo">
              <Calendar className="w-10 h-10 text-gray-400 stroke-[1.5]" />
              <p className="font-space text-base font-extrabold text-black">{entries.length === 0 ? 'Bạn chưa có trang nhật ký nào' : 'Không tìm thấy bài viết phù hợp'}</p>
              <p className="text-xs text-gray-600 max-w-sm">
                {entries.length === 0 ? 'Viết trang đầu tiên để bắt đầu lưu lại những ngày của bạn.' : 'Thử đổi từ khóa hoặc bỏ các điều kiện lọc hiện tại.'}
              </p>
              {entries.length === 0 ? <Link href="/journal-editor" className="mt-2"><NeoButton size="sm">Viết trang đầu tiên</NeoButton></Link> : <button type="button" onClick={clearFilters} className="mt-2 min-h-11 px-4 border border-black rounded-lg bg-primary-container font-space text-sm font-bold shadow-neo-sm">Bỏ bộ lọc</button>}
            </div>
          ) : (
            <div className="flex flex-col gap-6">
              {filteredEntries.map((entry, idx) => (
                <article
                  key={entry.id}
                  className="bg-surface-card border-neo rounded-3xl p-6 sm:p-7 shadow-neo hover:shadow-neo-lg transition-all relative flex flex-col gap-4 group hover:-translate-y-0.5"
                >
                  {/* Scrapbook Tape decoration on alternating items */}
                  {idx % 2 === 0 ? (
                    <WashiTape color="peach" rotate={2} className="absolute -top-3 right-10 w-28" />
                  ) : (
                    <WashiTape color="blue" rotate={-1.5} className="absolute -top-3 left-10 w-28" />
                  )}

                  {/* Entry Header */}
                  <div className="flex flex-wrap items-center justify-between gap-3 border-b-2 border-border-soft pb-3">
                    <div className="flex items-center gap-3 min-w-0 flex-1">
                      <span className="w-10 h-10 rounded-2xl bg-paper-warm border-neo-sm flex items-center justify-center shadow-neo-sm shrink-0">
                        {showPreviews ? renderMoodIcon(entry.mood, 'w-5 h-5', entry.emotion) : <BookOpen className="w-5 h-5" />}
                      </span>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <h2 className="font-space text-lg sm:text-xl font-extrabold text-on-surface group-hover:text-primary transition-colors break-words leading-snug">
                          <button type="button" data-open-entry onClick={(event) => {
                              event.stopPropagation();
                              entryTriggerRef.current = event.currentTarget;
                              void openEntry(entry);
                            }} className="text-left cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black rounded">
                              {entry.title}
                            </button>
                          </h2>
                          {showPreviews && entry.status === 'ANALYSIS_OUTDATED' && (
                            <span className="px-2 py-0.5 bg-amber-200 text-amber-900 border border-black rounded text-[10px] font-space font-extrabold shadow-neo-sm shrink-0">
                              Gợi ý cần cập nhật
                            </span>
                          )}
                          {showPreviews && entry.status === 'ANALYSIS_FAILED' && (
                            <span className="px-2 py-0.5 bg-red-200 text-red-900 border border-black rounded text-[10px] font-space font-extrabold shadow-neo-sm shrink-0">
                              Chưa thể tạo gợi ý
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
                        </div>
                      </div>
                    </div>

                    {/* Action Buttons & Quantitative Chips */}
                    <div className="flex flex-wrap items-center gap-1.5 ml-auto">
                      {/* Favorite Button (FR-JOURNAL-09) */}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          void favoriteEntry(entry);
                        }}
                        className={`w-11 h-11 flex items-center justify-center rounded-lg border transition-all cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${
                          entry.isFavorite
                            ? 'bg-amber-100 border-amber-500 text-amber-600 shadow-neo-sm'
                            : 'bg-white border-black/30 hover:border-black text-gray-400 hover:text-amber-500'
                        }`}
                        aria-label={entry.isFavorite ? 'Bỏ yêu thích' : 'Đánh dấu yêu thích'}
                      >
                        <Star className={`w-4 h-4 stroke-[2.3] ${entry.isFavorite ? 'fill-amber-400 text-amber-500' : ''}`} />
                      </button>

                      {/* Edit Button (FR-JOURNAL-03) */}
                      <Link
                        href={`/journal-editor?id=${entry.id}`}
                        onClick={(e) => e.stopPropagation()}
                        className="hidden sm:inline-flex min-h-11 items-center gap-1 px-2.5 py-1 bg-white hover:bg-paper-warm text-black border border-black rounded-lg text-xs font-space font-bold shadow-neo-sm transition-all cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
                        aria-label={`Sửa bài viết ${entry.title}`}
                      >
                        <Edit3 className="w-3.5 h-3.5 stroke-[2.3]" />
                        <span className="hidden sm:inline">Sửa</span>
                      </Link>

                      {showPreviews && <span className="px-2.5 py-1 bg-primary-container text-black border border-black rounded-lg text-xs font-space font-extrabold whitespace-nowrap shadow-neo-sm">{journalEmotionLabel(entry)}{entry.moodScore !== null ? ` · ${entry.moodScore}/10` : ''}</span>}

                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          setEntryToDelete(entry.id);
                        }}
                        className="hidden sm:flex w-11 h-11 items-center justify-center rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors border border-transparent hover:border-red-300 ml-1 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
                        aria-label={`Xóa bài viết ${entry.title}`}
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  {/* Entry Snippet Content */}
                  {showPreviews && entry.content && <p className="font-serif text-sm sm:text-base text-on-surface leading-relaxed line-clamp-3 italic">&ldquo;{entry.content.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()}&rdquo;</p>}

                  {/* Optional Polaroid thumbnail preview */}
                  {showPreviews && entry.photoUrl && (
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
                    {showPreviews && <div className="flex flex-wrap gap-1.5">
                      {entry.tags.map((tag, tIdx) => (
                        <span
                          key={tIdx}
                          className="font-space text-[11px] font-bold bg-surface-container-lowest px-2.5 py-0.5 rounded-md border border-black"
                        >
                          #{tag}
                        </span>
                      ))}
                    </div>}

                    <button type="button" onClick={(event) => { entryTriggerRef.current = event.currentTarget; void openEntry(entry); }} className="min-h-11 px-2 text-xs font-space font-extrabold text-primary flex items-center gap-1 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black">
                      <BookOpen className="w-3.5 h-3.5" /> Xem bài viết →
                    </button>
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
          ref={detailDialogRef}
          tabIndex={-1}
          onKeyDown={handleDetailKeyDown}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200"
          role="dialog"
          aria-modal="true"
          aria-labelledby="journal-detail-title"
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
              <div className="flex flex-wrap items-center justify-between gap-2 pr-8">
                <div className="flex items-center gap-2 text-xs font-space font-bold text-gray-700">
                  <span className="bg-white px-2 py-0.5 rounded border border-black">{selectedEntry.date}</span>
                  <span>•</span>
                  <span>{selectedEntry.time}</span>
                </div>

                <div className="flex items-center gap-2">
                  {/* Favorite Toggle Button inside Modal */}
                  <button
                    type="button"
                    onClick={() => {
                      void favoriteEntry(selectedEntry);
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

              <h2 id="journal-detail-title" className="font-space text-2xl sm:text-3xl font-extrabold text-black leading-tight mt-1">
                {selectedEntry.title}
              </h2>

              <div className="flex flex-wrap items-center gap-2 mt-1">
                <span className="px-3 py-1 bg-primary-container text-black font-space text-xs font-extrabold rounded-lg border border-black shadow-neo-sm">
                  Cảm xúc: {journalEmotionLabel(selectedEntry)}{selectedEntry.moodScore !== null ? ` · ${selectedEntry.moodScore}/10` : ''}
                </span>
                <span className="px-3 py-1 bg-white text-black font-space text-xs font-bold rounded-lg border border-black shadow-neo-sm">
                  Áp lực: {selectedEntry.stressScore === null ? 'Chưa tải' : `${selectedEntry.stressScore}/10`}
                </span>
                <span className="px-3 py-1 bg-white text-black font-space text-xs font-bold rounded-lg border border-black shadow-neo-sm">
                  Năng lượng: {selectedEntry.energyScore === null ? 'Chưa tải' : `${selectedEntry.energyScore}/10`}
                </span>
                {selectedEntry.status === 'ANALYSIS_OUTDATED' && (
                  <span className="px-3 py-1 bg-amber-200 text-amber-900 font-space text-xs font-extrabold rounded-lg border border-black shadow-neo-sm">
                    Gợi ý cần cập nhật
                  </span>
                )}
                {selectedEntry.status === 'ANALYSIS_FAILED' && (
                  <span className="px-3 py-1 bg-red-200 text-red-900 font-space text-xs font-extrabold rounded-lg border border-black shadow-neo-sm">
                    Chưa thể tạo gợi ý
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
                        Gợi ý nhìn lại đã lưu
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
                  setSelectedEntry(null);
                  setEntryToDelete(selectedEntry.id);
                }}
                className="text-xs font-space font-bold text-red-600 hover:text-red-800 underline flex items-center gap-1 cursor-pointer"
              >
                <Trash2 className="w-3.5 h-3.5" /> Xóa bài viết
              </button>

              <div className="flex flex-wrap items-center gap-2">
                <Link
                  href={`/journal-editor?id=${selectedEntry.id}`}
                  className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-paper-warm hover:bg-amber-100 text-black border border-black rounded-xl font-space font-bold text-xs shadow-neo-sm transition-all cursor-pointer"
                >
                  <Edit3 className="w-3.5 h-3.5 stroke-[2.3]" />
                  <span>Chỉnh sửa bài viết</span>
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
