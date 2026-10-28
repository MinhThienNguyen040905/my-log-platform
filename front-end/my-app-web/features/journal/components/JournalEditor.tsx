'use client';

import React, { Suspense } from 'react';
import { createPortal } from 'react-dom';
import Link from 'next/link';
import { useToast } from '@/providers/ToastProvider';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { SafetyModal } from './SafetyModal';
import { AddTopicModal } from './AddTopicModal';
import { JournalTipTapEditor } from './JournalTipTapEditor';
import { useJournalEditor } from '../hooks/useJournalEditor';
import { generateReflectiveQuestions, type ReflectiveQuestions } from '@/features/knowledge';
import { JOURNAL_EMOTIONS, JOURNAL_EMOTION_ORDER } from '../utils/journal-emotions';
import type { JournalEmotion } from '@/types';
import {
  ArrowLeft,
  Save,
  Sliders,
  Check,
  Calendar,
  Lightbulb,
  Zap,
  BatteryCharging,
  Moon,
  Plus,
  Edit3,
  Flame,
  Wrench,
  ImagePlus,
  X,
  MessageCircleQuestionMark,
} from 'lucide-react';

export function JournalEditor() {
  return (
    <Suspense
      fallback={
        <div className="p-12 text-center font-space text-sm font-bold">
          Đang mở trang sổ tay của bạn...
        </div>
      }
    >
      <JournalEditorContent />
    </Suspense>
  );
}

const MOOD_ICONS = JOURNAL_EMOTION_ORDER.map((id) => ({ id, ...JOURNAL_EMOTIONS[id] }));
const REFLECTION_TOPIC_CODE = 'RECOMMEND_MODEL_DOCS';
const TOPIC_TITLE_SUGGESTIONS = [
  'Một điều nhỏ khiến mình vui hôm nay',
  'Điều mình đang học cách chấp nhận',
  'Khoảnh khắc hôm nay mình muốn nhớ',
];
const MOOD_COLORS: Record<JournalEmotion, { activeBg: string; hoverBg: string }> = {
  neutral: { activeBg: 'bg-slate-200', hoverBg: 'hover:bg-slate-100' },
  happy: { activeBg: 'bg-primary-container', hoverBg: 'hover:bg-lime-50' },
  angry: { activeBg: 'bg-orange-200', hoverBg: 'hover:bg-orange-50' },
  sad: { activeBg: 'bg-blue-200', hoverBg: 'hover:bg-blue-50' },
  very_bad: { activeBg: 'bg-indigo-200', hoverBg: 'hover:bg-indigo-50' },
};

