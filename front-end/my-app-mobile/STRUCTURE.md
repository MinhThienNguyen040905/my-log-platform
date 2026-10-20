# MyLog mobile structure

This is the ownership guide for the Expo 57 app. Auth, onboarding, journal, and selected overview routes now exist. The tree below includes future targets; check source before treating a listed feature as implemented. Create each directory only when its first real file is needed.

For the delivery sequence and exit criteria, see [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

```text
my-app-mobile/
├── src/
│   ├── app/                       Expo Router screens and navigation
│   │   ├── _layout.tsx            Root providers and session bootstrap
│   │   ├── (auth)/                Login, register, email verification
│   │   ├── (onboarding)/          First-use flow
│   │   └── (app)/                 Signed-in tabs and detail screens
│   ├── features/
│   │   ├── auth/                  Session, auth API, forms
│   │   ├── onboarding/            Profile setup
│   │   ├── journal/               Editor, entries, drafts, tags
│   │   ├── overview/              Dashboard, insight, report and self-care API
│   │   └── user/                  Profile, consents, settings
│   ├── components/                Truly shared native UI
│   ├── providers/                 App-wide session, query, language, theme
│   ├── lib/
│   │   ├── api/                   Backend HTTP client and error handling
│   │   └── storage/               Secure tokens and local drafts
│   ├── constants/                 Shared design tokens and constants
│   └── types/                     Types used across features only
├── assets/                        Fonts, icons, and images
└── skills/mylog-mobile-design/    Agent instructions for mobile UI work
```

## Ownership

- `src/app/` defines routes, navigation, and screen composition. Put feature logic in `src/features/<feature>/`.
- A feature owns its API functions, state, schemas, types, and UI. Extract to `src/components/` or `src/lib/` only after a concrete cross-feature use appears.
- `src/lib/api/` calls the backend `/api/v1` contract directly from the native app. Next.js `app/api` routes and server cookie helpers in `my-app-web` are web-only.
- Backend owns authorization, safety checks, and data rules. Preserve contract details such as `Idempotency-Key`, `If-Match`, cursor pagination, and supported journal `contentJson`.
- Keep refresh credentials in device secure storage, short-lived access credentials in memory where practical, and drafts in local storage with a deliberate privacy and deletion policy. Never put secrets in `EXPO_PUBLIC_*`, logs, or fixtures.

## First delivery slice

The current flow covers registration and code verification, login/logout, onboarding, journal list/detail/text editor, dashboard, date history, insights/reports, self-care goals/habits, and session management. The email-link flow needs separate Universal Links/App Links setup.

Use [skills/mylog-mobile-design/SKILL.md](skills/mylog-mobile-design/SKILL.md) for visual work and [../my-app-web/README.md](../my-app-web/README.md) for current web behavior. Verify actual backend controllers and tests before treating documentation as an implemented contract.
