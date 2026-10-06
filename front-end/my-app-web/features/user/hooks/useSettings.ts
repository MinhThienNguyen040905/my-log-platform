'use client';

import { useState, useEffect } from 'react';
import { useJournal } from '@/features/journal';
import { useToast } from '@/lib/toast-context';

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
  const { userProfile, updateProfile, entries } = useJournal();
  const { showToast } = useToast();

  // Personal info state
  const [name, setName] = useState(userProfile.name || '');
  const [penName, setPenName] = useState(userProfile.penName || '');
  const [bio, setBio] = useState(userProfile.bio || 'Mỗi ngày là một trang sách mới.');
  const [avatarUrl, setAvatarUrl] = useState(userProfile.avatarUrl || '/avatar.png');
  const [selectedAvatarId, setSelectedAvatarId] = useState<string>('sprout');
  const [timezone, setTimezone] = useState(userProfile.timezone || 'Asia/Ho_Chi_Minh');
  const [language, setLanguage] = useState<'vi' | 'en'>(userProfile.language || 'vi');

  // Password change state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);

  // Sync with user profile on mount
  useEffect(() => {
    setName(userProfile.name || '');
    setPenName(userProfile.penName || '');
    setBio(userProfile.bio || 'Mỗi ngày là một trang sách mới.');
    setAvatarUrl(userProfile.avatarUrl || '/avatar.png');
    setTimezone(userProfile.timezone || 'Asia/Ho_Chi_Minh');
    setLanguage(userProfile.language || 'vi');
  }, [userProfile]);

  // Password strength calculation
  const getPasswordStrength = (pwd: string) => {
    if (!pwd) return { label: 'Chưa nhập', width: 'w-0', color: 'bg-gray-200' };
    if (pwd.length < 6) return { label: 'Yếu', width: 'w-1/3', color: 'bg-red-500' };
    if (pwd.length < 10) return { label: 'Vừa phải', width: 'w-2/3', color: 'bg-amber-300' };
    return { label: 'Rất mạnh', width: 'w-full', color: 'bg-primary-container' };
  };

  const passwordStrength = getPasswordStrength(newPassword);

  // Save personal profile & localization
  const handleSaveProfile = () => {
    if (!penName.trim()) {
      showToast({
        title: 'Bút danh không được để trống!',
        message: 'Vui lòng nhập bút danh hiển thị cho cuốn sổ tay của bạn.',
        type: 'error',
      });
      return;
    }

    updateProfile({
      name: name.trim() || penName.trim(),
      penName: penName.trim(),
      bio: bio.trim(),
      avatarUrl,
      timezone,
      language,
    });

    showToast({
      title: 'Đã cập nhật hồ sơ cá nhân!',
      message: 'Thông tin tác giả và thiết lập đã được lưu an toàn.',
      type: 'success',
    });
  };

  // Change password handler
  const handleChangePassword = (e: React.FormEvent) => {
    e.preventDefault();

    if (!currentPassword) {
      showToast({
        title: 'Chưa nhập mật khẩu hiện tại!',
        message: 'Vui lòng nhập mật khẩu hiện tại để xác thực tài khoản.',
        type: 'error',
      });
      return;
    }

    if (newPassword.length < 6) {
      showToast({
        title: 'Mật khẩu mới quá ngắn!',
        message: 'Mật khẩu mới phải có tối thiểu 6 ký tự.',
        type: 'error',
      });
      return;
    }

    if (newPassword !== confirmPassword) {
      showToast({
        title: 'Mật khẩu xác nhận không khớp!',
        message: 'Vui lòng kiểm tra lại mật khẩu xác nhận của bạn.',
        type: 'error',
      });
      return;
    }

    setPasswordLoading(true);

    setTimeout(() => {
      setPasswordLoading(false);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');

      showToast({
        title: 'Đổi mật khẩu thành công!',
        message: 'Chìa khóa sổ tay của bạn đã được cập nhật an toàn.',
        type: 'success',
      });
    }, 500);
  };

  const hasDraft = typeof window !== 'undefined' && !!localStorage.getItem('mylog_draft_journal');

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
    currentPassword,
    setCurrentPassword,
    newPassword,
    setNewPassword,
    confirmPassword,
    setConfirmPassword,
    showCurrentPassword,
    setShowCurrentPassword,
    showNewPassword,
    setShowNewPassword,
    showConfirmPassword,
    setShowConfirmPassword,
    passwordLoading,
    passwordStrength,
    hasDraft,
    handleSaveProfile,
    handleChangePassword,
  };
}

