# MYLOG – SOFTWARE REQUIREMENTS SPECIFICATION

| Field | Value |
| --- | --- |
| **Version** | 1.0 |
| **Status** | Draft – MVP Specification |
| **Product** | MyLog |
| **System type** | Web Application |
| **Development team** | 2 members |
| **MVP development period** | 1 month |

## 1. Introduction

### 1.1. Purpose

Tài liệu này mô tả các yêu cầu phần mềm của hệ thống MyLog.

**SRS được sử dụng làm cơ sở cho:**

- thiết kế hệ thống;
- thiết kế database;
- thiết kế API;
- thiết kế frontend;
- triển khai backend;
- triển khai AI pipeline;
- kiểm thử;
- nghiệm thu MVP.

## 2. System Overview

MyLog là nền tảng nhật ký thông minh cho phép người dùng ghi lại suy nghĩ và cảm xúc hằng ngày.
Hệ thống sử dụng dữ liệu nhật ký theo thời gian kết hợp với AI và Statistical Engine để:

- nhận diện sentiment;
- nhận diện emotion;
- trích xuất topic;
- phát hiện pattern;
- phân tích xu hướng;
- tạo insight;
- tạo câu hỏi tự phản tư;
- đề xuất hành động nhỏ;
- ghi nhận phản hồi người dùng.

**Core flow:**

- User
- ↓
- Write Journal
- ↓
- Risk Detection
- ↓
- AI Analysis
- ↓
- Structured Data
- ↓
- Statistical Analysis
- ↓
- Insight
- ↓
- Reflection
- ↓
- Suggested Action
- ↓
- User Feedback

## 3. System Scope

### 3.1. In Scope – MVP

**MVP bao gồm:**

- Authentication;
- Journal CRUD;
- autosave draft;
- mood tracking;
- AI sentiment analysis;
- AI emotion analysis;
- AI topic extraction;
- user correction;
- writing prompts;
- reflection questions;
- Statistical Engine;
- historical insight;
- dashboard;
- suggested actions;
- user feedback;
- AI safety pipeline.

## 4. Out of Scope – MVP

**Không triển khai đầy đủ trong MVP:**

- medical diagnosis;
- therapy chatbot;
- medication recommendation;
- complete habit tracker;
- complete goal management;
- action effectiveness tracking dài hạn;
- RAG;
- semantic journal search;
- mobile native application;
- OAuth;
- payment gateway;
- automatic emergency contact;
- image understanding;
- advanced recommendation engine;
- biometric authentication.

## 5. Actors

### `ACT-01` - Guest

Người chưa đăng nhập.

**Có thể:**

- xem landing page;
- đăng ký;
- đăng nhập.

Không thể truy cập journal hoặc dữ liệu người dùng.

### `ACT-02` - Authenticated User

Người đã đăng nhập.

**Có thể:**

- tạo journal;
- chỉnh sửa journal;
- xóa journal;
- xem lịch sử;
- xem AI analysis;
- sửa kết quả AI;
- xem dashboard;
- xem insight;
- trả lời reflection;
- đánh giá suggestion.

### `ACT-03` - AI Service

External/internal AI provider thực hiện các tác vụ:

- sentiment classification;
- emotion classification;
- topic extraction;
- risk classification;
- reflection generation;
- insight explanation;
- suggested action generation.

### `ACT-04` - Statistical Engine

**Module backend chịu trách nhiệm:**

- aggregate dữ liệu;
- tính average;
- frequency;
- correlation;
- trend;
- period comparison;
- pattern detection.

Statistical Engine không phụ thuộc trực tiếp vào LLM.

## 6. User Account Requirements

### `FR-AUTH-01` - Register

Hệ thống phải cho phép người dùng đăng ký bằng:

- email;
- password;
- display name.

Validation

**Email:**

- đúng định dạng;
- không được trùng.

Password phải đạt minimum password policy của hệ thống.
Acceptance Criteria
Given email chưa tồn tại
When user nhập thông tin hợp lệ
Then account được tạo.

### `FR-AUTH-02` - Login

**Người dùng phải có thể đăng nhập bằng:**

- email;
- password.

**Nếu hợp lệ:**

- cấp access token;
- cấp refresh token;
- tạo authenticated session.

### `FR-AUTH-03` - Logout

Người dùng có thể logout.
Refresh token/session tương ứng phải bị vô hiệu hóa.

### `FR-AUTH-04` - Refresh Token

Backend phải hỗ trợ refresh access token mà không yêu cầu login lại khi refresh token còn hợp lệ.

### `FR-AUTH-05` - Authorization

Mỗi API truy cập dữ liệu cá nhân phải kiểm tra:

- resource.userId == authenticatedUser.id

User A không được truy cập dữ liệu của User B.

## 7. User Profile

