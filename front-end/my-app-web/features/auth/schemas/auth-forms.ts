import { z } from 'zod';
import type { AppLocale } from '@/lib/i18n';

function messages(locale: AppLocale) {
  return locale === 'en' ? {
    email: 'Enter a valid email address.', emailLong: 'Email is too long.',
    passwordShort: 'Use at least 12 characters.', passwordLong: 'Use at most 128 characters.',
    passwordRequired: 'Enter your password.', confirm: 'Confirm your password.',
    mismatch: 'Passwords do not match.', terms: 'Accept the terms and privacy policy.',
    token: 'Enter the verification code.', tokenLong: 'Verification code is too long.',
  } : {
    email: 'Email không hợp lệ.', emailLong: 'Email quá dài.',
    passwordShort: 'Mật khẩu cần ít nhất 12 ký tự.', passwordLong: 'Mật khẩu tối đa 128 ký tự.',
    passwordRequired: 'Vui lòng nhập mật khẩu.', confirm: 'Vui lòng xác nhận mật khẩu.',
    mismatch: 'Mật khẩu xác nhận chưa khớp.', terms: 'Bạn cần đồng ý điều khoản và chính sách riêng tư.',
    token: 'Vui lòng nhập mã xác minh.', tokenLong: 'Mã xác minh quá dài.',
  };
}

export function createAuthSchemas(locale: AppLocale) {
  const m = messages(locale);
  const email = z.string().trim().pipe(z.email(m.email).max(254, m.emailLong));
  return {
    register: z.object({
      email,
      password: z.string().min(12, m.passwordShort).max(128, m.passwordLong),
      confirmPassword: z.string().min(1, m.confirm),
      agreeTerms: z.boolean().refine(Boolean, m.terms),
    }).refine((value) => value.password === value.confirmPassword, { path: ['confirmPassword'], message: m.mismatch }),
    login: z.object({ email, password: z.string().min(1, m.passwordRequired), rememberMe: z.boolean() }),
    verification: z.object({ token: z.string().trim().min(1, m.token).max(512, m.tokenLong) }),
    resendVerification: z.object({ email }),
  };
}

export const registerSchema = createAuthSchemas('vi').register;
export const loginSchema = createAuthSchemas('vi').login;
export const verificationSchema = createAuthSchemas('vi').verification;
export const resendVerificationSchema = createAuthSchemas('vi').resendVerification;
