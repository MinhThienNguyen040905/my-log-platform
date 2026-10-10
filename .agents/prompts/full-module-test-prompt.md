# Prompt cho Antigravity: kiểm thử toàn bộ module MyLog trên local

Sao chép phần dưới đây vào Antigravity. Danh sách ca và nơi ghi kết quả: `frontend/.agents/reports/full-module-test-cases.md`.

---

Bạn là QA agent kiểm thử repository `E:\Project\my-log-platform`. Hãy chạy lần lượt các lô M0–M8 trong `frontend/.agents/reports/full-module-test-cases.md` bằng Playwright trên FE local, kết hợp HTTP qua FE proxy và kiểm tra code/test BE khi module chưa có UI. Đọc `AGENTS.md`, `front-end/my-app-web/AGENTS.md`, API contract và code của module trước khi chạy. Dùng trạng thái **PASS / FAIL / BLOCKED / GAP**: `GAP` nghĩa là tính năng/API đã có trong scope nhưng FE chưa triển khai hoặc còn dữ liệu minh họa; `BLOCKED` nghĩa là thiếu môi trường/tài khoản/provider nên chưa xác minh được. Không tự biến `BLOCKED` thành `PASS`.

## Môi trường và tài khoản

- FE: `http://localhost:3000` (hoặc `BASE_URL`); BE: `http://localhost:8080`. Kiểm tra health và trạng thái FE/BE trước khi bắt đầu; nếu chưa chạy, dùng hướng dẫn repo để khởi động. Ghi phiên bản/commit hoặc trạng thái working tree tại thời điểm chạy.
- Dùng tài khoản thử riêng đã xác minh email, tài khoản onboarding mới, tài khoản admin **được cấp sẵn** và tài khoản dùng riêng cho xóa. Lấy credential qua biến môi trường/tài khoản test được chủ dự án cung cấp. Không tự nâng quyền admin hoặc tác động tài khoản thật. Nếu thiếu loại tài khoản, đánh dấu các ca tương ứng `BLOCKED`.
- Chỉ dùng nhật ký, mục tiêu, feedback và safety text **synthetic**. Không gửi email ra hộp thư thật; ca xác minh email cần local mail sink/test token được cấp. Không dùng số điện thoại hoặc nguồn khủng hoảng chưa được duyệt.
- Thao tác xóa tài khoản chỉ thực hiện trên tài khoản thử dùng một lần, sau khi hoàn tất mọi ca khác; nếu không có tài khoản này, đánh dấu `BLOCKED`. Không xóa dữ liệu môi trường chung để dọn test.
- Bật Playwright MCP/browser nếu sẵn có. Có thể dùng `page.request` hoặc browser `fetch` tới các route FE như `/api/me`, `/api/journal-entries`, `/api/backend/check-ins` để kiểm tra contract bằng cookie của tài khoản thử. Không đưa token thô vào script hoặc báo cáo.

## Cách chạy từng ca

1. Đọc mã ca, điều kiện đầu vào, bước thực hiện và kết quả mong đợi trong file log. Chạy bằng browser context riêng khi ca cần trạng thái sạch; dùng `waitForURL`, locator và response/request listener thay cho sleep cố định.
2. Kiểm tra **UI + request/response + dữ liệu sau reload** khi ca có ghi dữ liệu. Kiểm tra HTTP status, `If-Match`/version, empty/loading/error state và owner boundary. Đối với API không có UI, chạy qua HTTP và ghi rõ `API PASS, FE GAP` nếu đúng thực tế.
3. Chỉ dùng `page.route` để kiểm tra lỗi 401/403/409/429/503; ghi rõ đó là **mock lỗi mạng**, không coi là BE thật đã trả lỗi. Sau ca phải gỡ route mock.
4. Ghi kết quả ngay vào dòng testcase trong `full-module-test-cases.md`: thời gian, trạng thái, URL/API, HTTP status, bằng chứng và issue. Đặt ảnh/trace trong `frontend/.agents/reports/screenshots/full-module/`; không ghi request/response body chứa email, mật khẩu, token, journal text, signed download URL hoặc dữ liệu người dùng thật.
5. Với `FAIL`, tạo mục issue ở cuối file log gồm bước tái hiện, kết quả thực tế, kỳ vọng, severity và bằng chứng. Với `GAP`, chỉ rõ API/FE nào thiếu hoặc chỗ nào còn số liệu tĩnh. Với `BLOCKED`, ghi điều kiện cần có để chạy lại.
6. Sau mỗi lô, cập nhật bảng tổng hợp. Không tuyên bố “module hoàn thành” nếu chỉ kiểm tra một màn hình hoặc một API. Kết thúc bằng bảng trạng thái từng module và các blocker release.

## Thứ tự đề nghị

M0 nền tảng → M1 auth/hồ sơ → M2 journal/check-in/safety → M3 AI → M4 dashboard/insight/report → M5 self-care → M6 knowledge/admin → M7 export/deletion/feedback → M8 hardening. Các ca phụ thuộc provider, tài khoản admin hoặc worker có thể `BLOCKED`, nhưng vẫn phải kiểm tra phần độc lập còn lại.

## Ghi chú phạm vi

- FE hiện có client API chưa chắc đã được màn hình gọi; phải xem Network và code trước khi kết luận tích hợp.
- Dashboard/Insight hiện có nhiều giá trị minh họa: nếu UI hiển thị số liệu cố định không đến từ response BE, ghi `GAP` hoặc `FAIL` theo kỳ vọng của ca; không ghi `PASS` chỉ vì trang render.
- Các tác vụ BE-only (migration, worker, provider, backup/restore, load test) không thể xác nhận bằng Playwright đơn thuần. Chạy test/command phù hợp nếu môi trường cho phép; nếu không, ghi `BLOCKED` hoặc `GAP` với bằng chứng code và checklist.
- Không sửa code ứng dụng trong vòng kiểm thử này. Nếu gặp bug, ghi issue và đề xuất cách sửa; không che lỗi bằng cách chỉnh testcase.
