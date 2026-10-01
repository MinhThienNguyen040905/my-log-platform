# ADR-0001: Local credentials, JWT access token và opaque refresh session

- Status: Accepted
- Date: 2026-09-30
- Owners: backend team

## Context

mylog cần đăng nhập web, revoke từng thiết bị, khóa tài khoản và audit session. MVP chưa cần phụ thuộc external identity provider. Journal là dữ liệu nhạy cảm nên refresh token phải revoke/rotate được; JWT sống dài không đáp ứng yêu cầu này.

## Decision

- MVP dùng local email/password.
- Password hash bằng Argon2id hoặc BCrypt sau benchmark trên hạ tầng mục tiêu; không tự viết thuật toán.
- Access token là JWT ký bất đối xứng, TTL mục tiêu 10 phút, `sub` là user UUID, có issuer/audience/token ID.
- Refresh token là chuỗi opaque ngẫu nhiên TTL mục tiêu 30 ngày; database chỉ lưu hash.
- Mỗi refresh thuộc một token family/session. Mỗi lần refresh phải rotate; reuse token cũ revoke toàn family.
- Permission được resolve từ database/cache và đưa tối thiểu vào access token; thay đổi quyền quan trọng revoke session.
- Admin dùng cùng identity system nhưng role/permission riêng; production yêu cầu MFA trước khi mở admin nhạy cảm.
- Security mặc định deny; endpoint mới phải khai báo authorization.

## Consequences

- M1 phải triển khai key rotation, session cleanup, rate limit và token reuse test.
- JWT compromise bị giới hạn bởi TTL; refresh compromise xử lý bằng rotation/revoke.
- OAuth/OIDC có thể thêm sau qua identity adapter mà không đổi user ID nội bộ.
- Không lưu JWT/refresh token trong application log hoặc audit metadata.

## Rejected alternatives

- JWT refresh sống dài: không revoke/reuse-detect đáng tin cậy.
- External IdP ngay MVP: tăng dependency và cấu hình khi chưa có yêu cầu social/enterprise login.
- Cookie session thuần: khả thi nhưng không phù hợp định hướng API/mobile tiếp theo bằng access/refresh split.