### `FR-USER-01` - Profile

**Profile MVP gồm:**

- id
- email
- displayName
- plan
- timezone
- createdAt
- updatedAt

**plan:**

- FREE
- PLUS

MVP chưa yêu cầu payment integration.

## 8. Journal Requirements

### `FR-JOURNAL-01` - Create Journal

Người dùng có thể tạo journal mới.

**Một journal phải chứa:**

- Required
- content;
- moodScore.

Optional
- stressScore;
- energyScore;
- tags;
- image reference nếu attachment được triển khai.

### `FR-JOURNAL-02` - Content Validation

**Journal content:**

- không được rỗng;
- phải chứa ít nhất một ký tự meaningful;
- không được chỉ chứa whitespace.

MVP không đặt limit dựa trên 200 từ.
200 từ chỉ là expected usage.

### `FR-JOURNAL-03` - Mood Score

**Mood score:**

- 1 → 10

**Validation:**

- 1 <= moodScore <= 10

### `FR-JOURNAL-04` - Stress Score

**Nếu user nhập:**

- 1 <= stressScore <= 10

Không bắt buộc trong MVP.

### `FR-JOURNAL-05` - Energy Score

**Nếu user nhập:**

- 1 <= energyScore <= 10

Không bắt buộc.

### `FR-JOURNAL-06` - Multiple Journals Per Day

Một user được phép tạo nhiều journal trong cùng một ngày.

**Không đặt unique constraint:**

- (userId, date)

### `FR-JOURNAL-07` - Daily Mood

**Nếu một ngày có nhiều journal:**

- dailyMood =
- sum(journal moodScore)
- /
- numberOfJournals

**Ví dụ:**

- Journal 1 = 4
- Journal 2 = 8

Daily Mood = 6

### `FR-JOURNAL-08` - Journal History

**Người dùng có thể xem journal history theo:**

- newest first;
- oldest first.

**P1 có thể bổ sung:**

- filter;
- search;
- calendar view.

### `FR-JOURNAL-09` - Journal Detail

**User có thể mở journal để xem:**

- content;
- mood;
- created time;
- analysis;
- topics;
- emotions;
- reflection questions.

### `FR-JOURNAL-10` - Update Journal

User có thể chỉnh sửa journal của mình.

**Sau khi chỉnh sửa content:**

- Journal status
- ↓
- ANALYSIS_OUTDATED
- ↓
- Queue / Start AI Analysis
- ↓
- Analysis replaced
- ↓
- Relevant current insight recalculated

### `FR-JOURNAL-11` - Delete Journal

**Trước khi xóa:**

- Hệ thống phải hiển thị confirmation.

**Ví dụ:**

- Việc xóa nhật ký có thể làm thay đổi các thống kê và insight hiện tại.

**Khi xác nhận:**

- journal bị xóa;
- associated analysis bị xóa;
- associated reflection data được xử lý;
- current insight được recalculated.

### `FR-JOURNAL-12` - Autosave Draft

Journal Editor phải hỗ trợ autosave.

**Draft có thể được lưu:**

- local client storage; hoặc
- backend.

MVP ưu tiên local autosave nếu cần giảm backend complexity.
Autosave không được tự động kích hoạt AI analysis.

## 9. Journal State

**Journal có thể có trạng thái:**

- DRAFT
- SAVED
- ANALYZING
- ANALYZED
- ANALYSIS_FAILED
- ANALYSIS_OUTDATED

**Flow:**

- DRAFT
- ↓
- SAVED
- ↓
- ANALYZING
- ↓
- ANALYZED

**Nếu AI lỗi:**

- ANALYZING
- ↓
- ANALYSIS_FAILED

**Nếu journal được sửa:**

- ANALYZED
- ↓
- ANALYSIS_OUTDATED
- ↓
- ANALYZING

## 10. Writing Prompt Requirements

### `FR-PROMPT-01` - Request Writing Prompt

**Journal Editor có nút:**

- Gợi ý cho tôi

**Khi user nhấn:**

- Hệ thống cung cấp một writing prompt.

**Ví dụ:**

- Hôm nay điều gì khiến bạn suy nghĩ nhiều nhất?

### `FR-PROMPT-02` - Default Placeholder

**Editor mặc định có thể hiển thị:**

- Hôm nay điều gì khiến bạn vui nhất?
- Placeholder không được lưu thành journal content.

### `FR-PROMPT-03` - No Category Selection

**User không cần chọn:**

- family;
- work;
- school;
- relationship;
- trước khi viết.

Journal được viết tự do.

## 11. AI Analysis Pipeline

**Khi journal được Save:**

- Journal Saved
- ↓
- Risk Detection
- ↓
- Structured Analysis
- ↓
- Save Analysis
- ↓
- Generate Reflection

AI analysis chạy tự động.

