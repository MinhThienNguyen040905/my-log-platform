# Sứ mệnh của mylog

## mylog là gì

**mylog** là ứng dụng viết nhật ký và tự suy ngẫm. Người dùng ghi lại một ngày của mình, đánh dấu cảm xúc, rồi nhìn lại theo thời gian để hiểu bản thân rõ hơn và chăm sóc mình tốt hơn.

mylog **không chẩn đoán, không điều trị** và không thay thế chuyên gia sức khỏe tâm thần. Mọi gợi ý chỉ mang tính phản chiếu, giúp người dùng tự nhìn lại.

## Vì sao làm mylog

- Viết ra giúp sắp xếp suy nghĩ, nhưng ít ai duy trì được thói quen và càng ít người quay lại đọc những gì mình đã viết.
- Cảm xúc và áp lực thay đổi từng ngày; khi nhìn lại một khoảng thời gian dài, người ta mới thấy được nhịp và xu hướng của mình.
- Nhật ký là dữ liệu riêng tư nhất của một người. Một công cụ giúp suy ngẫm phải được xây trên sự tin tưởng: người dùng biết dữ liệu của mình ở đâu, ai được đọc và có thể lấy về hoặc xóa đi bất cứ lúc nào.

## Dành cho ai

- **Người dùng chính:** người muốn ghi nhật ký đều đặn, theo dõi cảm xúc và tự chăm sóc bản thân. Giao diện và nội dung ưu tiên tiếng Việt.
- **Nhóm vận hành:** quản trị viên, nhân viên hỗ trợ, biên tập viên và người duyệt nội dung hỗ trợ. Họ quản lý tài khoản và nội dung nhưng **không đọc nhật ký** của người dùng.

## mylog giúp người dùng làm gì

1. **Ghi lại:** viết nhật ký có định dạng, ảnh, nhãn, tâm trạng; check-in cảm xúc nhanh mỗi ngày.
2. **Nhìn lại:** lịch, tìm kiếm, xu hướng cảm xúc, báo cáo, những tiến bộ nhỏ, bản tóm tắt mang đến buổi tham vấn.
3. **Được đồng hành khi viết:** gợi ý câu hỏi và công cụ giúp bình tĩnh, nhận ra các kiểu suy nghĩ lặp lại. Chỉ chạy khi người dùng đồng ý cho AI xử lý.
4. **Được hỗ trợ an toàn:** khi bài viết có dấu hiệu nguy cơ, hiển thị hướng dẫn và nguồn hỗ trợ **đã được con người duyệt**, thay vì phản hồi do AI tự sinh.
5. **Chăm sóc bản thân:** mục tiêu, thói quen, lời nhắn cho chính mình trong tương lai.
6. **Làm chủ dữ liệu:** xuất dữ liệu, bắt đầu lại từ đầu, xóa tài khoản.

Danh sách đầy đủ và chi tiết từng tính năng nằm trong [`spec/mucluc.md`](spec/mucluc.md). Spec là nguồn sự thật duy nhất về hành vi của tính năng.

## Nguyên tắc sản phẩm

1. **An toàn trước tiên.** Mọi nội dung được sàng lọc trước khi AI phản hồi. Khi không chắc chắn, hệ thống chọn hướng an toàn: vẫn lưu bài nhưng không sinh phản hồi. AI không bao giờ tự tạo đường dây nóng hay nguồn hỗ trợ khẩn cấp.
2. **Riêng tư theo mặc định.** Nhật ký và dữ liệu nhạy cảm được mã hóa. Không ghi log nội dung người dùng. Quản trị viên không đọc được nhật ký.
3. **Người dùng là chủ dữ liệu.** AI chỉ xử lý khi người dùng đồng ý, và có thể rút lại bất cứ lúc nào. Người dùng luôn xuất được dữ liệu và xóa được tài khoản.
4. **Nhật ký không phụ thuộc AI.** Bài viết luôn được lưu, kể cả khi AI hoặc bộ sàng lọc gặp lỗi.
5. **Trung thực với dữ liệu.** Insight dựa trên số liệu thật và đủ mẫu; không diễn đạt tương quan thành nguyên nhân; không đưa ra kết luận như chẩn đoán.
6. **Nhẹ nhàng, không phán xét.** Ngôn ngữ khích lệ, không gây áp lực phải viết mỗi ngày hay phải "tốt lên".

## Không nằm trong phạm vi

- Chẩn đoán, sàng lọc lâm sàng, kê đơn hoặc can thiệp khủng hoảng thay cho dịch vụ chuyên môn.
- Mạng xã hội, chia sẻ nhật ký công khai.
- Dùng nhật ký của người dùng để huấn luyện mô hình khi chưa có sự đồng ý rõ ràng và chính sách đã được duyệt.

## Hiện trạng (2026-11-20)

- Backend, web và mobile đã nối với nhau cho các luồng chính; mobile mới kiểm chứng trên Expo web, chưa chạy trên thiết bị thật.
- Các tính năng AI mặc định **tắt** bằng cấu hình. Model safety mới là baseline, chưa được duyệt để xử lý dữ liệu thật; chưa có nguồn hỗ trợ an toàn nào được duyệt.
- Chưa đạt điều kiện phát hành: còn cần môi trường staging, kiểm thử khôi phục dữ liệu, duyệt provider AI và nội dung safety.
