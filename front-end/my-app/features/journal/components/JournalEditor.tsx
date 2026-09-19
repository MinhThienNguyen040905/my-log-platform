'use client';

import React, { Suspense } from 'react';
import Link from 'next/link';
import { useToast } from '@/lib/toast-context';
import { MoodType } from '@/types';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { SafetyModal } from './SafetyModal';
import { AiReflectionDrawer } from './AiReflectionDrawer';
import { ConfirmReflectModal } from './ConfirmReflectModal';
import { InsertImageModal } from './InsertImageModal';
import { AddTopicModal } from './AddTopicModal';
import { JournalTipTapEditor } from './JournalTipTapEditor';
import { useJournalEditor } from '../hooks/useJournalEditor';
import {
  ArrowLeft,
  Sparkles,
  Save,
  Sliders,
  Check,
  Calendar,
  MapPin,
  Image as ImageIcon,
  CheckCircle2,
  Lightbulb,
  ChevronDown,
  Paperclip,
  Zap,
  BatteryCharging,
  Moon,
  Plus,
  Frown,
  Annoyed,
  Meh,
  Smile,
  Laugh,
  Edit3,
  RotateCcw,
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

const MOOD_ICONS: {
  score: number;
  mood: MoodType;
  icon: React.ElementType;
  label: string;
  iconColor: string;
  activeBg: string;
  hoverBg: string;
}[] = [
  {
    score: 2.0,
    mood: 'anxiety-stress',
    icon: Frown,
    label: 'Mệt mỏi / Trầm',
    iconColor: 'text-rose-500',
    activeBg: 'bg-rose-300',
    hoverBg: 'hover:bg-rose-50',
  },
  {
    score: 4.5,
    mood: 'anxiety-stress',
    icon: Annoyed,
    label: 'Áp lực / Bối rối',
    iconColor: 'text-amber-500',
    activeBg: 'bg-amber-300',
    hoverBg: 'hover:bg-amber-50',
  },
  {
    score: 6.0,
    mood: 'neutral',
    icon: Meh,
    label: 'Bình thường',
    iconColor: 'text-slate-500',
    activeBg: 'bg-slate-200',
    hoverBg: 'hover:bg-slate-100',
  },
  {
    score: 8.0,
    mood: 'calm-joy',
    icon: Smile,
    label: 'Thư thái / Tốt',
    iconColor: 'text-lime-600',
    activeBg: 'bg-primary-container',
    hoverBg: 'hover:bg-lime-50',
  },
  {
    score: 9.5,
    mood: 'hope-energy',
    icon: Laugh,
    label: 'Hào hứng / Tuyệt vời',
    iconColor: 'text-yellow-500',
    activeBg: 'bg-yellow-300',
    hoverBg: 'hover:bg-yellow-50',
  },
];

function JournalEditorContent() {
  const { showToast } = useToast();
  const {
    editorRef,
    isEditMode,
    existingDate,
    title,
    setTitle,
    content,
    setContent,
    mood,
    setMood,
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
    showImageModal,
    setShowImageModal,
    showPromptModal,
    setShowPromptModal,
    isAiDrawerOpen,
    setIsAiDrawerOpen,
    isAnalyzing,
    showConfirmReflectModal,
    setShowConfirmReflectModal,
    isSafetyModalOpen,
    setIsSafetyModalOpen,
    detectedCrisisKeywords,
    lastSavedTime,
    aiResult,
    writingPrompts,
    handleResetToNewEntry,
    handleOpenAiDrawer,
    handleAnswerQuestionInJournal,
    executeSave,
    handleSaveClick,
    wordCount,
    router,
  } = useJournalEditor();

  return (
    <div className="w-full flex flex-col bg-bg-canvas min-h-[calc(100vh-80px)] pb-16 selection:bg-primary-container selection:text-black overflow-x-hidden relative">
      {/* Top Quiet Meta Bar */}
      <section className="w-full max-w-4xl mx-auto px-4 sm:px-6 pt-4 pb-2 flex flex-wrap items-center justify-between gap-3 text-xs font-space font-bold text-gray-600">
        <div className="flex items-center gap-2">
          <Link
            href="/history-calendar"
            className="inline-flex items-center gap-1 text-gray-700 hover:text-black hover:underline"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Lịch ký ức</span>
          </Link>

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
              <Sparkles className="w-3.5 h-3.5 text-black" />
              <span>Trang viết mới hôm nay</span>
            </span>
          )}
        </div>

        <div className="flex items-center gap-3">
          <span className="text-gray-500">
            {wordCount} từ
          </span>
          {lastSavedTime && (
            <span className="inline-flex items-center gap-1 text-green-700 font-bold bg-white px-2.5 py-1 rounded-full border border-black/30 shadow-neo-sm animate-in fade-in">
              <CheckCircle2 className="w-3.5 h-3.5" />
              Đã lưu nháp {lastSavedTime}
            </span>
          )}
        </div>
      </section>

      {/* Main Screen Layout Container */}
      <main className="w-full max-w-7xl mx-auto px-4 sm:px-6 py-2 flex items-start justify-center transition-all duration-300">
        {/* PHYSICAL NOTEBOOK DESK CANVAS (Max-w 850px centered) */}
        <div
          className={`w-full transition-all duration-300 ${
            isAiDrawerOpen ? 'lg:max-w-3xl lg:mr-[420px]' : 'max-w-[850px]'
          }`}
        >
          <article className="relative bg-paper-warm border-[2.5px] border-black rounded-3xl p-6 sm:p-10 shadow-neo-lg transition-all overflow-visible flex flex-col gap-6">
            {/* Washi Tape and Page Number Stamp */}
            <WashiTape color={isEditMode ? 'peach' : 'lime'} rotate={-2} className="absolute -top-3.5 left-12 w-32 z-10" />
            
            {/* Contextual Scrapbook Page Stamp */}
            <div
              className={`absolute -top-3 right-10 z-10 border border-black px-3 py-0.5 rounded-full font-space text-[10px] font-extrabold shadow-neo-sm uppercase flex items-center gap-1 ${
                isEditMode
                  ? 'bg-amber-300 text-black'
                  : 'bg-primary-container text-black'
              }`}
            >
              {isEditMode ? (
                <>
                  <Edit3 className="w-3 h-3" />
                  <span>Chế độ sửa bài</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-3 h-3" />
                  <span>Trang hôm nay</span>
                </>
              )}
            </div>

            {/* Notebook Header */}
            <header className="flex flex-col gap-3 pb-4 border-b-2 border-black/15">
              <div className="flex flex-wrap items-center justify-between gap-2 text-xs font-space font-bold text-gray-700">
                <span className="inline-flex items-center gap-1.5 bg-white px-3 py-1 rounded-xl border border-black shadow-neo-sm">
                  <Calendar className="w-3.5 h-3.5 text-black" />
                  {isEditMode && existingDate
                    ? `Ngày ghi: ${existingDate}`
                    : `Hôm nay, ${new Date().toLocaleDateString('vi-VN', {
                        weekday: 'long',
                        day: 'numeric',
                        month: 'long',
                        year: 'numeric',
                      })}`}
                </span>
                <span className="inline-flex items-center gap-1.5 bg-white px-3 py-1 rounded-xl border border-black/40">
                  <MapPin className="w-3.5 h-3.5 text-gray-500" />
                  Hà Nội, 24°C
                </span>
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
                <span>💡 Gợi ý chủ đề viết hôm nay</span>
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
              <JournalTipTapEditor
                ref={editorRef}
                initialContent={content}
                onChange={(html) => setContent(html)}
                onOpenImageModal={() => setShowImageModal(true)}
              />
            </div>

            {/* Bottom Toolbar: Quiet & Friendly */}
            <div className="pt-4 border-t-2 border-black/15 flex flex-col gap-4">
              {/* Mood 5-point Selector */}
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <span className="font-space text-xs font-extrabold uppercase text-gray-600">
                    Hôm nay bạn thấy thế nào?
                  </span>
                  <div className="flex items-center gap-1.5 bg-white py-1 px-2.5 rounded-2xl border border-black shadow-neo-sm">
                    {MOOD_ICONS.map((item) => {
                      const isSelected = Math.abs(moodScore - item.score) < 1.0;
                      const IconComponent = item.icon;
                      const isHovered = hoveredMoodScore === item.score;

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
                              setMood(item.mood);
                            }}
                            onMouseEnter={() => setHoveredMoodScore(item.score)}
                            onMouseLeave={() => setHoveredMoodScore(null)}
                            className={`w-8 h-8 sm:w-9 sm:h-9 rounded-xl flex items-center justify-center transition-all cursor-pointer ${
                              isSelected
                                ? `${item.activeBg} border border-black shadow-neo-sm scale-110 text-black`
                                : `opacity-85 hover:opacity-100 ${item.hoverBg}`
                            }`}
                            title={item.label}
                          >
                            <IconComponent
                              className={`w-5 h-5 stroke-[2.3] transition-transform ${
                                isSelected ? 'text-black' : item.iconColor
                              }`}
                            />
                          </button>
                        </div>
                      );
                    })}
                  </div>
                </div>

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
                <button
                  type="button"
                  onClick={() => router.back()}
                  className="px-4 py-2 bg-white text-black font-space text-xs font-bold border border-black rounded-xl shadow-neo-sm hover:bg-gray-100 transition-all cursor-pointer"
                >
                  Đóng sổ
                </button>

                <div className="flex items-center gap-3">
                  <NeoButton
                    variant="paper"
                    size="md"
                    onClick={handleOpenAiDrawer}
                    className="font-space font-bold text-xs sm:text-sm border-2 border-black shadow-neo hover:bg-lime-100"
                    icon={<Sparkles className="w-4 h-4 text-purple-700" />}
                  >
                    <span>{isAiDrawerOpen ? 'Đang mở phản chiếu' : 'Phản chiếu cùng MyLog'}</span>
                  </NeoButton>

                  <NeoButton
                    variant="primary"
                    size="md"
                    onClick={handleSaveClick}
                    className="font-space font-extrabold text-xs sm:text-sm shadow-neo"
                    icon={isEditMode ? <Check className="w-4 h-4 stroke-[2.5]" /> : <Save className="w-4 h-4 stroke-[2.5]" />}
                  >
                    <span>{isEditMode ? 'Cập nhật bài viết' : 'Lưu vào sổ tay'}</span>
                  </NeoButton>
                </div>
              </div>
            </div>
          </article>
        </div>

        {/* AI REFLECTION DRAWER (ON-DEMAND) */}
        <AiReflectionDrawer
          isOpen={isAiDrawerOpen}
          onClose={() => setIsAiDrawerOpen(false)}
          isAnalyzing={isAnalyzing}
          aiResult={aiResult}
          topics={topics}
          onInsertQuestionToJournal={handleAnswerQuestionInJournal}
          onSave={executeSave}
        />
      </main>

      {/* CONFIRM REFLECT MODAL (Appears on save if user hasn't clicked Reflect) */}
      <ConfirmReflectModal
        isOpen={showConfirmReflectModal}
        onConfirmReflect={() => {
          setShowConfirmReflectModal(false);
          handleOpenAiDrawer();
        }}
        onDirectSave={() => {
          setShowConfirmReflectModal(false);
          executeSave();
        }}
      />

      {/* INSERT IMAGE MODAL */}
      <InsertImageModal
        isOpen={showImageModal}
        onClose={() => setShowImageModal(false)}
        onInsert={(url, caption) => {
          if (editorRef.current) {
            editorRef.current.insertImage(url, caption);
            if (caption) {
              editorRef.current.insertContent(`<p style="text-align: center; font-size: 0.85rem; font-style: italic; color: #6b7280; margin-top: -0.5rem;">${caption}</p>`);
            }
          }
          showToast({ title: 'Đã dán ảnh vào trang sổ!', type: 'success' });
        }}
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
        onClose={() => setIsSafetyModalOpen(false)}
        onNavigateHome={() => router.push('/dashboard')}
        detectedKeywords={detectedCrisisKeywords}
      />
    </div>
  );
}
