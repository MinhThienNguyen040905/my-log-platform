# ADR-0005: S3-compatible private object storage

- Status: Superseded by ADR-0006
- Date: 2026-09-30
- Owners: backend/operations team

## Context

Journal có thể có ảnh và export PDF/CSV. Lưu binary lớn trong PostgreSQL tăng backup/restore time; public URL làm mất kiểm soát truy cập và expiry.

## Decision

- Dùng S3-compatible object storage qua application port.
- Local/staging có thể dùng MinIO; production dùng managed private bucket đáp ứng region/encryption policy.
- Bucket không public. Client upload/download qua presigned URL sống ngắn hoặc backend stream khi cần authorization chặt hơn.
- Database chỉ lưu opaque storage key, MIME, size, checksum, owner và lifecycle status; không lưu signed URL.
- Upload theo trạng thái `PENDING_SCAN → READY/REJECTED` và kiểm tra MIME, size, checksum, malware.
- Object key không chứa email, tên hoặc journal title.
- Export có prefix/bucket policy và TTL cleanup riêng.
- Account deletion xóa object idempotent trước khi hoàn tất metadata checkpoint.

## Consequences

- Asset không khả dụng cho user cho tới khi scan xong.
- Cần reconcile job cho orphan object/metadata.
- Local Compose chỉ thêm MinIO khi bắt đầu JRN-004/EXP-001, không chạy hạ tầng chưa dùng trong M0.
- CDN chỉ được thêm cho public approved content, không cho journal asset.