### `FR-AI-01` - Automatic Analysis

User không cần bấm nút Analyze.
Sau khi journal được lưu, hệ thống tự động bắt đầu analysis.

### `FR-AI-02` - Async Processing

Việc lưu journal không được phụ thuộc hoàn toàn vào thời gian xử lý của AI.

**Flow mong muốn:**

- POST Journal
- ↓
- Save Database
- ↓
- Return Success
- ↓
- Start Async Analysis

**MVP có thể dùng:**

- Spring @Async

hoặc cơ chế background execution tương đương.

### `FR-AI-03` - Sentiment

**AI phải trả:**

- POSITIVE
- NEUTRAL
- NEGATIVE

Một journal chỉ có một sentiment chính.

### `FR-AI-04` - Emotion

Một journal có thể có nhiều emotion.

**Output:**

- {
- "emotions": [
- {
- "type": "ANXIETY",
- "score": 0.74
- },
- {
- "type": "HOPE",
- "score": 0.41
- }
- ]
- }

**Score:**

- 0.0 → 1.0

**Frontend có thể hiển thị:**

- 74%
- 41%

## 12. Emotion Taxonomy

MVP phải dùng taxonomy cố định.

**Draft taxonomy:**

- JOY
- SADNESS
- ANGER
- FEAR
- ANXIETY
- CALM
- HOPE
- GRATITUDE
- LONELINESS
- FRUSTRATION
- EXCITEMENT

Open Decision
Danh sách cuối cùng cần được khóa trước implementation AI classifier.
Không cho phép AI tự sinh emotion mới ngoài taxonomy.

### `FR-AI-05` - Topic Extraction

AI có thể tự tạo topic.

**Ví dụ:**

- deadline
- work
- family
- finance
- relationship
- study
- health
- sleep

Topic không bị giới hạn trong taxonomy cố định.

### `FR-AI-06` - AI Structured Output

Backend không được phụ thuộc vào free-form AI response để xử lý logic.
AI phải trả structured response.

**Ví dụ:**

- {
- "sentiment": "NEGATIVE",
- "emotions": [
- {
- "type": "ANXIETY",
- "score": 0.74
- }
- ],
- "topics": [
- "deadline",
- "work"
- ],
- "riskLevel": "LOW"
- }

**Nếu response không hợp lệ:**

- reject result;
- retry khi phù hợp;
- không ghi dữ liệu malformed vào database.

### `FR-AI-07` - AI Explanation

Người dùng có thể xem explanation cho emotion/topic.

**Ví dụ:**

- Anxiety được nhận diện vì bài viết nhiều lần đề cập đến lo lắng về deadline và việc không hoàn thành đúng hạn.

**Explanation không được:**

- chẩn đoán;
- khẳng định nguyên nhân tuyệt đối;
- sử dụng giọng điệu y khoa.

## 13. User Correction

### `FR-CORRECTION-01` - Edit Topic

**Người dùng được phép:**

- thêm topic;
- sửa topic;
- xóa topic.

### `FR-CORRECTION-02` - Edit Emotion

Người dùng có thể chỉnh sửa kết quả emotion nếu nhận diện không phù hợp.

### `FR-CORRECTION-03` - User Data Priority

**Khi user sửa AI result:**

- User Corrected Data
- >
- Original AI Data

Statistical Engine phải sử dụng giá trị user-corrected.

### `FR-CORRECTION-04` - Preserve AI Original

**Hệ thống nên lưu:**

- originalValue
- correctedValue
- correctedByUser
- correctedAt

**để hỗ trợ:**

- audit;
- đánh giá chất lượng AI;
- cải thiện hệ thống.

## 14. Reflection Requirements

### `FR-REFLECTION-01` - Generate Reflection

Sau journal analysis, AI có thể tạo reflection questions.

**Mỗi lần tối đa:**

- 3 questions

### `FR-REFLECTION-02` - Historical Context

**Reflection generator có thể sử dụng:**

- journal hiện tại;
- relevant journal cũ;
- relevant topic;
- previous user feedback.

Không được lấy dữ liệu của user khác.

### `FR-REFLECTION-03` - Request More Questions

**User có thể:**

- Cho tôi câu hỏi khác
- Hệ thống tạo bộ câu hỏi mới.

### `FR-REFLECTION-04` - Helpful Feedback

**Mỗi reflection question có thể được đánh giá:**

- HELPFUL
- NOT_HELPFUL

### `FR-REFLECTION-05` - Sensitive Context Control

AI không nên chủ động khơi lại những sự kiện nhạy cảm cũ nếu:

- không liên quan trực tiếp;
- không cần thiết;
- có khả năng gây distress đáng kể.

Safety layer có quyền loại bỏ reflection question.

## 15. Statistical Engine

