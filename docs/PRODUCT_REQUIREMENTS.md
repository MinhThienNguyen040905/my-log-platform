# MYLOG – PRODUCT REQUIREMENTS DOCUMENT

| Field | Value |
| --- | --- |
| **Phiên bản** | 1.0 |
| **Trạng thái** | Draft – Product Scope Locked |
| **Loại sản phẩm** | Web Application |
| **Thời gian MVP dự kiến** | 1 tháng |
| **Nhân sự** | 2 thành viên |

## 1. Product Overview

### 1.1. Tên sản phẩm

MyLog

### 1.2. Mô tả sản phẩm

MyLog là nền tảng nhật ký thông minh sử dụng AI và dữ liệu cá nhân theo thời gian để giúp người dùng:

- ghi lại suy nghĩ và cảm xúc hằng ngày;
- nhận diện trạng thái cảm xúc;
- phát hiện những chủ đề, tình huống và hành vi thường xuất hiện cùng các trạng thái cảm xúc;
- tự phản tư thông qua các câu hỏi gợi mở;
- nhận các hành động nhỏ để cân nhắc cải thiện bản thân;
- theo dõi sự thay đổi cảm xúc theo thời gian.

MyLog không chỉ lưu trữ nhật ký mà biến dữ liệu nhật ký thành thông tin có cấu trúc để người dùng hiểu bản thân tốt hơn.

## 2. Product Vision

### 2.1. Vision Statement

MyLog giúp người dùng nhận diện và hiểu cảm xúc của chính mình dựa trên dữ liệu cá nhân, từ đó hỗ trợ quá trình tự phản tư và lựa chọn những hành động tích cực cho bản thân.

### 2.2. Core Value

**Giá trị cốt lõi của sản phẩm:**

- Ghi lại → Nhận diện → Hiểu → Tự vấn → Hành động
- Trong đó, Nhận diện là giá trị quan trọng nhất.

## 3. Problem Statement

Người dùng thường có thể nhận biết mình đang buồn, stress, tức giận hoặc mất động lực tại một thời điểm nhưng khó trả lời các câu hỏi:

- Tình trạng này bắt đầu từ khi nào?
- Nó xuất hiện thường xuyên hay chỉ nhất thời?
- Chủ đề nào thường xuất hiện khi tâm trạng xấu?
- Hành động hoặc hoàn cảnh nào thường lặp lại trước những cảm xúc tiêu cực?
- Trạng thái hiện tại khác tuần hoặc tháng trước như thế nào?
- Điều gì thường đi cùng những ngày tâm trạng tốt hơn?
- Nhật ký truyền thống giúp lưu lại trải nghiệm nhưng yêu cầu người dùng tự đọc và phân tích lượng lớn dữ liệu.

Chatbot AI có khả năng phân tích văn bản nhưng thường:

- thiếu dữ liệu lịch sử có cấu trúc;
- thiếu ngữ cảnh dài hạn của người dùng;
- không tập trung vào journaling;
- không có hệ thống theo dõi pattern;
- không có dashboard dữ liệu;
- phụ thuộc vào việc người dùng tự cung cấp context mỗi lần trò chuyện.

MyLog giải quyết khoảng trống này bằng việc xây dựng lịch sử dữ liệu cá nhân có cấu trúc.

## 4. Product Positioning

**Thứ tự định vị sản phẩm:**

- Smart Journaling Application
- Personal Emotional Analytics
- Self-Reflection Assistant
- Mental Wellness Support Application
- Mood Tracker
- MyLog không được định vị là chatbot trị liệu tâm lý.

MyLog cũng không phải công cụ chẩn đoán y khoa.

## 5. Target Users

### 5.1. Primary User

Người trẻ đang chịu áp lực trong học tập, công việc hoặc cuộc sống và gặp khó khăn trong việc chia sẻ cảm xúc với người khác.

**Ví dụ:**

- sinh viên;
- người trẻ mới đi làm;
- người có áp lực học tập;
- người có áp lực công việc;
- người thường xuyên cảm thấy stress;
- người muốn hiểu bản thân tốt hơn;
- người có hoặc chưa có thói quen viết nhật ký.

