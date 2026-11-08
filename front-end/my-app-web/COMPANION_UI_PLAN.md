# Kế hoạch giao diện và luồng: Trợ lý trang viết

> **Trạng thái:** **Bước 1, 2, 3, 4, 5 (mục 5) đã triển khai trong code, kèm mục 4.1 (gỡ icon bóng đèn).**
> Kế hoạch tổng thể gồm cả backend nằm ở [`backend/docs/COMPANION_CHATBOT_PLAN.md`](../../backend/docs/COMPANION_CHATBOT_PLAN.md) (backend đã triển khai Giai đoạn 1, 4a và 5). File này chỉ nói về phần web (`front-end/my-app-web`): giao diện, luồng tương tác, file cần tạo/sửa và cách kiểm tra.
> Bản mô phỏng UX (riêng tư, cần được chia sẻ mới xem được): https://claude.ai/artifact/QsFx59Xx6ZZVNpcyw9wSUy
>
> **Đã làm (Bước 1):** nút **Công cụ dịu** trên thanh công cụ editor, mở menu chọn Thở chậm 2 phút / Nghe âm thanh dịu / Viết xả — chạy thẳng trên máy, không gọi API, không cần consent. Logic ở `features/journal/components/calming-tools/` và hai hook `useCalmingTools`/`useBreathingTimer`.
>
> **Đã làm (Bước 2):** `CompanionPopover.tsx` đã **xóa hẳn**, thay bằng `features/journal/components/companion/` (`CompanionPanel`, `CompanionMessageList`, `CompanionComposer`, `CompanionConsentCard`, `CompanionClosingCard`) + hook state machine `useCompanionConversation` + `companion.types.ts` (khớp contract backend mục 5) + `companion-draft.ts`/`companion-fallback.ts`. `useCalmingTools` được nối hai chiều với chat: hành động trong tin nhắn (`BREATHING_2MIN`/`AMBIENT`/`VENT_PROMPT`/`TITLE`) gọi thẳng cùng hook của Bước 1 (`startBreathing('CHAT')`/`startAmbient('CHAT')`), và khi Thở chậm tự kết thúc (không phải bấm Dừng) **và** được bắt đầu từ trong chat, một lượt `TOOL_COMPLETED` tự gửi đi. Adapter giả `companion.api/companion.mock.ts` mô phỏng đủ 5 status (kèm 3 từ khóa QA: "test an toàn"/"test chặn"/"test lỗi"), chỉ bật khi `NEXT_PUBLIC_COMPANION_MOCK=1` và `NODE_ENV !== 'production'` (cả hai inline ở build time nên nhánh này bị loại khỏi bundle production).
>
> **Rút gọn có chủ đích so với kế hoạch gốc:** bố cục responsive còn 2 biến thể thay vì 3 (dưới 768px: bottom sheet; từ 768px trở lên: panel nổi bên phải) — khác biệt "sticky cột thứ 3" vs "fixed nổi" ở ≥1360px chỉ là hình thức.
>
> **Đã làm (Bước 3 + gỡ bóng đèn):** `secured-proxy.ts` đã thêm `journal-writing-suggestions` vào allowlist. `postCompanionTurn` nhánh thật gọi `apiJson` (nhánh mock chỉ còn dùng khi chủ động bật cờ dev). `grantCompanionConsent` nhánh thật gọi `getConsents()` lấy `documentVersion` của `AI_PROCESSING` rồi `setConsent(..., true)`. Đã gỡ hẳn nút `Lightbulb`, popover "Gợi ý chủ đề theo tâm trạng" và mảng `TOPIC_SUGGESTIONS_BY_EMOTION` tĩnh; tựa đề gợi ý chỉ còn đến qua trường `suggestedTitle` do backend sở hữu.
>
> **Đã làm (Bước 4 — Thẻ gợi ý thói quen + Modal tạo thói quen từ mẫu):**
> - Tạo [`CompanionHabitCard.tsx`](features/journal/components/companion/CompanionHabitCard.tsx) hiển thị `habitSuggestion` (`RECORD_EXISTING`, `NUDGE_EXISTING`, `CREATE_FROM_TEMPLATE`) ở các lượt sau câu mở đầu, tự ẩn khi cảm xúc là `very_bad`.
> - Nút "Ghi nhận hôm nay" gọi `completeHabit(habitId, suggestion.localDate, { completed: true, source: 'USER' })` với `localDate` do backend tính sẵn theo `habit.timezone`.
> - Nút "Không, cảm ơn" gọi endpoint riêng `POST /api/v1/companion/habit-suggestions/dismiss` (đã thêm vào allowlist của `secured-proxy.ts`), không đi qua endpoint trò chuyện nên không tốn quota `companion-turn`.
> - Tạo [`CreateHabitFromTemplateModal.tsx`](features/selfcare/components/CreateHabitFromTemplateModal.tsx) trong `@/features/selfcare` (export qua `features/selfcare/index.ts`), điền sẵn `presetTitle`, `presetTargetValue`, `presetUnit`, `presetToolCode` và cho phép chọn mục tiêu hiện có hoặc tạo mục tiêu mới trước khi gọi `createHabit(...)`.
>
> **Đã làm (Bước 5 — Hoàn thiện & Accessibility):**
> - Thêm đầy đủ `aria-label` trên các nút điều khiển trong `CompanionPanel`, `CompanionMessageList`, `CompanionConsentCard`, `CompanionHabitCard`, `CompanionClosingCard`, `CreateHabitFromTemplateModal`, vùng `role="log" aria-live="polite"` cho danh sách tin nhắn và hỗ trợ `prefers-reduced-motion` cho vòng thở.
> - Kiểm tra tĩnh: `npx tsc --noEmit` và `npm run lint` sạch (`0 errors`). Kiểm tra bằng trình duyệt thật với tài khoản thật cho Bước 3–5 cần chạy lại sau khi qua cửa sổ giới hạn đăng nhập.

