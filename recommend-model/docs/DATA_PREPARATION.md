# Quy trình tối ưu JSON trước khi embedding

## Trạng thái ngày 2026-10-11

Bốn file nguồn chứa 42 card duy nhất: 23 card nghiên cứu và 19 card nội dung tự suy ngẫm. `psychological_knowledge.json` trùng 23 card nghiên cứu và chỉ được dùng để kiểm tra tính nhất quán, không làm đầu vào import thứ hai. Mọi card vẫn `pending`; chưa có bản ghi nào đủ điều kiện lưu DB hoặc tạo embedding.

Đã đối chiếu tên bài, tác giả và nơi công bố ở nguồn chính thức: [SocratiQ](https://aclanthology.org/2023.eacl-main.12/), [NQG survey](https://www.ijcai.org/proceedings/2024/889), [reflection generation](https://aclanthology.org/2023.gem-1.6/) và [hướng dẫn WHO](https://www.who.int/publications-detail-redirect/9789240003927). **Chưa xác minh từng câu trích và trang của 42 card**. WHO có bản tiếng Việt, nhưng số trang của bản dịch có thể khác bản tiếng Anh. Bảy card thought record chưa có nguồn trực tiếp được định danh; [mẫu NHS](https://www.nhs.uk/every-mind-matters/mental-wellbeing-tips/self-help-cbt-techniques/thought-record/) là một ứng viên để đối chiếu, chưa tự động được nhận làm nguồn gốc.

## Phân nhóm

| Nhóm | Card | Cách dùng dự kiến |
|---|---:|---|
| `self_reflection_content` | 19 (`who_2020_*`, `thought_record_user_*`) | Ứng viên cho knowledge retrieval sau khi xác minh nguồn, biên tập và duyệt |
| `generation_guidance` | Một phần `soq_*`, `ref_*` | Hướng dẫn thiết kế prompt và cách đặt câu hỏi; không trả nguyên card như kiến thức tâm lý |
| `engineering_evaluation` | `nqg_*` và card về dataset, fine-tuning, metric | Tài liệu kỹ thuật, kiểm thử và đánh giá; không đưa vào retrieval cho người dùng |

Phân nhóm trong code là bản nháp kỹ thuật; người phụ trách nội dung có thể điều chỉnh trước khi duyệt. `observable_signals` hiện đều là `generated_example`; `question_templates` cũng là gợi ý ứng dụng, không phải kết quả đã được bài báo kiểm chứng. Không trộn chúng vào `retrieval_text` mặc định.

Bảng review tự động đánh dấu mẫu câu mở đầu bằng “Mình/Tôi” ở `ref_001`, `ref_004`, `ref_007`; cần biên tập lại nếu dùng làm lời AI nói trực tiếp với người dùng.

## Cách hoàn tất review

1. Chạy `python -X utf8 recommend-model/tools/prepare-knowledge.py`, mở `scratch/knowledge-preparation/curation_review.csv`.
2. Với từng card muốn dùng, mở nguồn chính thức và kiểm tra ý chính, giới hạn áp dụng và câu hỏi tiếng Việt. `citation_verified` ở luồng card tự viết nghĩa là đã xác nhận đúng nguồn và ý được tham khảo; không khẳng định câu chữ hay số trang trích dẫn nguyên văn. Ghi quyết định vào `docs/card_decisions.json`: `status`, `citation_verified`, `original_wording`, `curated_text`, `topic_code`, `locale`, `reviewed_by`. `curated_text` là một ý độc lập tối đa 800 ký tự, không chứa JSON thô hoặc ghi chú về model.
3. Xác nhận đúng tài liệu tham khảo trong `docs/source_registry.json` (`citation_review: verified`). Với hướng chỉ lấy ý tưởng, nội dung xuất phải do nhóm tự viết; đánh dấu `original_wording: true` trong quyết định từng card sau khi kiểm tra. Các trường quyền trích đoạn trong registry chỉ áp dụng khi dùng nguyên văn nội dung nguồn, không phải điều kiện xuất card tự viết.
4. Sau khi người duyệt độc lập xác nhận, đổi `review_status` của card nguồn sang `approved` cùng quyết định biên tập. Chạy lại script. Chỉ card vượt đủ điều kiện mới vào `embedding_ready.jsonl`.

Ví dụ cấu trúc quyết định (minh họa cú pháp, **không phải phê duyệt**):

```json
{
  "cards": {
    "who_2020_001": {
      "status": "pending",
      "citation_verified": false,
      "original_wording": false,
      "curated_text": "",
      "topic_code": "STRESS_REFLECTION",
      "locale": "vi",
      "reviewed_by": null
    }
  }
}
```

## Nối sang backend

Backend có workflow `DRAFT → IN_REVIEW → APPROVED`, chia `knowledge_versions.content` theo `paragraph-800-v1`, bảng `knowledge_embeddings`, adapter `/embeddings` có cấu hình và semantic retrieval. Xem `../../backend/docs/KNOWLEDGE_SEMANTIC_PIPELINE.md` để import và vận hành. Hiện chưa có model embedding cụ thể được chọn/cấu hình, cũng chưa có card được duyệt để nhập.

1. Chạy script import trong `backend/scripts/` để gửi từng card đủ điều kiện tới API quản trị. BE tạo item/version `DRAFT` hoặc bỏ qua bản đã nhập; người khác duyệt trong workflow hiện có.
2. Cấu hình endpoint và model embedding riêng với Chat Completions. BE backfill các chunk đã duyệt vào `knowledge_embeddings`.
3. Dùng API semantic của BE để tìm theo câu hỏi, lọc item `ACTIVE`, version `APPROVED`, locale, topic và ngày hiệu lực. So với retrieval theo topic trước khi nối frontend.

Hiện chưa card nào được duyệt và chưa có model embedding được cấu hình. Do đó chưa có dữ liệu thật để import hoặc backfill.
