# Use case của mylog

Tài liệu này mô tả **ai sử dụng mylog và họ muốn làm gì**. Một use case bắt đầu từ hành động hoặc nhu cầu của con người. Safety, AI, outbox và worker là cách hệ thống xử lý bên trong một use case, không phải use case độc lập.

mylog hỗ trợ ghi nhật ký, tự nhìn lại cảm xúc và chăm sóc bản thân. Ứng dụng không chẩn đoán hay điều trị. Người dùng là chủ dữ liệu nhật ký; quản trị viên thông thường không được đọc nội dung nhật ký của họ.

## Cách đọc trạng thái

- **Có backend:** Đã có luồng/API backend, nhưng giao diện, cấu hình hoặc nội dung được duyệt có thể chưa đủ để sử dụng trọn vẹn.
- **Demo giao diện:** Giao diện web có tương tác mẫu, chưa kết nối đầy đủ với backend; dữ liệu trình duyệt không phải dữ liệu production.
- **Dự kiến:** Ý tưởng hoặc công việc tiếp theo, chưa phải tính năng hoàn chỉnh.

Trạng thái là ảnh chụp repository ngày 2026-10-07, không phải cam kết phát hành. Ứng dụng mobile hiện chủ yếu là scaffold.

| Người tham gia | Điều họ cần làm |
|---|---|
| Khách chưa đăng nhập | Tạo tài khoản và truy cập mylog. |
| Người dùng | Viết, xem lại nhật ký; nhận hỗ trợ phù hợp; quản lý dữ liệu của mình. |
| Quản trị viên và nhân viên hỗ trợ được phân quyền | Quản lý tài khoản, theo dõi vận hành và xử lý phản hồi. |
| Biên tập viên và người duyệt | Chuẩn bị, kiểm duyệt nội dung hỗ trợ trước khi hiển thị. |

## A. Khách chưa đăng nhập

### UC-01 — Đăng ký và xác minh email

**Mục tiêu:** Có tài khoản cá nhân để bắt đầu sử dụng mylog.

**Luồng:** Khách nhập email và mật khẩu → hệ thống tạo tài khoản → khách xác minh email theo hướng dẫn. Nếu email đã tồn tại hoặc mã xác minh hết hạn, hệ thống báo lỗi phù hợp và cho phép thử lại.

**Hiện trạng:** Có backend; giao diện web là demo và việc gửi email thực tế cần cấu hình.

### UC-02 — Đăng nhập và đăng xuất

**Mục tiêu:** Vào tài khoản của mình và kết thúc phiên sử dụng.

**Luồng:** Người dùng nhập thông tin đăng nhập → hệ thống kiểm tra → mở phiên. Khi đăng xuất, phiên tương ứng bị thu hồi. Tài khoản bị đình chỉ hoặc thông tin sai thì không đăng nhập được.

**Hiện trạng:** Có backend; giao diện web hiện dùng luồng demo.

### UC-03 — Lấy lại mật khẩu

**Mục tiêu:** Khôi phục quyền truy cập khi quên mật khẩu.

**Luồng dự kiến:** Khách yêu cầu đặt lại mật khẩu → nhận liên kết hoặc mã qua email → đặt mật khẩu mới → phiên cũ được xử lý theo chính sách bảo mật.

**Hiện trạng:** Dự kiến; màn hình quên mật khẩu trên web chưa phải quy trình khôi phục thật.

## B. Người dùng: tài khoản và quyền riêng tư

### UC-04 — Hoàn thành thiết lập ban đầu và sửa hồ sơ

**Mục tiêu:** Cá nhân hóa trải nghiệm bằng tên hiển thị, ngôn ngữ, múi giờ và mục tiêu sử dụng.

**Luồng:** Người dùng điền thông tin ban đầu → lưu hồ sơ → có thể xem và sửa về sau. Dữ liệu không hợp lệ được yêu cầu chỉnh lại.

**Hiện trạng:** Có backend; giao diện web mới ở mức demo.

### UC-05 — Chọn hoặc rút lại sự đồng ý xử lý dữ liệu

**Mục tiêu:** Quyết định có cho phép dùng AI xử lý nội dung nhật ký hay không.

