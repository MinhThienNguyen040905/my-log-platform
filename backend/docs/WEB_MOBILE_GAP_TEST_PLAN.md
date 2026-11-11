# Kế hoạch kiểm thử: bù các tính năng còn thiếu trên web và mobile

> **Trạng thái:** kế hoạch, chưa chạy. Lập ngày 2026-11-11.
> **Đi kèm:** [`WEB_MOBILE_GAP_IMPLEMENTATION_PLAN.md`](WEB_MOBILE_GAP_IMPLEMENTATION_PLAN.md). Mã hạng mục (W1–W4, M1–M12) dùng chung giữa hai tài liệu.

---

## 1. Mục tiêu

- Chứng minh mỗi hạng mục chạy đúng **với backend thật ở local**, không chỉ với mock.
- Kiểm tra các nhánh lỗi quan trọng: hết hạn, sai mã, `429`, `404`, `409`, mất mạng.
- Kiểm tra quyền riêng tư: không lộ token, mật khẩu, mã xác minh hay nội dung nhật ký qua log, URL, header `Referer` hoặc bộ nhớ máy.
- Kiểm tra safety: client mở đúng màn hỗ trợ khi backend báo rủi ro, và không tự đặt nguồn hỗ trợ.

## 2. Môi trường và dữ liệu

| Thành phần | Cách chạy |
|---|---|
| Backend | Profile `local`, PostgreSQL, Redis, Mailpit theo [`backend/README.md`](../README.md) |
| Test tích hợp backend | JUnit với PostgreSQL Testcontainers (cần Docker) |
| Web | `npm run dev` ở `front-end/my-app-web`; Playwright qua `npm run test:e2e` |
| Mobile, tự động | `node scripts/e2e-api-smoke.mjs` và `node scripts/e2e-expo-web.mjs` ở `front-end/my-app-mobile` |
| Mobile, thủ công | Ít nhất 1 thiết bị hoặc emulator Android tầm trung và 1 iOS simulator, chạy bản development build |

**Dữ liệu**
- Chỉ dùng tài khoản và nội dung **synthetic** (ví dụ `qa+reset01@example.test`). Không dùng email, nhật ký hay dữ liệu sức khỏe tinh thần thật.
- Tài khoản Playwright đặt qua `PLAYWRIGHT_TEST_EMAIL` / `PLAYWRIGHT_TEST_PASSWORD`, không commit vào repo.
- Đọc email (liên kết, mã) qua Mailpit local.
- Để kiểm tra safety, dùng câu kích hoạt synthetic đã có trong corpus test của backend. Không viết câu mới mô tả tự hại chi tiết.
- `NEXT_PUBLIC_COMPANION_MOCK=1` chỉ dùng khi kiểm thử giao diện trợ lý; kết luận cuối phải chạy với backend thật.

## 3. Mức kiểm thử

| Mức | Áp dụng cho | Công cụ |
|---|---|---|
| Unit / tích hợp backend | W3, W4 (phương án B), M12 | JUnit, Testcontainers trong `backend/src/test/java/com/mylog/...` |
| Kiểm tra tĩnh | Mọi thay đổi web/mobile | `npx tsc --noEmit`, `npm run lint` |
| E2E web | W1–W4 | Playwright trong `front-end/my-app-web/tests/` |
| API smoke mobile | M1–M11 | Mở rộng `scripts/e2e-api-smoke.mjs` |
| UI mobile trên Expo web | M1–M11, phần không phụ thuộc native | Mở rộng `scripts/e2e-expo-web.mjs` |
| Thủ công trên thiết bị | M5, M7, M9, M11, M12 và mọi màn có bàn phím | Checklist ở mục 6 |

Kết quả Playwright trên Expo web **không** đủ để kết luận app Android/iOS đã sẵn sàng.

---

## 4. Ca kiểm thử nhóm 1: web

Cột "Tự động" ghi file test đề xuất. "Thủ công" nghĩa là chạy tay và ghi kết quả theo mẫu ở mục 8.

