'use client';

import { useState } from 'react';
import { useAccount } from '../context/AccountContext';
import { useToast } from '@/providers/ToastProvider';
import { createProfileSchema } from '../schemas/profile-form';
import { useTranslation } from 'react-i18next';
import { useAppLanguage } from '@/providers/AppLanguageProvider';

export function useSettings({ entryCount, draftStorageKey }: { entryCount: number | null; draftStorageKey: string }) {
  const { userProfile, updateProfile } = useAccount();
  const { showToast } = useToast();
  const { t } = useTranslation();
  const { locale } = useAppLanguage();

  // Personal info state
  const [name, setName] = useState(userProfile.name || '');
  const [penName, setPenName] = useState(userProfile.penName || '');
  const [timezone, setTimezone] = useState(userProfile.timezone || 'Asia/Ho_Chi_Minh');
  const [language, setLanguage] = useState<'vi' | 'en'>(userProfile.language || 'vi');
  const [fieldErrors, setFieldErrors] = useState<{ name?: string; penName?: string }>({});

  // Save personal profile & localization
  const handleSaveProfile = async () => {
    if (saving) return;
    const parsed = createProfileSchema(locale).safeParse({ name, penName, timezone, language });
    if (!parsed.success) {
      const errors = parsed.error.flatten().fieldErrors;
      setFieldErrors({ name: errors.name?.[0], penName: errors.penName?.[0] });
      showToast({ title: t('settings.invalid'), type: 'error' });
      return;
    }
    setFieldErrors({});

    setSaving(true);
    try {
      await updateProfile({ name: parsed.data.name || parsed.data.penName, penName: parsed.data.penName, timezone: parsed.data.timezone, language: parsed.data.language });
      showToast({ title: t('settings.saved'), message: t('settings.savedDetail'), type: 'success' });
    } catch (error) {
      showToast({ title: t('settings.saveError'), message: error instanceof Error ? error.message : t('auth.tryAgain'), type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const hasDraft = typeof window !== 'undefined' && !!localStorage.getItem(draftStorageKey);
  // Unsaved edits in the profile tab (the mascot saves on its own and is not part of this form).
  const dirty = name !== (userProfile.name || '') || penName !== (userProfile.penName || '')
    || timezone !== (userProfile.timezone || 'Asia/Ho_Chi_Minh') || language !== (userProfile.language || 'vi');
  const [saving, setSaving] = useState(false);

  return {
    userProfile,
    entryCount,
    name,
    setName,
    penName,
    setPenName,
    timezone,
    setTimezone,
    language,
    setLanguage,
    fieldErrors,
    hasDraft,
    dirty,
    saving,
    handleSaveProfile,
  };
}