---

## 1. Hiện trạng trong code

| Thành phần | Hiện tại | Vấn đề |
|---|---|---|
| [`CompanionPopover.tsx`](features/journal/components/CompanionPopover.tsx) (~700 dòng) | Popover nổi; kịch bản `getOpeningGreeting`/`buildContextualReply` viết cứng; widget thở; tiếng mưa Web Audio | Trộn UI, kịch bản, state machine và safety trong một file. Không gọi API. |
| Safety trong chat | `CRISIS_PATTERNS`, 6 cụm từ khóa ở client | Trái nguyên tắc "backend quyết định safety" |
| Consent | `hasConsent = true` | Thẻ xin phép không bao giờ hiện |
| [`JournalEditor.tsx`](features/journal/components/JournalEditor.tsx) | Nút `HeartHandshake` trên thanh công cụ phải mở popover qua `createPortal` + `questionsPopoverPosition` | Popover che trang viết; đóng là mất hội thoại |
| Thanh công cụ phải | Màn hình ≥ 1360px: cột `sticky`. Nhỏ hơn: nút nổi `Wrench` mở ngăn kéo `isToolsOpen` | Chưa có nút "Công cụ dịu" |
| [`JournalTipTapEditor.tsx`](features/journal/components/JournalTipTapEditor.tsx) | Ref có `insertContent`, `focus`, `getText` | Dùng được ngay để chèn câu hỏi và lấy bản nháp |
| [`SafetyModal.tsx`](features/journal/components/SafetyModal.tsx) | Đã tải nguồn hỗ trợ đã duyệt | Dùng lại được |
| Consent API | `getConsents`, `setConsent(type, documentVersion, granted)` trong `@/features/user` | Dùng lại được |
| Thói quen | `@/features/selfcare` có `listGoals`, `createHabit`, `completeHabit`; checklist hiển thị ở `InsightView` | **Chưa có màn hình tạo thói quen** |
| BFF | `secured-proxy.ts` dùng allowlist | Chưa có `journal-writing-suggestions` |

---

## 2. Trải nghiệm mục tiêu

### 2.1 Hai lối vào trên thanh công cụ
| Nút | Icon (lucide) | Khi nào dùng | Hành vi |
|---|---|---|---|
| **Trợ lý** | `HeartHandshake` | Chưa biết mình cần gì | Mở/thu nhỏ khung chat. Có chấm "chưa đọc" khi trợ lý trả lời lúc khung chat đang thu nhỏ. |
| **Công cụ dịu** (mới) | `Wind` | Đã biết mình muốn gì | Mở menu nhỏ: Thở chậm 2 phút · Nghe âm thanh dịu · Viết xả. Chạy ngay, **không gọi API, không cần consent**. |

