# Diary Web Design Specification

## 1. Project Overview

**Project name:** My Log

**Design concept:** A digital diary that feels like a physical notebook.

The website combines: - Playful Neo-Brutalism - Editorial / Magazine
layout - Scrapbook / Collage aesthetics - Minimal modern UI

The visual direction should feel personal, expressive, slightly
imperfect, and memorable while remaining clean enough for a professional
Front-end project.

------------------------------------------------------------------------

## 2. Design Keywords

-   Playful
-   Editorial
-   Scrapbook
-   Collage
-   Minimal
-   Bold typography
-   High contrast
-   Organic positioning
-   Physical notebook feeling
-   Modern digital experience

### Style name

**Playful Neo-Brutalism + Editorial Collage + Scrapbook**

This is not strict Neo-Brutalism. It uses Neo-Brutalist visual language
together with magazine-style typography and physical diary/scrapbook
objects.

------------------------------------------------------------------------

## 3. Design Principles

### 3.1 Typography first

Large, bold headlines should be one of the main visual elements.

Example:

> WRITE IT.\
> KEEP IT.\
> REMEMBER IT.

Avoid filling the hero section with too much text.

### 3.2 Physical objects in a digital space

Use objects associated with journaling as decorative elements:

-   Open book
-   Closed diary
-   Fountain pen
-   Pencil
-   Sticky note
-   Polaroid photo
-   Bookmark
-   Letter / envelope
-   Coffee cup
-   Small plant or leaf

Objects should look like physical items placed on a desk or scrapbook
page.

### 3.3 Intentional imperfection

Some decorative objects can: - Rotate slightly - Extend outside the
viewport - Overlap other elements - Be partially cropped - Have
different sizes

This creates the collage feeling.

### 3.4 Strong contrast

Use a mostly neutral background with black typography and borders.

A bright accent color should be used for: - Primary CTA - Small labels -
Selected states - Decorative details

------------------------------------------------------------------------

# 4. Color System

## Primary colors

  Token          Color       Usage
  -------------- ----------- --------------------------
  `background`   `#F5F5F3`   Main page background
  `surface`      `#FFFFFF`   Cards and paper surfaces
  `foreground`   `#111111`   Main text
  `border`       `#111111`   Borders
  `accent`       `#B7FF32`   Primary CTA / highlights
  `paper`        `#F3EBDD`   Notebook / paper objects
  `muted`        `#D9D7D2`   Secondary UI

The exact accent color can be adjusted during implementation, but the
visual direction should remain a bright lime/green accent on a neutral
background.

------------------------------------------------------------------------

# 5. Typography

## Recommended fonts

### Headings

Recommended: - Archivo Black - Anton - Space Grotesk - Manrope ExtraBold

### Body

Recommended: - Inter - DM Sans - Manrope

### Diary content

For diary entries, a serif font can optionally be used to reinforce the
physical-book feeling:

-   Lora
-   Playfair Display
-   Source Serif

## Typography hierarchy

``` text
Hero heading
72–120px
Extra Bold / Black
Very tight line-height

Section heading
48–72px
Bold

Card heading
24–32px
Bold

Body
16–18px
Regular

Metadata
12–14px
Medium
```

For responsive layouts, use `clamp()` rather than fixed desktop-only
sizes.

Example:

``` css
font-size: clamp(3rem, 8vw, 7.5rem);
```

------------------------------------------------------------------------

# 6. Layout System

## Desktop

Primary target:

``` text
1440 × 900
```

Maximum content width:

``` text
1280–1360px
```

Use generous whitespace.

The Hero section should occupy most of the first viewport.

## Grid

Use a flexible grid system.

Recommended:

``` text
12-column desktop grid
8-column tablet grid
4-column mobile grid
```

Decorative objects should be positioned independently from the main
content grid.

------------------------------------------------------------------------

# 7. Hero Section

## Objective

Immediately communicate that this is a personal digital diary.

## Layout

The hero should resemble the reference design:

``` text
┌─────────────────────────────────────────────────────────────┐
│  MY DIARY                                  [START WRITING →]│
│                                                             │
│        📖                                                   │
│                                                             │
│                         MY DAILY                            │
│                          DIARY                              │
│                                                             │
│                    WRITE IT. KEEP IT.                      │
│                       REMEMBER IT.                         │
│                                                             │
│               Your thoughts, memories,                     │
│               and little moments.                           │
│                                                             │
│                 [ WRITE TODAY → ]                           │
│                                                             │
│  📝                                             ✒️          │
│                                                             │
│                 📸                  🔖                      │
└─────────────────────────────────────────────────────────────┘
```

## Decorative object behavior

Objects should: - Have subtle rotation - Use black outlines - Have
physical-looking shadows - Be partially outside the viewport where
appropriate - Avoid covering important text

Example transforms:

