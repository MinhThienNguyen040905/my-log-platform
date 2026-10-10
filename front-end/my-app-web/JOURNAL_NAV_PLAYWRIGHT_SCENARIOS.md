# Kịch bản Playwright local cho Gemini: pop-up rời Journal Editor

## Prompt giao cho Gemini

> Hãy chạy các kịch bản bên dưới bằng Playwright trên MyLog local và báo kết quả từng ca PASS/FAIL/BLOCKED. Chỉ dùng dữ liệu nhật ký giả. Không sửa code ứng dụng trong lượt kiểm thử; nếu thấy lỗi, ghi bước tái hiện, kết quả thực tế, kết quả mong đợi và ảnh chụp pop-up khi cần. Không in hoặc chụp mật khẩu, cookie, access token, refresh token, nội dung response nhạy cảm. Không dùng `waitForTimeout` cố định khi có thể chờ URL, request hoặc locator.

## Chuẩn bị

- FE chạy tại `http://localhost:3000`, BE chạy tại `http://localhost:8080`. Nếu cổng khác, dùng biến `BASE_URL`.
- Dùng **tài khoản kiểm thử riêng đã xác minh email và hoàn tất onboarding**. Nhận thông tin đăng nhập qua biến môi trường `PLAYWRIGHT_TEST_EMAIL` và `PLAYWRIGHT_TEST_PASSWORD`; không ghi chúng vào file kết quả.
- Tạo browser context mới cho mỗi nhóm ca. Đặt viewport desktop `1440×900`; nhóm mobile dùng `390×844`.
- Nếu chưa đăng nhập: mở `/auth/login`, điền `#login-email`, `#login-password`, submit form và chờ `/dashboard`. Nếu không có tài khoản hoặc BE không hoạt động, đánh dấu BLOCKED kèm lý do, không tự dùng dữ liệu thật.
- Khi cần nội dung editor, dùng `.ProseMirror[contenteditable="true"]`; tiêu đề dùng input có placeholder `Đặt một tựa đề cho hôm nay...`. Pop-up có `role="dialog"` và tên `Bài nhật ký chưa được lưu`.
- Các ca tạo bài thật nên dùng nội dung tổng hợp như `Playwright journal <timestamp>` để có thể tìm lại; ghi ID bài tạo ra trong báo cáo và chỉ xóa qua UI/API nếu tài khoản kiểm thử cho phép.

## Kịch bản

### JN-01 — Bài mới chưa sửa

1. Mở `/journal-editor`, đợi editor và top nav hiển thị.
2. Nhấn `Dashboard` trong `header nav`.
3. Kỳ vọng: chuyển `/dashboard` ngay; không xuất hiện dialog.

### JN-02 — Bài mới có nội dung, chọn Ở lại

1. Mở `/journal-editor`, nhập tiêu đề hoặc nội dung giả vào editor.
2. Nhấn `Dashboard` trong `header nav`.
3. Kỳ vọng: URL vẫn là `/journal-editor`, dialog `Bài nhật ký chưa được lưu` xuất hiện; chưa có request POST/PATCH journal.
4. Nhấn `Ở lại`. Kỳ vọng: dialog đóng, URL giữ nguyên, tiêu đề/nội dung vừa nhập vẫn còn.
5. Nhấn `Insights & AI Reports`; nhấn `Escape`. Kỳ vọng: dialog đóng, vẫn ở editor, nội dung còn.

### JN-03 — Xác nhận rời đúng đích

1. Với bài đang có thay đổi chưa lưu, nhấn `History & Calendar` trong `header nav`.
2. Nhấn `Rời trang` trong dialog.
3. Kỳ vọng: chuyển **đúng** `/history-calendar`; dialog đóng; không tự gửi POST/PATCH journal. Quay lại `/journal-editor` và xác nhận bản nháp bài mới được khôi phục nếu local autosave đã hoàn tất.
4. Lặp lại với logo MyLog (`header a[href="/dashboard"]`) và liên kết `Hồ sơ & Thiết lập` trong menu tài khoản. Mỗi lần phải hỏi trước và chuyển đúng `/dashboard` hoặc `/setting` sau xác nhận.

### JN-04 — Các kiểu thay đổi đều được nhận diện

