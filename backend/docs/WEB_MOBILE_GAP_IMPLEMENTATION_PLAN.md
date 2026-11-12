# Kế hoạch thực thi: bù các tính năng còn thiếu trên web và mobile

> **Trạng thái:** đợt 1 (W1, M2, M4, M5) đã triển khai và kiểm thử với backend local ngày 2026-11-11, xem kết quả ở mục 9 của kế hoạch kiểm thử. Phần web đã xong (cập nhật 2026-11-12): W2 có card check-in và chọn hoạt động; W3 đã bật tô sáng ở backend và web; W4 theo phương án A (đã bỏ ô bio). Các hạng mục mobile của đợt 2–4 chưa làm. Lập ngày 2026-11-11.
> **Kế hoạch kiểm thử đi kèm:** [`WEB_MOBILE_GAP_TEST_PLAN.md`](WEB_MOBILE_GAP_TEST_PLAN.md).
> **Phạm vi:** hai nhóm việc. Nhóm 1 là các tính năng backend đã có API nhưng web chưa làm. Nhóm 2 là các tính năng mobile còn thiếu so với web và backend. Phần duyệt safety/AI/provider trước khi phát hành **không** nằm trong tài liệu này.

Danh sách được lập bằng cách đối chiếu controller backend với route `/api` và allowlist [`secured-proxy.ts`](../../front-end/my-app-web/features/auth/api/secured-proxy.ts) của web, cùng các endpoint mà `front-end/my-app-mobile/src` đang gọi. Code là nguồn sự thật. Một số ghi chú trong [`BACKEND_DEVELOPMENT_PLAN.md`](BACKEND_DEVELOPMENT_PLAN.md), ví dụ "frontend chưa tích hợp auth", và trong [`PROJECT_STATUS.md`](../../front-end/my-app-mobile/PROJECT_STATUS.md) của mobile đã cũ; mục 6 nêu phần cần cập nhật.

---

## 1. Tổng quan và thứ tự

| ID | Hạng mục | Nơi làm | Cần sửa backend? | Độ lớn | Ưu tiên |
|---|---|---|---|---|---|
| W1 | Quên / đặt lại mật khẩu | Web | Không | M | P0 |
| W2 | Check-in hằng ngày | Web | Không | M | P0 |
| W3 | Tô sáng chữ (highlight) | Backend + web | Có, nhỏ | S | P2 |
| W4 | Lời đề tựa (bio) trong hồ sơ | Web, có thể cả backend | Tùy quyết định | S/M | P2 |
| M1 | Đánh dấu nhãn đã gắn cho từng bài | Mobile | Không | S | P1 |
| M2 | Quên / đặt lại mật khẩu | Mobile | Không | M | P0 |
| M3 | Đổi mật khẩu | Mobile | Không | S | P1 |
| M4 | Xem kết quả phân tích AI | Mobile | Không | M | P0 |
| M5 | Màn hỗ trợ an toàn | Mobile | Không | M | P0 |
| M6 | Check-in hằng ngày | Mobile | Không | M | P1 |
| M7 | Trợ lý trang viết | Mobile | Không | L | P1 |
| M8 | Câu hỏi gợi ý | Mobile | Không | S | P2 |
| M9 | Rich text và chèn ảnh | Mobile | Không | L | P1 |
| M10 | Gửi phản hồi | Mobile | Không | S | P2 |
| M11 | Yêu cầu xóa tài khoản | Mobile | Không | M | P1 |
| M12 | Đăng nhập Google | Backend + mobile | **Có, đáng kể** | L | P2 |

**Thứ tự đề xuất**
1. **Đợt 1, bảo vệ tài khoản và an toàn:** W1, M2, M5, M4.
2. **Đợt 2, dữ liệu hằng ngày:** W2, M6, M1, M3.
3. **Đợt 3, trải nghiệm viết trên mobile:** M9, M7, M8.
4. **Đợt 4, hoàn thiện:** M11, M10, W3, W4, M12.

