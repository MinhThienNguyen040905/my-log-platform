# Architecture Decision Records

Các quyết định nền tảng của backend **mylog** được ghi lại dưới dạng ADR. ADR đã được chấp nhận chỉ thay đổi bằng một ADR mới thay thế quyết định cũ, không sửa mất lịch sử lý do và đánh đổi.

| ADR | Quyết định | Trạng thái |
|---|---|---|
| [0001](0001-authentication-and-session.md) | Local authentication, JWT và refresh-token rotation | Accepted |
| [0002](0002-application-envelope-encryption.md) | Application-level envelope encryption | Accepted |
| [0003](0003-layered-safety-screening.md) | Layered, fail-safe safety screening | Accepted |
| [0004](0004-ai-provider-and-data-handling.md) | Provider-neutral AI và data minimization | Accepted |
| [0005](0005-private-object-storage.md) | Private S3-compatible object storage | Superseded by ADR-0006 |
| [0006](0006-cloudinary-image-storage.md) | Cloudinary for journal image storage | Accepted |
| [0007](0007-encrypted-database-export-artifacts.md) | Encrypted temporary export artifacts in PostgreSQL | Accepted |
| [0008](0008-backend-only-database-api.md) | Backend-only access to application tables | Accepted |

## Quy ước

- Tên file: `NNNN-short-title.md`.
- Trạng thái: `Proposed`, `Accepted`, `Deprecated` hoặc `Superseded`.
- Một ADR mới phải nêu context, decision, consequences và migration/rollback considerations khi có liên quan.
- Nếu thay thế quyết định cũ, liên kết hai ADR bằng trường `Supersedes`/`Superseded by`.
