# Klabis Frontend Design System

A modern minimal intentionality design system for the Klabis member management application. This guide ensures
consistency and quality across all UI components.

## Table of Contents

1. [Design Philosophy](#design-philosophy)
2. [Color Palette](#color-palette)
3. [Typography](#typography)
4. [Spacing & Layout](#spacing--layout)
5. [Components](#components)
6. [Micro-Interactions](#micro-interactions)
7. [Accessibility](#accessibility)
8. [Best Practices](#best-practices)

---

## Design Philosophy

### Core Values

- **Refined Minimalism**: Every element serves a purpose. No decorative clutter.
- **Professional Trustworthiness**: Blue primary color signals reliability and security.
- **Intentional Restraint**: Bold, distinctive choices in typography and color, conservative in ornamentation.
- **Clarity First**: Clear visual hierarchy guides user attention naturally.

### Aesthetic Direction

This is an admin/data-management interface built for daily users who care about craft. The design reflects:

- Modern, clean aesthetic
- Sophisticated neutral palette
- Subtle depth through layering
- Smooth micro-interactions that don't distract
- Professional typography choices

---

## Color Palette

### Base Neutrals

```
Primary Background: #0f0f0f (Pure dark)
Secondary Background: #1a1a1a (Slight elevation)
Surface/Cards: #242424 (Elevated surfaces)
Text Primary: #f5f5f5 (Main text, off-white)
Text Secondary: #a0a0a0 (Secondary text)
Text Tertiary: #707070 (Hints and labels)
Border Default: #2a2a2a (Subtle borders)
Border Light: #3a3a3a (Lighter borders on hover)
```

### Primary Action Color

```
Primary: #3b82f6 (Blue - trustworthy, calm)
Primary Light: #60a5fa (Hover state)
Primary Dark: #1e40af (Active state)
```

### Semantic Colors

```
Success: #10b981 (Emerald green)
Warning: #f59e0b (Amber)
Error: #ef4444 (Bright red)
Info: #06b6d4 (Cyan)
Focus/Accent: #8b5cf6 (Purple - only for focus rings)
Secondary Action: #6366f1 (Indigo)
```

### Alert Backgrounds (Dark Mode)

```
Success Alert BG: #064e3b
Success Alert Text: #d1fae5

Warning Alert BG: #78350f
Warning Alert Text: #fef3c7

Error Alert BG: #7f1d1d
Error Alert Text: #fee2e2

Info Alert BG: #164e63
Info Alert Text: #cffafe
```

### Usage Guidelines

- **Primary color** (#3b82f6): Main call-to-action buttons, links, focus states
- **Success/Warning/Error**: Only for semantic feedback. Never use for other purposes
- **Text colors**: Always use semantic naming (primary, secondary, tertiary) not gray values
- **Borders**: Use subtle colors (#2a2a2a). Avoid pure gray or black borders
- **Backgrounds**: Use surface tokens (surface-base, surface-raised) not hard-coded colors

---

## Typography

### Font Stack

```
Display Font: Sohne (bold, distinctive headings)
Body Font: Geist (excellent readability, modern)
Code Font: Fira Code (monospace, technical content)

Fallback: system-ui, sans-serif
```

### Font Sizes & Weights

#### Headings

```
H1: 30px (text-3xl), font-bold, font-display
H2: 24px (text-2xl), font-bold, font-display
H3: 20px (text-xl), font-bold, font-display
H4: 18px (text-lg), font-semibold
H5: 16px (text-base), font-semibold
H6: 14px (text-sm), font-semibold, uppercase, tracking-wider
```

#### Body Text

```
Large: 16px (text-base), font-normal
Regular: 14px (text-sm), font-normal
Small: 12px (text-xs), font-normal
```

#### Labels & Metadata

```
Label: 12px (text-xs), font-semibold, uppercase, tracking-wider, text-text-secondary
Helper Text: 12px (text-xs), font-normal, text-text-tertiary
```

### Best Practices

- Use **display font (Sohne)** for: Page titles, section headers, key data labels
- Use **body font (Geist)** for: Body copy, descriptions, UI labels, form fields
- Use **code font** for: Code blocks, error messages with technical details, variable names
- **Never mix fonts arbitrarily** - each font choice should be intentional
- **Line height**: 1.5 for body text, 1.2 for headings
- **Letter spacing**: 0.5px for uppercase labels, 0px for body text

---

## Spacing & Layout

### Spacing Scale

All spacing uses a consistent 4px base unit:

```
2px   (xs)
4px   (sm)
8px   (md)
12px  (lg)
16px  (xl)
24px  (2xl)
32px  (3xl)
48px  (4xl)
```

### Usage

- **xs (2px)**: Border widths, tiny gaps between inline elements
- **sm (4px)**: Internal component spacing (icon gaps, small padding)
- **md (8px)**: Component padding, small margins
- **lg (12px)**: Component margins, moderate spacing
- **xl (16px)**: Section spacing, form field margins
- **2xl (24px)**: Card/container gaps, major divisions
- **3xl (32px)**: Section margins, component spacing
- **4xl (48px)**: Major section breaks, page-level spacing

### Common Patterns

```
Button Padding: px-4 py-2.5 (16px horizontal, 10px vertical)
Card Padding: p-6 (24px all sides)
Form Field Padding: px-4 py-2.5 (16px horizontal, 10px vertical)
Modal Padding: p-6 (24px)
Table Cell Padding: px-4 py-3 (16px horizontal, 12px vertical)
Section Gap: gap-4 (16px spacing)
```

### Container Rules

- **Min padding**: 16px on sides (mobile), 24px (desktop)
- **Max width**: Depends on content type, but maintain readability
- **Breathing room**: Never crowd elements. More space = more sophisticated

---

## Components

### Buttons

#### Variants

```
Primary: bg-primary, white text, shadow-sm
  Hover: bg-primary-light, shadow-md, translateY(-1px)
  Active: scale-95, shadow-none
  Disabled: opacity-50, no shadow

Secondary: bg-surface, border border-border, text-primary
  Hover: border-border-light, bg-surface-raised
  Active: bg-surface
  Disabled: opacity-50

Ghost: bg-transparent, text-text-primary
  Hover: bg-surface-base
  Active: bg-surface-raised
  Disabled: opacity-50

Danger: bg-feedback-error, white text, shadow-sm
  Hover: bg-red-500, shadow-md, translateY(-1px)
  Active: scale-95
  Disabled: opacity-50
```

#### States

- **Default**: Base styling, ready for interaction
- **Hover**: Lift effect (translateY -1px), shadow elevation
- **Active**: Scale down (0.95), immediate visual feedback
- **Focus**: Purple focus ring (ring-2 ring-accent ring-offset-0)
- **Disabled**: 50% opacity, no hover/active effects
- **Loading**: Spinner inside, disabled interactions

#### Size Guide

```
Small: px-3 py-1.5, text-sm
Medium: px-4 py-2.5, text-base (default)
Large: px-6 py-3, text-lg
```

### Cards

```
Background: bg-surface-raised
Border: 1px border-border
Padding: p-6 (24px)
Border Radius: rounded-md (8px)

Hover State:
  Shadow: shadow-md (from shadow-sm)
  Border: border-border-light (lightens slightly)
  Transition: 200ms all
```

### Form Fields

#### Text Input

```
Background: bg-surface-base (#161616)
Border: 1px border-border (#2a2a2a)
Padding: px-4 py-2.5
Border Radius: rounded-md (6px)
Text Color: text-text-primary

Focus State:
  Border: border-primary (#3b82f6)
  Ring: ring-2 ring-primary ring-opacity-20
  Scale: scaleY(1.05)
  Transition: 200ms all

Error State:
  Border: border-feedback-error (#ef4444)
  Ring: ring-feedback-error ring-opacity-20
  Text Color (error message): text-feedback-error
```

#### Labels

```
Typography: text-xs, font-semibold, uppercase, tracking-wider
Color: text-text-secondary
Margin Bottom: mb-2
Required Indicator: text-feedback-error
```

#### Select/Dropdown

Use same styling as text input. Follows form-input base styles.

### Alerts

#### Styling Pattern

All alerts use: **left border accent (4px) + semantic background + semantic text**

```
Success:
  Background: #064e3b
  Border: 4px solid #10b981 (left), 1px solid #10b981 (other sides)
  Text: #d1fae5

Warning:
  Background: #78350f
  Border: 4px solid #f59e0b (left), 1px solid #f59e0b (other sides)
  Text: #fef3c7

Error:
  Background: #7f1d1d
  Border: 4px solid #ef4444 (left), 1px solid #ef4444 (other sides)
  Text: #fee2e2

Info:
  Background: #164e63
  Border: 4px solid #06b6d4 (left), 1px solid #06b6d4 (other sides)
  Text: #cffafe

Animation: fade-in 200ms on display
```

### Modals

```
Backdrop: bg-black bg-opacity-60
Modal Background: bg-surface-raised
Border Radius: rounded-md (8px)
Shadow: shadow-lg
Header Background: bg-surface-base with border-b
Padding: p-6 (24px)
Close Button: text-text-secondary hover:text-text-primary

Entry Animation: animate-scale-in (scale 0.95 → 1, opacity 0 → 1, 300ms)
```

### Tables

```
Header:
  Background: bg-surface-base
  Border: border-b-2 border-border
  Text: uppercase, tracking-wider, font-semibold
  Padding: px-4 py-3

Rows:
  Background: bg-dark
  Border: divide-y divide-border
  Padding: px-4 py-3
  Text Color: text-text-primary

Row Hover:
  Background: bg-surface-base
  Border: border-l-4 border-l-primary (left accent)
  Transition: all 150ms

Pagination:
  Background: bg-surface-base
  Border: border-t border-border
  Select: form-input styling
```

### Badges

```
Primary:
  Background: bg-primary/20
  Text: text-primary-light

Success:
  Background: bg-feedback-success/20
  Text: text-feedback-success

Warning:
  Background: bg-feedback-warning/20
  Text: text-feedback-warning

Error:
  Background: bg-feedback-error/20
  Text: text-feedback-error
```

---

## Micro-Interactions

### Animation Timings

```
Fast (150ms): Button clicks, quick state changes
Standard (200ms): Color transitions, form focus
Slow (300ms): Modal opens, page transitions, complex animations
```

### Easing Function

Use `cubic-bezier(0.4, 0, 0.2, 1)` for all smooth transitions (Tailwind's default ease).

### Common Animations

#### Button Press

```
Default → Hover: translateY(-1px), shadow-sm → shadow-md (150ms)
Hover → Active: scale(0.95), shadow removed (immediate)
Active → Released: Reset to default (immediate)
```

#### Form Field Focus

```
Unfocused: normal appearance, opacity 1
Focus: scale-y(1.05), border-primary, ring-primary (200ms)
Error: shake animation on validation fail, border-feedback-error
Success: subtle checkmark appears (fade-in 200ms)
```

#### Modal Entry

```
Start: transform scale(0.95) opacity(0)
End: transform scale(1) opacity(1)
Duration: 300ms cubic-bezier(0.4, 0, 0.2, 1)
Backdrop fade-in: simultaneous with modal
```

#### Toast Notification

```
Entry: slide-up from bottom (translateY 1rem → 0)
Opacity: 0 → 1
Duration: 300ms cubic-bezier(0.4, 0, 0.2, 1)
Exit: fade-out, optional slide-down
```

#### Row Hover (Tables)

```
Start: no background, transparent left border
Hover: bg-surface-base, border-l-4 border-l-primary
Duration: 150ms all
```

### When NOT to Animate

- Loading states (use spinners, not animations)
- Navigation between pages (fade-in is enough)
- Error states (shake if dramatic, but prefer static styling)
- Multiple simultaneous animations (focus on one impact moment)

---

## Accessibility

### Color Contrast

- **Text on background**: Minimum 4.5:1 ratio (WCAG AA)
- **Large text** (18px+): Minimum 3:1 ratio
- **Disabled states**: Aim for 3:1 even though interaction is disabled
- **Always test** contrast with tools like WebAIM

### Focus States

```
Default: 2px ring-accent (purple #8b5cf6)
Ring offset: 0 (no white space)
Applied to: Buttons, links, form fields, interactive rows
Never remove default browser focus indicators
```

### Keyboard Navigation

- All interactive elements must be keyboard accessible
- Tab order must follow visual flow (left to right, top to bottom)
- Buttons and links: Space/Enter to activate
- Form fields: Tab to navigate, arrow keys for selects
- Modals: Trap focus within modal, escape to close

### Semantic HTML

```
✓ Use <button> for buttons (not <div>)
✓ Use <input> for form fields (not <div>)
✓ Use <table> for tabular data (not <div>)
✓ Use <nav> for navigation
✓ Use <header>, <main>, <footer> for page structure
✓ Use proper heading hierarchy (h1 → h2 → h3)
✓ Include alt text for images
✓ Use aria-labels where text labels aren't available
```

### Text Requirements

- **Font size minimum**: 12px for helper text, 14px+ for body
- **Line height**: 1.5 for body text (for readability)
- **Not color alone**: Don't convey information through color alone
- **Error messages**: Clear, actionable, adjacent to form field

---

## Best Practices

### DO

✅ Use semantic color names (primary, surface-base, border) in code
✅ Maintain consistent spacing using the scale (2, 4, 8, 12, 16, 24, 32, 48px)
✅ Apply transitions to all state changes (hover, focus, active)
✅ Test components in both desktop and mobile views
✅ Use Tailwind utility classes, avoid custom CSS when possible
✅ Follow component patterns from existing components
✅ Test keyboard navigation for all interactive elements
✅ Use semantic HTML elements
✅ Apply focus rings to all interactive elements
✅ Keep animations under 300ms (except special cases)

### DON'T

❌ Use hardcoded hex values in components (#f44336 etc.)
❌ Mix Tailwind utilities with inline styles
❌ Create custom animations without adding to theme config
❌ Remove focus states or focus rings
❌ Use color alone to convey meaning
❌ Animate too many things simultaneously
❌ Break the spacing scale with arbitrary values
❌ Use generic font stack (always use Sohne/Geist/Fira Code)
❌ Create new button/card variants without design review
❌ Ignore dark mode (test all new components in dark mode)

### Component Checklist

Before creating a new component:

- [ ] Design matches this guide (colors, spacing, typography)
- [ ] Component is accessible (keyboard nav, focus states, semantic HTML)
- [ ] Mobile responsive (test on 320px, 768px, 1200px)
- [ ] Dark mode compatible
- [ ] Proper type definitions (TypeScript)
- [ ] Props are documented
- [ ] States are clearly visible (default, hover, active, disabled, error)
- [ ] Loading states handled appropriately
- [ ] Error states are clear and actionable
- [ ] Transitions are smooth and purposeful
- [ ] No hardcoded colors or spacing values
- [ ] Matches existing component patterns

---

## Typography Scale Reference

For quick reference when sizing text:

```
Component/Usage                 Size          Weight
=========================================================
Page Title (H1)                 30px (3xl)    Bold
Section Title (H2)              24px (2xl)    Bold
Subsection (H3)                 20px (xl)     Bold
Component Title (H4)            18px (lg)     Semibold
Label/Caption (H6)              14px (sm)     Semibold + uppercase
Body Text (default)             16px (base)   Normal
Secondary Text                  14px (sm)     Normal
Small Text / Helper             12px (xs)     Normal
Input Label                      12px (xs)     Semibold + uppercase
Error Message                    14px (sm)     Normal
Badge / Tag                      12px (xs)     Medium
Button Text                      14-16px       Medium
Table Header                     14px (sm)     Semibold + uppercase
Table Cell                       14px (sm)     Normal
```

---

## Component Library

### Core Components (Ready to Use)

- Button (variants: primary, secondary, danger, ghost)
- Card (with optional shadow)
- Alert (types: success, warning, error, info)
- Modal (sizes: sm, md, lg, xl)
- TextField (text, email, password, number, date, etc.)
- TextAreaField
- SelectField
- CheckboxField
- RadioGroup
- SwitchField
- Badge (types: primary, success, warning, error)
- Toast (types: success, warning, error, info)
- Spinner (loading indicator)
- Skeleton (loading placeholder)
- Table (with sorting and pagination)
- Pagination (custom controls)

### Creating New Components

When creating a new component:

1. **Follow existing patterns** - Study similar components first
2. **Use semantic colors** - Never hardcode hex values
3. **Use spacing scale** - Never use arbitrary margins/padding
4. **Add transitions** - All interactive elements need smooth feedback
5. **Test accessibility** - Keyboard nav, focus rings, contrast
6. **Document props** - TypeScript interfaces with clear descriptions
7. **Show all states** - Create stories for default, hover, active, disabled, loading, error states

---

## Common Patterns

### Form Layout

```
<div className="space-y-6">
  <TextField label="Field Name" placeholder="..." />
  <SelectField label="Option" options={[...]} />
  <div className="flex gap-3">
    <Button variant="primary">Submit</Button>
    <Button variant="ghost">Cancel</Button>
  </div>
</div>
```

### Card with Action

```
<Card hoverable className="p-6">
  <h3 className="text-xl font-semibold mb-2">Card Title</h3>
  <p className="text-text-secondary mb-4">Card description</p>
  <Button variant="secondary" size="sm">Action</Button>
</Card>
```

### Alert with Close

```
<Alert severity="error" onClose={() => setVisible(false)}>
  Something went wrong. Please try again.
</Alert>
```

### Table with Action

```
<KlabisTable fetchData={fetchMembers} onRowClick={handleRowClick}>
  <Column header="Name" accessor="name" />
  <Column header="Email" accessor="email" />
  <Column header="Status" accessor="status" render={renderStatus} />
</KlabisTable>
```

---

## Resources

- **Figma Design File**: (link to design file)
- **Component Library**: Check `/src/components/UI/` for implementations
- **Tailwind Config**: Check `tailwind.config.ts` for theme values
- **Global Styles**: Check `src/index.css` for base styles

---

**Last Updated**: December 2025
**Version**: 1.0
**Maintainer**: Design Team
