'use client';

import React, { useState } from 'react';
import { Eye, EyeOff, CheckCircle2, AlertCircle, ShieldCheck } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { changePassword } from '../api/client';
import { checkPasswordContainsUserInfo, resolvePasswordErrorMessage } from '../utils/password-rules';
import { NeoButton } from '@/components/ui/NeoButton';

interface ChangePasswordFormProps {
  canChangePassword?: boolean;
  userEmail?: string;
}

export function ChangePasswordForm({ canChangePassword = true, userEmail = '' }: ChangePasswordFormProps) {
  const { t } = useTranslation();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // If user signed in with Google only (no local password)
  if (!canChangePassword) {
    return (
      <div className="bg-amber-50/60 border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm font-space text-sm">
        <div className="flex items-start gap-3">
          <div className="w-9 h-9 rounded-xl bg-amber-200 border-2 border-black flex items-center justify-center shrink-0 shadow-neo-xs">
            <ShieldCheck className="w-5 h-5 text-black" />
          </div>
          <div className="flex flex-col gap-1">
            <h3 className="font-extrabold text-black uppercase tracking-wider text-sm">
              Tài khoản liên kết Google
            </h3>
            <p className="text-gray-700 text-xs sm:text-sm font-sans leading-relaxed">
              {t('settings.googleAccountNotice')}
            </p>
          </div>
        </div>
      </div>
    );
  }

  const byteLength = new TextEncoder().encode(newPassword).length;
  const isLengthValid = byteLength >= 12 && byteLength <= 72;
  const isMatch = newPassword.length > 0 && newPassword === confirmPassword;
  const containsEmail = checkPasswordContainsUserInfo(newPassword, userEmail);
  const canSubmit = currentPassword.length > 0 && isLengthValid && isMatch && !containsEmail && !isSubmitting;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;

    setIsSubmitting(true);
    setStatusMessage(null);

    try {
      await changePassword(currentPassword, newPassword);
      setStatusMessage({
        type: 'success',
        text: t('settings.changePasswordSuccess'),
      });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: unknown) {
      const message = resolvePasswordErrorMessage(err, t);
      setStatusMessage({ type: 'error', text: message });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="bg-white border-2 border-black rounded-2xl p-5 sm:p-6 shadow-neo-sm font-space flex flex-col gap-5">
      {statusMessage && (
        <div
          role="alert"
          className={`p-4 border-2 border-black rounded-xl text-xs sm:text-sm font-sans flex items-start gap-3 shadow-neo-xs ${
            statusMessage.type === 'success'
              ? 'bg-emerald-100 text-emerald-950'
              : 'bg-rose-100 text-rose-950'
          }`}
        >
          {statusMessage.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-700 shrink-0 mt-0.5" />
          ) : (
            <AlertCircle className="w-5 h-5 text-rose-700 shrink-0 mt-0.5" />
          )}
          <span className="leading-relaxed">{statusMessage.text}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {/* Current Password Field */}
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="currentPassword"
            className="text-xs font-bold uppercase tracking-wider text-gray-700 flex items-center justify-between"
          >
            <span>{t('settings.currentPassword')}</span>
          </label>
          <div className="relative">
            <input
              id="currentPassword"
              type={showCurrent ? 'text' : 'password'}
              autoComplete="current-password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
              className="w-full px-3.5 py-2.5 pr-11 bg-bg-canvas border-2 border-black rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-black shadow-neo-xs"
              placeholder="••••••••••••"
            />
            <button
              type="button"
              onClick={() => setShowCurrent(!showCurrent)}
              tabIndex={-1}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-600 hover:text-black p-1"
              aria-label={showCurrent ? 'Hide password' : 'Show password'}
            >
              {showCurrent ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* New Password Field */}
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="newPassword"
            className="text-xs font-bold uppercase tracking-wider text-gray-700 flex items-center justify-between"
          >
            <span>{t('settings.newPassword')}</span>
            <span className="text-[11px] font-normal normal-case tracking-normal text-gray-500">
              {t('settings.passwordMinHint')}
            </span>
          </label>
          <div className="relative">
            <input
              id="newPassword"
              type={showNew ? 'text' : 'password'}
              autoComplete="new-password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
              className="w-full px-3.5 py-2.5 pr-11 bg-bg-canvas border-2 border-black rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-black shadow-neo-xs"
              placeholder="••••••••••••"
            />
            <button
              type="button"
              onClick={() => setShowNew(!showNew)}
              tabIndex={-1}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-600 hover:text-black p-1"
              aria-label={showNew ? 'Hide password' : 'Show password'}
            >
              {showNew ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Confirm Password Field */}
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="confirmPassword"
            className="text-xs font-bold uppercase tracking-wider text-gray-700"
          >
            {t('settings.confirmPassword')}
          </label>
          <input
            id="confirmPassword"
            type={showNew ? 'text' : 'password'}
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            className="w-full px-3.5 py-2.5 bg-bg-canvas border-2 border-black rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-black shadow-neo-xs"
            placeholder="••••••••••••"
          />
        </div>

        {/* Live Validation Checklist */}
        <div className="bg-bg-canvas border border-black/30 rounded-xl p-3 flex flex-col gap-1.5 text-xs font-sans">
          <div className="flex items-center gap-2">
            <CheckCircle2
              className={`w-3.5 h-3.5 shrink-0 ${
                isLengthValid ? 'text-emerald-600' : 'text-gray-400'
              }`}
            />
            <span className={isLengthValid ? 'text-emerald-900 font-semibold' : 'text-gray-600'}>
              {t('settings.passwordLengthRule')}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle2
              className={`w-3.5 h-3.5 shrink-0 ${
                isMatch ? 'text-emerald-600' : 'text-gray-400'
              }`}
            />
            <span className={isMatch ? 'text-emerald-900 font-semibold' : 'text-gray-600'}>
              {t('settings.confirmPassword')} khớp nhau
            </span>
          </div>
          {userEmail && (
            <div className="flex items-center gap-2">
              <CheckCircle2
                className={`w-3.5 h-3.5 shrink-0 ${
                  !containsEmail ? 'text-emerald-600' : 'text-rose-500'
                }`}
              />
              <span
                className={
                  !containsEmail ? 'text-emerald-900 font-semibold' : 'text-rose-700 font-semibold'
                }
              >
                {t('settings.passwordNoEmailRule')}
              </span>
            </div>
          )}
        </div>

        {/* Action Button */}
        <div className="pt-2">
          <NeoButton
            type="submit"
            variant="primary"
            disabled={!canSubmit}
            className="w-full sm:w-auto px-6 py-2.5 text-xs font-space font-extrabold uppercase tracking-wider"
          >
            {isSubmitting ? t('settings.passwordUpdating') : t('settings.changePasswordSubmit')}
          </NeoButton>
        </div>
      </form>
    </div>
  );
}

