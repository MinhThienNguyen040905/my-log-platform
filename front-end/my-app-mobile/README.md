# MyLog mobile

Ứng dụng Expo SDK 57 / React Native cho MyLog. Auth, onboarding, hồ sơ, nhật ký và một số luồng mở rộng đã được nối với backend. Xem [PROJECT_STATUS.md](PROJECT_STATUS.md) để biết phần đã kiểm tra và phần còn mở.

Read [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) for delivery phases, [STRUCTURE.md](STRUCTURE.md) for code ownership, [API_CONTRACT.md](API_CONTRACT.md) for the verified backend endpoints, and [mobile design skill](skills/mylog-mobile-design/SKILL.md) for UI direction.

## Run locally

```powershell
cd front-end/my-app-mobile
npm ci
Copy-Item .env.example .env
npm run start
```

Set `EXPO_PUBLIC_API_BASE_URL` in `.env` to a backend address reachable from your emulator or phone, without `/api/v1`. `127.0.0.1` works only when the app runtime can reach the backend on the same host; a physical phone normally needs the computer's LAN address or a development HTTPS endpoint. Do not place tokens or secret keys in it. Native refresh tokens and user-scoped journal drafts use Expo SecureStore; Expo web sessions and drafts live only in memory. HTTP is for isolated local testing with synthetic data; real user data requires HTTPS.

## Checks

```powershell
./node_modules/.bin/tsc.cmd --noEmit
$env:EXPO_NO_TELEMETRY='1'
./node_modules/.bin/expo.cmd lint
$env:EXPO_NO_TELEMETRY='1'
$env:EXPO_OFFLINE='1'
./node_modules/.bin/expo.cmd export --platform web --output-dir .mobile-check-web
./node_modules/.bin/expo.cmd export --platform android --output-dir .mobile-check-android
```

With a local backend and Mailpit, run `node scripts/e2e-api-smoke.mjs`. With Expo web on port 3000 and a backend on `http://127.0.0.1:18080`, run `node scripts/e2e-expo-web.mjs`. Both use synthetic accounts and local HTTP only. Web/Android exports and Playwright web tests do not replace native device checks.
