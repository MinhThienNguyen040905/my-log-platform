'use client';

import { useEffect, useState } from 'react';
import { useJournal } from '@/features/journal';
import { AccountApiError, getConsents, setConsent, type Consent } from '@/features/user';
import { createOnboardingProfileSchema } from '../schemas/onboarding-profile';
import { useAppLanguage } from '@/app/_components/AppLanguageProvider';

export const GOALS = [
  { id: 'MINDFULNESS', label: 'Quan sát cảm xúc' },
  { id: 'REFLECTION', label: 'Nhìn lại bản thân' },
  { id: 'SLEEP', label: 'Chăm sóc giấc ngủ' },
  { id: 'EXERCISE', label: 'Duy trì vận động' },
  { id: 'SOCIAL', label: 'Kết nối với mọi người' },
] as const;

export const OPTIONAL_CONSENTS = [
  { type: 'AI_PROCESSING', label: 'Xử lý dữ liệu bằng AI' },
  { type: 'ANALYTICS', label: 'Phân tích sử dụng' },
  { type: 'MODEL_TRAINING', label: 'Dùng dữ liệu để huấn luyện mô hình' },
] as const;

export function useOnboarding() {
  const { locale } = useAppLanguage();
  const { accountProfile, reloadProfile, updateProfile, completeOnboarding } = useJournal();
  const [displayName, setDisplayName] = useState(accountProfile.displayName ?? '');
  const [penName, setPenName] = useState(accountProfile.penName ?? '');
  const [fieldErrors, setFieldErrors] = useState<{ displayName?: string; penName?: string }>({});
  const [goals, setGoals] = useState<string[]>(accountProfile.onboardingGoals);
  const [avatar, setAvatar] = useState('sprout');
  const [writingTime, setWritingTime] = useState('evening');
  const [consents, setConsents] = useState<Consent[]>([]);
  const [revocations, setRevocations] = useState<string[]>([]);
  const [loadingConsents, setLoadingConsents] = useState(true);
  const [consentError, setConsentError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const loadConsents = async () => {
    setLoadingConsents(true);
    try {
      const latest = await getConsents();
      setConsents(latest);
      setRevocations((previous) => previous.filter((type) => latest.some((item) => item.type === type && item.granted)));
      setConsentError(null);
      return true;
    } catch (error) {
      setConsentError(error instanceof Error ? error.message : 'Không thể tải quyền đồng ý.');
      return false;
    } finally {
      setLoadingConsents(false);
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => void loadConsents(), 0);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      try {
        const stored = JSON.parse(localStorage.getItem(`mylog_${accountProfile.userId}_onboarding_preferences`) ?? '{}');
        if (['sprout', 'coffee', 'cat', 'palette'].includes(stored.avatar)) setAvatar(stored.avatar);
        if (['evening', 'morning', 'anytime'].includes(stored.writingTime)) setWritingTime(stored.writingTime);
      } catch { /* Browser storage is optional. */ }
    }, 0);
    return () => clearTimeout(timer);
  }, [accountProfile.userId]);

  const toggleGoal = (id: string) => {
    setGoals((previous) => previous.includes(id) ? previous.filter((goal) => goal !== id) : [...previous, id]);
  };

  const toggleRevocation = (type: string) => {
    setRevocations((previous) => previous.includes(type) ? previous.filter((item) => item !== type) : [...previous, type]);
  };

  const submit = async (skip = false) => {
    if (!skip && (loadingConsents || consentError || submitting)) throw new Error('Vui lòng tải lại trạng thái quyền đồng ý trước khi tiếp tục.');
    const parsed = createOnboardingProfileSchema(locale).safeParse({ displayName, penName });
    if (!skip && !parsed.success) {
      const errors = parsed.error.flatten().fieldErrors;
      setFieldErrors({ displayName: errors.displayName?.[0], penName: errors.penName?.[0] });
      throw new Error(locale === 'en' ? 'Please correct the highlighted profile details.' : 'Vui lòng sửa thông tin hồ sơ được đánh dấu.');
    }
    setFieldErrors({});
    setSubmitting(true);
    try {
      if (!skip) {
        const changed = displayName.trim() !== (accountProfile.displayName ?? '')
          || penName.trim() !== (accountProfile.penName ?? '')
          || goals.join('|') !== accountProfile.onboardingGoals.join('|');
        if (changed) await updateProfile({ name: displayName.trim(), penName: penName.trim(), journalingGoals: goals });
        for (const type of revocations) {
          const current = consents.find((item) => item.type === type);
          if (!current?.granted) continue;
          await setConsent(type, current.documentVersion, false);
          setConsents((previous) => previous.map((item) => item.type === type ? { ...item, granted: false } : item));
        }
        if (revocations.length && !await loadConsents()) throw new Error('Không thể xác nhận quyền đồng ý đã lưu. Vui lòng thử lại.');
      }
      const completed = await completeOnboarding();
      if (!completed.onboardingCompletedAt) throw new Error('Máy chủ chưa xác nhận hoàn tất hướng dẫn.');
      if (!skip) {
        try {
          localStorage.setItem(`mylog_${accountProfile.userId}_onboarding_preferences`, JSON.stringify({ avatar, writingTime }));
        } catch { /* Account data was saved on the server; local preferences are optional. */ }
      }
      return completed;
    } catch (error) {
      if (error instanceof AccountApiError && error.status === 409) await reloadProfile();
      throw error;
    } finally {
      setSubmitting(false);
    }
  };

  return {
    displayName, setDisplayName, penName, setPenName, fieldErrors, goals, toggleGoal,
    avatar, setAvatar, writingTime, setWritingTime,
    consents, revocations, toggleRevocation, loadingConsents, consentError, loadConsents,
    submitting, submit,
  };
}
