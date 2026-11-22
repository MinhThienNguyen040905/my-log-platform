# Hướng dẫn gán nhãn kiểu suy nghĩ (`tp-v1`)

> **Trạng thái:** bản nháp, chờ duyệt. Dùng cho tập đánh giá [`eval-tp-v1.jsonl`](../src/test/resources/thinking-patterns/eval-tp-v1.jsonl) và cho prompt của bộ trích tín hiệu. Kế hoạch tổng thể: [THINKING_PATTERN_RECOGNITION_PLAN.md](THINKING_PATTERN_RECOGNITION_PLAN.md).
>
> Mọi câu ví dụ trong tài liệu và tập dữ liệu là **câu tổng hợp**, không lấy từ nhật ký thật.

## 1. Quy tắc chung

1. **Gán nhãn theo từng câu.** Chỉ dựa vào chính câu đó, không đoán từ bối cảnh bên ngoài.
2. **Một câu có thể có 0, 1 hoặc nhiều nhãn.** Phần lớn câu trong nhật ký thật **không có nhãn nào**.
3. **Cảm xúc tiêu cực không phải là bẫy suy nghĩ.** "Mình buồn", "mình lo", "mình mệt" là cảm xúc bình thường. Chỉ gán nhãn khi có **cách suy luận** đặc trưng: cường điệu, khái quát hóa, đoán ý, tự dán nhãn…
4. **Có căn cứ thì không gán.** Lo vì chưa ôn bài, nhận lỗi khi thật sự mắc lỗi, "phải" làm theo hạn chót có thật là phản ứng hợp lý.
5. **Đã tự điều chỉnh thì không gán.** Ví dụ: "mình nghĩ tới điều tệ nhất, nhưng rồi nhắc mình khả năng đó rất thấp".
6. **Lời của người khác không tính.** "Bạn trêu mình là đồ ham ăn" không phải là tự dán nhãn.
7. **Không chắc thì không gán.** Tính năng ưu tiên độ chính xác: bỏ sót không gây hại, báo nhầm làm mất niềm tin.
8. **Ngoài phạm vi:** nội dung tự hại hoặc khủng hoảng thuộc luồng safety, không gán nhãn trong bộ này. Bộ nhãn cũng không có tên bệnh hay tính cách.

## 2. Nhóm A: Bẫy suy nghĩ

### `CATASTROPHIZING`: Đoán trước điều tệ nhất
- **Là:** từ một sự việc nhỏ hoặc chưa xảy ra, nhảy thẳng tới kết cục xấu nhất và coi nó là chắc chắn hoặc không thể chịu nổi.
- **Dấu hiệu:** "lỡ… thì coi như hết", "chắc chắn sẽ…", chuỗi hậu quả leo thang ("rớt môn → mất học bổng → nghỉ học").
- **Không gán khi:** lo có căn cứ kèm cách xử lý; tính phương án dự phòng một cách bình thản.
- **Phân biệt với `WORRY_LOOP`:** catastrophizing là **một cú nhảy** tới kết cục tệ nhất. Worry loop là **lo lặp đi lặp lại** nhiều kịch bản và không dừng được. Một câu có thể có cả hai.

### `ALL_OR_NOTHING`: Tất cả hoặc không có gì
- **Là:** đánh giá theo hai cực: hoàn hảo hoặc thất bại, không có mức ở giữa.
- **Dấu hiệu:** "không… thì coi như công cốc", "thất bại hoàn toàn", "hoặc là… hoặc là thôi".
- **Không gán khi:** sự việc thật sự chỉ có hai khả năng (đề đúng/sai, chọn một trong hai việc làm).
- **Phân biệt với `PERFECTIONISM`:** all-or-nothing là **cách đánh giá kết quả**. Perfectionism là **tiêu chuẩn và hành vi** (làm lại mãi, không dám nộp).

### `OVERGENERALIZATION`: Vơ đũa cả nắm
- **Là:** từ một hoặc vài sự việc, kết luận thành quy luật chung về mình, người khác hay cuộc sống.
- **Dấu hiệu:** "lúc nào cũng", "chẳng bao giờ", "ai cũng", "mãi mãi", "lần nào cũng" mang nghĩa tiêu cực.
- **Không gán khi:** "luôn" mô tả thói quen trung tính ("mình luôn uống nước ấm"); thống kê thật kèm hành động.

### `MIND_READING`: Đoán ý người khác
- **Là:** cho rằng mình biết người khác nghĩ gì (thường là tiêu cực về mình) mà không có thông tin trực tiếp.
- **Dấu hiệu:** "chắc họ nghĩ…", "ai cũng thấy mình…", suy từ nét mặt hoặc sự im lặng ra suy nghĩ.
- **Không gán khi:** người kia **đã nói ra**; mình nói rõ là chưa biết và định hỏi; suy luận trung tính có căn cứ.