M5 phải xong **trước hoặc cùng lúc** với M4 và M7, vì cả hai luồng này đều có thể trả về trạng thái cần mở màn hỗ trợ an toàn.

**Nguyên tắc chung cho mọi hạng mục**
- Backend sở hữu business rule, authorization và safety. Client không tự phân loại rủi ro, không tự đặt hotline, không tự sinh phân tích.
- Không log nội dung nhật ký, ghi chú check-in, mật khẩu, token, mã xác minh hay raw response của AI. Test và ví dụ chỉ dùng dữ liệu synthetic.
- Web gọi backend qua route `/api` của Next.js; không gọi thẳng từ trình duyệt. Mobile dùng [`src/lib/api/client.ts`](../../front-end/my-app-mobile/src/lib/api/client.ts), lưu token trong SecureStore.
- Đặt code theo quy ước của từng app: web theo [`front-end/my-app-web/AGENTS.md`](../../front-end/my-app-web/AGENTS.md) (`features/<feature>/{api,components,hooks}`); mobile theo `src/features/<feature>` và route trong `src/app`.
- Mọi chuỗi giao diện web có đủ tiếng Việt và tiếng Anh trong `lib/i18n.ts`.

---

## 2. Nhóm 1: web

### W1. Quên / đặt lại mật khẩu (P0)

**Hiện trạng:** [`app/auth/forgot-password/page.tsx`](../../front-end/my-app-web/app/auth/forgot-password/page.tsx) chỉ hiện dòng "đang được hoàn thiện". Web chưa có route nào gọi `password-resets`.

**Contract backend** ([`IdentityController`](../src/main/java/com/mylog/identity/api/IdentityController.java), [`IdentityService`](../src/main/java/com/mylog/identity/application/IdentityService.java)):

| Endpoint | Body | Kết quả |
|---|---|---|
| `POST /api/v1/auth/password-resets` | `{ email }` | `202` và một thông điệp chung, **luôn giống nhau** dù email có tồn tại hay không |
| `POST /api/v1/auth/password-resets:confirm` | `{ token, newPassword }` | `204` |
| `POST /api/v1/auth/password-resets:confirm-code` | `{ email, code, newPassword }`; `code` gồm 8 chữ số | `204` |

- `newPassword` dài 12–72 ký tự. Trùng mật khẩu cũ thì backend trả lỗi `NEW_PASSWORD_SAME_AS_OLD`.
- Token có hạn 1 giờ, mã có hạn 15 phút. Yêu cầu mới làm hết hiệu lực token và mã cũ.
- Có giới hạn tần suất theo IP và theo email; vượt quá thì trả `429`.
- Đặt lại thành công sẽ **thu hồi mọi phiên đăng nhập** của tài khoản.
- Email gửi liên kết dạng `${MYLOG_PASSWORD_RESET_URL}#token=…`. Token nằm sau dấu `#`, nên không đi lên server qua request hay header `Referer`. Ở profile `local`/`test`, URL mặc định là `http://localhost:3000/auth/reset-password`.

**Việc cần làm**
1. **Route BFF** trong `app/api/auth/`, theo mẫu của [`verify/route.ts`](../../front-end/my-app-web/app/api/auth/verify/route.ts): kiểm tra `sameOrigin`, chuyển tiếp qua `authBackend`, trả lỗi bằng `forwardProblem`.
   - `password-resets/route.ts`
   - `password-resets/confirm/route.ts`
   - `password-resets/confirm-code/route.ts`
2. **Schema:** thêm `forgotPassword`, `resetWithToken`, `resetWithCode` vào [`features/auth/schemas/auth-forms.ts`](../../front-end/my-app-web/features/auth/schemas/auth-forms.ts). Dùng lại quy tắc mật khẩu của form đăng ký, có ô xác nhận mật khẩu.
3. **Trang `/auth/forgot-password`:** form email.
   - Gửi xong thì luôn hiện cùng một thông báo chung, kèm nút "Tôi đã có mã" dẫn tới `/auth/reset-password`.
   - **Điều chỉnh khi triển khai:** email được chuyển sang trang sau qua `sessionStorage` (hết hạn 20 phút, file `features/auth/password-reset-email.ts`), **không** qua query `?email=`, để email không nằm trong lịch sử trình duyệt và log.
   - Không tiết lộ email có tồn tại hay không.