### 5.2. Age

Product Direction
14+.
MVP Assumption

**Đối với phiên bản MVP, khuyến nghị giới hạn:**

- 18+
- Nhóm 14–17 tuổi được đưa vào Future Scope sau khi giải quyết đầy đủ các yêu cầu về consent, safeguarding, privacy và AI provider.

## 6. Primary Persona

Persona – Người trẻ chịu nhiều áp lực
Đặc điểm
Sinh viên hoặc người trẻ mới đi làm.
Có nhiều vấn đề khó chia sẻ với người khác.
Thường xuyên suy nghĩ nhiều.
Không nhất thiết đã có thói quen viết nhật ký.
Sử dụng cả máy tính và điện thoại.
Muốn hiểu tại sao bản thân thường xuyên cảm thấy stress, buồn hoặc mất động lực.
Mục tiêu
“Tôi muốn có một nơi để viết ra những điều đang xảy ra, sau đó nhìn lại để hiểu điều gì đang ảnh hưởng đến cảm xúc của mình.”

## 7. Jobs To Be Done

### `JTBD-01` - Emotional Release

Khi có nhiều suy nghĩ hoặc cảm xúc, tôi muốn viết chúng ra để giải tỏa và lưu lại trải nghiệm của mình.

### `JTBD-02` - Emotional Awareness

Khi cảm thấy bản thân gần đây không ổn, tôi muốn nhìn lại dữ liệu của mình để hiểu trạng thái cảm xúc đã thay đổi như thế nào.

### `JTBD-03` - Pattern Discovery

Khi một cảm xúc tiêu cực xuất hiện nhiều lần, tôi muốn biết những chủ đề hoặc hoàn cảnh nào thường xuất hiện cùng cảm xúc đó.

### `JTBD-04` - Self Reflection

Sau khi viết nhật ký, tôi muốn nhận những câu hỏi phù hợp để suy nghĩ sâu hơn về điều mình vừa trải qua.

### `JTBD-05` - Improvement

Khi nhận ra một pattern tiêu cực, tôi muốn có một hành động nhỏ mà mình có thể thử để cải thiện tình trạng đó.

## 8. Product Principles

MyLog tuân theo 6 nguyên tắc.

### P1. Journal First

Trải nghiệm chính luôn bắt đầu từ việc viết nhật ký.
Không biến ứng dụng thành chatbot.

### P2. Data Before AI Conclusion

AI không được đưa ra các kết luận dài hạn chỉ từ một journal entry.
Insight dài hạn phải dựa trên dữ liệu lịch sử.

### P3. Evidence-Based Insight

Insight phải có bằng chứng định lượng khi có thể.

**Ví dụ:**

- Deadline xuất hiện trong 6/8 ngày có stress cao.

**Thay vì:**

- Bạn thường stress vì deadline.

### P4. Association, Not Diagnosis

**MyLog chỉ xác định:**

- pattern;
- association;
- correlation;
- frequency;
- trend.

Không khẳng định quan hệ nhân quả hoặc chẩn đoán bệnh.

### P5. Human-in-the-loop

**Người dùng được quyền sửa:**

- emotion;
- topic;
- kết quả AI không chính xác.

Dữ liệu do người dùng sửa được ưu tiên hơn kết quả AI ban đầu.

### P6. Privacy by Design

Dữ liệu journal được xem là dữ liệu nhạy cảm.
Việc thu thập, phân tích và gửi dữ liệu đến AI provider phải minh bạch với người dùng.

## 9. Core Product Loop

MyLog có hai vòng lặp chính.

### 9.1. Awareness Loop

```text
Journal
   ↓
AI Analysis
   ↓
Structured Data
   ↓
Historical Analysis
   ↓
Insight
   ↓
Self Reflection
```

Đây là vòng lặp chính của MVP.

### 9.2. Lightweight Feedback Loop

```text
Insight
   ↓
Suggested Action
   ↓
User Accept / Ignore
   ↓
Helpful / Not Helpful
   ↓
Future Personalization
```

