import type { CompanionDraft } from '../types/companion.types';

const MAX_DRAFT_CHARS = 4000;
const MAX_TITLE_CHARS = 200;

/** Builds the draft snapshot sent with each companion turn. Keeps the END of a long draft, per contract. */
export function buildCompanionDraft(title: string, plainText: string): CompanionDraft {
  const trimmed = plainText.trim();
  const truncated = trimmed.length > MAX_DRAFT_CHARS;
  const text = truncated ? trimmed.slice(trimmed.length - MAX_DRAFT_CHARS) : trimmed;
  const wordCount = trimmed ? trimmed.split(/\s+/).filter(Boolean).length : 0;
  return { title: title.slice(0, MAX_TITLE_CHARS), text, wordCount, truncated };
}
