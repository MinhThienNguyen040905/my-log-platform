# Kế hoạch huấn luyện model phân loại nguy cơ cho mylog

> Trạng thái: **thiết kế và baseline có thể train; chưa có model được duyệt cho production**.  
> Phạm vi: model `RiskClassifier` trước bước AI journal analysis. Tài liệu này không mô tả việc train LLM tạo reflection hay model embedding.

Đọc cùng [AI analysis guide](AI_ANALYSIS_GUIDE.md), [ADR-0003](adr/0003-layered-safety-screening.md), [ADR-0004](adr/0004-ai-provider-and-data-handling.md), [ADR-0009](adr/0009-hybrid-ai-inference.md) và [pipeline hiện có](../../safety-model/README.md). Khi tài liệu này mô tả tiêu chí đánh giá hoặc quy trình duyệt, đó là **yêu cầu trước khi triển khai**, không phải kết quả đã đạt.

## 1. Model giải quyết việc gì?

Model đọc **title và plain text của một journal entry** để ước lượng mức nguy cơ an toàn khi ứng dụng chuẩn bị tạo phản hồi AI. Nó trả một nhãn và độ tin cậy; nó **không chẩn đoán**, không đánh giá toàn diện tình trạng sức khỏe tâm thần, không tự chọn nội dung hỗ trợ và không quyết định có gọi LLM hay không.

```text
JournalService
  → rule có phiên bản: dấu hiệu rõ ràng HIGH/CRITICAL thì đi safety flow
  → RiskClassifier: phân loại các trường hợp còn lại
  → SafetyPolicyGate: kiểm tra cấu hình rule/model/ngưỡng đã được duyệt
  → SafetyDecision: ALLOW, CONSTRAIN, SAFETY_FLOW hoặc FAIL_SAFE
```

Model được gọi **đồng bộ khi lưu hoặc sửa journal**. Nếu service timeout, lỗi, trả dữ liệu sai hoặc độ tin cậy không đủ, backend vẫn lưu bài nhưng không tạo reflection thông thường. Worker kiểm tra lại quyết định safety và consent ngay trước lúc phân tích. Luồng hiện tại nằm trong [SafetyScreeningService](../src/main/java/com/mylog/safety/application/SafetyScreeningService.java) và [AnalysisJobHandler](../src/main/java/com/mylog/analysis/application/AnalysisJobHandler.java).

### Những model không thuộc kế hoạch train này

| Thành phần | Kế hoạch hiện tại |
|---|---|
| `JournalAnalyzer` | Dùng API key trong worker sau khi provider và output safety được duyệt. Chưa train LLM riêng. |
| Embedding/RAG | Đánh giá model embedding có sẵn trên kho kiến thức đã duyệt trước khi cân nhắc fine-tune. |
| Output safety | Cần bộ đánh giá và cơ chế kiểm tra riêng; risk classifier đầu vào không được tái sử dụng như bằng chứng reflection an toàn. |
| Rule và policy gate | Là code/cấu hình do người phụ trách safety duyệt, không phải model được train. |

## 2. Nhãn và nguyên tắc gắn nhãn

Hợp đồng hiện tại dùng năm nhãn `NORMAL`, `LOW`, `MODERATE`, `HIGH`, `CRITICAL`; service có thể trả `UNKNOWN` khi không đủ chắc chắn. **Định nghĩa nghiệp vụ chính xác, ví dụ biên và hành động tương ứng phải được người phụ trách safety phê duyệt trước khi gắn nhãn dữ liệu thật.** Không tự suy diễn nhãn chỉ từ một từ khóa.

