'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Eye, EyeOff, Hash, Key, Mail } from 'lucide-react';
import { useForm, type FieldErrors, type UseFormRegister } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import type { z } from 'zod';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { useAppLanguage } from '@/providers/AppLanguageProvider';
import { useToast } from '@/providers/ToastProvider';
import { authError, authPost } from '../api/client';
import { createAuthSchemas } from '../schemas/auth-forms';
import { forgetPasswordResetEmail, readPasswordResetEmail } from '../password-reset-email';
import { AuthCardShell } from './AuthCardShell';

type Schemas = ReturnType<typeof createAuthSchemas>;
type LinkValues = z.infer<Schemas['resetWithToken']>;
type CodeValues = z.infer<Schemas['resetWithCode']>;
type PasswordValues = { newPassword: string; confirmPassword: string };

const inputClass =
  'w-full pl-10 pr-10 py-2.5 bg-paper-warm border-neo-sm rounded-xl font-sans text-sm text-on-surface placeholder:text-gray-400 focus:outline-none focus:bg-white focus:shadow-neo-sm transition-all';
const submitClass =
  'w-full py-3 px-4 bg-primary-container border-neo rounded-xl font-space font-extrabold text-sm uppercase tracking-wider shadow-neo hover:-translate-x-0.5 hover:-translate-y-0.5 hover:shadow-neo-lg active:translate-x-1 active:translate-y-1 active:shadow-none transition-all cursor-pointer disabled:opacity-70';

/** Mã lỗi `detail` backend trả cho chính sách mật khẩu (PasswordChangeException). */
const PASSWORD_POLICY_ERRORS: Record<string, string> = {
  NEW_PASSWORD_SAME_AS_OLD: 'auth.resetSamePassword',
  PASSWORD_CONTAINS_USER_INFO: 'auth.resetPasswordContainsEmail',
  PASSWORD_LENGTH_INVALID: 'auth.passwordShort',
};

async function resetErrorMessage(response: Response, locale: 'vi' | 'en', t: TFunction): Promise<string> {
  if (response.status === 401) return t('auth.resetInvalid');
  if (response.status === 400) {
    try {
      const problem: unknown = await response.clone().json();
      const detail = problem && typeof problem === 'object' && 'detail' in problem ? problem.detail : null;
      if (typeof detail === 'string' && PASSWORD_POLICY_ERRORS[detail]) return t(PASSWORD_POLICY_ERRORS[detail]);
    } catch { /* fall through to the generic message */ }
  }
  return authError(response, locale);
}

/**
 * Đặt lại mật khẩu. Liên kết trong email có dạng `/auth/reset-password#token=…`: token nằm sau `#`
 * nên không đi lên server; trang đọc rồi xóa ngay khỏi thanh địa chỉ. Không có token thì dùng email + mã.
 */
export function ResetPasswordForm() {
  const { t } = useTranslation();
  const [token, setToken] = useState<string | null>(null);
  const [ready, setReady] = useState(false);
  const [preferCode, setPreferCode] = useState(false);

  useEffect(() => {
    const fragment = new URLSearchParams(window.location.hash.slice(1));
    const linkToken = fragment.get('token');
    if (linkToken) {
      window.history.replaceState(window.history.state, '', window.location.pathname + window.location.search);
    }
    // Only ever set a found token: under React StrictMode the effect runs twice, and the second run
    // sees the hash already cleared — it must not overwrite the token read by the first run.
    window.setTimeout(() => {
      if (linkToken) setToken(linkToken);
      setReady(true);
    }, 0);
  }, []);

  const useLink = !!token && !preferCode;
  return (
    <AuthCardShell
      title={t('auth.resetTitle')}
      description={useLink ? t('auth.resetDescriptionLink') : t('auth.resetDescriptionCode')}
      exitLabel={t('auth.exitAuth')}
    >
      {!ready ? (
        <p role='status' className='font-space text-sm'>{t('common.loading')}</p>
      ) : token && useLink ? (
        <LinkResetForm token={token} onUseCode={() => setPreferCode(true)} />
      ) : (
        <CodeResetForm />
      )}
      <div className='mt-6 pt-3 border-t border-border-soft flex flex-wrap justify-between gap-2'>
        <Link href='/auth/forgot-password' className='font-space text-xs font-bold underline'>{t('auth.requestNewReset')}</Link>
        <Link href='/auth/login' className='font-space text-xs font-bold underline'>{t('auth.backToLogin')}</Link>
      </div>
    </AuthCardShell>
  );
}