function JournalEditorContent() {
  const { showToast } = useToast();
  const metricsButtonRef = React.useRef<HTMLButtonElement>(null);
  const questionsButtonRef = React.useRef<HTMLButtonElement>(null);
  const topicSuggestionButtonRef = React.useRef<HTMLButtonElement>(null);
  const [metricsPopoverPosition, setMetricsPopoverPosition] = React.useState<React.CSSProperties | null>(null);
  const [questionsPopoverPosition, setQuestionsPopoverPosition] = React.useState<React.CSSProperties | null>(null);
  const [topicSuggestionPosition, setTopicSuggestionPosition] = React.useState<React.CSSProperties | null>(null);
  const [recommendation, setRecommendation] = React.useState<ReflectiveQuestions | null>(null);
  const [isLoadingRecommendation, setIsLoadingRecommendation] = React.useState(false);
  const [recommendationError, setRecommendationError] = React.useState<string | null>(null);
  const {
    editorRef,
    isEditMode,
    editId,
    rowVersion,
    loadedEntryId,
    isSaving,
    existingDate,
    title,
    setTitle,
    content,
    setContent,
    setMood,
    selectedEmotion,
    setSelectedEmotion,
    moodScore,
    setMoodScore,
    hoveredMoodScore,
    setHoveredMoodScore,
    stressScore,
    setStressScore,
    energyScore,
    setEnergyScore,
    sleepHours,
    setSleepHours,
    showDetailedMetrics,
    setShowDetailedMetrics,
    topics,
    setTopics,
    showAddTopicModal,
    setShowAddTopicModal,
    isToolsOpen,
    setIsToolsOpen,
    isSafetyModalOpen,
    safetySaved,
    setIsSafetyModalOpen,
    detectedCrisisKeywords,
    handleResetToNewEntry,
    executeSave,
    wordCount,
    router,
    streakCount,
  } = useJournalEditor();

  const isQuestionsPopoverOpen = questionsPopoverPosition !== null;
  React.useEffect(() => {
    if (!isQuestionsPopoverOpen) return;
    let active = true;
    generateReflectiveQuestions(REFLECTION_TOPIC_CODE)
      .then((result) => { if (active) setRecommendation(result); })
      .catch(() => { if (active) setRecommendationError('Chưa tải được gợi ý. Bạn có thể thử mở lại sau.'); })
      .finally(() => { if (active) setIsLoadingRecommendation(false); });
    return () => { active = false; };
  }, [isQuestionsPopoverOpen]);

  const escapeHtml = (value: string) => {
    const entities: Record<string, string> = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
    return value.replace(/[&<>"']/g, (character) => entities[character]);
  };
  const selectedMoodScore = MOOD_ICONS.reduce((closest, candidate) =>
    Math.abs(candidate.score - moodScore) < Math.abs(closest - moodScore) ? candidate.score : closest,
    MOOD_ICONS[0].score);

  const toggleMetricsPopover = () => {
    setTopicSuggestionPosition(null);
    if (showDetailedMetrics) {
      setShowDetailedMetrics(false);
      setMetricsPopoverPosition(null);
      return;
    }
    setQuestionsPopoverPosition(null);
    const button = metricsButtonRef.current;
    if (!button) return;
    const rect = button.getBoundingClientRect();
    if (window.matchMedia('(min-width: 1360px)').matches) {
      setMetricsPopoverPosition({ position: 'fixed', top: rect.top, left: rect.left - 300 });
    } else {
      const width = Math.min(290, window.innerWidth - 32);
      setMetricsPopoverPosition({
        position: 'fixed',
        bottom: window.innerHeight - rect.top + 8,
        left: Math.max(16, Math.min(rect.right - width, window.innerWidth - width - 16)),
      });
    }
    setShowDetailedMetrics(true);
  };

  const toggleQuestionsPopover = () => {
    setTopicSuggestionPosition(null);
    setShowDetailedMetrics(false);
    setMetricsPopoverPosition(null);
    if (questionsPopoverPosition) {
      setQuestionsPopoverPosition(null);
      setIsLoadingRecommendation(false);
      return;
    }
    const button = questionsButtonRef.current;
    if (!button) return;
    const rect = button.getBoundingClientRect();
    setRecommendation(null);
    setRecommendationError(null);
    setIsLoadingRecommendation(true);
    if (window.matchMedia('(min-width: 1360px)').matches) {
      setQuestionsPopoverPosition({ position: 'fixed', top: rect.top, left: rect.left - 300 });
    } else {
      const width = Math.min(290, window.innerWidth - 32);
      setQuestionsPopoverPosition({
        position: 'fixed',
        bottom: window.innerHeight - rect.top + 8,
        left: Math.max(16, Math.min(rect.right - width, window.innerWidth - width - 16)),
      });
    }
  };

  const toggleTopicSuggestions = () => {
    setMetricsPopoverPosition(null);
    setShowDetailedMetrics(false);
    setQuestionsPopoverPosition(null);
    if (topicSuggestionPosition) {
      setTopicSuggestionPosition(null);
      return;
    }
    const button = topicSuggestionButtonRef.current;
    if (!button) return;
    const rect = button.getBoundingClientRect();
    const width = Math.min(290, window.innerWidth - 32);
    const estimatedPopoverHeight = 220;
    setTopicSuggestionPosition({
      position: 'fixed',
      top: Math.max(16, Math.min(rect.top - estimatedPopoverHeight - 8, window.innerHeight - estimatedPopoverHeight - 16)),
      left: Math.max(16, Math.min(rect.right - width, window.innerWidth - width - 16)),
    });
  };

  return (
    <div className="w-full flex flex-col bg-bg-canvas min-h-[calc(100vh-80px)] pb-16 selection:bg-primary-container selection:text-black overflow-x-clip relative">
      {/* Top Quiet Meta Bar */}
      <section className="w-full max-w-4xl mx-auto px-4 sm:px-6 pt-4 pb-2 flex flex-wrap items-center gap-3 text-xs font-space font-bold text-gray-600">
        <div className="flex items-center gap-2">
          {/* Chỉ hiển thị nút quay về Lịch ký ức khi đang sửa bài (isEditMode) */}
          {isEditMode && (
            <Link
              href="/history-calendar"
              className="inline-flex items-center gap-1 text-gray-700 hover:text-black hover:underline"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Lịch ký ức</span>
            </Link>
          )}

          {isEditMode ? (
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-1 bg-amber-200 text-black border border-black rounded-xl text-xs font-extrabold flex items-center gap-1.5 shadow-neo-xs">
                <Edit3 className="w-3.5 h-3.5 text-amber-900" />
                <span>Đang sửa bài{existingDate ? `: ${existingDate}` : ''}</span>
              </span>
              <button
                type="button"
                onClick={handleResetToNewEntry}
                className="px-2.5 py-1 bg-white hover:bg-primary-container text-black border border-black rounded-xl text-xs font-bold transition-all shadow-neo-xs hover:-translate-y-0.5 cursor-pointer flex items-center gap-1"
                title="Mở một trang trắng mới để viết"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Viết trang mới</span>
              </button>
            </div>
          ) : (
            <span className="px-2.5 py-1 bg-primary-container text-black border border-black rounded-xl text-xs font-extrabold flex items-center gap-1.5 shadow-neo-xs">
              <span>Trang viết mới hôm nay</span>
            </span>
          )}
        </div>

      </section>

      {/* Main Screen Layout Container */}
      <main className="relative mx-auto grid w-full max-w-[1440px] grid-cols-1 px-4 sm:px-6 py-2 transition-all duration-300 min-[1360px]:grid-cols-[minmax(0,1fr)_minmax(0,920px)_minmax(0,1fr)]">
        <div className="mx-auto w-full max-w-[920px] min-w-0 transition-all duration-300 min-[1360px]:col-start-2 min-[1360px]:row-start-1">
          <article className="relative bg-paper-warm border-[2.5px] border-black rounded-3xl p-6 sm:p-10 shadow-neo-lg transition-all overflow-visible flex flex-col gap-6">
            {/* Washi Tape and Page Number Stamp */}
            <WashiTape color={isEditMode ? 'peach' : 'lime'} rotate={2} className="absolute -top-3.5 right-12 w-32 z-10" />
            
            {/* Notebook Header */}
            <header className="flex flex-col gap-3 pb-4 border-b-2 border-black/15">
              <div className="grid grid-cols-1 md:grid-cols-2 items-center gap-3 text-xs font-space font-bold text-gray-700">
                <span className="inline-flex w-fit max-w-full items-center gap-1.5 bg-white px-3 py-1 rounded-xl border border-black shadow-neo-sm">
                  <Calendar className="w-3.5 h-3.5 text-black" />
                  {isEditMode && existingDate
                    ? `Ngày ghi: ${existingDate}`
                    : `${new Date().toLocaleDateString('vi-VN', {
                        weekday: 'long',
                        day: 'numeric',
                        month: 'long',
                        year: 'numeric',
                      })}`}
                </span>

                <div className="flex w-fit max-w-full flex-wrap items-center gap-2 md:justify-self-end">
                  <div className="flex w-fit items-center gap-1 bg-white px-[14px] py-1 rounded-2xl border border-black shadow-neo-sm" role="group" aria-label="Chọn cảm xúc hôm nay">
                    {MOOD_ICONS.map((item) => {
                      const isSelected = selectedEmotion ? selectedEmotion === item.id : selectedMoodScore === item.score;
                      const isHovered = hoveredMoodScore === item.score;
                      const IconComponent = item.icon;

                      return (
                      <div key={item.score} className="relative flex items-center justify-center">
                        {/* Floating Neo-Brutalist Tooltip when hovered */}
                        {isHovered && (
                          <div className="absolute -top-10 left-1/2 -translate-x-1/2 bg-white text-black border border-black font-space text-[11px] font-extrabold px-2.5 py-1 rounded-lg shadow-neo-xs whitespace-nowrap z-40 pointer-events-none animate-in fade-in zoom-in-95 flex flex-col items-center">
                            <span>{item.label}</span>
                            <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-2 h-2 bg-white border-b border-r border-black rotate-45" />
                          </div>
                        )}

                        <button
                          type="button"
                          onClick={() => {
                            setMoodScore(item.score);
                            setMood(JOURNAL_EMOTIONS[item.id].mood);
                            setSelectedEmotion(item.id);
                          }}
                          onMouseEnter={() => setHoveredMoodScore(item.score)}
                          onMouseLeave={() => setHoveredMoodScore(null)}
                          aria-label={item.label}
                          aria-pressed={isSelected}
                          className={`w-9 h-9 rounded-xl flex items-center justify-center transition-all cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${
                            isSelected
                              ? `${MOOD_COLORS[item.id].activeBg} border border-black scale-110 text-black`
                              : `opacity-85 hover:opacity-100 ${MOOD_COLORS[item.id].hoverBg}`
                          }`}
                          title={item.label}
                        >
                          <span aria-hidden="true" className="relative inline-flex items-center justify-center">
                            <IconComponent className="w-6 h-6 stroke-[2.2]" />
                          </span>
                        </button>
                      </div>
                      );
                    })}
                  </div>
                  {!isEditMode && (
                    <div className="inline-flex w-fit items-center gap-1.5 bg-amber-100 text-orange-700 px-2.5 py-1 rounded-xl shadow-neo-xs font-space font-extrabold animate-in fade-in" aria-label={`${streakCount} ngày viết liên tục`}>
                      <span className="text-lg leading-none">{streakCount}</span>
                      <Flame className="w-5 h-5 text-orange-600 fill-orange-500 shrink-0" aria-hidden="true" />
                    </div>
                  )}
                </div>
              </div>

              {/* Title Input */}
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Đặt một tựa đề cho hôm nay..."
                className="w-full bg-transparent font-space text-xl sm:text-2xl font-bold text-on-surface focus:outline-none placeholder:text-gray-400 py-1 border-b border-transparent focus:border-black/30 transition-all"
              />

              {/* Tags / Topics */}
              <div className="flex flex-col gap-2 pt-1">
                <div className="flex items-center justify-end">
                  <button
                    ref={topicSuggestionButtonRef}
                    type="button"
                    onClick={toggleTopicSuggestions}
                    aria-label="Gợi ý chủ đề viết"
                    aria-expanded={!!topicSuggestionPosition}
                    title="Gợi ý chủ đề viết"
                    className={`rounded-full border border-black p-2 transition-all hover:-translate-y-0.5 hover:bg-amber-100 hover:shadow-neo-sm active:translate-y-0 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${topicSuggestionPosition ? 'bg-primary-container shadow-neo-sm' : 'bg-white'}`}
                  >
                    <Lightbulb className="h-4 w-4 text-amber-600" />
                  </button>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                {topics.map((tag, idx) => (
                  <span key={`${tag}-${idx}`} className="inline-flex items-center gap-1 px-2.5 py-0.5 bg-white rounded-lg border border-black text-xs font-space font-bold shadow-neo-sm">
                    #{tag}
                    <button type="button" onClick={() => setTopics(topics.filter((_, i) => i !== idx))} aria-label={`Xóa tag ${tag}`} className="text-gray-400 hover:text-black ml-1 text-xs">×</button>
                  </span>
                ))}
                <button
                  type="button"
                  onClick={() => setShowAddTopicModal(true)}
                  className="inline-flex items-center gap-1 px-2.5 py-0.5 bg-primary-container/40 hover:bg-primary-container rounded-lg border border-black text-xs font-space font-bold transition-all cursor-pointer shadow-neo-xs hover:-translate-y-0.5 active:translate-y-0.5"
                  title="Thêm tag"
                >
                  <Plus className="w-3 h-3" />
                  <span>Tag</span>
                </button>
                </div>
              </div>
            </header>

            {/* Writing Area: Tactile Lined Paper with TipTap */}
            <div className="relative">
              {editId && loadedEntryId !== editId ? <p className="p-6 font-space text-sm" role="status">Đang tải bài viết...</p> :
              <JournalTipTapEditor
                key={editId ? `${editId}:${rowVersion}` : 'new'}
                ref={editorRef}
                initialContent={content}
                onChange={(html) => setContent(html)}
                onOpenImageModal={() => showToast({ title: 'Chèn ảnh chưa khả dụng', message: 'Backend chưa hỗ trợ ảnh trong nội dung nhật ký.', type: 'info' })}
              />}
            </div>

            <div className="pt-4 border-t-2 border-black/15 text-xs font-space font-bold text-gray-500">{wordCount} từ</div>
          </article>
        </div>

        <aside className={`${isToolsOpen ? 'fixed inset-x-3 bottom-[calc(env(safe-area-inset-bottom)+10rem)] z-[100]' : 'hidden'} min-[1360px]:sticky min-[1360px]:top-[120px] min-[1360px]:z-[100] min-[1360px]:col-start-3 min-[1360px]:row-start-1 min-[1360px]:ml-[10px] min-[1360px]:mt-10 min-[1360px]:block min-[1360px]:w-32 min-[1360px]:self-start`}>
          <div className="max-h-[65dvh] overflow-y-auto flex flex-col gap-[10px] rounded-2xl border-2 border-black bg-paper-warm p-[10px] shadow-neo-lg" style={{ paddingBottom: 'max(0.625rem, env(safe-area-inset-bottom))' }}>
            <div className="flex items-center justify-between min-[1360px]:hidden">
              <span className="font-space text-sm font-extrabold">Công cụ</span>
              <button type="button" onClick={() => setIsToolsOpen(false)} aria-label="Đóng công cụ" className="rounded-lg p-2 hover:bg-white"><X className="h-4 w-4" /></button>
            </div>
            <button type="button" onClick={executeSave} disabled={isSaving || (!!editId && loadedEntryId !== editId)} aria-label={isSaving ? 'Đang lưu' : isEditMode ? 'Cập nhật bài' : 'Lưu'} title={isSaving ? 'Đang lưu' : isEditMode ? 'Cập nhật bài' : 'Lưu'} className="inline-flex h-14 w-full items-center justify-center rounded-xl border border-black bg-primary-container p-2 shadow-neo-sm transition-all hover:-translate-y-0.5 hover:bg-lime-200 hover:shadow-neo active:translate-y-0 active:scale-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black disabled:opacity-50">
              {isEditMode ? <Check className="h-5 w-5" /> : <Save className="h-5 w-5" />}
            </button>
            <button type="button" onClick={() => showToast({ title: 'Chèn nội dung', message: 'Gõ “/” trong trang viết để mở menu chèn.', type: 'info' })} aria-label="Chèn nội dung" title="Chèn nội dung (gõ / trong trang viết)" className="inline-flex h-14 w-full items-center justify-center rounded-xl border border-black bg-white p-2 transition-all hover:-translate-y-0.5 hover:bg-lime-100 hover:shadow-neo-sm active:translate-y-0 active:scale-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"><ImagePlus className="h-5 w-5" /></button>
            <div className="relative">
              <button ref={metricsButtonRef} type="button" onClick={toggleMetricsPopover} aria-label={showDetailedMetrics ? 'Ẩn chỉ số' : 'Hiện chỉ số'} title={showDetailedMetrics ? 'Ẩn chỉ số' : 'Hiện chỉ số'} aria-pressed={showDetailedMetrics} className={`inline-flex h-14 w-full items-center justify-center rounded-xl border border-black p-2 transition-all hover:-translate-y-0.5 hover:shadow-neo-sm active:translate-y-0 active:scale-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${showDetailedMetrics ? 'bg-primary-container shadow-neo-sm' : 'bg-white hover:bg-lime-100'}`}><Sliders className="h-5 w-5" /></button>
            </div>
            <button ref={questionsButtonRef} type="button" onClick={toggleQuestionsPopover} aria-label="Câu hỏi tự vấn" title="Câu hỏi tự vấn" aria-pressed={!!questionsPopoverPosition} className={`inline-flex h-14 w-full items-center justify-center rounded-xl border border-black p-2 transition-all hover:-translate-y-0.5 hover:shadow-neo-sm active:translate-y-0 active:scale-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${questionsPopoverPosition ? 'bg-primary-container shadow-neo-sm' : 'bg-white hover:bg-lime-100'}`}><MessageCircleQuestionMark className="h-5 w-5" /></button>
          </div>
        </aside>
      </main>

      {showDetailedMetrics && metricsPopoverPosition && createPortal(
        <div style={metricsPopoverPosition} className="z-[1000] w-[min(290px,calc(100vw-2rem))] rounded-2xl border-2 border-black bg-paper-warm p-4 shadow-neo-lg animate-in fade-in" role="dialog" aria-label="Chỉ số nhật ký">
          <div className="mb-3 flex items-center justify-between font-space text-sm font-extrabold">
            <span>Chỉ số hôm nay</span>
            <button type="button" onClick={toggleMetricsPopover} aria-label="Đóng chỉ số" className="rounded-lg p-1 transition-colors hover:bg-white focus-visible:outline-2 focus-visible:outline-black"><X className="h-4 w-4" /></button>
          </div>
          <div className="flex flex-col gap-4">
            <label className="text-xs font-space font-bold"><span className="mb-1 flex justify-between"><span><Zap className="mr-1 inline h-3.5 w-3.5 text-amber-500" />Áp lực</span><span>{stressScore}/10</span></span><input aria-label="Áp lực" type="range" min="1" max="10" step="0.5" value={stressScore} onChange={(e) => setStressScore(parseFloat(e.target.value))} className="w-full cursor-pointer accent-amber-500" /></label>
            <label className="text-xs font-space font-bold"><span className="mb-1 flex justify-between"><span><BatteryCharging className="mr-1 inline h-3.5 w-3.5 text-green-600" />Năng lượng</span><span>{energyScore}/10</span></span><input aria-label="Năng lượng" type="range" min="1" max="10" step="0.5" value={energyScore} onChange={(e) => setEnergyScore(parseFloat(e.target.value))} className="w-full cursor-pointer accent-green-600" /></label>
            <label className="text-xs font-space font-bold"><span className="mb-1 flex justify-between"><span><Moon className="mr-1 inline h-3.5 w-3.5 text-blue-500" />Giấc ngủ</span><span>{sleepHours}h</span></span><input aria-label="Giấc ngủ" type="range" min="3" max="12" step="0.5" value={sleepHours} onChange={(e) => setSleepHours(parseFloat(e.target.value))} className="w-full cursor-pointer accent-blue-500" /></label>
          </div>
        </div>,
        document.body,
      )}

      {questionsPopoverPosition && createPortal(
        <div style={questionsPopoverPosition} className="z-[1000] w-[min(290px,calc(100vw-2rem))] rounded-2xl border-2 border-black bg-paper-warm p-4 shadow-neo-lg animate-in fade-in" role="dialog" aria-label="Câu hỏi tự vấn">
          <div className="mb-3 flex items-center justify-between font-space text-sm font-extrabold">
            <span>Câu hỏi tự vấn</span>
            <button type="button" onClick={() => setQuestionsPopoverPosition(null)} aria-label="Đóng câu hỏi tự vấn" className="rounded-lg p-1 transition-colors hover:bg-white focus-visible:outline-2 focus-visible:outline-black"><X className="h-4 w-4" /></button>
          </div>
          {isLoadingRecommendation ? <p className="py-3 text-sm text-gray-600" role="status">Đang tải gợi ý...</p>
            : recommendationError ? <p className="py-3 text-sm text-red-700" role="alert">{recommendationError}</p>
            : !recommendation?.questions.length ? <p className="py-3 text-sm text-gray-600">{recommendation?.message ?? 'Chưa đủ thông tin để tạo câu hỏi phù hợp.'}</p>
            : <div className="flex max-h-[min(55dvh,24rem)] flex-col gap-2 overflow-y-auto">
              {recommendation.questions.map((question, index) => (
                <div key={`${index}-${question}`} className="rounded-xl border border-black bg-white p-3 text-sm leading-relaxed">
                  <p>{question}</p>
                  <div className="mt-2 flex justify-end border-t border-black/10 pt-2">
                    <button type="button" onClick={() => {
                      editorRef.current?.insertContent(`<blockquote><p>${escapeHtml(question)}</p></blockquote><p></p>`);
                      editorRef.current?.focus();
                      setQuestionsPopoverPosition(null);
                    }} className="shrink-0 rounded-lg border border-black px-2 py-1 font-bold transition-colors hover:bg-lime-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black">Chèn</button>
                  </div>
                </div>
              ))}
            </div>}
        </div>,
        document.body,
      )}

      {topicSuggestionPosition && createPortal(
        <div style={topicSuggestionPosition} className="z-[1000] w-[min(290px,calc(100vw-2rem))] rounded-2xl border-2 border-black bg-paper-warm p-4 shadow-neo-lg animate-in fade-in" role="dialog" aria-label="Gợi ý chủ đề viết">
          <div className="mb-3 flex items-center justify-between font-space text-sm font-extrabold">
            <span>Gợi ý chủ đề</span>
            <button type="button" onClick={() => setTopicSuggestionPosition(null)} aria-label="Đóng gợi ý chủ đề" className="rounded-lg p-1 transition-colors hover:bg-white focus-visible:outline-2 focus-visible:outline-black"><X className="h-4 w-4" /></button>
          </div>
          <div className="flex flex-col gap-2">
            {TOPIC_TITLE_SUGGESTIONS.map((suggestion) => (
              <button key={suggestion} type="button" onClick={() => {
                setTitle(suggestion);
                setTopicSuggestionPosition(null);
              }} className="rounded-xl border border-black bg-white p-3 text-left text-sm leading-relaxed transition-all hover:-translate-y-0.5 hover:bg-lime-100 hover:shadow-neo-sm active:translate-y-0 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black">
                {suggestion}
              </button>
            ))}
          </div>
        </div>,
        document.body,
      )}

      <button type="button" onClick={() => setIsToolsOpen(!isToolsOpen)} aria-label={isToolsOpen ? 'Đóng công cụ' : 'Mở công cụ'} aria-expanded={isToolsOpen} className="fixed bottom-[calc(env(safe-area-inset-bottom)+5.5rem)] right-4 z-50 flex h-14 w-14 items-center justify-center rounded-full border-2 border-black bg-primary-container text-black shadow-neo-lg min-[1360px]:hidden"><Wrench className="h-6 w-6" /></button>

      {/* ADD TOPIC MODAL */}
      <AddTopicModal
        isOpen={showAddTopicModal}
        onClose={() => setShowAddTopicModal(false)}
        onAddTopic={(newTopic) => {
          setTopics((prev) => [...prev, newTopic]);
          showToast({
            title: 'Đã thêm tag mới!',
            message: `#${newTopic} đã được gắn vào trang viết.`,
            type: 'success',
          });
        }}
        existingTopics={topics}
      />

      {/* SAFETY CRISIS MODAL */}
      <SafetyModal
        isOpen={isSafetyModalOpen}
        onClose={() => { setIsSafetyModalOpen(false); if (safetySaved) router.push('/history-calendar'); }}
        onNavigateHome={() => router.push('/dashboard')}
        detectedKeywords={detectedCrisisKeywords}
      />
    </div>
  );
}