### W1. Quên / đặt lại mật khẩu

File đề xuất: `tests/password-reset.spec.ts`.

| ID | Kịch bản | Kết quả mong đợi | Tự động |
|---|---|---|---|
| W1-01 | Gửi yêu cầu với email đã đăng ký | Hiện thông báo chung; Mailpit nhận 1 email có liên kết `#token=` và mã 8 chữ số | Có |
| W1-02 | Gửi yêu cầu với email chưa đăng ký | **Cùng** thông báo, cùng mã HTTP `202` như W1-01; không có email nào được gửi | Có |
| W1-03 | Mở liên kết trong email | Trang `/auth/reset-password` hiện form mật khẩu mới; thanh địa chỉ **không còn** `#token` sau khi tải | Có |
| W1-04 | Đặt mật khẩu mới hợp lệ qua liên kết | Thành công, chuyển về đăng nhập; đăng nhập bằng mật khẩu mới được, mật khẩu cũ bị từ chối | Có |
| W1-05 | Đặt lại bằng email + mã 8 chữ số | Thành công giống W1-04 | Có |
| W1-06 | Mã sai, rồi mã có ít hơn 8 chữ số | Mã sai thì báo lỗi chung; mã thiếu chữ số thì bị chặn ngay ở client | Có |
| W1-07 | Dùng lại liên kết đã dùng thành công | Báo liên kết không còn hiệu lực, gợi ý gửi lại | Có |
| W1-08 | Yêu cầu 2 lần liên tiếp rồi dùng liên kết/mã của lần đầu | Lần đầu bị từ chối; lần sau dùng được | Có |
| W1-09 | Mật khẩu mới trùng mật khẩu cũ | Báo lỗi riêng `NEW_PASSWORD_SAME_AS_OLD` | Có |
| W1-10 | Mật khẩu dưới 12 ký tự, hoặc ô xác nhận không khớp | Bị chặn ở client, không gửi request | Có |
| W1-11 | Gửi yêu cầu vượt giới hạn tần suất | Nhận `429`, giao diện báo thử lại sau, không crash | Có, hoặc thủ công nếu giới hạn quá cao |
| W1-12 | Đang đăng nhập ở trình duyệt B, đặt lại mật khẩu ở trình duyệt A | Trình duyệt B bị đăng xuất ở request kế tiếp | Có (2 context) |
| W1-13 | Token hết hạn (sau 1 giờ) / mã hết hạn (sau 15 phút) | Báo hết hạn | Thủ công, hoặc test backend dùng clock giả |
| W1-14 | Bảo mật trang reset | Response có `Referrer-Policy: no-referrer` và `Cache-Control: no-store`; log server Next.js và backend không chứa token, mã hay mật khẩu | Có (header) + thủ công (log) |
| W1-15 | Chuyển ngôn ngữ sang tiếng Anh | Mọi chữ trên 2 trang có bản tiếng Anh, không lộ key i18n | Thủ công |
| W1-16 | Bàn phím và screen reader | Tab đúng thứ tự; lỗi được đọc qua `role="alert"` | Thủ công |

### W2. Check-in hằng ngày

File đề xuất: `tests/daily-checkin.spec.ts`.

