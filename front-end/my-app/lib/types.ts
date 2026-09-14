export type MoodType = 'calm-joy' | 'hope-energy' | 'anxiety-stress' | 'sadness-reflect' | 'neutral';

export type JournalStatus =
  | 'DRAFT'
  | 'SAVED'
  | 'ANALYZING'
  | 'ANALYZED'
  | 'ANALYSIS_FAILED'
  | 'ANALYSIS_OUTDATED';

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

export const EMOTION_TAXONOMY: Record<EmotionType, EmotionDefinition> = {
  JOY: { type: 'JOY', label: 'Hân hoan (Joy)', color: '#70E000', description: 'Cảm giác vui tươi, mãn nguyện' },
  CALM: { type: 'CALM', label: 'Bình an (Calm)', color: '#38B000', description: 'Tĩnh lặng, thư thái nội tâm' },
  HOPE: { type: 'HOPE', label: 'Hy vọng (Hope)', color: '#FFD166', description: 'Lạc quan, hướng về phía trước' },
  GRATITUDE: { type: 'GRATITUDE', label: 'Biết ơn (Gratitude)', color: '#06D6A0', description: 'Trân trọng những điều tốt đẹp' },
  EXCITEMENT: { type: 'EXCITEMENT', label: 'Hào hứng (Excitement)', color: '#FFB703', description: 'Tràn đầy sinh lực và hứng khởi' },
  ANXIETY: { type: 'ANXIETY', label: 'Lo âu (Anxiety)', color: '#FF6B6B', description: 'Bồn chồn, áp lực suy nghĩ' },
  SADNESS: { type: 'SADNESS', label: 'Buồn bã (Sadness)', color: '#4D96FF', description: 'Trầm tư, trống trải' },
  LONELINESS: { type: 'LONELINESS', label: 'Cô đơn (Loneliness)', color: '#9D4EDD', description: 'Cảm giác thiếu gắn kết' },
  FRUSTRATION: { type: 'FRUSTRATION', label: 'Bực bội (Frustration)', color: '#F72585', description: 'Ức chế, nghẽn trở ý muốn' },
  FEAR: { type: 'FEAR', label: 'Sợ hãi (Fear)', color: '#D90429', description: 'Bất an trước bất định' },
  ANGER: { type: 'ANGER', label: 'Giận dữ (Anger)', color: '#E63946', description: 'Căng thẳng phản ứng gay gắt' },
};

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
  status?: JournalStatus;
  updatedAt?: string;
  aiAnalysis?: AIAnalysisResult;
}

export interface UserProfile {
  name: string;
  penName: string;
  email: string;
  plan: string;
  timezone: string;
  avatarUrl?: string;
  language?: 'vi' | 'en';
  journalingGoals?: string[];
  preferredTime?: string;
  isOnboarded?: boolean;
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
