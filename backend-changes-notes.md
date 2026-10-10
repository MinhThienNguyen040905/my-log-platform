# Ghi Chú Chi Tiết Các Thay Đổi Trong Thư Mục Backend

Tài liệu này lưu trữ lại toàn bộ các thay đổi và bổ sung đã được thực hiện trên mã nguồn và tài liệu của thư mục `backend/` trước khi khôi phục (revert) về trạng thái commit gốc.

---

## 1. Mục Đích & Bối Cảnh Thay Đổi
- **Hỗ trợ giao diện dòng thời gian & xem nhanh (Frontend Timeline & Journal Summary):** Bổ sung các chỉ số đo lường cảm xúc (`moodScore`, `stressScore`, `energyScore`, `sleepMinutes`) vào API tóm tắt bài viết (`JournalSummaryResponse`) để frontend có thể hiển thị biểu đồ và thống kê trực tiếp mà không cần giải mã tải toàn bộ nội dung từng bài viết.
- **Hỗ trợ truy vấn Tag theo bài viết:** Mở thêm endpoint `GET /api/v1/journal-entries/{entryId}/tags` để frontend lấy danh sách các nhãn chủ đề gắn liền với một bài viết cụ thể của người dùng hiện tại (có kiểm tra quyền sở hữu).

---

## 2. Chi Tiết Từng File & Code Diff

### A. Nhóm API & Controller

#### 1. `backend/src/main/java/com/mylog/journal/api/JournalTagController.java`
- Thêm endpoint `GET /api/v1/journal-entries/{entryId}/tags`:
```java
    @GetMapping("/api/v1/journal-entries/{entryId}/tags")
    public List<JournalTagResponse> listForEntry(@PathVariable UUID entryId) {
        return tags.listForEntry(user(), entryId).stream().map(JournalTagResponse::from).toList();
    }
```

#### 2. `backend/src/main/java/com/mylog/journal/api/response/JournalSummaryResponse.java`
- Thêm các trường số liệu vào record DTO:
```java
public record JournalSummaryResponse(UUID id, String title, Instant occurredAt, LocalDate localDate,
                                     String moodCode, BigDecimal moodScore, BigDecimal stressScore,
                                     BigDecimal energyScore, Integer sleepMinutes, boolean favorite, String riskLevel,
                                     String analysisStatus, long rowVersion) {
    public static JournalSummaryResponse from(JournalSummaryView view) {
        return new JournalSummaryResponse(view.id(), view.title(), view.occurredAt(), view.localDate(),
                view.moodCode(), view.moodScore(), view.stressScore(), view.energyScore(), view.sleepMinutes(),
                view.favorite(), view.riskLevel(), view.analysisStatus(), view.version());
    }
}
```

---

### B. Nhóm Application Service & Query View

#### 3. `backend/src/main/java/com/mylog/journal/application/query/JournalSummaryView.java`
- Bổ sung các trường vào view projection:
```java
public record JournalSummaryView(UUID id, String title, Instant occurredAt, LocalDate localDate,
                                 String moodCode, BigDecimal moodScore, BigDecimal stressScore,
                                 BigDecimal energyScore, Integer sleepMinutes, boolean favorite, String riskLevel,
                                 String analysisStatus, long version) {}
```

#### 4. `backend/src/main/java/com/mylog/journal/application/JournalService.java`
- Ánh xạ các trường cảm xúc từ snapshot vào `summary`:
```java
    private JournalSummaryView summary(JournalEntrySnapshot entry) {
        return new JournalSummaryView(entry.id(), payload(entry).title(), entry.occurredAt(), entry.localDate(),
                entry.moodCode(), entry.moodScore(), entry.stressScore(), entry.energyScore(), entry.sleepMinutes(),
                entry.favorite(), entry.riskLevel(), entry.analysisStatus(), entry.rowVersion());
    }
```

#### 5. `backend/src/main/java/com/mylog/journal/application/JournalTagService.java`
- Bổ sung dependency `JournalService` và phương thức `listForEntry`:
```java
    private final JournalService journal;

    public JournalTagService(JournalTagStore store, Clock clock, JournalService journal) {
        this.store = store; this.clock = clock; this.journal = journal;
    }

    @Transactional(readOnly = true)
    public List<JournalTagView> listForEntry(UUID userId, UUID entryId) {
        journal.get(userId, entryId);
        return store.listForEntry(userId, entryId);
    }
```

#### 6. `backend/src/main/java/com/mylog/journal/application/JournalTagStore.java`
- Khai báo thêm trong interface:
```java
    List<JournalTagView> listForEntry(UUID userId, UUID entryId);
```

---

