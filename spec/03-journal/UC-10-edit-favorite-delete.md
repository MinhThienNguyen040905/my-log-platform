# UC-10 — Sửa, đánh dấu và xóa nhật ký

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](../README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

- **Feature:** [Nhật ký](README.md)
- **Catalog:** UC-10 trong [`USE_CASE_CATALOG.md`](../../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên)

File này gồm cả **quản lý nhãn**.

## 1. Tác nhân

Người dùng đã đăng nhập, là chủ bài viết.

## 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** bài tồn tại, chưa xóa, thuộc người dùng.
- **Hậu điều kiện (sửa):** `contentVersion` và `rowVersion` tăng 1; kết quả phân tích cũ hết hiệu lực (`ANALYSIS_OUTDATED`) hoặc bị chặn (`BLOCKED_BY_SAFETY`); outbox `JournalEntryChanged` và (nếu được phép) `JournalEntrySubmitted`.
- **Hậu điều kiện (xóa):** bài bị xóa mềm (`deleted_at`), ảnh của bài chuyển `DELETED`; outbox `JournalEntryDeleted`.
- **Hậu điều kiện (yêu thích):** `favorite` thay đổi, `rowVersion` tăng.

## 3. Luồng chính

**Sửa** (`JournalService.java:229-272`)

1. Client gửi `PATCH /journal-entries/{id}` với `If-Match: "<rowVersion>"` và các trường cần đổi.
2. Backend gộp với giá trị cũ, kiểm tra dữ liệu, sàng lọc an toàn lại, lưu, thay nhãn (nếu gửi `tagIds`), đồng bộ ảnh theo nội dung mới.
3. Trả `200` với bài mới và `ETag` mới.

**Xóa**

1. Client gửi `DELETE /journal-entries/{id}` với `If-Match`. Trả `204`.

**Yêu thích**

1. `PUT /journal-entries/{id}/favorite` để đánh dấu, `DELETE` để bỏ. Không cần `If-Match`. Trả bài mới.

**Nhãn**

1. Tạo nhãn: `POST /journal-tags {name, color?}`.
2. Gắn/bỏ nhãn cho bài: `PUT` / `DELETE /journal-entries/{id}/tags/{tagId}`, hoặc gửi cả danh sách `tagIds` khi tạo/sửa bài.
3. Xem nhãn: `GET /journal-tags`, `GET /journal-entries/{id}/tags`.

## 4. Luồng phụ và ngoại lệ

- `If-Match` lệch `rowVersion` (sửa ở nơi khác): `409` "Nhật ký đã được cập nhật ở nơi khác."
- Bài hoặc nhãn không thuộc người dùng: `404`.
- Ảnh bị bỏ khỏi nội dung khi sửa: chuyển `DELETED`.
- Tên nhãn rỗng, dài quá 40 ký tự, màu sai định dạng: `400`.
- Quá quota ghi: `429`.

## 5. Business rule

- Trường không gửi (`null`) khi sửa = giữ nguyên.
- Gửi `tagIds` khi sửa = thay toàn bộ nhãn của bài; không gửi = giữ nguyên.
- Tên nhãn chuẩn hóa NFKC, trim, ≤ 40 ký tự; so trùng không phân biệt hoa thường. Tạo nhãn trùng tên **không báo lỗi** mà trả về nhãn có sẵn (màu cũ giữ nguyên). Màu dạng `#RRGGBB` hoặc `#RRGGBBAA`.
- Xóa là xóa mềm; dữ liệu bị xóa thật khi "bắt đầu lại" hoặc xóa tài khoản ([12-export-deletion](../12-export-deletion/README.md)).

## 6. API liên quan

| Method | Path | Request | Response | Lỗi |
|---|---|---|---|---|
| PATCH | `/api/v1/journal-entries/{id}` | `If-Match` + body | `200` + `ETag` | 400, 404, 409, 429 |
| DELETE | `/api/v1/journal-entries/{id}` | `If-Match` | `204` | 404, 409, 429 |
| PUT / DELETE | `/api/v1/journal-entries/{id}/favorite` | — | `200` JournalEntry | 404, 429 |
| POST | `/api/v1/journal-tags` | `name`, `color?` | `201` Tag (nhãn có sẵn nếu trùng tên) | 400, 429 |
| GET | `/api/v1/journal-tags` | — | `200 [Tag]` | |
| GET | `/api/v1/journal-entries/{id}/tags` | — | `200 [Tag]` | 404 |
| PUT / DELETE | `/api/v1/journal-entries/{id}/tags/{tagId}` | — | `204` | 404 |

## 7. Giao diện web và mobile

- **Web:** sửa trong editor; nhãn qua `AddTopicModal`; yêu thích và xóa trong danh sách/chi tiết.
- **Mobile:** sửa, yêu thích, xóa, tạo/gắn/bỏ nhãn trong `(app)/journal-entry/[id].tsx` và editor.

## 8. Tiêu chí chấp nhận

- [x] Nhãn được mã hóa và mọi tài nguyên HTTP chỉ trong phạm vi chủ sở hữu (`tagsAreEncryptedAndEveryHttpResourceIsOwnerScoped`).
- [x] Trả nhãn của bài (`JournalTagControllerTest`).
- [x] Mobile: CRUD, ETag, `409`, yêu thích, nhãn (script `e2e-api-smoke.mjs`, `e2e-expo-web.mjs`).
- [ ] Test backend riêng cho xóa làm ảnh chuyển `DELETED`.

## 9. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `JournalCheckinIntegrationTest`, `JournalTagControllerTest` |
| Web | xong | |
| Mobile | xong | Chưa thử xung đột sửa đồng thời trên thiết bị thật |

## 10. Câu hỏi mở

- Có cần sửa/xóa nhãn (đổi tên, đổi màu, xóa nhãn) không? Hiện backend chỉ có tạo và gắn/bỏ.
- Có cho khôi phục bài đã xóa (thùng rác) không?