``` css
transform: rotate(-8deg);
transform: rotate(5deg);
transform: rotate(12deg);
```

Use different rotations rather than applying the same angle to every
object.

------------------------------------------------------------------------

# 8. Hero Content

Recommended copy:

### Option A

``` text
WRITE IT.
KEEP IT.
REMEMBER IT.
```

Supporting text:

``` text
A little place for your thoughts,
memories, and everyday moments.
```

CTA:

``` text
WRITE TODAY →
```

### Option B

``` text
YOUR STORY.
YOUR SPACE.
```

Supporting text:

``` text
Capture the little moments worth remembering.
```

CTA:

``` text
START WRITING →
```

------------------------------------------------------------------------

# 9. Navigation

Keep navigation simple.

``` text
┌────────────────────────────────────────────────────────────┐
│ MY DIARY       Journal   Calendar   Insights   About       │
│                                      [WRITE TODAY →]       │
└────────────────────────────────────────────────────────────┘
```

Navigation should not look like a traditional admin dashboard.

Prefer: - Horizontal navigation - Strong typography - Minimal icons -
One prominent CTA

On mobile:

``` text
MY DIARY                                  ☰
```

------------------------------------------------------------------------

# 10. Recent Memories Section

After the Hero, introduce the user's recent diary entries.

``` text
RECENT
MEMORIES.

┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ SEP 09       │ │ SEP 08       │ │ SEP 07       │
│              │ │              │ │              │
│ A productive │ │ Project day  │ │ Weekend      │
│ day...       │ │ ...          │ │ memories...  │
│              │ │              │ │              │
│ 😊 #study    │ │ 😄 #project  │ │ ☕ #daily    │
└──────────────┘ └──────────────┘ └──────────────┘
```

Cards can have slightly different rotations:

``` text
Card 1: -2deg
Card 2:  1deg
Card 3: -1deg
```

Keep rotations subtle.

------------------------------------------------------------------------

# 11. Write Diary Page

The writing experience should be distraction-free.

``` text
┌─────────────────────────────────────────────────────────────┐
│ ← BACK                                  SAVE     PUBLISH ✓  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ September 9, 2026                                           │
│                                                             │
│ How was your day?                                           │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │                                                         │ │
│ │ Start writing...                                        │ │
│ │                                                         │ │
│ │                                                         │ │
│ │                                                         │ │
│ │                                                         │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ Mood:  😄  😊  😐  😔  😡                                  │
│                                                             │
│ Tags: [study] [university] [+ Add tag]                      │
│                                                             │
│ 📷 Add photo       🎵 Music       📍 Location               │
└─────────────────────────────────────────────────────────────┘
```

Recommended features: - Title - Rich text editor - Mood - Tags - Image
upload - Auto-save - Draft status - Date - Optional location - Optional
music

------------------------------------------------------------------------

# 12. Diary Entry Detail

The detail page should feel like opening a physical diary.

``` text
← BACK

September 9, 2026

A Productive Day

😊 Happy

────────────────────────────────

Today was a really productive day...

I went to university and worked
on my project.

        [ PHOTO ]

Later in the afternoon...

────────────────────────────────

#study   #university   #project

← Previous                         Next →
```

Use generous margins and readable line length.

Recommended content width:

``` text
650–760px
```

------------------------------------------------------------------------

# 13. Calendar Page

Calendar should focus on memories rather than productivity.

``` text
             SEPTEMBER 2026

MON   TUE   WED   THU   FRI   SAT   SUN

      1     2     3     4     5     6
            😊          😄

7     8     9    10    11    12    13
      😐    😊

14   15    16    17    18    19    20

21   22    23    24    25    26    27

28   29    30
```

Each day with a diary entry can display: - Mood - Small dot - Tiny paper
icon - Entry preview on hover

------------------------------------------------------------------------

# 14. Insights Page

Optional feature for a more advanced project.

Display:

``` text
YOUR JOURNAL
INSIGHTS.

42
ENTRIES

12
DAY STREAK

18
FAVORITES
```

Mood chart:

``` text
😊 ███████████████
😄 ███████████
😐 ██████
😔 ███
```

Other possible statistics: - Entries per month - Most-used tags - Most
common mood - Current writing streak - Total photos - Favorite entries

Avoid making this look like a business analytics dashboard. Keep the
editorial diary style.

------------------------------------------------------------------------

# 15. Components

## Global components

``` text
Navbar
Button
IconButton
Badge
Tag
Modal
Tooltip
Footer
```

## Diary components

``` text
DiaryCard
DiaryEditor
MoodPicker
TagInput
PhotoUploader
EntryPreview
EntryMetadata
```

## Navigation components

``` text
Calendar
MonthSelector
Pagination
SearchBar
FilterBar
```

## Decorative components

