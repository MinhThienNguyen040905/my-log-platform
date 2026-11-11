import { apiRequest } from '@/lib/api/client';

/** Nguồn hỗ trợ an toàn đã được backend duyệt và xác minh (`GET /api/v1/safety/resources`, công khai). */
export type ApprovedSafetyResource = {
  id: string;
  name: string;
  resourceType: string;
  contactValue: string | null;
  description: string | null;
  sourceUrl: string;
  version: number;
};

/** Backend quyết định mức rủi ro; client chỉ đọc giá trị này, không tự phân loại nội dung. */
export function needsSafetySupport(riskLevel: string | null | undefined) {
  return riskLevel === 'HIGH' || riskLevel === 'CRITICAL';
}

export async function loadApprovedSafetyResources(): Promise<ApprovedSafetyResource[]> {
  const response = await apiRequest('/safety/resources?locale=vi-VN&country=VN', {}, false);
  const items: unknown = await response.json();
  if (!Array.isArray(items)) return [];
  return items.filter((item): item is ApprovedSafetyResource => {
    if (typeof item !== 'object' || item === null) return false;
    const resource = item as Record<string, unknown>;
    return typeof resource.id === 'string' && typeof resource.name === 'string'
      && typeof resource.resourceType === 'string'
      && (resource.contactValue === null || typeof resource.contactValue === 'string')
      && (resource.description === null || typeof resource.description === 'string')
      && typeof resource.sourceUrl === 'string' && resource.sourceUrl.startsWith('https://')
      && typeof resource.version === 'number';
  });
}

/** Chỉ tạo liên kết gọi điện từ số do backend trả về; không có số nào được viết sẵn trong app. */
export function phoneLink(contactValue: string | null): string | null {
  if (!contactValue) return null;
  const compact = contactValue.replace(/[\s().-]/g, '');
  return /^\+?[0-9]{3,15}$/.test(compact) ? `tel:${compact}` : null;
}
