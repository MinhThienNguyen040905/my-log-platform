# Mobile API contract inventory — phase 0

Verified against the current backend controllers and request/response records on 2026-10-13. Native mobile calls `${EXPO_PUBLIC_API_BASE_URL}/api/v1` directly; the Next.js `/api/*` routes belong to the web app. Auth, profile, onboarding, journal, dashboard, insight/report, self-care and session calls are implemented in source. The synthetic local API smoke test covers core endpoints; native device behavior remains unverified.

## Auth — `/api/v1/auth`

| Operation | Request | Success | Notes |
| --- | --- | --- | --- |
| `POST /register` | `email`, `password` (12–128 characters), IANA `timezone`, `locale`, `termsVersion`, `privacyVersion`, `acceptTerms=true`, `acceptPrivacy=true` | `202` `{registrationId, ...}` | Pending registration only; no session. |
| `POST /email-verifications` | `{email}` | `202` | Resend. |
| `POST /email-verifications:confirm-code` | `{email, code}` where code has 8 digits | `204` | Verify by code. |
| `POST /email-verifications:confirm` | `{token}` | `204` | Verify by link, user confirmation required. |
| `POST /login` | `{email, password, deviceName?}` | `200` `{userId, accessToken, refreshToken, expiresInSeconds}` | Tokens are returned in JSON for native storage. |
| `POST /refresh` | `{refreshToken}` | `200` same token shape | Refresh token rotates; replace stored token atomically. |
| `POST /logout` | Bearer access token | `204` | Clear local credentials even when network logout cannot complete. |

## Account — `/api/v1/me` (Bearer)

| Operation | Request | Success | Notes |
| --- | --- | --- | --- |
| `GET /me` | — | `200` profile and `ETag` | Profile includes `userId`, names, goals, timezone, locale, `onboardingCompletedAt`, version. |
| `PATCH /me` | Profile fields plus `If-Match` version | `200` updated profile/ETag | Handle version conflict without silent overwrite. |
| `POST /me/onboarding:complete` | `If-Match` version | `200` updated profile/ETag | No JSON body. |
| `GET /me/consents` | — | `200` consent array | Each item has type, document version, granted state and time. |
| `PUT /me/consents/{type}` | `{documentVersion, granted}` | `204` | Optional consent UI must follow published documents. |
| `GET /me/sessions` | — | `200` session array | Account management slice. |
| `DELETE /me/sessions/{sessionId}` | — | `204` | Account management slice. |
| `DELETE /me/sessions?exceptCurrent=true` | — | `204` | Required query flag. |

## Journal — `/api/v1` (Bearer)

| Operation | Request | Success | Notes |
| --- | --- | --- | --- |
| `GET /journal-entries` | Optional `from`, `to`, `tag`, `favorite`, `cursor`, `limit` | `200` `{items, nextCursor}` | `limit` 1–100 per backend README. |
| `GET /journal-entries/{id}` | — | `200` detail and `ETag` | Detail contains `contentJson`, `rowVersion`, status and safety fields. |
| `POST /journal-entries` | `Idempotency-Key`; `contentJson`, `timezone`, optional title, time, mood and metrics | `201` detail and `ETag` | JSON must match backend allowlist. |
| `PATCH /journal-entries/{id}` | `If-Match` plus changed fields | `200` detail and `ETag` | Conflict is reported to the user. |
| `DELETE /journal-entries/{id}` | `If-Match` | `204` | Version required. |
| `PUT/DELETE /journal-entries/{id}/favorite` | — | `200` detail and `ETag` | Toggle favorite. |
| `GET/POST /journal-tags` | POST `{name, color?}` | `200` list / `201` tag | Tag metadata. |
| `PUT/DELETE /journal-entries/{id}/tags/{tagId}` | — | `204` | Attach/detach. |

Backend journal create/update DTOs accept `title`, `contentJson`, `location`, `occurredAt`, `timezone`, `moodCode`, `moodScore`, `stressScore`, `energyScore`, and `sleepMinutes`; create requires `contentJson` and `timezone`. The current mobile editor sends plain paragraph Tiptap JSON. Web's editor, storage, and proxy code are not shared with native. Journal detail does not expose attached tag IDs, so the mobile UI cannot display each tag's attachment state yet.

## Overview, self-care and data export — `/api/v1` (Bearer)

| Operation | Request | Success | Mobile use |
| --- | --- | --- | --- |
| `GET /dashboard` | `range=7d\|30d\|90d` | `200` totals and daily timeline | Dashboard shows only returned metrics. |
| `GET /insights`, `GET /reports` | `cursor`, `limit` | `200` `{items,nextCursor}` | Empty state when backend has no result. |
| `GET /reports/{id}` | — | `200` report detail | Detail screen. |
| `GET/POST /self-care/goals` | POST `{category,title}` | `200` list / `201` goal | Category is one of `SLEEP`, `MINDFULNESS`, `EXERCISE`, `SOCIAL`, `CUSTOM`. |
| `POST /self-care/goals/{id}/habits` | `{title,targetValue,unit,frequencyType}` | `201` habit | Current mobile UI creates daily habits with target 1. |
| `PUT/DELETE /self-care/habits/{id}/completions/{localDate}` | PUT `{value}` | `200`/`204` | Uses profile timezone for the local date. |
| `POST /exports`, `GET /exports/{id}` | POST `{format: CSV\|PDF}` | `202`/`200` export status | Last export ID is kept per user. |
| `POST /exports/{id}:authorize-download` | `{password}` | `200` short-lived signed relative URL | Password is used only for reauthentication. |
| `GET /exports/{id}/file` | Signed `expires`, `signature` plus bearer token | `200` file | File is shared from temporary cache on native and removed after sharing. |

The account deletion API exists on the backend but is not exposed in mobile yet. It needs a reviewed request/status/cancel flow because requesting deletion disables the account before the scheduled purge.

## Development connection

- Set `EXPO_PUBLIC_API_BASE_URL` to a backend URL reachable **from the device**, without `/api/v1`. `localhost` on a physical phone is the phone itself. Android emulator, iOS simulator, and a phone may need different hostnames. Never put tokens or service keys in this public variable.
- Run the backend local profile and verify `/actuator/health` from the same device/network before API integration. The development host was not reachable during this phase unless separately noted in the implementation report.
- Real user data requires HTTPS. HTTP is only for isolated local development with synthetic accounts and journal content.

Sources: backend `IdentityController`, `UserController`, `JournalController`, `JournalTagController` and their request/response records; `backend/README.md` for validation and paging constraints.