Statistical Engine phải xử lý dữ liệu có cấu trúc.
LLM không tự tính các statistic quan trọng.

### `FR-STAT-01` - Minimum Historical Data

Không tạo historical insight khi user chưa có tối thiểu:

- 3 distinct journal days

**Lưu ý:**

- 3 ngày chỉ là global minimum.

Từng loại insight có thể yêu cầu nhiều sample hơn.

### `FR-STAT-02` - Mood Average

**Hệ thống phải tính:**

- daily mood average;
- weekly mood average;
- monthly mood average khi đủ dữ liệu.

### `FR-STAT-03` - Emotion Frequency

Hệ thống phải tính frequency của emotion.

**Ví dụ:**

**ANXIETY:**

- 6 / 10 journal entries

### `FR-STAT-04` - Topic Frequency

**Ví dụ:**

- work = 8
- deadline = 6
- family = 3

### `FR-STAT-05` - Topic-Mood Analysis

**Hệ thống phải có khả năng tính:**

- average mood for journals containing topic X

**Ví dụ:**

- Topic = work
- Sample = 7
- Average Mood = 4.7

### `FR-STAT-06` - Day-of-week Pattern

**Hệ thống có thể aggregate mood theo:**

- Monday
- Tuesday
- ...

Sunday

Không tạo pattern nếu sample quá ít.

### `FR-STAT-07` - Trend

Hệ thống phải hỗ trợ phát hiện một số trend cơ bản.

**Ví dụ:**

- weeklyMoodWeek1 = 7.1
- weeklyMoodWeek2 = 6.3
- weeklyMoodWeek3 = 5.5

**Có thể tạo candidate insight:**

- Mood trung bình giảm ba tuần liên tục.

### `FR-STAT-08` - Correlation

Nếu đủ dữ liệu, Statistical Engine có thể tính correlation.

**Candidate variables:**

- mood ↔ stress
- mood ↔ energy
- mood ↔ sleep

## 16. Sleep Conflict Resolution

Sleep không bắt buộc trong MVP hiện tại.

**Do đó:**

- IF sleep tracking disabled
- THEN sleep-mood insight disabled

**Nếu sleep được đưa vào MVP:**

- sleepQuality = optional

**và Statistical Engine mới được kích hoạt:**

- sleep ↔ mood

Không được sinh sleep insight khi hệ thống không có dữ liệu sleep thực tế.

## 17. Insight Engine

### `FR-INSIGHT-01` - Generate Insight

Insight chỉ được tạo từ structured evidence.

**Flow:**

- Statistical Result
- ↓
- Candidate Pattern
- ↓
- Confidence Evaluation
- ↓
- LLM Explanation
- ↓
- Insight

### `FR-INSIGHT-02` - Insight Evidence

Insight phải lưu evidence.

**Ví dụ:**

- {
- "sampleSize": 8,
- "matchingCount": 6,
- "frequency": 0.75,
- "topic": "deadline",
- "condition": "stress >= 8"
- }

### `FR-INSIGHT-03` - No Causation Claim

**Insight không được khẳng định:**

- X causes Y

**Chỉ được dùng ngôn ngữ:**

- liên quan;
- xuất hiện cùng;
- có xu hướng;
- thường gặp;
- correlation.

### `FR-INSIGHT-04` - Confidence Category

**MVP sử dụng:**

- WEAK
- MODERATE
- STRONG

**Không hiển thị:**

- 87% AI chắc chắn.

Confidence được tính từ evidence, không phải self-reported confidence của LLM.

### `FR-INSIGHT-05` - Insight Storage

Insight phải được lưu database.

### `FR-INSIGHT-06` - Insight Lifecycle

**Trạng thái:**

- ACTIVE
- FADING
- EXPIRED

ACTIVE
Dữ liệu gần đây vẫn hỗ trợ pattern.
FADING
Pattern cũ vẫn tồn tại nhưng support từ dữ liệu mới giảm đáng kể.
EXPIRED
Không còn đủ evidence trong analysis window.

### `FR-INSIGHT-07` - Lifecycle Threshold

Threshold chuyển trạng thái chưa được khóa.
Đây là Open Decision.

**MVP có thể dùng rule đơn giản dựa trên:**

- support frequency;
- sample size;
- recency.

## 18. Dashboard

### `FR-DASH-01` - Default Dashboard

**Sau khi mở Dashboard:**

**Default period:**

- Last 7 days

### `FR-DASH-02` - Mood Trend

Dashboard phải hiển thị line chart.

**X-axis:**

- Date

**Y-axis:**

- Mood 1–10

### `FR-DASH-03` - Multiple Metrics

**Nếu có dữ liệu:**

- mood;
- stress;
- energy;
- có thể được hiển thị chung chart.

Frontend phải cho phép người dùng phân biệt từng metric.

