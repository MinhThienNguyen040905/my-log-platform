# MYLOG – AI Operations Runbook

## 1. Runtime model

API và AI worker dùng chung artifact nhưng chạy với profile khác nhau:

```text
API:    SPRING_PROFILES_ACTIVE=api,supabase
Worker: SPRING_PROFILES_ACTIVE=worker,supabase
```

Profile `api` không consume RabbitMQ và không poll `analysis_jobs`. Profile `worker` thực hiện cả hai. Profile `local` chạy kết hợp để phát triển thuận tiện.

## 2. Provider

Local/test mặc định dùng provider xác định:

```text
AI_PROVIDER=mock
```

Production MVP dùng OpenAI-compatible Responses API:

```text
AI_PROVIDER=openai
AI_API_KEY=<secret injected by deployment platform>
AI_ENDPOINT=https://api.openai.com/v1/responses
AI_MODEL=gpt-5-mini
```

Không commit `AI_API_KEY`. Model, prompt version, schema version, token count và latency được lưu; journal text không được lưu trong usage record, job, event, log hoặc metric label.

## 3. Job lifecycle

```text
PENDING -> PROCESSING -> COMPLETED
                    |-> RETRY_WAIT -> PROCESSING
                    |-> FAILED
                    |-> OBSOLETE
```

- Timeout, HTTP 429, HTTP 5xx và circuit-open là lỗi transient và được retry với exponential backoff.
- Malformed JSON, schema sai hoặc output vi phạm safety là lỗi terminal.
- Job `PROCESSING` quá `AI_JOB_CLAIM_LEASE` được worker khác reclaim sau crash.
- Worker kiểm tra lại journal version khi commit. Version cũ hoặc journal đã xóa chuyển `OBSOLETE`.

## 4. Safety invariants

- Rule deterministic chạy trước provider.
- HIGH/CRITICAL không gọi normal coaching path và không tạo reflection question.
- Output có diagnosis, medication advice hoặc therapy claim bị reject.
- `safety_events` chỉ chứa risk/action/provider metadata, không chứa journal content.
- API trả `safety.blocksNormalResponse` và `safety.actionTaken` để frontend chọn popup phù hợp.

## 5. Diagnostics

Kiểm tra số job theo trạng thái:

```sql
SELECT status, job_type, count(*)
FROM analysis_jobs
GROUP BY status, job_type
ORDER BY job_type, status;
```

Kiểm tra lỗi gần nhất mà không đọc journal content:

```sql
SELECT id, journal_entry_id, journal_version, job_type,
       attempt_count, max_attempts, last_error_code, last_error_at
FROM analysis_jobs
WHERE status IN ('RETRY_WAIT', 'FAILED')
ORDER BY updated_at DESC
LIMIT 100;
```

Metrics chính:

```text
analysis.jobs{outcome,type}
ai.provider.requests{outcome}
safety.events{risk}
```

Không thêm `userId`, `journalId`, topic hoặc journal text vào metric label.

## 6. Recovery

Retry một analysis current bị lỗi nên thực hiện qua API để kiểm tra ownership và version:

```text
POST /api/v1/journals/{journalId}/analysis/retry
```

Nếu worker crash, không sửa trực tiếp job `PROCESSING`; đợi claim lease hết hạn để worker tự reclaim. Chỉ khi incident response xác nhận scheduler không thể phục hồi mới đưa job terminal hiện hành về hàng đợi:

```sql
UPDATE analysis_jobs job
SET status = 'PENDING', attempt_count = 0, next_attempt_at = now(),
    started_at = NULL, completed_at = NULL, last_error_code = NULL,
    last_error_at = NULL, updated_at = now()
FROM journal_entries journal
WHERE job.journal_entry_id = journal.id
  AND job.journal_version = journal.journal_version
  AND journal.deleted_at IS NULL
  AND job.status = 'FAILED'
  AND job.id = :verified_job_id;
```

Luôn xác minh chính xác `verified_job_id` trước khi chạy câu lệnh recovery.