| ID | Kịch bản | Kết quả mong đợi | Tự động |
|---|---|---|---|
| W2-01 | Tài khoản mới mở dashboard | Card check-in ở trạng thái "chưa check-in"; `404` từ backend **không** hiện thành lỗi | Có |
| W2-02 | Lưu check-in với cảm xúc, 3 chỉ số và ghi chú | `PUT` trả `200`; card chuyển sang tóm tắt; tải lại trang vẫn còn | Có |
| W2-03 | Sửa check-in cùng ngày | Cùng một bản ghi được cập nhật (upsert), `version` tăng, không tạo bản ghi thứ hai | Có |
| W2-04 | Giá trị biên: điểm 1 và 10, ngủ 0 và 24 giờ | Lưu được | Có |
| W2-05 | Giá trị sai: điểm 0 hoặc 11, ghi chú quá 4000 ký tự | Bị chặn ở client; nếu gọi thẳng API thì backend trả `400` | Có |
| W2-06 | `moodCode` gửi đi | Đúng bảng map đã chốt, khớp `[a-z0-9-]`, không có `very_bad` | Có (kiểm tra request) |
| W2-07 | Hồ sơ có timezone `Asia/Ho_Chi_Minh`, máy tính đặt múi giờ khác, giờ sát nửa đêm | `localDate` tính theo timezone của hồ sơ | Có (giả lập `timezoneId` và clock của Playwright) |
| W2-08 | Sau khi lưu | Dashboard và insight được làm mới, không phải tải lại trang | Có |
| W2-09 | Mất mạng khi lưu | Báo lỗi, giữ nguyên dữ liệu trong form để thử lại | Có (`context.setOffline`) |
| W2-10 | Lưu một bài nhật ký có chỉ số | **Không** tự tạo check-in | Có |
| W2-11 | Bản xuất dữ liệu CSV | Có check-in vừa tạo | Thủ công |

### W3. Tô sáng chữ

| ID | Kịch bản | Kết quả mong đợi | Tự động |
|---|---|---|---|
| W3-01 | Backend: lưu nội dung có mark `highlight` | Chấp nhận | JUnit |
| W3-02 | Backend: `highlight` có `attrs` (ví dụ màu), hoặc mark lạ | Từ chối `400`, giống quy tắc allowlist hiện tại | JUnit |
| W3-03 | Backend: trích plain text và bản xuất PDF/CSV có highlight | Không lỗi; chữ được giữ nguyên | JUnit |
| W3-04 | Web: bôi đen chữ, bấm tô sáng, lưu, mở lại bài | Highlight còn nguyên | Playwright |
| W3-05 | Mobile: xem bài có highlight | Hiển thị được (cần M9 bước A) | Thủ công |

### W4. Lời đề tựa (bio)

- **Phương án A:** ô bio không còn trên trang cài đặt; không còn chữ "chưa được hỗ trợ". Kiểm tra bằng Playwright.
- **Phương án B:**
  - JUnit: migration `V22` chạy trên Testcontainers; cột bio được mã hóa (đọc thẳng DB không thấy plaintext).
  - Cập nhật bio với `If-Match` đúng thì `200`, sai version thì `409`.
  - Bio có trong bản xuất dữ liệu và bị xóa khi xóa tài khoản.
  - Web: lưu bio rồi tải lại vẫn còn.

---

## 5. Ca kiểm thử nhóm 2: mobile

Mỗi hạng mục cần: (a) bước mới trong `e2e-api-smoke.mjs` để kiểm tra contract, (b) bước mới trong `e2e-expo-web.mjs` nếu chạy được trên web, (c) các dòng tương ứng trong checklist thiết bị ở mục 6.

### M1. Nhãn đã gắn cho từng bài

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| M1-01 | Gắn 2 nhãn cho một bài, mở lại bài | 2 nhãn được đánh dấu đã chọn |
| M1-02 | Bỏ 1 nhãn | Cập nhật ngay; tải lại vẫn đúng |
| M1-03 | Backend trả lỗi khi gắn nhãn | Giao diện hoàn tác về trạng thái cũ và báo lỗi |
| M1-04 | Nhãn gắn trên web, mở trên mobile | Hiển thị đúng |

### M2. Quên / đặt lại mật khẩu

Chạy lại các ca W1-01, W1-02, W1-05, W1-06, W1-09, W1-10, W1-11 trên màn mobile, cộng thêm:

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| M2-01 | Đặt lại thành công khi app đang có phiên cũ trong SecureStore | Token cũ bị xóa; app về màn đăng nhập |
| M2-02 | Link "Quên mật khẩu?" trên màn đăng nhập | Mở đúng màn, email đã nhập được điền sẵn |
| M2-03 | Bàn phím số khi nhập mã; dán mã 8 chữ số từ clipboard | Nhập được, không mất chữ số |

