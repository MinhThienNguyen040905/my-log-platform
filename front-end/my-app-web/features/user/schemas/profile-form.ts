import { z } from 'zod';
import type { AppLocale } from '@/lib/i18n';

export function createProfileSchema(locale: AppLocale) {
  const en = locale === 'en';
  return z.object({
    name: z.string().trim().max(120, en ? 'Name must be at most 120 characters.' : 'Họ tên tối đa 120 ký tự.'),
    penName: z.string().trim().min(1, en ? 'Enter a display name.' : 'Vui lòng nhập bút danh.').max(120, en ? 'Display name must be at most 120 characters.' : 'Bút danh tối đa 120 ký tự.'),
    timezone: z.string().min(1, en ? 'Choose a timezone.' : 'Vui lòng chọn múi giờ.').max(64, en ? 'Invalid timezone.' : 'Múi giờ không hợp lệ.'),
    language: z.enum(['vi', 'en']),
  });
}