function useResetSuccess() {
  const router = useRouter();
  const { showToast } = useToast();
  const { t } = useTranslation();
  return () => {
    forgetPasswordResetEmail();
    showToast({ title: t('auth.resetSuccess'), message: t('auth.resetSuccessDetail'), type: 'success' });
    router.replace('/auth/login');
  };
}

function LinkResetForm({ token, onUseCode }: { token: string; onUseCode: () => void }) {
  const { locale } = useAppLanguage();
  const { t } = useTranslation();
  const onSuccess = useResetSuccess();
  const schema = useMemo(() => createAuthSchemas(locale).resetWithToken, [locale]);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LinkValues>({
    resolver: zodResolver(schema),
    mode: 'onBlur',
    defaultValues: { newPassword: '', confirmPassword: '' },
  });
  const [error, setError] = useState('');

  const submit = handleSubmit(async ({ newPassword }) => {
    setError('');
    const response = await authPost('password-resets/confirm', { token, newPassword }).catch(() => null);
    if (!response) { setError(t('auth.tryAgain')); return; }
    if (!response.ok) { setError(await resetErrorMessage(response, locale, t)); return; }
    onSuccess();
  });

  return (
    <form onSubmit={submit} noValidate className='flex flex-col gap-4'>
      <PasswordFields register={register} errors={errors} idPrefix='reset-link' />
      {error && <p role='alert' className='text-xs text-red-700'>{error}</p>}
      <button type='submit' disabled={isSubmitting} className={submitClass}>
        {isSubmitting ? t('auth.resetting') : t('auth.resetSubmit')}
      </button>
      <button type='button' onClick={onUseCode} className='font-space text-xs font-bold underline self-start cursor-pointer'>
        {t('auth.useCodeInstead')}
      </button>
    </form>
  );
}