``` text
BookDecoration
PenDecoration
PaperNote
Polaroid
Bookmark
Sticker
```

Decorative components should be separate from functional components.

------------------------------------------------------------------------

# 16. Button Style

Primary CTA should be visually strong.

Example:

``` text
┌──────────────────────────────┐
│  WRITE TODAY              →  │
└──────────────────────────────┘
```

Recommended characteristics: - Black border - Rounded corners - Bright
lime background - Bold text - Small offset shadow - Arrow icon

Hover:

``` text
Default:
[ WRITE TODAY → ]

Hover:
[ WRITE TODAY → ]
       ↓
Slight movement + stronger shadow
```

Do not use excessive animation.

------------------------------------------------------------------------

# 17. Borders and Shadows

Use visible borders.

Example:

``` css
border: 2px solid #111;
```

For important objects:

``` css
box-shadow: 4px 4px 0 #111;
```

For larger objects:

``` css
box-shadow: 6px 6px 0 #111;
```

Avoid soft, generic SaaS-style shadows everywhere.

The shadow should feel like a physical object sitting on paper.

------------------------------------------------------------------------

# 18. Border Radius

Use a combination of rounded and slightly irregular shapes.

Recommended:

``` text
Buttons:       12–16px
Cards:         12–20px
Paper objects: 4–12px
Images:        8–16px
```

Do not make every component extremely rounded.

------------------------------------------------------------------------

# 19. Animation

Animations should support the playful physical feeling.

Recommended:

### Hover

``` text
translateY(-2px)
rotate(1deg)
```

### Decorative objects

Use very subtle floating animation.

Example concept:

``` text
Object A:
rotate(-5deg) → rotate(-3deg) → rotate(-5deg)

Object B:
translateY(0) → translateY(-5px) → translateY(0)
```

Animation duration:

``` text
2–5 seconds
```

Use `ease-in-out`.

Avoid excessive motion because the website is intended to feel calm and
personal.

------------------------------------------------------------------------

# 20. Responsive Design

## Desktop

Decorative objects can surround the Hero.

``` text
Object      HERO CONTENT       Object

   Object                       Object

              Main content

   Object                       Object
```

## Tablet

Reduce the number of decorative elements.

``` text
       HERO CONTENT

    Object       Object
```

## Mobile

Prioritize content.

``` text
┌───────────────────────┐
│ MY DIARY          ☰   │
│                       │
│       MY DAILY        │
│        DIARY          │
│                       │
│   WRITE IT. KEEP IT.  │
│                       │
│ [ WRITE TODAY → ]     │
│                       │
│        📖             │
└───────────────────────┘
```

Use only 1--2 decorative objects on mobile.

Never allow decorative elements to cover the headline or CTA.

------------------------------------------------------------------------

# 21. Accessibility

Requirements: - Maintain sufficient text/background contrast - All
buttons must have accessible labels - Images require meaningful `alt`
text when they convey information - Decorative images should use empty
alt text - Keyboard navigation must work - Focus states must be
visible - Do not rely only on color to communicate mood or status -
Respect `prefers-reduced-motion`

------------------------------------------------------------------------

# 22. Front-end Implementation Direction

Recommended stack:

``` text
Next.js
React
TypeScript
Tailwind CSS
Lucide React
```

Optional:

``` text
Framer Motion
TipTap / Lexical
Zustand
React Hook Form
Zod
```

Suggested component structure:

``` text
src/
├── app/
│   ├── page.tsx
│   ├── diary/
│   ├── calendar/
│   ├── insights/
│   └── settings/
│
├── components/
│   ├── layout/
│   ├── diary/
│   ├── calendar/
│   ├── insights/
│   ├── ui/
│   └── decorations/
│
├── lib/
├── hooks/
├── types/
└── data/
```

------------------------------------------------------------------------

# 23. Design Do / Don't

## DO

-   Use large typography
-   Use strong black borders
-   Use a bright accent color
-   Mix digital UI with physical diary objects
-   Rotate decorative objects slightly
-   Use whitespace
-   Create asymmetrical compositions
-   Keep functional UI simple
-   Make the diary content the visual focus

## DON'T

-   Turn the website into a generic admin dashboard
-   Use too many gradients
-   Use excessive glassmorphism
-   Use too many colors
-   Rotate functional UI excessively
-   Cover important text with decorations
-   Use huge numbers of animations
-   Make every card look identical
-   Overcrowd the Hero section

------------------------------------------------------------------------

# 24. Overall Visual Direction

The final website should feel like:

``` text
Modern website
      +
Editorial magazine
      +
Physical notebook
      +
Scrapbook
      +
Playful Neo-Brutalism
```

The most important visual idea is:

> **A digital diary that looks like someone placed a real notebook,
> photographs, notes, and pens onto a modern website.**

The interface should be memorable without sacrificing usability.