| Nhãn | Cách hiểu sơ bộ để thiết kế corpus | Tác động hiện tại |
|---|---|---|
| `NORMAL` | Không có dấu hiệu nguy cơ trong phạm vi screening đã định nghĩa. | Chỉ mở ordinary analysis khi policy được duyệt và confidence đạt ngưỡng. |
| `LOW` | Có tín hiệu nhẹ/không trực tiếp, cần phân biệt với buồn bã thông thường. | Cùng điều kiện policy như `NORMAL`. |
| `MODERATE` | Tín hiệu đáng chú ý nhưng chưa thuộc nhóm nguy cơ cao theo rubric được duyệt. | `CONSTRAIN`; hiện **không** tạo job reflection thông thường. |
| `HIGH` | Tín hiệu nguy cơ cao theo rubric được duyệt. | `SAFETY_FLOW`; không tạo reflection thông thường. |
| `CRITICAL` | Tín hiệu nguy cơ tức thời/nghiêm trọng theo rubric được duyệt. | `SAFETY_FLOW`; không tạo reflection thông thường. |
| `UNKNOWN` | Model không chắc, lỗi hoặc kết quả không hợp lệ. | `FAIL_SAFE`; lưu bài, chặn AI và có thể rescreen. |

Corpus cần có tiếng Việt và tiếng Anh, dấu tiếng Việt, tiếng lóng, câu phủ định, trích dẫn, hồi tưởng quá khứ, mỉa mai, câu rất ngắn, ngữ cảnh dài và các trường hợp không có nguy cơ nhưng trùng từ khóa. Dữ liệu có rủi ro cao cần nhãn do người được đào tạo duyệt; bất đồng nhãn phải được adjudicate và ghi nhận. Không dùng output của LLM làm nhãn chuẩn nếu chưa có người kiểm tra.

## 3. Dữ liệu train và quyền sử dụng

Pipeline nhận hai CSV UTF-8 độc lập (`train.csv`, `holdout.csv`) với cột `text,label`. Script **không tự trích xuất journal từ production DB**. Nguồn dữ liệu phải có hồ sơ nguồn gốc, quyền sử dụng và mục đích đã duyệt. Ưu tiên corpus synthetic và dữ liệu được cấp phép; nếu dùng nội dung người dùng thì cần `MODEL_TRAINING` consent rõ ràng và quy trình privacy riêng. `AI_PROCESSING` chỉ cho inference trên bài viết, không cho train.

Trước khi đưa dữ liệu vào pipeline:

1. Xác minh quyền sử dụng từng nguồn và phiên bản consent tại thời điểm lấy dữ liệu.
2. Loại bỏ thông tin định danh không cần thiết; hạn chế người được đọc bản rõ. Không coi việc ẩn tên là bảo đảm dữ liệu đã vô danh hoàn toàn.
3. Tách người viết/nguồn và các biến thể gần trùng giữa train và holdout để tránh đánh giá lạc quan. Script hiện chỉ bắt **trùng text chính xác**; kiểm tra gần trùng phải làm thêm trong quy trình dữ liệu.
4. Mã hóa tập dữ liệu khi lưu và truyền, giới hạn TTL và quyền truy cập. Không commit CSV, journal thật hoặc artifact model vào Git; không log mẫu text.
5. Chốt cách xử lý khi người dùng rút consent hoặc xóa tài khoản **trước khi** train trên dữ liệu người dùng, gồm khả năng truy dấu mẫu đã dùng và quyết định retrain/retire artifact liên quan. Pipeline hiện chưa tự động thực hiện bước này.

Holdout phải được thu thập và gắn nhãn độc lập, đóng băng trước khi chọn ngưỡng. Nếu dùng nhiều vòng thử trên cùng holdout, cần một tập đánh giá cuối chưa từng dùng để chọn model/ngưỡng. Không công bố hoặc đưa ví dụ journal thật vào báo cáo đánh giá.

## 4. Baseline và lựa chọn model

[`safety-model/train.py`](../../safety-model/train.py) hiện tạo **baseline**: TF-IDF ký tự 2–5 gram → logistic regression có cân bằng lớp → hiệu chỉnh xác suất bằng `CalibratedClassifierCV`. Lựa chọn này nhỏ, nhanh và dễ kiểm tra luồng train/inference; **không có giả định rằng nó đủ tốt cho safety production**, đặc biệt với ngữ cảnh phức tạp. Script yêu cầu tối thiểu 20 mẫu train mỗi lớp như một kiểm tra đầu vào, **không phải kích thước dữ liệu đủ để được duyệt**.