### `SHOULD_STATEMENTS`: Áp lực "phải"
- **Là:** quy tắc cứng về việc mình hay người khác *phải* hoặc *không được* làm gì, *đáng lẽ* phải ra sao. Thường đi kèm tự trách khi không đạt.
- **Dấu hiệu:** "phải luôn…", "không được…", "đáng lẽ…", "lẽ ra không nên cảm thấy…".
- **Không gán khi:** nghĩa vụ hoặc hạn chót thật ("phải nộp trước 5 giờ"), lời khuyên thực tế ("nên mang ô"), mong muốn tự chọn ("mình muốn tập đều hơn").

### `SELF_LABELING`: Dán nhãn bản thân
- **Là:** gắn cho cả con người mình một nhãn cố định, thường là danh từ, từ một hành động.
- **Dấu hiệu:** "mình đúng là đồ…", "mình là kẻ…", "mình là loại người…", "mình ngu thật".
- **Không gán khi:** mô tả hành động ("mình làm vỡ cốc"); tự mô tả trung tính ("mình là người hướng nội"); nhãn do người khác nói.
- **Phân biệt với `HARSH_SELF_CRITICISM`:** labeling có **một nhãn cụ thể**. Harsh self-criticism là **giọng điệu** mắng mỏ, cho rằng mình không xứng đáng, không nhất thiết có nhãn. Có cả hai thì gán cả hai.

### `PERSONALIZATION`: Nhận hết lỗi về mình
- **Là:** coi mình là nguyên nhân chính của sự việc tiêu cực, dù có nhiều yếu tố khác hoặc việc nằm ngoài tầm kiểm soát.
- **Dấu hiệu:** "tại mình mà…", "chắc là do mình…", "lỗi của mình" với chuyện của người khác.
- **Không gán khi:** mình thật sự mắc lỗi cụ thể và nhận đúng phần của mình; buồn vì chuyện người khác mà không tự nhận lỗi.

### `DISCOUNTING_POSITIVE`: Gạt bỏ điều tốt
- **Là:** cho rằng điều tích cực không đáng kể, chỉ là may mắn, hoặc người khác nói cho có.
- **Dấu hiệu:** "chỉ là may", "chắc nói cho có", "có gì đâu, ai chả làm được", "chuyện nhỏ thôi".
- **Không gán khi:** ghi nhận cân bằng ("một phần nhờ chuẩn bị, một phần nhờ may"); sự thật khách quan (cả phòng được tăng lương).

### `EMOTIONAL_REASONING`: Lấy cảm xúc làm bằng chứng
- **Là:** coi cảm xúc là bằng chứng cho sự thật: "mình thấy X nên X là thật".
- **Dấu hiệu:** "thấy… nên chắc… thật", "cảm giác như… vậy chắc là…".
- **Không gán khi:** cảm giác cơ thể dẫn tới hành động hợp lý ("thấy mệt nên ngủ sớm"); dùng cảm giác như tín hiệu để **kiểm tra lại** chứ không coi là kết luận.

## 3. Nhóm B: Vòng lặp và thói quen

### `RUMINATION`: Nghĩ đi nghĩ lại chuyện đã qua
- **Là:** lặp lại suy nghĩ về chuyện **đã xảy ra**, không đi tới kết luận hay hành động.
- **Dấu hiệu:** "cứ nghĩ mãi", "tua lại", "không dứt ra được", "giá như…" lặp đi lặp lại.
- **Không gán khi:** suy ngẫm có kết luận hoặc bài học; hồi tưởng nhẹ nhàng; kể ra để nhẹ lòng.
- **Phân biệt với `WORRY_LOOP`:** rumination hướng về **quá khứ**, worry loop hướng về **tương lai**.

### `WORRY_LOOP`: Lo lắng vòng tròn
- **Là:** lo lặp lại về chuyện **chưa xảy ra**, nhảy từ mối lo này sang mối lo khác, biết là không giúp gì nhưng không dừng được.
- **Dấu hiệu:** "lo không ngủ được", "nghĩ đủ kịch bản", "lỡ… lỡ… lỡ…", kiểm tra đi kiểm tra lại.
- **Không gán khi:** lo một lúc rồi chuyển sang chuẩn bị hoặc lập kế hoạch; kiểm tra một lần cho chắc.

