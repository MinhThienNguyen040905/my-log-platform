export { SettingsView } from './components/SettingsView';
export { useSettings } from './hooks/useSettings';
export { updateProfile, completeOnboarding, getProfile, getConsents, setConsent, AccountApiError } from './api/client';
export type { Profile, Consent } from './api/client';
export { requestAccountDeletion, getAccountDeletionStatus, cancelAccountDeletion } from './api/account-deletion';
export type { DeletionRequest } from './api/account-deletion';
export { useAccountProfile } from './hooks/useAccountProfile';
export { AccountProvider, useAccount } from './context/AccountContext';