4. **Trang mới `/auth/reset-password`** (client component, đặt trong `features/auth/components/ResetPasswordForm.tsx`):
   - Khi tải trang, đọc `window.location.hash`. Nếu có `token`, lưu vào state rồi xóa hash ngay bằng `history.replaceState`.
   - **Có token:** form mật khẩu mới và ô xác nhận, gửi tới `:confirm`.
   - **Không có token:** form email, mã 8 chữ số và mật khẩu mới, gửi tới `:confirm-code`. Ô mã có thể dùng lại component nhập mã của trang xác minh email.
   - **Thành công:** toast báo thành công, nói rõ mọi thiết bị đã bị đăng xuất, rồi `router.replace('/auth/login')`.
   - **Lỗi:** token hoặc mã sai/hết hạn thì gợi ý gửi lại. `429` thì báo thử lại sau. Trùng mật khẩu cũ thì báo riêng.
5. **Header:** thêm `/auth/reset-password` vào `next.config.ts` với `Referrer-Policy: no-referrer` và `Cache-Control: no-store`, giống `/auth/verify-email`.
6. **Giao diện:** dùng `AuthHeader` và phong cách card của trang đăng nhập/đăng ký hiện tại.
7. **i18n:** bỏ key `auth.forgotDescription` (đang ghi "đang được hoàn thiện"), thêm các key mới cho cả vi và en.
8. **Cấu hình:** `MYLOG_PASSWORD_RESET_URL` hiện chưa được ghi trong [`backend/README.md`](../README.md). Thêm vào phần biến môi trường: bắt buộc ở môi trường không phải local/test, là URL HTTPS trỏ tới trang web `/auth/reset-password`, không có query hay fragment.

### W2. Check-in hằng ngày (P0)

> **Đã làm:** `features/checkin/` với card "Check-in hôm nay" trên dashboard, kèm chip chọn hoạt động (`utils/checkin-activities.ts`, mobile nên dùng cùng danh sách mã). Contract `activities` (2026-11-12): **không gửi trường** thì backend giữ nguyên hoạt động đã lưu của ngày đó; gửi `[]` thì xóa. Web luôn gửi danh sách đầy đủ và giữ thời lượng/cường độ của các hoạt động vẫn được chọn. Phần dưới là kế hoạch ban đầu.

**Hiện trạng (lúc lập kế hoạch):** web không gọi `/api/v1/check-ins` ở đâu. Điểm áp lực, năng lượng và giấc ngủ trong trang viết được lưu **kèm từng bài nhật ký**, không phải check-in. Backend đã dùng check-in khi tính daily aggregate (`JpaDailyAggregateSource`), trong dashboard admin và trong bản xuất dữ liệu.

**Contract backend** ([`CheckinController`](../src/main/java/com/mylog/checkin/api/CheckinController.java), [`CheckinService`](../src/main/java/com/mylog/checkin/application/CheckinService.java)):

| Endpoint | Ghi chú |
|---|---|
| `PUT /api/v1/check-ins/{localDate}` | Upsert theo ngày, không cần `If-Match`. Trả về bản ghi và `ETag` |
| `GET /api/v1/check-ins/{localDate}` | `404` nếu ngày đó chưa check-in |
| `GET /api/v1/check-ins?from=&to=` | Khoảng tối đa 366 ngày, `from` không được sau `to` |

- **Body:** `timezone` (bắt buộc, IANA), `moodCode`, `moodScore`, `stressScore`, `energyScore`, `sleepMinutes`, `note`, `activities`.
- **Ràng buộc:**
  - Điểm số từ 1 đến 10, tối đa 1 chữ số thập phân.
  - `sleepMinutes` từ 0 đến 1440.
  - `note` tối đa 4000 ký tự.
  - `moodCode` khớp `[a-z0-9-]{1,32}`.
  - `activities` tối đa 20 mục, mỗi `code` là duy nhất, `intensity` thuộc `LOW|MODERATE|HIGH`.
  - `localDate` không được sau ngày mai.

