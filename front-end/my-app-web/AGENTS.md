Dưới đây là bản `AGENTS.md` mình viết lại theo hướng **Feature-Based Architecture + Next.js App Router**, đồng thời bổ sung các quy tắc để AI Agent khó "đặt file tùy tiện", hạn chế spaghetti code, over-engineering và cross-feature dependency.

Bạn có thể copy trực tiếp vào `AGENTS.md`.

````md
<!-- BEGIN:nextjs-agent-rules -->
<!--
Giữ nguyên block Next.js agent rules hiện có của dự án tại đây.
Không tự ý xóa hoặc chỉnh sửa nếu không có yêu cầu rõ ràng.
-->
<!-- END:nextjs-agent-rules -->

# MyLog — AGENTS.md

Tài liệu này là quy chuẩn kiến trúc và cách tổ chức mã nguồn bắt buộc cho tất cả AI Coding Agent và lập trình viên làm việc trong dự án MyLog.

Áp dụng cho:

- Antigravity
- Cursor
- Claude Code
- GitHub Copilot
- Codex
- Các AI Coding Agent khác
- Developer tham gia dự án

Mục tiêu của tài liệu:

- Giữ cấu trúc project nhất quán.
- Ngăn spaghetti code.
- Ngăn business logic bị nhét vào `page.tsx`.
- Ngăn components/hooks/utils bị đặt tùy tiện.
- Hạn chế duplicated code.
- Giữ feature độc lập.
- Hạn chế cyclic dependency.
- Tránh over-engineering.
- Giúp code dễ đọc, dễ maintain và dễ mở rộng.

---

# 1. Architecture Principle

Nguyên tắc quan trọng nhất của dự án:

> Organize code by business ownership first, technical type second.

Hay:

> Ưu tiên tổ chức code theo Feature / Domain trước, loại kỹ thuật sau.

Không tổ chức toàn bộ project theo kiểu:

```text
components/
hooks/
services/
utils/
````

rồi đưa tất cả các feature vào chung các thư mục trên.

Ví dụ KHÔNG NÊN:

```text
components/
├── LoginForm.tsx
├── JournalEditor.tsx
├── MoodChart.tsx
├── InsightCard.tsx
└── Calendar.tsx

hooks/
├── useAuth.ts
├── useJournal.ts
└── useMood.ts

services/
├── auth.ts
├── journal.ts
└── insight.ts
```

Nên tổ chức:

```text
features/
├── auth/
│   ├── components/
│   ├── hooks/
│   ├── api/
│   └── schemas/
│
├── journal/
│   ├── components/
│   ├── hooks/
│   ├── api/
│   └── schemas/
│
└── insights/
    ├── components/
    ├── hooks/
    ├── api/
    └── types/