### C. Nhóm Infrastructure & Persistence

#### 7. `backend/src/main/java/com/mylog/journal/infrastructure/persistence/JpaJournalTagStore.java`
- Triển khai câu truy vấn JPQL lấy tag gắn với entry:
```java
    @Override public List<JournalTagView> listForEntry(UUID userId, UUID entryId) {
        return em.createQuery("""
                select t from JournalTag t, JournalEntryTag jt
                where t.userId=:userId and jt.journalEntryId=:entryId and jt.tagId=t.id
                order by t.createdAt
                """, JournalTag.class)
                .setParameter("userId", userId).setParameter("entryId", entryId)
                .getResultList().stream().map(this::view).toList();
    }
```

---

### D. Nhóm Testing & Tài Liệu

#### 8. `backend/src/test/java/com/mylog/journal/JournalCheckinIntegrationTest.java`
- Thêm assertion kiểm tra `moodScore` trong summary list và kiểm thử quyền sở hữu với endpoint `GET /api/v1/journal-entries/{id}/tags`:
```java
        assertEquals(0, BigDecimal.valueOf(3).compareTo(journal.list(owner, null, null, null, null, null, 20)
                .entries().getFirst().moodScore()));

        mvc.perform(get("/api/v1/journal-entries/{id}/tags", entry.id()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/journal-entries/{id}/tags", entry.id())
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/journal-entries/{id}/tags", entry.id())
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(tag.id().toString()));
```

#### 9. `backend/docs/BACKEND_ARCHITECTURE.md`
- Bổ sung `GET /journal-entries/{entryId}/tags` vào danh sách API v1.
- Ghi chú: *"Journal summary trả thêm moodScore, stressScore, energyScore và sleepMinutes để frontend hiển thị chỉ số mà không phải tải nội dung từng bài. `GET /journal-entries/{entryId}/tags` chỉ trả tag của entry thuộc current user."*

#### 10. `backend/docs/BACKEND_DEVELOPMENT_PLAN.md`
- Cập nhật mục 8 tiến độ M2: *"Frontend my-app-web đã nối auth và CRUD journal cơ bản qua API có token, bổ sung đọc tag theo entry và các chỉ số vào journal summary..."*

---

## 3. Kế hoạch nối API web sau khi hoàn tác backend (2026-10-07)

Phạm vi triển khai: `front-end/my-app-web` cho auth, tài khoản và journal. Mục này chỉ ghi nhận kế hoạch và chênh lệch contract; không yêu cầu chỉnh sửa `backend/` trong đợt này. Các endpoint bên dưới có tiền tố `/api/v1` ở backend.

| Thứ tự | Nhóm | Việc thực hiện ở frontend | Tiêu chí hoàn thành |
|---|---|---|---|
| 1 | Contract và phiên | Rà các Next.js route handlers hiện có, chuẩn hóa chuyển tiếp status, `application/problem+json`, `ETag`, `If-Match`, `Idempotency-Key`; giữ token trong cookie `HttpOnly`. | Client không lưu token; các lỗi `401`, `409`, `429`, `503` có cách xử lý rõ ràng. |
| 2 | Auth | Hoàn thiện register, gửi lại/xác nhận email, login, refresh rotation và logout; kiểm tra tải lại trang và các request đồng thời khi access token hết hạn. | Người dùng đi hết luồng đăng ký đến đăng nhập; logout xóa phiên và cookie. |
| 3 | Tài khoản | Nối Settings và onboarding với `GET/PATCH /me`, `POST /me/onboarding:complete`; nối `GET/PUT /me/consents`, `GET/DELETE /me/sessions`. Lấy version từ response và gửi `If-Match` khi cập nhật profile. | Dữ liệu đã lưu tồn tại sau reload; không báo thành công khi server từ chối; xung đột version được hiển thị. |
| 4 | Journal | Hoàn thiện tạo, danh sách cursor/filter, đọc, sửa, xóa, favorite và tag theo contract backend đang có; giữ `Idempotency-Key` khi tạo và `If-Match` khi sửa/xóa. Đồng bộ editor, lịch và danh sách sau mutation. | Dữ liệu sau reload khớp backend; phân trang ổn định; không ghi đè bài có version mới hơn. |
| 5 | Kiểm chứng | Kiểm thử luồng web với backend thật, quyền truy cập, phiên hết hạn, lỗi mạng, xung đột và dữ liệu synthetic. Chạy lint/build frontend; chỉ chạy test backend nếu sau này có thay đổi backend được giao riêng. | Luồng chính và các trạng thái lỗi hoạt động đúng. |

### Chênh lệch contract cần theo dõi

