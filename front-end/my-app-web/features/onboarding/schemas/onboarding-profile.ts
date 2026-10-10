import { z } from 'zod';
import type { AppLocale } from '@/lib/i18n';

export function createOnboardingProfileSchema(locale: AppLocale) {
  const en = locale === 'en';
  return z.object({
    displayName: z.string().trim().min(1, en ? 'Enter your name.' : 'Vui lòng nhập họ tên.').max(120, en ? 'Name must be at most 120 characters.' : 'Họ tên tối đa 120 ký tự.'),
    penName: z.string().trim().min(1, en ? 'Enter a display name.' : 'Vui lòng nhập biệt danh.').max(120, en ? 'Display name must be at most 120 characters.' : 'Biệt danh tối đa 120 ký tự.'),
  });
}
