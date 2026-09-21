# MyLog Messaging Runbook

## Mục tiêu

Runbook này mô tả cách kiểm tra và replay transactional outbox khi RabbitMQ hoặc publisher gặp sự cố. Journal content không được phép xuất hiện trong outbox, message header, log hoặc DLQ.

## Delivery semantics

- Journal và outbox event commit trong cùng PostgreSQL transaction.
- Publisher claim theo batch bằng `FOR UPDATE SKIP LOCKED`.
- Event ở trạng thái `PUBLISHING` có lease; instance khác reclaim được sau khi lease hết hạn.
- Chỉ publisher confirm dương và message không bị return mới chuyển event sang `PUBLISHED`.
- Delivery là **at-least-once**. Consumer phải ghi business result và `processed_messages` trong cùng transaction.
- Consumer chỉ `basicAck` sau khi transaction commit; lỗi dùng `basicNack(requeue=false)` để RabbitMQ chuyển message vào DLQ.

## Metrics và health

Các metric chính tại `/actuator/prometheus`:

```text
outbox_pending_count
outbox_oldest_pending_age_seconds
outbox_publish_failed_total
outbox_retry_scheduled_total
outbox_dead_letter_total
messaging_queue_depth{queue="..."}
messaging_dead_letter_depth
```

Health contributor `outbox` trả số event pending, tuổi event cũ nhất và queue depth. Cảnh báo tối thiểu nên bật khi:

- oldest pending age vượt 5 phút;
- publish failure tăng liên tục;
- DLQ depth lớn hơn 0;
- event `PUBLISHING` tồn tại lâu hơn claim lease.

## Kiểm tra sự cố

1. Kiểm tra RabbitMQ health, connection và queue depth.
2. Kiểm tra outbox theo trạng thái, không select hoặc log cột `payload` trong output vận hành:

```sql
SELECT status, count(*), min(occurred_at) AS oldest
FROM outbox_events
GROUP BY status
ORDER BY status;
```

3. Kiểm tra error code đã được sanitize:

```sql
SELECT id, event_type, attempt_count, next_attempt_at, last_error_code
FROM outbox_events
WHERE published_at IS NULL
ORDER BY occurred_at
LIMIT 100;
```

## Replay event retryable

Publisher tự retry event `FAILED` theo exponential backoff. Khi dependency đã phục hồi và cần replay sớm:

```sql
UPDATE outbox_events
SET next_attempt_at = now()
WHERE id = :event_id
  AND status = 'FAILED'
  AND published_at IS NULL;
```

Không đổi `attempt_count`, không sửa `payload`, và không chuyển trực tiếp sang `PUBLISHED`.

## Replay event đã vào DLQ

Event terminal được lưu `status = 'FAILED'`, có `published_at` và `next_attempt_at` ở xa trong tương lai. Sau khi xác định nguyên nhân và xác nhận replay an toàn:

```sql
UPDATE outbox_events
SET status = 'FAILED',
    attempt_count = 0,
    published_at = NULL,
    next_attempt_at = now(),
    last_error_code = NULL
WHERE id = :event_id
  AND status = 'FAILED'
  AND published_at IS NOT NULL;
```

Sau replay, theo dõi publisher confirm và `processed_messages`. Không xóa marker processed để ép consumer chạy lại nếu chưa có quy trình sửa business result riêng.

## Recovery sau publisher crash

Không cập nhật thủ công event `PUBLISHING` ngay lập tức. Chờ `OUTBOX_CLAIM_LEASE` hết hạn; publisher đang hoạt động sẽ tự reclaim. Việc publish trùng sau crash là hợp lệ với at-least-once và được consumer idempotency xử lý.
