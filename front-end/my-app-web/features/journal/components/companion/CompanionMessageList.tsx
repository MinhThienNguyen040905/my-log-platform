'use client';

import React from 'react';
import { Sparkles, BookOpen, Wind, Volume2, PenLine } from 'lucide-react';
import type { CompanionActionCode, CompanionMessage } from '../../types/companion.types';

const ACTION_ICON: Record<CompanionActionCode, React.ComponentType<{ className?: string }>> = {
  BREATHING_2MIN: Wind,
  AMBIENT: Volume2,
  VENT_PROMPT: PenLine,
  TITLE: Sparkles,
};

interface CompanionMessageListProps {
  messages: CompanionMessage[];
  isTyping: boolean;
  insertedQuestions: ReadonlySet<string>;
  onInsertQuestion: (question: string) => void;
  onApplyTitle: (title: string) => void;
  onRunAction: (code: CompanionActionCode) => void;
}

export function CompanionMessageList({ messages, isTyping, insertedQuestions, onInsertQuestion, onApplyTitle, onRunAction }: CompanionMessageListProps) {
  const bottomRef = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length, isTyping]);


  return (
    <div className="flex flex-1 flex-col gap-2.5 overflow-y-auto p-3" aria-live="polite" aria-label="Hội thoại với trợ lý">
      {messages.map((message) => (
        <div key={message.id} className={`flex flex-col gap-1.5 ${message.role === 'user' ? 'items-end' : 'items-start'}`}>
          <div
            className={`max-w-[90%] rounded-2xl border border-black px-3 py-2 text-xs leading-relaxed shadow-neo-xs ${
              message.role === 'user' ? 'bg-primary-container font-semibold text-black' : 'bg-white text-gray-900'
            }`}
          >
            <p>{message.text}</p>

            {message.suggestedTitle && (
              <div className="mt-2 rounded-xl border border-black bg-amber-50 p-2">
                <p className="font-space text-[11px] font-bold text-black">💡 “{message.suggestedTitle}”</p>
                <button
                  type="button"
                  onClick={() => onApplyTitle(message.suggestedTitle!)}
                  aria-label={`Điền làm tựa đề: ${message.suggestedTitle}`}
                  className="mt-1.5 inline-flex items-center gap-1 rounded-lg border border-black bg-white px-2 py-0.5 font-space text-[10px] font-bold hover:bg-lime-100"
                >
                  <Sparkles className="h-3 w-3" />
                  <span>Điền làm tựa đề</span>
                </button>
              </div>
            )}

            {message.questions?.map((question, index) => {
              const key = `${message.id}:${index}`;
              const done = insertedQuestions.has(question);
              return (
                <div key={key} className="mt-2 border-t border-black/10 pt-2">
                  <p className="font-medium text-black">“{question}”</p>
                  <div className="mt-1.5 flex justify-end">
                    <button
                      type="button"
                      disabled={done}
                      aria-label={done ? 'Đã chèn câu hỏi vào bài' : `Chèn câu hỏi vào bài: ${question}`}
                      onClick={() => {
                        onInsertQuestion(question);
                      }}
                      className="inline-flex items-center gap-1 rounded-lg border border-black bg-paper-warm px-2 py-0.5 font-space text-[10px] font-bold transition-colors hover:bg-lime-100 disabled:opacity-60"
                    >
                      <BookOpen className="h-3 w-3" />
                      <span>{done ? 'Đã chèn vào bài' : '+ Chèn câu hỏi vào bài'}</span>
                    </button>
                  </div>
                </div>
              );
            })}

            {message.actions && message.actions.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1.5 border-t border-black/10 pt-2">
                {message.actions.map((action) => {
                  const Icon = ACTION_ICON[action.code];
                  return (
                    <button
                      key={action.code}
                      type="button"
                      aria-label={action.label}
                      onClick={() => onRunAction(action.code)}
                      className="inline-flex items-center gap-1 rounded-lg border border-black bg-sky-50 px-2 py-1 font-space text-[11px] font-bold transition-all hover:-translate-y-0.5 hover:bg-sky-100 hover:shadow-neo-xs"
                    >
                      <Icon className="h-3.5 w-3.5" />
                      <span>{action.label}</span>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      ))}

      {isTyping && (
        <div className="inline-flex w-fit items-center gap-1.5 rounded-2xl border border-black bg-white px-3 py-2 text-xs text-gray-500 shadow-neo-xs">
          <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-black" />
          <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-black [animation-delay:150ms]" />
          <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-black [animation-delay:300ms]" />
          <span className="ml-1 font-space text-[11px] font-bold">Đang lắng nghe...</span>
        </div>
      )}
      <div ref={bottomRef} />
    </div>
  );
}
