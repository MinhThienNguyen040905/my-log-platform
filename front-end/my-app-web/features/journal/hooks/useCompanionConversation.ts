'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import type { JournalEmotion } from '@/types';
import { ApiError } from '@/lib/api/client';
import { dismissCompanionHabitSuggestion, grantCompanionConsent, postCompanionTurn } from '../api/companion';
import { buildCompanionDraft } from '../utils/companion-draft';
import type {
  CompanionActionCode,
  CompanionChipOption,
  CompanionHabitSuggestion,
  CompanionHistoryTurn,
  CompanionMessage,
  CompanionPanelState,
  CompanionTurnRequest,
  CompanionTurnResponse,
} from '../types/companion.types';

const MAX_HISTORY_TURNS = 8;

interface MoodSnapshot {
  selectedEmotion: JournalEmotion | null;
  stressScore: number;
  energyScore: number;
  sleepHours: number;
}

interface UseCompanionConversationOptions {
  getDraft: () => { title: string; text: string };
  getMood: () => MoodSnapshot;
  onInsertQuestion: (question: string) => void;
  onApplyTitle: (title: string) => void;
  /** Chỉ các câu hỏi gợi ý chưa chèn — không bao giờ có câu chào/cảm ơn của trợ lý hay chữ trên chip. */
  onInsertQuestions: (questions: string[]) => void;
  onRunAction: (code: CompanionActionCode) => void;
  onAcceptHabitSuggestion?: (suggestion: CompanionHabitSuggestion) => void;
  onSafetyTriggered: () => void;
}

let seq = 0;
const nextId = (prefix: string) => `${prefix}-${++seq}`;

/**
 * Companion state machine (xem COMPANION_UI_PLAN.md mục 3.1). Hội thoại sống ở đây, trong
 * component cha — thu nhỏ panel chỉ ẩn UI, không reset state, nên mở lại vẫn còn hội thoại.
 * Không lưu gì xuống localStorage/server; rời trang là mất.
 */