### 2.2 Luồng chính
1. Người dùng viết và chọn cảm xúc (không bắt buộc).
2. Bấm **Trợ lý**: khung chat mở. Lần đầu dùng, nếu chưa có consent `AI_PROCESSING`, hiện thẻ xin phép.
3. Trợ lý mở lời gồm **1 câu**, **câu hỏi gợi ý** (bấm để chèn vào bài) và **hành động tức thời** (chỉ hiện khi cảm xúc đang lên).
4. Người dùng chọn: chèn câu hỏi · chạy hành động · nhắn tiếp · "Mình chỉ muốn viết tiếp" (thu nhỏ khung chat).
5. Chạy xong hành động: trợ lý hỏi thăm; nếu phù hợp thì kèm **thẻ gợi ý thói quen**.
6. Khép lại: chấm 1–5 (chỉ giữ trên máy, không gửi đi) · "Đưa cuộc trò chuyện vào bài" · "Chỉ vậy thôi".

### 2.3 Bố cục
| Màn hình | Khung chat | Công cụ dịu |
|---|---|---|
| ≥ 1360px | Panel neo phải, rộng ~360px, cao theo viewport trừ header, `sticky`. Lưới chính chuyển sang 3 cột: trang viết, cột công cụ, panel chat. | Menu bật ra cạnh nút |
| 768–1359px | Panel phủ bên phải (`fixed`, rộng ~380px) đè lên phần lề, **không** có lớp nền tối chặn trang viết | Menu trong ngăn kéo công cụ |
| < 768px | **Bottom sheet** cao ~70dvh, có tay kéo; thu nhỏ thành thanh mỏng ở đáy | Bottom sheet riêng |

Ở mọi kích thước, người dùng vẫn gõ được vào trang viết khi khung chat đang mở. Khung chat không được giữ focus bắt buộc (focus trap).

### 2.4 Các trạng thái của khung chat
| Trạng thái | Giao diện |
|---|---|
| Đang chờ trả lời | Bong bóng "đang gõ"; câu mở dựng sẵn và nút hành động hiện ngay, câu hỏi hiện sau |
| `CONSENT_REQUIRED` | Thẻ: "Cho phép trợ lý đọc bài bạn đang viết?" · Đồng ý / Để sau |
| Để sau / `UNAVAILABLE` / mất mạng / 429 / 503 | "Trợ lý đang tạm nghỉ"; chỉ còn nút công cụ; **khóa ô nhắn** |
| `BLOCKED` | Câu trung tính backend trả; khóa ô nhắn |
| `SAFETY_FLOW` | Đóng khung chat, **xóa hội thoại**, mở `SafetyModal` |
| Thu nhỏ | Hội thoại giữ nguyên; nút Trợ lý có chấm chưa đọc |

### 2.5 Quy tắc trải nghiệm
- Không tự mở khung chat, không tự phát âm thanh, không tự chèn gì vào bài.
- Bấm câu hỏi thì chèn vào bài dạng `blockquote`, con trỏ đặt ở đoạn trống ngay bên dưới, kèm toast "Đã chèn câu hỏi".
- Gợi ý thói quen không hiện ở câu mở và không hiện khi cảm xúc là "Rất tệ". Mỗi lượt có tối đa 1 thẻ.
- Không ghi hội thoại vào `localStorage`. Rời trang thì hội thoại mất.
- Header khung chat: "Đọc bài bạn đang viết · Không lưu đoạn chat".

---

## 3. Kiến trúc code

Theo `AGENTS.md` của web (mục 32: tách UI, business state, AI request và safety; mục 11: không deep-import feature khác).