MVP không xây dựng hệ thống thử nghiệm hành vi dài hạn phức tạp.

## 10. Primary User Journey

### Step 1 - Authentication

Người dùng đăng ký hoặc đăng nhập.

### Step 2 - Journal

Sau khi đăng nhập, người dùng được đưa trực tiếp tới khu vực viết nhật ký.

**Editor hiển thị placeholder:**

- “Hôm nay điều gì khiến bạn vui nhất?”

**Người dùng có thể:**

- viết tự do;
- chọn mood;
- sử dụng nút gợi ý nếu chưa biết viết gì.

### Step 3 - Save

Journal được autosave trong khi người dùng viết.
Khi hoàn thành, người dùng lưu journal.

### Step 4 - AI Analysis

**Hệ thống tự động phân tích:**

- sentiment;
- emotion;
- topic;
- risk level.

### Step 5 - Review Analysis

Người dùng xem kết quả AI.
Người dùng có thể sửa emotion hoặc topic nếu AI nhận diện sai.

### Step 6 - Reflection

Hệ thống tạo tối đa 3 câu hỏi tự phản tư.
Người dùng có thể yêu cầu câu hỏi khác.

### Step 7 - Historical Analysis

Khi đã có đủ dữ liệu, MyLog phân tích các pattern.

### Step 8 - Insight

**Người dùng xem:**

- trend;
- correlation;
- recurring topic;
- emotion pattern;
- day-of-week pattern.

### Step 9 - Suggested Action

Đối với insight phù hợp, AI có thể đưa ra một hành động nhỏ.

### Step 10 - Feedback

**Người dùng đánh giá:**

- Helpful;
- Not Helpful.

Feedback được sử dụng cho cá nhân hóa tương lai.

## 11. Journal Feature

### 11.1. Journal Content

**Mỗi journal bao gồm:**

- nội dung văn bản;
- mood;
- thời gian;
- optional stress;
- optional energy;
- optional topic/tag;
- optional icon;
- optional image attachment.

AI MVP chỉ phân tích nội dung văn bản.
Image understanding nằm ngoài MVP.

### 11.2. Journal Length

Không đặt giới hạn cứng gần 200 từ.
200 từ được xem là độ dài sử dụng điển hình.

### 11.3. Required Fields

**Bắt buộc:**

- journal content;
- mood.

**Không bắt buộc:**

- stress;
- energy;
- sleep;
- tags.

### 11.4. Mood Scale

**Mood:**

- 1–10

### 11.5. Multiple Journal Entries

Một ngày có thể có nhiều journal entry.

**Mood của ngày được tính:**

- Average Mood = trung bình mood của các journal trong ngày.

### 11.6. Autosave

Journal Editor phải hỗ trợ autosave draft.

### 11.7. Editing

**Khi journal cũ được chỉnh sửa:**

- journal được lưu lại;
- AI Analysis được chạy lại;
- dữ liệu derived liên quan được cập nhật;
- insight hiện tại có thể được tính lại.

### 11.8. Deletion

**Khi xóa journal:**

- hiển thị confirmation;
- cảnh báo việc xóa có thể ảnh hưởng tới insight/report;
- journal analysis liên quan phải được xóa;
- insight hiện tại liên quan được tính lại.

## 12. Writing Prompt

Có hai loại câu hỏi.
Type A – Writing Prompt
Dùng trước khi người dùng viết.

**Mục đích:**

- Giúp người dùng bắt đầu viết dễ dàng hơn.

**Ví dụ:**

- Hôm nay điều gì khiến bạn suy nghĩ nhiều nhất?
Writing Prompt chỉ xuất hiện khi người dùng chủ động nhấn:

- Gợi ý cho tôi
- Không yêu cầu người dùng chọn category.

## 13. Reflection Questions

Type B – Reflection Question
Xuất hiện sau khi journal được phân tích.

**Mục tiêu:**

- Giúp người dùng suy nghĩ sâu hơn theo mạch cảm xúc vừa viết.

**Mỗi lần hiển thị tối đa:**

