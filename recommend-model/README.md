# Phòng thử nghiệm câu hỏi tự suy ngẫm

Thư mục này chứa các kịch bản **giả định** và knowledge card đang chờ kiểm duyệt. Đây là công cụ biên tập và thử nghiệm, chưa dùng nhật ký thật hoặc làm nguồn knowledge production.

## Dữ liệu hiện có

- Ba JSON bài nghiên cứu (`socratic_question_generation.json`, `neural_question_generation_survey.json`, `automatic_reflection_generation.json`): 23 card về phương pháp sinh câu hỏi/phản hồi và đánh giá kỹ thuật.
- `psychological_knowledge_additional.json`: 12 card từ hướng dẫn WHO và 7 card thought record; xuất xứ của mẫu thought record vẫn cần xác nhận.
- `psychological_knowledge.json` là bản tổng hợp **trùng đúng** 23 card của ba file nghiên cứu. Bộ chuẩn hóa kiểm tra sự trùng khớp và không nhập file này lần nữa.
- Ba PDF trong `docs/sources/` thuộc bộ sách cũ, **không phải nguồn PDF của các JSON mới**. Không dùng chúng để xác nhận citation mới.

Tất cả 42 card duy nhất vẫn `pending`. `docs/source_registry.json` theo dõi nguồn tham khảo; các trường quyền trích đoạn chỉ dùng khi tái sử dụng nguyên văn nguồn hoặc gửi card nguồn của demo tới provider. Export cho BE chỉ chứa nội dung do nhóm tự viết và đã duyệt. `docs/card_decisions.json` là nơi ghi quyết định biên tập riêng, ban đầu rỗng. Không sửa `review_status` trong JSON nguồn chỉ để vượt qua cổng nhập liệu.

## Chuẩn bị dữ liệu retrieval

Từ thư mục gốc repository:

```powershell
python -X utf8 recommend-model/tools/prepare-knowledge.py
```

Kết quả ở `scratch/knowledge-preparation/` (Git bỏ qua):

- `curation_review.csv`: 42 card, nhóm mục đích sử dụng, câu gợi ý biên tập và lý do chưa đủ điều kiện.
- `embedding_ready.jsonl`: chỉ chứa đoạn do nhóm tự viết, có nguồn tham khảo và đã được duyệt; không xuất trích đoạn nguyên văn. Hiện file này rỗng.
- `preparation_manifest.json`: số lượng và các điều kiện còn thiếu.

Script **không tạo vector và không ghi PostgreSQL**. `suggested_text` trong CSV chỉ là bản nháp, không phải câu đã được duyệt để embed. Xem [quy trình biên tập và nhập backend](docs/DATA_PREPARATION.md).

## Demo terminal

Demo dùng năm tình huống trong `input.md`; ánh xạ card đã được chuyển sang bộ JSON mới. Nó chỉ gọi LLM khi các card được chọn đã `approved` và nguồn có bằng chứng cho quyền gửi nội dung tới generation provider. Vì hiện chưa có phê duyệt, demo sẽ dừng trước lời gọi mạng với thông báo tương ứng. Việc này ngăn gửi card `pending` tới provider.

Sau khi nguồn và quyền gửi đã được xác nhận, cấu hình endpoint tương thích Chat Completions qua `config.local.ps1` (Git bỏ qua), rồi chạy `./run.ps1`. Endpoint ngoài máy phải là HTTPS; localhost có thể dùng HTTP. Không đưa API key hoặc nội dung nhật ký thật vào repository.

Kiểm thử local, không cần key:

```powershell
python -X utf8 -m unittest discover -s tests -v
```

## Backend retrieval corpus

`recommend-model/docs` is a repository-owned static corpus used by Journal Editor recommendations. The `EmbedAllDocsToPgvector.java` importer publishes these imported versions as `APPROVED` under a system audit event; no end-user or developer account is required. This exception is limited to the files in this importer feed. Knowledge content created through `/api/v1/admin/knowledge-items` still uses the normal submit and review permissions.

The importer is idempotent for already imported source slugs, embeds only missing vectors, and publishes existing `DRAFT` versions for those same source slugs. Rebuild the PDF page/chunk feed before importing after adding new sources. Updating an already imported source requires a new knowledge version rather than changing its approved content in place.

The Journal Editor uses locale vi. For the RECOMMEND_MODEL_DOCS topic, the recommendation service maps vi to the corpus locale vi-VN.