### M3. Đổi mật khẩu

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| M3-01 | Đổi mật khẩu đúng | Thành công; đăng nhập lại bằng mật khẩu mới được |
| M3-02 | Mật khẩu hiện tại sai | Báo lỗi, không đổi |
| M3-03 | Tài khoản chỉ đăng nhập Google (`canChangePassword=false`) | Không hiện form đổi mật khẩu |
| M3-04 | Phiên trên thiết bị khác sau khi đổi | Đúng với hành vi backend đã xác nhận ở bước triển khai |

### M4. Kết quả phân tích AI

Môi trường local dùng adapter phân tích giả của backend; không gửi dữ liệu tới provider thật.

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| M4-01 | Lưu bài, mở chi tiết khi phân tích đang chạy | Hiện "đang phân tích"; polling dừng khi có kết quả |
| M4-02 | Kết quả thành công | Hiện `reflection`, chip `emotions`/`topics`; **không** hiện `sentimentScore`, `analysisId` |
| M4-03 | Phân tích lỗi (`FAILED`) | Có nút Thử lại; bấm thì gọi `:retry` và quay lại trạng thái chờ |
| M4-04 | Bị chặn vì an toàn (`BLOCKED_BY_SAFETY`) | Không có nút Thử lại; mở M5 nếu `riskLevel` HIGH/CRITICAL |
| M4-05 | Backend chậm quá thời gian polling (khoảng 2 phút) | Polling dừng, hiện thông báo quay lại sau, không có spinner chạy mãi |
| M4-06 | Rời màn hình khi đang polling | Polling dừng (không còn request chạy ngầm) |
| M4-07 | Sửa bài sau khi đã có kết quả | Kết quả cũ hiện là đã cũ (`STALE`) hoặc đang phân tích lại, không hiện như kết quả mới |

### M5. Màn hỗ trợ an toàn

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| M5-01 | Lưu bài với câu kích hoạt synthetic, backend trả `riskLevel` HIGH/CRITICAL | Màn hỗ trợ mở ngay; bài vẫn được lưu |
| M5-02 | DB chưa có nguồn hỗ trợ được duyệt (hiện trạng) | Hiện nội dung chung; **không** có số điện thoại nào |
| M5-03 | Thêm 1 nguồn hỗ trợ synthetic đã duyệt vào DB local | Hiện đúng tên và số từ backend; bấm số thì mở trình gọi điện với đúng số đó |
| M5-04 | Bài bình thường | Không mở màn hỗ trợ |
| M5-05 | Trợ lý trả `SAFETY_FLOW` (M7) | Đóng trợ lý, xóa hội thoại, mở màn hỗ trợ |
| M5-06 | Screen reader (TalkBack / VoiceOver) | Tiêu đề, nội dung và nút đóng được đọc đúng thứ tự |
| M5-07 | Kiểm tra bundle | Không có số hotline nào hard-code trong mã nguồn mobile (`grep` theo mẫu số điện thoại) |

### M6. Check-in hằng ngày

Chạy lại W2-01 đến W2-10 trên mobile, cộng thêm:
- **M6-01:** check-in trên web, mở mobile cùng ngày → hiện bản ghi đó (và ngược lại).
- **M6-02:** thanh trượt dùng được bằng ngón tay và bằng screen reader (tăng/giảm từng bước).

