export function clearJournalDrafts() {
  if (typeof window === 'undefined') return;
  const keys: string[] = [];
  for (let index = 0; index < localStorage.length; index++) {
    const key = localStorage.key(index);
    if (key && /^mylog_[0-9a-fA-F-]{36}_draft_journal$/.test(key)) keys.push(key);
  }
  for (const key of keys) localStorage.removeItem(key);
}
