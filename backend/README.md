# mylog backend

Backend modular monolith của **mylog**, được khởi tạo bằng Java 21 và Spring Boot 4.1.1.

Tài liệu thiết kế:

- [Backend architecture](docs/BACKEND_ARCHITECTURE.md)
- [AI analysis, safety và RAG](docs/AI_ANALYSIS_GUIDE.md)
- [Database design](docs/DATABASE_OVERVIEW.md)
- [Database DBML](docs/database/mylog.dbml)
- [Backend development plan](docs/BACKEND_DEVELOPMENT_PLAN.md)
- [Architecture decisions](docs/adr/README.md)

## Yêu cầu

- Java 21+
- Docker Desktop hoặc Docker Engine có Compose
- Không cần cài Maven; repository đã có Maven Wrapper

## Biến môi trường với `.env`

mylog dùng Spring Boot Externalized Configuration. File `application.yml` import trực tiếp file `.env`:

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

Khởi tạo cấu hình local:

```powershell
cd backend
Copy-Item .env.example .env
```

Repository đã có một `.env` local để chạy ngay trên máy hiện tại. File này bị Git ignore; chỉ `.env.example` được commit.

Thứ tự ưu tiên quan trọng của Spring Boot vẫn được giữ nguyên. Biến môi trường thật của hệ điều hành hoặc container có thể override giá trị đọc từ `.env`. Vì vậy production nên inject secret từ secret manager/container environment, không đóng gói `.env` vào image.

Các nhóm biến hiện có:

```text
SPRING_PROFILES_ACTIVE
MYLOG_APP_PROFILE
MYLOG_SERVER_PORT
MYLOG_DB_*
MYLOG_REDIS_URL
MYLOG_REDIS_SSL_ENABLED
MYLOG_ALLOWED_ORIGINS
MYLOG_OPENAPI_ENABLED
MYLOG_SWAGGER_UI_ENABLED
MYLOG_CLOUDINARY_*
MYLOG_JOBS_ENABLED
MYLOG_JOBS_POLL_DELAY_MS
MYLOG_AI_FAKE_ENABLED
MYLOG_RAG_EMBEDDING_*
MYLOG_INSIGHTS_MINIMUM_SAMPLES
MYLOG_REPORTS_ENABLED
```

Database local có thể dùng Docker Compose; môi trường được triển khai dùng PostgreSQL do Supabase quản lý qua `MYLOG_DB_URL`, `MYLOG_DB_USERNAME` và `MYLOG_DB_PASSWORD`. Ảnh nhật ký dùng Cloudinary; bật adapter bằng `MYLOG_CLOUDINARY_ENABLED=true` sau khi điền credential server-side.

Redis local trong Compose dùng `MYLOG_REDIS_URL=redis://localhost:6379` và `MYLOG_REDIS_SSL_ENABLED=false`. Với Upstash, sao chép URL từ **Connect → TCP** (bắt đầu bằng `rediss://`) vào `MYLOG_REDIS_URL` và đặt `MYLOG_REDIS_SSL_ENABLED=true`; giữ URL chứa token trong `.env` local hoặc secret manager, không commit. Hiện Redis chưa lưu nghiệp vụ của mylog.

## Chạy local

Nếu `.env` trỏ PostgreSQL tới Supabase, khởi động Redis và Mailpit để chạy identity/email verification:

```powershell
cd backend
docker compose up -d redis mailpit
```

Nếu muốn phát triển hoàn toàn offline bằng PostgreSQL/pgvector local, đổi `MYLOG_DB_*` về giá trị trong `.env.example` rồi chạy `docker compose up -d`. Các biến `MYLOG_LOCAL_DB_*` của Compose được tách riêng để credential Supabase không bị dùng cho container local.

Mailpit UI: `http://localhost:8025`. Nếu dùng PostgreSQL local thay Supabase, chạy `docker compose up -d` để khởi động cả ba service.

### Gửi email xác minh bằng Gmail trong profile local