### M7. Trợ lý trang viết

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| M7-01 | Lần đầu mở, chưa có consent `AI_PROCESSING` | Hiện thẻ xin phép; "Để sau" thì khóa ô nhắn, không gọi API |
| M7-02 | Đồng ý | Gọi `PUT /me/consents/AI_PROCESSING` với đúng `documentVersion`; trợ lý mở lời |
| M7-03 | Chèn câu hỏi gợi ý | Câu hỏi xuất hiện trong bài (`promptQuestion` nếu M9 đã xong) |
| M7-04 | Gợi ý tựa đề | Bấm thì điền vào ô tựa đề |
| M7-05 | Thở chậm 2 phút | Chạy trên máy, không gọi API; xong tự nhiên thì báo lại cho trợ lý |
| M7-06 | Backend trả lỗi `503` / `429` / mất mạng | Trạng thái "trợ lý tạm nghỉ", khóa ô nhắn, không crash |
| M7-07 | Thu nhỏ rồi mở lại | Hội thoại còn nguyên; có chấm chưa đọc nếu có tin mới |
| M7-08 | Thoát hẳn app rồi mở lại | Hội thoại **không** còn, không có nội dung chat trong bộ nhớ máy |
| M7-09 | Bàn phím mở khi trợ lý đang hiện | Ô nhắn và editor không bị che |

### M8. Câu hỏi gợi ý

- **M8-01:** danh sách tải được.
- **M8-02:** chèn một câu vào bài.
- **M8-03:** danh sách rỗng thì hiện trạng thái trống, không có lỗi.

### M9. Rich text và chèn ảnh

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| M9-01 | Bài tạo trên web có đủ node/mark được allowlist | Mobile hiển thị đúng định dạng |
| M9-02 | Bài có node lạ (dữ liệu synthetic) | Bỏ qua node đó, không crash |
| M9-03 | Soạn trên mobile: đậm, nghiêng, danh sách, trích dẫn; lưu; mở trên web | Web hiển thị giống hệt; backend chấp nhận (không `400`) |
| M9-04 | Gõ tiếng Việt bằng Gboard / bàn phím iOS (Telex, VNI) | Không mất dấu, không nhảy con trỏ |
| M9-05 | Chèn ảnh từ thư viện, lưu bài | Upload thành công; node `image` có `assetId`; xem lại được trên web |
| M9-06 | Chèn ảnh rồi thoát mà không lưu | Không có ảnh mồ côi hiện trong bài nào; backend để ở trạng thái pending |
| M9-07 | Ảnh quá lớn hoặc sai định dạng | Báo lỗi rõ ràng |
| M9-08 | Bản nháp text cũ trong SecureStore | Được chuyển sang định dạng mới, không mất nội dung |
| M9-09 | Hiệu năng: bài khoảng 3000 từ trên Android tầm trung | Gõ không giật thấy rõ; mở bài dưới 2 giây |

### M10. Gửi phản hồi

- **M10-01:** gửi với từng loại `BUG`, `IDEA`, `OTHER` → backend trả thành công.
- **M10-02:** nội dung rỗng → bị chặn ở client.
- **M10-03:** phản hồi hiện trong trang quản trị feedback trên web.

### M11. Yêu cầu xóa tài khoản

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| M11-01 | Yêu cầu xóa với mật khẩu đúng | Thành công; token bị xóa; về màn đăng nhập; đăng nhập lại bị từ chối |
| M11-02 | Mật khẩu sai | Báo lỗi, không tạo yêu cầu |
| M11-03 | Hủy trong thời gian chờ bằng email + mật khẩu | Tài khoản dùng lại được |
| M11-04 | Xem trạng thái yêu cầu | Đúng trạng thái backend trả |
| M11-05 | Màn xác nhận | Có 2 bước, nêu rõ thời gian chờ 7 ngày và cách hủy |

### M12. Đăng nhập Google

| ID | Kịch bản | Kết quả mong đợi | Mức |
|---|---|---|---|
| M12-01 | Backend: code hợp lệ với `platform=android`/`ios` và `redirectUri` thuộc allowlist | Đăng nhập thành công | JUnit (Google giả lập) |
| M12-02 | Backend: `redirectUri` ngoài allowlist; `aud` không khớp platform; thiếu `codeVerifier` | Từ chối | JUnit |
| M12-03 | Backend: request kiểu web cũ (không có `platform`) | Vẫn chạy như trước (tương thích ngược) | JUnit |
| M12-04 | Web: đăng nhập Google sau khi đổi backend | Vẫn chạy | Playwright, thủ công |
| M12-05 | Android và iOS thật: đăng nhập, hủy giữa chừng, tài khoản Google mới, tài khoản đã có email trùng | Đúng hành vi của web | Thủ công |

