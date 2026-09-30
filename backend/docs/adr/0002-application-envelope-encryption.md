# ADR-0002: Envelope encryption cấp application cho dữ liệu nhạy cảm

- Status: Accepted
- Date: 2026-09-30
- Owners: backend team

## Context

Encryption-at-rest của database không bảo vệ journal trước truy vấn DB trực tiếp, backup bị lộ hoặc quyền admin ứng dụng quá rộng. Backend vẫn cần aggregate mood/stress/sleep mà không giải mã toàn bộ nội dung.

## Decision

- Journal/profile/email/tag/AI narrative/goal text được mã hóa trong application trước persistence.
- Payload dùng AES-256-GCM với data encryption key (DEK) và nonce duy nhất.
- DEK được wrap bằng key encryption key (KEK). Row lưu ciphertext, nonce, wrapped DEK và key version.
- AAD gồm table, row ID, owner ID và payload/schema version.
- Production triển khai `KeyEncryptionService` bằng managed KMS/secret manager.
- Local/test dùng adapter key cố định từ biến môi trường hoặc deterministic fake; adapter local không được bật ở production.
- Email/tag cần equality lookup dùng keyed HMAC của giá trị normalize; không dùng hash không khóa.
- Structured metrics cần analytics được lưu plaintext nhưng luôn đi cùng owner authorization.
- `pgcrypto` không phải lớp mã hóa journal chính; extension chỉ phục vụ primitive/database use case có kiểm soát.

## Consequences

- Không hỗ trợ full-text search journal trong MVP.
- Key rotation có thể re-wrap DEK mà không mã hóa lại payload; đổi thuật toán/schema cần migration job riêng.
- Backup chứa ciphertext nhưng KMS access và backup access vẫn phải tách quyền.
- Crypto adapter phải có tamper/wrong-AAD/rotation tests.

## Rejected alternatives

- Chỉ bật disk/database encryption: không đủ cho threat model.
- Mã hóa toàn bộ database bằng một key ứng dụng: blast radius lớn, rotate khó.
- Lưu plaintext để search: không phù hợp privacy-by-design của mylog.