**Luồng:** Người dùng xem các lựa chọn đồng ý → bật hoặc rút lại từng lựa chọn → hệ thống ghi nhận phiên bản và thời điểm. Nếu rút đồng ý xử lý AI, những phân tích AI chưa chạy sẽ không được tiếp tục chỉ vì đã được lên lịch trước đó.

**Hiện trạng:** Có backend. Đồng ý phân tích AI không đồng nghĩa đồng ý dùng nhật ký để huấn luyện model.

### UC-06 — Xem và thu hồi phiên đăng nhập

**Mục tiêu:** Kiểm soát các thiết bị đang truy cập tài khoản.

**Luồng:** Người dùng xem danh sách phiên → chọn thu hồi một phiên hoặc các phiên khác → thiết bị bị thu hồi phải đăng nhập lại.

**Hiện trạng:** Có backend.

## C. Người dùng: nhật ký và ghi nhận hằng ngày

### UC-07 — Nhận gợi ý để viết tiếp

**Mục tiêu:** Có một câu hỏi gợi mở khi đang viết và bị bí ý.

**Luồng mong muốn:** Người dùng dừng gõ khoảng 3–5 giây → ứng dụng hỏi backend → hiện một gợi ý ngắn nếu phù hợp. Người dùng có thể bỏ qua; gợi ý không tự ghi vào bài.

**Hiện trạng:** Backend có endpoint gợi ý mẫu, mặc định tắt và chưa dùng AI tạo sinh; editor web chưa nối luồng này. Việc chờ 3–5 giây là hành vi frontend dự kiến.

### UC-08 — Viết và lưu nhật ký

**Mục tiêu:** Giữ lại điều mình đã trải qua, kể cả khi dịch vụ AI gặp lỗi.

**Luồng:** Người dùng viết và nhấn Lưu → hệ thống lưu bài → trả kết quả lưu. Sau đó hệ thống tự kiểm tra an toàn và, khi có đủ điều kiện, phân tích bài để tạo phản chiếu hoặc dữ liệu cảm xúc. Người dùng không phải chờ toàn bộ phân tích mới lưu xong.

**Trường hợp cần chú ý:** Nếu bài có dấu hiệu nguy cơ cao, hệ thống đi theo luồng hỗ trợ an toàn và không tạo phản chiếu thông thường. Nếu bộ kiểm tra không hoạt động, bài vẫn được lưu nhưng phản hồi AI thông thường tạm dừng. Đây là hai tình huống khác nhau; hệ thống không tự kết luận tình trạng y khoa của người viết.

**Hiện trạng:** Có backend; web hiện chủ yếu lưu bản demo trong trình duyệt. Phân tích còn phụ thuộc consent, chính sách và provider được duyệt.

### UC-09 — Đọc, tìm và xem lại nhật ký

**Mục tiêu:** Tìm lại bài đã viết theo ngày hoặc bộ lọc phù hợp.

**Luồng:** Người dùng mở danh sách hoặc lịch → chọn bài → đọc nội dung và các thông tin liên quan của chính mình. Bài của tài khoản khác không được hiển thị.

**Hiện trạng:** Có backend; web là demo.

### UC-10 — Sửa, đánh dấu và xóa nhật ký

**Mục tiêu:** Chủ động quản lý bài đã viết.

**Luồng:** Người dùng sửa nội dung, gắn nhãn/yêu thích hoặc xóa bài → hệ thống lưu thay đổi → phân tích cũ không còn phù hợp sẽ không được hiển thị như kết quả của bản mới. Khi hai nơi cùng sửa một bài, hệ thống báo xung đột để tránh ghi đè âm thầm.

**Hiện trạng:** Có backend cho các thao tác nhật ký chính; mức tích hợp web còn là demo.

### UC-11 — Thêm ảnh vào nhật ký

**Mục tiêu:** Minh họa trải nghiệm bằng ảnh của mình.

**Luồng mong muốn:** Người dùng chọn ảnh → tải lên → thấy ảnh gắn với bài → có thể gỡ ảnh. Chỉ chủ tài khoản được truy cập ảnh theo quyền phù hợp.

