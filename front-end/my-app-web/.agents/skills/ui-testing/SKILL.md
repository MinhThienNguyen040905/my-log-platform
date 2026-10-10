---
name: ui-testing
description: >-
  Use this skill to inspect, interact with, and execute automated UI/UX and responsive tests on the local frontend application (http://localhost:3000) using Playwright MCP.
---

# Frontend UI/UX Testing Skill with Playwright MCP

Quy trình chuẩn hướng dẫn Agent thực hiện kiểm thử tự động toàn diện giao diện Frontend của nền tảng MyLog trên trình duyệt thực tế thông qua Playwright MCP.

---

## 1. Điều Kiện Tiên Quyết & Xác Minh Local Dev Server
Trước khi bắt đầu bất kỳ thao tác trình duyệt nào:
1. **Kiểm tra cổng lắng nghe**:
   - Chạy lệnh kiểm tra cổng 3000:
     ```powershell
     Get-NetTCPConnection -LocalPort 3000 -State Listen -ErrorAction SilentlyContinue
     ```
   - Nếu chưa chạy, điều hướng tới thư mục frontend (`front-end/my-app-web` hoặc `frontend`) và khởi động bằng `npm run dev`.
2. **Kiểm tra phản hồi HTTP**:
   - Gửi yêu cầu HTTP kiểm tra mã trạng thái:
     ```powershell
     Invoke-WebRequest -Uri "http://localhost:3000" -UseBasicParsing
     ```
   - Xác nhận nhận về mã `200 OK` trước khi gọi Playwright MCP.

---

## 2. Điều Hướng Trình Duyệt Qua Playwright MCP (`browser_navigate`)
- Gọi công cụ điều hướng đến URL mục tiêu:
  - Homepage: `http://localhost:3000`
  - Đăng nhập: `http://localhost:3000/auth/login`
  - Đăng ký: `http://localhost:3000/auth/register`
  - Onboarding: `http://localhost:3000/onboarding`
- Đợi trang tải hoàn tất các component client và trạng thái hydration của Next.js/React.

---

## 3. Khảo Sát & Trích Xuất DOM (`browser_snapshot` & `browser_find`)
- Chụp ảnh chụp kiến trúc cây DOM hoặc accessibility tree bằng `browser_snapshot`.
- Định vị các nút bấm trọng yếu, tiêu đề (heading), input field và thanh điều hướng chính.
- Kiểm tra các thuộc tính trợ năng (`aria-label`, `role`, `data-testid`).

---

## 4. Chụp Ảnh Màn Hình Minh Chứng (`browser_take_screenshot`)
- Chụp ảnh màn hình ở trạng thái trang đầy đủ hoặc từng khung nhìn viewable:
  - Định dạng: `.png`
  - Lưu vào thư mục minh chứng: `front-end/my-app-web/.agents/reports/screenshots/`
  - Đặt tên tệp theo quy ước: `<route>_<viewport>_<timestamp>.png` (ví dụ: `home_desktop_1440.png`, `login_mobile_375.png`).

---

## 5. Kiểm Thử Responsive Đa Độ Phân Giải (`browser_resize`)
Thay đổi kích thước cửa sổ trình duyệt và kiểm tra 3 mốc tiêu chuẩn:

1. **Desktop**: `1440 × 900`
   - Bố cục 2-3 cột, hiển thị đầy đủ menu chính trên thanh Navbar.
2. **Tablet**: `768 × 1024`
   - Bố cục linh hoạt, lưới card co lại thành 2 cột, kiểm tra không vỡ lề.
3. **Mobile**: `375 × 812` (iPhone X/13/15 chuẩn)
   - Chuyển đổi sang giao diện di động: menu rút gọn, CTA trải rộng vừa ngón tay.
   - **Quy tắc vàng**: Đảm bảo `document.documentElement.scrollWidth <= window.innerWidth` (không bị tràn ngang).

---

## 6. Kiểm Thử Tương Tác Biểu Mẫu & Nút Bấm An Toàn (`browser_click`, `browser_fill_form`)
- **Nguyên tắc an toàn (Non-destructive)**:
  - Chỉ điền dữ liệu giả lập (`test_dummy@example.com`, `pass123`) vào các trường input.
  - Kiểm tra hành vi validation tại chỗ (onBlur / onChange / onSubmit bị chặn bởi client schema Zod).
  - Thử nghiệm đóng/mở Modal hoặc Drawer thông báo (`NotificationModal`, `AiReflectionDrawer`).
  - **Tuyệt đối không gửi form làm thay đổi dữ liệu thật hoặc xóa tài khoản**.

---

## 7. Kiểm Thử Điều Hướng Tuyến Đường (Route Navigation)
- Thử nghiệm nhấp chuột vào các liên kết điều hướng trên thanh Navbar (ví dụ: Logo -> `/`, Nút "Đăng nhập" -> `/auth/login`, "Bắt đầu ngay" -> `/auth/register`).
- Đảm bảo URL trên thanh địa chỉ thay đổi tương ứng và giao diện render đúng trang đích mà không tải lại toàn trang (Next.js client-side navigation).

---

## 8. Chẩn Đoán Lỗi Trình Duyệt (`browser_console_messages` & `browser_network_requests`)
- **Console Inspection**:
  - Gọi `browser_console_messages` để phát hiện các lỗi `error` hoặc `warning`.
  - Đặc biệt chú ý lỗi: React Hydration mismatch, Uncaught TypeError, Next.js dynamic routing warning.
- **Network Inspection**:
  - Gọi `browser_network_requests` để phát hiện các yêu cầu thất bại (Status >= 400).
  - Kiểm tra các tài nguyên tĩnh bị thiếu: 404 cho hình ảnh, SVG icon, font Space Grotesk.

---

## 9. Phân Loại Mức Độ Nghiêm Trọng Của Lỗi (Bug Severity Matrix)
Mọi lỗi phát hiện được phải phân loại chính xác theo 4 mức độ:

| Mức độ | Định nghĩa & Tiêu chuẩn | Ví dụ |
| :--- | :--- | :--- |
| **Critical** | Chặn hoàn toàn trải nghiệm người dùng; crash ứng dụng; màn hình trắng (White Screen of Death); loop vô hạn. | Không thể tải trang; runtime crash trong React root layout. |
| **High** | Chức năng chính bị hỏng nghiêm trọng; form quan trọng không thể submit; lỗi điều hướng cốt lõi; tràn ngang làm mất nội dung trên mobile. | Bấm nút Đăng nhập không phản hồi; giao diện mobile vỡ hoàn toàn sang phải > 100px. |
| **Medium** | Lỗi hiển thị visual đáng chú ý; styling sai lệch chuẩn Neo-Brutalism; validation message hiển thị sai vị trí; icon thiếu/sai kích thước. | Màu nút CTA sai lệch `#B7FF32`; text bị che một phần; padding lệch chuẩn 16px. |
| **Low** | Vấn đề nhỏ về thẩm mỹ, viền, khoảng cách li ti, console warning không ảnh hưởng chức năng thực tế. | Warning về passive event listener trong console; thiếu hover transition ở footer link. |

---

## 10. Xuất Báo Cáo Kiểm Thử (Report Generation)
- Sau khi hoàn thành các bước kiểm thử, tổng hợp toàn bộ kết quả vào tệp:
  `front-end/my-app-web/.agents/reports/frontend-test-report.md`
- Tuân thủ cấu trúc 5 phần chuẩn:
  1. **Environment** (Framework, URL, Browser, MCP status).
  2. **Test Results Matrix** (Tên test, PASS/FAIL/BLOCKED, Chi tiết).
  3. **UI/UX Review** (Layout, Spacing, Typography, Colors, Neo-Brutalist elements).
  4. **Bug Report** (Severity, Các bước tái hiện, Thực tế vs Kỳ vọng, Đề xuất sửa chữa).
  5. **Final Summary** (Tổng số test, Tỷ lệ đạt, Khuyến nghị tiếp theo).
