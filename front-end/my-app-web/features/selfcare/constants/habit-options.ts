import type { HabitToolCode } from '../api/self-care';

export const TOOL_LABELS: Record<HabitToolCode, string> = {
  BREATHING_2MIN: 'Thở chậm 2 phút',
  AMBIENT: 'Âm thanh dịu',
  VENT_PROMPT: 'Viết xả ra trang',
};

export const UNIT_LABELS: Record<string, string> = {
  minutes: 'phút',
  sessions: 'lần',
  steps: 'bước',
};

export const WEEKDAY_OPTIONS = [
  { value: 1, label: 'T2' },
  { value: 2, label: 'T3' },
  { value: 3, label: 'T4' },
  { value: 4, label: 'T5' },
  { value: 5, label: 'T6' },
  { value: 6, label: 'T7' },
  { value: 7, label: 'CN' },
] as const;

export const GOAL_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Đang thực hiện',
  PAUSED: 'Tạm dừng',
  COMPLETED: 'Đã đạt',
  ARCHIVED: 'Lưu trữ',
};

export const HABIT_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Đang theo dõi',
  PAUSED: 'Tạm dừng',
  ARCHIVED: 'Lưu trữ',
};