- 3 câu hỏi

**Người dùng có thể:**

- trả lời;
- bỏ qua;
- yêu cầu câu hỏi khác;
- đánh giá Helpful / Not Helpful.

AI có thể sử dụng journal cũ để cá nhân hóa câu hỏi khi phù hợp.
AI không nên tự động nhắc lại những sự kiện nhạy cảm nếu việc đó có nguy cơ gây distress không cần thiết.

## 14. AI Journal Analysis

Sau khi journal được lưu, hệ thống tự động phân tích.

### 14.1. Sentiment

**Giá trị:**

- Positive
- Neutral
- Negative
- Sentiment được hiển thị cho người dùng.

### 14.2. Emotion

Một journal có thể chứa nhiều emotion.
Emotion taxonomy phải cố định để có thể thống kê theo thời gian.

**Ví dụ taxonomy có thể bao gồm:**

- Joy
- Sadness
- Anger
- Fear
- Anxiety
- Hope
- Gratitude
- Loneliness
- Calm
- Frustration
- Excitement
- Danh sách chính thức sẽ được khóa trong SRS.

**Emotion có thể có score:**

- 0–100%

### 14.3. Topic

Topic do AI tạo.

**Ví dụ:**

- deadline;
- work;
- family;
- finance;
- relationship;
- study.

Không sử dụng taxonomy cố định bắt buộc cho topic.
Người dùng được chỉnh sửa topic.

### 14.4. User Correction

**Nếu người dùng sửa:**

- AI: Study

User: Work

**thì:**

- Work trở thành dữ liệu chính thức được sử dụng trong historical analysis.

### 14.5. Summary

Journal Summary không nằm trong MVP.

## 15. AI Explainability

Người dùng có thể xem lý do AI nhận diện một emotion hoặc topic.

**Ví dụ:**

- Anxiety được nhận diện vì journal nhiều lần nhắc đến việc lo lắng về deadline và không hoàn thành công việc đúng hạn.

**AI explanation phải:**

- ngắn;
- dễ hiểu;
- tránh phán đoán;
- không sử dụng ngôn ngữ chẩn đoán.

## 16. Historical Analysis Engine

Historical Analysis là một trong những chức năng quan trọng nhất của MyLog.

**MVP hỗ trợ:**

### 16.1. Frequency Analysis

**Ví dụ:**

- “Deadline xuất hiện trong 6/8 journal có stress ≥ 8.”

### 16.2. Correlation Analysis

**Ví dụ:**

- sleep ↔ mood;
- stress ↔ mood;
- energy ↔ mood.

### 16.3. Topic – Mood Relationship

**Ví dụ:**

- Journal chứa topic “Work” có mood trung bình 4.8.

### 16.4. Day-of-week Pattern

**Ví dụ:**

- Thứ Hai có mood trung bình thấp hơn các ngày còn lại.

### 16.5. Trend Detection

**Ví dụ:**

- Mood trung bình giảm trong 3 tuần liên tiếp.

### 16.6. Period Comparison

**Hỗ trợ:**

- tuần này vs tuần trước;
- tháng này vs tháng trước.

## 17. Minimum Data Requirement

Không tạo historical insight khi chưa đủ dữ liệu.

**Mốc tối thiểu ban đầu:**

- 3 ngày có journal.

Tuy nhiên một số loại insight có thể yêu cầu nhiều sample hơn.
Threshold chính xác sẽ được định nghĩa trong SRS.

## 18. Insight Generation Architecture

LLM không trực tiếp đọc journal rồi tự kết luận toàn bộ.

**Pipeline:**

- Journal Data
- ↓
- Structured Analysis
- ↓
- Statistical Engine
- ↓
- Calculated Evidence
- ↓
- LLM Interpretation
- ↓
- User-friendly Insight

**Ví dụ Statistical Engine tạo:**

- sampleSize = 18
- correlation = 0.62
- supportFrequency = 77%

LLM chỉ thực hiện nhiệm vụ diễn giải dữ liệu.

## 19. Insight Confidence

Confidence phải dựa trên dữ liệu.

