export type MoodType = 'calm-joy' | 'hope-energy' | 'anxiety-stress' | 'sadness-reflect' | 'neutral';
export type JournalEmotion = 'neutral' | 'happy' | 'angry' | 'sad' | 'very_bad';

export type JournalStatus =
  | 'DRAFT'
  | 'SAVED'
  | 'PENDING'
  | 'ANALYZING'
  | 'ANALYZED'
  | 'ANALYSIS_FAILED'
  | 'ANALYSIS_OUTDATED'
  | 'BLOCKED_BY_SAFETY';

export interface JournalEntry {
  id: string;
  contentVersion?: number;
  rowVersion?: number;
  contentJson?: Record<string, unknown>;
  title: string;
  content: string;
  date: string; // YYYY-MM-DD
  time: string; // HH:mm
  mood: MoodType;
  emotion?: JournalEmotion;
  moodScore: number | null; // 1-10 when loaded from detail
  stressScore: number | null; // 1-10 when loaded from detail
  energyScore: number | null; // 1-10 when loaded from detail
  sleepHours: number | null;
  tags: string[];
  photoUrl?: string;
  photoCaption?: string;
  location?: string;
  isFavorite?: boolean;
  status?: JournalStatus;
  riskLevel?: string;
  updatedAt?: string;
}

export interface UserProfile {
  name: string;
  penName: string;
  email: string;
  plan: string;
  timezone: string;
  avatarUrl: string;
  /** Insight-page mascot id or "off". */
  mascot?: string;
  language?: 'vi' | 'en';
  isOnboarded?: boolean;
  journalingGoals?: string[];
  preferredTime?: string;
  isAdmin?: boolean;
  canChangePassword?: boolean;
}

