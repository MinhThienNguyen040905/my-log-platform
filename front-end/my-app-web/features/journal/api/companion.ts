import { apiJson, apiJsonBody, apiQuery, apiResponse } from '@/lib/api/client';
import { getConsents, setConsent } from '@/features/user';
import type { CompanionTurnRequest, CompanionTurnResponse } from '../types/companion.types';

/**
 * Single entry point the hook calls — real backend by default, dev-only mock when explicitly
 * opted into. `NODE_ENV`/`NEXT_PUBLIC_COMPANION_MOCK` are inlined at build time, so this whole
 * branch (dynamic import included) is dead-code-eliminated from production bundles; see
 * companion.mock.ts. `journal-writing-suggestions` is allowlisted in secured-proxy.ts (Bước 3).
 */
export async function postCompanionTurn(
  request: CompanionTurnRequest,
  signal?: AbortSignal,
): Promise<CompanionTurnResponse> {
  if (process.env.NODE_ENV !== 'production' && process.env.NEXT_PUBLIC_COMPANION_MOCK === '1') {
    const { mockCompanionTurn } = await import('./companion.mock');
    return mockCompanionTurn(request);
  }
  return apiJson<CompanionTurnResponse>('journal-writing-suggestions', {
    method: 'POST',
    ...apiJsonBody(request),
    signal,
  });
}

/** Grants `AI_PROCESSING` consent for the companion flow, using the document version already on record. */
export async function grantCompanionConsent(): Promise<void> {
  if (process.env.NODE_ENV !== 'production' && process.env.NEXT_PUBLIC_COMPANION_MOCK === '1') {
    const { grantMockConsent } = await import('./companion.mock');
    grantMockConsent();
    return;
  }
  const consents = await getConsents();
  const current = consents.find((consent) => consent.type === 'AI_PROCESSING');
  if (!current) throw new Error('Không tìm thấy điều khoản AI_PROCESSING để đồng ý.');
  await setConsent('AI_PROCESSING', current.documentVersion, true);
}

/**
 * Dismisses companion habit suggestions for 7 days via dedicated endpoint
 * `POST /api/v1/companion/habit-suggestions/dismiss` (does not consume companion-turn quota).
 */
export async function dismissCompanionHabitSuggestion(): Promise<void> {
  if (process.env.NODE_ENV !== 'production' && process.env.NEXT_PUBLIC_COMPANION_MOCK === '1') {
    const { dismissMockHabitSuggestion } = await import('./companion.mock');
    dismissMockHabitSuggestion();
    return;
  }
  await apiResponse('companion/habit-suggestions/dismiss', { method: 'POST' });
}

/**
 * Tựa đề soạn sẵn theo cảm xúc (không phải AI), luôn dùng được ở mọi phiên — không cần consent,
 * không tốn lượt trò chuyện. Nội dung do backend `TitleSuggestionCatalog` quản lý.
 */
export async function getCompanionTitleSuggestions(mood: string | null): Promise<string[]> {
  if (process.env.NODE_ENV !== 'production' && process.env.NEXT_PUBLIC_COMPANION_MOCK === '1') {
    const { mockTitleSuggestions } = await import('./companion.mock');
    return mockTitleSuggestions(mood);
  }
  const query = apiQuery({ mood });
  const response = await apiJson<{ titles: string[] }>(`companion/title-suggestions${query}`);
  return response.titles;
}
