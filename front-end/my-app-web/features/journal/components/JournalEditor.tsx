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
import { AmbientPlayerBar } from './ambient-player/AmbientPlayerBar';
import { AmbientSuggestionChip } from './ambient-player/AmbientSuggestionChip';
import { useAmbientPlayer } from '../hooks/useAmbientPlayer';
import type { JournalEmotion } from '@/types';
import { VENT_PROMPT_TEXT } from './calming-tools/vent-prompt';
import { JournalTipTapEditor } from './JournalTipTapEditor';
import { JournalMoodBar } from './JournalMoodBar';
import { JournalToolRail } from './JournalToolRail';
import { useJournalEditor } from '../hooks/useJournalEditor';
import { useCalmingTools, type CalmingToolSource } from '../hooks/useCalmingTools';
import { useCompanionConversation } from '../hooks/useCompanionConversation';
import { JOURNAL_EMOTIONS, JOURNAL_EMOTION_ORDER } from '../utils/journal-emotions';
import type { CompanionActionCode } from '../types/companion.types';
import {
  ArrowLeft,
  Zap,
  BatteryCharging,
  Moon,
  Plus,
  Edit3,
  Flame,
  Wrench,
  X,
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
/** Từ mốc này cột công cụ nằm trong card trang viết; nhỏ hơn thì là ngăn kéo nổi. */
const TOOL_RAIL_MEDIA = '(min-width: 768px)';

function formatEntryTimestamp(date: Date) {
  const day = date.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit' });
  const time = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  return `${day.charAt(0).toUpperCase()}${day.slice(1)} · ${time}`;
}

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
  // Music suggestions follow only a mood the user actually picked on this visit, not the editor's
  // default emotion.
  const [pickedMood, setPickedMood] = React.useState<JournalEmotion | null>(null);
  const ambient = useAmbientPlayer(pickedMood);

  const escapeHtml = (value: string) => {
    const entities: Record<string, string> = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
    return value.replace(/[&<>"']/g, (character) => entities[character]);
  };
  const SelectedEmotionIcon = selectedEmotion ? JOURNAL_EMOTIONS[selectedEmotion].icon : null;
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
        // "Nghe âm thanh dịu" from the companion opens the music player on the rain track.
        const started = ambient.openAndPlay('rain');
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
    if (window.matchMedia(TOOL_RAIL_MEDIA).matches) {
      setMetricsPopoverPosition({ position: 'fixed', top: rect.top, left: Math.max(16, rect.left - 290 - 12) });
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
    if (window.matchMedia(TOOL_RAIL_MEDIA).matches) {
      setCalmingToolsMenuPosition({ position: 'fixed', top: rect.top, left: Math.max(16, rect.left - width - 12) });
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
    const started = ambient.openAndPlay('rain');
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
    <div className="w-full bg-bg-canvas min-h-[calc(100dvh-64px)] px-3 sm:px-6 pt-5 pb-28 md:pb-16 selection:bg-primary-container selection:text-black overflow-x-clip relative">
      <div
        className={`relative mx-auto w-full flex flex-col gap-4 bg-paper-warm border-[2.5px] border-black rounded-3xl p-3 sm:p-5 shadow-neo-lg transition-[max-width] duration-300 ${
          companion.isOpen ? 'max-w-[1040px] xl:max-w-[1320px]' : 'max-w-[1040px]'
        }`}
      >
        <WashiTape color={isEditMode ? 'peach' : 'lime'} rotate={2} className="absolute -top-3.5 right-12 w-32 z-10 hidden md:block" />

        <JournalMoodBar
          isSelected={(emotion) => (selectedEmotion ? selectedEmotion === emotion : selectedMoodScore === JOURNAL_EMOTIONS[emotion].score)}
          onSelect={(emotion) => {
            setMoodScore(JOURNAL_EMOTIONS[emotion].score);
            setMood(JOURNAL_EMOTIONS[emotion].mood);
            setSelectedEmotion(emotion);
            setPickedMood(emotion);
          }}
          trailing={
            isEditMode ? (
              <>
                <Link
                  href="/history-calendar"
                  className="inline-flex items-center gap-1 font-space text-xs font-bold text-gray-700 hover:text-black hover:underline"
                >
                  <ArrowLeft className="w-4 h-4" />
                  <span>Lịch ký ức</span>
                </Link>
                <span className="px-2.5 py-1 bg-amber-200 text-black border border-black rounded-xl font-space text-xs font-extrabold flex items-center gap-1.5 shadow-neo-xs">
                  <Edit3 className="w-3.5 h-3.5 text-amber-900" />
                  <span>Đang sửa bài</span>
                </span>
                <button
                  type="button"
                  onClick={handleResetToNewEntry}
                  className="px-2.5 py-1 bg-white hover:bg-primary-container text-black border border-black rounded-xl font-space text-xs font-bold transition-all shadow-neo-xs hover:-translate-y-0.5 cursor-pointer flex items-center gap-1"
                  title="Mở một trang trắng mới để viết"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Viết trang mới</span>
                </button>
              </>
            ) : (
              <span
                className="inline-flex items-center gap-1.5 bg-amber-100 text-orange-700 border border-black px-2.5 py-1 rounded-xl shadow-neo-xs font-space font-extrabold"
                aria-label={`${streakCount} ngày viết liên tục`}
                title={`${streakCount} ngày viết liên tục`}
              >
                <span className="text-base leading-none">{streakCount}</span>
                <Flame className="w-4 h-4 text-orange-600 fill-orange-500 shrink-0" aria-hidden="true" />
              </span>
            )
          }
        />

        <main className={`grid grid-cols-1 gap-4 ${companion.isOpen ? 'xl:grid-cols-[minmax(0,1fr)_360px]' : ''}`}>
          <div className="flex min-w-0 rounded-2xl border-2 border-black bg-white">
            <article className="flex min-w-0 flex-1 flex-col gap-4 p-4 sm:p-7">
              {/* Notebook Header */}
              <header className="flex flex-col gap-3">
                <div className="flex flex-wrap items-center gap-2 font-space text-xs font-bold text-gray-500">
                  {SelectedEmotionIcon && selectedEmotion && (
                    <span className="inline-flex items-center gap-1.5 rounded-full border border-black bg-paper-warm px-2.5 py-0.5 text-black">
                      <SelectedEmotionIcon className="w-3.5 h-3.5" aria-hidden="true" />
                      {JOURNAL_EMOTIONS[selectedEmotion].label}
                    </span>
                  )}
                  <span suppressHydrationWarning>
                    {isEditMode && existingDate ? `Ngày ghi: ${existingDate}` : formatEntryTimestamp(new Date())}
                  </span>
                </div>

                {/* Title Input */}
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="Tựa đề bài viết"
                  aria-label="Tựa đề bài viết"
                  className="w-full bg-transparent font-space text-xl sm:text-2xl font-bold text-on-surface focus:outline-none placeholder:text-gray-400 pb-2 border-b-2 border-dashed border-black/25 focus:border-black/60 transition-colors"
                />

                {/* Tags / Topics */}
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
              </header>

              {/* Background music player and "Thở chậm" can run side by side above the writing area */}
              {ambient.open && <AmbientPlayerBar player={ambient} />}
              {!ambient.open && ambient.suggestion && (
                <AmbientSuggestionChip
                  track={ambient.suggestion}
                  onPlay={() => ambient.select(ambient.suggestion!.id)}
                  onDismiss={ambient.dismissSuggestion}
                />
              )}
              {calmingTools.activeTool === 'BREATHING' && (
                <CalmingToolsStatusBar
                  breathingPhase={calmingTools.breathing.phase}
                  breathingLabel={calmingTools.breathing.label}
                  onStop={calmingTools.stop}
                />
              )}

              {/* Writing Area with TipTap */}
              <div className="relative flex-1">
                {editId && loadedEntryId !== editId ? <p className="p-6 font-space text-sm" role="status">Đang tải bài viết...</p> :
                <JournalTipTapEditor
                  key={editId ? `${editId}:${rowVersion}` : 'new'}
                  ref={editorRef}
                  initialContent={content}
                  onChange={(html) => setContent(html)}
                  onOpenImageModal={() => setIsImageModalOpen(true)}
                />}
              </div>

              <div className="pt-3 border-t border-black/15 text-xs font-space font-bold text-gray-500">{wordCount} từ</div>
            </article>

            <JournalToolRail
              isDrawerOpen={isToolsOpen}
              onCloseDrawer={() => setIsToolsOpen(false)}
              isEditMode={isEditMode}
              isSaving={isSaving}
              saveDisabled={!!editId && loadedEntryId !== editId}
              onSave={executeSave}
              onInsertImage={() => setIsImageModalOpen(true)}
              metricsButtonRef={metricsButtonRef}
              isMetricsOpen={showDetailedMetrics}
              onToggleMetrics={toggleMetricsPopover}
              isCompanionOpen={companion.isOpen}
              hasCompanionUnread={companion.unread}
              onToggleCompanion={() => (companion.isOpen ? companion.minimize() : companion.open())}
              calmingToolsButtonRef={calmingToolsButtonRef}
              isCalmingToolsActive={!!calmingToolsMenuPosition || !!calmingTools.activeTool}
              onToggleCalmingTools={toggleCalmingToolsMenu}
              isMusicOpen={ambient.open}
              isMusicPlaying={ambient.status === 'playing'}
              onToggleMusic={() => {
                if (ambient.open) { ambient.close(); return; }
                if (!ambient.openAndPlay()) {
                  showToast({ title: 'Chưa thể phát âm thanh', message: 'Trình duyệt của bạn chưa cho phép phát âm thanh lúc này. Thử lại nhé.', type: 'error' });
                }
              }}
            />
          </div>

          {companion.isOpen && <CompanionPanel conversation={companion} />}
        </main>
      </div>

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

      <button type="button" onClick={() => setIsToolsOpen(!isToolsOpen)} aria-label={isToolsOpen ? 'Đóng công cụ' : 'Mở công cụ'} aria-expanded={isToolsOpen} className="fixed bottom-[calc(env(safe-area-inset-bottom)+5.5rem)] right-4 z-30 flex h-14 w-14 items-center justify-center rounded-full border-2 border-black bg-primary-container text-black shadow-neo-lg md:hidden"><Wrench className="h-6 w-6" /></button>

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