---

## 6. Checklist thiết bị thật cho mobile

Chạy trên 1 Android và 1 iOS cho mỗi đợt phát hành:

- [ ] Cỡ chữ hệ thống lớn nhất: không vỡ bố cục, không cắt chữ ở các màn mới.
- [ ] Bàn phím: không che ô nhập ở mọi form mới (M2, M3, M6, M7, M9, M10, M11).
- [ ] Safe area: nội dung không bị tai thỏ hoặc thanh điều hướng che.
- [ ] TalkBack / VoiceOver: đọc được mọi nút chỉ có icon (có `accessibilityLabel`).
- [ ] Chuyển app xuống nền rồi mở lại giữa luồng đặt lại mật khẩu và trợ lý: không mất dữ liệu đang nhập, không lộ nội dung trong ảnh chụp màn hình đa nhiệm (nếu đã bật bảo vệ).
- [ ] Mất mạng ở giữa thao tác lưu: báo lỗi, giữ dữ liệu.
- [ ] Khóa và mở lại máy khi token đã hết hạn: refresh chạy đúng, không đăng xuất nhầm.

## 7. Kiểm tra quyền riêng tư và bảo mật (mọi hạng mục)

- [ ] Log backend (mức INFO/DEBUG ở local) không chứa nội dung nhật ký, ghi chú check-in, mật khẩu, token, mã 8 chữ số, nội dung chat.
- [ ] Log server Next.js và console trình duyệt không in các dữ liệu trên.
- [ ] Mobile chỉ lưu token và bản nháp trong SecureStore; không ghi dữ liệu nhạy cảm vào AsyncStorage hay file thường.
- [ ] Không có URL nào chứa token hoặc mật khẩu trong query string.
- [ ] Mọi route web mới có kiểm tra `sameOrigin` với method khác GET.
- [ ] Ảnh test là ảnh synthetic, không chứa thông tin cá nhân.

## 8. Tiêu chí hoàn thành và cách báo cáo

**Một hạng mục được coi là xong khi**
1. `tsc` và `lint` sạch ở app liên quan. Nếu có sửa backend thì test backend liên quan pass.
2. Mọi ca "Tự động" của hạng mục đã có test và pass với backend local thật.
3. Mọi ca "Thủ công" đã chạy và ghi kết quả.
4. Mục 7 không có vi phạm.
5. Tài liệu ở mục 6 của kế hoạch thực thi đã được cập nhật.

**Mẫu ghi kết quả** (thêm vào cuối file này hoặc PR mô tả):

```text
Hạng mục: W1
Ngày / người chạy:
Commit:
Môi trường: backend local (profile local), web dev, Chrome <phiên bản>
Tự động: tests/password-reset.spec.ts — <số pass>/<tổng>
Thủ công: W1-13 pass, W1-15 pass, W1-16 fail (<mô tả ngắn, link issue>)
Chưa chạy / lý do:
```

Báo rõ ca nào **chưa chạy** và lý do (ví dụ thiếu thiết bị iOS, thiếu Docker). Không ghi "đã kiểm thử" cho phần chưa chạy.

---

## 9. Kết quả đã chạy

### Đợt 1 — chạy lại và đối chiếu từng ca, 2026-11-11

**Môi trường:** backend local (profile local) với PostgreSQL, Redis và Mailpit qua Docker; web `next dev` ở `localhost:3000`; Expo web ở `localhost:8081` với `EXPO_PUBLIC_API_BASE_URL=http://localhost:8080`; Chromium của Playwright (web) và Edge headless (mobile). Chưa commit.

