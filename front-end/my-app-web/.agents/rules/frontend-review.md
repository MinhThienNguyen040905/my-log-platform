# Quy Chuẩn Đánh Giá và Kiểm Thử Giao Diện Frontend (frontend-review)

Tài liệu này định nghĩa các quy tắc bắt buộc cho AI Agent khi đánh giá, kiểm thử và can thiệp vào tầng Frontend (`frontend/` hoặc `front-end/my-app-web/`) của nền tảng **MyLog**.

---

## 1. Nguyên Tắc Trực Quan (Visual Inspection First)
1. **Luôn kiểm tra trên trình duyệt thực tế trước khi đưa ra nhận định visual**:
   - Không suy đoán bố cục hay lỗi hiển thị chỉ dựa trên code JSX/Tailwind.
   - Phải sử dụng Playwright MCP (`browser_navigate`, `browser_snapshot`, `browser_take_screenshot`) để ghi nhận DOM và hình ảnh thực tế của trình duyệt.
2. **Minh chứng rõ ràng (Supporting Evidence)**:
   - Mọi lỗi visual, vỡ khung hay tràn lề phải kèm theo bằng chứng cụ thể: kích thước viewport (`1440x900`, `768x1024`, `375x812`), selector phần tử, thông số đo đạc (`bounding box`, `scrollWidth` vs `clientWidth`), và ảnh chụp màn hình thực tế.

---

## 2. Kiến Trúc & Thiết Kế (Design System & Architecture)
1. **Tuân thủ Feature-Based Architecture (Next.js App Router)**:
   - Giữ mã nguồn phân bổ theo `features/<feature-name>/{components, hooks, api, schemas}`.
   - Không đặt mã tùy tiện tại thư mục gốc hoặc dồn logic nghiệp vụ phức tạp vào `page.tsx`.
2. **Phong cách thiết kế chuẩn (Playful Neo-Brutalism + Editorial Collage)**:
   - **Bảng màu**: Background `#F5F5F3`, Surface `#FFFFFF`, Foreground/Border `#111111`, Accent Neon `#B7FF32`.
   - **Typography**: Space Grotesk kết hợp font editorial có chân cho tiêu đề lớn, độ tương phản cao, nét viền cứng đậm đặc trưng.
   - **Thành phần scrapbook**: Sticky notes, stickers, polaroid cards với góc xoay nhẹ và đổ bóng cứng (`box-shadow: 4px 4px 0 #111111`).
   - Giữ sự nhất quán nghiêm ngặt về spacing, border-width và button styling trong toàn bộ ứng dụng.

---

## 3. Kiểm Thử Responsive (Responsive & Layout Standards)
1. **Kiểm tra trên 3 dải độ phân giải tiêu chuẩn**:
   - **Desktop**: 1440 × 900 px
   - **Tablet**: 768 × 1024 px
   - **Mobile**: 375 × 812 px
2. **Tiêu chuẩn chống vỡ giao diện**:
   - **Không có thanh cuộn ngang ngoài ý muốn** (`document.body.scrollWidth <= window.innerWidth`).
   - Không bị cắt chữ (`text clipping` / `overflow ellipsis` bất thường).
   - Thanh điều hướng (Navbar) phải co giãn hợp lý: chuyển đổi mượt mà sang Mobile Drawer / Hamburger Menu trên màn hình nhỏ.
   - Đảm bảo vùng bấm (touch target) trên mobile tối thiểu 44 × 44 px cho các nút tương tác chính.

---

## 4. Bảo Toàn Nghiệp Vụ & Ranh Giới Dự Án
1. **Tuyệt đối không can thiệp backend**:
   - Không chỉnh sửa, xóa hoặc tạo file trong thư mục `backend/` trừ khi có chỉ đạo rõ ràng từ người dùng.
2. **Tránh over-engineering & refactor không cần thiết**:
   - Giữ nguyên các tính năng đang hoạt động bình thường. Không tự ý viết lại component nếu không nằm trong phạm vi sửa lỗi đã xác định.
3. **Thao tác tương tác an toàn (Non-destructive Testing)**:
   - Khi kiểm thử tương tác, chỉ thử nghiệm focus, click điều hướng, nhập form giả lập để kiểm tra validation.
   - Không nhấn nút xóa dữ liệu, submit form làm thay đổi dữ liệu thật của người dùng.