**Việc cần làm**
1. **Allowlist:** thêm vào `secured-proxy.ts`.
   - `GET|PUT ^check-ins/\d{4}-\d{2}-\d{2}$`
   - `GET ^check-ins$`; query string đã được proxy chuyển tiếp.
2. **Feature mới `features/checkin/`:**
   - `api/client.ts` có `getCheckin(date)`, `putCheckin(date, input)`, `listCheckins(from, to)`. `404` khi lấy theo ngày được hiểu là "chưa check-in", không coi là lỗi.
   - `schemas/checkin.schema.ts` dùng zod, khớp đúng ràng buộc backend.
   - `hooks/useDailyCheckin.ts` dùng React Query, key `['checkin', userId, localDate]`. Lưu xong thì invalidate `['dashboard', …]` và `['insights', …]`.
   - `components/DailyCheckinCard.tsx`
3. **`localDate`:** tính theo `profile.timezone`, không theo múi giờ của trình duyệt. Gửi `timezone` cùng giá trị.
4. **`moodCode`:** **không** dùng id của `JournalEmotion`, vì `very_bad` có dấu gạch dưới, không khớp regex. Trường `mood` có sẵn trong `JOURNAL_EMOTIONS` (`calm-joy`, `anxiety-stress`, …) khớp regex, nhưng gộp `sad` và `very_bad` chung thành `sadness-reflect`, nên sẽ mất phân biệt. Đề xuất một bảng map riêng một-một, ví dụ `very_bad` → `very-bad`, và chốt bảng này cùng người phụ trách insight trước khi code.
5. **Giao diện:** card "Check-in hôm nay" trên dashboard.
   - Gồm chọn cảm xúc, ba thanh trượt Áp lực / Năng lượng / Giấc ngủ, và ô ghi chú ngắn tùy chọn.
   - Khi đã check-in thì hiện tóm tắt và nút "Sửa".
   - Bản đầu chưa làm `activities`.
6. **Không tự tạo check-in** từ số liệu của bài nhật ký. Chỉ tạo khi người dùng chủ động bấm lưu.

**Cần chốt với sản phẩm:** có muốn trang viết gợi ý "lưu các chỉ số này làm check-in hôm nay" không. Mặc định là không, để tránh ghi dữ liệu người dùng không chủ động gửi.

### W3. Tô sáng chữ (P2)

> **Đã làm:** backend nhận mark `highlight` (`TipTapContent.MARKS`), nút tô sáng trong `JournalBubbleMenu` đã bật. Mobile (M9) vẫn cần hiển thị mark này.

**Hiện trạng (lúc lập kế hoạch):** nút tô sáng trong [`JournalBubbleMenu.tsx`](../../front-end/my-app-web/features/journal/components/JournalBubbleMenu.tsx) bị tắt. Backend chỉ chấp nhận các mark `bold, italic, underline, strike, code` ([`TipTapContent.java:18`](../src/main/java/com/mylog/journal/application/TipTapContent.java)).

**Việc cần làm**
1. **Backend:**
   - Thêm `highlight` vào `MARKS`, **không cho thuộc tính màu**: mark có `attrs` thì từ chối.
   - Thêm unit test trong `src/test/java/com/mylog/journal`.
   - Kiểm tra bản xuất PDF/CSV và phần trích plain text không bị ảnh hưởng.
   - Không cần migration, vì nội dung được lưu dưới dạng JSON đã mã hóa.
2. **Web:** bật extension `Highlight` của TipTap (một màu) và bật nút trong `JournalBubbleMenu`. `JournalEditorToolbar.tsx` hiện không được dùng ở đâu; có thể xóa trong một thay đổi riêng.
3. **Mobile:** renderer ở M9 phải hiển thị được mark này.