Chạy từng biến thể trên một editor mới, không tích lũy thay đổi giữa các biến thể: chỉ sửa **tiêu đề**, chỉ sửa **nội dung**, chỉ đổi **cảm xúc/điểm tâm trạng**. Với mỗi biến thể, nhấn top nav và kỳ vọng dialog xuất hiện. Nếu đổi giá trị rồi đổi lại đúng giá trị ban đầu, kỳ vọng không còn dialog.

### JN-05 — Bài đang sửa

1. Tạo và lưu một bài kiểm thử trước (có thể dùng bước lưu thành công của JN-06), rồi mở `/history-calendar`, chọn bài và nhấn liên kết sửa có URL `/journal-editor?id=<id>`.
2. Đợi bài tải xong, nhấn `Dashboard` ngay khi **chưa sửa**. Kỳ vọng: chuyển trang, không có dialog.
3. Mở lại bài, sửa tiêu đề hoặc nội dung, nhấn `Dashboard`. Kỳ vọng: dialog xuất hiện.
4. Nhấn `Ở lại`. Kỳ vọng: vẫn ở editor và nội dung sửa còn nguyên.

### JN-06 — Lưu thành công / lưu lỗi

1. Bài mới: nhập nội dung giả hợp lệ, nhấn `Lưu`, đợi request `POST /api/journal-entries` thành công và chuyển `/history-calendar`. Kỳ vọng: **không** có dialog rời trang.
2. Bài đang sửa: sửa một nội dung, nhấn `Cập nhật bài viết`, đợi request `PATCH /api/journal-entries/<id>` thành công. Kỳ vọng: không có dialog rời trang.
3. Ca lỗi riêng: dùng `page.route` để trả `503` cho đúng request POST hoặc PATCH journal; nhấn Lưu/Cập nhật. Kỳ vọng: vẫn ở editor, có thông báo lỗi. Nhấn top nav: dialog **vẫn** xuất hiện. Gỡ route mock sau ca này.

### JN-07 — Đăng xuất từ top nav

1. Với bài đang sửa, mở menu tài khoản và nhấn `Đăng xuất`.
2. Kỳ vọng: dialog xuất hiện; trước xác nhận **không** gửi `POST /api/auth/logout`.
3. Nhấn `Ở lại`. Kỳ vọng: vẫn đăng nhập, vẫn ở editor, nội dung còn nguyên.
4. Nhấn Đăng xuất lần nữa rồi nhấn nút `Đăng xuất` trong dialog. Kỳ vọng: sau xác nhận mới gửi request logout và chuyển `/auth/login`.

### JN-08 — Mobile

1. Tạo context viewport `390×844`, đăng nhập và mở editor.
2. Nhập nội dung giả, nhấn `Dashboard` hoặc `History & Calendar` ở thanh điều hướng dưới; xác nhận dialog xuất hiện.
3. Nhấn `Ở lại`, xác nhận nội dung còn. Mở menu mobile, nhấn liên kết cài đặt; xác nhận dialog xuất hiện. Nhấn `Rời trang`, xác nhận đi đúng `/setting`.

## Quy tắc kiểm tra

- Chỉ tính PASS khi kiểm tra được **URL, trạng thái dialog, nội dung editor và request API** tương ứng. Dùng `page.waitForURL`, `expect(locator)` và `page.waitForResponse`/request listener; tránh ngủ cố định.
- Các liên kết ngoài top nav, nút Back của trình duyệt và reload **không thuộc phạm vi** pop-up này. Không đánh dấu FAIL vì các thao tác đó chưa được chặn.
- Nếu dialog xuất hiện ngay khi vừa mở bài đang sửa mà chưa thay đổi, báo FAIL với ID bài và ảnh; đây là lỗi nhận diện trạng thái chưa lưu.
- Nếu có request lưu journal sau khi chọn `Rời trang` mà chưa nhấn Lưu, báo FAIL.
- Sau mỗi ca, tạo context mới hoặc khôi phục trạng thái rõ ràng để bản nháp của ca trước không làm sai kết quả ca sau.

## Mẫu báo cáo

| Mã ca | Kết quả | URL trước → sau | Dialog | Request liên quan | Bằng chứng/lỗi |
| --- | --- | --- | --- | --- | --- |
| JN-01 | PASS/FAIL/BLOCKED | `/journal-editor` → `/dashboard` | Không | Không POST/PATCH | ... |

Cuối báo cáo ghi môi trường FE/BE, trình duyệt, kích thước viewport, danh sách ca lỗi và bước tái hiện ngắn. Che mọi dữ liệu xác thực và chỉ dùng văn bản nhật ký giả.
