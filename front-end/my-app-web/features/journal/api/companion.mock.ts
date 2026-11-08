/**
 * DEV-ONLY DEMO ADAPTER. Lets Bước 2 (COMPANION_UI_PLAN.md) exercise the whole panel without a
 * running backend. Never calls any network, never leaves this file. Only reachable when
 * `NODE_ENV !== 'production'` AND `NEXT_PUBLIC_COMPANION_MOCK=1` — both are inlined at build time,
 * so a production build dead-code-eliminates the branch that imports this module; see
 * features/journal/api/companion.ts. Deleted once Bước 3 wires the real endpoint for good.
 *
 * QA trigger words (type as the chat message, not case sensitive) to reach every status without a
 * backend: "test an toàn" → SAFETY_FLOW, "test chặn" → BLOCKED, "test lỗi" → UNAVAILABLE. Anything
 * else gets a REPLY. Consent starts withheld; granting it (via the UI) calls `grantMockConsent()`.
 */
import type {
  CompanionActionCode,
  CompanionTurnRequest,
  CompanionTurnResponse,
} from '../types/companion.types';

let consentGranted = false;
let habitDismissed = false;

export function grantMockConsent(): void {
  consentGranted = true;
}

export function dismissMockHabitSuggestion(): void {
  habitDismissed = true;
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

const MOOD_REPLY: Record<
  string,
  { reply: string; questions: string[]; actions: CompanionActionCode[]; suggestedTitle?: string }
> = {
  happy: {
    reply: 'Nghe như hôm nay có một khoảnh khắc đáng nhớ.',
    questions: ['Lúc đó điều đầu tiên bạn nghĩ tới là gì?', 'Bạn muốn giữ lại cảm giác này bằng cách nào?'],
    actions: [],
    suggestedTitle: 'Một điều nhỏ khiến mình mỉm cười hôm nay',
  },
  angry: {
    reply: 'Nghe như có chuyện làm bạn bức bối. Bạn muốn hạ nhiệt trước, hay xả hết ra trang viết?',
    questions: ['Chuyện gì chạm vào bạn nhiều nhất?'],
    actions: ['BREATHING_2MIN', 'VENT_PROMPT'],
  },
  sad: {
    reply: 'Nghe thật sự nặng lòng. Mình ở đây.',
    questions: ['Bạn có muốn kể thêm một chút về chuyện này không?'],
    actions: ['AMBIENT', 'BREATHING_2MIN'],
  },
  very_bad: {
    reply: 'Mình ở đây với bạn. Mình thở chậm cùng nhau một chút trước nhé?',
    questions: ['Nếu muốn, bạn có thể viết một câu về điều đang dồn lại.'],
    actions: ['BREATHING_2MIN', 'AMBIENT'],
  },
  neutral: {
    reply: 'Chào bạn. Hôm nay trong lòng bạn đang có gì?',
    questions: ['Có khoảnh khắc nhỏ nào hôm nay bạn vẫn còn nhớ?', 'Điều gì hôm nay còn hơi lấn cấn trong bạn?'],
    actions: [],
    suggestedTitle: 'Vài dòng nhìn lại nhịp sống hôm nay',
  },
};

const EXIT_CHIP = { label: 'Mình chỉ muốn viết tiếp', intent: 'EXIT' };
const LONG_TERM_CHIP = { label: 'Mình muốn ổn định hơn về lâu dài', intent: 'LONG_TERM' };

export async function mockCompanionTurn(request: CompanionTurnRequest): Promise<CompanionTurnResponse> {
  await delay(650);

  if (!consentGranted) {
    return empty('CONSENT_REQUIRED');
  }

  const probe = request.message.trim().toLowerCase();
  if (probe === 'test an toàn') return empty('SAFETY_FLOW');
  if (probe === 'test chặn') {
    return {
      status: 'BLOCKED',
      reply: 'Mình chưa thể trò chuyện tiếp về điều này ngay bây giờ. Bạn vẫn có thể viết tiếp vào trang nhật ký.',
      questions: [],
      suggestedTitle: null,
      immediateActions: [],
      chips: [],
      nextState: 'LISTENING',
      source: 'TEMPLATE',
      promptVersion: 'companion-mock-v1',
    };
  }
  if (probe === 'test lỗi') return empty('UNAVAILABLE');

  if (request.intent === 'TOOL_COMPLETED') {
    const mood = request.context.moodCode ?? 'neutral';
    return {
      status: 'REPLY',
      reply: mood === 'very_bad' ? 'Cứ thong thả nhé. Mình vẫn ở đây nếu bạn cần.' : 'Bạn thấy dịu hơn chút nào chưa?',
      questions: [],
      suggestedTitle: null,
      immediateActions: [],
      habitSuggestion:
        mood === 'very_bad' || habitDismissed
          ? null
          : {
              type: 'CREATE_FROM_TEMPLATE',
              habitId: null,
              localDate: null,
              targetValue: null,
              unit: null,
              templateCode: 'EVENING_BREATHING',
              category: 'MINDFULNESS',
              presetTitle: 'Thở sâu 2 phút mỗi tối',
              presetTargetValue: 2,
              presetUnit: 'minutes',
              presetToolCode: 'BREATHING_2MIN',
              text: 'Bạn có muốn thử duy trì thói quen “Thở sâu 2 phút mỗi tối” không?',
            },
      chips: [EXIT_CHIP],
      nextState: 'CLOSING',
      source: 'TEMPLATE',
      promptVersion: 'companion-mock-v1',
    };
  }
  if (request.intent === 'CLOSING') {
    const mood = request.context.moodCode ?? 'neutral';
    return {
      status: 'REPLY',
      reply: 'Cảm ơn bạn đã chia sẻ cùng mình hôm nay.',
      questions: [],
      suggestedTitle: null,
      immediateActions: [],
      habitSuggestion:
        mood === 'very_bad' || habitDismissed
          ? null
          : {
              type: 'CREATE_FROM_TEMPLATE',
              habitId: null,
              localDate: null,
              targetValue: null,
              unit: null,
              templateCode: 'EVENING_BREATHING',
              category: 'MINDFULNESS',
              presetTitle: 'Thở sâu 2 phút mỗi tối',
              presetTargetValue: 2,
              presetUnit: 'minutes',
              presetToolCode: 'BREATHING_2MIN',
              text: 'Bạn có muốn thử duy trì thói quen “Thở sâu 2 phút mỗi tối” không?',
            },
      chips: [],
      nextState: 'CLOSING',
      source: 'TEMPLATE',
      promptVersion: 'companion-mock-v1',
    };
  }

  const draftEmpty = request.draft.text.trim().length === 0;
  const moodKey = request.context.moodCode ?? 'neutral';
  const template = MOOD_REPLY[moodKey] ?? MOOD_REPLY.neutral;
  const isDistressed = moodKey === 'sad' || moodKey === 'very_bad' || moodKey === 'angry';

  const reply = request.intent === 'OPEN' && draftEmpty && !isDistressed
    ? 'Trang vẫn còn trống. Bạn muốn bắt đầu từ đâu?'
    : request.intent === 'FREE_TEXT'
      ? 'Mình nghe rồi. Cảm ơn bạn đã kể thêm cho mình.'
      : template.reply;
  const questions = request.intent === 'FREE_TEXT' ? ['Bạn muốn nói thêm điều gì nữa không?'] : template.questions;

  return {
    status: 'REPLY',
    reply,
    questions,
    suggestedTitle: request.intent === 'OPEN' && !draftEmpty ? (template.suggestedTitle ?? null) : null,
    immediateActions: request.intent === 'OPEN' ? template.actions.map((code) => ({ code, label: actionLabel(code) })) : [],
    chips: moodKey === 'very_bad' ? [EXIT_CHIP] : [EXIT_CHIP, LONG_TERM_CHIP],
    nextState: 'LISTENING',
    source: 'TEMPLATE',
    promptVersion: 'companion-mock-v1',
  };
}

function actionLabel(code: CompanionActionCode): string {
  return { BREATHING_2MIN: 'Thở chậm 2 phút', AMBIENT: 'Nghe âm thanh dịu', VENT_PROMPT: 'Viết xả ra trang', TITLE: 'Gợi ý tựa đề' }[code];
}

function empty(status: CompanionTurnResponse['status']): CompanionTurnResponse {
  return {
    status,
    reply: null,
    questions: [],
    suggestedTitle: null,
    immediateActions: [],
    chips: [],
    nextState: null,
    source: null,
    promptVersion: null,
  };
}

/** Dev-only mirror of backend `TitleSuggestionCatalog` (only 2 items per mood — enough for UI checks). */
export async function mockTitleSuggestions(mood: string | null): Promise<string[]> {
  await delay(150);
  const byMood: Record<string, string[]> = {
    happy: ['Một điều nhỏ khiến mình vui hôm nay', 'Khoảnh khắc hôm nay mình muốn giữ lại mãi'],
    angry: ['Tháo gỡ những bức bối trong lòng hôm nay', 'Viết ra hết để ngày mai nhẹ đầu hơn'],
    sad: ['Dịu dàng với bản thân trong một ngày trùng xuống', 'Điều đang làm lòng mình nặng trĩu hôm nay'],
    very_bad: ['Cho phép mình nghỉ ngơi khi mọi thứ hơi quá tải', 'Thở chậm lại qua một ngày nhiều mệt mỏi'],
    neutral: ['Vài dòng nhìn lại nhịp sống hôm nay', 'Một suy nghĩ chợt đến lúc cuối ngày'],
  };
  return byMood[mood ?? 'neutral'] ?? byMood.neutral;
}