**Lệnh**

| Phạm vi | Lệnh | Kết quả |
|---|---|---|
| W1 | `PLAYWRIGHT_SKIP_WEBSERVER=1 npx playwright test tests/password-reset.spec.ts --project=chromium` | 6/6 pass |
| W1-11 web | `RUN_RATE_LIMIT=1 PLAYWRIGHT_SKIP_WEBSERVER=1 npx playwright test tests/password-reset-rate-limit.spec.ts --project=chromium` | 1/1 pass |
| Hồi quy web | `npx playwright test tests/smoke.spec.ts tests/same-origin-guard.spec.ts` | 12/12 pass |
| M2 | `CORS_SHIM=1 node scripts/e2e-password-reset.mjs` | Pass 10 bước |
| W1-11 mobile | `CORS_SHIM=1 CHECK_RATE_LIMIT=1 node scripts/e2e-password-reset.mjs` | Pass |
| M4 + M5 | `CORS_SHIM=1 node scripts/e2e-safety-analysis.mjs` | Pass 8 bước |
| M4-05 | `LONG=1 CORS_SHIM=1 node scripts/e2e-safety-analysis.mjs` (thêm khoảng 3 phút) | Pass bước hết thời gian; lần chạy này dừng ở bước cuối do selector của script, đã sửa và chạy lại không có `LONG` (dòng trên) |
| Backend | `./mvnw.cmd test -Dtest=PasswordResetUnitTest,IdentityControllerPasswordResetTest` | 14/14 pass |
| Tĩnh | `npx tsc --noEmit`, `npx eslint` (web `features/auth tests`, mobile `src`) | Sạch |

**Đối chiếu từng ca**

