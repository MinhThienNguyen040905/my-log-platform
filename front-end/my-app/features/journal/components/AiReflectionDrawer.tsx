'use client';

import React, { useState } from 'react';
import {
  Sparkles,
  Save,
  Check,
  X,
  RefreshCw,
  Lightbulb,
  Edit3,
  Compass,
  ChevronDown,
} from 'lucide-react';
import { NeoButton } from '@/components/ui/NeoButton';
import { useToast } from '@/lib/toast-context';
import { AIAnalysisResult } from '@/lib/types';

interface AiReflectionDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  isAnalyzing: boolean;
  aiResult: AIAnalysisResult | null;
  topics: string[];
  onInsertQuestionToJournal: (question: string) => void;
  onSave: () => void;
}

export const AiReflectionDrawer: React.FC<AiReflectionDrawerProps> = ({
  isOpen,
  onClose,
  isAnalyzing,
  aiResult,
  topics,
  onInsertQuestionToJournal,
  onSave,
}) => {
  const { showToast } = useToast();
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [actionAccepted, setActionAccepted] = useState<boolean | null>(null);
  const [showEmotionSpectrum, setShowEmotionSpectrum] = useState(false);

  if (!isOpen) return null;

  const questions = aiResult?.reflectionQuestions || [];
  const activeQuestion =
    questions.length > 0
      ? questions[currentQuestionIndex % questions.length]
      : aiResult?.reflectionPrompt || 'Khi nhìn lại ngày hôm nay, điều gì khiến bạn thấy trân quý nhất?';

  const handleNextQuestion = () => {
    if (questions.length === 0) return;
    setCurrentQuestionIndex((prev) => (prev + 1) % questions.length);
  };

  return (
    <>
      {/* Backdrop overlay for mobile & tablet focus */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/40 backdrop-blur-xs z-40 lg:hidden animate-in fade-in duration-200"
        aria-hidden="true"
      />

      {/* Slide-over Drawer (Pinned cleanly to screen right edge) */}
      <aside
        className="fixed top-20 right-0 w-full sm:w-[420px] h-[calc(100vh-80px)] bg-surface-card border-l-2 border-black shadow-neo-lg z-50 p-6 flex flex-col justify-between overflow-y-auto animate-in slide-in-from-right duration-300"
      >
        <div className="flex flex-col gap-5">
        {/* Drawer Header */}
        <div className="flex items-center justify-between pb-3 border-b-2 border-black">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-primary-container border border-black flex items-center justify-center font-bold shadow-neo-sm">
              <Sparkles className="w-4 h-4 text-black" />
            </div>
            <div>
              <h3 className="font-space text-base font-extrabold text-on-surface">
                MyLog Phản Chiếu
              </h3>
              <span className="font-sans text-[11px] text-gray-600 block">
                Lắng nghe và tự thấu cảm
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-xl bg-paper-warm border border-black flex items-center justify-center text-gray-700 hover:text-black hover:bg-white transition-all cursor-pointer shadow-neo-sm"
            title="Đóng drawer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {isAnalyzing ? (
          <div className="py-12 flex flex-col items-center justify-center gap-3 text-center">
            <div className="w-10 h-10 rounded-full bg-primary-container border border-black flex items-center justify-center animate-bounce">
              <Sparkles className="w-5 h-5 text-black" />
            </div>
            <p className="font-space text-xs font-bold text-gray-700">
              MyLog đang đọc và lắng nghe tâm tư của bạn...
            </p>
          </div>
        ) : (
          <div className="flex flex-col gap-4">
            {/* Empathetic Summary */}
            <div className="p-4 bg-paper-warm rounded-2xl border border-black shadow-neo-sm">
              <p className="font-serif italic text-sm text-on-surface leading-relaxed">
                &ldquo;{aiResult?.summary}&rdquo;
              </p>
            </div>

            {/* 1 Single Thought-Provoking Question */}
            <div className="p-4 bg-white rounded-2xl border border-black shadow-neo-sm flex flex-col gap-3">
              <div className="flex items-center justify-between border-b border-black/10 pb-2">
                <span className="font-space text-xs font-extrabold uppercase text-gray-700 flex items-center gap-1.5">
                  <Lightbulb className="w-3.5 h-3.5 text-amber-500 fill-amber-300" />
                  Một điều để suy ngẫm
                </span>
                <button
                  type="button"
                  onClick={handleNextQuestion}
                  className="text-[11px] font-space font-bold underline text-gray-600 hover:text-black flex items-center gap-1 cursor-pointer"
                >
                  <RefreshCw className="w-3 h-3" /> Đổi câu hỏi
                </button>
              </div>

              <p className="font-serif text-sm font-semibold text-on-surface leading-relaxed">
                {activeQuestion}
              </p>

              <button
                type="button"
                onClick={() => onInsertQuestionToJournal(activeQuestion)}
                className="w-full py-2 px-3 bg-primary-container hover:bg-lime-300 text-black border border-black rounded-xl font-space text-xs font-bold shadow-neo-sm transition-all flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <Edit3 className="w-3.5 h-3.5" />
                <span>Viết tiếp vào sổ tay</span>
              </button>
            </div>

            {/* 1 Gentle Action for Tomorrow */}
            <div className="p-4 bg-white rounded-2xl border border-black shadow-neo-sm flex flex-col gap-2.5">
              <span className="font-space text-xs font-extrabold uppercase text-gray-700 flex items-center gap-1.5">
                <Compass className="w-3.5 h-3.5 text-purple-600" />
                Bước nhỏ cho ngày mai
              </span>
              <p className="font-sans text-xs text-on-surface leading-relaxed">
                {aiResult?.mindfulAction || aiResult?.suggestedAction}
              </p>

              <div className="flex items-center gap-2 pt-2 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => {
                    setActionAccepted(true);
                    showToast({ title: 'Đã nhận thử thách nhỏ!', type: 'success' });
                  }}
                  className={`flex-1 py-1.5 text-xs font-space font-bold rounded-xl border border-black transition-all cursor-pointer flex items-center justify-center gap-1 ${
                    actionAccepted === true ? 'bg-primary-container text-black' : 'bg-paper-warm hover:bg-white'
                  }`}
                >
                  <Check className="w-3 h-3 stroke-[2.5]" />
                  <span>Tôi muốn thử</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setActionAccepted(false);
                    showToast({ title: 'Để sau nhé!', type: 'info' });
                  }}
                  className="px-3 py-1.5 text-xs font-space font-bold rounded-xl border border-black/40 hover:bg-gray-100 text-gray-500 cursor-pointer"
                >
                  Để sau
                </button>
              </div>
            </div>

            {/* Expandable Emotion Details (Optional) */}
            <div className="pt-1">
              <button
                type="button"
                onClick={() => setShowEmotionSpectrum(!showEmotionSpectrum)}
                className="w-full flex items-center justify-between text-xs font-space font-bold text-gray-600 hover:text-black py-1 cursor-pointer"
              >
                <span>Xem phổ cảm xúc & chủ đề</span>
                <ChevronDown className={`w-3.5 h-3.5 transition-transform ${showEmotionSpectrum ? 'rotate-180' : ''}`} />
              </button>

              {showEmotionSpectrum && (
                <div className="mt-2 p-3 bg-paper-warm rounded-2xl border border-black flex flex-col gap-3 animate-in fade-in">
                  <div className="flex flex-col gap-1.5">
                    {aiResult?.emotions.map((em, i) => (
                      <div key={i} className="flex flex-col gap-0.5">
                        <div className="flex justify-between text-[11px] font-space font-bold">
                          <span>{em.label}</span>
                          <span>{em.percentage}%</span>
                        </div>
                        <div className="w-full h-1.5 bg-white rounded-full border border-black/30 overflow-hidden">
                          <div
                            style={{ width: `${em.percentage}%`, backgroundColor: em.color }}
                            className="h-full rounded-full"
                          />
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Extracted Topics */}
                  <div className="flex flex-wrap gap-1.5 pt-2 border-t border-black/10">
                    {topics.map((t, idx) => (
                      <span
                        key={idx}
                        className="text-[10px] font-space font-bold bg-white px-2 py-0.5 rounded-md border border-black/30 text-gray-700"
                      >
                        #{t}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Bottom Drawer CTA */}
      <div className="pt-4 border-t border-black/15 flex flex-col gap-2">
        <NeoButton
          variant="primary"
          size="md"
          onClick={onSave}
          className="w-full font-space font-extrabold text-xs shadow-neo"
          icon={<Save className="w-4 h-4" />}
        >
          Lưu vào Lịch ký ức
        </NeoButton>
      </div>
      </aside>
    </>
  );
};
