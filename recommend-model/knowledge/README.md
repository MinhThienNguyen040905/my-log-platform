# Kho kiến thức mylog (bản làm lại)

Thư mục này là nguồn nội dung cho kho kiến thức RAG từ tháng 11/2026, thay cho corpus cũ `RECOMMEND_MODEL_DOCS` (3 PDF + card JSON trong `../docs`, nhập bằng `../tools/EmbedAllDocsToPgvector.java`). Corpus cũ được **archive**, không xóa. **Không chạy lại `EmbedAllDocsToPgvector`**: importer đó tự đặt nội dung thành `APPROVED`, còn kho mới bắt buộc qua bước duyệt của người.

Kế hoạch tính năng dùng kho này: [`backend/docs/THINKING_PATTERN_RECOGNITION_PLAN.md`](../../backend/docs/THINKING_PATTERN_RECOGNITION_PLAN.md).

## Cấu trúc

| Thư mục | Nội dung |
|---|---|
| `tp-v1/` | 19 bài nháp, mỗi bài một kiểu suy nghĩ/thói quen trong danh mục `tp-v1`, `topicCode = TP_<MÃ>` |
| `extra/` | Bài bổ sung từ tài liệu bạn cung cấp, viết lại theo cùng khuôn |

Mỗi file là markdown có front matter:

```markdown
---
slug: tp-catastrophizing-vi          # chữ thường, gạch nối; duy nhất
topicCode: TP_CATASTROPHIZING        # [A-Z0-9_], tối đa 48 ký tự
locale: vi
title: Đoán trước điều tệ nhất
sourceName: Nhóm nội dung mylog (...)
sourceUrl:                           # để trống hoặc https://
ownerTeam: mylog-content
taxonomyVersion: tp-v1
reviewStatus: draft
---
```

Thân bài bắt buộc đủ 6 mục `##`: Đây là gì · Nhận ra qua những câu như · Vì sao nhiều người gặp · Thử từng bước (3 tuần) · Khi nào nên tìm thêm hỗ trợ · Nguồn.

Khi nhập, **mỗi mục (trừ Nguồn) thành đúng một đoạn**, tức một chunk để embed. Mục Nguồn được rút gọn vào `sourceName`. Mỗi mục nên dưới 800 ký tự; dài hơn, backend sẽ cắt đôi.

## Thêm tài liệu của bạn vào `extra/`

1. Kiểm tra quyền sử dụng của tài liệu gốc. Không chép nguyên văn sách hoặc bài có bản quyền; viết lại bằng lời của mylog và ghi nguồn.
2. Tạo một file `.md` theo khuôn trên. Nếu bài thuộc một mã trong danh mục, dùng `topicCode` đó; nếu là chủ đề mới (ví dụ kỹ thuật thở), đặt mã mới không có tiền tố `TP_` và ghi vào danh sách câu hỏi mở của kế hoạch.
3. Không đưa nội dung nhật ký thật, tên người thật hay thông tin cá nhân vào ví dụ.
4. PDF chưa được nhập trực tiếp: cần tóm tắt và viết lại thành bài theo khuôn.

## Quy trình nhập và duyệt

Chạy từ thư mục gốc repository.

### 1. Chuẩn bị hai tài khoản

Backend yêu cầu **người tạo và người duyệt là hai tài khoản khác nhau** ([KNOWLEDGE_M6_API.md](../../backend/docs/KNOWLEDGE_M6_API.md)):

- Tài khoản biên tập có role `CONTENT_EDITOR` (quyền `knowledge:write`).
- Tài khoản duyệt có role `CONTENT_APPROVER` (quyền `knowledge:review`), lý tưởng là người có chuyên môn tâm lý.

Gán role bằng câu SQL bootstrap trong KNOWLEDGE_M6_API.md. Thêm thông tin đăng nhập vào `backend/.env` (đã gitignore), không commit:

```properties
MYLOG_KNOWLEDGE_EDITOR_EMAIL=
MYLOG_KNOWLEDGE_EDITOR_PASSWORD=
MYLOG_KNOWLEDGE_APPROVER_EMAIL=
MYLOG_KNOWLEDGE_APPROVER_PASSWORD=
```

### 2. Kiểm tra (không gọi API)

```powershell
node recommend-model/tools/import-knowledge.mjs
```

Script kiểm tra front matter, đủ 6 mục, slug trùng, độ dài đoạn, và đánh dấu bài có nguồn chưa xác minh.

### 3. Nhập và gửi duyệt

Backend phải đang chạy (`MYLOG_API_URL`, mặc định `http://localhost:8080`).

```powershell
node recommend-model/tools/import-knowledge.mjs --apply
```

Mỗi bài được tạo ở trạng thái `DRAFT` rồi submit sang `IN_REVIEW` (backend cắt đoạn lúc submit). Script lưu `slug → itemId/version/checksum` ở `scratch/knowledge-import/state.json` (gitignored):

- Chạy lại mà file không đổi: bỏ qua.
- File đã sửa: tạo **version mới** và gửi duyệt lại; nội dung đã duyệt không bị sửa tại chỗ.

### 4. Duyệt

Màn admin tìm bài theo Item ID; script đã ghi danh sách slug/tiêu đề/Item ID/version ở `scratch/knowledge-import/review-list.md`. Người duyệt đăng nhập màn admin (Quản trị → Kiến thức), dán từng Item ID, đọc bài, **xác minh mục Nguồn**, rồi approve hoặc reject. Bài nháp hiện ghi "Cần người duyệt xác minh nguồn và bổ sung URL https trước khi duyệt": người duyệt không nên approve khi chưa làm việc đó.

Sau khi approve, `KnowledgeEmbeddingWorker` của backend tự embed các đoạn còn thiếu vector (cần `MYLOG_RAG_EMBEDDING_ENABLED=true`, cờ duyệt provider và chạy ở profile không phải `api`).

### 5. Archive corpus cũ

Lấy danh sách version đang `APPROVED` của corpus cũ bằng truy vấn **chỉ đọc** (Supabase SQL editor), xuất CSV có header `item_id,version`:

```sql
SELECT v.item_id, v.version
FROM knowledge_versions v
JOIN knowledge_items i ON i.id = v.item_id
WHERE i.topic_code = 'RECOMMEND_MODEL_DOCS' AND v.status = 'APPROVED'
ORDER BY v.item_id;
```

Lưu file vào `scratch/` (gitignored), rồi:

```powershell
node recommend-model/tools/import-knowledge.mjs --archive-legacy scratch/legacy-knowledge.csv          # xem trước
node recommend-model/tools/import-knowledge.mjs --archive-legacy scratch/legacy-knowledge.csv --apply  # archive
```

Archive dùng tài khoản duyệt, `reasonCode = RETIRED`, có ghi audit. Nội dung và chunk cũ vẫn nằm trong database để giữ lịch sử trích dẫn nhưng không còn được truy xuất. Nên archive **sau khi** các bài mới đã được duyệt, để kho không bị trống giữa chừng.