### W4. Lời đề tựa (bio) trong hồ sơ (P2)

> **Quyết định:** phương án A, ô bio đã được bỏ khỏi `SettingsView`. Chỉ làm phương án B khi sản phẩm xác nhận cần bio.

**Hiện trạng (lúc lập kế hoạch):** [`SettingsView.tsx`](../../front-end/my-app-web/features/user/components/SettingsView.tsx) có ô bio, nhưng [`UpdateProfileRequest`](../src/main/java/com/mylog/user/api/request/UpdateProfileRequest.java) không có trường này.

**Cần chốt một trong hai phương án**
- **A (đề xuất trước mắt):** bỏ ô bio khỏi giao diện cho tới khi sản phẩm xác nhận cần. Chỉ sửa web.
- **B:** thêm bio vào backend.
  - Tạo migration mới `V22__…` (không sửa migration đã áp dụng), cột được mã hóa theo ADR-0002 vì là văn bản cá nhân.
  - Thêm trường vào `UpdateProfileRequest`/response, cập nhật bản xuất dữ liệu và luồng xóa tài khoản, cập nhật OpenAPI.
  - Web và mobile hiển thị và lưu với `If-Match`.

---

## 3. Nhóm 2: mobile

### M0. Nền tảng chung (làm trước M4, M5, M7, M9)

- **Thư viện:** cài qua `npx expo install` để khớp SDK 57.
  - `expo-image-picker` cho M9.
  - Thư viện editor dựa trên WebView chạy TipTap, nếu chọn hướng B của M9.
  - `expo-auth-session` và `expo-crypto` cho M12.
- **API client:** bổ sung hỗ trợ `multipart/form-data` cho M9; giữ nguyên cơ chế refresh token.
- **Hiển thị lỗi:** tạo hàm dùng chung cho `ProblemDetail` của backend, gồm `429`, `409` và lỗi mạng. Không hiện chi tiết kỹ thuật cho người dùng.
- **Cập nhật [`API_CONTRACT.md`](../../front-end/my-app-mobile/API_CONTRACT.md):** mỗi endpoint mới được dùng phải có dòng mô tả trong file này.

### M1. Đánh dấu nhãn đã gắn cho từng bài (P1)

- `PROJECT_STATUS.md` ghi backend chưa trả nhãn theo từng bài. Thực tế backend **đã có** `GET /api/v1/journal-entries/{entryId}/tags`.
- Trong `src/features/journal/api.ts`, thêm `getEntryTags(entryId)`.
- Trên màn chi tiết và editor, đánh dấu nhãn đã gắn. Gắn/bỏ nhãn xong thì cập nhật state lạc quan và hoàn tác nếu lỗi.

### M2. Quên / đặt lại mật khẩu (P0)

- Dùng chung contract với W1.
- **Màn mới:**
  - `src/app/(auth)/forgot-password.tsx`: nhập email, sau đó luôn hiện thông báo chung.
  - `src/app/(auth)/reset-password.tsx`: nhập email, mã 8 chữ số, mật khẩu mới và ô xác nhận, gửi tới `:confirm-code`.
- **Liên kết trong email** mở trang web `/auth/reset-password`. Bản đầu **không** mở trong app; muốn mở trong app phải cấu hình Universal Links / App Links, việc này đang để mở cùng luồng email link.
- Thành công thì xóa token trong SecureStore (backend đã thu hồi mọi phiên) và quay về màn đăng nhập.
- Thêm link "Quên mật khẩu?" trên `login.tsx`.
- **Đã triển khai:** email đã nhập được chuyển giữa các màn bằng biến trong bộ nhớ (`src/features/auth/password-reset.ts`), không qua route param, vì trên Expo web route param hiện lên thanh địa chỉ. `ApiError` của mobile có thêm trường `detail` để đọc mã lỗi ổn định của backend.

### M3. Đổi mật khẩu (P1)

