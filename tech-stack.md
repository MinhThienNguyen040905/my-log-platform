# Tech stack của mylog

Những quyết định kỹ thuật cốt lõi của dự án. Lý do chi tiết của từng quyết định nền tảng nằm trong ADR ở [`backend/docs/adr/`](backend/docs/adr/). Hành vi tính năng nằm trong [`spec/`](spec/mucluc.md).

## Tổng quan kiến trúc

```
Web (Next.js) ──► route /api của Next.js ──┐
                                            ├──► Backend Spring Boot ──► PostgreSQL + pgvector
Mobile (Expo) ─────────────────────────────┘          │                  Redis
                                                       ├──► Cloudinary (ảnh)
                                                       ├──► SMTP (email)
                                                       ├──► Dịch vụ safety classifier (Python)
                                                       └──► Provider AI (OpenAI / Gemini), mặc định tắt
```

- **Backend sở hữu** business rule, phân quyền, safety và mọi quyền truy cập dữ liệu. Client chỉ gọi API theo contract.
- Web không gọi backend trực tiếp từ trình duyệt: các route `/api` của Next.js làm proxy và giữ token trong cookie `httpOnly`.
- Mobile gọi thẳng backend, lưu token và bản nháp trong SecureStore.

## Backend — [`backend/`](backend/)

| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ, framework | Java 21, Spring Boot 4.1 (Web MVC, Security, Validation, Actuator), Maven Wrapper |
| Kiến trúc | Modular monolith theo feature (`com.mylog.<feature>`), mỗi feature chia `api / application / domain / infrastructure`; ArchUnit kiểm tra ranh giới |
| Database | PostgreSQL 17 + pgvector, truy cập bằng JPA/Hibernate (native SQL khi cần tính năng riêng của PostgreSQL) |
| Schema | Flyway; migration đã áp dụng là bất biến, mọi thay đổi là migration mới |
| Cache, rate limit, lock | Redis 8; không dùng làm hàng đợi duy nhất cho dữ liệu quan trọng |
| Xác thực | Email/mật khẩu (bcrypt), Google OAuth; access JWT 10 phút, refresh token ngẫu nhiên chỉ lưu hash, có xoay vòng và phát hiện tái sử dụng (ADR-0001) |
| Mã hóa | Envelope encryption AES-256-GCM ở tầng ứng dụng cho nhật ký, email, hồ sơ, nhãn...; tra cứu bằng HMAC (ADR-0002) |
| Xử lý bất đồng bộ | Transactional outbox + worker claim bằng lease/`FOR UPDATE SKIP LOCKED`; payload chỉ chứa id và version |
| Ảnh | Cloudinary với URL có chữ ký (ADR-0006); local file có chữ ký HMAC khi dev |
| Export | CSV và PDF (PDFBox), artifact mã hóa trong PostgreSQL, tối đa 5 MB, sống 24 giờ (ADR-0007) |
| Email | Spring Mail qua SMTP; Mailpit khi chạy local |
| API | REST `/api/v1`, JSON camelCase, lỗi `application/problem+json`, `If-Match`/`ETag` cho cập nhật, `Idempotency-Key` cho tạo mới, phân trang bằng cursor; OpenAPI qua springdoc |
| Test | JUnit 5, Testcontainers với PostgreSQL thật (không dùng H2), ArchUnit, OWASP dependency-check |

## Web — [`front-end/my-app-web/`](front-end/my-app-web/)

| Thành phần | Lựa chọn |
|---|---|
| Framework | Next.js 16 (App Router), React 19, TypeScript |
| Giao diện | Tailwind CSS, lucide-react, motion, Recharts (biểu đồ), react-day-picker (lịch) |
| Editor nhật ký | TipTap 3 (nội dung lưu dạng JSON TipTap) |
| Dữ liệu và form | TanStack Query, React Hook Form + Zod |
| Đa ngôn ngữ | i18next / react-i18next |
| Test | Playwright |

## Mobile — [`front-end/my-app-mobile/`](front-end/my-app-mobile/)

| Thành phần | Lựa chọn |
|---|---|
| Framework | Expo 57 (expo-router), React Native 0.86, React 19, TypeScript |
| Lưu trữ an toàn | expo-secure-store cho token và bản nháp |
| Tệp và chia sẻ | expo-file-system, expo-sharing (export) |
| Test | Script smoke API và Playwright trên Expo web; chưa có test trên thiết bị |

## AI và safety

| Thành phần | Lựa chọn |
|---|---|
| Kiến trúc | Lai (ADR-0009): classifier safety nội bộ + phân tích nhật ký qua provider API, chạy trong worker, không trên request path |
| Safety | Nhiều lớp: validate → luật soạn sẵn → classifier → policy có phiên bản; lỗi thì chọn hướng an toàn (ADR-0003) |
| Classifier | [`safety-model/`](safety-model/): Python, scikit-learn, FastAPI/uvicorn; baseline, chưa được duyệt cho dữ liệu thật |
| Provider sinh nội dung | Port ở tầng application, adapter ở infrastructure; có adapter OpenAI và Gemini; mặc định `none` và mọi tính năng AI tắt bằng cấu hình (ADR-0004) |
| RAG | Embedding (mặc định Gemini) lưu trong pgvector; kho kiến thức phải qua bước duyệt của biên tập viên |
| Consent | Kiểm tra consent (`AI_PROCESSING`, `THINKING_PATTERNS`) **tại lúc xử lý**, không chỉ lúc đưa vào hàng đợi |
| Thử nghiệm nội dung | [`recommend-model/`](recommend-model/): công cụ biên tập và thử nghiệm knowledge card, không phải nguồn production |

## Hạ tầng và vận hành

| Thành phần | Lựa chọn |
|---|---|
| Chạy local | Docker Compose: `pgvector/pgvector:pg17`, `redis:8-alpine`, `axllent/mailpit` |
| Database production | PostgreSQL (Supabase được hỗ trợ); Data API của Supabase bị khóa, chỉ backend truy cập DB (ADR-0008) |
| Đóng gói | Dockerfile cho backend và safety-model |
| Chế độ chạy | Backend tách được chế độ API và worker; profile `migrate` chạy Flyway một lần |

## Quy ước bắt buộc xuyên suốt

- Không log, commit hoặc đưa vào fixture: nội dung nhật ký, email, token, mật khẩu, khóa, raw prompt/response, URL có chữ ký. Chỉ dùng dữ liệu synthetic.
- Mọi truy vấn dữ liệu cá nhân lọc theo `resourceId + currentUserId`.
- Thời gian lưu UTC; ngày của người dùng lưu kèm `local_date` và timezone.
- Đổi contract thì cập nhật cả bên dùng, spec và tài liệu trong cùng thay đổi.

Chi tiết quy ước theo từng phần: [`AGENTS.md`](AGENTS.md), [`backend/AGENTS.md`](backend/AGENTS.md), [`front-end/my-app-web/AGENTS.md`](front-end/my-app-web/AGENTS.md), [`front-end/my-app-mobile/AGENTS.md`](front-end/my-app-mobile/AGENTS.md).
