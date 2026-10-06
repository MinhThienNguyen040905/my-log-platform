# ADR-0003: Safety screening nhiều lớp và fail-safe

- Status: Accepted
- Date: 2026-09-30
- Owners: backend team; nội dung/policy cần người duyệt safety

## Context

Journal có thể chứa nội dung tự làm hại, tự sát, bạo lực hoặc nguy cơ gây hại. Giao toàn bộ quyết định cho một generative prompt tạo false negative khó kiểm soát và không tái hiện được.

## Decision

- Pipeline: input validation → curated deterministic rules → risk classifier → versioned policy engine.
- Risk levels: `NORMAL`, `LOW`, `MODERATE`, `HIGH`, `CRITICAL`.
- `HIGH/CRITICAL` chuyển sang safety flow đã duyệt và không enqueue reflection/recommendation thông thường.
- Classifier lỗi/timeout dẫn tới `FAIL_SAFE`: journal vẫn lưu, generative response bị chặn cho tới khi screen thành công.
- Safety response/resource được quản lý theo locale/country/version; LLM không tạo hotline.
- Output generative cũng qua validation/policy.
- Safety event chỉ lưu decision/provenance/confidence/pseudonymous subject, không lưu đoạn text kích hoạt.
- Mọi thay đổi rule, threshold, classifier hoặc response phải version hóa và chạy regression corpus.

## Ownership

- Backend team sở hữu engine, versioning, observability và enforcement.
- Người được phân quyền `ADMIN_SAFETY` sở hữu nội dung/threshold đã duyệt.
- AI provider không phải decision owner.

## Consequences

- M2 phải hoàn thành safety ingress trước M3 AI.
- Cần curated synthetic Vietnamese regression corpus.
- Safety metrics chỉ ở dạng aggregate có privacy threshold.
- Hệ thống không tự động liên hệ bên thứ ba khi chưa có consent/legal/operational process.