- `POST /api/v1/me/password` với `{ currentPassword, newPassword }` ([`ChangePasswordRequest`](../src/main/java/com/mylog/user/api/request/ChangePasswordRequest.java)).
- Thêm form vào `settings.tsx`, theo luồng và thông báo lỗi của [`ChangePasswordForm.tsx`](../../front-end/my-app-web/features/user/components/ChangePasswordForm.tsx) trên web.
- Ẩn form khi `profile.canChangePassword === false`, tức tài khoản chỉ đăng nhập bằng Google.
- Đọc `IdentityService`/`UserProfileUseCase` để xác nhận đổi mật khẩu có thu hồi phiên khác hay không, rồi hiện thông báo tương ứng.

### M4. Xem kết quả phân tích AI (P0)

**Hiện trạng:** màn chi tiết chỉ in chuỗi `Phân tích: {analysisStatus}`.

- **Đọc kết quả:** `GET /api/v1/journal-entries/{id}/analysis` trả `status`, `sentiment`, `sentimentScore`, `emotions`, `topics`, `reflection`, `contentVersion`.
- **Thử lại:** `POST /api/v1/journal-entries/{id}/analysis:retry`.
- **Polling:** chạy khi trạng thái đang chờ, với backoff 2s → 4s → 8s → 16s → 30s. Dừng sau khoảng 2 phút, rồi hiện "Phân tích đang mất nhiều thời gian hơn, quay lại sau". Không có spinner chạy mãi.
- **Tách ba trạng thái:**
  - Đang phân tích.
  - Không phân tích được: có nút Thử lại.
  - Không phân tích vì lý do an toàn: không có nút Thử lại; nếu `riskLevel` là HIGH/CRITICAL thì mở M5.
  - `AnalysisResponse.status` là cột `journal_entries.analysis_status` (ràng buộc trong migration V5): `NOT_REQUESTED`, `PENDING`, `ANALYZING`, `ANALYZED`, `ANALYSIS_FAILED`, `ANALYSIS_OUTDATED`, `BLOCKED_BY_SAFETY`. Chỉ `ANALYZED` mới có nội dung. Đang chờ: `PENDING`, `ANALYZING`, `ANALYSIS_OUTDATED`. Có nút thử lại: `ANALYSIS_FAILED`, `NOT_REQUESTED`.
  - `emotions`/`topics` là mã (ví dụ `CALM`, `WORK`) theo allowlist của `AnalysisOutputValidator`; client hiển thị nhãn tiếng Việt và bỏ qua mã lạ.
- **Hiển thị:** `reflection` dạng đoạn văn, `emotions`/`topics` dạng chip.
- **Không hiển thị:** `sentimentScore`, `analysisId` hay metadata kỹ thuật (theo mục 32 của `AGENTS.md` web).
- **Kiểu dữ liệu:** thêm type vào `src/types` và hàm vào `src/features/journal/api.ts`.

### M5. Màn hỗ trợ an toàn (P0)

- **Khi nào mở:** khi lưu bài mà response có `riskLevel` là `HIGH` hoặc `CRITICAL`; khi M4 hoặc M7 trả trạng thái safety. Logic giống [`useJournalEditor.ts`](../../front-end/my-app-web/features/journal/hooks/useJournalEditor.ts) trên web.
- **Nguồn hỗ trợ:** `GET /api/v1/safety/resources?locale=vi-VN&country=VN` (endpoint công khai). Backend chỉ trả nguồn đã duyệt và đã xác minh.
- **Khi chưa có nguồn nào được duyệt** (tình trạng hiện tại của DB):
  - Hiện nội dung chung đã thống nhất với web.
  - **Không hard-code số điện thoại** trong app.
  - Cuộc gọi chỉ thực hiện khi người dùng bấm vào số do backend trả về.
- **Màn hình:** modal toàn màn hình, nút đóng rõ ràng, đọc được bằng screen reader. Bài viết vẫn được lưu.

### M6. Check-in hằng ngày (P1)