**Các yếu tố gồm:**

- sample size;
- frequency;
- consistency;
- dữ liệu gần đây.

Không hiển thị confidence như độ chính xác y khoa.

**UI ưu tiên:**

- Weak Pattern;
- Moderate Pattern;
- Strong Pattern.

## 20. Insight Lifecycle

Insight có ba trạng thái.
ACTIVE
Pattern đang được dữ liệu gần đây tiếp tục hỗ trợ.
FADING
Pattern từng xuất hiện rõ nhưng dữ liệu mới cho thấy pattern đang yếu đi.
EXPIRED
Pattern không còn đủ bằng chứng trong khoảng dữ liệu hiện tại.
Insight được lưu trong database.

## 21. Dashboard

### 21.1. Mục tiêu

**Dashboard phải giúp người dùng trả lời nhanh:**

- “Cảm xúc của tôi gần đây đang thay đổi như thế nào?”

### 21.2. Default Range

**Mặc định:**

- 7 ngày

### 21.3. Main Chart

Line chart hiển thị dữ liệu theo thời gian.

**Có thể bao gồm:**

- mood;
- stress;
- energy;
- sleep khi có dữ liệu.

### 21.4. Emotion Distribution

Hiển thị emotion distribution bằng Distribution Chart.

### 21.5. Mood Calendar

Có calendar visualization giúp người dùng quan sát mood theo ngày.

### 21.6. Topic Frequency

Hiển thị số lần topic xuất hiện trong khoảng thời gian lựa chọn.
Dashboard MVP không hỗ trợ customization.

## 22. Reports

### 22.1. Weekly Report

**Weekly report tự động tạo:**

- Chủ nhật

**Điều kiện:**

- Ít nhất 3 journal trong kỳ.

### 22.2. Report Pipeline

Structured Data
      ↓
Statistics
      ↓
Pattern Detection
      ↓
LLM Interpretation
      ↓
Report

### 22.3. Weekly Report Content

**Có thể bao gồm:**

- số journal;
- average mood;
- emotion distribution;
- frequent topics;
- notable patterns;
- comparison với tuần trước;
- reflection questions;
- suggested actions.

### 22.4. Monthly Report

Monthly Report là P1, triển khai sau khi Weekly Report ổn định.

### 22.5. Report Immutability

Report được xem là snapshot của dữ liệu tại thời điểm tạo.

**Journal được sửa sau đó:**

- không tự động rewrite report cũ;
- report có thể được đánh dấu STALE;
- insight hiện tại sử dụng dữ liệu mới.

MVP không hỗ trợ regenerate report thủ công.

## 23. Suggested Actions

AI có thể tạo 1–3 hành động nhỏ dựa trên insight.

**Ví dụ:**

- Insight
- Deadline xuất hiện trong phần lớn những ngày stress cao.

Suggested Action
Thử dành 10 phút chia deadline lớn nhất hiện tại thành 3 nhiệm vụ nhỏ.

**Suggested Action phải:**

- nhỏ;
- cụ thể;
- có thể thực hiện;
- không phải điều trị y khoa;
- không yêu cầu người dùng thay đổi thuốc;
- không đưa ra kết luận sức khỏe.

## 24. User Feedback

**Người dùng có thể đánh giá:**

- 👍 Helpful
- 👎 Not Helpful
- Feedback được lưu lại.

**Trong tương lai, feedback được sử dụng để:**

- ưu tiên kiểu câu hỏi phù hợp;
- hạn chế câu hỏi không hữu ích;
- cá nhân hóa Suggested Action.

MVP chỉ cần lưu feedback và sử dụng một số rule đơn giản.
Không cần xây recommendation learning system hoàn chỉnh.

## 25. Safety System

### 25.1. Risk Levels

NORMAL
LOW
MODERATE
HIGH
CRITICAL

### 25.2. Safety Pipeline

Journal
   ↓
Risk Detection
   ↓
Risk Level
   ↓
NORMAL / LOW / MODERATE
   ↓
Normal AI Pipeline

**Trong trường hợp:**