```

Feature là đơn vị tổ chức chính của business code.

---

# 2. Project Structure

Cấu trúc tổng quát của MyLog:

```text
src/
│
├── app/
│   │
│   ├── (public)/
│   │   ├── page.tsx
│   │   └── layout.tsx
│   │
│   ├── (auth)/
│   │   ├── login/
│   │   │   └── page.tsx
│   │   │
│   │   ├── register/
│   │   │   └── page.tsx
│   │   │
│   │   └── layout.tsx
│   │
│   ├── (app)/
│   │   ├── dashboard/
│   │   │   ├── page.tsx
│   │   │   ├── loading.tsx
│   │   │   └── error.tsx
│   │   │
│   │   ├── journal/
│   │   │   └── page.tsx
│   │   │
│   │   ├── calendar/
│   │   │   └── page.tsx
│   │   │
│   │   ├── insights/
│   │   │   └── page.tsx
│   │   │
│   │   └── layout.tsx
│   │
│   ├── api/
│   │   └── ...
│   │
│   ├── layout.tsx
│   ├── globals.css
│   ├── error.tsx
│   └── not-found.tsx
│
├── features/
│   ├── auth/
│   ├── journal/
│   ├── calendar/
│   ├── insights/
│   ├── mood/
│   └── user/
│
├── components/
│   ├── ui/
│   └── layout/
│
├── lib/
├── hooks/
├── providers/
├── config/
├── constants/
└── types/
```

Không bắt buộc mọi thư mục trên phải tồn tại ngay từ đầu.

Chỉ tạo folder khi có nhu cầu thực tế.

KHÔNG tạo folder rỗng chỉ để làm cấu trúc trông "đầy đủ".

---

# 3. Path Alias

Ưu tiên sử dụng path alias:

```ts
@/*
```

trỏ tới:

```text
src/*
```

Ví dụ:

```ts
import { Button } from "@/components/ui/Button";
```

thay vì:

```ts
import { Button } from "../../../../components/ui/Button";
```

Không sử dụng relative import quá sâu nếu có thể dùng alias.

---

# 4. Layer Responsibilities

---

## 4.1 `app/` — Routing & Composition Layer

`app/` chịu trách nhiệm cho:

* Routing.
* Route Groups.
* Layout.
* Metadata.
* Loading boundary.
* Error boundary.
* Not Found.
* Route params.
* Search params.
* Page composition.
* Server entry point.

`app/` KHÔNG phải nơi chứa business logic chính.

### `page.tsx`

`page.tsx` nên chủ yếu:

1. Nhận `params`.
2. Nhận `searchParams`.
3. Fetch dữ liệu cấp page nếu phù hợp.
4. Import feature component.
5. Compose page.
6. Render.

Ví dụ:

```tsx
import { JournalEditor } from "@/features/journal";

export default function JournalPage() {
  return <JournalEditor />;
}
```

Không nên:

```tsx
export default function JournalPage() {
  // 10 useState

  // fetch API

  // validate journal

  // calculate mood

  // transform data

  // AI processing

  // save journal

  // 400 dòng JSX
}
```

### Lưu ý về độ dài file

Không sử dụng số dòng như một luật tuyệt đối.

`100–150 dòng` chỉ là tín hiệu cần review.

Cần refactor khi:

* Có business logic đáng kể.
* Có nhiều state không liên quan.
* Có nhiều section UI độc lập.
* Component khó đọc.
* Component khó test.
* Page biết quá nhiều chi tiết implementation.

Không chia file chỉ vì file dài.

---

# 5. Route Groups

Sử dụng Route Groups để phân vùng application.

```text
(public)
(auth)
(app)
```

## `(public)`

Trang công khai.

Ví dụ:

```text
/
about
features
privacy
```

## `(auth)`

Authentication.

Ví dụ:

```text
/login
/register
/forgot-password
```

## `(app)`

Các trang yêu cầu đăng nhập.

Ví dụ:

```text
/dashboard
/journal
/calendar
/insights
/profile
```

---

# 6. `features/` — Business / Feature Layer

`features/` là trung tâm của kiến trúc MyLog.

Mỗi feature đại diện cho một capability hoặc domain nghiệp vụ.

Ví dụ:

```text
features/
├── auth/
├── journal/
├── calendar/
├── mood/
├── insights/
└── user/
```

Một feature có thể có cấu trúc:

```text
features/
└── journal/
    ├── components/
    │   ├── JournalEditor.tsx
    │   ├── JournalEntry.tsx
    │   └── JournalMoodSelector.tsx
    │
    ├── hooks/
    │   └── useJournalEditor.ts
    │
    ├── api/
    │   ├── get-journal.ts
    │   └── get-journals.ts
    │
    ├── actions/
    │   └── save-journal.action.ts
    │
    ├── schemas/
    │   └── journal.schema.ts
    │
    ├── types/
    │   └── journal.types.ts
    │
    ├── utils/
    │   └── journal-date.ts
    │
    └── index.ts
```

Không cần tạo tất cả các folder trên nếu feature chưa cần.

Ví dụ feature đơn giản có thể chỉ có:

```text
features/
└── calendar/
    ├── components/
    └── index.ts
```

---

# 7. Feature Ownership

Trước khi tạo file mới, Agent PHẢI xác định:

> Ai sở hữu đoạn code này?

Có 3 loại ownership chính.

---

## 7.1 Route-specific

Nếu component chỉ được dùng bởi một route:

```text
app/[route]/_components/
```

Ví dụ:

```text
app/
└── (app)/
    └── dashboard/
        ├── _components/
        │   ├── DashboardGreeting.tsx
        │   └── DashboardSkeleton.tsx
        │
        └── page.tsx
```

Không đưa `DashboardGreeting` vào:

```text
components/
```

nếu nó chỉ phục vụ Dashboard.

---

## 7.2 Feature-specific

Nếu code thuộc business của một feature:

```text
features/[feature]/
```

Ví dụ:

```text
JournalEditor
JournalMoodSelector
JournalCard
useJournalEditor
journalSchema
saveJournal
```

phải nằm trong:

```text
features/journal/
```

Không đưa chúng vào:

```text
src/components/
src/hooks/
src/lib/
```

---

## 7.3 Shared

Chỉ đưa code lên Shared Layer khi:

* Không thuộc business cụ thể.
* Có thể được dùng bởi nhiều feature.
* Có API tổng quát.
* Không phụ thuộc feature.

Ví dụ hợp lệ:

```text
components/ui/Button.tsx
components/ui/Input.tsx

hooks/useDebounce.ts

lib/api/client.ts
```

Ví dụ không hợp lệ:

```text
components/ui/JournalCard.tsx

hooks/useJournal.ts

lib/calculateMoodScore.ts
```

---

# 8. File Placement Decision Tree

Trước khi tạo code mới, sử dụng decision tree sau:

```text
Tôi sắp tạo code mới
        │
        ▼
Chỉ dùng trong một route?
        │
   Yes ────────────→ app/[route]/_components
        │
       No
        ▼
Thuộc business/domain cụ thể?
        │
   Yes ────────────→ features/[feature]
        │
       No
        ▼
Là UI generic?
        │
   Yes ────────────→ components/ui
        │
       No
        ▼
Là shared layout?
        │
   Yes ────────────→ components/layout
        │
       No
        ▼
Là generic hook?
        │
   Yes ────────────→ hooks/
        │
       No
        ▼
Là infrastructure?
        │
   Yes ────────────→ lib/
        │
       No
        ▼
DỪNG.

Xem lại ownership trước khi tạo file.
```

Không tạo file nếu chưa xác định được ownership hợp lý.

---

# 9. `components/` — Shared UI Layer

`components/` chỉ chứa các component dùng chung.

Cấu trúc:

```text
components/
├── ui/
└── layout/
```

---

## 9.1 `components/ui/`

Dùng cho reusable UI primitives.

Ví dụ:

```text
Button.tsx
Input.tsx
Modal.tsx
Card.tsx
Spinner.tsx
Badge.tsx
Tabs.tsx
NeoButton.tsx
ScrapbookDecoration.tsx
```

Các component này nên:

* Generic.
* Reusable.
* Không biết business domain.
* Không biết Journal là gì.
* Không biết Insight là gì.
* Không fetch business API.

Không được:

```tsx
import { Journal } from "@/features/journal";
```

trong:

```text
components/ui/Card.tsx
```

---

## 9.2 `components/layout/`

Dùng cho layout cấp ứng dụng.

Ví dụ:

```text
Header.tsx
Sidebar.tsx
Footer.tsx
Navigation.tsx
AppShell.tsx
```

---

# 10. Dependency Direction

Dependency phải đi theo hướng:

```text
app
 ↓
features
 ↓
shared layer
```

Shared layer không được phụ thuộc ngược lại feature.

---

## Allowed

```text
app → features

app → components

app → lib

features → components/ui

features → components/layout

features → lib

features → hooks

features → config

features → constants

features → shared types
```

---

## Forbidden

Không được:

```text
components/ui → features

components/layout → feature business logic

lib → features

hooks → features

config → features
```

Ví dụ sai:

```tsx
// components/ui/Card.tsx

import type { Journal } from "@/features/journal";
```

Ví dụ đúng:

```tsx
interface CardProps {
  children: React.ReactNode;
}

export function Card({ children }: CardProps) {
  return <div>{children}</div>;
}
```

Sau đó feature compose:

```tsx
<Card>
  <JournalContent />
</Card>
```

---

# 11. Feature Encapsulation

Mỗi feature được xem như một module độc lập.

Không được deep-import implementation nội bộ của feature khác.

Ví dụ:

```text
features/
├── journal/
└── insights/
```

Trong `insights`, không nên:

```ts
import { something } from "@/features/journal/utils/internalSomething";
```

Nếu `journal` cần expose functionality ra ngoài, phải thông qua public API.

Ví dụ:

```text
features/
└── journal/
    ├── components/
    ├── hooks/
    ├── types/
    └── index.ts
```

`index.ts`:

```ts
export { JournalCard } from "./components/JournalCard";
export type { Journal } from "./types/journal.types";
```

Feature khác import:

```ts
import {
  JournalCard,
  type Journal,
} from "@/features/journal";
```

Tránh:

```ts
import { JournalCard }
  from "@/features/journal/components/JournalCard";
```

đặc biệt khi import từ feature khác.

---

# 12. Cross-Feature Dependency

Cross-feature dependency phải được hạn chế.

Ví dụ:

```text
insights
↓
journal
```

chỉ được phép nếu thật sự có quan hệ business rõ ràng.

Không được tạo dependency chỉ để reuse một helper nhỏ.

Nếu helper thật sự generic, cân nhắc promote sang shared layer.

Ví dụ:

```text
features/journal/utils/formatDate.ts
```

nếu sau này:

```text
journal
calendar
insights
```

đều cần cùng behavior generic, có thể chuyển thành:

```text
lib/date/format-date.ts
```

Không promote lên shared chỉ vì "có thể sẽ dùng sau này".

---

# 13. Business Logic Definition

Business logic bao gồm nhưng không giới hạn:

* Journal validation.
* Journal state transitions.
* Mood calculation.
* Emotion mapping.
* Insight calculation.
* Calendar journal aggregation.
* Feature-specific filtering.
* AI reflection state.
* Authorization rule.
* Journal status.
* Domain transformation.
* Feature-specific sorting.
* Feature-specific normalization.

Business logic không được viết trực tiếp trong:

```text
page.tsx
layout.tsx
components/ui/
```

Ví dụ không nên:

```tsx
const journals = data
  .filter(...)
  .map(...)
  .sort(...)
  .reduce(...);
```

ngay trong page nếu đây là journal business logic.

Nên:

```ts
const journals = getJournalTimeline(data);
```

và:

```text
features/
└── journal/
    └── utils/
        └── get-journal-timeline.ts
```

---

# 14. Server Component First

Trong Next.js App Router:

> Server Component là mặc định.

Không thêm:

```tsx
"use client";
```

nếu component không thật sự cần.

Client Component thường chỉ cần khi sử dụng:

* `useState`
* `useEffect`
* Event handlers
* Browser APIs
* Client-only libraries
* Interactive UI
* Client Context

Ví dụ không nên:

```tsx
"use client";

export default function DashboardPage() {
  ...
}
```

chỉ vì một button cần `onClick`.

Nên:

```text
DashboardPage          Server
├── DashboardStats     Server
├── JournalList        Server
└── MoodSelector       Client
```

Giữ Client boundary càng nhỏ càng tốt.

Không chuyển parent thành Client Component chỉ để phục vụ một child interactive.

---

# 15. API vs Server Actions

Không sử dụng `api/` và `actions/` lẫn lộn.

---

## `api/`

Dùng cho data access hoặc API client.

Ví dụ:

```text
features/
└── journal/
    └── api/
        ├── get-journals.ts
        ├── get-journal.ts
        └── create-journal.ts
```

Ví dụ:

```ts
export async function getJournals() {
  ...
}
```

---

## `actions/`

Chỉ dùng cho Next.js Server Actions.

Ví dụ:

```text
features/
└── journal/
    └── actions/
        └── save-journal.action.ts
```

File Server Action phải phù hợp với convention đang được dự án sử dụng.

Không gọi một function là `action` nếu nó chỉ là API helper thông thường.

---

# 16. State Management Rules

State phải được giữ ở phạm vi nhỏ nhất có thể.

Ưu tiên:

```text
Local State
    ↓
Feature Hook
    ↓
Feature Context
    ↓
Global State
```

Không đưa state lên Global nếu chỉ một feature sử dụng.

Ví dụ:

```text
Modal open/close
→ component local state

Journal editor draft
→ journal feature

Calendar selected date
→ calendar feature

Theme
→ shared/global provider
```

Không tự động thêm:

* Redux
* Zustand
* Context
* Global store

nếu local state hoặc feature hook đã giải quyết được.

---

# 17. Provider Ownership

Provider chỉ nằm trong:

```text
src/providers/
```

nếu nó thực sự được dùng ở mức application/global.

Ví dụ:

```text
QueryProvider
ThemeProvider
ToastProvider
AuthProvider
```

Nếu provider chỉ phục vụ Journal:

```text
features/
└── journal/
    └── providers/
        └── JournalProvider.tsx
```

Không đưa feature-specific Provider lên global nếu không cần.

---

# 18. `lib/` — Shared Infrastructure

`lib/` chỉ chứa:

* Infrastructure.
* Generic utilities.
* External integrations.
* API foundation.
* Auth infrastructure.
* Storage.
* Date utility dùng chung.

Ví dụ hợp lệ:

```text
lib/
├── api/
│   └── client.ts
│
├── auth/
├── storage/
└── date/
    └── format-date.ts
```

Không sử dụng `lib/` như một "sọt rác".

Ví dụ không nên:

```text
lib/calculateMoodScore.ts
lib/journalAnalyzer.ts
lib/generateReflection.ts
```

nếu những logic này thuộc feature.

Nên:

```text
features/
└── insights/
    └── utils/
        └── calculate-mood-score.ts
```

---

# 19. Shared Hooks

`src/hooks/` chỉ dành cho hook generic dùng bởi nhiều feature.

Ví dụ:

```text
useDebounce.ts
useMediaQuery.ts
useOutsideClick.ts
```

Không đặt:

```text
useJournal.ts
useInsight.ts
useMoodAnalysis.ts
```

vào `src/hooks/`.

Các hook trên phải thuộc feature tương ứng.

---

# 20. Naming Conventions

## Components

PascalCase:

```text
JournalEditor.tsx
LoginForm.tsx
MoodSelector.tsx
InsightCard.tsx
```

---

## Hooks

camelCase và bắt đầu bằng `use`.

```text
useAuth.ts
useJournalEditor.ts
useDebounce.ts
```

---

## Schema

```text
auth.schema.ts
journal.schema.ts
profile.schema.ts
```

---

## Actions

```text
login.action.ts
save-journal.action.ts
delete-journal.action.ts
```

---

## Types

```text
journal.types.ts
auth.types.ts
insight.types.ts
```

---

## Utilities

Tên mô tả rõ chức năng.

Ví dụ:

```text
calculate-mood-score.ts
group-journals-by-date.ts
format-journal-date.ts
```

Tránh:

```text
helper.ts
utils.ts
common.ts
misc.ts
functions.ts
```

trừ khi file thật sự chứa generic API nhỏ và có lý do rõ ràng.

---

# 21. Design System Rules

MyLog sử dụng visual language:

* Neo-Brutalism.
* Scrapbook.
* Paper texture.
* Black borders.
* Neo shadows.
* Brand lime.
* Warm paper background.

Không được tự ý phá design system.

Ưu tiên sử dụng token hiện có.

Ví dụ:

```text
paper-warm
brand-lime
shadow-neo
```

Không hard-code màu mới nếu design token phù hợp đã tồn tại.

---

# 22. Icons

UI control phải sử dụng icon từ:

```text
lucide-react
```

Ví dụ:

```tsx
import { Search, Plus, Trash2 } from "lucide-react";
```

Không sử dụng emoji làm icon điều khiển chính.

Không nên:

```tsx
<button>🗑️</button>
```

Nên:

```tsx
<button>
  <Trash2 />
</button>
```

Emoji có thể sử dụng nếu nó là nội dung semantic của sản phẩm.

Ví dụ mood:

```text
😄
😐
😢
```

là hợp lệ nếu mood system của MyLog sử dụng emoji.

---

# 23. Existing Pattern First

Trước khi implement feature mới, Agent PHẢI:

1. Tìm feature gần giống.
2. Xem naming convention hiện tại.
3. Xem cách project fetch data.
4. Xem cách project mutation data.
5. Xem cách validation đang được thực hiện.
6. Xem cách UI component đang được tổ chức.
7. Reuse pattern hiện có nếu phù hợp.

Không tạo một architecture mới cho mỗi feature.

Ví dụ:

Nếu `auth` đang dùng:

```text
components/
hooks/
api/
schemas/
```

không tự ý tạo cho `journal`:

```text
domain/
repositories/
entities/
useCases/
adapters/
```

nếu project không sử dụng kiến trúc đó.

---

# 24. Avoid Over-Engineering

Không tạo abstraction nếu chưa có nhu cầu thực tế.

Không tự động tạo:

* Repository layer.
* Factory.
* Adapter.
* Service layer.
* Global store.
* Context.
* Generic wrapper.
* Base class.
* Generic component.
* Custom hook.

chỉ vì "có thể sẽ dùng".

Ưu tiên:

```text
Simple
↓
Readable
↓
Colocated
↓
Reusable khi thực sự cần
```

thay vì:

```text
Generic ngay từ đầu
```

---

# 25. No Premature Shared Abstraction

Không đưa code lên Shared Layer chỉ vì dự đoán:

> "Sau này có thể dùng lại."

Một component chỉ dùng trong Journal nên ở:

```text
features/journal/
```

Nếu sau này Calendar cũng cần, khi đó mới xem xét abstraction.

Promote code lên shared khi reuse thực tế đã xuất hiện.

---

# 26. Scope Discipline

Agent KHÔNG được tự ý:

* Refactor code không liên quan.
* Rename folder ngoài phạm vi task.
* Thay architecture toàn hệ thống.
* Thay state management library.
* Thay API contract.
* Thay design system.
* Thay package hiện có.
* Move nhiều file chỉ để "clean hơn".
* Rewrite toàn feature khi chỉ cần sửa một bug.

Nếu phát hiện vấn đề ngoài phạm vi task:

1. Báo vấn đề.
2. Giải thích ảnh hưởng.
3. Đề xuất hướng xử lý.
4. Không tự sửa nếu chưa cần cho task hiện tại.

---

# 27. Reuse Before Create

Trước khi tạo:

```text
component
hook
schema
type
utility
API function
```

Agent PHẢI tìm implementation hiện có.

Không tạo:

```text
Button.tsx
PrimaryButton.tsx
AppButton.tsx
NeoButton.tsx
```

nếu một component hiện tại có thể mở rộng một cách hợp lý.

Không duplicate helper chỉ vì nằm ở feature khác.

Hãy kiểm tra ownership và khả năng reuse trước.

---

# 28. Refactoring Rules

Khi refactor:

* Không thay đổi behavior nếu task không yêu cầu.
* Không thay đổi public API không cần thiết.
* Không đổi naming hàng loạt nếu không liên quan.
* Không thêm abstraction chỉ để giảm số dòng.
* Không tách component quá nhỏ nếu không cải thiện readability hoặc reuse.
* Giữ scope refactor nhỏ nhất có thể.

Tách logic khỏi UI khi:

* Logic phức tạp.
* Có thể test độc lập.
* Có nhiều state transition.
* Có business rule.
* Component trở nên khó hiểu.

---

# 29. Validation

Validation thuộc feature phải nằm cạnh feature.

Ví dụ:

```text
features/
└── auth/
    └── schemas/
        └── auth.schema.ts
```

Không đưa tất cả validation schema của mọi feature vào:

```text
src/schemas/
```

trừ khi project có lý do kiến trúc rõ ràng.

---

# 30. Types

Type chỉ dùng trong một feature:

```text
features/[feature]/types/
```

Ví dụ:

```text
features/
└── journal/
    └── types/
        └── journal.types.ts
```

Type dùng thực sự xuyên nhiều module mới đưa vào:

```text
src/types/
```

Không đưa tất cả type vào `src/types/` theo mặc định.

---

# 31. Error / Loading / Empty States

Khi tạo feature có data fetching, Agent phải cân nhắc ít nhất:

```text
Loading
Error
Empty
Success
```

Không chỉ implement happy path.

Ví dụ Journal History:

```text
Loading
↓
Skeleton

Error
↓
Retry UI

Empty
↓
"Bạn chưa có trang nhật ký nào."

Success
↓
Journal list
```

---

# 32. AI Feature Rules

Các feature liên quan AI của MyLog phải tách rõ:

```text
UI
Business state
AI request
AI response transformation
Safety handling
```

Không viết tất cả trong một component.

Không nên:

```tsx
JournalEditor.tsx

- textarea
- fetch AI
- prompt building
- parse AI
- safety logic
- mood calculation
- API save
- rendering
```

Nên phân tách ownership phù hợp.

Ví dụ:

```text
features/
└── journal/
    ├── components/
    │   ├── JournalEditor.tsx
    │   └── ReflectionPanel.tsx
    │
    ├── hooks/
    │   └── useJournalEditor.ts
    │
    ├── api/
    │   ├── save-journal.ts
    │   └── request-reflection.ts
    │
    ├── schemas/
    ├── types/
    └── utils/
```

Không expose technical AI metadata cho user nếu UI không cần.

Ví dụ:

```text
confidence
model
engine id
internal safety flag
debug ID
```

nên nằm trong internal/debug layer nếu không phải user-facing information.

---

# 33. Agent Workflow

Mọi Coding Agent PHẢI thực hiện theo workflow sau.

---

## BEFORE CODING

Trước khi viết code:

1. Đọc cấu trúc thư mục liên quan.
2. Xác định feature sở hữu yêu cầu.
3. Tìm implementation hiện có.
4. Tìm component/hook/helper có thể reuse.
5. Xác định code thuộc:

   * Route-local.
   * Feature.
   * Shared UI.
   * Shared infrastructure.
6. Xác định Server hay Client Component.
7. Xác định file nào cần:

   * Tạo.
   * Sửa.
   * Xóa.
8. Kiểm tra thay đổi có nằm đúng scope task không.

Không tạo file mới trước khi xác định ownership.

---

## WHILE CODING

Agent PHẢI:

* Giữ business logic trong feature.
* Giữ `page.tsx` là composition layer.
* Ưu tiên Server Components.
* Giữ Client Component nhỏ.
* Không duplicate code.
* Không deep-import feature khác.
* Không đưa feature code vào Shared Layer tùy tiện.
* Không thêm dependency nếu chưa cần.
* Không refactor unrelated code.
* Không thay đổi architecture ngoài scope.
* Không tạo abstraction chưa cần thiết.
* Tuân thủ Design System.
* Tuân thủ naming convention.
* Giữ TypeScript type-safe.

---

## AFTER CODING

Agent phải tự kiểm tra:

### Architecture

* File có đúng ownership không?
* Business logic có nằm đúng feature không?
* Có dependency ngược từ Shared → Feature không?
* Có cross-feature deep import không?

### Next.js

* Có `"use client"` không cần thiết không?
* Có biến toàn page thành Client Component không?
* Loading/error boundary có cần thiết không?

### Code Quality

* Có duplicate helper không?
* Có duplicate component không?
* Có function quá nhiều trách nhiệm không?
* Có `any` không cần thiết không?
* Có magic value nên dùng constant không?

### UI

* Có reuse Design System không?
* Có hard-code màu không cần thiết không?
* Icon có dùng `lucide-react` không?
* Có loading / error / empty state không?

### Scope

* Có sửa file ngoài phạm vi không?
* Có refactor thứ user không yêu cầu không?
* Có thêm package không cần thiết không?

---

# 34. Do / Don't Examples

## Page

### Don't

```tsx
export default function JournalPage() {
  const [content, setContent] = useState("");
  const [mood, setMood] = useState(0);

  const saveJournal = async () => {
    // business logic
  };

  const analyzeJournal = async () => {
    // AI logic
  };

  return (
    // hundreds of lines
  );
}
```

### Do

```tsx
import { JournalEditor } from "@/features/journal";

export default function JournalPage() {
  return <JournalEditor />;
}
```

---

## Feature component

### Don't

```text
components/
└── JournalEditor.tsx
```

### Do

```text
features/
└── journal/
    └── components/
        └── JournalEditor.tsx
```

---

## Shared component

### Don't

```tsx
interface CardProps {
  journal: Journal;
}
```

trong:

```text
components/ui/Card.tsx
```

### Do

```tsx
interface CardProps {
  children: React.ReactNode;
}
```

---

## Hook

### Don't

```text
src/hooks/useJournalEditor.ts
```

### Do

```text
features/journal/hooks/useJournalEditor.ts
```

---

## Utility

### Don't

```text
src/lib/calculateMoodScore.ts
```

### Do

```text
features/insights/utils/calculate-mood-score.ts
```

nếu đây là logic của Insights.

---

# 35. Rules Priority

Khi có xung đột, ưu tiên theo thứ tự:

1. Correctness.
2. Security.
3. Existing project architecture.
4. Feature ownership.
5. Simplicity.
6. Maintainability.
7. Reusability.
8. Performance optimization.
9. Abstraction.

Không hy sinh correctness để giữ architecture "đẹp".

Không tạo abstraction phức tạp chỉ để đạt reuse lý thuyết.

---

# 36. Final Architecture Rule

Nếu Agent không chắc file nên nằm ở đâu, hãy hỏi:

> Business nào sở hữu đoạn code này?

Nếu câu trả lời là:

```text
Journal
```

đặt trong:

```text
features/journal/
```

Nếu:

```text
Auth
```

đặt trong:

```text
features/auth/
```

Nếu:

```text
Calendar
```

đặt trong:

```text
features/calendar/
```

Nếu không thuộc business nào và thật sự generic:

```text
components/
hooks/
lib/
```

mới được cân nhắc.

---

# 37. Golden Rule

> Colocate first. Share later.

Hay:

> Đặt code gần feature sở hữu nó trước.
> Chỉ đưa lên shared khi có nhu cầu reuse thực tế.

Mục tiêu cuối cùng:

```text
app/
    routing + composition

features/
    business

components/
    shared UI

lib/
    infrastructure

hooks/
    generic shared hooks

providers/
    application-level providers

config/
    configuration

constants/
    shared constants

types/
    truly shared types
```

Nếu một thay đổi vi phạm cấu trúc trên mà không có lý do rõ ràng, Agent phải dừng lại và đánh giá lại kiến trúc trước khi tiếp tục.

```

Có một điểm mình đặc biệt khuyên giữ trong file này: **`Colocate first. Share later.`** Đây là rule đơn giản nhưng ngăn được rất nhiều lỗi kiến trúc do AI gây ra. Nếu không có rule này, Agent rất dễ tạo `components/`, `utils/`, `services/` thành các "sọt rác" chỉ sau vài feature.
```
