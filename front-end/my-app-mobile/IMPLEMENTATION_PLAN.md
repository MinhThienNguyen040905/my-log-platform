# Kế hoạch thực thi MyLog mobile

Trạng thái kiểm chứng ngày 2026-10-13: giai đoạn 0–3 có mã và luồng API/UI web chạy với backend local. Giai đoạn 4 có nhật ký văn bản, bản nháp, CRUD, yêu thích và thao tác nhãn; giai đoạn 5 có các lát cắt dashboard, lịch sử theo ngày, insight/report, self-care, phiên đăng nhập và xuất dữ liệu. Xem [PROJECT_STATUS.md](PROJECT_STATUS.md) để biết phần đã kiểm tra và phần còn mở. Chưa có kiểm thử Android/iOS native vì không có thiết bị hoặc emulator được kết nối.

## Cách triển khai

Đi theo từng luồng hoàn chỉnh: **component → màn hình với dữ liệu synthetic → state/validation → API backend → kiểm thử trên thiết bị**. Hoàn thiện một luồng trước khi tạo hàng loạt màn hình khác. `src/app/` chỉ giữ route và ghép màn hình; logic ở `src/features/`. Chỉ tạo file/thư mục khi có công việc thực tế.

Mọi màn hình dữ liệu cần các trạng thái loading, empty, error và retry phù hợp. Dữ liệu synthetic chỉ dùng trong preview/test, không được trình bày như dữ liệu backend thật. Không log token, nội dung nhật ký hoặc phản hồi AI.

## Giai đoạn 0 — Chốt contract và môi trường chạy

1. Lập bảng endpoint/backend DTO cho các luồng MVP từ controller, OpenAPI và test hiện tại: auth, `/me`, consent, journal, tag. Ghi method, body, status, lỗi, `ETag`/`If-Match`, `Idempotency-Key`, cursor và timezone.
2. Chốt URL backend cho Android emulator, iOS simulator và thiết bị thật; kiểm tra kết nối thực tế. URL đưa vào `EXPO_PUBLIC_*` chỉ là cấu hình công khai; credential không được đóng vào app. Môi trường dùng dữ liệu thật cần HTTPS.
3. Chốt phạm vi editor đầu tiên: văn bản và những node/mark `contentJson` backend chấp nhận. Chốt cách lưu/xóa bản nháp cục bộ. Xác minh cơ chế lưu token native trước khi chọn thư viện.

**Hoàn thành khi:** có bảng contract được đối chiếu với backend đang chạy và thiết bị phát triển gọi được endpoint sức khỏe/API phù hợp. Không dùng các route Next.js `/api/*` làm contract của native app.

## Giai đoạn 1 — Design system và component nền

1. Từ [design skill](skills/mylog-mobile-design/SKILL.md), định nghĩa token màu, chữ, spacing, radius, viền và bóng trong `src/constants/`; chọn font hỗ trợ tiếng Việt và trạng thái light/dark thực tế.
2. Tạo những component có ít nhất hai nơi dùng hoặc cần nhất quán: `Screen`, `Text`, `Button`, `TextField`, `IconButton`, `PaperCard`, `StatusView`, `FormError`, `LoadingView`. Đặt trong `src/components/ui/`; component riêng của feature ở feature đó.
3. Tạo lớp điều hướng Expo Router gồm root layout, nhóm auth, onboarding và nhóm app; xử lý safe area, keyboard, back và trạng thái tải phiên. Chưa hiển thị tab dẫn đến màn hình chưa hoạt động.
4. Dùng nội dung synthetic để dựng một màn hình auth và một màn hình journal mẫu; kiểm tra nhỏ/lớn, font tăng cỡ, tiếng Việt dài, Android/iOS nếu có thiết bị.

**Hoàn thành khi:** UI nền thể hiện đúng màu/viền/chữ/scrapbook của MyLog, component có pressed/disabled/loading/error, không còn phụ thuộc vào màn hình Expo mẫu cho luồng MyLog đã dựng.

## Giai đoạn 2 — Auth: giao diện → phiên → API

1. Ghép `Login`, `Register`, `VerifyCode` từ component form; validation dựa trên contract backend. Có màn hình báo gửi email và gửi lại mã. Luồng mã 8 chữ số là đường đi đầu tiên; link email xử lý ở giai đoạn sau.
2. Viết `src/lib/api/` cho base URL, JSON, status/error mapping, `Authorization` và hủy request; viết `src/features/auth/api/` cho register, verify, resend, login, refresh, logout. Gọi trực tiếp backend `/api/v1/*`.
3. Tạo session provider: access token trong bộ nhớ, refresh token trong kho bảo mật thiết bị, khôi phục phiên khi mở app, gom refresh đồng thời, retry một lần khi `401`, và xóa phiên khi refresh thất bại. Không dùng cookie/session helper của Next.js.
4. Nối form với API; sau verify chuyển sang login, không tự coi tài khoản là đã đăng nhập. Đăng xuất gọi backend rồi xóa token/dữ liệu phiên cục bộ kể cả khi mạng lỗi; xử lý kết quả backend theo contract.

