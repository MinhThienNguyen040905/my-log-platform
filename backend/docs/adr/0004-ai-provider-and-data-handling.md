# ADR-0004: Provider-neutral AI ports và data minimization

- Status: Accepted
- Date: 2026-09-30
- Owners: backend team

## Context

Model/provider có thể thay đổi theo chất lượng tiếng Việt, chi phí, vùng xử lý và điều khoản retention. Gắn domain vào một SDK làm khó thay thế và kiểm thử. Journal không được dùng để train hoặc lưu bởi bên thứ ba ngoài consent/policy.

## Decision

- Domain/application chỉ phụ thuộc các port `JournalAnalyzer`, `EmbeddingProvider`, `KnowledgeRetriever`.
- Local/test mặc định dùng fake deterministic provider; không gọi dịch vụ ngoài âm thầm.
- Adapter provider đầu tiên chỉ được bật sau khi xác nhận: no-training/default opt-out, retention phù hợp, encryption in transit, region và deletion capability.
- Chỉ gửi context tối thiểu cần thiết; không gửi email, raw ID hoặc metadata không phục vụ use case.
- Provider call chạy async sau safety screening, có timeout/circuit breaker và structured output schema.
- Database lưu provider/model/prompt/policy/content version, token/cost/latency; không lưu chain-of-thought.
- Raw provider request/response không được ghi application log.
- Đổi provider/model là config + adapter deployment, không đổi domain contract.

## Retention

- Temporary in-memory payload chỉ sống trong job execution.
- Nếu provider cung cấp zero-retention mode, production phải bật.
- Nếu không đáp ứng policy, provider đó không được dùng cho journal content.
- Dữ liệu nội bộ dẫn xuất tuân theo account deletion/retention policy.

## Consequences

- Quyết định model cụ thể được hoãn tới M3 evaluation nhưng data-handling gate không được hoãn.
- Cần provider contract tests và quality/safety evaluation trước rollout.
- Một số feature provider-specific không được đưa vào application interface nếu chưa có use case chung.
