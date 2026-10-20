---
name: mylog-mobile-design
description: Design or revise MyLog screens, components, and visual tokens in the Expo React Native app using the web design specification as the visual source. Use for mobile UI and interaction work, not for backend rules or Next.js implementation.
---

# MyLog mobile design

Create a digital diary that feels like a physical notebook: **playful Neo-Brutalism, editorial typography, and restrained scrapbook details**. Read the complete [web design specification](../../../my-app-web/design.md) when starting a new screen or changing the visual system. It is a design reference, not evidence that every illustrated feature exists. Check the current web code and backend contract before describing behavior as implemented.

Read [mobile AGENTS.md](../../AGENTS.md) and [STRUCTURE.md](../../STRUCTURE.md) before editing code. Use the exact Expo SDK 57 documentation required there for Expo APIs. Build with React Native and Expo Router; translate the web's appearance and interaction intent rather than copying DOM, CSS, hover behavior, or Tiptap components.

## Visual foundation

The web specification defines these light-theme anchors. Start mobile tokens from them and adjust a value only for a concrete contrast or native usability reason. Put repeated values in `src/constants/`, and document any final dark palette separately.

| Token | Reference | Mobile use |
| --- | --- | --- |
| Canvas | `#F5F5F3` | Quiet page background |
| Surface | `#FFFFFF` | Cards and controls |
| Ink / border | `#111111` | Main text and strong outlines |
| Accent | `#B7FF32` | Primary action and small highlights |
| Paper | `#F3EBDD` | Journal surfaces and paper details |
| Muted | `#D9D7D2` | Secondary surfaces |

- Lead with bold editorial headings and a clear type hierarchy. The web's 72–120 px hero sizes are desktop examples; scale headings to the phone, available text space, and system font setting. Body and journal text must remain comfortable for long Vietnamese passages. A serif face can distinguish diary content; keep controls highly legible.
- Use strong dark borders, rounded corners, and small **hard offset shadows** for important buttons or paper cards. The web reference uses roughly 2 px borders, 4–6 px offset shadows, 12–16 px button radii, and 12–20 px card radii. Translate these to React Native density-independent units and tune on device; avoid giving every element the same heavy treatment.
- The main action should be easy to find: bold label, lime fill when appropriate, dark border, and clear pressed/disabled/loading states. Use consistent vector icons and stroke weight. Do not use OS emoji as mood controls or interface icons; their appearance changes by platform.
- Place notebook, pen, note, bookmark, or polaroid motifs as separate decorative elements. Subtle rotation or overlap can make a screen feel assembled by hand. On a phone, use at most one or two such objects in a hero and keep them away from text, controls, and safe areas. Rotate decoration, not functional content.
- Use whitespace and occasional asymmetry without weakening reading order. Avoid generic dashboard styling, excessive gradients, glass effects, motion, or dense clusters of cards.

## Apply the system by screen

- **Welcome and auth:** one clear message and one primary action. Let a single paper or notebook detail establish the visual identity; keep forms straightforward and error text visible.
- **Journal list:** show date, title or short preview, and relevant mood/tag metadata in readable paper cards. Vary decorative treatment sparingly; preserve a stable tap target and accessible reading order.
- **Editor and entry detail:** the writing surface is the focus. Provide generous margins, legible line height, keyboard-safe controls, and an unambiguous save or draft state. Keep decoration outside editable content. Show conflict and retry states without losing the user's text.
- **Calendar:** emphasize memories on dates rather than productivity metrics. Pair any mood color or symbol with a text or accessible label. Use tap to reveal entry previews; the web's hover preview does not transfer to native.
- **Insights and reports:** use editorial headings and concise cards or charts. Display only values returned by the backend; do not invent sample counts, analysis, or AI reflections to fill a layout.

The web spec includes photos, music, location, auto-save, and rich editing as design ideas. Add their controls only when the mobile implementation and backend contract support the action. MyLog is for journaling and self-reflection; UI copy must not imply diagnosis or treatment.

## Interaction and review

Use Expo Router navigation, safe-area insets, keyboard handling, and sufficiently large touch targets. Replace web hover with pressed, selected, and focus states. Keep motion subtle, respect reduced-motion settings, and avoid animations that distract from writing. Give controls accessible names; hide purely decorative art from screen readers. Do not communicate mood, safety, or error solely through color.

Before finishing, inspect the screen on a small and a large phone, check long Vietnamese text and increased font size, keyboard overlap, dark appearance if supported, loading/empty/error/success states, and Android/iOS behavior where available. Use synthetic journal content in previews and screenshots. Report what was actually checked and what remains unverified.
