let refreshToken: string | null = null;
export const tokenStorage = {
  read: async () => refreshToken,
  write: async (value: string) => { refreshToken = value; },
  clear: async () => { refreshToken = null; },
};
