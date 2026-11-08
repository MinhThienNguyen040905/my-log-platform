'use client';

import { useEffect } from 'react';
import { HeartHandshake, X } from 'lucide-react';
import type { UseCompanionConversationReturn } from '../../hooks/useCompanionConversation';
import { CompanionMessageList } from './CompanionMessageList';
import { CompanionConsentCard } from './CompanionConsentCard';
import { CompanionClosingCard } from './CompanionClosingCard';
import { CompanionHabitCard } from './CompanionHabitCard';
import { CompanionComposer } from './CompanionComposer';
import { CompanionTitleSuggestions } from './CompanionTitleSuggestions';

interface CompanionPanelProps {
  conversation: UseCompanionConversationReturn;
}

const TOOLS_ONLY_NOTICE: Record<'DECLINED' | 'UNAVAILABLE', string> = {
  DECLINED: 'Bạn đã chọn để sau. Khi sẵn sàng, bấm "Đồng ý" ở trên để trò chuyện tiếp.',
  UNAVAILABLE: 'Trợ lý đang tạm nghỉ. Bạn vẫn có thể dùng các công cụ dịu phía trên trang viết.',
};

/**
 * Khung chat của trợ lý. Responsive theo COMPANION_UI_PLAN.md mục 2.3, rút gọn còn 2 biến thể
 * (dưới 768px: bottom sheet; từ 768px trở lên: panel nổi bên phải) thay vì 3, vì khác biệt giữa
 * "sticky cột thứ 3" và "fixed nổi" ở ≥1360px chỉ là hình thức, không đổi hành vi cho người dùng.
 */
export function CompanionPanel({ conversation }: CompanionPanelProps) {
  const {
    panelState,
    messages,
    chips,
    isTyping,
    inputValue,
    setInputValue,
    closingRating,
    setClosingRating,
    toolsOnlyReason,
    habitSuggestion,
    minimize,
  } = conversation;
  const isConsent = panelState === 'CONSENT';
  const isClosing = panelState === 'CLOSING';
  const isLocked = panelState === 'TOOLS_ONLY';

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        minimize();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [minimize]);

  return (
    <div
      role="complementary"
      aria-label="Trợ lý trang viết"
      className="fixed inset-x-3 bottom-[calc(env(safe-area-inset-bottom)+1rem)] z-40 flex h-[min(70dvh,640px)] flex-col overflow-hidden rounded-3xl border-2 border-black bg-paper-warm shadow-neo-lg animate-in fade-in slide-in-from-bottom-4 sm:inset-x-auto sm:bottom-4 sm:left-auto sm:right-4 sm:top-24 sm:h-auto sm:w-[380px] sm:rounded-2xl sm:slide-in-from-bottom-0"
    >
      <div className="flex items-center justify-between border-b border-black/15 p-3">
        <div className="flex items-center gap-2">
          <span className="inline-flex h-7 w-7 items-center justify-center rounded-xl border border-black bg-primary-container shadow-neo-xs">
            <HeartHandshake className="h-4 w-4 text-black" />
          </span>
          <div>
            <h3 className="font-space text-xs font-extrabold leading-tight text-black">Trợ lý trang viết</h3>
            <p className="text-[10px] font-medium text-gray-600">Đọc bài bạn đang viết · Không lưu đoạn chat</p>
          </div>
        </div>
        <div className="flex items-center gap-1">
          {messages.length > 0 && !isClosing && (
            <button
              type="button"
              onClick={conversation.requestClosing}
              aria-label="Khép lại cuộc trò chuyện"
              className="rounded-lg px-2 py-1 font-space text-[10px] font-bold text-gray-600 transition-colors hover:bg-white hover:text-black"
            >
              Khép lại
            </button>
          )}
          <button
            type="button"
            onClick={conversation.minimize}
            aria-label="Thu nhỏ khung chat"
            className="rounded-lg p-1 text-gray-700 transition-colors hover:bg-white focus-visible:outline-2 focus-visible:outline-black"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      </div>

      <CompanionTitleSuggestions getMood={conversation.getMoodCode} onApply={conversation.applyTitle} />

      <CompanionMessageList
        messages={messages}
        insertedQuestions={conversation.insertedQuestions}
        isTyping={isTyping}
        onInsertQuestion={conversation.insertQuestion}
        onApplyTitle={conversation.applyTitle}
        onRunAction={conversation.runAction}
      />

      {habitSuggestion && (
        <CompanionHabitCard
          suggestion={habitSuggestion}
          onAccept={conversation.acceptHabitSuggestion}
          onDismiss={conversation.dismissHabitSuggestion}
        />
      )}

      {isConsent && (
        <CompanionConsentCard onGrant={() => void conversation.grantConsent()} onDeclineForNow={conversation.declineConsentForNow} />
      )}

      {isClosing && (
        <CompanionClosingCard
          rating={closingRating}
          onRate={setClosingRating}
          pendingQuestionCount={conversation.pendingQuestionCount}
          onInsertQuestions={conversation.finishConversationIntoDraft}
          onJustClose={conversation.justClose}
        />
      )}

      {isLocked && (
        <p role="status" className="m-3 rounded-xl border border-dashed border-black/40 bg-white p-2 text-[11px] text-gray-600">
          {TOOLS_ONLY_NOTICE[toolsOnlyReason ?? 'UNAVAILABLE']}
        </p>
      )}

      {!isConsent && !isClosing && !isLocked && (
        <CompanionComposer
          value={inputValue}
          onChange={setInputValue}
          onSend={() => conversation.sendMessage(inputValue)}
          disabled={isTyping}
          chips={chips}
          onChip={conversation.sendChip}
        />
      )}
    </div>
  );
}