- HIGH / CRITICAL

hệ thống chuyển sang Safety Flow.

### 25.3. High-risk Response

**MyLog:**

- dừng Suggested Action thông thường;
- hiển thị safety popup;
- khuyến khích tìm hỗ trợ từ con người;
- cung cấp lựa chọn hỗ trợ phù hợp;
- không khẳng định AI có thể xử lý khủng hoảng.

### 25.4. Emergency Contact

Emergency Contact được lưu riêng.
EmergencyContact

name
relationship
phone

MVP không tự động gọi hoặc nhắn tin cho Emergency Contact.

### 25.5. Safety Event

**Safety Event ghi:**

- risk level;
- timestamp;
- journal reference;
- safety action đã kích hoạt.

Không lưu Emergency Contact trực tiếp trong SafetyEvent.

## 26. Privacy

Người dùng phải được thông báo khi journal được gửi đến AI provider bên ngoài để phân tích.

**Người dùng phải có khả năng:**

- xóa journal;
- xóa account;
- xóa dữ liệu cá nhân;
- xuất dữ liệu.

Khi account bị xóa, hệ thống phải xóa hoặc xử lý phù hợp:

- journals;
- analysis;
- insights;
- reports;
- feedback;
- safety records;
- personal profile data.

MyLog không sử dụng journal của người dùng để huấn luyện model của chính MyLog nếu chưa có consent.

## 27. Free and Plus

Product direction có hai plan.
FREE

**Giới hạn một số tài nguyên như:**

- số lần AI analysis;
- số historical insight;
- số report;
- advanced AI features.

PLUS
Có quyền sử dụng nhiều hơn hoặc không giới hạn trong chính sách sản phẩm.
MVP
Không triển khai payment gateway.

**Database chỉ cần hỗ trợ:**

- FREE
- PLUS

để demo authorization theo plan.
Giới hạn chính xác giữa Free và Plus là Open Decision.

## 28. MVP Scope

P0 – MUST HAVE
Authentication
Register
Login
Logout
JWT Authentication
Journaling
Create journal
Read journal
Update journal
Delete journal
Multiple journals/day
Mood input
Autosave
AI Analysis
Sentiment
Emotion
Topic
Risk detection
Automatic analysis
Human Correction
Edit emotion
Edit topic
Reflection
Writing prompt
Reflection questions
Request another question
Analytics
Daily mood calculation
Emotion statistics
Topic frequency
Basic correlations
Basic trend detection
Day-of-week pattern
Insight
Generate insight
Evidence
Confidence category
Active/Fading/Expired
Dashboard
7-day view
Line chart
Emotion distribution
Mood calendar
Topic frequency
Suggested Action
AI-generated small action
Accept / Ignore
Feedback
Helpful
Not Helpful
Safety
Risk classification
Safety popup
SafetyEvent

## 29. P1 – SHOULD HAVE

**Nếu còn thời gian:**

- Weekly Report
- Monthly comparison
- PDF export
- Image attachment
- Emergency Contact
- Monthly Report
- Free/Plus limits.

## 30. Future Scope

**Không triển khai trong MVP:**

- complete Goal Management;
- habit tracking engine;
- long-term action evaluation;
- RAG;
- mobile application;
- OAuth;
- biometric lock;
- advanced notification;
- semantic journal search;
- payment gateway;
- advanced recommendation engine;
- automatic emergency contact;
- image emotion analysis;
- medical diagnosis;
- therapy chatbot.

## 31. Success Metrics

North Star Metric
Weekly Reflection User – WRU

**Một user được tính là WRU khi trong một tuần:**

- Journal Entries >= 3

AND

Insight / Report Viewed >= 1

Supporting Metrics

**Theo dõi:**

- journals/user/week;
- weekly active users;
- percentage of users viewing insight;
- percentage of users answering reflection;
- Helpful rate;
- journal completion rate;
- AI correction rate;
- weekly retention.

## 32. MVP Demo Success Criteria

**Một demo thành công phải thể hiện được flow:**

