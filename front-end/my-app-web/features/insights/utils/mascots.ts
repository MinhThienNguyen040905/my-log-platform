/**
 * Insight-page mascot: which companion the user picked (stored in the profile) and which expression to show.
 * The art and its CSS animation live in components/MascotArt.tsx and MoodMascot.module.css.
 */
export const MASCOT_IDS = ['sprout', 'cat', 'coffee'] as const;
export type MascotId = (typeof MASCOT_IDS)[number];
/** Profile value: one of the mascots, or "off" to hide it on the Insight page. */
export type MascotChoice = MascotId | 'off';
export const DEFAULT_MASCOT: MascotId = 'sprout';

/** The five moods follow the mood icons users pick when writing; "idle" means no mood in the window. */
export const MASCOT_MOODS = ['happy', 'neutral', 'angry', 'sad', 'very-bad'] as const;
export type MascotMood = (typeof MASCOT_MOODS)[number] | 'idle';

/** Drawn expressions; each has its own animation cycle shared by all three mascots. */
export type MascotExpression = 'happy' | 'sad' | 'angry' | 'tired' | 'normal';

export const MASCOTS: Record<MascotId, { id: MascotId; emoji: string }> = {
  sprout: { id: 'sprout', emoji: '🌱' },
  cat: { id: 'cat', emoji: '🐱' },
  coffee: { id: 'coffee', emoji: '☕' },
};

const MOOD_EXPRESSION: Record<MascotMood, MascotExpression> = {
  happy: 'happy', neutral: 'normal', angry: 'angry', sad: 'sad', 'very-bad': 'tired', idle: 'normal',
};

/** In a calm (high-risk) week the mascot never shows the harsh angry face. */
export function toMascotExpression(mood: MascotMood, calm: boolean): MascotExpression {
  const expression = MOOD_EXPRESSION[mood];
  return calm && expression === 'angry' ? 'normal' : expression;
}

export function isMascotId(value: unknown): value is MascotId {
  return typeof value === 'string' && (MASCOT_IDS as readonly string[]).includes(value);
}

/** Unknown values (including retired mascots) fall back to the default. */
export function toMascotChoice(value: unknown): MascotChoice {
  return value === 'off' ? 'off' : isMascotId(value) ? value : DEFAULT_MASCOT;
}

/** Maps the API mood code (or null when nothing was picked in the window) to an expression. */
export function toMascotMood(code: string | null | undefined): MascotMood {
  return code && (MASCOT_MOODS as readonly string[]).includes(code) ? (code as MascotMood) : 'idle';
}