- Backend hiện **không có** `GET /journal-entries/{entryId}/tags`, trong khi `JournalContext` và `useJournalEditor` đang gọi endpoint này. Frontend phải tránh gọi nó trong luồng đang dùng hoặc tạm ẩn chức năng đọc/sửa tag theo bài; không đánh dấu phần này hoàn thành. Bổ sung endpoint vào backend là hạng mục riêng, chỉ ghi nhận ở log này.
- Journal summary backend hiện **không trả** `moodScore`, `stressScore`, `energyScore`, `sleepMinutes`, trong khi kiểu frontend có các trường đó. Frontend cần xử lý chúng là thiếu dữ liệu và không suy diễn chỉ số từ giá trị mặc định. Nếu lịch cần các chỉ số từ summary, việc mở rộng contract backend là hạng mục riêng.
- `useSettings` và onboarding hiện cập nhật profile trong state/localStorage. Chỉ chuyển thông báo “đã lưu” sang sau khi `PATCH /me` hoặc `onboarding:complete` thành công.
- Các thay đổi backend đã hoàn tác được ghi ở mục 2 chỉ là lịch sử tham khảo, không phải API hiện hành.

### Tiến độ thực thi frontend (2026-10-07)

- Đã thêm Next.js route handlers cho `GET/PATCH /me`, hoàn tất onboarding, consent và quản lý phiên. Các route dùng cookie `HttpOnly`, refresh token khi cần, chuyển tiếp `ETag` và `If-Match`.
- Settings lưu các trường backend hỗ trợ (`displayName`, `penName`, `timezone`, `locale`) và chỉ báo thành công sau phản hồi server. Onboarding lưu mục tiêu rồi gọi endpoint hoàn tất. Lời đề tựa đang được vô hiệu hóa vì backend chưa có trường lưu.
- Settings đọc consent/session; cho phép thu hồi consent đã cấp, thu hồi từng phiên và các phiên khác. Chưa có luồng cấp consent mới vì UI chưa hiển thị nội dung tài liệu tương ứng để người dùng xem trước khi đồng ý.
- Journal editor không còn gọi endpoint đọc tag theo bài chưa tồn tại. Bài không có chủ đề vẫn lưu được; nếu người dùng chọn chủ đề, UI báo rõ chưa hỗ trợ và không gửi yêu cầu lưu. Calendar không hiện chỉ số `0` giả khi summary thiếu metrics; khi mở bài, chi tiết được tải từ backend.
- `npm.cmd run build` thành công sau thay đổi cuối. ESLint theo các file route/client/context/hook mới sửa đã qua; lint toàn repo vẫn có lỗi ở các file frontend khác đang thay đổi. Chưa kiểm thử luồng HTTP với backend đang chạy.


---

## 4. Ghi chú phạm vi FE (2026-10-08)

Bỏ nhóm API check-in khỏi phạm vi nối API frontend ở đợt tiếp theo.

## 5. Đề xuất BE cho luồng quên mật khẩu (2026-10-08)

- Backend hiện chưa có API yêu cầu đặt lại mật khẩu, xác minh mã hoặc hoàn tất đổi mật khẩu; `IdentityController` hiện chỉ có đăng ký, xác minh email, đăng nhập, refresh và đăng xuất. Đây là đề xuất cho đợt BE sau, chưa phải API hiện hành.
- Thiết kế luồng khôi phục mật khẩu riêng với mã/token dùng một lần, thời hạn ngắn, giới hạn tần suất và phản hồi không tiết lộ email có tồn tại hay không. Chỉ cập nhật mật khẩu sau khi BE xác minh mã/token; xem xét thu hồi các phiên hiện có sau khi đổi mật khẩu.
- Khi BE chốt request/response/error contract và triển khai xong, FE mới nối luồng quên mật khẩu. Trong đợt refactor FE hiện tại chỉ loại bỏ thông báo xác minh/đổi mật khẩu thành công giả.

## 6. Đề xuất kiểm tra refresh token khi FE chạy nhiều instance (2026-10-08)

- FE hiện gộp các yêu cầu refresh đồng thời bằng bộ nhớ của từng tiến trình Next.js. Cách này không đồng bộ giữa nhiều instance; hai request dùng cùng refresh token cũ có thể đến BE cùng lúc và kích hoạt reuse detection.
- Trước khi triển khai FE nhiều instance, kiểm thử rotation dưới tải đồng thời và chốt cơ chế idempotency/grace ngắn hoặc điều phối phân tán phù hợp với chính sách bảo mật của BE. Đây là việc cần thiết kế và kiểm chứng riêng; đợt FE hiện tại chưa thay đổi cơ chế refresh của BE.