- Dùng chung contract, quy tắc `localDate` theo `profile.timezone` và bảng map `moodCode` đã chốt ở W2.
- Đặt card check-in trên `dashboard.tsx`/`home.tsx`. Lưu xong thì làm mới dữ liệu dashboard.
- Thêm hàm vào `src/features/overview/api.ts` hoặc tạo `src/features/checkin/` nếu file trở nên lớn.

### M7. Trợ lý trang viết (P1)

- **Endpoint:**
  - `POST /api/v1/journal-writing-suggestions`
  - `GET /api/v1/companion/title-suggestions`
  - `POST /api/v1/companion/habit-suggestions/dismiss`
  - Consent `AI_PROCESSING` qua `/me/consents`.
- **Trạng thái và luồng:** theo [`COMPANION_UI_PLAN.md`](../../front-end/my-app-web/COMPANION_UI_PLAN.md) mục 2.2 và 2.4, gồm consent, tạm nghỉ, bị chặn, `SAFETY_FLOW` (mở M5) và khép lại.
- **Giao diện:** bottom sheet mở từ editor; thu nhỏ vẫn giữ hội thoại; không chặn bàn phím của editor.
- **Hành động:**
  - Chèn câu hỏi gợi ý cần M9 (node `promptQuestion`). Nếu M9 chưa xong thì tạm chèn dạng đoạn văn thường.
  - Thở chậm 2 phút chạy trên máy, không gọi API.
  - Âm thanh dịu: hoãn sang sau nếu chưa có thư viện âm thanh phù hợp.
- **Riêng tư:** không lưu nội dung chat ra bộ nhớ máy.

### M8. Câu hỏi gợi ý (P2)

- `GET /api/v1/recommendations` và `POST /api/v1/recommendations/questions`.
- Danh sách câu hỏi tự vấn, có nút chèn vào bài. Không thay thế màn safety.

### M9. Rich text và chèn ảnh (P1)

**Hiện trạng:** editor mobile chỉ có văn bản thô; màn chi tiết in plain text từ `contentJson`.

Backend chỉ chấp nhận schema TipTap đã allowlist: `paragraph`, `heading`, `blockquote`, `promptQuestion`, danh sách, `image` có `assetId`, cùng các mark ở W3. Mobile **phải** sinh JSON đúng schema này.

- **Bước A, xem nội dung:** renderer native đọc `contentJson` và hiển thị đủ các node/mark trên. Ảnh lấy URL qua `POST /api/v1/journal-assets/urls`. Gặp node lạ thì bỏ qua, không làm crash.
- **Bước B, soạn thảo:** chọn một trong hai hướng.
  - **Đề xuất:** editor TipTap chạy trong WebView, giữ chung schema với web.
  - **Thay thế:** toolbar native giới hạn ở đậm, nghiêng và danh sách.
  - Chốt bằng một spike khoảng một ngày, đo bàn phím, IME tiếng Việt và hiệu năng trên Android tầm trung.
- **Ảnh:**
  - Dùng `expo-image-picker`, nén ảnh trước khi gửi.
  - Upload: `POST /api/v1/journal-assets/upload` (multipart, field `file`), rồi chèn node `image` với `assetId`.
  - Ảnh được upload nhưng bài chưa lưu thì backend để ở trạng thái pending (V20).
- **Bản nháp:** bản nháp trong SecureStore lưu JSON thay vì text. Cần đường chuyển đổi cho bản nháp cũ.

### M10. Gửi phản hồi (P2)

- `POST /api/v1/feedback` với `{ category, message }`, trong đó `category` thuộc `BUG|IDEA|OTHER`.
- Form trong `settings.tsx`. Có cảnh báo ngắn: không nên dán nội dung nhật ký vào phản hồi.

### M11. Yêu cầu xóa tài khoản (P1)

- **Yêu cầu xóa:** `POST /api/v1/account-deletion-requests` với `{ password }`. Có thời gian chờ 7 ngày, phiên bị thu hồi ngay.
- **Hủy hoặc xem trạng thái:** `POST /{id}:cancel` và `POST /{id}:status` với `{ email, password }`. Hai endpoint này công khai (không cần đăng nhập).
- **Luồng:** màn xác nhận hai bước, nêu rõ hậu quả và thời gian chờ. Thành công thì xóa token khỏi SecureStore và về màn đăng nhập, kèm hướng dẫn cách hủy trong 7 ngày.
- Đối chiếu luồng web ở `features/user` để dùng cùng thông điệp.

