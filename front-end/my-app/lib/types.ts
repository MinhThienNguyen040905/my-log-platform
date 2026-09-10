export type MoodType = 'calm-joy' | 'hope-energy' | 'anxiety-stress' | 'sadness-reflect' | 'neutral';

export interface EmotionBreakdown {
  label: string;
  percentage: number;
  color: string;
  description: string;
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
  emotions: EmotionBreakdown[];
  entities: ExtractedEntity[];
  mindfulAction: string;
}

export interface JournalEntry {
  id: string;
  title: string;
  content: string;
  date: string; // YYYY-MM-DD
  time: string; // HH:mm
  mood: MoodType;
  moodScore: number; // 1-10
  stressScore: number; // 1-10
  energyScore: number; // 1-10
  sleepHours: number;
  tags: string[];
  photoUrl?: string;
  photoCaption?: string;
  location?: string;
  isFavorite?: boolean;
  aiAnalysis?: AIAnalysisResult;
}

export interface WellnessGoal {
  id: string;
  title: string;
  description: string;
  category: 'sleep' | 'mindfulness' | 'exercise' | 'social';
  targetDays: number;
  completedDays: number;
  unit: string;
  completed: boolean;
}

export interface DailyStat {
  date: string;
  dayName: string;
  moodScore: number;
  stressScore: number;
  sleepHours: number;
}
