import { ApiError, apiJson, apiJsonBody, apiQuery } from '@/lib/api/client';

/** Check-in hằng ngày (backend `CheckinController`): một bản ghi mỗi ngày, PUT là upsert. */
export type CheckinActivity = { code: string; durationMinutes: number | null; intensity: 'LOW' | 'MODERATE' | 'HIGH' | null };
export type Checkin = {
  id: string;
  localDate: string;
  timezone: string;
  moodCode: string | null;
  moodScore: number | null;
  stressScore: number | null;
  energyScore: number | null;
  sleepMinutes: number | null;
  note: string | null;
  activities: CheckinActivity[];
  version: number;
  createdAt: string;
  updatedAt: string;
};
export type CheckinInput = {
  timezone: string;
  moodCode?: string | null;
  moodScore?: number | null;
  stressScore?: number | null;
  energyScore?: number | null;
  sleepMinutes?: number | null;
  note?: string | null;
  /** Omit to keep the day's existing activities; send `[]` to clear them. */
  activities?: CheckinActivity[];
};

/** `null` khi ngày đó chưa check-in (backend trả 404) — đây là trạng thái bình thường, không phải lỗi. */
export async function getCheckin(localDate: string): Promise<Checkin | null> {
  try {
    return await apiJson<Checkin>(`check-ins/${encodeURIComponent(localDate)}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

export function putCheckin(localDate: string, input: CheckinInput): Promise<Checkin> {
  return apiJson(`check-ins/${encodeURIComponent(localDate)}`, { method: 'PUT', ...apiJsonBody(input) });
}

/** Tối đa 366 ngày mỗi lần gọi. */
export function listCheckins(from: string, to: string): Promise<Checkin[]> {
  return apiJson(`check-ins${apiQuery({ from, to })}`);
}