| Ca | Kết quả | Ghi chú |
|---|---|---|
| W1-01 | Pass | Mailpit có thư chứa `#token=` và mã 8 chữ số |
| W1-02 | Pass | Cùng HTTP `202` và cùng thông điệp với email có thật; không có thư. Mobile cũng hiện cùng thông điệp |
| W1-03 | Pass | Token biến khỏi thanh địa chỉ, form liên kết hiện ra |
| W1-04 | Pass | Mật khẩu mới đăng nhập được, mật khẩu cũ bị từ chối |
| W1-05 | Pass | Web và mobile; mã được dán dạng `1234 5678` |
| W1-06 | Pass | Mã thiếu chữ số bị chặn ở client; mã sai báo lỗi chung |
| W1-07 | Pass | Báo không hợp lệ và có link "Gửi yêu cầu mới" |
| W1-08 | Pass | Mã của lần yêu cầu đầu bị từ chối, mã lần sau dùng được |
| W1-09 | Pass | Web và mobile báo riêng "phải khác mật khẩu hiện tại" |
| W1-10 | Pass | Web và mobile: lỗi hiện ra, **0** request xác nhận được gửi |
| W1-11 | Pass | Web và mobile hiện "thử quá nhiều lần", form vẫn dùng được, không lỗi trang |
| W1-12 | Pass | Trình duyệt thứ hai đang đăng nhập bị chuyển về `/auth/login` sau khi đặt lại |
| W1-13 | **Chưa chạy** | Cần chờ 15–60 phút hoặc clock giả. Logic hết hạn nằm ở `IdentityRepository` (`expiresAt.isAfter(now)`); unit test backend dùng store giả nên không phủ query này |
| W1-14 | Một phần | `Referrer-Policy: no-referrer` pass. `Cache-Control` trên `next dev` là `no-cache, must-revalidate` (dev ghi đè); `no-store` cần kiểm trên bản build production. Chưa rà log server |
| W1-15 | Pass | Hai trang hiện đủ tiếng Anh, không còn key i18n hay chữ tiếng Việt |
| W1-16 | Một phần | Thứ tự Tab: ô email rồi đến nút gửi; lỗi có `role="alert"`. Chưa thử với screen reader thật |
| M2-01 | Không quan sát được | Màn đặt lại chỉ vào được khi đã đăng xuất; code vẫn gọi `clearSession()` sau khi thành công |
| M2-02 | Pass | Email đã nhập ở màn đăng nhập được điền sẵn, không xuất hiện trên URL |
| M2-03 | Pass sau khi sửa lỗi | Xem "Lỗi tìm thấy" bên dưới. Bàn phím số chỉ kiểm được trên thiết bị thật |
| M4-01 | Pass | Hiện "Đang đọc trang viết của bạn…" và có polling |
| M4-02 | **Chưa chạy** | Local không bật provider phân tích nên không có kết quả `ANALYZED` |
| M4-03 | **Chưa chạy** | Không tạo được trạng thái `ANALYSIS_FAILED` ở local |
| M4-04 | Pass | Bài `BLOCKED_BY_SAFETY` không có nút "Phân tích lại", mở lại được màn hỗ trợ |
| M4-05 | Pass | Sau khoảng 2 phút hiện thông báo quay lại sau, không còn request nào trong 35 giây tiếp theo |
| M4-06 | Pass | Rời màn hình thì không còn request phân tích nào trong 35 giây |
| M4-07 | **Chưa chạy** | Cần có kết quả `ANALYZED` trước (như M4-02) |
| M5-01 | Pass | Màn hỗ trợ mở ngay sau khi lưu; bài vẫn có trong danh sách |
| M5-02 | Pass | DB có 0 nguồn được duyệt; không có số điện thoại hay nút "Gọi" nào |
| M5-03 | **Chưa chạy** | Cần thêm một nguồn hỗ trợ synthetic đã duyệt vào DB local; chưa thêm vì sẽ thay đổi dữ liệu local của bạn |
| M5-04 | Pass | Bài bình thường không mở màn hỗ trợ |
| M5-05 | Chưa áp dụng | Cần M7 (trợ lý trên mobile) |
| M5-06 | **Chưa chạy** | Cần TalkBack/VoiceOver trên thiết bị thật |
| M5-07 | Pass | Không có số hotline nào viết sẵn trong `my-app-mobile/src` và modal safety của web |
| Mục 6 (thiết bị thật) | **Chưa chạy** | Chưa có Android/iOS |
| Mục 7 (riêng tư) | Một phần | URL không chứa token/email/mật khẩu (đã kiểm); chưa rà log backend và Next.js |

**Lỗi tìm thấy khi đối chiếu (đã sửa)**
- Ô nhập mã trên **web và mobile** đặt `maxLength=8` trước khi lọc ký tự, nên dán mã dạng `1234 5678` sẽ mất chữ số cuối. Đã sửa: lọc chữ số rồi mới cắt còn 8.
- Lần chạy trước: React StrictMode chạy effect hai lần làm mất token của liên kết đặt lại (đã sửa ở `ResetPasswordForm`).

**Lưu ý môi trường và phát hiện ngoài phạm vi**
- Backend local chỉ cho CORS từ `http://localhost:3000` và từ chối `Origin` khác ngay ở server. Script mobile có cờ `CORS_SHIM=1`: trong phiên test, trình duyệt bỏ header `Origin` khỏi request API và thêm header CORS vào response. Backend không bị thay đổi.
- Giới hạn tần suất theo IP được tính riêng cho web (qua BFF, IP lấy từ `X-Forwarded-For`) và cho mobile (gọi thẳng từ trình duyệt/thiết bị).
- **Cần xem xét (backend):** `server.forward-headers-strategy: framework` tin `X-Forwarded-For` từ mọi nguồn. Client gọi thẳng backend có thể tự đặt header này để né giới hạn theo IP; giới hạn theo email vẫn áp dụng. Nên giới hạn các proxy được tin cậy khi triển khai.
- Bước W1-11 dùng hết hạn mức đặt lại mật khẩu của IP local trong 15 phút.
