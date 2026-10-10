'use client';

import { useState } from 'react';
import { useJournal } from '@/features/journal';
import { useToast } from '@/lib/toast-context';
import { createProfileSchema } from '../schemas/profile-form';
import { useTranslation } from 'react-i18next';
import { useAppLanguage } from '@/app/_components/AppLanguageProvider';

export interface AvatarOption {
  id: string;
  label: string;
  iconText: string;
  url: string;
  color: string;
}

export const AVATAR_OPTIONS: AvatarOption[] = [
  { id: 'sprout', label: 'Mầm Xanh', iconText: '🌱', url: '/avatar.png', color: 'bg-primary-container text-black' },
  { id: 'cat', label: 'Mèo Cam', iconText: '🐱', url: '/avatar.png', color: 'bg-paper-warm text-black' },
  { id: 'coffee', label: 'Ly Cà Phê', iconText: '☕', url: '/avatar.png', color: 'bg-amber-200 text-black' },
  { id: 'palette', label: 'Bút Vẽ', iconText: '🎨', url: '/avatar.png', color: 'bg-purple-200 text-black' },
  { id: 'book', label: 'Sách Cũ', iconText: '📖', url: '/avatar.png', color: 'bg-rose-200 text-black' },
];

export function useSettings() {
  const { userProfile, updateProfile, entries, draftStorageKey } = useJournal();
  const { showToast } = useToast();
  const { t } = useTranslation();
  const { locale } = useAppLanguage();

  // Personal info state
  const [name, setName] = useState(userProfile.name || '');
  const [penName, setPenName] = useState(userProfile.penName || '');
  const [bio, setBio] = useState(userProfile.bio || 'Mỗi ngày là một trang sách mới.');
  const [avatarUrl, setAvatarUrl] = useState(userProfile.avatarUrl || '/avatar.png');
  const [selectedAvatarId, setSelectedAvatarId] = useState<string>('sprout');
  const [timezone, setTimezone] = useState(userProfile.timezone || 'Asia/Ho_Chi_Minh');
  const [language, setLanguage] = useState<'vi' | 'en'>(userProfile.language || 'vi');
  const [fieldErrors, setFieldErrors] = useState<{ name?: string; penName?: string }>({});

  // Save personal profile & localization
  const handleSaveProfile = async () => {
    const parsed = createProfileSchema(locale).safeParse({ name, penName, timezone, language });
    if (!parsed.success) {
      const errors = parsed.error.flatten().fieldErrors;
      setFieldErrors({ name: errors.name?.[0], penName: errors.penName?.[0] });
      showToast({ title: t('settings.invalid'), type: 'error' });
      return;
    }
    setFieldErrors({});

    try {
      await updateProfile({ name: parsed.data.name || parsed.data.penName, penName: parsed.data.penName, timezone: parsed.data.timezone, language: parsed.data.language });
      showToast({ title: t('settings.saved'), message: t('settings.savedDetail'), type: 'success' });
    } catch (error) {
      showToast({ title: t('settings.saveError'), message: error instanceof Error ? error.message : t('auth.tryAgain'), type: 'error' });
    }
  };

  const hasDraft = typeof window !== 'undefined' && !!localStorage.getItem(draftStorageKey);

  return {
    userProfile,
    entries,
    name,
    setName,
    penName,
    setPenName,
    bio,
    setBio,
    avatarUrl,
    setAvatarUrl,
    selectedAvatarId,
    setSelectedAvatarId,
    timezone,
    setTimezone,
    language,
    setLanguage,
    fieldErrors,
    hasDraft,
    handleSaveProfile,
  };
}