### `AVOIDANCE`: Né tránh
- **Là:** tránh việc, người hoặc thông tin vì **sợ hay khó chịu**, dù biết sớm muộn cũng phải đối mặt.
- **Dấu hiệu:** "không dám mở", "để máy im lặng", "viện cớ", "lần lữa vì sợ".
- **Không gán khi:** ranh giới lành mạnh có chủ đích; lý do thực tế (đang lái xe, trùng lịch).
- **Phân biệt với `PROCRASTINATION_GUILT`:** avoidance nhấn vào **nỗi sợ hoặc khó chịu** khi đối mặt. Procrastination guilt nhấn vào **trì hoãn rồi tự trách**.

### `PROCRASTINATION_GUILT`: Trì hoãn rồi tự trách
- **Là:** trì hoãn việc cần làm, sau đó thấy tội lỗi, chán ghét bản thân, và vòng lặp tiếp diễn.
- **Dấu hiệu:** "lại lướt cả tối… ghét bản thân", "tự trách", "áy náy", "lần nào cũng hứa không lặp lại".
- **Không gán khi:** nghỉ có chủ đích và thấy thoải mái; dời việc có lý do và không tự trách; còn kịp hạn.

### `PERFECTIONISM`: Cầu toàn
- **Là:** đặt tiêu chuẩn quá cao: làm lại mãi, không dám nộp hay bắt đầu, chỉ thấy lỗi nhỏ.
- **Dấu hiệu:** "làm lại lần thứ…", "chưa đủ tốt", "chưa hoàn hảo nên chưa…", "chỉ nhìn vào câu sai".
- **Không gán khi:** yêu cầu chính xác thật của công việc; kiểm tra một lần trước khi gửi; muốn làm tốt và tập thêm.

### `PEOPLE_PLEASING`: Khó từ chối, đặt người khác lên trước
- **Là:** đồng ý hoặc nhường nhịn **vì sợ** người khác buồn, giận hay mất lòng, đi ngược nhu cầu của mình.
- **Dấu hiệu:** "không dám từ chối", "sợ họ buồn", "ngại từ chối", nhận hết dù quá tải.
- **Không gán khi:** giúp vì muốn và còn sức; từ chối được; xin lỗi khi thật sự có lỗi; thật sự không quan trọng chuyện chọn gì.

### `HARSH_SELF_CRITICISM`: Giọng nói bên trong khắt khe
- **Là:** tự mắng mỏ nặng nề, cho rằng mình không xứng đáng, chỉ thấy khuyết điểm.
- **Dấu hiệu:** "thảm hại", "không xứng đáng", "tự mắng mình", "chẳng ra gì", "đáng xấu hổ".
- **Không gán khi:** nhận xét kết quả bằng giọng tử tế ("lần sau cần chuẩn bị kỹ hơn"); tự nhận chưa giỏi một việc nhưng đang học.

### `SOCIAL_WITHDRAWAL`: Thu mình
- **Là:** rút khỏi kết nối xã hội một cách kéo dài hoặc né tránh: không muốn gặp ai, không trả lời, từ chối mọi lời mời.
- **Dấu hiệu:** "không muốn gặp ai", "ở trong phòng cả tuần", "tắt hết thông báo", "không bắt máy".
- **Không gán khi:** nghỉ ngơi có chủ đích sau giai đoạn bận rộn; thích ở một mình mà thấy dễ chịu; có lý do thực tế (ốm, bận).

## 4. Định dạng tập đánh giá

Mỗi dòng của file `.jsonl` là một câu:

```json
{"id":"p-cat-01","kind":"POSITIVE","labels":["CATASTROPHIZING"],"text":"..."}
{"id":"h-cat-01","kind":"HARD_NEGATIVE","labels":[],"confusableWith":"CATASTROPHIZING","why":"...","text":"..."}
{"id":"m-01","kind":"MULTI","labels":["SELF_LABELING","OVERGENERALIZATION"],"text":"..."}
{"id":"n-001","kind":"NEUTRAL","labels":[],"text":"..."}
```

| `kind` | Ý nghĩa |
|---|---|
| `POSITIVE` | Có đúng một nhãn rõ ràng |
| `MULTI` | Có từ hai nhãn trở lên |
| `HARD_NEGATIVE` | Nghe giống một mã (`confusableWith`) nhưng **không** gán; `why` giải thích lý do |
| `NEUTRAL` | Câu nhật ký bình thường, không nhãn |

Khi duyệt, sửa trực tiếp trường `labels` (hoặc xóa dòng). Thêm `"reviewed":true` vào dòng đã duyệt. Thêm `"note":"..."` nếu muốn ghi lại lý do sửa.
