import type { CheckinActivity } from '../api/checkin';

/**
 * Activity codes the web check-in offers. Codes must match `[a-z0-9-]{1,40}` (backend `CheckinService`)
 * and are the ones the backend's pattern insights label in Vietnamese (`InsightTemplates.activity`).
 * Mobile should offer the same list. Labels live in `lib/i18n.ts` under `checkin.activityLabels`.
 */
export const CHECKIN_ACTIVITY_CODES = [
  'walking', 'running', 'exercise', 'stretching', 'meditation', 'reading', 'studying', 'journaling',
  'cooking', 'housework', 'family-time', 'social', 'eating-out', 'nap', 'meeting', 'work-overtime',
] as const;

/** Backend limit per check-in. */
export const CHECKIN_ACTIVITY_MAX = 20;

/**
 * Builds the activities to send for the selected codes, keeping the duration/intensity already stored for
 * codes that stay selected. Always sent explicitly so saving from the web never drops another client's data
 * by accident: what is not selected here is removed on purpose.
 */
export function activitiesForSave(selected: readonly string[], existing: readonly CheckinActivity[]): CheckinActivity[] {
  const byCode = new Map(existing.map((activity) => [activity.code, activity]));
  return Array.from(new Set(selected)).slice(0, CHECKIN_ACTIVITY_MAX)
    .map((code) => byCode.get(code) ?? { code, durationMinutes: null, intensity: null });
}

/** Selectable chips: the catalogue plus any code already saved (e.g. from another client) so it can be kept. */
export function activityChoices(existing: readonly CheckinActivity[]): string[] {
  const extra = existing.map((activity) => activity.code)
    .filter((code) => !(CHECKIN_ACTIVITY_CODES as readonly string[]).includes(code));
  return [...CHECKIN_ACTIVITY_CODES, ...extra];
}