Nếu baseline không đạt tiêu chí đã chốt, đánh giá một encoder đa ngôn ngữ được fine-tune cho cùng bộ nhãn, cùng split và cùng hợp đồng inference. So sánh chất lượng, độ trễ, độ ổn định giữa tiếng Việt/Anh, bộ nhớ và khả năng rollback. Quyết định nâng cấp dựa trên kết quả đánh giá, không dựa trên tên hoặc kích thước model. Mỗi thay đổi kiến trúc model tạo một model version mới; backend vẫn gọi `RiskClassifier` như cũ.

Lệnh mẫu, chạy trong `safety-model/` với dữ liệu synthetic/được phép dùng:

```powershell
python train.py --train train.csv --holdout holdout.csv --output artifacts/v1 --version v1
```

Đầu ra gồm `model.joblib` và `manifest.json`. Manifest ghi version, số mẫu, báo cáo holdout theo lớp, confusion matrix, hash dữ liệu/artifact và phiên bản Python/scikit-learn; `approved` ban đầu là `false`. Artifact `joblib` chỉ được load từ pipeline tin cậy vì định dạng này có thể thực thi mã khi giải tuần tự hóa.

## 5. Đánh giá và điều kiện phê duyệt

**Accuracy tổng thể hoặc macro F1 một mình không đủ.** Team safety phải chốt ngưỡng chấp nhận và mức rủi ro tồn dư trước khi xem kết quả final holdout. Báo cáo tối thiểu cần:

| Nhóm đo | Câu hỏi cần trả lời |
|---|---|
| Recall `HIGH`/`CRITICAL` và false negative | Model bỏ sót bao nhiêu trường hợp nguy cơ cao? Chúng bị nhầm sang nhãn nào? |
| Precision và false positive | Bao nhiêu bài không nguy cơ bị chặn? Ảnh hưởng tới trải nghiệm và khả năng rescreen? |
| Confusion matrix năm lớp | `MODERATE`, `HIGH`, `CRITICAL` bị lẫn với `NORMAL/LOW` như thế nào? |
| Calibration và abstention | Confidence có phản ánh tần suất đúng không? Tăng ngưỡng `UNKNOWN` đổi chất lượng và tỷ lệ chặn ra sao? |
| Slice evaluation | Kết quả riêng theo tiếng Việt/Anh, phủ định, trích dẫn, tiếng lóng, độ dài và các nhóm dữ liệu được phép đo. |
| End-to-end pipeline | Rule + classifier + policy gate có cải thiện safety so với từng thành phần riêng lẻ không? |
| Vận hành | p95/p99 latency, timeout, lỗi, tải đồng thời và thời gian rollback. |

Không điền một tỷ lệ “đạt” tùy ý vào code hoặc tài liệu này. Người phụ trách safety phải duyệt **rubric, dataset, metric, ngưỡng, model version và policy version** cùng nhau. Bộ synthetic regression của backend vẫn phải chạy mỗi lần đổi rule/model/ngưỡng. Đánh giá trên staging cần xác minh trường hợp dependency chết: journal vẫn lưu, ordinary AI bị chặn và không rò raw content vào log.

## 6. Hợp đồng inference và vị trí triển khai

Model chạy trong service riêng; Spring backend gọi qua [HttpRiskClassifier](../src/main/java/com/mylog/safety/infrastructure/HttpRiskClassifier.java). Service chỉ nhận text cần phân loại, không cần user ID, email hoặc journal ID:

```http
POST /classify
Authorization: Bearer <internal-service-token>
Content-Type: application/json

{"text":"Ví dụ synthetic để kiểm tra hợp đồng"}
```

```json
{"level":"LOW","confidence":0.91,"version":"v1"}
```