Mặc định local gửi đến Mailpit, không cần tài khoản hay mật khẩu Gmail. Nếu muốn gửi email thật bằng Gmail, bật xác minh 2 bước trên tài khoản Google, tạo **mật khẩu ứng dụng** dành riêng cho mylog tại [Google App Passwords](https://myaccount.google.com/apppasswords), rồi thêm các dòng sau vào file `.env` **trên máy của bạn** (không commit, không gửi mật khẩu vào chat):

```properties
MYLOG_MAIL_HOST=smtp.gmail.com
MYLOG_MAIL_PORT=587
MYLOG_MAIL_USERNAME=<địa chỉ Gmail gửi thư>
MYLOG_MAIL_PASSWORD=<mật khẩu ứng dụng Google>
MYLOG_MAIL_FROM=<cùng địa chỉ Gmail ở trên>
MYLOG_MAIL_SMTP_AUTH=true
MYLOG_MAIL_STARTTLS_ENABLED=true
```

Ghi mật khẩu ứng dụng liền nhau, không có dấu cách hoặc dấu ngoặc nhọn; đừng dùng mật khẩu đăng nhập Google. Khởi động lại backend sau khi sửa `.env`. Email xác minh hiển thị người gửi là `mylog <địa chỉ trong MYLOG_MAIL_FROM>`; tên hiển thị không che địa chỉ gửi. Với Gmail, có thể chỉ khởi động `redis` từ Compose nếu database đang dùng Supabase; không cần Mailpit. Kiểm tra bằng luồng đăng ký/xác minh ở mục M1 và xem email nhận được (kể cả Spam). Nếu tài khoản Google không cho tạo mật khẩu ứng dụng, cần dùng dịch vụ SMTP khác hoặc cơ chế xác thực mà tài khoản đó hỗ trợ. Cấu hình trên dùng cổng 587 với STARTTLS và xác thực SMTP; tham khảo [hướng dẫn SMTP của Gmail](https://support.google.com/mail/answer/7104828) và [hướng dẫn mật khẩu ứng dụng](https://support.google.com/mail/answer/185833).

Chạy ứng dụng:

```powershell
./mvnw.cmd spring-boot:run
```

Kiểm tra:

```text
GET http://localhost:8080/actuator/health
```

OpenAPI trong profile local:

```text
GET http://localhost:8080/internal/openapi
GET http://localhost:8080/internal/swagger-ui
```

API docs mặc định bị tắt trong production.

## M3 Outbox và AI analysis

Flyway V8 tạo bảng job/analysis và bổ sung lease cho outbox; migration đã áp dụng lên Supabase.
Worker mặc định tắt (`MYLOG_JOBS_ENABLED=false`). Chỉ bật khi môi trường có schema V8 và
đã chốt classifier, safety policy cùng provider theo ADR-0003/0004. `MYLOG_AI_FAKE_ENABLED=true`
chỉ phục vụ local/test với dữ liệu synthetic; adapter này tạo reflection cố định và không gọi mạng.

Worker kiểm tra lại consent `AI_PROCESSING` và safety policy ngay lúc xử lý job. API đọc trạng thái
và reflection: `GET /api/v1/journal-entries/{entryId}/analysis`; retry có cooldown 60 giây:
`POST /api/v1/journal-entries/{entryId}/analysis:retry`. `GET` trả `status` kể cả khi reflection
chưa sẵn sàng; client không nên chờ vô hạn. Các metric `mylog.outbox.*` và `mylog.ai.*` chỉ gồm
metadata queue, kết quả, token, chi phí và độ trễ.

## M1 Identity, profile và consent

### Xem email tài khoản khi phát triển local

Để thử đăng nhập mà không cần nhớ email, đặt cùng một chuỗi ngẫu nhiên ít nhất 16 ký tự vào `MYLOG_DEV_USERS_KEY` trong `backend/.env` và `front-end/my-app-web/.env.local`, rồi khởi động lại cả backend lẫn web. `GET /internal/dev/users` chỉ tồn tại trong profile `local` (không có ở `staging`/`prod`) và cần header `X-Dev-Users-Key` khớp giá trị trên. API trả tối đa 100 tài khoản `ACTIVE` chưa xóa mỗi lần, gồm `id`, `status`, email đã giải mã; dùng `?before=<id-cuối>` để lấy trang kế tiếp. Response không được cache. Không đưa khóa hoặc kết quả email vào log hay commit. Trang web `http://localhost:3000/dev/users` tự tải danh sách khi chạy `npm run dev`; web giữ khóa ở server và không gửi xuống trình duyệt. Lệnh `npm run dev` chỉ lắng nghe trên `127.0.0.1`. Tính năng này chỉ dành cho dữ liệu phát triển, không dùng với database người dùng thật.

Các endpoint M1 được bật trong profile `local`, `staging`, `prod`; profile `test` chỉ bật khi integration test yêu cầu. Luồng local:

1. `POST /api/v1/auth/register` với email, password (tối thiểu 12 ký tự), timezone IANA, locale, `termsVersion`, `privacyVersion`, `acceptTerms=true`, `acceptPrivacy=true`. Backend trả `202` với `registrationId` và lưu lượt đăng ký trong `pending_registrations`; chưa có dòng `users`, role hoặc profile. Đăng ký lại bằng cùng email trước khi xác minh sẽ thay password/thông tin và cấp mã/link mới; mã/link cũ hết hiệu lực. Email đã có tài khoản thật vẫn trả `409`.
2. Email HTML có nút mở trang xác minh qua liên kết `MYLOG_VERIFICATION_URL?token=...` (token dài, hạn 24 giờ); phần văn bản thuần vẫn chứa URL để dùng với ứng dụng mail không hiển thị HTML. Email cũng chứa mã 8 chữ số (hạn 10 phút). Backend không xử lý liên kết bằng `GET`: trang web/app nhận liên kết phải để người dùng bấm xác nhận rồi gọi `POST /api/v1/auth/email-verifications:confirm` với `{ "token": "..." }`. Nếu nhập mã trên thiết bị khác, gọi `POST /api/v1/auth/email-verifications:confirm-code` với `{ "email": "...", "code": "12345678" }`. Xác minh thành công mới tạo `users`, role và profile trong cùng transaction. Có thể yêu cầu gửi lại bằng `POST /api/v1/auth/email-verifications` với `{ "email": "..." }` khi lượt đăng ký tạm còn tồn tại; mã và liên kết cũ hết hiệu lực khi gửi lại. Lượt đăng ký tạm quá 24 giờ được job dọn dẹp xóa; sau đó đăng ký lại.
3. `POST /api/v1/auth/login` trả access JWT 10 phút và refresh token opaque 30 ngày. `POST /api/v1/auth/refresh` đổi refresh token mỗi lần; dùng lại token cũ sẽ revoke session family.
4. Dùng `Authorization: Bearer <accessToken>` cho `GET/PATCH /api/v1/me`, consent và session APIs. `PATCH /me` dùng `If-Match` từ ETag của `GET /me`. `GET /me` trả cả `email` của chính tài khoản (đã giải mã). `GET /me/sessions` đánh dấu `current: true` cho phiên đang gọi (claim `sid` trong JWT); `deviceName` do client gửi lúc đăng nhập (web gửi dạng `MyLog Web · Chrome · Windows`, suy ra từ User-Agent). `GET /api/v1/feedback?limit=20` trả góp ý của chính user, mới nhất trước. `POST /api/v1/me/data:reset` với `{"password", "confirm": true}` ("Bắt đầu hành trình mới") xóa ngay, không hoàn tác: nhật ký, thẻ, ảnh (cả file lưu trữ, xóa sau khi commit), check-in, phân tích/job AI, insight, report, self-care, bản xuất, góp ý, sự kiện an toàn và idempotency key của user; giữ tài khoản, hồ sơ, consent và phiên đăng nhập, ghi một dòng audit `DATA_RESET`. Tài khoản có mật khẩu phải nhập lại mật khẩu (sai trả `403` với `detail=PASSWORD_INCORRECT`, tối đa 5 lần sai/15 phút rồi `429`); tài khoản Google cần phiên hiện tại được tạo trong 10 phút gần nhất (`403 RECENT_LOGIN_REQUIRED`). Dùng 403 thay 401 để nhập sai mật khẩu không làm mất phiên.

`MYLOG_VERIFICATION_URL` phải là URL HTTPS của trang xác minh trên web ở staging/prod; profile local/test cho phép HTTP. Web hiện đã hỗ trợ cả liên kết và mã 8 chữ số; mobile cần nối riêng nếu muốn xử lý liên kết bằng iOS Universal Links/Android App Links. Không xác minh tự động khi GET và không lưu token trong log, analytics hoặc lịch sử điều hướng. Mã ngắn chỉ dùng một lần, tối đa 5 lần nhập sai cho mỗi email được gửi; sau khi xác minh bằng liên kết hoặc mã, phương thức còn lại hết hiệu lực. Không tự tạo phiên đăng nhập ở bước xác minh.

Đặt lại mật khẩu: `POST /api/v1/auth/password-resets` với `{ "email": "..." }` luôn trả `202` cùng một thông điệp, dù email có tồn tại hay không. Email chứa liên kết `MYLOG_PASSWORD_RESET_URL#token=...` (hạn 1 giờ) và mã 8 chữ số (hạn 15 phút); token nằm sau `#` nên không đi lên server qua request hay `Referer`. Xác nhận bằng `POST /api/v1/auth/password-resets:confirm` với `{ "token", "newPassword" }` hoặc `POST /api/v1/auth/password-resets:confirm-code` với `{ "email", "code", "newPassword" }`; mật khẩu mới dài 12–72 byte UTF-8. Thành công thì mọi phiên của tài khoản bị thu hồi. `MYLOG_PASSWORD_RESET_URL` là bắt buộc ở staging/prod và phải là URL HTTPS của trang web `/auth/reset-password`, không có query hay fragment; profile local/test mặc định `http://localhost:3000/auth/reset-password`.

M1 dùng BCrypt cost 12, mã hóa email/profile bằng AES-GCM envelope, HMAC có khóa cho email lookup và token hash. Migration V17 xóa các `users.status='PENDING'` cũ để giải phóng email; chỉ áp dụng sau khi đã chấp nhận mất những lượt đăng ký chưa xác minh đó. Khóa AES dùng để mã hóa payload có thể rotate bằng `MYLOG_IDENTITY_PREVIOUS_KEYS`; `MYLOG_IDENTITY_LOOKUP_KEY` phải giữ ổn định. JWT có thể chuyển khóa ký bằng `MYLOG_JWT_PREVIOUS_PUBLIC_KEYS`. Local/test có khóa phát triển mặc định; staging/prod yêu cầu khóa và SMTP từ secret manager/environment, không có fallback.

Xóa tài khoản: API `/api/v1/account-deletion-requests` (yêu cầu, xem trạng thái, hủy trong thời gian chờ) vẫn còn ở backend nhưng web không còn gọi. User muốn xóa tài khoản thì gửi email cho admin; người vận hành gọi API này khi cần.

Persistence M1 dùng JPA entity trong `identity`/`user` `infrastructure/persistence/entity`, còn adapter dùng `EntityManager` trong `infrastructure/persistence`. Flyway quản lý schema PostgreSQL; Hibernate chạy ở chế độ `validate`. Các thao tác PostgreSQL đặc thù có thể dùng native SQL qua JPA.

## M2 Journal, check-in và safety đầu vào

Sau khi đăng nhập, gửi `Authorization: Bearer <accessToken>` cho các endpoint sau:

| Tài nguyên | Endpoint |
|---|---|
| Journal | `POST/GET /api/v1/journal-entries`, `GET/PATCH/DELETE /api/v1/journal-entries/{entryId}` |
| Lịch | `GET /api/v1/journal-entries/daily-counts?from=&to=` |
| Favorite | `PUT/DELETE /api/v1/journal-entries/{entryId}/favorite` |
| Tag | `POST/GET /api/v1/journal-tags`, `PUT/DELETE /api/v1/journal-entries/{entryId}/tags/{tagId}` |
| Check-in | `PUT/GET /api/v1/check-ins/{localDate}`, `GET /api/v1/check-ins?from=&to=` |

`POST journal` cần `Idempotency-Key` dài 8–160 ký tự và `contentJson` là TipTap document đã allowlist. `PATCH/DELETE journal` cần `If-Match` bằng ETag trả từ GET/create; version xung đột trả `409`. Danh sách journal dùng cursor, `limit` 1–100, và lọc `from`, `to`, `tag`, `favorite`. Ngày được tính từ `occurredAt` với timezone IANA gửi trong request. Check-in dùng một bản ghi cho mỗi user/ngày; `PUT` thay cả metrics, note và activities của ngày đó. Note và journal payload được mã hóa trước khi ghi DB.

`GET /api/v1/journal-entries/daily-counts?from=YYYY-MM-DD&to=YYYY-MM-DD` cho lịch: trả `{from, to, days:[{date, count, latestMoodCode}]}`, chỉ gồm ngày có bài `SAVED` chưa xóa của user hiện tại, nhóm theo `local_date` (ngày theo timezone lúc viết). `latestMoodCode` là icon cảm xúc của bài mới nhất trong ngày (có thể `null`). Không đọc hay giải mã nội dung nhật ký. Khoảng ngày tối đa 92 ngày tính cả hai đầu; thiếu tham số, `from > to` hoặc khoảng quá dài trả `400`. Web gọi endpoint này cho lưới tháng đang xem thay vì đếm từ danh sách bài đã tải (danh sách tải 5 bài mỗi trang).

Thiếu query parameter bắt buộc ở bất kỳ endpoint nào trả `400 MALFORMED_REQUEST` (trước đây rơi vào `500`).

Safety ingress luôn chạy lúc tạo/sửa journal. Rule HIGH/CRITICAL đặt `analysisStatus=BLOCKED_BY_SAFETY`; nếu classifier không khả dụng thì trạng thái cũng bị chặn và outbox chỉ có ID/version cho lần screen lại. Chưa có classifier hoặc bộ nội dung/nguồn hỗ trợ được duyệt; các bản ghi `safety_resources` không được tự điền hotline. Web đã dùng auth M1 và API journal/check-in thật qua route `/api` của Next.js.

`GET /api/v1/safety/resources?locale=vi-VN&country=VN` là API công khai, chỉ trả nguồn hỗ trợ đã được duyệt, có `verified_at` và nguồn HTTPS. Frontend safety modal dùng `NEXT_PUBLIC_BACKEND_URL` để đọc API này (mặc định `http://localhost:8080` khi phát triển local) và chỉ hiện liên hệ khi API trả dữ liệu đã xác minh. Classifier trả mức rủi ro thấp cũng không mở ordinary analysis nếu `safety_policy_versions` chưa có policy `APPROVED` đang hiệu lực với rule version, classifier provider/version và ngưỡng confidence khớp. Hiện chưa có policy được duyệt nên API nguồn hỗ trợ trả danh sách rỗng và journal vẫn ở chế độ fail-safe.

## M5 Self-care: CRUD mục tiêu và thói quen

Ngoài tạo/xem/sửa goal, tạo habit và đánh dấu completion, API có:

- `PATCH /api/v1/self-care/habits/{habitId}` (cần `If-Match` từ `version`/ETag): cập nhật một phần `title`,
  `targetValue`, `unit`, `frequencyType` + `daysOfWeek`, `toolCode`, `status` (`ACTIVE|PAUSED|ARCHIVED`). Field
  `null` giữ nguyên; `toolCode: ""` gỡ công cụ đã gắn. Đổi lịch hoặc mức mục tiêu không xóa completion cũ. Sai
  version trả `409`.
- `DELETE /api/v1/self-care/habits/{habitId}` và `DELETE /api/v1/self-care/goals/{goalId}` trả `204`, xóa vĩnh
  viễn; completion (và habit của goal) đi theo `ON DELETE CASCADE` của V10. Tài nguyên không thuộc user trả `404`.
- Habit/goal không ở `ACTIVE` không nhận completion mới (`409`).
- `PATCH` goal chưa xóa được `description` (giá trị `null` nghĩa là giữ nguyên).

## M4 Dashboard, insight và report

Flyway V9 đã áp dụng lên Supabase. Các API cần Bearer token: `GET /api/v1/dashboard?range=7d|30d|90d`,
`GET /api/v1/insights?from=&to=&cursor=`, `GET /api/v1/reports?type=WEEKLY&cursor=` và
`GET /api/v1/reports/{reportId}`. Dashboard dùng check-in của ngày trước, journal mới nhất làm
fallback và kèm `source`; current journal streak đếm ngày có journal SAVED, không đếm check-in đơn lẻ.

Insight sleep–mood chỉ xuất hiện khi đủ cặp dữ liệu (`MYLOG_INSIGHTS_MINIMUM_SAMPLES`, mặc định 7),
có correlation strength, sample size và evidence cấu trúc; narrative không khẳng định nguyên nhân.
Report tuần/tháng được scheduler enqueue theo timezone user, worker tạo snapshot bất biến và
regenerate tạo version mới. `MYLOG_REPORTS_ENABLED` mặc định false; bật rõ khi cần scheduler/worker.
Narrative hiện là template từ metric, chưa dùng provider AI. Testcontainers local đo dashboard 30 ngày
trên 10.000 journal synthetic/user p95 45 ms (30 lần gọi sau warmup); cần đo lại trên staging.

### Seed dữ liệu demo cho dashboard (chỉ local)

`scripts/seed-demo-journal.mjs` (Node 18+, không cần dependency) tạo một tài khoản test synthetic rồi
seed khoảng 120 ngày journal, tag và check-in qua API công khai, nên dữ liệu đi đúng luồng mã hóa,
safety và quota như người dùng thật. Cần backend profile `local` với `MYLOG_SAFETY_DEV_ALLOW=true`,
PostgreSQL, Redis và Mailpit (`docker compose up -d`). Chạy từ thư mục gốc repository:

```powershell
node backend/scripts/seed-demo-journal.mjs                    # tạo tài khoản mới + seed 120 ngày
node backend/scripts/seed-demo-journal.mjs --days 90 --seed 7 # độ dài/kịch bản khác
node backend/scripts/seed-demo-journal.mjs --email <email> --password <mật khẩu> [--reset]
node backend/scripts/seed-demo-journal.mjs --email <email> --password <mật khẩu> --rewrite-content
```

Mỗi nhật ký buổi tối dài khoảng 200 chữ (4 đoạn: mở đầu, sự việc theo chủ đề, suy ngẫm, dự định),
ghi chú buổi sáng khoảng 110 chữ. `--rewrite-content` thay nội dung các nhật ký ngắn đã có trong khoảng
ngày bằng văn bản dài, giữ nguyên ngày, điểm số và tag.

Script đăng ký, đọc mã xác minh từ Mailpit và in email/mật khẩu (lưu thêm ở
`scratch/seed-demo-account.txt`, đã gitignore). Dữ liệu có kịch bản: stress cao đầu tuần, ngủ ít thì
mood hôm sau thấp hơn, một giai đoạn 2 tuần đi xuống rồi hồi phục, vài khoảng trống làm ngắt streak.
Quota journal là 60 lượt ghi/15 phút mỗi user, nên script tự chờ khi gặp `429`; seed 120 ngày mất khoảng
30 phút, ngày gần nhất được tạo trước. Script từ chối chạy với host khác localhost. Entry ở trạng thái
`PENDING` vì không có worker phân tích.

`--analyze` (sau khi seed) hoặc `--analyze-only` (tài khoản đã có dữ liệu) làm thêm ba việc cho tài khoản
synthetic: bật consent `AI_PROCESSING`, gọi `POST /api/v1/dev/analyses:seed` để gán cảm xúc/chủ đề mẫu cho
30 ngày gần nhất, rồi `POST /api/v1/insights:refresh` và in các insight. Thêm `--reseed-analysis`
(`?reseed=true`) để thay các phân tích mẫu `LOCAL_FAKE` cũ trong cửa sổ, ví dụ sau khi đổi quy tắc seeder; phân
tích của provider khác không bị đụng tới. Seeder lấy cảm xúc chính theo icon người dùng chọn và chỉ thêm sắc thái
phụ không mâu thuẫn với icon (bài buồn không có Hy vọng/Vui, bài vui không có Buồn/Sợ).

### Dashboard: phổ cảm xúc và sắc thái

`GET /api/v1/dashboard` trả hai phân bố tách biệt:
- `topMoods`: số bài theo **icon cảm xúc người dùng chọn** (`mood_code` của journal: `happy`, `neutral`,
  `angry`, `sad`, `very-bad`). Web vẽ thành "Phổ cảm xúc".
- `topEmotions`: **sắc thái chính** (rank 1) của mỗi bài đã phân tích, nên tổng bằng số bài đã phân tích. Web vẽ
  thành "Sắc thái trong bài viết" theo % số bài. `topEmotions` trong snapshot report dùng cùng cách đếm này;
  pattern `EMOTION_TOPIC_COOCCURRENCE` vẫn xét mọi sắc thái của bài (một bài có thể vừa lo lắng vừa hy vọng).

Hai phân bố có thể khác nhau (chọn "Vui" nhưng viết về deadline có thể mang sắc thái lo lắng); giao diện không dùng
sắc thái để thay thế cảm xúc người dùng tự chọn.

### Pattern insight 30 ngày

- `POST /api/v1/insights:refresh` tính lại insight cho 30 ngày gần nhất theo timezone của user (tối đa
  20 lần/giờ). Các detector thống kê ở `insight/domain/PatternDetectors` (nhịp theo thứ, xu hướng hai nửa
  tháng, cảm xúc–chủ đề, thẻ–mood, activity–mood, giấc ngủ–mood) quyết định nội dung; mỗi detector có
  ngưỡng mẫu/hiệu ứng và trả về không có gì khi dữ liệu chưa đủ. Giữ tối đa 5 insight mạnh nhất, kèm evidence;
  `algorithmVersion = pattern-30d-v1`.
- Câu chữ mặc định là mẫu câu đã duyệt (`InsightTemplates`, `generatedBy = STATISTICAL`). Khi
  `MYLOG_INSIGHTS_AI_NARRATIVE_ENABLED=true`, cấu hình `mylog.ai.*` đã được duyệt và user có consent
  `AI_PROCESSING`, `LlmInsightNarrator` viết lại câu cho gần gũi hơn (`AI_ASSISTED`). Chỉ gửi số liệu đã tính
  và câu mẫu; tên thẻ do user đặt được thay bằng `[THẺ]`. Câu AI phải qua `InsightNarrativeValidator`
  (không thêm số, không nói nguyên nhân/chẩn đoán/liên hệ) và safety screening đầu ra, nếu không dùng câu
  mẫu. Xu hướng mood đi xuống và cửa sổ có bài risk `HIGH`/`CRITICAL` luôn dùng câu mẫu. Token/chi phí ghi
  vào `ai_usage_records` với `operation = INSIGHT_NARRATIVE`.
- `PATCH /api/v1/insights/{id}` với `{"status":"HIDDEN"}` ẩn insight; loại insight đã ẩn vẫn ẩn khi refresh
  trong cùng cửa sổ.
- `GET /api/v1/insights/mascot` cho linh vật ở Góc thấu hiểu: icon mood user chọn nhiều nhất trong 7 ngày
  địa phương gần nhất (`mood` = `happy|neutral|angry|sad|very-bad` hoặc `null`; hòa thì lấy mood của bài mới
  nhất), `moodCount`, `entryCount`, `from`, `to` và `calm = true` khi trong cửa sổ có bài risk
  `HIGH`/`CRITICAL`. Linh vật đã chọn lưu ở hồ sơ: `PATCH /api/v1/me` với `mascot` =
  `sprout|cat|coffee|off` (mặc định `sprout`; giá trị cũ `palette`/`book` đọc ra thành `sprout`).
- `POST /api/v1/dev/analyses:seed` chỉ có ở profile `local`: gán cảm xúc/chủ đề **mẫu** (`provider =
  LOCAL_FAKE`) cho bài 30 ngày gần nhất của user hiện tại, suy ra từ icon mood, điểm số, giấc ngủ và thẻ;
  không đọc hay gửi nội dung nhật ký. Không phải AI phân tích thật.
- Job hằng đêm `InsightRefreshScheduler` (`MYLOG_INSIGHTS_REFRESH_ENABLED`, mặc định `false`, không chạy ở profile
  `api`): mỗi `MYLOG_INSIGHTS_REFRESH_POLL_DELAY_MS` (15 phút) xét user đang ở giờ `MYLOG_INSIGHTS_REFRESH_LOCAL_HOUR`
  (mặc định 3 giờ sáng theo timezone hồ sơ), chỉ tính lại khi có journal/check-in mới và chưa làm mới trong ngày.
  Mặc định dùng câu mẫu; `MYLOG_INSIGHTS_REFRESH_AI_NARRATIVE=true` mới gọi AI (một lần gọi cho mỗi user được làm mới).
  Giới hạn đã biết: user chưa đủ dữ liệu để có pattern không có dòng insight làm mốc, nên có thể được tính lại tối đa
  vài lần trong khung giờ đó (chỉ là truy vấn thống kê, không gọi AI).

### Report tuần/tháng chi tiết

- Snapshot có thêm `journalDays`, `periodDays`, `previous` (trung bình kỳ trước), `bestDay`/`lowestDay` (ngày,
  điểm, `entryId`), `topEmotions`/`topTopics` kèm `previousCount`. Tiêu đề bài không lưu trong snapshot (JSONB không
  mã hóa); `GET /api/v1/reports/{id}` trả `entryTitles` giải mã cho chính chủ lúc đọc.
- Câu tóm tắt dựng từ mẫu `ReportNarratives` (dấu phẩy thập phân, so sánh với kỳ trước, không nói nguyên nhân).
  AI diễn đạt lại theo cùng điều kiện với pattern insight qua `NarrativeRewriter`; kỳ có tâm trạng giảm hoặc căng
  thẳng tăng rõ luôn giữ câu mẫu. `generatedBy` là `RULE` hoặc `AI_ASSISTED`.
- `POST /api/v1/reports` với `{"type":"WEEKLY"|"MONTHLY"}` tạo report cho tuần/tháng gần nhất đã kết thúc (trả
  report có sẵn nếu kỳ đó đã có, không tính lại); `POST /api/v1/reports/{id}:regenerate` tạo phiên bản mới, bản cũ
  giữ nguyên. **Mỗi user chỉ được tạo lại 1 lần mỗi tuần** (thứ Hai–Chủ nhật theo timezone hồ sơ, tính trên mọi report
  của user); lần thứ hai trong tuần trả `409`. Kiểm tra và tạo nằm trong một transaction có advisory lock theo user,
  nên hai lần bấm đồng thời cũng chỉ tạo một phiên bản. `GET /api/v1/reports/regeneration` trả
  `{available, availableFrom}` (`availableFrom` là thứ Hai tuần sau khi đã dùng) để web khóa nút. Lượt dùng được đếm
  từ các phiên bản `version > 1` tạo từ thứ Hai, không cần bảng mới. `MYLOG_REPORTS_WEEKLY_REGENERATE_LIMIT`
  (mặc định `true`) bật giới hạn này; đặt `false` ở local để tạo lại bất cứ lúc nào khi test (API khi đó luôn trả
  `available: true`). Không tắt ở staging/prod. Hai endpoint tạo vẫn dùng chung giới hạn 10 lần/giờ.
  Worker (`MYLOG_REPORTS_ENABLED=true`) mới tính report; nếu tắt, report ở trạng thái `PENDING`.

### Check-in: hoạt động

`PUT /api/v1/check-ins/{localDate}` không gửi trường `activities` thì giữ nguyên hoạt động đã lưu của ngày đó;
gửi `[]` thì xóa; gửi danh sách thì thay thế. Web luôn gửi danh sách đầy đủ.

## Supabase PostgreSQL

Project `mylog` dùng Supabase PostgreSQL 17 và kết nối qua IPv4 session pooler với SSL. Flyway vẫn là nguồn quản lý schema; không sửa schema production trực tiếp bằng Table Editor.

Credential nằm trong `.env` local hoặc secret manager khi deploy. Không đưa database password, service-role key hoặc connection string chứa password vào Git.

## Cloudinary

Cloudinary chỉ dùng cho ảnh journal. Backend đã có Cloudinary Java SDK và fail-fast validation khi `MYLOG_CLOUDINARY_ENABLED=true`. Cần cấu hình bốn biến server-side:

```text
MYLOG_CLOUDINARY_CLOUD_NAME
MYLOG_CLOUDINARY_API_KEY
MYLOG_CLOUDINARY_API_SECRET
MYLOG_CLOUDINARY_FOLDER=mylog
```

Không gửi `MYLOG_CLOUDINARY_API_SECRET` xuống frontend. Quy tắc signed delivery, metadata và deletion được chốt trong [ADR-0006](docs/adr/0006-cloudinary-image-storage.md).

Tắt hạ tầng local nhưng giữ dữ liệu:

```powershell
docker compose down
```

## Test và build

Test integration dùng Testcontainers với PostgreSQL/pgvector và Redis:

```powershell
./mvnw.cmd test
```

Build artifact:

```powershell
./mvnw.cmd clean package
```

Kiểm tra tên và thứ tự Flyway migration:

```powershell
./scripts/check-migrations.ps1
```

Chạy dependency vulnerability scan:

```powershell
./mvnw.cmd -Psecurity-checks verify
```

Report được tạo tại `target/dependency-check-report.html`. CI còn chạy Gitleaks, xuất OpenAPI JSON và build container trên mỗi pull request.

Build container:

```powershell
docker build -t mylog-backend:local .
```

## Cấu trúc module hiện tại

```text
src/main/java/com/mylog/<feature>/
├── api/
│   ├── request/
│   └── response/
├── application/
│   ├── command/
│   └── query/
├── domain/                         # chỉ khi feature cần domain type riêng
└── infrastructure/
    └── persistence/
        ├── entity/                 # @Entity và @IdClass
        └── Jpa<Feature>Store.java  # adapter/repository, ví dụ JpaJournalStore
```

Mỗi feature chỉ tạo những package cần thiết. Entity nằm trong `persistence/entity/`; adapter JPA nằm trực tiếp trong `persistence/` và chỉ module sở hữu được dùng entity của mình. Tách file/package không làm đổi bảng PostgreSQL; Flyway vẫn quản lý schema. Xem [kiến trúc backend](docs/BACKEND_ARCHITECTURE.md) để biết chiều phụ thuộc và ví dụ `journal`.

## Trạng thái security ban đầu

M8 release preparation: xem [`docs/M8_THREAT_MODEL.md`](docs/M8_THREAT_MODEL.md) và
[`docs/M8_RELEASE_RUNBOOK.md`](docs/M8_RELEASE_RUNBOOK.md). Staging/prod mặc định không chạy
Flyway trong từng app instance; cần migration process riêng trước rollout.
Chạy image một lần với `--spring.profiles.active=staging,migrate` hoặc `prod,migrate`;
profile `migrate` không mở HTTP, không chạy worker và thoát sau Flyway.
`MYLOG_APP_PROFILE=api` ngăn scheduled worker, `worker` chỉ mở health HTTP,
`all` dành cho local/test. Journal/tag writes được giới hạn 60 lần/15 phút/user,
export creation 3 lần/ngày/user. Readiness staging/prod kiểm tra PostgreSQL;
liveness không gọi provider ngoài. M8 chưa đạt release gate cho đến khi hoàn thành
staging, restore drill và các phê duyệt privacy/safety.

- Chỉ `/actuator/health` được truy cập công khai.
- Mọi endpoint khác bị deny mặc định.
- CORS chỉ cho phép origin khai báo bởi `MYLOG_ALLOWED_ORIGINS`.
- JWT/resource server dependency đã có, nhưng decoder và auth endpoints sẽ được cấu hình trong phase Identity.

Đây là fail-closed baseline: endpoint nghiệp vụ mới phải khai báo authorization rõ ràng trước khi có thể truy cập.
