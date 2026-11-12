import { JOURNAL_EMOTIONS } from '@/features/journal';
import type { Checkin } from '../api/checkin';
import { emotionFromMoodCode } from './checkin-values';

type Translate = (key: string, options?: Record<string, unknown>) => string;

/** Short labels for a saved check-in: mood, stress, energy, sleep, then activities. */
export function summarizeCheckin(checkin: Checkin, t: Translate, withActivities = true): string[] {
  const emotion = emotionFromMoodCode(checkin.moodCode);
  return [
    emotion ? JOURNAL_EMOTIONS[emotion].label : null,
    checkin.stressScore !== null ? `${t('checkin.stress')} ${checkin.stressScore}/10` : null,
    checkin.energyScore !== null ? `${t('checkin.energy')} ${checkin.energyScore}/10` : null,
    checkin.sleepMinutes !== null ? `${t('checkin.sleep')} ${Math.round((checkin.sleepMinutes / 60) * 10) / 10}h` : null,
    ...(withActivities ? checkin.activities.map((activity) => t(`checkin.activityLabels.${activity.code}`, { defaultValue: activity.code.replace(/-/g, ' ') })) : []),
  ].filter((value): value is string => value !== null);
}
