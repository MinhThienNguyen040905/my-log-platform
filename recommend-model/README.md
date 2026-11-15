# Phòng thử nghiệm câu hỏi tự suy ngẫm

Thư mục này chứa các kịch bản **giả định**, knowledge card và công cụ biên tập/thử nghiệm. Đối chiếu ngày **2026-11-15**: có card được đánh dấu duyệt trong repository và importer corpus riêng cho backend; chưa có bằng chứng vận hành production trong đợt rà soát. Không dùng nhật ký thật cho demo.

## Dữ liệu hiện có

- Ba JSON bài nghiên cứu (`socratic_question_generation.json`, `neural_question_generation_survey.json`, `automatic_reflection_generation.json`): 23 card về phương pháp sinh câu hỏi/phản hồi và đánh giá kỹ thuật.
- `psychological_knowledge_additional.json`: 12 card từ hướng dẫn WHO và 7 card thought record; xuất xứ của mẫu thought record vẫn cần xác nhận.
- `psychological_knowledge.json` là bản tổng hợp **trùng đúng** 23 card của ba file nghiên cứu. Bộ chuẩn hóa kiểm tra sự trùng khớp và không nhập file này lần nữa.
- Ba PDF trong `docs/sources/` thuộc bộ sách cũ, **không phải nguồn PDF của các JSON mới**. Không dùng chúng để xác nhận citation mới.

Trong 42 card duy nhất, **7 card có `review_status=approved`, 35 card còn `pending`**. `docs/card_decisions.json` có 7 quyết định `approved`; các bản ghi này chưa có `curated_text` và `original_wording=true`, nên chưa đủ cổng export của `prepare-knowledge.py`. `docs/source_registry.json` theo dõi citation/quyền gửi nguồn; một số quyền generation đã được đánh dấu `approved`, không đồng nghĩa mọi quyền sử dụng nguồn hay production đã được xác minh. Export curation cho BE chỉ chứa nội dung do nhóm tự viết và đủ điều kiện duyệt. Không sửa trạng thái nguồn để vượt cổng nhập liệu; cần đối chiếu bằng chứng phê duyệt tương ứng.

## Chuẩn bị dữ liệu retrieval

Từ thư mục gốc repository:

```powershell
python -X utf8 recommend-model/tools/prepare-knowledge.py
```

Kết quả ở `scratch/knowledge-preparation/` (Git bỏ qua):

- `curation_review.csv`: 42 card, nhóm mục đích sử dụng, câu gợi ý biên tập và lý do chưa đủ điều kiện.
- `embedding_ready.jsonl`: chỉ chứa đoạn do nhóm tự viết, có nguồn tham khảo và đủ điều kiện duyệt; không xuất trích đoạn nguyên văn. Với dữ liệu curation hiện tại, export vẫn có 0 card đủ điều kiện; việc có 7 card `approved` chưa đủ để tạo đầu vào embedding qua đường này.
- `preparation_manifest.json`: số lượng và các điều kiện còn thiếu.

Script **không tạo vector và không ghi PostgreSQL**. `suggested_text` trong CSV chỉ là bản nháp, không phải câu đã được duyệt để embed. Xem [quy trình biên tập và nhập backend](docs/DATA_PREPARATION.md).

## Demo terminal

Demo dùng năm tình huống trong `input.md`; ánh xạ card đã được chuyển sang bộ JSON mới. `load_cards` mặc định có thể nạp 7 card `approved`; cổng generation còn kiểm tra trạng thái card và quyền gửi nguồn tới provider. Một số kịch bản hiện có thể qua cổng metadata này nếu được cấu hình; không còn đúng khi nói mọi kịch bản đều dừng trước mạng. Cần xác minh bằng chứng quyền dùng nguồn trước khi chạy live. Card `pending` vẫn không được gửi tới provider.

Sau khi nguồn và quyền gửi đã được xác nhận, cấu hình endpoint tương thích Chat Completions qua `config.local.ps1` (Git bỏ qua), rồi chạy `./run.ps1`. Endpoint ngoài máy phải là HTTPS; localhost có thể dùng HTTP. Không đưa API key hoặc nội dung nhật ký thật vào repository.

Kiểm thử local, không cần key:

```powershell
python -X utf8 -m unittest discover -s tests -v
```

Ngày 2026-11-15: **12/15 test đạt, 3 test thất bại** vì còn giả định toàn bộ card là `pending` hoặc không có card được nạp/gửi provider. Các test là `test_sources_are_unique_and_pending_export_is_empty`, `test_pending_cards_are_not_live_by_default`, `test_pending_cards_cannot_be_sent_to_provider`. Cần đối chiếu quyết định duyệt rồi cập nhật fixture/assertion cho cả card đủ và chưa đủ điều kiện; không hạ cổng approval chỉ để làm test đạt. Đợt cập nhật tài liệu chưa sửa test hoặc dữ liệu nguồn.

## Backend retrieval corpus

This importer feed is separate from the curated `embedding_ready.jsonl` export above. Its implementation does not establish that an import or provider call ran on a deployed database during the 2026-11-15 review.

`recommend-model/docs` is a repository-owned static corpus used by Journal Editor recommendations. The `EmbedAllDocsToPgvector.java` importer publishes these imported versions as `APPROVED` under a system audit event; no end-user or developer account is required. This exception is limited to the files in this importer feed. Knowledge content created through `/api/v1/admin/knowledge-items` still uses the normal submit and review permissions.

The importer is idempotent for already imported source slugs, embeds only missing vectors, and publishes existing `DRAFT` versions for those same source slugs. Rebuild the PDF page/chunk feed before importing after adding new sources. Updating an already imported source requires a new knowledge version rather than changing its approved content in place.

The Journal Editor uses locale vi. For the RECOMMEND_MODEL_DOCS topic, the recommendation service maps vi to the corpus locale vi-VN.
