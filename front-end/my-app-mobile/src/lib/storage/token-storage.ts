import * as SecureStore from 'expo-secure-store';

const key = 'mylog_refresh_token';
export const tokenStorage = {
  read: () => SecureStore.getItemAsync(key),
  write: (value: string) => SecureStore.setItemAsync(key, value),
  clear: () => SecureStore.deleteItemAsync(key),
};