**Hiện trạng:** Có nền tảng Cloudinary và metadata; luồng giao diện và kiểm chứng đầu cuối cần hoàn thiện.

### UC-12 — Check-in cảm xúc hằng ngày

**Mục tiêu:** Ghi nhanh cảm xúc, năng lượng hoặc thông tin tự đánh giá trong ngày ngay cả khi không viết bài dài.

**Luồng:** Người dùng mở check-in → chọn mức và thông tin muốn ghi → lưu → có thể xem lại theo ngày. Khi hiển thị xu hướng, check-in là dữ liệu chính của ngày; quan sát từ nhật ký chỉ là nguồn dự phòng có ghi rõ.

**Hiện trạng:** Có backend; giao diện web cần kiểm tra tích hợp thực tế.

## D. Người dùng: phản chiếu và hỗ trợ sức khỏe tinh thần

### UC-13 — Xem phản chiếu sau khi lưu bài

**Mục tiêu:** Nhận một phản hồi giúp tự suy ngẫm về bài đã viết.

**Luồng mong muốn:** Người dùng mở bài → thấy trạng thái đang chờ hoặc phản chiếu đã có → đọc khi hệ thống hoàn tất. Nếu bài được sửa, kết quả cũ không được coi là của phiên bản mới.

**Trường hợp cần chú ý:** Không có consent, thiếu phê duyệt provider/chính sách, lỗi phân tích hoặc bài thuộc luồng safety thì không hiển thị phản chiếu thông thường. Bài nhật ký vẫn tồn tại.

**Hiện trạng:** Có backend và nền xử lý bất đồng bộ; việc chạy thật phụ thuộc cổng cấu hình và phê duyệt.

### UC-14 — Nhận hướng dẫn an toàn khi có dấu hiệu nguy cơ

**Mục tiêu:** Được hướng tới hỗ trợ phù hợp khi nội dung có dấu hiệu cần chú ý ngay.

**Luồng:** Người dùng lưu hoặc đang viết nội dung liên quan → hệ thống kiểm tra → nếu rơi vào mức cần can thiệp theo chính sách, ứng dụng hiển thị thông điệp và nguồn hỗ trợ đã được duyệt. Người dùng vẫn quyết định có liên hệ hỗ trợ hay không.

**Hiện trạng:** Có nền safety ở backend; nội dung và nguồn hỗ trợ thực tế cần được duyệt trước khi bật. Hệ thống không tự bịa số điện thoại hoặc chẩn đoán người dùng.

### UC-15 — Xem xu hướng cảm xúc và báo cáo

**Mục tiêu:** Hiểu sự thay đổi theo tuần/tháng từ các ghi nhận của chính mình.

**Luồng:** Người dùng mở dashboard hoặc báo cáo → chọn khoảng thời gian → xem các chỉ số, chủ đề và nhận xét có ghi rõ nguồn/số lượng dữ liệu. Khi dữ liệu quá ít, ứng dụng nói rõ chưa đủ cơ sở để kết luận xu hướng.

**Hiện trạng:** Có backend cho insight/báo cáo; một số luồng theo lịch hoặc phần diễn giải AI cần cấu hình và kiểm duyệt thêm.

### UC-16 — Đặt mục tiêu và theo dõi thói quen tự chăm sóc

**Mục tiêu:** Thử những hành động nhỏ phù hợp với bản thân và theo dõi việc thực hiện.

**Luồng:** Người dùng tạo mục tiêu/thói quen → đánh dấu đã làm → xem tiến trình → có thể sửa hoặc dừng. Nội dung được diễn đạt như gợi ý tự chăm sóc, không phải phác đồ điều trị.

**Hiện trạng:** Có backend; cần kiểm tra từng màn hình web trước khi coi là tích hợp hoàn chỉnh.

### UC-17 — Xem nội dung và nguồn hỗ trợ đã duyệt

**Mục tiêu:** Tìm bài viết hoặc tài nguyên hỗ trợ đáng tin cậy.

