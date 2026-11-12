/**
 * Removes what this browser keeps for the user's journal (drafts, habit ticks, pending export job) after a data
 * reset. Onboarding preferences are kept: they belong to the profile, which the reset keeps.
 */
export function clearLocalJournalData(userId: string): void {
  const keep = (key: string) => key.endsWith('_onboarding_preferences');
  for (const storage of [safeStorage('localStorage'), safeStorage('sessionStorage')]) {
    if (!storage) continue;
    const keys: string[] = [];
    for (let i = 0; i < storage.length; i++) {
      const key = storage.key(i);
      if (key && key.startsWith('mylog') && key.includes(userId) && !keep(key)) keys.push(key);
    }
    for (const key of keys) {
      try { storage.removeItem(key); } catch { /* storage unavailable */ }
    }
  }
}

function safeStorage(name: 'localStorage' | 'sessionStorage'): Storage | null {
  try { return window[name]; } catch { return null; }
}