```text
features/journal/
├── types/
│   └── companion.types.ts            # Request/response, Turn, ImmediateAction, HabitSuggestion, CompanionStatus
├── api/
│   └── companion.ts                  # postCompanionTurn(req, signal) qua apiJson('journal-writing-suggestions')
├── hooks/
│   ├── useCompanionConversation.ts   # state machine, gọi API, AbortController, unread, consent pending
│   └── useCalmingTools.ts            # chạy/dừng tool: timer thở, Web Audio, trạng thái đang chạy
├── utils/
│   └── companion-draft.ts            # lấy plain text từ editor, cắt 4.000 ký tự cuối, đếm từ
├── constants/
│   └── companion-fallback.ts         # câu mở dựng sẵn theo cảm xúc, dùng cho nhịp đầu và khi trợ lý tạm nghỉ
└── components/
    ├── companion/
    │   ├── CompanionPanel.tsx        # khung (panel / bottom sheet), header, thu nhỏ
    │   ├── CompanionMessageList.tsx  # bong bóng, câu hỏi, nút hành động
    │   ├── CompanionComposer.tsx     # ô nhắn + chip trả lời nhanh
    │   ├── CompanionConsentCard.tsx
    │   ├── CompanionHabitCard.tsx
    │   └── CompanionClosingCard.tsx
    └── calming-tools/
        ├── CalmingToolsMenu.tsx      # menu từ nút "Công cụ dịu"
        ├── BreathingWidget.tsx       # 2 phút, đếm ngược, dừng; dạng mini và dạng đầy đủ
        ├── AmbientSoundWidget.tsx    # Web Audio offline (chuyển từ CompanionPopover)
        └── VentPrompt.ts             # nội dung dòng mở đầu viết xả
```

- **Xóa** `CompanionPopover.tsx` sau khi đã chuyển hết phần trình bày dùng lại được.
- `calming-tools` nằm trong `features/journal` vì hiện chỉ trang viết dùng. Chỉ chuyển ra feature riêng khi một trang khác (Insight, Dashboard) thật sự cần.
- Thói quen gọi qua public API `@/features/selfcare` (`completeHabit`, `listGoals`), không import file nội bộ.
- Consent gọi qua `@/features/user` (`getConsents`, `setConsent`).

### 3.1 State machine (`useCompanionConversation`)

```text
closed ──open──► opening ──response──► listening ◄──────────────┐
                    │                     │  │                  │
                    │                     │  └─send/chip──► waiting ──response──┘
                    │                     └─run tool──► tool_running ──done──► after_tool ──► closing
                    ├─CONSENT_REQUIRED──► consent ──grant──► opening
                    │                        └─later──► tools_only
                    ├─UNAVAILABLE/lỗi mạng──► tools_only
                    └─SAFETY_FLOW──► closed (xóa hội thoại, mở SafetyModal)
minimized: giữ nguyên state bên dưới, chỉ ẩn khung
```

- Mỗi request có `AbortController`. Gửi tin mới hoặc đóng khung chat thì hủy request cũ. Response về muộn (sai `requestId`) bị bỏ qua.
- `history` gửi đi tối đa 8 lượt gần nhất.
- `draft` lấy lúc gửi bằng `editorRef.current.getText()` và `title`; không theo dõi từng lần gõ.
- `habitShownToday`: cờ trong bộ nhớ để tránh hiện thẻ thói quen hai lần trong một phiên. Giới hạn 1 lần/ngày và ẩn 7 ngày do backend quyết định.

### 3.2 Hiển thị hai nhịp
1. Ngay khi mở khung chat: hiện câu mở dựng sẵn từ `companion-fallback.ts` theo cảm xúc, kèm nút hành động tức thời tương ứng. Phần này chạy trên máy, không cần mạng.
2. Khi backend trả về: thay câu mở bằng câu của backend, thêm câu hỏi gợi ý.

Nếu backend trả `SAFETY_FLOW`, toàn bộ nhịp 1 bị xóa.

---

## 4. Thay đổi trong `JournalEditor.tsx`
- Bỏ `questionsPopoverPosition`, `toggleQuestionsPopover` và `createPortal(<CompanionPopover/>)`.
- Thêm state `isCompanionOpen` và nút **Công cụ dịu** (`Wind`) ngay dưới nút Trợ lý trong cột công cụ, đi kèm ngăn kéo `isToolsOpen` trên màn hình nhỏ.
- Lưới `main` ở ≥ 1360px thêm cột cho panel chat khi khung chat mở. Kiểm tra lại `max-w-[1440px]` và cột `minmax(0,920px)` của trang viết để trang viết không bị bóp quá hẹp.
- Truyền cho hook: `editorRef`, `title`, `selectedEmotion`, `stressScore`, `energyScore`, `sleepHours`, `wordCount`, và các callback `insertQuestion`, `applyTitle`, `insertConversation`, `openSafetyModal`.
- Gom `escapeHtml` và các hàm chèn nội dung vào một util của journal thay vì để trong component.
- Nút lightbulb gợi ý tựa đề: **gỡ bỏ** — xem mục 4.1.

---