**Luồng:** Người dùng duyệt nội dung, nhận gợi ý phù hợp hoặc mở danh sách nguồn hỗ trợ an toàn → chỉ thấy nội dung đã qua quy trình duyệt và còn hiệu lực.

**Hiện trạng:** Có API/nền quản lý nội dung và nguồn safety; nguồn hỗ trợ thật hiện cần được xác minh, duyệt và nhập vào hệ thống.

### UC-18 — Viết lời nhắn cho chính mình trong tương lai

**Mục tiêu:** Chuẩn bị một lời động viên do chính mình viết để xem lại lúc khó khăn.

**Luồng dự kiến:** Khi cảm thấy ổn, người dùng viết và chọn lưu lời nhắn → về sau có thể tự mở lại hoặc cho phép ứng dụng gợi ý xem lại khi phù hợp. Người dùng có quyền sửa, xóa và tắt nhắc lại.

**Hiện trạng:** Dự kiến. Không tự gửi chỉ vì một chỉ số cảm xúc thấp; cần quyền chọn rõ ràng và tránh dùng trong tình huống khủng hoảng thay cho hỗ trợ an toàn.

### UC-19 — Xem lại những tiến bộ nhỏ

**Mục tiêu:** Nhớ lại các việc tích cực nhỏ mà mình từng ghi nhận.

**Luồng dự kiến:** Người dùng lưu hoặc xác nhận một “điểm sáng” từ nhật ký → xem chúng trong một bộ sưu tập riêng → sửa/xóa mục bất kỳ. Việc trích xuất tự động chỉ diễn ra khi có sự đồng ý phù hợp.

**Hiện trạng:** Dự kiến.

### UC-20 — Tạo bản tóm tắt mang đến buổi tham vấn

**Mục tiêu:** Tự chuẩn bị thông tin muốn chia sẻ với chuyên gia.

**Luồng dự kiến:** Người dùng chọn thời gian và loại dữ liệu → xem trước bản tóm tắt trung tính về giấc ngủ, năng lượng và chủ đề lặp lại → tải xuống hoặc chủ động chia sẻ. Mặc định không kèm nguyên văn nhật ký nhạy cảm; chuyên gia không có quyền truy cập tài khoản qua tính năng này.

**Hiện trạng:** Dự kiến. Chức năng export dữ liệu cá nhân hiện có không đồng nghĩa đã có bản handout này.

## E. Người dùng: phản hồi và quyền đối với dữ liệu

### UC-21 — Gửi phản hồi về nội dung hoặc gợi ý

**Mục tiêu:** Báo rằng một phản hồi AI, nội dung hoặc trải nghiệm là hữu ích hay có vấn đề.

**Luồng:** Người dùng chọn mục muốn phản hồi → gửi đánh giá và mô tả tùy chọn → hệ thống ghi nhận để người phụ trách xem xét. Không đưa nguyên văn nhật ký vào phản hồi nếu người dùng không chủ động chọn.

**Hiện trạng:** Có backend cho phản hồi; cần kiểm tra tích hợp giao diện.

### UC-22 — Xuất dữ liệu cá nhân

**Mục tiêu:** Lấy bản sao dữ liệu của chính mình.

**Luồng:** Người dùng yêu cầu xuất → hệ thống chuẩn bị file → người dùng tải trong thời gian hiệu lực. File hết hạn được dọn dẹp; chỉ chủ tài khoản nhận được file.

**Hiện trạng:** Có backend xuất CSV/PDF bằng job, giới hạn dung lượng và thời hạn theo chính sách hiện tại.

### UC-23 — Xóa tài khoản

**Mục tiêu:** Chấm dứt sử dụng và yêu cầu xóa dữ liệu cá nhân.

**Luồng:** Người dùng xác nhận yêu cầu → tài khoản chuyển sang quy trình xóa → hệ thống xóa/thu hồi dữ liệu liên quan theo chính sách, kể cả tài nguyên bên ngoài khi áp dụng.

**Hiện trạng:** Có backend cho yêu cầu và job xóa; cần kiểm chứng vận hành trước khi coi toàn bộ vòng đời production đã sẵn sàng.

## F. Quản trị viên và nhóm nội dung

### UC-24 — Quản lý trạng thái tài khoản người dùng

