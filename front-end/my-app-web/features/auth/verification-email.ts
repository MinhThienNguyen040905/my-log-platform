const key = 'mylog-pending-verification-email';
const lifetimeMs = 30 * 60 * 1000;

export function rememberVerificationEmail(email: string) {
  try {
    sessionStorage.setItem(key, JSON.stringify({ email: email.trim(), expiresAt: Date.now() + lifetimeMs }));
  } catch { /* Verification can still continue from the email link. */ }
}

export function readVerificationEmail(): string {
  try {
    const value: unknown = JSON.parse(sessionStorage.getItem(key) ?? 'null');
    if (value && typeof value === 'object' && 'email' in value && 'expiresAt' in value
      && typeof value.email === 'string' && typeof value.expiresAt === 'number'
      && value.expiresAt > Date.now() && value.email.length <= 254) return value.email;
    sessionStorage.removeItem(key);
  } catch { /* Storage may be unavailable or contain an old value. */ }
  return '';
}

export function forgetVerificationEmail() {
  try { sessionStorage.removeItem(key); } catch { /* Storage may be unavailable. */ }
}
