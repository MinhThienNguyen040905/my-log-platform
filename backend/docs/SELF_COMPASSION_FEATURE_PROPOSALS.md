# Đề xuất tính năng: kết nối quá khứ và chuẩn bị tìm hỗ trợ

Trạng thái: **đề xuất sản phẩm, chưa triển khai**. Tài liệu này mô tả trải nghiệm mong muốn, không xác nhận API, bảng dữ liệu, model hay nội dung hỗ trợ đã được xây dựng hoặc phê duyệt. mylog hỗ trợ self-reflection và chăm sóc bản thân; không chẩn đoán, điều trị hoặc thay thế chuyên gia.

## 1. Thư gửi tương lai

**Nhu cầu:** Người dùng muốn tự chuẩn bị một lời nhắn để đọc lại vào ngày khó khăn. Lời nhắn do chính họ viết; AI không viết thay và không diễn giải nó thành lời khuyên lâm sàng.

**Trải nghiệm đề xuất:** Người dùng chủ động tạo, sửa, xóa lời nhắn và chọn cách mở lại. Khi check-in hoặc bài viết gợi ý một ngày khó khăn, giao diện chỉ mời xem một cách kín đáo: “Bạn có muốn đọc lời nhắn mình từng để lại không?”. Người dùng có thể bỏ qua, tắt gợi ý hoặc tự mở thư bất kỳ lúc nào. Không tự gửi push notification hay tự mở nội dung chỉ vì một điểm năng lượng thấp. Điểm số thấp không đồng nghĩa với tình huống khủng hoảng.

**Ranh giới:** Safety screening hiện hành vẫn chạy độc lập trên bài viết. Nếu kết quả là `SAFETY_FLOW`, nguồn hỗ trợ và phản hồi an toàn đã duyệt được ưu tiên; lá thư không thay thế hoặc trì hoãn luồng đó. Nếu classifier không khả dụng, không dùng lá thư làm cách vượt policy gate. Cần đánh giá với người dùng xem việc đọc lời nhắn cũ có thể gây khó chịu trong tình huống nào.

## 2. Điểm sáng nhỏ (Micro-Wins Vault)

**Nhu cầu:** Giữ lại những việc nhỏ người dùng thấy có ý nghĩa để họ tự xem lại, không buộc mọi bài viết phải có mặt tích cực.

**Trải nghiệm đề xuất:** Người dùng có thể tự lưu một điểm sáng. Sau khi bài đã lưu và đủ điều kiện phân tích, hệ thống *có thể* đề xuất một đoạn ngắn có nguồn từ bài viết hiện tại. Giao diện cho xem câu gốc/ngữ cảnh phù hợp, sửa, chấp nhận hoặc bỏ qua trước khi lưu. Không tự thêm vào vault, không tạo thành tích không có trong bài và không gắn nhãn “chiến thắng” cho một câu mơ hồ. Có thể xem, sửa và xóa các mục đã lưu; không nhắc lại ngoài ngữ cảnh người dùng chọn.

**Ranh giới AI:** Trích xuất là một tác vụ riêng cần đánh giá chất lượng và safety; `sentiment`, `emotions` hoặc `topics` hiện có không đủ để suy ra micro-win. Chỉ phân tích khi consent còn hiệu lực, safety cho phép và `contentVersion` còn khớp. Có thể bắt đầu bằng nhập thủ công trước khi thêm AI. Văn bản trích xuất là dữ liệu nhạy cảm, cần mã hóa, owner-scope và nằm trong export/deletion khi thiết kế persistence.

## 3. Bản chuẩn bị buổi tham vấn (Therapist Handout)

**Nhu cầu:** Người dùng muốn mang một bản ghi chú ngắn, trung tính tới cuộc trao đổi với chuyên gia hoặc người hỗ trợ mà không phải chia sẻ toàn bộ nhật ký.

**Trải nghiệm đề xuất:** Người dùng chủ động chọn khoảng thời gian và các mục muốn đưa vào. Bản xem trước khoảng một trang có thể gồm số ngày có check-in, xu hướng mood/giấc ngủ/năng lượng **do người dùng ghi nhận**, chủ đề lặp lại nếu analysis đủ điều kiện, và câu hỏi hoặc điều họ muốn trao đổi do họ tự viết. Mỗi mục có thể bỏ trước khi xuất. Mặc định không đưa nguyên văn nhật ký, reflection, safety event, suy đoán nguyên nhân hay nhãn chẩn đoán. Nêu rõ nguồn dữ liệu, số mẫu và ngày tạo; thiếu dữ liệu thì để trống hoặc ghi “chưa đủ dữ liệu”.

**Ranh giới chia sẻ:** Chỉ tạo khi người dùng yêu cầu và xác nhận bản xem trước; không gửi tự động cho chuyên gia, email hay bên thứ ba. Khác với export toàn bộ dữ liệu cá nhân của M7: handout là bản chọn lọc do người dùng kiểm soát. Nếu xuất PDF, cần xử lý artifact mã hóa, TTL, tải có xác thực và xóa theo chính sách export hiện hành hoặc một quyết định mới được ghi rõ. Báo cáo không được trình bày như hồ sơ hay kết luận lâm sàng.

## 4. Thứ tự khám phá và tiêu chí trước khi triển khai

1. **Micro-Wins Vault:** thử luồng nhập thủ công và quyền sửa/xóa; chỉ thêm trích xuất AI sau khi có tập đánh giá synthetic và bước xác nhận của người dùng.
2. **Thư gửi tương lai:** thử cách tạo, mở và tắt gợi ý với người dùng; kiểm tra riêng tình huống người dùng không muốn nhận nhắc lại.
3. **Therapist Handout:** chốt trường dữ liệu, bản xem trước và quyền loại từng mục; kiểm thử thiếu dữ liệu, quyền sở hữu và xuất/xóa artifact.

Trước khi chuyển bất kỳ mục nào thành công việc triển khai, cần chốt acceptance criteria, consent và retention, API/error contract, quyền sở hữu, migration mới nếu cần, kiểm thử safety/privacy và cách đo giá trị từ phản hồi tự nguyện của người dùng. Không coi tần suất sử dụng, streak hoặc điểm cảm xúc là bằng chứng sức khỏe tinh thần đã cải thiện.