**Hoàn thành khi:** đăng ký → xác minh mã → đăng nhập → mở lại app → refresh → đăng xuất chạy với backend và tài khoản synthetic; sai mật khẩu, mã hết hạn, `429`, mất mạng và refresh thất bại có phản hồi rõ.

## Giai đoạn 3 — Onboarding và hồ sơ

1. Dựng màn hình onboarding và profile từ component riêng của feature; hiển thị trạng thái consent thực tế. Không đưa nút cấp quyền tùy chọn khi tài liệu đồng ý chưa được công bố.
2. Nối `GET/PATCH /me`, `GET/PUT /me/consents`, `POST /me/onboarding:complete`; giữ version/ETag và gửi `If-Match` khi backend yêu cầu.
3. Điều hướng theo trạng thái phiên và `onboardingCompletedAt`; không quyết định chỉ từ một cờ local.

**Hoàn thành khi:** người dùng mới hoàn tất onboarding, người dùng cũ vào app, sửa hồ sơ phản ánh trên web, xung đột phiên bản không ghi đè âm thầm.

## Giai đoạn 4 — Nhật ký MVP

1. Dựng `JournalList`, `JournalCard`, `JournalDetail`, `JournalEditor`, `MoodPicker`, `TagPicker` với dữ liệu synthetic. UI editor hiển thị trạng thái bản nháp/lưu và bảo toàn nội dung khi rời màn hình hoặc lỗi mạng.
2. Nối danh sách cursor, đọc chi tiết, tạo/sửa/xóa, yêu thích và tag. Giữ `nextCursor`, `Idempotency-Key` khi tạo, `If-Match` khi sửa/xóa, timezone và `occurredAt`. Khi `409`, cho tải phiên bản mới và giữ bản nháp để người dùng quyết định.
3. Chuyển nội dung editor sang `contentJson` đúng allowlist backend. Bắt đầu với tập định dạng nhỏ đã kiểm chứng; không sao chép Tiptap DOM editor của web vào React Native. Không gửi ảnh khi chưa có luồng upload/delivery mobile hoàn chỉnh.
4. Lưu bản nháp cục bộ có ranh giới theo user; xóa khi logout hoặc đổi tài khoản theo chính sách được chốt ở giai đoạn 0. Backend tiếp tục sở hữu safety screening; mobile chỉ hiển thị trạng thái/nguồn hỗ trợ đã được backend trả về.

**Hoàn thành khi:** tạo trên mobile thấy trên web và ngược lại; sửa/xóa/yêu thích/tag đồng bộ; pagination không lặp/mất bài; mất mạng và xung đột không làm mất nội dung đang viết.

## Giai đoạn 5 — Mở rộng theo giá trị sử dụng

Thực hiện theo từng vertical slice sau khi MVP ổn định: dashboard → lịch sử lịch → insight/report → self-care → settings/quyền dữ liệu. Mỗi slice có component, màn hình synthetic, state, API, và kiểm chứng backend. Chỉ hiển thị insight/AI khi backend có dữ liệu và trạng thái cho phép; không tạo số liệu hay phản hồi giả trong sản phẩm. Admin và công cụ `/dev/users` của web không nằm trong phạm vi mobile mặc định.

Liên kết xác minh email qua iOS Universal Links/Android App Links là slice riêng: cấu hình domain, mở app vào màn hình xác nhận, người dùng bấm xác nhận rồi mới gửi POST; không lưu token liên kết trong log hoặc local storage.

## Cổng kiểm tra cho mỗi slice

- `npx tsc --noEmit`, lint theo cấu hình mobile, và kiểm tra thủ công trên ít nhất một nền tảng native; mở rộng sang Android và iOS trước khi coi một luồng sẵn sàng phát hành.
- Kiểm tra navigation/back, keyboard, safe area, font lớn, tiếng Việt dài, screen reader labels, loading/empty/error/retry và mất mạng.
- Kiểm tra API bằng tài khoản và nhật ký synthetic: status/lỗi, refresh token, phân quyền, ETag, idempotency, cursor và dữ liệu chéo web/mobile.
- Ghi rõ phần chưa kiểm chứng. Không coi build web của Expo hay ảnh chụp UI là bằng chứng luồng native/backend hoạt động.

## Tài liệu tham chiếu

- [Cấu trúc dự kiến](STRUCTURE.md), [design skill](skills/mylog-mobile-design/SKILL.md), [web README](../my-app-web/README.md), [backend README](../../backend/README.md).
- Expo SDK 57: https://docs.expo.dev/versions/v57.0.0/ ; SecureStore: https://docs.expo.dev/versions/v57.0.0/sdk/securestore/ ; Linking: https://docs.expo.dev/linking/overview/ . Kiểm tra tài liệu theo phiên bản trước khi thêm package hoặc viết mã native.