**Tác nhân:** Quản trị viên có quyền phù hợp.

**Mục tiêu:** Xử lý tài khoản vi phạm hoặc cần hỗ trợ vận hành.

**Luồng:** Quản trị viên tra cứu metadata cần thiết → đình chỉ hoặc khôi phục theo quyền → hệ thống ghi audit. Quản trị viên thông thường không mở hoặc giải mã nhật ký thô.

**Hiện trạng:** Có backend cho một số thao tác quản trị; giao diện và phân quyền chi tiết cần đối chiếu khi tích hợp.

### UC-25 — Theo dõi vận hành và xử lý tác vụ lỗi

**Tác nhân:** Quản trị viên/nhân viên vận hành được phân quyền.

**Mục tiêu:** Biết hệ thống có xử lý được các yêu cầu xuất, xóa và phân tích hay không.

**Luồng:** Xem trạng thái tổng hợp và tác vụ lỗi → xác định nguyên nhân từ metadata an toàn → thử lại tác vụ được phép hoặc chuyển cho người phụ trách. Màn hình vận hành không hiển thị nội dung nhật ký hay prompt thô.

**Hiện trạng:** Có nền API/job ở backend; quy trình vận hành và giao diện đầy đủ còn cần hoàn thiện.

### UC-26 — Xem và xử lý phản hồi người dùng

**Tác nhân:** Nhân viên hỗ trợ hoặc người duyệt được phân quyền.

**Mục tiêu:** Phát hiện nội dung chưa phù hợp và cải thiện trải nghiệm.

**Luồng:** Mở hàng đợi phản hồi → xem thông tin được phép → phân loại, xử lý hoặc chuyển tiếp → ghi lại kết quả. Quyền xem nội dung nhạy cảm không tự sinh ra từ quyền quản trị chung.

**Hiện trạng:** Có backend ghi nhận phản hồi; quy trình xử lý cần kiểm tra theo vai trò và giao diện thực tế.

### UC-27 — Biên tập và duyệt nội dung hỗ trợ

**Tác nhân:** Biên tập viên và người duyệt nội dung.

**Mục tiêu:** Chỉ cho người dùng thấy nội dung đáng tin cậy và còn hiệu lực.

**Luồng:** Biên tập viên soạn/sửa bài → gửi duyệt → người có quyền duyệt xuất bản hoặc trả lại → có thể gỡ/đưa vào lưu trữ khi nội dung cũ. Người dùng chỉ thấy bản đã xuất bản.

**Hiện trạng:** Có backend cho quy trình knowledge/content; nguồn safety và các câu trả lời khẩn cấp cần quy trình phê duyệt riêng trước khi sử dụng.

### UC-28 — Duyệt nguồn hỗ trợ và quy tắc phản hồi an toàn

**Tác nhân:** Người chịu trách nhiệm safety được phân quyền.

**Mục tiêu:** Bảo đảm thông điệp và nguồn hỗ trợ được kiểm chứng trước khi hiển thị cho người dùng.

**Luồng dự kiến:** Xem nguồn, nội dung, phiên bản và bằng chứng kiểm chứng → phê duyệt hoặc từ chối → theo dõi hết hạn và rút phê duyệt khi cần. Thay đổi này ảnh hưởng luồng UC-14.

**Hiện trạng:** Có cổng chính sách và dữ liệu nền ở backend; chưa có bộ nguồn hỗ trợ thực tế được duyệt đầy đủ.

## Những việc không đặt thành use case của người dùng

Lưu outbox event, worker tạo AI job, kiểm tra consent/safety, gọi classifier/analyzer, train model và cấu hình API key là **bước triển khai**. Chúng phục vụ các mục tiêu như UC-08, UC-13, UC-14 và UC-15. Xem [AI_ANALYSIS_GUIDE.md](AI_ANALYSIS_GUIDE.md), [BACKEND_ARCHITECTURE.md](BACKEND_ARCHITECTURE.md) và [SELF_COMPASSION_FEATURE_PROPOSALS.md](SELF_COMPASSION_FEATURE_PROPOSALS.md) khi cần thiết kế chi tiết.
