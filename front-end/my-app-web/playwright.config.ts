import { defineConfig, devices } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';

/**
 * Playwright setup for this web app. `/journal-editor` and every other `(app)` route sit behind
 * `app/(app)/layout.tsx`, which redirects to `/auth/login` unless there is a real session cookie —
 * so anything beyond the public pages needs the backend running too (see tests/README.md) and a
 * seeded test account, same constraints as JOURNAL_NAV_PLAYWRIGHT_SCENARIOS.md already documents
 * for manual runs. Never point BASE_URL at a production or staging environment.
 *
 * Must be `http://localhost:3000`, not `http://127.0.0.1:3000`: every state-changing route under
 * `app/api/**` is guarded by `sameOrigin()` (features/auth/api/backend.ts), which compares the
 * browser's `Origin` header against `new URL(request.url).origin`. Under `next dev` that always
 * resolves to `localhost`, even though the dev script binds `--hostname 127.0.0.1` — so a real,
 * correct same-origin request from `http://127.0.0.1:3000` gets rejected with 403 before the
 * backend is ever called. Confirmed directly: identical request, only the Origin header's host
 * changed `127.0.0.1` → `localhost`, and the response went from 403 to a real backend response.
 * This isn't a Playwright quirk; any browser reaching the app via its `127.0.0.1` URL hits the same
 * wall on login/register/etc. Worth fixing in `sameOrigin()` or the dev script — out of scope here.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list']],
  use: {
    baseURL: BASE_URL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile-chromium', use: { ...devices['Pixel 7'] } },
  ],
  webServer: process.env.PLAYWRIGHT_SKIP_WEBSERVER
    ? undefined
    : {
        command: 'npm run dev',
        url: BASE_URL,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
});