### M12. Đăng nhập Google (P2, cần sửa backend trước)

**Vướng mắc:** [`GoogleOAuthClient`](../src/main/java/com/mylog/identity/infrastructure/GoogleOAuthClient.java) đổi code với `redirect_uri=postmessage` (luồng popup của web) và chỉ kiểm tra một `client_id`. App native dùng client ID riêng cho Android/iOS và redirect URI theo scheme của app, có PKCE. Vì vậy mobile chưa thể dùng endpoint hiện tại.

1. **Backend:**
   - Ghi quyết định vào ADR-0001 hoặc tạo ADR mới.
   - Mở rộng `POST /api/v1/auth/google` để nhận thêm `platform`, `redirectUri` (thuộc allowlist), `codeVerifier`.
   - Cấu hình nhiều audience (web, Android, iOS) và kiểm tra `aud` theo platform.
   - Không nhận ID token từ client nếu chưa xác minh chữ ký, `aud` và `iss`.
   - Viết test với token giả lập.
2. **Mobile:** dùng `expo-auth-session` với PKCE; kết quả trả về được xử lý giống đăng nhập bằng email.
3. **Vận hành:** đăng ký client ID Android (SHA-1 của bản ký) và iOS (bundle ID). Không commit secret vào repo.

---

## 4. Rủi ro và phụ thuộc

| Rủi ro | Ảnh hưởng | Cách giảm |
|---|---|---|
| Chưa có nguồn hỗ trợ an toàn nào được duyệt | M5 chỉ hiện nội dung chung | Phần duyệt nội dung là việc riêng, không chặn việc code M5 |
| Editor WebView trên mobile có độ trễ, lỗi IME tiếng Việt | Trải nghiệm viết kém | Spike trước khi cam kết hướng B; có phương án toolbar native |
| Bảng map `moodCode` giữa web, mobile và insight không thống nhất | Insight tính sai | Chốt bảng map một lần, ghi vào contract trước khi code W2/M6 |
| Email link đặt lại mật khẩu không mở được app | Người dùng mobile phải qua web | Bản đầu dùng mã 8 chữ số; Universal Links làm sau |
| Thay đổi contract Google ảnh hưởng web | Web không đăng nhập được | Giữ tương thích ngược: thiếu `platform` thì xử lý như web |

## 5. Ngoài phạm vi

- Duyệt policy safety, classifier, provider AI, nguồn hỗ trợ thật và các release gate M8.
- Đăng nhập Apple, thông báo đẩy, chế độ offline đầy đủ cho mobile.
- Thư gửi tương lai, Micro-Wins Vault, bản chuẩn bị buổi tham vấn ([`SELF_COMPASSION_FEATURE_PROPOSALS.md`](SELF_COMPASSION_FEATURE_PROPOSALS.md)).

## 6. Tài liệu cần cập nhật khi làm xong từng hạng mục

- [`BACKEND_DEVELOPMENT_PLAN.md`](BACKEND_DEVELOPMENT_PLAN.md): sửa các ghi chú "frontend chưa tích hợp" đã cũ ở M1, M2, M5.
- [`PROJECT_STATUS.md`](../../front-end/my-app-mobile/PROJECT_STATUS.md) và [`API_CONTRACT.md`](../../front-end/my-app-mobile/API_CONTRACT.md) của mobile.
- [`COMPANION_UI_PLAN.md`](../../front-end/my-app-web/COMPANION_UI_PLAN.md): thêm phần mobile khi làm M7.
- OpenAPI artifact của backend nếu W3, W4 (phương án B) hoặc M12 đổi contract.
- [`backend/README.md`](../README.md): biến môi trường `MYLOG_PASSWORD_RESET_URL` và cấu hình Google cho mobile.
