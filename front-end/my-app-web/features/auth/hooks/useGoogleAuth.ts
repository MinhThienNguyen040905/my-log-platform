'use client';

import { useState, useCallback, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useToast } from '@/providers/ToastProvider';
import { useTranslation } from 'react-i18next';
import { authPost, authError } from '../api/client';
import { useAppLanguage } from '@/providers/AppLanguageProvider';

declare global {
  interface Window {
    google?: {
      accounts?: {
        oauth2?: {
          initCodeClient: (config: {
            client_id: string;
            scope: string;
            ux_mode?: 'popup' | 'redirect';
            state?: string;
            callback: (response: { code: string; state?: string }) => void;
            error_callback?: (error: { type: string; message?: string }) => void;
          }) => {
            requestCode: () => void;
          };
        };
      };
    };
  }
}

const GIS_SCRIPT_ID = 'google-identity-services-script';
const OAUTH_STATE_COOKIE = 'mylog_oauth_state';

export function useGoogleAuth(rememberMe = false) {
  const router = useRouter();
  const { showToast } = useToast();
  const { locale } = useAppLanguage();
  const { t } = useTranslation();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const scriptLoadedRef = useRef(false);

  const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || '';
  const isConfigured = Boolean(clientId);

  useEffect(() => {
    if (!isConfigured || typeof window === 'undefined') return;
    if (window.google?.accounts?.oauth2) {
      scriptLoadedRef.current = true;
      return;
    }
    const existingScript = document.getElementById(GIS_SCRIPT_ID);
    if (!existingScript) {
      const script = document.createElement('script');
      script.id = GIS_SCRIPT_ID;
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => {
        scriptLoadedRef.current = true;
      };
      document.head.appendChild(script);
    } else {
      existingScript.addEventListener('load', () => {
        scriptLoadedRef.current = true;
      });
    }
  }, [isConfigured]);

  const loginWithGoogle = useCallback(() => {
    if (!isConfigured) {
      showToast({
        title: t('auth.googleLoginUnavailable'),
        message: 'Google OAuth Client ID chưa được cấu hình.',
        type: 'info',
      });
      return;
    }

    if (!window.google?.accounts?.oauth2) {
      showToast({
        title: t('auth.googleLoggingIn'),
        message: 'Đang tải thư viện Google, vui lòng thử lại sau giây lát...',
        type: 'info',
      });
      return;
    }

    // Tạo state ngẫu nhiên chống Login CSRF
    const state = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : Math.random().toString(36).substring(2) + Date.now().toString(36);

    // Lưu state vào cookie 10 phút
    document.cookie = `${OAUTH_STATE_COOKIE}=${state}; path=/; max-age=600; SameSite=Lax`;

    try {
      setIsSubmitting(true);
      const client = window.google.accounts.oauth2.initCodeClient({
        client_id: clientId,
        scope: 'openid email profile',
        ux_mode: 'popup',
        state,
        callback: async (response) => {
          if (!response.code) {
            setIsSubmitting(false);
            return;
          }

          try {
            const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
            const res = await authPost('google', {
              code: response.code,
              state: response.state || state,
              timezone,
              locale,
              rememberMe,
            });

            if (!res.ok) {
              const errorMessage = await authError(res, locale);
              throw new Error(errorMessage);
            }

            showToast({ title: t('auth.loginSuccess'), type: 'success' });
            router.replace('/dashboard');
            router.refresh();
          } catch (err: unknown) {
            const message = err instanceof Error ? err.message : t('auth.loginError');
            showToast({ title: t('auth.loginError'), message, type: 'error' });
          } finally {
            setIsSubmitting(false);
          }
        },
        error_callback: (error) => {
          setIsSubmitting(false);
          if (error.type === 'popup_closed') {
            // Người dùng chủ động tắt popup Google, không bắn toast lỗi đỏ
            return;
          }
          showToast({ title: t('auth.loginError'), message: error.message, type: 'error' });
        },
      });

      client.requestCode();
    } catch {
      setIsSubmitting(false);
      showToast({ title: t('auth.loginError'), type: 'error' });
    }
  }, [clientId, isConfigured, locale, rememberMe, router, showToast, t]);

  return {
    loginWithGoogle,
    isSubmitting,
    isConfigured,
  };
}