### `FR-DASH-04` - Emotion Distribution

Dashboard hiển thị emotion distribution.
Không bắt buộc dùng Pie Chart.
Ưu tiên visualization dễ đọc.

### `FR-DASH-05` - Mood Calendar

Dashboard có mood calendar để thể hiện mood theo ngày.

**Một ngày có nhiều journal:**

- sử dụng daily mood average.

### `FR-DASH-06` - Topic Frequency

Dashboard phải hiển thị topic phổ biến trong period.

### `FR-DASH-07` - No Custom Dashboard

MVP không hỗ trợ drag/drop hoặc custom dashboard.

## 19. Report Requirements

Weekly Report được xem là P1 nếu timeline không đủ.

**Nếu triển khai:**

### `FR-REPORT-01` - Weekly Report

**Report được tạo:**

- Sunday

### `FR-REPORT-02` - Minimum Journals

**Chỉ tạo khi có ít nhất:**

- 3 journal entries

trong kỳ.

### `FR-REPORT-03` - Report Content

**Report có thể chứa:**

- journal count;
- mood average;
- common emotions;
- common topics;
- notable insight;
- comparison;
- reflection;
- suggested action.

### `FR-REPORT-04` - Report Snapshot

Report là snapshot tại thời điểm tạo.
Không tự rewrite report cũ sau khi journal cũ được chỉnh sửa.

### `FR-REPORT-05` - Stale Report

**Nếu dữ liệu nguồn bị thay đổi:**

**Report có thể chuyển:**

- VALID
- →
- STALE

Không bắt buộc regenerate.

## 20. Suggested Action

### `FR-ACTION-01` - Action Generation

AI có thể đề xuất hành động dựa trên insight.

**Ví dụ:**

- Thử dành 10 phút chia deadline lớn nhất hiện tại thành ba nhiệm vụ nhỏ.

### `FR-ACTION-02` - Action Constraint

**Suggested Action phải:**

- nhỏ;
- cụ thể;
- low-risk;
- không phải medical treatment;
- không đưa ra diagnosis;
- không đề xuất thay đổi medication.

### `FR-ACTION-03` - Maximum Actions

**Mỗi insight:**

- 1–3 suggested actions

MVP ưu tiên một action nổi bật.

### `FR-ACTION-04` - User Response

**User có thể:**

- ACCEPT
- IGNORE

MVP không bắt buộc theo dõi completion dài hạn.

## 21. Feedback Requirements

### `FR-FEEDBACK-01`

**User có thể đánh giá:**

- HELPFUL
- NOT_HELPFUL

**cho:**

- reflection question;
- suggested action.

### `FR-FEEDBACK-02`

Feedback phải được lưu database.

### `FR-FEEDBACK-03`

MVP có thể sử dụng rule-based personalization đơn giản.

**Ví dụ:**

**Nếu user liên tục chọn:**

- NOT_HELPFUL

cho một loại suggestion thì giảm ưu tiên loại đó.
Không cần machine-learning recommendation engine.

## 22. Safety Requirements

### `FR-SAFETY-01` - Risk Detection

Mọi journal phải qua Risk Detection trước khi sinh normal recommendation.

**Risk level:**

- NORMAL
- LOW
- MODERATE
- HIGH
- CRITICAL

### `FR-SAFETY-02` - Safety Layer

Risk Detection không được phụ thuộc duy nhất vào free-form response.

**Có thể dùng:**

- AI Classification
- +
- Backend Rules

### `FR-SAFETY-03` - HIGH / CRITICAL

**Nếu:**

- riskLevel == HIGH
- OR
- riskLevel == CRITICAL

**hệ thống phải:**

- ngừng normal suggested action;
- không tạo response mang tính coaching thông thường;
- hiển thị safety popup;
- khuyến khích user tìm sự hỗ trợ từ người thật;
- hiển thị các lựa chọn hỗ trợ phù hợp với sản phẩm.

### `FR-SAFETY-04` - Safety Popup

**Popup phải:**

- rõ ràng;
- không gây phán xét;
- không tuyên bố diagnosis;
- không nói AI có thể xử lý tình huống khẩn cấp.

### `FR-SAFETY-05` - SafetyEvent

**Hệ thống ghi:**

- id
- userId
- journalEntryId
- riskLevel
- actionTaken
- createdAt

Không lưu toàn bộ journal text trực tiếp trong SafetyEvent nếu không cần thiết.

### `FR-SAFETY-06` - Emergency Contact

**Nếu triển khai P1:**

- EmergencyContact
- - id
- - userId
- - name
- - relationship
- - phone

**Không tự động:**

- gọi;
- SMS;
- email;
- Emergency Contact trong MVP.

## 23. AI Failure Handling

### `FR-FAIL-01`

**Nếu AI Provider lỗi:**