Backend gắn `provider=MYLOG_INTERNAL`, kiểm tra nhãn/confidence/version, rồi chuyển sang policy gate. Production dùng endpoint HTTPS riêng tư và token từ secret manager; HTTP chỉ được adapter chấp nhận trên loopback cho local/test. Timeout mặc định 2 giây, giới hạn cấu hình tối đa 10 giây. Không bật access log chứa request body. `safety-model/serve.py` trả `UNKNOWN` nếu xác suất cao nhất thấp hơn `MYLOG_SAFETY_MODEL_MIN_CONFIDENCE` (mặc định `0.8`). Sau đó policy gate của backend còn kiểm tra `minConfidence` riêng; hai ngưỡng này phải được đánh giá và version hóa cùng nhau.

Policy hiện yêu cầu row `APPROVED`, người và thời điểm phê duyệt, khoảng hiệu lực, `allowOrdinaryAnalysis=true`, đúng `ruleVersion`, `classifierProvider=MYLOG_INTERNAL`, `classifierVersion` và `minConfidence`. Row này **không được tự tạo hoặc tự phê duyệt bởi pipeline train**. Mọi safety event lưu mức nguy cơ/quyết định/provenance tối thiểu, không lưu đoạn văn kích hoạt.

## 7. Phát hành, theo dõi và rollback

1. Train và đánh giá bằng dữ liệu được phép dùng; giữ model artifact bất biến cùng manifest và hồ sơ duyệt. Không sửa lại artifact dưới cùng version.
2. Triển khai service inference ở mạng nội bộ với artifact được mount chỉ đọc; kiểm tra hash, version, token, TLS, liveness và timeout. Không đưa CSV train vào image/service inference.
3. Chạy synthetic smoke test và shadow evaluation được kiểm soát trước khi cho model ảnh hưởng quyết định. Không ghi raw journal vào telemetry; chỉ metric tổng hợp theo model/rule/policy version và ngưỡng privacy.
4. Phê duyệt và kích hoạt policy đúng tổ hợp rule/model/ngưỡng. Sau đó mới cấu hình `MYLOG_SAFETY_CLASSIFIER_URL` và token cho backend. Bật classifier **không tự bật journal analyzer**; provider và output safety có release gate riêng.
5. Theo dõi lỗi/timeout, `UNKNOWN`, phân bố nhãn, tỷ lệ chặn/rescreen, drift và các lỗi được đánh giá lại. Dừng ordinary AI nếu safety dependency hoặc policy không còn hợp lệ.
6. Rollback bằng cách vô hiệu URL classifier hoặc quay về model artifact/version và policy đã duyệt trước đó. Backend fail-safe trong lúc chuyển đổi. Không sửa lịch sử `safety_events` hoặc policy cũ.

Runbook triển khai nằm ở [M8_RELEASE_RUNBOOK.md](M8_RELEASE_RUNBOOK.md). V16 đã dành cho mã xác minh email; mọi thay đổi schema tiếp theo cần migration V17 trở lên. Kế hoạch classifier này hiện **không yêu cầu migration**.

## 8. Những việc còn thiếu trước production

- Corpus và rubric được người phụ trách safety duyệt; dữ liệu train/holdout độc lập và đủ đại diện.
- Báo cáo chất lượng, calibration, latency và end-to-end safety đạt ngưỡng được chốt trước khi đánh giá final holdout.
- Quy trình thu hồi consent/xóa dữ liệu huấn luyện và artifact nếu dùng journal thật.
- Artifact/model version được phê duyệt; policy phù hợp có hiệu lực; nguồn hỗ trợ safety đã xác minh.
- Kiểm thử PostgreSQL/staging, fail-safe, log redaction và rollback; không dựa vào một test local duy nhất.

Cho tới khi các mục này hoàn thành, `MYLOG_SAFETY_CLASSIFIER_URL` để trống và ordinary AI vẫn bị chặn theo fail-safe hiện tại.