## 4.1 Dọn dẹp: gỡ icon bóng đèn gợi ý chủ đề — ✅ Đã triển khai

Icon `Lightbulb` cạnh ô Tag mở popover 3 gợi ý tựa đề theo cảm xúc (`TOPIC_SUGGESTIONS_BY_EMOTION`) — tính năng độc lập, không thuộc trợ lý, đã bỏ khỏi dự án **cùng với chính mảng dữ liệu đó** (quyết định cuối: không giữ lại gì ở client).

**Đã gỡ:** nút `Lightbulb` và import từ `lucide-react`; state/ref `topicSuggestionPosition`/`topicSuggestionButtonRef`; hàm `toggleTopicSuggestions`; portal popover "Gợi ý chủ đề theo tâm trạng"; hằng số `TOPIC_SUGGESTIONS_BY_EMOTION` (không còn ở đâu trong client).

**Hệ quả cần xử lý — đã làm:** hành động `TITLE` trong chat trợ lý trước đó dùng chính mảng này. Tựa đề gợi ý giờ **chỉ đến từ `suggestedTitle`** trong phản hồi (hiển thị kèm nút "Điền làm tựa đề" trên từng tin nhắn, cơ chế có sẵn từ Bước 2) — không còn dữ liệu tĩnh ở client. `case 'TITLE'` trong `handleCompanionAction` giờ là no-op (giữ cho đủ nhánh switch, phòng khi backend phát lại mã này). Backend (`CompanionMoodTemplates`) đã đổi theo: `happy`/`neutral` không còn trả hành động `TITLE`, thay vào đó set `suggestedTitle` trực tiếp trên `Variant` — xem [`backend/docs/COMPANION_CHATBOT_PLAN.md`](../../backend/docs/COMPANION_CHATBOT_PLAN.md) mục 4.

**Xong khi**: không còn icon bóng đèn hay `TOPIC_SUGGESTIONS_BY_EMOTION` nào trên trang viết; `npx tsc --noEmit`/`npm run lint` sạch; tựa đề gợi ý trong chat vẫn hoạt động qua `suggestedTitle`. ✅ cả ba.

---

## 5. Lộ trình

### Bước 1: Tách công cụ dịu (không phụ thuộc backend) — ✅ Đã triển khai
- Tạo `calming-tools/*` và `useCalmingTools`. Chuyển Web Audio và widget thở từ `CompanionPopover`, nâng widget thở lên 2 phút có đếm ngược.
- Thêm nút **Công cụ dịu** và `CalmingToolsMenu`. Tool chạy dưới dạng thanh trạng thái phía trên trang viết, có nút Dừng.
- Viết xả: chèn đoạn mở đầu "Điều làm mình bực nhất hôm nay là…" và đặt con trỏ ở cuối đoạn.
- Âm thanh dừng khi rời trang hoặc đóng tool; tôn trọng `prefers-reduced-motion` cho vòng thở.
- **Xong khi**: mở được cả 3 tool từ nút mới trên desktop và điện thoại; dừng giữa chừng không lỗi; rời trang thì âm thanh tắt.

### Bước 2: Khung chat mới (giao diện, dùng adapter giả) — ✅ Đã triển khai
- Tạo `companion.types.ts` theo contract trong kế hoạch backend.
- Tạo `CompanionPanel` và các thẻ con, `useCompanionConversation`.
- Tạm dùng **adapter giả** cho `postCompanionTurn`, chỉ bật trong `next dev` qua biến môi trường, trả về các status theo kịch bản. Adapter này nằm trong `features/journal/api/companion.mock.ts`, có ghi chú rõ là bản demo, và **không được import trong build production**.
- Bố cục 3 kích thước, thu nhỏ/mở lại, chấm chưa đọc, chèn câu hỏi, điền tựa đề, đưa hội thoại vào bài, thẻ thói quen (nút bấm chỉ hiện toast "sẽ có ở bước 4"), thẻ khép lại.
- Xóa `CompanionPopover.tsx`, `CRISIS_PATTERNS`, `buildContextualReply`, `getOpeningGreeting`.
- **Xong khi**: đi được toàn bộ luồng ở mục 2.2 với adapter giả ở cả 3 kích thước màn hình.

