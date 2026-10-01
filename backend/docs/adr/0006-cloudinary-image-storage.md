# ADR-0006: Cloudinary for journal image storage

- Status: Accepted
- Date: 2026-09-30
- Owners: backend/operations team
- Supersedes: ADR-0005 for image assets

## Context

mylog cần lưu ảnh riêng khỏi PostgreSQL, phân phối qua HTTPS/CDN và vẫn kiểm soát quyền truy cập đối với nội dung nhật ký nhạy cảm. Dự án đã chọn Cloudinary thay cho S3-compatible storage cho ảnh.

## Decision

- Ảnh nhật ký được upload và quản lý bằng Cloudinary Java SDK qua một application port của module `journal`.
- API secret chỉ tồn tại ở backend environment/secret manager; không trả về frontend và không commit vào Git.
- Asset dùng delivery type yêu cầu authorization/signature; không dùng unsigned upload preset hoặc URL public không kiểm soát cho ảnh nhật ký.
- Backend tạo `public_id` không chứa email, tên hoặc nội dung nhật ký, dưới prefix `mylog/{environment}/{user-id}`.
- Database lưu Cloudinary `asset_id`, `public_id`, version, format, bytes, dimensions, checksum, owner và lifecycle status; không coi URL đã ký là dữ liệu bền vững.
- Upload phải kiểm tra MIME, kích thước và trạng thái moderation/malware trước khi chuyển sang `READY`.
- Xóa tài khoản và xóa journal phải enqueue thao tác destroy idempotent trên Cloudinary, sau đó reconcile orphan assets.
- PDF/CSV export không thuộc quyết định này; storage và retention cho export sẽ được chốt trước EXP-001.

## Consequences

- Không cần vận hành MinIO/S3 cho ảnh.
- Phụ thuộc Cloudinary availability, quota, retention và deletion API.
- Signed delivery URL được tạo khi đọc và có TTL ngắn; cache không được vượt quá authorization lifetime.
- Test unit dùng fake port; integration test thật chỉ chạy với tài khoản Cloudinary chuyên dụng, không dùng asset người dùng thật.
