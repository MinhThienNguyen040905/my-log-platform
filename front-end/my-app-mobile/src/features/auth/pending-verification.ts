let pendingEmail = '';
export function rememberPendingEmail(email: string) { pendingEmail = email.trim(); }
export function readPendingEmail() { return pendingEmail; }
export function clearPendingEmail() { pendingEmail = ''; }