- Journal vẫn phải được lưu.

### `FR-FAIL-02`

**User vẫn có thể:**

- xem journal;
- chỉnh sửa journal;
- xóa journal;
- xem statistics không cần AI.

### `FR-FAIL-03`

**Analysis chuyển:**

- ANALYSIS_FAILED

### `FR-FAIL-04`

Hệ thống có thể hỗ trợ retry.
Retry phải có giới hạn.
Không tạo infinite retry loop.

## 24. Provider Abstraction

Backend không được hard-code business logic trực tiếp vào một AI provider.

**Interface có thể theo dạng:**

```java
public interface AiAnalysisProvider {

    JournalAnalysisResult analyzeJournal(...);

    List<ReflectionQuestion> generateReflection(...);

    String explainInsight(...);

    SuggestedAction generateAction(...);
}
```

**Implementation:**

- OpenAiProvider
- GeminiProvider
- OtherProvider

MVP chỉ cần implement một provider chính.

## 25. Data Model Requirements

```text
User
id
email
passwordHash
displayName
plan
timezone
createdAt
updatedAt

JournalEntry
id
userId
content
moodScore
stressScore
energyScore
status
createdAt
updatedAt

JournalAnalysis
id
journalEntryId
sentiment
riskLevel
explanation
analyzedAt
version

JournalEmotion
id
analysisId
emotionType
originalScore
correctedScore
correctedByUser

Topic
id
name

JournalTopic
journalEntryId
topicId
source

**Source:**

- AI
- USER

ReflectionQuestion
id
journalEntryId
question
createdAt

Insight
id
userId
type
title
description
confidence
status
periodStart
periodEnd
createdAt
updatedAt

InsightEvidence
id
insightId
sampleSize
matchingCount
metric
value
evidenceJson

SuggestedAction
id
insightId
description
status
createdAt

**Status:**

- PENDING
- ACCEPTED
- IGNORED

Feedback
id
userId
targetType
targetId
value
createdAt

**Value:**

- HELPFUL
- NOT_HELPFUL

WeeklyReport
id
userId
periodStart
periodEnd
status
summary
createdAt

**Status:**

- VALID
- STALE

SafetyEvent
id
userId
journalEntryId
riskLevel
actionTaken
createdAt
```

## 26. Database Relationship

```text
User
 │
 ├── JournalEntry
 │       │
 │       ├── JournalAnalysis
 │       │       └── JournalEmotion
 │       │
 │       ├── JournalTopic
 │       │
 │       └── ReflectionQuestion
 │
 ├── Insight
 │       ├── InsightEvidence
 │       └── SuggestedAction
 │
 ├── Feedback
 │
 ├── WeeklyReport
 │
 └── SafetyEvent
```

## 27. Privacy Requirements

### `NFR-PRIVACY-01`

Không ghi toàn bộ journal content vào application log.

### `NFR-PRIVACY-02`

**Không ghi:**

- password;
- JWT;
- refresh token;
- vào log.

### `NFR-PRIVACY-03`

Người dùng phải biết journal có thể được gửi tới external AI provider.

### `NFR-PRIVACY-04`

Chỉ gửi dữ liệu cần thiết cho AI task tương ứng.

### `NFR-PRIVACY-05`

Nếu sử dụng provider/API có chính sách không phù hợp với dữ liệu nhạy cảm:

- Production journal không được gửi tới provider đó.

### `NFR-PRIVACY-06`

**User phải có quyền:**

- delete journal;
- delete account;
- export data nếu export feature được triển khai.

## 28. Security Requirements

### `NFR-SEC-01`

Password phải được hash bằng password hashing algorithm phù hợp.

**Ví dụ:**

- BCrypt / Argon2

### `NFR-SEC-02`

Không lưu plaintext password.

### `NFR-SEC-03`

Production phải sử dụng HTTPS.

### `NFR-SEC-04`

Backend phải validate authorization cho từng resource.

### `NFR-SEC-05`

Input phải được validate server-side.
Không chỉ dựa vào frontend validation.

### `NFR-SEC-06`

**Secrets phải nằm trong:**

- environment variables;
- secrets manager;
- không commit vào source code.

## 29. Performance Requirements

### `NFR-PERF-01`

**Các operation không liên quan AI:**

- login;
- journal CRUD;
- history;
- dashboard data;
- phải phản hồi ở mức phù hợp với web application thông thường.

**Target MVP:**

- <= 2 seconds

trong điều kiện development/demo hợp lý.

### `NFR-PERF-02`

AI request không cần đáp ứng target 2 giây.

**Frontend phải hiển thị trạng thái:**

- Analyzing...

## 30. Reliability Requirements

### `NFR-REL-01`

AI failure không được làm mất journal.

### `NFR-REL-02`

**Nếu Statistical Engine lỗi:**

