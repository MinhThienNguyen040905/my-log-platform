# 11 — Kho kiến thức

> Spec là nguồn sự thật duy nhất về feature này. Khi feature thay đổi, cập nhật file này trong cùng commit/PR. Xem [quy ước](README.md).

> **Trạng thái spec:** điền từ code ngày 2026-11-20, **chờ xác nhận**.

Biên tập, duyệt và phát hành nội dung hỗ trợ; truy xuất đoạn trích có trích nguồn (RAG); sinh câu hỏi gợi mở từ nội dung đã duyệt.

- **Module backend:** `com.mylog.knowledge`
- **API gốc:** `/api/v1/admin/knowledge-items`, `/api/v1/recommendations`
- **Web:** [`features/knowledge`](../front-end/my-app-web/features/knowledge) (`AdminKnowledgePanel.tsx`), trang `app/(app)/admin`
- **Mobile:** —
- **Migration:** `V11`, `V13`, `V18` (trong [`db/migration`](../backend/src/main/resources/db/migration))
- **Tài liệu bổ trợ:** [`KNOWLEDGE_M6_API.md`](../backend/docs/KNOWLEDGE_M6_API.md), [`recommend-model/README.md`](../recommend-model/README.md)

## 1. Mục tiêu và phạm vi

Mọi nội dung hỗ trợ người dùng thấy phải do con người viết và duyệt, có nguồn rõ ràng. AI chỉ được dùng nội dung đã duyệt.

- **Trong phạm vi:** quy trình nháp → duyệt → phát hành → lưu trữ có phiên bản; chia đoạn và embedding; truy xuất theo chủ đề; câu hỏi gợi mở.
- **Ngoài phạm vi:** nguồn hỗ trợ khẩn cấp (thuộc [08-safety](08-safety/README.md)); công cụ chuẩn bị nội dung trong `recommend-model/` (chỉ là công cụ biên tập, không phải nguồn production).

## 2. Use case

### UC-27 — Biên tập và duyệt nội dung hỗ trợ

Catalog: UC-27 trong [`USE_CASE_CATALOG.md`](../backend/docs/USE_CASE_CATALOG.md) (bổ trợ, spec này được ưu tiên).

#### 1. Tác nhân

- **Biên tập viên** (vai trò `CONTENT_EDITOR`, quyền `knowledge:write`).
- **Người duyệt** (vai trò `CONTENT_APPROVER`, quyền `knowledge:review`).

#### 2. Tiền điều kiện và hậu điều kiện

- **Tiền điều kiện:** đã đăng nhập với vai trò phù hợp.
- **Hậu điều kiện:** phiên bản được duyệt có thể được truy xuất trong thời gian hiệu lực; mọi duyệt/từ chối/lưu trữ được ghi audit.

#### 3. Luồng chính

1. Biên tập viên tạo mục mới: `slug`, `topicCode`, `locale`, nguồn (tên, URL), nhóm phụ trách, tiêu đề, nội dung, thời gian hiệu lực → phiên bản 1 ở `DRAFT`.
2. Biên tập viên sửa nháp (`PATCH`), hoặc tạo phiên bản mới từ mục đã có (`POST .../versions`).
3. Biên tập viên gửi duyệt (`:submit`) → `IN_REVIEW`.
4. Người duyệt **khác** người viết duyệt (`:approve`) → `APPROVED`, hoặc từ chối kèm mã lý do (`:reject`) → `REJECTED`.
5. Phiên bản `APPROVED` được chia đoạn (chiến lược `paragraph-800-v1`); nếu bật embedding, worker tạo vector lưu vào pgvector.
6. Khi cần gỡ, người duyệt lưu trữ (`:archive`) kèm mã lý do → `ARCHIVED`.

#### 4. Luồng phụ và ngoại lệ

- Người viết tự duyệt bài của mình: `409` "Version không thể được review bởi actor này."
- Sửa hoặc gửi duyệt phiên bản không ở `DRAFT`: `409`.
- Lưu trữ phiên bản chưa `APPROVED`, hoặc mã lý do sai (`[A-Z0-9_]{3,60}`): `400`.
- Thiếu quyền: `403`.
- Sửa nháp mà tiêu đề và nội dung không đổi: trả phiên bản cũ, không ghi gì.

#### 5. Business rule

- `slug`: `[a-z0-9]+(-[a-z0-9]+)*`, ≤ 160 ký tự. `topicCode`: `[A-Z0-9_]{1,48}`.
- Nội dung có checksum SHA-256 để phát hiện thay đổi.
- Bài kiến thức cho thẻ kiểu suy nghĩ dùng `topicCode` dạng `TP_<CODE>`, tiếng Việt ([06-thinking-patterns](06-thinking-patterns.md)).

#### 6. API liên quan

Xem mục 4 bên dưới.

#### 7. Giao diện web và mobile

- **Web:** `AdminKnowledgePanel` trong trang quản trị.
- **Mobile:** không có (chỉ dành cho quản trị).

#### 8. Tiêu chí chấp nhận

- [x] Quy trình duyệt, truy xuất có trích nguồn, ma trận quyền quản trị (`M6IntegrationTest`).
- [x] Cấu hình embedding đòi credentials, phê duyệt và số chiều hỗ trợ (`EmbeddingPropertiesTest`); provider Gemini chuẩn hóa vector, từ chối endpoint không TLS (`GeminiEmbeddingProviderTest`); vector hợp lệ (`JpaKnowledgeEmbeddingStoreTest`).
- [ ] Eval truy xuất bằng bộ câu hỏi chọn sẵn.
- [ ] Đánh giá grounding và an toàn đầu ra của câu hỏi gợi mở.

