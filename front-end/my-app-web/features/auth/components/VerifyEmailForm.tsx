'use client';

import { useEffect, useMemo, useRef, useState, type ClipboardEvent, type FormEvent, type KeyboardEvent } from 'react';
import Link from 'next/link';
import { authError, authPost } from '../api/client';
import { createAuthSchemas } from '../schemas/auth-forms';
import { forgetVerificationEmail, readVerificationEmail } from '../verification-email';
import { useAppLanguage } from '@/providers/AppLanguageProvider';
import { useTranslation } from 'react-i18next';
import { LanguageSwitcher } from '@/components/ui/LanguageSwitcher';

const codeLength = 8;

export function VerifyEmailForm() {
  const { locale } = useAppLanguage();
  const { t } = useTranslation();
  const schemas = useMemo(() => createAuthSchemas(locale), [locale]);
  const inputs = useRef<(HTMLInputElement | null)[]>([]);
  const [email, setEmail] = useState('');
  const [digits, setDigits] = useState<string[]>(() => Array(codeLength).fill(''));
  const [token, setToken] = useState<string | null>(null);
  const [ready, setReady] = useState(false);
  const [busy, setBusy] = useState(false);
  const [verified, setVerified] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    const url = new URL(window.location.href);
    const linkToken = url.searchParams.get('token');
    if (linkToken) {
      url.searchParams.delete('token');
      window.history.replaceState(window.history.state, '', url.pathname + url.search + url.hash);
      window.setTimeout(() => setToken(linkToken), 0);
    }
    window.setTimeout(() => {
      setEmail(readVerificationEmail());
      setReady(true);
    }, 0);
  }, []);

  const focusDigit = (index: number) => {
    inputs.current[index]?.focus();
    inputs.current[index]?.select();
  };

  const fillDigits = (index: number, value: string) => {
    const numbers = value.replace(/\D/g, '');
    if (!numbers) {
      setDigits((current) => current.map((digit, position) => position === index ? '' : digit));
      return;
    }
    setDigits((current) => {
      const next = [...current];
      for (let offset = 0; offset < numbers.length && index + offset < codeLength; offset++) next[index + offset] = numbers[offset];
      return next;
    });
    focusDigit(Math.min(index + numbers.length, codeLength - 1));
    setError('');
  };

  const handlePaste = (event: ClipboardEvent<HTMLInputElement>, index: number) => {
    event.preventDefault();
    fillDigits(index, event.clipboardData.getData('text'));
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>, index: number) => {
    if (event.key === 'Backspace' && !digits[index] && index > 0) {
      event.preventDefault();
      focusDigit(index - 1);
    } else if (event.key === 'ArrowLeft' && index > 0) {
      event.preventDefault();
      focusDigit(index - 1);
    } else if (event.key === 'ArrowRight' && index < codeLength - 1) {
      event.preventDefault();
      focusDigit(index + 1);
    }
  };

  const confirmCode = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const result = schemas.verification.safeParse({ email, code: digits.join('') });
    if (!result.success) {
      setError(result.error.issues.find((issue) => issue.path[0] === 'code')?.message ?? t('auth.verifyError'));
      return;
    }
    setBusy(true); setError(''); setMessage('');
    try {
      const response = await authPost('verify-code', result.data);
      if (!response.ok) throw new Error(await authError(response, locale, 'verification'));
      forgetVerificationEmail();
      setVerified(true);
      setMessage(t('auth.verifySuccess'));
    } catch (cause) { setError(cause instanceof Error ? cause.message : t('auth.verifyError')); }
    finally { setBusy(false); }
  };

  const confirmLink = async () => {
    if (!token) return;
    setBusy(true); setError(''); setMessage('');
    try {
      const response = await authPost('verify', { token });
      if (!response.ok) throw new Error(await authError(response, locale, 'verification'));
      forgetVerificationEmail();
      setToken(null);
      setVerified(true);
      setMessage(t('auth.verifySuccess'));
    } catch (cause) { setError(cause instanceof Error ? cause.message : t('auth.verifyError')); }
    finally { setBusy(false); }
  };

  const resend = async () => {
    if (!email) return;
    setBusy(true); setError(''); setMessage('');
    try {
      const response = await authPost('resend', { email });
      if (!response.ok) throw new Error(await authError(response, locale, 'verification'));
      setToken(null);
      setDigits(Array(codeLength).fill(''));
      setMessage(t('auth.resendSuccess'));
    } catch (cause) { setError(cause instanceof Error ? cause.message : t('auth.resendError')); }
    finally { setBusy(false); }
  };

  return <main className="min-h-screen bg-bg-canvas flex items-center justify-center px-4">
    <section className="w-full max-w-md bg-surface-card border-neo rounded-3xl p-6 shadow-neo space-y-5">
      <LanguageSwitcher />
      <h1 className="font-space text-2xl font-extrabold">{t('auth.verify')}</h1>
      {!verified && <p className="text-sm text-on-surface-variant">
        {!ready ? t('common.loading') : token ? t('auth.verifyLinkDescription') : email ? t('auth.verifyDescription') : t('auth.verifyEmailUnavailable')}
      </p>}
      {!verified && ready && token && <button type="button" onClick={confirmLink} disabled={busy} className="w-full min-h-11 rounded-xl border border-black bg-primary-container font-bold disabled:opacity-60">{t('auth.verifyLinkAction')}</button>}
      {!verified && ready && !token && email && <form onSubmit={confirmCode} noValidate className="space-y-3">
        <div id="verify-code-label" className="text-sm font-bold">{t('auth.verificationCode')}</div>
        <div role="group" aria-labelledby="verify-code-label" className="grid grid-cols-8 gap-1 sm:gap-2">
          {digits.map((digit, index) => <input
            key={index}
            ref={(element) => { inputs.current[index] = element; }}
            value={digit}
            onChange={(event) => fillDigits(index, event.target.value)}
            onPaste={(event) => handlePaste(event, index)}
            onKeyDown={(event) => handleKeyDown(event, index)}
            onFocus={(event) => event.currentTarget.select()}
            type="text"
            inputMode="numeric"
            autoComplete={index === 0 ? 'one-time-code' : 'off'}
            maxLength={index === 0 ? codeLength : 1}
            aria-label={t('auth.codeDigit', { number: index + 1 })}
            aria-invalid={Boolean(error)}
            aria-describedby={error ? 'verify-code-error' : undefined}
            className="w-full min-w-0 h-12 rounded-xl border border-black bg-white text-center text-xl font-bold"
          />)}
        </div>
        <button type="submit" disabled={busy} className="w-full min-h-11 rounded-xl border border-black bg-primary-container font-bold disabled:opacity-60">{t('auth.verify')}</button>
      </form>}
      {!verified && ready && !token && email && <div className="border-t border-border-soft pt-4">
        <button type="button" onClick={resend} disabled={busy} className="min-h-11 px-4 rounded-xl border border-black disabled:opacity-60">{t('auth.resend')}</button>
      </div>}
      {message && <p role="status" className="text-sm">{message}</p>}
      {error && <p id="verify-code-error" role="alert" className="text-sm text-red-700">{error}</p>}
      <Link href="/auth/login" className="block text-sm underline">{t('auth.backToLogin')}</Link>
    </section>
  </main>;
}
