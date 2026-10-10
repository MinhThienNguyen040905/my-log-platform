'use client';

import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { authError, authPost } from '../api/client';
import { createAuthSchemas, verificationSchema, resendVerificationSchema } from '../schemas/auth-forms';
import { useAppLanguage } from '@/providers/AppLanguageProvider';
import { useTranslation } from 'react-i18next';
import { LanguageSwitcher } from '@/components/ui/LanguageSwitcher';

export function VerifyEmailForm() {
  const router = useRouter();
  const { locale } = useAppLanguage();
  const { t } = useTranslation();
  const schemas = useMemo(() => createAuthSchemas(locale), [locale]);
  const verifyForm = useForm<z.infer<typeof verificationSchema>>({ resolver: zodResolver(schemas.verification), defaultValues: { token: '' } });
  const resendForm = useForm<z.infer<typeof resendVerificationSchema>>({ resolver: zodResolver(schemas.resendVerification), defaultValues: { email: '' } });
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const confirm = verifyForm.handleSubmit(async ({ token }) => {
    setBusy(true); setError(''); setMessage('');
    try {
      const response = await authPost('verify', { token: token.trim() });
      if (!response.ok) throw new Error(await authError(response, locale));
      router.replace('/auth/login');
    } catch (cause) { setError(cause instanceof Error ? cause.message : t('auth.verifyError')); }
    finally { setBusy(false); }
  });

  const resend = resendForm.handleSubmit(async ({ email }) => {
    setBusy(true); setError(''); setMessage('');
    try {
      const response = await authPost('resend', { email: email.trim() });
      if (!response.ok) throw new Error(await authError(response, locale));
      setMessage(t('auth.resendSuccess'));
    } catch (cause) { setError(cause instanceof Error ? cause.message : t('auth.resendError')); }
    finally { setBusy(false); }
  });

  return <main className="min-h-screen bg-bg-canvas flex items-center justify-center px-4">
    <section className="w-full max-w-md bg-surface-card border-neo rounded-3xl p-6 shadow-neo space-y-5">
      <LanguageSwitcher />
      <h1 className="font-space text-2xl font-extrabold">{t('auth.verify')}</h1>
      <p className="text-sm text-on-surface-variant">{t('auth.verifyDescription')}</p>
      <form onSubmit={confirm} noValidate className="space-y-3">
        <label htmlFor="verify-token" className="block text-sm font-bold">{t('auth.verificationCode')}</label>
        <input id="verify-token" {...verifyForm.register('token')} aria-invalid={!!verifyForm.formState.errors.token} aria-describedby={verifyForm.formState.errors.token ? 'verify-token-error' : undefined} autoComplete="one-time-code" className="w-full min-h-11 rounded-xl border border-black px-3" />
        {verifyForm.formState.errors.token && <p id="verify-token-error" role="alert" className="text-sm text-red-700">{verifyForm.formState.errors.token.message}</p>}
        <button type="submit" disabled={busy} className="w-full min-h-11 rounded-xl border border-black bg-primary-container font-bold disabled:opacity-60">{t('auth.verify')}</button>
      </form>
      <form onSubmit={resend} noValidate className="border-t border-border-soft pt-4 space-y-3">
        <label htmlFor="verify-email" className="block text-sm font-bold">{t('auth.resendToEmail')}</label>
        <input id="verify-email" type="email" {...resendForm.register('email')} aria-invalid={!!resendForm.formState.errors.email} aria-describedby={resendForm.formState.errors.email ? 'verify-email-error' : undefined} className="w-full min-h-11 rounded-xl border border-black px-3" />
        {resendForm.formState.errors.email && <p id="verify-email-error" role="alert" className="text-sm text-red-700">{resendForm.formState.errors.email.message}</p>}
        <button type="submit" disabled={busy} className="min-h-11 px-4 rounded-xl border border-black disabled:opacity-60">{t('auth.resend')}</button>
      </form>
      {message && <p role="status" className="text-sm">{message}</p>}
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      <Link href="/auth/login" className="block text-sm underline">{t('auth.backToLogin')}</Link>
    </section>
  </main>;
}