- Journal và AI analysis vẫn được giữ.

### `NFR-REL-03`

**Nếu LLM explanation lỗi:**

- Raw statistic/evidence không được mất.

## 31. Maintainability Requirements

Backend Spring Boot phải chia module rõ ràng.

**Ví dụ:**

- auth
- user
- journal
- analysis
- insight
- statistics
- reflection
- feedback
- safety
- report

### `NFR-MAIN-02`

Controller không chứa business logic phức tạp.

**Flow:**

- Controller
- ↓
- Service
- ↓
- Repository

### `NFR-MAIN-03`

AI provider phải nằm sau abstraction.

### `NFR-MAIN-04`

**Database migration phải được quản lý bằng:**

- Flyway

**hoặc:**

- Liquibase

## 32. Frontend Requirements

**Frontend dự kiến:**

- React / Next.js
- TypeScript

### `UI-01` - Journal First

**Sau khi login, primary CTA phải là:**

- Viết nhật ký

### `UI-02` - Responsive

**Các màn hình chính phải sử dụng được trên:**

- desktop;
- mobile browser.

### `UI-03` - AI State

**Frontend phải xử lý các state:**

- Analyzing
- Completed
- Failed

### `UI-04` - Charts

**Chart phải có:**

- title;
- time range;
- scale;
- legend khi cần;
- tooltip hoặc cách đọc rõ ràng.

## 33. Main Screens

**MVP dự kiến gồm:**

- Login
- Register
- Journal Editor
- Journal History
- Journal Detail
- Dashboard
- Insight Detail
- Settings / Profile

**P1:**

- Reports
- Emergency Contact
- Export
- Plan

## 34. API Groups

**Backend REST API dự kiến:**

