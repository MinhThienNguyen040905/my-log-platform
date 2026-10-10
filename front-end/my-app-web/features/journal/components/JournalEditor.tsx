'use client';

import React, { Suspense } from 'react';
import Link from 'next/link';
import { useToast } from '@/providers/ToastProvider';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { SafetyModal } from './SafetyModal';
import { AiReflectionDrawer } from './AiReflectionDrawer';
import { AddTopicModal } from './AddTopicModal';
import { JournalTipTapEditor } from './JournalTipTapEditor';
import { useJournalEditor } from '../hooks/useJournalEditor';
import { JOURNAL_EMOTIONS, JOURNAL_EMOTION_ORDER } from '../utils/journal-emotions';
import type { JournalEmotion } from '@/types';
import {
  ArrowLeft,
  Save,
  Sliders,
  Check,
  Calendar,
  Lightbulb,
  ChevronDown,
  Zap,
  BatteryCharging,
  Moon,
  Plus,
  Edit3,
  Flame,
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
  const {
    editorRef,
    isEditMode,
    editId,
    rowVersion,
    contentVersion,
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
    showPromptModal,
    setShowPromptModal,
    isAiDrawerOpen,
    setIsAiDrawerOpen,
    isSafetyModalOpen,
    safetySaved,
    setIsSafetyModalOpen,
    detectedCrisisKeywords,
    writingPrompts,
    handleResetToNewEntry,
    handleOpenAiDrawer,
    executeSave,
    wordCount,
    router,
    streakCount,
  } = useJournalEditor();
  const selectedMoodScore = MOOD_ICONS.reduce((closest, candidate) =>
    Math.abs(candidate.score - moodScore) < Math.abs(closest - moodScore) ? candidate.score : closest,
    MOOD_ICONS[0].score);

  return (
    <div className="w-full flex flex-col bg-bg-canvas min-h-[calc(100vh-80px)] pb-16 selection:bg-primary-container selection:text-black overflow-x-hidden relative">
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
      <main className="w-full max-w-7xl mx-auto px-4 sm:px-6 py-2 flex items-start justify-center transition-all duration-300">
        {/* PHYSICAL NOTEBOOK DESK CANVAS (Max-w 850px centered) */}
        <div
          className="w-full max-w-[850px] transition-all duration-300"
        >
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
              <div className="flex flex-wrap items-center gap-2 pt-1">
                {topics.map((tag, idx) => (
                  <span
                    key={idx}
                    className="inline-flex items-center gap-1 px-2.5 py-0.5 bg-white rounded-lg border border-black text-xs font-space font-bold shadow-neo-sm"
                  >
                    #{tag}
                    <button
                      type="button"
                      onClick={() => setTopics(topics.filter((_, i) => i !== idx))}
                      className="text-gray-400 hover:text-black ml-1 text-xs"
                    >
                      ×
                    </button>
                  </span>
                ))}
                <button
                  type="button"
                  onClick={() => setShowAddTopicModal(true)}
                  className="inline-flex items-center gap-1 px-2.5 py-0.5 bg-primary-container/40 hover:bg-primary-container rounded-lg border border-black text-xs font-space font-bold transition-all cursor-pointer shadow-neo-xs hover:-translate-y-0.5 active:translate-y-0.5"
                  title="Thêm chủ đề / hashtag"
                >
                  <Plus className="w-3 h-3" />
                  <span>Chủ đề</span>
                </button>
              </div>
            </header>

            {/* Prompt Helper Bar */}
            <div className="relative">
              <button
                type="button"
                onClick={() => setShowPromptModal(!showPromptModal)}
                className="inline-flex items-center gap-2 text-xs font-space font-bold text-gray-700 bg-white px-3 py-1.5 rounded-xl border border-black shadow-neo-sm hover:bg-lime-100 transition-all cursor-pointer"
              >
                <Lightbulb className="w-4 h-4 text-amber-500 fill-amber-300" />
                <span> Gợi ý chủ đề</span>
                <ChevronDown className={`w-3.5 h-3.5 transition-transform ${showPromptModal ? 'rotate-180' : ''}`} />
              </button>

              {showPromptModal && (
                <div className="absolute top-10 left-0 z-30 w-full sm:w-[420px] bg-white border-2 border-black rounded-2xl p-3 shadow-neo-lg animate-in fade-in">
                  <div className="flex items-center justify-between pb-2 mb-2 border-b border-black/10">
                    <span className="font-space text-xs font-bold text-gray-800">
                      Chọn câu hỏi để bắt đầu dòng chảy:
                    </span>
                    <button
                      type="button"
                      onClick={() => setShowPromptModal(false)}
                      className="text-xs font-bold text-gray-500 hover:text-black"
                    >
                      Đóng
                    </button>
                  </div>
                  <div className="flex flex-col gap-1.5 max-h-56 overflow-y-auto">
                    {writingPrompts.map((item, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => {
                          if (editorRef.current) {
                            editorRef.current.insertContent(`<blockquote>💡 <strong>[${item.category}]</strong> ${item.prompt}</blockquote><p></p>`);
                            editorRef.current.focus();
                          } else {
                            setContent((prev) => `${prev}<blockquote>💡 <strong>[${item.category}]</strong> ${item.prompt}</blockquote><p></p>`);
                          }
                          setShowPromptModal(false);
                          showToast({ title: 'Đã thêm gợi ý vào trang viết!', type: 'info' });
                        }}
                        className="text-left p-2 rounded-xl hover:bg-paper-warm border border-transparent hover:border-black transition-all cursor-pointer flex flex-col gap-0.5"
                      >
                        <span className="font-space text-[10px] font-extrabold uppercase text-purple-700">
                          {item.category}
                        </span>
                        <span className="font-serif text-xs text-gray-800">
                          {item.prompt}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>

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

            {/* Bottom Toolbar: Quiet & Friendly */}
            <div className="pt-4 border-t-2 border-black/15 flex flex-col gap-4">
              <div className="flex flex-wrap items-center justify-end gap-3">
                {/* Optional Detailed Metrics */}
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setShowDetailedMetrics(!showDetailedMetrics)}
                    className="text-xs font-space font-bold text-gray-700 hover:text-black underline flex items-center gap-1 cursor-pointer"
                  >
                    <Sliders className="w-3.5 h-3.5" />
                    <span>{showDetailedMetrics ? 'Ẩn chỉ số' : '+ Thêm chỉ số'}</span>
                  </button>
                </div>
              </div>

              {/* Expandable Health Metrics (Quiet Sliders) */}
              {showDetailedMetrics && (
                <div className="p-4 bg-white rounded-2xl border border-black shadow-neo-sm grid grid-cols-1 sm:grid-cols-3 gap-4 animate-in fade-in">
                  <div>
                    <div className="flex justify-between text-xs font-space font-bold mb-1">
                      <span className="flex items-center gap-1 text-gray-700">
                        <Zap className="w-3.5 h-3.5 text-amber-500" /> Áp lực (Stress):
                      </span>
                      <span>{stressScore}/10</span>
                    </div>
                    <input
                      type="range"
                      min="1"
                      max="10"
                      step="0.5"
                      value={stressScore}
                      onChange={(e) => setStressScore(parseFloat(e.target.value))}
                      className="w-full accent-amber-500 cursor-pointer"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between text-xs font-space font-bold mb-1">
                      <span className="flex items-center gap-1 text-gray-700">
                        <BatteryCharging className="w-3.5 h-3.5 text-green-600" /> Năng lượng:
                      </span>
                      <span>{energyScore}/10</span>
                    </div>
                    <input
                      type="range"
                      min="1"
                      max="10"
                      step="0.5"
                      value={energyScore}
                      onChange={(e) => setEnergyScore(parseFloat(e.target.value))}
                      className="w-full accent-green-600 cursor-pointer"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between text-xs font-space font-bold mb-1">
                      <span className="flex items-center gap-1 text-gray-700">
                        <Moon className="w-3.5 h-3.5 text-blue-500" /> Giấc ngủ:
                      </span>
                      <span>{sleepHours}h</span>
                    </div>
                    <input
                      type="range"
                      min="3"
                      max="12"
                      step="0.5"
                      value={sleepHours}
                      onChange={(e) => setSleepHours(parseFloat(e.target.value))}
                      className="w-full accent-blue-500 cursor-pointer"
                    />
                  </div>
                </div>
              )}

              {/* Action Buttons: 2 Clear Actions */}
              <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
                <span className="text-xs font-space font-bold text-gray-500">{wordCount} từ</span>
                <div className="flex items-center gap-3">
                  <NeoButton
                    variant="paper"
                    size="md"
                    onClick={handleOpenAiDrawer}
                    className="font-space font-bold text-xs sm:text-sm border-2 border-black shadow-neo hover:bg-lime-100"
                  >
                    <span>{isAiDrawerOpen ? 'Đang mở gợi ý' : 'Gợi ý nhìn lại'}</span>
                  </NeoButton>

                  <NeoButton
                    variant="primary"
                    size="md"
                    onClick={executeSave}
                    disabled={isSaving || (!!editId && loadedEntryId !== editId)}
                    className="font-space font-extrabold text-xs sm:text-sm shadow-neo"
                    icon={isEditMode ? <Check className="w-4 h-4 stroke-[2.5]" /> : <Save className="w-4 h-4 stroke-[2.5]" />}
                  >
                    <span>{isSaving ? 'Đang lưu...' : isEditMode ? 'Cập nhật bài viết' : 'Lưu'}</span>
                  </NeoButton>
                </div>
              </div>
            </div>
          </article>
        </div>
      </main>

      {/* AI REFLECTION DRAWER */}
      <AiReflectionDrawer
        isOpen={isAiDrawerOpen}
        onClose={() => setIsAiDrawerOpen(false)}
        entryId={editId}
        contentVersion={contentVersion}
      />

      {/* ADD TOPIC MODAL */}
      <AddTopicModal
        isOpen={showAddTopicModal}
        onClose={() => setShowAddTopicModal(false)}
        onAddTopic={(newTopic) => {
          setTopics((prev) => [...prev, newTopic]);
          showToast({
            title: 'Đã thêm chủ đề mới!',
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
