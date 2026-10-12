# MyLog Web

Ứng dụng web Next.js của MyLog. Các trang đăng ký, xác minh email và đăng nhập gọi backend qua các route `/api/auth/*` của Next.js. Nhật ký đã gọi backend để lưu và tải bài viết; bản nháp đang viết có thể lưu tạm trong browser `localStorage`.

## Chạy local

1. Chạy backend tại `http://localhost:8080` theo [hướng dẫn backend](../../backend/README.md).
2. Trong thư mục này, chạy `npm install` và `npm run dev`.
3. Mở `http://localhost:3000`.

Nếu backend ở địa chỉ khác, cấu hình `MYLOG_BACKEND_URL` cho Next.js. Backend cần `MYLOG_VERIFICATION_URL` trỏ tới `https://<web-host>/auth/verify-email` ở staging/production; local có thể dùng `http://localhost:3000/auth/verify-email`.

Khi thử nghiệm local, mở `/dev/users` để xem email của các tài khoản đang hoạt động. Nhập khóa `MYLOG_DEV_USERS_KEY` đã cấu hình trong `backend/.env`; trang và route `/api/dev/users` chỉ hoạt động khi Next.js chạy `npm run dev`. Khóa không được lưu trong trình duyệt. Backend phải chạy profile `local`; xem [hướng dẫn backend](../../backend/README.md).

## Đăng ký và xác minh email

1. `/auth/register` gửi thông tin đăng ký tới `POST /api/v1/auth/register`. Backend lưu lượt đăng ký tạm và gửi email; tài khoản thật chỉ được tạo khi xác minh thành công. Người dùng có thể đăng ký lại bằng cùng email nếu chưa xác minh; lần mới thay mã/link và thông tin tạm. Web giữ tạm email đã nhập trong `sessionStorage` tối đa 30 phút rồi mở trang xác minh, nên form không yêu cầu nhập lại email và vẫn hoạt động khi tải lại trang. Trang đăng nhập cũng dùng cách này khi người dùng chọn xác minh bằng mã. Email được xóa khỏi `sessionStorage` sau khi xác minh thành công.
2. Người dùng nhập mã **8 chữ số** từ email vào 8 ô. Web gửi `{ email, code }` tới `POST /api/v1/auth/email-verifications:confirm-code`. Mã có hiệu lực 10 phút.
3. Người dùng cũng có thể mở liên kết trong email, sau đó bấm nút xác nhận trên `/auth/verify-email`. Web gửi token của liên kết tới `POST /api/v1/auth/email-verifications:confirm`. Liên kết có hiệu lực 24 giờ; mở trang bằng GET không tự xác minh.
4. Nút gửi lại gọi `POST /api/v1/auth/email-verifications` với email đã biết. Liên kết và mã cũ hết hiệu lực khi gửi lại. Nếu mở trang xác minh trực tiếp mà không có email trong phiên, người dùng cần mở liên kết trong thư hoặc nhập email ở trang đăng nhập trước khi chọn xác minh bằng mã.
5. Xác minh thành công chỉ kích hoạt tài khoản. Người dùng đăng nhập riêng tại `/auth/login`.

Token của liên kết được xóa khỏi thanh địa chỉ sau khi trang tải và không được lưu trong browser storage. Đường dẫn xác minh dùng `Referrer-Policy: no-referrer` và `Cache-Control: no-store`. Cần tránh ghi URL chứa token vào access log và analytics khi triển khai.

## Kiểm tra

```bash
npm run lint
npm run build
```

## Trạng thái nhật ký và AI

Trang nhật ký lưu bài viết trên backend; `localStorage` chỉ được dùng cho bản nháp chưa lưu và dữ liệu cũ để nhập lại. Cần đánh giá cách lưu bản nháp nhạy cảm này trước khi dùng production. Backend sở hữu kiểm tra an toàn bản nháp và gợi ý viết tại `POST /api/v1/journal-writing-suggestions`; web chưa gọi endpoint gợi ý viết đó. Xem [AI Analysis Guide](../../backend/docs/AI_ANALYSIS_GUIDE.md).