- Write Journal
- ↓
- AI Emotion Analysis
- ↓
- Reflection Question
- ↓
- Historical Statistics
- ↓
- Insight
- ↓
- Suggested Action
- ↓
- User Feedback

**5 capability quan trọng nhất khi bảo vệ:**

- Viết nhật ký.

AI nhận diện cảm xúc/topic.
Câu hỏi tự phản tư.
Thống kê dữ liệu lịch sử và phát hiện insight.
Đề xuất hành động nhỏ dựa trên insight.

## 33. Why MyLog Instead of ChatGPT?

MyLog khác chatbot AI thông thường ở việc xây dựng dữ liệu lịch sử có cấu trúc.

**Chatbot:**

- Prompt
- ↓
- Response

**MyLog:**

- Journal History
- +
- Mood Data
- +
- Emotion
- +
- Topic
- +
- User Corrections
- ↓
- Statistical Analysis
- ↓
- Historical Pattern
- ↓
- LLM Interpretation
- ↓
- Personalized Reflection

Giá trị kỹ thuật quan trọng nhất không nằm ở việc AI có thể trả lời một journal.

**Giá trị nằm ở việc:**

- MyLog xây dựng lịch sử dữ liệu cá nhân, phân tích pattern theo thời gian và sử dụng AI để diễn giải dữ liệu đó cho người dùng.

## 34. Key Product Risks

Risk 1 – Scope quá lớn

**Biện pháp:**

- Ưu tiên P0 và bỏ các tính năng P1 khi cần.

Risk 2 – LLM hallucination

**Biện pháp:**

- Statistical Engine tính dữ liệu trước.

LLM chỉ diễn giải structured evidence.
Risk 3 – User hiểu insight như chẩn đoán

**Biện pháp:**

**Sử dụng ngôn ngữ:**

- “Dữ liệu cho thấy…”
- “Có xu hướng…”
- “Thường xuất hiện cùng…”

**Không sử dụng:**

- “Bạn mắc…”
- “Nguyên nhân chắc chắn là…”
- “Bạn bị…”
- Risk 4 – Thiếu dữ liệu
- Không tạo insight khi sample không đủ.

Risk 5 – AI classification sai
Cho phép user correction.
Risk 6 – Sensitive Data
Áp dụng privacy controls và AI consent.

## 35. Open Decisions

**Các vấn đề cần khóa khi viết SRS:**

### `OD-01` - Emotion taxonomy

Danh sách emotion cố định chính thức.

### `OD-02` - Sleep

Sleep tracking có vào MVP hay Future Scope.
Nếu muốn triển khai sleep ↔ mood, MVP bắt buộc phải thu thập sleep data.

### `OD-03` - Insight Threshold

Bao nhiêu sample được xem là đủ cho từng loại insight.

### `OD-04` - Insight Lifecycle

**Điều kiện chuyển:**

- ACTIVE → FADING → EXPIRED

### `OD-05` - Free Plan Limit

Giới hạn chính xác của Free.

### `OD-06` - AI Provider

**Provider chính:**

- OpenAI;
- Gemini;
- provider khác.

Thiết kế backend không được phụ thuộc cứng vào một provider.

### `OD-07` - Image Attachment

Có triển khai upload ảnh trong MVP hay chuyển sang P1.

## 36. MVP Definition of Done

MVP được xem là hoàn thành khi người dùng có thể:

- Đăng ký và đăng nhập.

Viết nhiều journal.
Nhập mood.
Nhận kết quả phân tích emotion/sentiment/topic.
Chỉnh lại kết quả AI.
Nhận câu hỏi reflection.
Xem dữ liệu cảm xúc theo thời gian.
Nhận ít nhất một insight dựa trên lịch sử.
Nhận một Suggested Action.
Đánh giá Helpful/Not Helpful.
Gặp Safety Flow khi journal được phân loại HIGH/CRITICAL.
Dữ liệu của người dùng được cô lập và bảo vệ.
Nếu các yêu cầu trên hoạt động end-to-end, MyLog đã chứng minh được giá trị cốt lõi của đồ án mà không cần triển khai toàn bộ Future Scope.