- /api/auth/*
- /api/users/*
- /api/journals/*
- /api/analysis/*
- /api/reflections/*
- /api/insights/*
- /api/statistics/*
- /api/feedback/*
- /api/safety/*
- /api/reports/*

API specification chi tiết thuộc tài liệu API Design.

## 35. Core Edge Cases

### `EC-01`

User tạo journal rồi AI API timeout.

**Expected:**

- Journal saved
- Analysis failed
- Retry possible

### `EC-02`

User sửa journal trong lúc analysis đang chạy.

**Expected:**

- Kết quả analysis cũ không được overwrite journal version mới.

**Cần sử dụng:**

- journalVersion

hoặc equivalent check.

### `EC-03`

User xóa journal khi AI analysis đang chạy.

**Expected:**

- Worker không được recreate deleted data.

### `EC-04`

User có 1 journal nhưng mở insight.

**Expected:**

**Hiển thị:**

- Chưa đủ dữ liệu để tạo insight.

### `EC-05`

Statistical Engine phát hiện correlation cao nhưng sample nhỏ.

**Expected:**

- Không tạo Strong Insight.

### `EC-06`

LLM diễn giải khác với statistic.

**Expected:**

- Structured evidence là source of truth.

Invalid explanation phải bị reject hoặc regenerate.

### `EC-07`

User sửa topic.

**Expected:**

- Historical analysis lần sau sử dụng topic mới.

### `EC-08`

Journal cũ được sửa sau khi report được tạo.

**Expected:**

**Report:**

- STALE

hoặc giữ snapshot.
Không silently rewrite.

### `EC-09`

User không nhập stress.

**Expected:**

- Không coi stress là 0.

**Phải lưu:**

- NULL

### `EC-10`

User không nhập sleep.
Không sinh sleep-based insight.

## 36. Business Rules

### `BR-01`

Mỗi journal chỉ thuộc một user.

### `BR-02`

Một user có thể có nhiều journal mỗi ngày.

### `BR-03`

Mood bắt buộc.

### `BR-04`

Stress và energy optional.

### `BR-05`

User correction được ưu tiên hơn AI output.

### `BR-06`

Historical Insight yêu cầu tối thiểu 3 ngày dữ liệu.

### `BR-07`

Insight không được khẳng định causation.

### `BR-08`

HIGH/CRITICAL risk không được nhận normal action recommendation.

### `BR-09`

AI error không được xóa hoặc rollback journal.

### `BR-10`

Report cũ là snapshot.

### `BR-11`

Không dùng missing value như giá trị 0.

### `BR-12`

Statistical result là source of truth cho quantitative insight.

## 37. MVP Priorities

P0 – Required
Authentication
Journal CRUD
Autosave
Mood
AI sentiment
AI emotion
AI topic
Risk Detection
User Correction
Writing Prompt
Reflection
Statistics
Insight
Dashboard
Suggested Action
Helpful / Not Helpful

P1 – If Time Allows
Weekly Report
PDF Export
Image Attachment
Emergency Contact
Monthly Comparison
Free / Plus Limits

P2 – Future
RAG
Advanced Goal Tracking
Recommendation Learning
Native Mobile App
Payment
Semantic Search
Action Effectiveness Tracking

## 38. MVP Acceptance Flow

**MVP phải demo được end-to-end flow sau:**

### Step 1

User đăng ký.

### Step 2

User đăng nhập.

### Step 3

**User viết:**

- “Hôm nay tôi rất áp lực vì deadline đồ án. Tôi ngủ rất muộn và cảm thấy mình không làm kịp.”

**Mood:**

- 4 / 10

### Step 4

Journal được lưu.

### Step 5

**AI trả structured analysis:**

**Sentiment:**

- NEGATIVE

**Emotion:**

- ANXIETY
- FRUSTRATION

**Topics:**

- deadline
- project

**Risk:**

- NORMAL

### Step 6

**User chỉnh:**

- project
- →
- study

### Step 7

**AI tạo reflection:**

- Phần nào của deadline hiện tại khiến bạn cảm thấy áp lực nhất?

### Step 8

Sau nhiều journal, Statistical Engine phát hiện:

- 6 / 8 low-mood journals
- contain topic "deadline"

### Step 9

**Insight:**

- Trong dữ liệu gần đây, deadline thường xuất hiện cùng những ngày có mood thấp.

**Evidence:**

- 6 / 8
- Moderate / Strong Pattern

### Step 10

**Suggested Action:**

- Thử chọn deadline gần nhất và chia thành ba nhiệm vụ nhỏ có thể hoàn thành lần lượt.

### Step 11

**User:**

- Helpful 👍

**Flow trên chứng minh:**

- MyLog không chỉ gọi LLM để trả lời một đoạn journal mà sử dụng dữ liệu lịch sử có cấu trúc để tạo insight cá nhân hóa.

## 39. Definition of Done

**MyLog MVP hoàn thành khi:**

- authentication hoạt động;
- authorization đúng;
- user viết được journal;
- user sửa/xóa được journal;
- mood được lưu;
- AI analysis chạy;
- AI failure không làm mất journal;
- emotion/topic được lưu structured;
- user sửa được AI result;
- reflection hoạt động;
- historical data được aggregate;
- ít nhất một loại pattern được phát hiện từ dữ liệu;
- insight có evidence;
- dashboard hiển thị đúng dữ liệu;
- suggested action hoạt động;
- Helpful/Not Helpful được lưu;
- Safety Flow hoạt động;
- dữ liệu của user được cách ly;
- end-to-end demo chạy ổn định.

## 40. Open Decisions Before Implementation

### `OD-01` - Emotion Taxonomy

Khóa danh sách emotion cuối cùng.

### `OD-02` - Sleep Tracking

**Quyết định:**

- MVP
- OR
- P1

Nếu không có sleep input thì disable toàn bộ sleep-mood insight.

### `OD-03` - Statistical Thresholds

**Cần định nghĩa:**

- minimum sample per pattern;
- Weak;
- Moderate;
- Strong.

### `OD-04` - Insight Lifecycle Threshold

**Cần định nghĩa:**

- ACTIVE → FADING
- FADING → EXPIRED

### `OD-05` - AI Provider

Chọn một provider chính cho MVP.
Architecture vẫn giữ provider abstraction.

### `OD-06` - Free / Plus

**Chưa khóa:**

- AI request limit;
- insight limit;
- report limit.

Không ảnh hưởng tới core MVP.

## 41. Recommended MVP Architecture

               React / Next.js
                       │
                       │ REST API
                       ▼
                Spring Boot
                       │
       ┌───────────────┼────────────────┐
       │               │                │
       ▼               ▼                ▼
   Journal         Statistics          AI
   Service          Engine           Service
       │               │                │
       └───────────────┼────────────────┘
                       │
                       ▼
                  PostgreSQL
                       │
                       ▼
                Background Task
                       │
                       ▼
                AI Provider API

**Recommended backend:**

- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT
- Flyway

**Frontend:**

- Next.js / React
- TypeScript
- Recharts

**MVP không cần:**

- Kafka
- RabbitMQ
- Redis
- Microservices
- Kubernetes

## 42. Final MVP Boundary

**Nếu deadline bị áp lực, ưu tiên giữ:**

- Journal
- +
- AI Analysis
- +
- Reflection
- +
- Historical Statistics
- +
- Insight

**Nếu phải cắt tính năng:**

**Cắt theo thứ tự:**

- Monthly Report
- ↓
- PDF Export
- ↓
- Emergency Contact
- ↓
- Free / Plus Restrictions
- ↓
- Image Upload
- ↓
- Weekly Report

**Không nên cắt:**

- Historical Insight

vì đây là phần tạo ra khác biệt kỹ thuật quan trọng nhất giữa MyLog và một chatbot AI thông thường.

