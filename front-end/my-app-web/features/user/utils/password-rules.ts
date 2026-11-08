import { AccountApiError } from '../api/client';

export const PASSWORD_ERROR_MAP: Record<string, string> = {
  CURRENT_PASSWORD_INCORRECT: 'settings.passwordCurrentIncorrect',
  NEW_PASSWORD_SAME_AS_OLD: 'settings.passwordSameAsOld',
  PASSWORD_CONTAINS_USER_INFO: 'settings.passwordContainsEmail',
  PASSWORD_LENGTH_INVALID: 'settings.passwordLengthInvalid',
  OAUTH_ONLY_ACCOUNT: 'settings.passwordOAuthOnly',
  ACCOUNT_NOT_ACTIVE: 'settings.passwordAccountNotActive',
  RATE_LIMITED: 'settings.passwordRateLimited',
  'current-password-required': 'settings.passwordCurrentIncorrect',
  'new-password-required': 'settings.passwordLengthInvalid',
};

/**
 * Checks if new password contains email local-part or the entire email.
 * Matches backend IdentityService.java:370-373 exactly:
 * - Extracts substring before '@' (case-insensitive)
 * - Only triggers if local-part length >= 4
 * - Includes safe guard against null/undefined/malformed email
 */
export function checkPasswordContainsUserInfo(newPassword: string, userEmail?: string | null): boolean {
  if (!userEmail || typeof userEmail !== 'string' || !userEmail.includes('@') || !newPassword) {
    return false;
  }
  const emailLower = userEmail.toLowerCase();
  const emailLocalPart = emailLower.split('@')[0];

  if (emailLocalPart.length >= 4 && newPassword.toLowerCase().includes(emailLocalPart)) {
    return true;
  }
  return newPassword.toLowerCase().includes(emailLower);
}

/**
 * Resolves a friendly localized error message for password change failures.
 * Never leaks raw technical error codes to the user.
 */
export function resolvePasswordErrorMessage(err: unknown, t: (key: string) => string): string {
  if (err instanceof AccountApiError) {
    const key = err.detail || err.code || err.message;
    if (key && PASSWORD_ERROR_MAP[key]) {
      return t(PASSWORD_ERROR_MAP[key]);
    }
    if (err.status === 429) {
      return t('settings.passwordRateLimited');
    }
  }
  return t('settings.passwordGeneralError');
}