## 3. Business rule và phân quyền chung

**Trạng thái phiên bản:** `DRAFT` → `IN_REVIEW` → `APPROVED` → `ARCHIVED`; hoặc `IN_REVIEW` → `REJECTED`.

**Truy xuất** (`ApprovedKnowledgeRetriever.java`)

- Chỉ phiên bản `APPROVED` đang trong thời gian hiệu lực, đúng `topicCode` và `locale`; tối đa 10 đoạn (API gợi ý dùng 3).
- Nếu bật embedding: tìm theo vector với truy vấn "recommendations for topic <topicCode>"; không có kết quả hoặc lỗi → **dự phòng** đọc thẳng từ danh mục đã duyệt.
- `locale` lấy từ hồ sơ người dùng (riêng chủ đề `RECOMMEND_MODEL_DOCS` đổi `vi` thành `vi-VN`).

**Câu hỏi gợi mở** (`GeminiReflectiveQuestionGenerator`)

- Chỉ chạy khi `mylog.rag.embedding.provider=gemini`, có API key và cờ `provider-data-approved`; nếu không, không có bộ sinh và API trả `503`.
- Đầu vào chỉ là đoạn trích đã duyệt (không có nhật ký người dùng); tối đa 3 câu, đầu ra phải đúng định dạng đánh số, ≤ 2.048 ký tự.

## 4. API contract

| Method | Path | Quyền | Request | Response | Lỗi |
|---|---|---|---|---|---|
| POST | `/admin/knowledge-items` | `knowledge:write` | `slug`, `topicCode`, `locale`, `sourceName`, `sourceUrl?`, `ownerTeam`, `title`, `content`, `effectiveFrom?`, `effectiveTo?` | `201` Version | 400, 403 |
| GET | `/admin/knowledge-items/{itemId}/versions/{version}` | write hoặc review | — | `200` Version | 403, 404 |
| POST | `/admin/knowledge-items/{itemId}/versions` | `knowledge:write` | `title`, `content`, `effectiveFrom?`, `effectiveTo?` | `200` Version mới | 400, 403 |
| PATCH | `/admin/knowledge-items/{itemId}/versions/{version}` | `knowledge:write` | `title`, `content` | `200` | 403, 409 |
| POST | `.../versions/{version}:submit` | `knowledge:write` | — | `200` | 403, 409 |
| POST | `.../versions/{version}:approve` | `knowledge:review` | — | `200` | 403, 409 |
| POST | `.../versions/{version}:reject` | `knowledge:review` | `reasonCode` | `200` | 400, 403, 409 |
| POST | `.../versions/{version}:archive` | `knowledge:review` | `reasonCode` | `200` | 400, 403 |
| GET | `/recommendations` | đăng nhập | `topicCode` | `200 {topicCode, excerpts: [{text, citation: {itemId, versionId, version, chunkId, sourceName, sourceUrl}}]}` | 400 |
| POST | `/recommendations/questions` | đăng nhập | `topicCode` | `200 {topicCode, questions[], message}` | 400, 503 |

**Version:** `{itemId, versionId, version, slug, topicCode, locale, title, content, status, checksum, chunkStrategyVersion, effectiveFrom, effectiveTo}`. Đường dẫn tính từ `/api/v1`.

## 5. Dữ liệu

| Bảng | Vai trò | Migration |
|---|---|---|
| `knowledge_items` | Mục kiến thức: slug, chủ đề, ngôn ngữ, nguồn, nhóm phụ trách | V11 |
| `knowledge_versions` | Phiên bản: tiêu đề, nội dung, checksum, trạng thái, người tạo/duyệt, hiệu lực | V11, V13 |
| `knowledge_chunks` | Đoạn đã chia của phiên bản | V11 |
| `knowledge_embeddings` | Vector (pgvector) theo model/phiên bản/số chiều | V11, V18 |
| `journal_prompts` | Câu gợi ý viết (`DRAFT`/`PUBLISHED`/`ARCHIVED`) | V11 |

V13 cho phép xóa tác giả khỏi phiên bản khi tài khoản bị xóa (giữ nội dung).

## 6. Safety, quyền riêng tư và consent

- Nội dung kiến thức không chứa dữ liệu người dùng; gửi đoạn trích đã duyệt tới Gemini không làm lộ nhật ký.
- Câu hỏi gợi mở do AI sinh **chưa qua sàng lọc an toàn đầu ra** và không được thay thế phản hồi safety.

## 7. Trạng thái triển khai

| Phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đã kiểm chứng | `M6IntegrationTest` và các test embedding |
| Web | đang làm | Có panel quản trị; API gợi ý có client nhưng chưa có màn hình người dùng |
| Mobile | chưa làm | |
| Nội dung | chưa có | 42 card trong `recommend-model/` và bộ `tp-v1` đều đang chờ duyệt |

## 8. Câu hỏi mở

1. `journal_prompts` có trong schema nhưng không có code nào dùng. Giữ hay bỏ?
2. Câu hỏi gợi mở RAG cần sàng lọc đầu ra trước khi hiển thị cho người dùng?
3. Truy vấn vector luôn là "recommendations for topic X", không dựa vào ngữ cảnh người dùng, nên kết quả gần như cố định theo chủ đề. Đây có phải ý định?
4. Chủ đề đặc biệt `RECOMMEND_MODEL_DOCS` (bộ cũ, sắp archive theo README của `recommend-model/`) còn được xử lý riêng trong `RecommendationService`. Gỡ khi archive?
5. Ai sẽ dùng `recommend-model/tools/import-knowledge.mjs` để nhập nội dung, và quy trình duyệt nội dung chuyên môn (người có chuyên môn sức khỏe tinh thần) ra sao?
