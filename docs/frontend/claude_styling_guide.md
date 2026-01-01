🎨 Klabis Design System - Memory Summary

Design Philosophy

- Aesthetic: Modern Minimal Intentionality
- Purpose: Admin/data-management tool for daily users
- Tone: Professional, trustworthy, intelligent
- Core: Every element serves a purpose; refined minimalism

  ---

Color System

Base Neutrals:

- #0f0f0f - Primary background
- #1a1a1a - Secondary background
- #242424 - Surface/cards (bg-surface-raised)
- #161616 - Form inputs (bg-surface-base)
- #2a2a2a - Borders (border-default)
- #3a3a3a - Light borders (border-light)

Text Colors:

- #f5f5f5 - Primary text (text-text-primary)
- #a0a0a0 - Secondary text (text-text-secondary)
- #707070 - Tertiary text (text-text-tertiary)

Primary Action (Blue):

- #3b82f6 - Primary
- #60a5fa - Primary light (hover)
- #1e40af - Primary dark (active)

Semantic Colors:

- #10b981 - Success (green)
- #f59e0b - Warning (amber)
- #ef4444 - Error (red)
- #06b6d4 - Info (cyan)
- #8b5cf6 - Accent/Focus (purple)

Alert Backgrounds:

- Success: #064e3b bg, #d1fae5 text
- Warning: #78350f bg, #fef3c7 text
- Error: #7f1d1d bg, #fee2e2 text
- Info: #164e63 bg, #cffafe text

  ---

Typography

Fonts:

- Display: Sohne (bold, distinctive headers)
- Body: Geist (readable, modern)
- Code: Fira Code (monospace)

Hierarchy:

- H1: 30px, bold, font-display
- H2: 24px, bold, font-display
- H3: 20px, bold, font-display
- H4: 18px, semibold
- Body: 16px, normal
- Small: 14px, normal
- Labels: 12px, semibold, uppercase, 0.5px tracking

  ---

Spacing Scale (4px base)

- 2px (xs) - Tiny gaps
- 4px (sm) - Component spacing
- 8px (md) - Padding, margins
- 12px (lg) - Component margins
- 16px (xl) - Section spacing
- 24px (2xl) - Card gaps
- 32px (3xl) - Section margins
- 48px (4xl) - Major breaks

Common Patterns:

- Button: px-4 py-2.5 (16×10px)
- Card: p-6 (24px)
- Table cell: px-4 py-3 (16×12px)
- Form label: uppercase, text-xs, font-semibold, tracking-wider

  ---

Component Standards

Buttons:

- Primary: Blue bg, white text, shadow-sm, lift on hover (-1px)
- Secondary: Transparent, border, text-primary
- Ghost: Transparent, text-primary
- Danger: Red bg, white text
- Focus ring: Purple (#8b5cf6), 2px, no offset
- States: Default → Hover (lift) → Active (scale 0.95)

Cards:

- BG: surface-raised (#242424)
- Border: 1px border-border (#2a2a2a)
- Padding: 24px
- Radius: 8px (rounded-md)
- Hover: shadow-md, border-border-light, smooth 200ms

Forms:

- Input BG: surface-base (#161616)
- Input Border: 1px border-border
- Radius: 6px (rounded-md)
- Focus: border-primary, ring-2 ring-primary ring-opacity-20
- Error: border-feedback-error
- Label: uppercase, xs, semibold, text-text-secondary

Alerts:

- Pattern: 4px left border + semantic background + text
- All use left border accent + subtle background
- Animation: fade-in 200ms on display

Tables:

- Header BG: surface-base (#242424)
- Header Border: border-b-2 border-border
- Rows BG: dark (#1a1a1a)
- Row Hover: bg-surface-base + border-l-4 border-l-primary
- Padding: px-4 py-3

Modals:

- Backdrop: black, 60% opacity
- Content: surface-raised, shadow-lg, rounded-md
- Header: surface-base background
- Animation: scale-in (0.95→1, opacity fade, 300ms)

  ---

Animations

Timings:

- Fast (150ms): Button clicks, quick state changes
- Standard (200ms): Color transitions, form focus
- Slow (300ms): Modal opens, page transitions

Easing: cubic-bezier(0.4, 0, 0.2, 1) for all

Common Patterns:

- Button hover: translateY(-1px), shadow elevation, 150ms
- Button active: scale(0.95), immediate
- Form focus: scale-y(1.05), ring effect, 200ms
- Toast entry: slide-up + fade-in, 300ms
- Modal entry: scale-in (0.95→1) + fade-in, 300ms

  ---

Accessibility Requirements

Focus States:

- All interactive elements: ring-2 ring-accent (purple)
- No ring offset (ring-offset-0)
- Visible at all times

Color Contrast:

- Minimum 4.5:1 (WCAG AA)
- Never use color alone for meaning
- Semantic colors always have sufficient contrast

Keyboard Navigation:

- Tab order follows visual flow
- Space/Enter activates buttons
- Escape closes modals
- Arrow keys for dropdowns

Semantic HTML:

- Use &lt;button>, &lt;input&gt;, &lt;table&gt;, &lt;nav&gt;, &lt;header&gt;, &lt;main&gt;, &lt;footer&gt;
- Proper heading hierarchy
- Alt text for images
- ARIA labels where needed

  ---

Best Practices Checklist

✅ DO:

- Use semantic color names (primary, surface-base, border)
- Stick to 4px spacing scale
- Apply transitions to all state changes (150ms/200ms)
- Use Tailwind utilities, avoid custom CSS
- Include focus rings on all interactive elements
- Test keyboard navigation
- Use semantic HTML
- Test dark mode

❌ DON'T:

- Hardcode hex values in components
- Mix Tailwind with inline styles
- Remove focus states
- Animate too many things at once
- Break spacing scale with arbitrary values
- Use generic fonts (always use Sohne/Geist/Fira Code)
- Use color alone to convey meaning
- Create custom animations without config

  ---

Component Library (Available)

✅ Button (variants: primary, secondary, danger, ghost)
✅ Card (with shadow options)
✅ Alert (types: success, warning, error, info)
✅ Modal (sizes: sm, md, lg, xl)
✅ TextField, TextAreaField, SelectField
✅ CheckboxField, RadioGroup, SwitchField
✅ Badge, Toast, Spinner, Skeleton
✅ Tables with sorting/pagination
✅ Forms with Formik + Yup validation

  ---
File Locations

- Design Guide: /frontend-2/DESIGN_SYSTEM.md
- HTML Preview: /frontend-2/design-preview.html
- Tailwind Config: /frontend-2/tailwind.config.ts
- Global Styles: /frontend-2/src/index.css
- Components: /frontend-2/src/components/UI/

  ---

Key Decisions & Why

1. Blue Primary (#3b82f6) instead of red: Signals trust, reliability, calm for admin interface
2. Sohne + Geist fonts: Distinctive, modern, professional - not generic defaults
3. Left border accents: Alerts and hover states use 4px left border for visual impact
4. Fast animations (150ms): Snappy feedback, not sluggish
5. Subtle shadows: Depth without heaviness
6. Uppercase labels: Clear visual hierarchy, easier to scan
7. 4px spacing scale: Harmony, predictability, easier math

  ---
Status: ✅ Design system fully documented and implemented across all components
Last Updated: December 2025
Version: 1.0
Total Components Refactored: 13+ core components + design tokens