### Bước 3: Nối API thật (cần backend giai đoạn 1) — ✅ Đã triển khai
- Thêm `{ methods: ['POST'], pattern: /^journal-writing-suggestions$/ }` vào `secured-proxy.ts`.
- `postCompanionTurn` gọi `apiJson`. Ánh xạ 401 sang luồng phiên đăng nhập hiện có (im lặng, để `AuthGate` tự chuyển trang); 429/503/lỗi mạng khác sang `tools_only` qua `handleUnavailable()`.
- Consent: Đồng ý thì gọi `setConsent('AI_PROCESSING', documentVersion, true)` (lấy `documentVersion` từ `getConsents`), rồi gửi lại lượt đang chờ.
- `SAFETY_FLOW`: đóng khung chat, xóa hội thoại, mở `SafetyModal` — đã có sẵn từ Bước 2, không cần sửa.
- Adapter giả chỉ còn chạy khi chủ động bật `NEXT_PUBLIC_COMPANION_MOCK=1` trong dev; mặc định (production lẫn dev không bật cờ) đi thẳng nhánh thật.
- **Xong khi**: `npx tsc --noEmit`/`npm run lint` sạch (✅); chưa xác nhận bằng trình duyệt thật + tài khoản thật (đang chờ hết giới hạn đăng nhập từ lúc gỡ lỗi Bước 1/2 — xem ghi chú đầu file).
- **Xong khi**: chạy cục bộ với `MYLOG_WRITING_SUGGESTIONS_ENABLED=true`, cả 5 status hiển thị đúng; không còn kịch bản hay safety tự viết ở client.

### Bước 4: Thói quen (cần backend giai đoạn 5)
- Thẻ thói quen loại `LOG_EXISTING`: gọi `completeHabit(habitId, habitSuggestion.localDate, habitSuggestion.targetValue)` từ `@/features/selfcare` (dùng `localDate` do backend tính sẵn theo `habit.timezone`, không lấy theo đồng hồ trình duyệt), rồi hiện toast "Đã ghi nhận".
- Loại `CREATE_FROM_TEMPLATE`: mở **màn hình tạo thói quen đã điền sẵn**. Màn hình này chưa tồn tại nên cần làm trong `features/selfcare` (form + `createHabit`, chọn mục tiêu hoặc tạo mục tiêu mới, chọn "gắn với công cụ" `toolCode`).
- "Không, cảm ơn": gọi endpoint riêng `POST /api/v1/companion/habit-suggestions/dismiss` (kèm allowlist trong `secured-proxy.ts`) để backend ghi nhận việc ẩn 7 ngày, không đi qua `POST /api/v1/journal-writing-suggestions` để không tốn quota `companion-turn`.
- **Xong khi**: chỉ ghi dữ liệu khi người dùng bấm; thẻ không hiện khi cảm xúc là "Rất tệ".

### Bước 5: Hoàn thiện
- Khả năng truy cập:
  - Khung chat có `role="complementary"` và `aria-label`.
  - Danh sách tin nhắn có `aria-live="polite"`.
  - Mọi nút có `aria-label` tiếng Việt.
  - Thứ tự Tab: trang viết, cột công cụ, khung chat.
  - `Esc` thu nhỏ khung chat, không đóng hẳn.
- Văn bản dùng `lib/i18n.ts` nếu trang viết đã dùng; nếu chưa thì giữ chuỗi tiếng Việt trong `constants`.
- Cập nhật `README.md` của web và [`backend/docs/COMPANION_CHATBOT_PLAN.md`](../../backend/docs/COMPANION_CHATBOT_PLAN.md) theo trạng thái thật.

---

## 6. Kiểm tra

`package.json` nay đã có `@playwright/test` thật (`npm run test:e2e`, cấu hình ở `playwright.config.ts`, test ở `tests/`, xem `tests/README.md`) bên cạnh `lint`/`build`.

| Loại | Cách làm |
|---|---|
| Tĩnh | `npx tsc --noEmit`, `npm run lint` sau mỗi bước (`npm run build` hiện lỗi Turbopack không liên quan tới companion, xem ghi chú trong `tests/README.md`/báo cáo phiên làm việc) |
| Playwright — không cần backend | `npx playwright test smoke` |
| Playwright — cần backend + tài khoản kiểm thử | `npx playwright test journal-editor-calming-tools`, các spec Bước 2–4 thêm sau sẽ theo mẫu `tests/helpers/login.ts` |
| Kịch bản thủ công kiểu cũ | [`JOURNAL_NAV_PLAYWRIGHT_SCENARIOS.md`](JOURNAL_NAV_PLAYWRIGHT_SCENARIOS.md) (điều hướng rời trang, không riêng cho companion) |

