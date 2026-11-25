# UC-11 — Thêm ảnh vào nhật ký

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Nhật ký](README.md)
- **Catalog:** UC-11 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

## 1. Tác nhân

Người dùng đang viết hoặc sửa nhật ký.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đã đăng nhập; chưa vượt quota dung lượng và số lần tải.
- **Hậu điều kiện (tải lên):** ảnh đã làm sạch được lưu, bản ghi `journal_assets` trạng thái `PENDING_ATTACH`, chưa gắn bài.
- **Hậu điều kiện (lưu bài):** ảnh có trong nội dung chuyển `READY`, có `position` theo thứ tự xuất hiện, ảnh đầu tiên là ảnh bìa.

## 3. Luồng chính

1. Người dùng chọn ảnh trong editor.
2. Client gửi `POST /journal-assets/upload` (multipart `file`).
3. Backend (`JournalAssetService.java`, `ImageInspectionService.java`):
   1. kiểm tra quota dung lượng và số lần tải;
   2. nhận diện định dạng thật theo nội dung file (không tin đuôi file);
   3. kiểm tra kích thước file và số điểm ảnh;
   4. ghi lại ảnh để loại bỏ metadata (JPEG, PNG); WebP giữ nguyên bytes;
   5. lưu vào kho, tạo bản ghi `PENDING_ATTACH`;
   6. trả `201 {id, format, mimeType, width, height, sizeBytes, url}` với URL có chữ ký sống 1 giờ.
4. Client chèn node ảnh (mang `assetId`) vào nội dung TipTap.
5. Khi lưu bài (UC-08/UC-10), backend đọc các `assetId` trong nội dung và gắn chúng vào bài.
6. Khi đọc bài, backend tạo URL có chữ ký mới cho từng ảnh; client có thể làm mới bằng `POST /journal-assets/urls` hoặc `GET /journal-assets/{id}/url`.

## 4. Luồng phụ và ngoại lệ

- File rỗng, > 5 MB, > 25 megapixel, không phải JPEG/PNG/WebP hoặc bị giả mạo: `400`.
- Tổng dung lượng ảnh của user vượt 200 MB (`mylog.assets.max-user-quota-bytes`): `400`.
- Quá 30 ảnh/giờ: `400` "Đã vượt quá giới hạn tải lên tối đa 30 ảnh mỗi giờ."
- Ảnh đã xóa: `404` khi xin URL; `410` khi tải qua URL local.
- Ảnh tải lên nhưng không được gắn bài trong **72 giờ**: job dọn dẹp (04:20 UTC hằng ngày) xóa file và đánh dấu `DELETED`. Bản ghi `DELETED` quá 30 ngày bị xóa hẳn (`OrphanJournalAssetCleanupJob.java`).

## 5. Business rule

- Tối đa 20 ảnh mỗi bài.
- Kho lưu: Cloudinary khi `mylog.cloudinary.enabled=true` (ADR-0006); nếu không, lưu file local và phục vụ qua `GET /journal-assets/raw/{id}?expires=&sig=` (chữ ký HMAC, `Cache-Control: private, max-age=900`).
- Mọi thao tác lọc theo chủ sở hữu; không thể gắn ảnh của người khác hoặc ảnh đã gắn bài khác.

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| POST | `/api/v1/journal-assets/upload` | multipart `file` | `201` | 400, 401 |
| POST | `/api/v1/journal-assets/urls` | `{assetIds}` | `200 {urls}` | 400 |
| GET | `/api/v1/journal-assets/{assetId}/url` | — | `200 {url}` | 404 |
| GET | `/api/v1/journal-assets/raw/{assetId}` | `expires`, `sig` | `200` ảnh | 403, 404, 410 |

## 7. Giao diện web và mobile

- **Web:** `InsertImageModal`, extension `journal-image` cho TipTap; route proxy `app/api/journal-assets/*`.
- **Mobile:** chưa có.

## 8. Tiêu chí chấp nhận

- [x] Kiểm tra định dạng, kích thước, làm sạch ảnh (`ImageInspectionServiceTest`).
- [x] Lưu local và chữ ký URL (`LocalFileAssetStorageTest`).
- [x] Web: chèn ảnh (`journal-image-insert.spec.ts`).
- [ ] Test tích hợp HTTP cho upload và gắn ảnh vào bài (owner, ảnh đã gắn bài khác, ảnh hết hạn).
- [ ] Test cho job dọn ảnh mồ côi.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | xong | Chỉ có unit test, thiếu test tích hợp HTTP |
| Web | đã kiểm chứng | Playwright |
| Mobile | chưa làm | |

## 10. Câu hỏi mở

- Trạng thái `PENDING_SCAN` và `REJECTED` có trong schema nhưng chưa có luồng quét nội dung ảnh. Có cần quét ảnh không?
- Giới hạn 30 ảnh/giờ chỉ trong bộ nhớ từng instance (xem [README](README.md#8-câu-hỏi-mở)).
