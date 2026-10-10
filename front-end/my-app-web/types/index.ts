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

export type EmotionType =
  | 'JOY'
  | 'CALM'
  | 'HOPE'
  | 'GRATITUDE'
  | 'EXCITEMENT'
  | 'ANXIETY'
  | 'SADNESS'
  | 'LONELINESS'
  | 'FRUSTRATION'
  | 'FEAR'
  | 'ANGER';

export interface EmotionDefinition {
  type: EmotionType;
  label: string;
  color: string;
  description: string;
}

export interface EmotionBreakdown {
  label: string;
  percentage: number;
  color: string;
  description: string;
  emotionType?: EmotionType;
}

export interface ExtractedEntity {
  name: string;
  category: 'người' | 'hoạt động' | 'công việc' | 'cảm xúc' | 'địa điểm';
  sentiment: 'positive' | 'neutral' | 'negative';
}

export interface AIAnalysisResult {
  sentiment: string;
  summary: string;
  reflectionPrompt: string;
  reflectionQuestions?: string[];
  emotions: EmotionBreakdown[];
  entities: ExtractedEntity[];
  mindfulAction: string;
  suggestedAction?: string;
  topics?: string[];
  riskLevel?: 'NORMAL' | 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';
  userCorrected?: boolean;
  correctedAt?: string;
  status?: JournalStatus;
}

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
  aiAnalysis?: AIAnalysisResult;
}

export interface UserProfile {
  name: string;
  penName: string;
  email: string;
  plan: string;
  timezone: string;
  avatarUrl: string;
  bio?: string;
  language?: 'vi' | 'en';
  isOnboarded?: boolean;
  journalingGoals?: string[];
  preferredTime?: string;
}

export interface WellnessGoal {
  id: string;
  title: string;
  description?: string;
  category?: 'sleep' | 'mindfulness' | 'exercise' | 'social';
  targetDays: number;
  completedDays: number;
  unit?: string;
  completed: boolean;
  currentStreak?: number;
  targetStreak?: number;
  completedToday?: boolean;
  icon?: string;
}

export interface DailyStat {
  date: string;
  dayName: string;
  score?: number;
  moodScore: number;
  stressScore: number;
  sleepHours?: number;
  mood?: MoodType;
  dominantEmotion?: string;
}