export function useCompanionConversation(options: UseCompanionConversationOptions) {
  const optionsRef = useRef(options);
  useEffect(() => {
    optionsRef.current = options;
  });

  const [isOpen, setIsOpen] = useState(false);
  const [hasStarted, setHasStarted] = useState(false);
  const [unread, setUnread] = useState(false);
  const [panelState, setPanelState] = useState<CompanionPanelState>('LISTENING');
  const [messages, setMessages] = useState<CompanionMessage[]>([]);
  const [chips, setChips] = useState<CompanionChipOption[]>([]);
  const [isTyping, setIsTyping] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [closingRating, setClosingRating] = useState<number | null>(null);
  const [pendingMessage, setPendingMessage] = useState<string | null>(null);
  const [toolsOnlyReason, setToolsOnlyReason] = useState<'DECLINED' | 'UNAVAILABLE' | null>(null);
  const [habitSuggestion, setHabitSuggestion] = useState<CompanionHabitSuggestion | null>(null);
  const [insertedQuestions, setInsertedQuestions] = useState<ReadonlySet<string>>(new Set());

  const historyRef = useRef<CompanionHistoryTurn[]>([]);
  const abortRef = useRef<AbortController | null>(null);
  const prevMessageCountRef = useRef(0);
  const habitShownTodayRef = useRef(false);

  useEffect(() => {
    if (messages.length > prevMessageCountRef.current && !isOpen) setUnread(true);
    prevMessageCountRef.current = messages.length;
  }, [messages.length, isOpen]);

  useEffect(() => () => abortRef.current?.abort(), []);

  const appendHistory = useCallback((turn: CompanionHistoryTurn) => {
    historyRef.current = [...historyRef.current, turn].slice(-MAX_HISTORY_TURNS);
  }, []);

  const resetConversation = useCallback(() => {
    setMessages([]);
    setChips([]);
    historyRef.current = [];
    setHasStarted(false);
    setPanelState('LISTENING');
    setPendingMessage(null);
    setClosingRating(null);
    setToolsOnlyReason(null);
    setHabitSuggestion(null);
    setInsertedQuestions(new Set());
  }, []);

  const handleUnavailable = useCallback(() => {
    setPanelState('TOOLS_ONLY');
    setToolsOnlyReason('UNAVAILABLE');
    setChips([]);
    // Không thêm bong bóng chat: ô thông báo "tạm nghỉ" của CompanionPanel đã nói điều này (tránh hiện 2 lần).
  }, []);

  const handleResponse = useCallback(
    (response: CompanionTurnResponse, sentMessage: string) => {
      switch (response.status) {
        case 'CONSENT_REQUIRED':
          setPendingMessage(sentMessage);
          setPanelState('CONSENT');
          return;
        case 'SAFETY_FLOW':
          resetConversation();
          setIsOpen(false);
          optionsRef.current.onSafetyTriggered();
          return;
        case 'UNAVAILABLE':
          handleUnavailable();
          return;
        case 'BLOCKED':
        case 'REPLY':
        default: {
          const text = response.reply ?? '…';
          appendHistory({ role: 'assistant', text });
          setMessages((prev) => [
            ...prev,
            {
              id: nextId('assistant'),
              role: 'assistant',
              text,
              questions: response.questions,
              suggestedTitle: response.suggestedTitle,
              actions: response.immediateActions,
            },
          ]);
          setChips(response.chips);
          const currentMood = optionsRef.current.getMood().selectedEmotion;
          if (response.habitSuggestion && !habitShownTodayRef.current && currentMood !== 'very_bad') {
            habitShownTodayRef.current = true;
            setHabitSuggestion(response.habitSuggestion);
          }
          setPanelState(response.nextState === 'CLOSING' ? 'CLOSING' : 'LISTENING');
        }
      }
    },
    [appendHistory, handleUnavailable, resetConversation],
  );

  const buildRequest = useCallback(
    (intent: CompanionTurnRequest['intent'], message: string, completedTool?: CompanionActionCode): CompanionTurnRequest => {
      const draft = optionsRef.current.getDraft();
      const mood = optionsRef.current.getMood();
      return {
        intent,
        message,
        draft: buildCompanionDraft(draft.title, draft.text),
        context: {
          moodCode: mood.selectedEmotion,
          stressScore: mood.stressScore,
          energyScore: mood.energyScore,
          sleepMinutes: Math.round(mood.sleepHours * 60),
          localHour: new Date().getHours(),
        },
        completedTool,
        history: historyRef.current,
      };
    },
    [],
  );

  const runTurn = useCallback(
    async (intent: CompanionTurnRequest['intent'], message: string, completedTool?: CompanionActionCode) => {
      abortRef.current?.abort();
      const controller = new AbortController();
      abortRef.current = controller;
      setIsTyping(true);
      try {
        const request = buildRequest(intent, message, completedTool);
        const response = await postCompanionTurn(request, controller.signal);
        if (controller.signal.aborted) return;
        handleResponse(response, message);
      } catch (error) {
        if (controller.signal.aborted) return;
        // Phiên đăng nhập hết hạn: `AuthGate` đã tự polling và sẽ chuyển về /auth/login ngay sau
        // đây — không hiện thông báo "trợ lý tạm nghỉ" gây hiểu nhầm, chỉ dừng hiệu ứng đang gõ.
        if (error instanceof ApiError && error.status === 401) return;
        handleUnavailable();
      } finally {
        if (!controller.signal.aborted) setIsTyping(false);
      }
    },
    [buildRequest, handleResponse, handleUnavailable],
  );

  const open = useCallback(() => {
    setIsOpen(true);
    setUnread(false);
    if (!hasStarted) {
      setHasStarted(true);
      void runTurn('OPEN', '');
    }
  }, [hasStarted, runTurn]);

  const minimize = useCallback(() => setIsOpen(false), []);

  const sendMessage = useCallback(
    (text: string) => {
      const trimmed = text.trim();
      if (!trimmed) return;
      setMessages((prev) => [...prev, { id: nextId('user'), role: 'user', text: trimmed }]);
      appendHistory({ role: 'user', text: trimmed });
      setInputValue('');
      setPanelState('LISTENING');
      void runTurn('FREE_TEXT', trimmed);
    },
    [appendHistory, runTurn],
  );

  const sendChip = useCallback(
    (chip: CompanionChipOption) => {
      if (chip.intent === 'EXIT') {
        minimize();
        return;
      }
      sendMessage(chip.label);
    },
    [minimize, sendMessage],
  );

  const insertQuestion = useCallback((question: string) => {
    setInsertedQuestions((prev) => new Set(prev).add(question));
    optionsRef.current.onInsertQuestion(question);
  }, []);

  /** Câu hỏi gợi ý trong cuộc trò chuyện chưa được chèn vào bài, theo thứ tự xuất hiện, không trùng. */
  const pendingQuestions = Array.from(
    new Set(messages.filter((m) => m.role === 'assistant').flatMap((m) => m.questions ?? [])),
  ).filter((question) => !insertedQuestions.has(question));
  const applyTitle = useCallback((title: string) => optionsRef.current.onApplyTitle(title), []);
  const getMoodCode = useCallback(() => optionsRef.current.getMood().selectedEmotion, []);
  const runAction = useCallback((code: CompanionActionCode) => optionsRef.current.onRunAction(code), []);

  /** JournalEditor gọi khi một tool bấm từ TRONG chat (không phải từ menu Công cụ dịu) kết thúc tự nhiên. */
  const notifyToolCompleted = useCallback(
    (tool: 'BREATHING' | 'AMBIENT') => {
      const code: CompanionActionCode = tool === 'BREATHING' ? 'BREATHING_2MIN' : 'AMBIENT';
      void runTurn('TOOL_COMPLETED', '', code);
    },
    [runTurn],
  );

  const grantConsent = useCallback(async () => {
    await grantCompanionConsent();
    const resend = pendingMessage;
    setPendingMessage(null);
    setToolsOnlyReason(null);
    setPanelState('LISTENING');
    if (resend !== null) void runTurn(resend === '' ? 'OPEN' : 'FREE_TEXT', resend);
  }, [pendingMessage, runTurn]);

  const declineConsentForNow = useCallback(() => {
    setPendingMessage(null);
    setToolsOnlyReason('DECLINED');
    setPanelState('TOOLS_ONLY');
  }, []);

  /** Kết thúc hẳn cuộc trò chuyện: lần mở trợ lý sau sẽ chào lại từ đầu (khác nút X chỉ thu nhỏ). */
  const endConversation = useCallback(() => {
    abortRef.current?.abort();
    setIsTyping(false);
    resetConversation();
    setIsOpen(false);
  }, [resetConversation]);

  const finishConversationIntoDraft = useCallback(() => {
    if (pendingQuestions.length > 0) optionsRef.current.onInsertQuestions(pendingQuestions);
    endConversation();
  }, [pendingQuestions, endConversation]);

  const justClose = endConversation;

  const requestClosing = useCallback(() => {
    setChips([]);
    void runTurn('CLOSING', '');
  }, [runTurn]);

  const acceptHabitSuggestion = useCallback((suggestion: CompanionHabitSuggestion) => {
    setHabitSuggestion(null);
    optionsRef.current.onAcceptHabitSuggestion?.(suggestion);
  }, []);

  const dismissHabitSuggestion = useCallback(() => {
    setHabitSuggestion(null);
    void dismissCompanionHabitSuggestion().catch(() => {});
  }, []);

  return {
    isOpen,
    unread,
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
    open,
    minimize,
    sendMessage,
    sendChip,
    insertQuestion,
    getMoodCode,
    insertedQuestions,
    pendingQuestionCount: pendingQuestions.length,
    applyTitle,
    runAction,
    notifyToolCompleted,
    grantConsent,
    declineConsentForNow,
    finishConversationIntoDraft,
    justClose,
    requestClosing,
    acceptHabitSuggestion,
    dismissHabitSuggestion,
  };
}

export type UseCompanionConversationReturn = ReturnType<typeof useCompanionConversation>;
