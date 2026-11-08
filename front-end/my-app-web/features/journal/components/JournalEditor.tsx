'use client';

import React, { Suspense } from 'react';
import { createPortal } from 'react-dom';
import Link from 'next/link';
import { useToast } from '@/providers/ToastProvider';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { completeHabit, CreateHabitFromTemplateModal, type HabitTemplatePreset } from '@/features/selfcare';
import { SafetyModal } from './SafetyModal';
import { AddTopicModal } from './AddTopicModal';
import { InsertImageModal } from './InsertImageModal';
import { CompanionPanel } from './companion/CompanionPanel';
import { CalmingToolsMenu } from './calming-tools/CalmingToolsMenu';
import { CalmingToolsStatusBar } from './calming-tools/CalmingToolsStatusBar';
import { VENT_PROMPT_TEXT } from './calming-tools/vent-prompt';
import { JournalTipTapEditor } from './JournalTipTapEditor';
import { useJournalEditor } from '../hooks/useJournalEditor';
import { useCalmingTools, type CalmingToolSource } from '../hooks/useCalmingTools';
import { useCompanionConversation } from '../hooks/useCompanionConversation';
import { JOURNAL_EMOTIONS, JOURNAL_EMOTION_ORDER } from '../utils/journal-emotions';
import type { JournalEmotion } from '@/types';
import type { CompanionActionCode } from '../types/companion.types';
import {
  ArrowLeft,
  Save,
  Sliders,
  Check,
  Calendar,
  Zap,
  BatteryCharging,
  Moon,
  Plus,
  Edit3,
  Flame,
  Wrench,
  Wind,
  ImagePlus,
  X,
  HeartHandshake,
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
  const calmingToolsButtonRef = React.useRef<HTMLButtonElement>(null);
  const [metricsPopoverPosition, setMetricsPopoverPosition] = React.useState<React.CSSProperties | null>(null);
  const [calmingToolsMenuPosition, setCalmingToolsMenuPosition] = React.useState<React.CSSProperties | null>(null);
  const [isImageModalOpen, setIsImageModalOpen] = React.useState(false);
  const [habitTemplatePreset, setHabitTemplatePreset] = React.useState<HabitTemplatePreset | null>(null);

  // Phá vòng lặp phụ thuộc giữa hai hook: calmingTools cần biết khi nào báo cho companion (tool
  // chạy xong tự nhiên, bấm từ TRONG chat), còn companion cần gọi calmingTools để chạy tool. Hook
  // useCalmingTools tạo trước với một callback ổn định đọc qua ref; ref được gán sau khi companion
  // đã tồn tại — xem effect bên dưới.
  const companionNotifyRef = React.useRef<(tool: 'BREATHING' | 'AMBIENT') => void>(() => {});
  const handleToolNaturalComplete = React.useCallback((tool: 'BREATHING', source: CalmingToolSource) => {
    if (source === 'CHAT') companionNotifyRef.current(tool);
  }, []);
  const calmingTools = useCalmingTools(handleToolNaturalComplete);
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

  const escapeHtml = (value: string) => {
    const entities: Record<string, string> = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
    return value.replace(/[&<>"']/g, (character) => entities[character]);
  };
  const selectedMoodScore = MOOD_ICONS.reduce((closest, candidate) =>
    Math.abs(candidate.score - moodScore) < Math.abs(closest - moodScore) ? candidate.score : closest,
    MOOD_ICONS[0].score);

  /** Mỗi câu hỏi thành một thẻ gợi ý (khối promptQuestion) kèm một dòng trống bên dưới để trả lời. */
  const insertPromptQuestions = (questions: string[]) => {
    if (questions.length === 0) return;
    const html = questions
      .map((question) => `<div data-prompt-question>${escapeHtml(question)}</div><p></p>`)
      .join('');
    editorRef.current?.insertContent(html);
    editorRef.current?.focus();
  };

  const insertVentPrompt = () => {
    editorRef.current?.insertContent(`<p>${escapeHtml(VENT_PROMPT_TEXT)}</p>`);
    editorRef.current?.focus();
  };

  const handleCompanionAction = (code: CompanionActionCode) => {
    switch (code) {
      case 'BREATHING_2MIN':
        calmingTools.startBreathing('CHAT');
        break;
      case 'AMBIENT': {
        const started = calmingTools.startAmbient('CHAT');
        if (!started) {
          showToast({
            title: 'Chưa thể phát âm thanh',
            message: 'Trình duyệt của bạn chưa cho phép phát âm thanh lúc này. Thử lại nhé.',
            type: 'error',
          });
        }
        break;
      }
      case 'VENT_PROMPT':
        insertVentPrompt();
        showToast({ title: 'Đã thêm dòng mở đầu', message: 'Viết hết ra, không cần đúng sai.', type: 'info' });
        break;
      case 'TITLE':
        // Không còn dữ liệu tĩnh ở client: tựa đề gợi ý giờ chỉ đến qua `suggestedTitle` của
        // backend, hiển thị kèm nút "Điền làm tựa đề" ngay trên từng tin nhắn (xem onApplyTitle).
        break;
    }
  };

  const companion = useCompanionConversation({
    getDraft: () => ({ title, text: editorRef.current?.getText() ?? '' }),
    getMood: () => ({ selectedEmotion, stressScore, energyScore, sleepHours }),
    onInsertQuestion: (question) => {
      insertPromptQuestions([question]);
      showToast({
        title: 'Đã chèn câu hỏi gợi mở',
        message: 'Câu hỏi đã được đưa vào trang viết của bạn.',
        type: 'info',
      });
    },
    onApplyTitle: (newTitle) => {
      setTitle(newTitle);
      showToast({ title: 'Đã điền tựa đề gợi ý', message: `“${newTitle}”`, type: 'info' });
    },
    onInsertQuestions: (questions) => {
      insertPromptQuestions(questions);
      showToast({
        title: questions.length > 1 ? `Đã đưa ${questions.length} câu hỏi vào bài` : 'Đã đưa câu hỏi vào bài',
        message: 'Viết câu trả lời ngay bên dưới từng câu hỏi nhé.',
        type: 'success',
      });
    },
    onRunAction: handleCompanionAction,
    onAcceptHabitSuggestion: (suggestion) => {
      if (suggestion.type === 'LOG_EXISTING' && suggestion.habitId && suggestion.localDate) {
        void completeHabit(suggestion.habitId, suggestion.localDate, suggestion.targetValue ?? 1)
          .then(() => {
            showToast({
              title: 'Đã ghi nhận thói quen',
              message: 'Thói quen của bạn đã được đánh dấu hoàn thành hôm nay.',
              type: 'success',
            });
          })
          .catch(() => {
            showToast({
              title: 'Chưa thể ghi nhận',
              message: 'Vui lòng thử lại sau giây lát.',
              type: 'error',
            });
          });
        return;
      }
      if (suggestion.type === 'CREATE_FROM_TEMPLATE') {
        setHabitTemplatePreset({
          templateCode: suggestion.templateCode,
          category: suggestion.category,
          presetTitle: suggestion.presetTitle,
          presetTargetValue: suggestion.presetTargetValue,
          presetUnit: suggestion.presetUnit,
          presetToolCode: suggestion.presetToolCode,
        });
      }
    },
    onSafetyTriggered: () => setIsSafetyModalOpen(true),
  });

  React.useEffect(() => {
    companionNotifyRef.current = companion.notifyToolCompleted;
  }, [companion.notifyToolCompleted]);

  const toggleMetricsPopover = () => {
    setCalmingToolsMenuPosition(null);
    if (showDetailedMetrics) {
      setShowDetailedMetrics(false);
      setMetricsPopoverPosition(null);
      return;
    }
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

  const toggleCalmingToolsMenu = () => {
    setMetricsPopoverPosition(null);
    setShowDetailedMetrics(false);
    if (calmingToolsMenuPosition) {
      setCalmingToolsMenuPosition(null);
      return;
    }
    const button = calmingToolsButtonRef.current;
    if (!button) return;
    const rect = button.getBoundingClientRect();
    const width = Math.min(260, window.innerWidth - 32);
    if (window.matchMedia('(min-width: 1360px)').matches) {
      setCalmingToolsMenuPosition({ position: 'fixed', top: rect.top, left: rect.left - width - 12 });
    } else {
      setCalmingToolsMenuPosition({
        position: 'fixed',
        bottom: window.innerHeight - rect.top + 8,
        left: Math.max(16, Math.min(rect.right - width, window.innerWidth - width - 16)),
      });
    }
  };

  const handleStartBreathing = () => {
    calmingTools.startBreathing();
    setCalmingToolsMenuPosition(null);
  };

  const handleStartAmbient = () => {
    const started = calmingTools.startAmbient();
    setCalmingToolsMenuPosition(null);
    if (!started) {
      showToast({
        title: 'Chưa thể phát âm thanh',
        message: 'Trình duyệt của bạn chưa cho phép phát âm thanh lúc này. Thử lại nhé.',
        type: 'error',
      });
    }
  };

  const handleVent = () => {
    insertVentPrompt();
    setCalmingToolsMenuPosition(null);
    showToast({
      title: 'Đã thêm dòng mở đầu',
      message: 'Viết hết ra, không cần đúng sai.',
      type: 'info',
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

            {/* Calming tool running inline above the writing area */}
            {calmingTools.activeTool && (
              <CalmingToolsStatusBar
                tool={calmingTools.activeTool}
                breathingPhase={calmingTools.breathing.phase}
                breathingLabel={calmingTools.breathing.label}
                onStop={calmingTools.stop}
              />
            )}

            {/* Writing Area: Tactile Lined Paper with TipTap */}
            <div className="relative">
              {editId && loadedEntryId !== editId ? <p className="p-6 font-space text-sm" role="status">Đang tải bài viết...</p> :
              <JournalTipTapEditor
                key={editId ? `${editId}:${rowVersion}` : 'new'}
                ref={editorRef}
                initialContent={content}
                onChange={(html) => setContent(html)}
                onOpenImageModal={() => setIsImageModalOpen(true)}
              />}
            </div>

            <div className="pt-4 border-t-2 border-black/15 text-xs font-space font-bold text-gray-500">{wordCount} từ</div>
          </article>
        </div>

        <aside className={`${isToolsOpen ? 'fixed inset-x-3 bottom-[calc(env(safe-area-inset-bottom)+10rem)] z-30' : 'hidden'} min-[1360px]:sticky min-[1360px]:top-[120px] min-[1360px]:z-20 min-[1360px]:col-start-3 min-[1360px]:row-start-1 min-[1360px]:ml-[10px] min-[1360px]:mt-10 min-[1360px]:block min-[1360px]:w-32 min-[1360px]:self-start`}>
          <div className="max-h-[65dvh] overflow-y-auto flex flex-col gap-[10px] rounded-2xl border-2 border-black bg-paper-warm p-[10px] shadow-neo-lg" style={{ paddingBottom: 'max(0.625rem, env(safe-area-inset-bottom))' }}>
            <div className="flex items-center justify-between min-[1360px]:hidden">
              <span className="font-space text-sm font-extrabold">Công cụ</span>
              <button type="button" onClick={() => setIsToolsOpen(false)} aria-label="Đóng công cụ" className="rounded-lg p-2 hover:bg-white"><X className="h-4 w-4" /></button>
            </div>
            <button type="button" onClick={executeSave} disabled={isSaving || (!!editId && loadedEntryId !== editId)} aria-label={isSaving ? 'Đang lưu' : isEditMode ? 'Cập nhật bài' : 'Lưu'} title={isSaving ? 'Đang lưu' : isEditMode ? 'Cập nhật bài' : 'Lưu'} className="inline-flex h-14 w-full items-center justify-center rounded-xl border border-black bg-primary-container p-2 shadow-neo-sm transition-all hover:-translate-y-0.5 hover:bg-lime-200 hover:shadow-neo active:translate-y-0 active:scale-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black disabled:opacity-50">
              {isEditMode ? <Check className="h-5 w-5" /> : <Save className="h-5 w-5" />}
            </button>
            <button type="button" onClick={() => setIsImageModalOpen(true)} aria-label="Chèn ảnh" title="Chèn ảnh vào bài viết" className="inline-flex h-14 w-full items-center justify-center rounded-xl border border-black bg-white p-2 transition-all hover:-translate-y-0.5 hover:bg-lime-100 hover:shadow-neo-sm active:translate-y-0 active:scale-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"><ImagePlus className="h-5 w-5" /></button>
            <div className="relative">
              <button ref={metricsButtonRef} type="button" onClick={toggleMetricsPopover} aria-label={showDetailedMetrics ? 'Ẩn chỉ số' : 'Hiện chỉ số'} title={showDetailedMetrics ? 'Ẩn chỉ số' : 'Hiện chỉ số'} aria-pressed={showDetailedMetrics} className={`inline-flex h-14 w-full items-center justify-center rounded-xl border border-black p-2 transition-all hover:-translate-y-0.5 hover:shadow-neo-sm active:translate-y-0 active:scale-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${showDetailedMetrics ? 'bg-primary-container shadow-neo-sm' : 'bg-white hover:bg-lime-100'}`}><Sliders className="h-5 w-5" /></button>
            </div>
            <button
              type="button"
              onClick={() => (companion.isOpen ? companion.minimize() : companion.open())}
              aria-label="Trợ lý đồng hành"
              title="Trợ lý đồng hành (Chạm là có người hỏi han)"
              aria-pressed={companion.isOpen}
              className={`relative inline-flex h-14 w-full items-center justify-center rounded-xl border border-black p-2 transition-all hover:-translate-y-0.5 hover:shadow-neo-sm active:translate-y-0 active:scale-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${companion.isOpen ? 'bg-primary-container shadow-neo-sm' : 'bg-white hover:bg-lime-100'}`}
            >
              <HeartHandshake className="h-5 w-5" />
              {companion.unread && (
                <span aria-hidden="true" className="absolute right-1.5 top-1.5 h-2.5 w-2.5 rounded-full border border-black bg-red-500" />
              )}
            </button>
            <button ref={calmingToolsButtonRef} type="button" onClick={toggleCalmingToolsMenu} aria-label="Công cụ dịu" title="Công cụ dịu (Dùng ngay, không cần trò chuyện)" aria-pressed={!!calmingToolsMenuPosition || !!calmingTools.activeTool} className={`inline-flex h-14 w-full items-center justify-center rounded-xl border border-black p-2 transition-all hover:-translate-y-0.5 hover:shadow-neo-sm active:translate-y-0 active:scale-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${calmingToolsMenuPosition || calmingTools.activeTool ? 'bg-primary-container shadow-neo-sm' : 'bg-white hover:bg-lime-100'}`}><Wind className="h-5 w-5" /></button>
          </div>
        </aside>

        {companion.isOpen && <CompanionPanel conversation={companion} />}
      </main>

      {showDetailedMetrics && metricsPopoverPosition && createPortal(
        <div style={metricsPopoverPosition} className="z-40 w-[min(290px,calc(100vw-2rem))] rounded-2xl border-2 border-black bg-paper-warm p-4 shadow-neo-lg animate-in fade-in" role="dialog" aria-label="Chỉ số nhật ký">
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

      {calmingToolsMenuPosition && createPortal(
        <CalmingToolsMenu
          style={calmingToolsMenuPosition}
          onClose={() => setCalmingToolsMenuPosition(null)}
          onBreathing={handleStartBreathing}
          onAmbient={handleStartAmbient}
          onVent={handleVent}
        />,
        document.body,
      )}

      <button type="button" onClick={() => setIsToolsOpen(!isToolsOpen)} aria-label={isToolsOpen ? 'Đóng công cụ' : 'Mở công cụ'} aria-expanded={isToolsOpen} className="fixed bottom-[calc(env(safe-area-inset-bottom)+5.5rem)] right-4 z-30 flex h-14 w-14 items-center justify-center rounded-full border-2 border-black bg-primary-container text-black shadow-neo-lg min-[1360px]:hidden"><Wrench className="h-6 w-6" /></button>

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

      {/* INSERT IMAGE MODAL */}
      <InsertImageModal
        isOpen={isImageModalOpen}
        onClose={() => setIsImageModalOpen(false)}
        onInsert={(url, assetId, caption) => {
          editorRef.current?.insertImage(url, assetId, caption);
        }}
      />

      {/* SAFETY CRISIS MODAL */}
      <SafetyModal
        isOpen={isSafetyModalOpen}
        onClose={() => { setIsSafetyModalOpen(false); if (safetySaved) router.push('/history-calendar'); }}
        onNavigateHome={() => router.push('/dashboard')}
        detectedKeywords={detectedCrisisKeywords}
      />

      {/* CREATE HABIT FROM COMPANION TEMPLATE MODAL */}
      <CreateHabitFromTemplateModal
        isOpen={habitTemplatePreset !== null}
        preset={habitTemplatePreset}
        onClose={() => setHabitTemplatePreset(null)}
        onCreated={(habit) => {
          showToast({
            title: 'Đã tạo thói quen mới!',
            message: `“${habit.title}” đã được thêm vào danh sách tự chăm sóc của bạn.`,
            type: 'success',
          });
        }}
      />
    </div>
  );
}
