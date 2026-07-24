## Using @klabis/design-system

No provider or root wrapper is required — components read all styling from Tailwind utility classes and CSS custom properties, not React context. Light/dark theme is a `dark` class on `<html>` (or any ancestor); components have no theme prop of their own.

### Styling idiom: Tailwind preset with semantic tokens

Style layout and spacing with the DS's own Tailwind classes — these resolve to CSS variables, not hardcoded colors, so they stay on-brand and theme-aware automatically:

| Concern | Classes |
|---|---|
| Background | `bg-bg-base`, `bg-surface`, `bg-surface-raised` |
| Text | `text-text-primary`, `text-text-secondary`, `text-text-tertiary` |
| Border | `border-border`, `border-border-light` |
| Brand | `bg-primary` / `hover:bg-primary-light`, `bg-secondary`, `bg-accent` |
| Status | `bg-success`/`text-success`, `bg-warning`, `bg-error`, `bg-info` — usually combined with a `/20` opacity modifier for tinted badges (`bg-success/20 text-success`) |
| Spacing | `p-xs` … `p-4xl` (2px → 48px scale), same suffixes work for `gap-*`, `m-*` |
| Radius | `rounded-sm`, `rounded-md`/`rounded-base`, `rounded-lg`, `rounded-xl`, `rounded-pill` |
| Shadow | `shadow-sm`, `shadow-md`, `shadow-lg` |
| Fonts | `font-sans` (Plus Jakarta Sans, body), `font-display` (Space Grotesk, headings), `font-mono` (JetBrains Mono) |
| Motion | `animate-fade-in`, `animate-slide-up`, `animate-scale-in`, `animate-shimmer` (skeletons) |

Never invent new color names — every color a design needs should map to one of the semantic tokens above (`primary`/`secondary`/`accent`/`success`/`warning`/`error`/`info`), not a raw Tailwind palette color (`bg-blue-500`), so it stays consistent with the DS across light and dark theme.

### Where the truth lives

- `styles.css` (and its `@import` closure, including the compiled component CSS) is the canonical source for every class above — read it before styling if a class's exact value matters.
- Each component's own `.d.ts` and `.prompt.md` under `components/<group>/<Name>/` document its props and usage precisely — prefer those over guessing from the name.

### Example

```tsx
import { Card, Button, Badge } from '@klabis/design-system';

<Card className="p-lg">
  <div className="flex items-center justify-between mb-md">
    <h3 className="font-display text-text-primary">Jan Novák</h3>
    <Badge variant="success">Aktivní</Badge>
  </div>
  <p className="text-text-secondary text-sm mb-lg">ZBM1234 · SK Žabovřesky</p>
  <Button variant="primary">Uložit člena</Button>
</Card>
```