`tests/journal-editor-calming-tools.spec.ts` đã viết đủ cho Bước 1 (mở menu, chạy/dừng Thở 2 phút, chạy/dừng Âm thanh dịu, Viết xả chèn đúng dòng, rời trang khi đang phát không kẹt). `smoke.spec.ts` chạy thật, pass 3/3.

**Đã chạy thật với backend thật + tài khoản thật (không phải mock), project `chromium`: 4/4 pass.** Project `mobile-chromium` (Pixel 7) bị `429 Too Many Requests` ở bước đăng nhập — tự gây ra do thử đăng nhập liên tiếp nhiều lần trong lúc gỡ lỗi, không phải lỗi app (`IdentityService.java:340`: tối đa 5 lần/15 phút/subject); chưa xác nhận lại được ở viewport điện thoại, nên cần chạy lại sau khi hết cửa sổ giới hạn.

**Phát hiện khi gỡ lỗi — đã sửa.** `sameOrigin()` so khớp `Origin` header với `new URL(request.url).origin`, mà dưới `next dev` origin đó luôn là `http://localhost:3000` bất kể `--hostname 127.0.0.1` trong script `dev` — nên mọi request đổi trạng thái (đăng nhập, đăng ký...) qua `http://127.0.0.1:3000` đều bị 403 trước khi chạm backend, dù `Origin` trình duyệt gửi hoàn toàn đúng. Đã sửa [`features/auth/api/backend.ts`](features/auth/api/backend.ts) để so khớp `Origin` với **`Host` header** (cách chống CSRF chuẩn, không phụ thuộc cách Next.js tự suy ra `request.url`) thay vì `new URL(request.url).origin`. Có test hồi quy ở [`tests/same-origin-guard.spec.ts`](tests/same-origin-guard.spec.ts) (3 ca: origin khớp → chạm được backend; origin giả mạo → 403; thiếu origin → 403), đã chạy thật và pass cả `chromium` lẫn `mobile-chromium`. `playwright.config.ts` vẫn giữ `http://localhost:3000` làm `BASE_URL` mặc định (không còn bắt buộc sau khi sửa, nhưng là domain ổn định nhất cho dev).

Các kịch bản cần có:
1. Mở Công cụ dịu → Thở 2 phút → Dừng giữa chừng; không có request mạng nào.
2. Mở Trợ lý lần đầu chưa có consent → thẻ xin phép → Đồng ý → nhận câu mở.
3. Chọn "Để sau" → chỉ còn công cụ, ô nhắn bị khóa.
4. Bấm câu hỏi → được chèn vào bài, con trỏ ở dòng dưới, gõ tiếp được.
5. Đang chờ trả lời thì gửi tin khác → chỉ response mới nhất được hiển thị.
6. Thu nhỏ → trợ lý trả lời → có chấm chưa đọc → mở lại vẫn còn hội thoại.
7. Backend trả `SAFETY_FLOW` → khung chat đóng, hội thoại bị xóa, `SafetyModal` mở.
8. Tắt mạng → "Trợ lý đang tạm nghỉ".
9. Điện thoại (~390px): bottom sheet không che nút Lưu khi thu nhỏ; gõ bài được khi sheet mở một nửa.
10. Cảm xúc "Rất tệ" → không có thẻ thói quen sau khi thở xong.
11. Rời trang khi âm thanh đang phát → âm thanh tắt; không có gì trong `localStorage` ngoài bản nháp.

---

## 7. Phụ thuộc và câu hỏi mở

| Mục | Phụ thuộc / cần quyết định |
|---|---|
| Bước 3 | Backend giai đoạn 1 (contract mở rộng, quota `companion-turn`) |
| Bước 4 | Backend giai đoạn 5 (`habits.tool_code`, `habitSuggestion`) và **màn hình tạo thói quen** chưa có |
| Âm thanh thật | Hiện chỉ có tiếng mưa tạo bằng Web Audio. Nhạc thật cần file có bản quyền, tự host. |
| Panel ở 768–1359px | Chốt với thiết kế: panel phủ bên phải hay bottom sheet giống điện thoại |
| Mobile app (Expo) | Ngoài phạm vi; dùng lại contract khi làm |