function CodeResetForm() {
  const { locale } = useAppLanguage();
  const { t } = useTranslation();
  const onSuccess = useResetSuccess();
  const schema = useMemo(() => createAuthSchemas(locale).resetWithCode, [locale]);
  const { register, handleSubmit, setValue, formState: { errors, isSubmitting } } = useForm<CodeValues>({
    resolver: zodResolver(schema),
    mode: 'onBlur',
    defaultValues: { email: '', code: '', newPassword: '', confirmPassword: '' },
  });
  const [error, setError] = useState('');

  useEffect(() => {
    const remembered = readPasswordResetEmail();
    if (remembered) setValue('email', remembered);
  }, [setValue]);

  const submit = handleSubmit(async ({ email, code, newPassword }) => {
    setError('');
    const response = await authPost('password-resets/confirm-code', { email: email.trim(), code: code.trim(), newPassword }).catch(() => null);
    if (!response) { setError(t('auth.tryAgain')); return; }
    if (!response.ok) { setError(await resetErrorMessage(response, locale, t)); return; }
    onSuccess();
  });

  return (
    <form onSubmit={submit} noValidate className='flex flex-col gap-4'>
      <div className='flex flex-col gap-1.5'>
        <label htmlFor='reset-email' className='font-space text-xs font-bold uppercase text-on-surface'>{t('auth.email')}</label>
        <div className='relative flex items-center'>
          <Mail className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' aria-hidden='true' />
          <input
            id='reset-email'
            type='email'
            autoComplete='email'
            aria-invalid={!!errors.email}
            aria-describedby={errors.email ? 'reset-email-error' : undefined}
            {...register('email')}
            className={inputClass}
          />
        </div>
        {errors.email && <p id='reset-email-error' role='alert' className='text-xs text-red-700'>{errors.email.message}</p>}
      </div>

      <div className='flex flex-col gap-1.5'>
        <label htmlFor='reset-code' className='font-space text-xs font-bold uppercase text-on-surface'>{t('auth.verificationCode')}</label>
        <div className='relative flex items-center'>
          <Hash className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' aria-hidden='true' />
          <input
            id='reset-code'
            type='text'
            inputMode='numeric'
            autoComplete='one-time-code'
            // Cho phép dán "1234 5678" / "1234-5678": giới hạn ký tự rộng hơn 8, lọc chữ số khi đọc giá trị.
            maxLength={16}
            aria-invalid={!!errors.code}
            aria-describedby={errors.code ? 'reset-code-error' : undefined}
            {...register('code', { setValueAs: (value: string) => value.replace(/\D/g, '').slice(0, 8) })}
            placeholder='12345678'
            className={`${inputClass} font-mono tracking-[0.3em]`}
          />
        </div>
        {errors.code && <p id='reset-code-error' role='alert' className='text-xs text-red-700'>{errors.code.message}</p>}
      </div>

      <PasswordFields register={register} errors={errors} idPrefix='reset-code' />
      {error && <p role='alert' className='text-xs text-red-700'>{error}</p>}
      <button type='submit' disabled={isSubmitting} className={submitClass}>
        {isSubmitting ? t('auth.resetting') : t('auth.resetSubmit')}
      </button>
    </form>
  );
}

function PasswordFields<T extends PasswordValues>({ register, errors, idPrefix }: {
  register: UseFormRegister<T>;
  errors: FieldErrors<T>;
  idPrefix: string;
}) {
  const { t } = useTranslation();
  const [visible, setVisible] = useState(false);
  // Both forms share these two fields; the casts narrow the generic form to its password part.
  const reg = register as unknown as UseFormRegister<PasswordValues>;
  const errs = errors as FieldErrors<PasswordValues>;
  const fields = [
    { name: 'newPassword', label: t('auth.newPassword'), autoComplete: 'new-password' },
    { name: 'confirmPassword', label: t('auth.confirmNewPassword'), autoComplete: 'new-password' },
  ] as const;

  return (
    <>
      {fields.map((field) => {
        const id = `${idPrefix}-${field.name}`;
        const fieldError = errs[field.name];
        return (
          <div key={field.name} className='flex flex-col gap-1.5'>
            <label htmlFor={id} className='font-space text-xs font-bold uppercase text-on-surface'>{field.label}</label>
            <div className='relative flex items-center'>
              <Key className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' aria-hidden='true' />
              <input
                id={id}
                type={visible ? 'text' : 'password'}
                autoComplete={field.autoComplete}
                aria-invalid={!!fieldError}
                aria-describedby={fieldError ? `${id}-error` : undefined}
                {...reg(field.name)}
                placeholder={field.name === 'newPassword' ? t('auth.passwordPlaceholder') : t('auth.confirmPlaceholder')}
                className={inputClass}
              />
              {field.name === 'newPassword' && (
                <button
                  type='button'
                  onClick={() => setVisible((value) => !value)}
                  aria-label={visible ? t('auth.hidePassword') : t('auth.showPassword')}
                  aria-pressed={visible}
                  className='absolute right-3 text-gray-500 hover:text-black transition-colors p-1'
                >
                  {visible ? <EyeOff className='w-4 h-4' /> : <Eye className='w-4 h-4' />}
                </button>
              )}
            </div>
            {fieldError && <p id={`${id}-error`} role='alert' className='text-xs text-red-700'>{fieldError.message}</p>}
          </div>
        );
      })}
    </>
  );
}
