'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { Mail, MailCheck } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import type { z } from 'zod';
import { useTranslation } from 'react-i18next';
import { useAppLanguage } from '@/providers/AppLanguageProvider';
import { authError, authPost } from '../api/client';
import { createAuthSchemas } from '../schemas/auth-forms';
import { rememberPasswordResetEmail } from '../password-reset-email';
import { AuthCardShell } from './AuthCardShell';

type ForgotValues = z.infer<ReturnType<typeof createAuthSchemas>['forgotPassword']>;

export function ForgotPasswordForm() {
  const { locale } = useAppLanguage();
  const { t } = useTranslation();
  const schema = useMemo(() => createAuthSchemas(locale).forgotPassword, [locale]);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<ForgotValues>({
    resolver: zodResolver(schema),
    mode: 'onBlur',
    defaultValues: { email: '' },
  });
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');

  const submit = handleSubmit(async ({ email }) => {
    setError('');
    const response = await authPost('password-resets', { email: email.trim() }).catch(() => null);
    if (!response) { setError(t('auth.tryAgain')); return; }
    if (!response.ok) { setError(await authError(response, locale)); return; }
    rememberPasswordResetEmail(email);
    setSent(true);
  });

  return (
    <AuthCardShell title={t('auth.forgotTitle')} description={t('auth.forgotDescription')} exitLabel={t('auth.exitAuth')}>
      {sent ? (
        <div className='flex flex-col gap-4'>
          <p role='status' className='flex items-start gap-2 rounded-xl border-2 border-black bg-paper-warm p-3 font-sans text-sm text-on-surface'>
            <MailCheck className='w-5 h-5 shrink-0 mt-0.5' aria-hidden='true' />
            <span>{t('auth.forgotSent')}</span>
          </p>
          <Link
            href='/auth/reset-password'
            className='w-full py-3 px-4 bg-primary-container border-neo rounded-xl font-space font-extrabold text-sm uppercase tracking-wider shadow-neo text-center hover:-translate-x-0.5 hover:-translate-y-0.5 hover:shadow-neo-lg transition-all'
          >
            {t('auth.forgotHaveCode')}
          </Link>
          <button type='button' onClick={() => setSent(false)} className='font-space text-xs font-bold underline self-start cursor-pointer'>
            {t('auth.requestNewReset')}
          </button>
        </div>
      ) : (
        <form onSubmit={submit} noValidate className='flex flex-col gap-4'>
          <div className='flex flex-col gap-1.5'>
            <label htmlFor='forgot-email' className='font-space text-xs font-bold uppercase text-on-surface'>{t('auth.email')}</label>
            <div className='relative flex items-center'>
              <Mail className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' aria-hidden='true' />
              <input
                id='forgot-email'
                type='email'
                autoComplete='email'
                aria-invalid={!!errors.email}
                aria-describedby={errors.email ? 'forgot-email-error' : undefined}
                {...register('email')}
                placeholder='minhanh@gmail.com'
                className='w-full pl-10 pr-3 py-2.5 bg-paper-warm border-neo-sm rounded-xl font-sans text-sm text-on-surface placeholder:text-gray-400 focus:outline-none focus:bg-white focus:shadow-neo-sm transition-all'
              />
            </div>
            {errors.email && <p id='forgot-email-error' role='alert' className='text-xs text-red-700'>{errors.email.message}</p>}
          </div>
          {error && <p role='alert' className='text-xs text-red-700'>{error}</p>}
          <button
            type='submit'
            disabled={isSubmitting}
            className='w-full py-3 px-4 bg-primary-container border-neo rounded-xl font-space font-extrabold text-sm uppercase tracking-wider shadow-neo hover:-translate-x-0.5 hover:-translate-y-0.5 hover:shadow-neo-lg active:translate-x-1 active:translate-y-1 active:shadow-none transition-all cursor-pointer disabled:opacity-70'
          >
            {isSubmitting ? t('auth.forgotSending') : t('auth.forgotSubmit')}
          </button>
          <Link href='/auth/reset-password' className='font-space text-xs font-bold underline self-start'>{t('auth.forgotHaveCode')}</Link>
        </form>
      )}
      <div className='mt-6 pt-3 border-t border-border-soft text-center'>
        <Link href='/auth/login' className='font-space text-xs font-bold underline'>{t('auth.backToLogin')}</Link>
      </div>
    </AuthCardShell>
  );
}
