/** Mirrors backend/docs/COMPANION_CHATBOT_PLAN.md mục 5 (contract) and WritingSuggestionRequest/Response. */
export type CompanionIntent = 'OPEN' | 'FREE_TEXT' | 'TOOL_COMPLETED' | 'CLOSING';
export type CompanionStatus = 'REPLY' | 'CONSENT_REQUIRED' | 'SAFETY_FLOW' | 'BLOCKED' | 'UNAVAILABLE';
/** Same fixed catalogue as backend's `CompanionActionCode` enum (journal/domain). Never extended client-side. */
export type CompanionActionCode = 'BREATHING_2MIN' | 'AMBIENT' | 'VENT_PROMPT' | 'TITLE';

export interface CompanionDraft {
  title: string;
  text: string;
  wordCount: number;
  truncated: boolean;
}

export interface CompanionContext {
  moodCode: string | null;
  stressScore: number | null;
  energyScore: number | null;
  sleepMinutes: number | null;
  localHour: number;
}

export interface CompanionHistoryTurn {
  role: 'assistant' | 'user';
  text: string;
}

export interface CompanionTurnRequest {
  intent: CompanionIntent;
  message: string;
  draft: CompanionDraft;
  context: CompanionContext;
  completedTool?: CompanionActionCode;
  history: CompanionHistoryTurn[];
}

export interface CompanionActionOption {
  code: CompanionActionCode;
  label: string;
}

export interface CompanionChipOption {
  label: string;
  intent: string;
}

export interface CompanionHabitSuggestion {
  type: 'LOG_EXISTING' | 'CREATE_FROM_TEMPLATE';
  habitId: string | null;
  localDate: string | null;
  targetValue: number | null;
  unit: string | null;
  templateCode: string | null;
  category: string | null;
  presetTitle: string | null;
  presetTargetValue: number | null;
  presetUnit: string | null;
  presetToolCode: 'BREATHING_2MIN' | 'AMBIENT' | 'VENT_PROMPT' | null;
  text: string;
}

export interface CompanionTurnResponse {
  status: CompanionStatus;
  reply: string | null;
  questions: string[];
  suggestedTitle: string | null;
  immediateActions: CompanionActionOption[];
  habitSuggestion?: CompanionHabitSuggestion | null;
  chips: CompanionChipOption[];
  nextState: 'LISTENING' | 'CLOSING' | null;
  source: 'TEMPLATE' | 'GENERATED' | null;
  promptVersion: string | null;
}

/** One bubble in the chat panel. UI-only; never persisted (see AGENTS.md — ephemeral companion). */
export interface CompanionMessage {
  id: string;
  role: 'assistant' | 'user';
  text: string;
  questions?: string[];
  suggestedTitle?: string | null;
  actions?: CompanionActionOption[];
}

export type CompanionPanelState =
  | 'OPENING'
  | 'LISTENING'
  | 'WAITING'
  | 'CONSENT'
  | 'TOOLS_ONLY'
  | 'CLOSING';
