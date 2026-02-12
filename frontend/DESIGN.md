# Klabis Frontend Design System

## Přehled

Tento dokument popisuje nový design systém implementovaný v Klabis frontend aplikaci. Design se zaměřuje na výrazný, profesionální vzhled s podporou světlého a tmavého režimu.

## Designové Řízení: "Klabis Club Elegance"

### Light Mode - "Organic Clubhouse"
- **Teplá kremová paleta** s elegantními teplými podtóny
- **Sage green** (#2D6A4F) jako primární akcentní barva
- **Warm amber** (#D4915C) jako sekundární barva
- **Subtle borders** s jemným oddělením
- **Crisp whites** a teplé šedi pro high contrast

### Dark Mode - "Sophisticated Night"
- **Hluboké modré podklady** (#0F1419)
- **Electric emerald** (#10D89E) jako primární akcentní barva
- **Lavender purple** (#A78BFA) jako sekundární barva
- **Gradient backgrounds** pro moderní luxusní feel
- **Elevated surfaces** s jemnými border misto wire

## Fontové párování

### Display Fonts (Nadpisy)
**Space Grotesk** - Výrazné, geometrické bezpatkové písmo
- Pouití: H1, H2, H3, `.font-display` třída
- Váhy: 600, 700 (semibold, bold)
- Character: Moderní, tech-forward, profesionální

### Body Fonts (Text)
**Plus Jakarta Sans** - Refinované, humanistické bezpatkové písmo
- Pouití: Vęchný text, odstavce, UI komponenty
- Váhy: 400, 500, 600, 700 (regular, medium, semibold, bold)
- Character: itelné, přirodané, excellentní čitelnost

### Monospace Fonts
**JetBrains Mono** - Vývojářské monospace písmo
- Pouití: Kód, technické údaje, developer tools
- Character: Clean, readable, profesionální

## Barevný Systém

### CSS Variables (Semantic Naming)

Věchné barvy jsou definovány jako CSS variables pro jednoduchou perzistenci témat:

```css
/* Backgrounds */
--color-bg-base        /* Hlavní pozadí aplikace */
--color-bg-elevated   /* Karty, elevated surfaces */
--color-bg-subtle     /* Vnořené sekce, nested containers */

/* Text */
--color-text-primary    /* Hlavní text, nadpisy */
--color-text-secondary /* Sekundární text, popisky */
--color-text-tertiary  /* Subtle text, placeholdery */

/* Primary Action */
--color-primary         /* Sage green (light), Electric emerald (dark) */
--color-primary-hover
--color-primary-active
--color-primary-subtle

/* Secondary Action */
--color-secondary       /* Warm amber (light), Lavender (dark) */
--color-secondary-hover
--color-secondary-active
--color-secondary-subtle

/* Semantic Colors */
--color-success          /* Green success states */
--color-warning          /* Orange warning states */
--color-error            /* Red-pink error states */
--color-info             /* Teal info states */

/* Borders */
--color-border-subtle   /* Velmi jemné borders */
--color-border-default  /* Standard borders */
--color-border-strong    /* Silné borders/dividers */
```

### Kontrast a Čitelnost

**Light Mode:**
- High contrast (WCAG AAA compliant)
- 1A1915 text na FAFAFA pozadí
- Subtle borders (#E8E5E0)

**Dark Mode:**
- Enhanced contrast pro lepí čitelnost
- F8F7F4 text na 0F1419 pozadí
- Subtle rgba borders pro jemný odděl

## UI Komponenty

### Tlačítko (Buttons)
```tsx
// Primary - hlavní akce
<button className="btn-primary">Save</button>

// Secondary - sekundární akce
<button className="btn-secondary">Cancel</button>

// Danger - destruktivní akce
<button className="btn-danger">Delete</button>

// Ghost - subtletní akce
<button className="btn-ghost">Learn More</button>
```

**Vlastnosti:**
- Rounded corners (8-12px)
- Smooth transitions (150-200ms)
- Hover elevation changes
- Active scale micro-interactions (0.98)
- Focus rings pro accessibilitu

### Karty (Cards)
```tsx
// Standard card
<div className="card">Content</div>

// Hoverable (s micro-interakcí)
<div className="card card-hoverable">Clickable content</div>
```

**Vlastnosti:**
- Elevated background (--color-bg-elevated)
- Subtle border (--color-border-subtle)
- Smooth shadow transitions
- Hover: -translate-y-0.5, border strengthen

### Form Inputs
```tsx
<input className="form-input" placeholder="Enter text..." />
```

**Vlastnosti:**
- Border thickness increase na hover
- Primary ring na focus
- Error state (`.form-input.error`)
- Disabled opacity reduction

### Alerty
```tsx
<div className="alert alert-success">Success message</div>
<div className="alert alert-warning">Warning message</div>
<div className="alert alert-error">Error message</div>
<div className="alert alert-info">Info message</div>
```

**Vlastnosti:**
- Left border accent (4px)
- Semantic color backgrounds (subtle)
- Flex layout s gap

### Tabulky
```tsx
<div className="table-container">
  <table className="table">
    <thead>...</thead>
    <tbody>...</tbody>
  </table>
</div>
```

**Vlastnosti:**
- Rounded corners (xl)
- Subtle row dividers
- Hover row background
- Vertical typography layout

## Layout Struktura

### Header (AppBar)
- **Fixed positioning** (top-0, z-50)
- **Glass morphism** effect (backdrop-blur)
- **Logo**: Gradient background (primary → secondary)
- **User info**: Kliknutelné pro členy
- **Theme toggle**: Icon-based tlačítko
- **Responsive**: Mobile menu hamburger

### Sidebar (Navigace)
- **Fixed positioning** (left-0, h-[calc(100vh-4rem)])
- **Width**: 288px (w-72)
- **Elevated background** (--color-bg-elevated)
- **Active state**: Primary border-left (4px), primary tint background
- **Hover state**: Subtle background tint
- **Mobile**: Overlay drawer s backdrop-blur

### Main Content
- **Top padding**: 80px (lg:pl-80)
- **Max width**: None (full width available)
- **Overflow**: Auto s smooth scrolling

## Animace a Motion

### Page Load
```css
.animate-fade-in {
  animation: fade-in 200ms ease-out;
}
```

### Hover Micro-interakce
```css
group-hover:scale-110    /* Icon scale */
group-hover:opacity-20    /* Background opacity */
```

### Transition Durations
- **Fast**: 150ms (hover states, buttons)
- **Base**: 200ms (standard transitions)
- **Slow**: 300ms (layout changes, modals)

### Custom Keyframes
- `fade-in`: Opacity 0 → 1
- `slide-up`: TranslateY + fade
- `scale-in`: Scale 0.95 → 1 + fade
- `bounce-gentle`: Gentle Y bounce
- `shake`: Error feedback X shake

## Responsivní Design

### Breakpoints
- **sm**: 640px
- **md**: 768px
- **lg**: 1024px (sidebar persistent)
- **xl**: 1280px
- **2xl**: 1536px

### Mobile Adaptace
- **Sidebar**: Drawer overlay (z-30)
- **Header**: Compact layout, hidden text labels
- **Cards**: Full-width stacked
- **Typography**: Responsive font sizes

## Accessibilita

### WCAG Compliance
- **Contrast**: AAA (minimal 7:1 for normal text)
- **Keyboard Navigation**: Visible focus rings
- **Screen Reader**: ARIA labels na tlačítka
- **Touch Targets**: Minimum 44×44px

### Focus States
```css
focus:ring-2 focus:ring-primary focus:ring-offset-2
```

### ARIA Examples
```tsx
<button
  aria-label="Toggle theme"
  title="Light mode (click for dark)"
>
  <SunIcon />
</button>
```

## Performance

### Font Loading
- `display=swap` pro immediate text render
- Subset loading (Latin characters)
- Preload critical fonts

### CSS Optimization
- CSS variables pro theming (no duplicate styles)
- Component layer organization
- Minimal specificity (.card, not div.card.card)

### Bundle Size
- Post-gzip: ~160KB total
- Code splitting implemented
- Lazy loading routes

## File Structure

```
frontend/
├── src/
│   ├── index.css              # Design system variables
│   ├── tailwind.config.ts     # Tailwind theme mapping
│   ├── theme/
│   │   └── ThemeContext.tsx  # Theme provider + hook
│   ├── components/
│   │   ├── ThemeToggle/    # Theme switcher component
│   │   └── UI/             # Reusable UI components
│   └── pages/
│       ├── Layout.tsx        # Main layout (header + sidebar)
│       └── HomePage.tsx      # Dashboard with cards
```

## Pouití

### Theme Switching
```tsx
import {useTheme} from '@/theme/ThemeContext'

const {theme, effectiveTheme, toggleTheme, isDark} = useTheme()
```

### Custom Colors
```tsx
<div className="bg-bg-elevated text-text-primary">
  Custom themed content
</div>
```

### Gradient Text
```tsx
<h1 className="text-gradient-primary">
  Gradient Heading
</h1>
```

### Glass Effect
```tsx
<div className="glass">
  Glass morphism container
</div>
```

## Future Enhancements

1. **Animation Library**: Framer Motion pro komplexší přechody
2. **Custom Properties**: scroll-driven animations
3. **Theme Customization**: User-defined color schemes
4. **Micro-interactions**: Particle effects na hover
5. **Voice Commands**: Accessibility enhancement

---

**Vytvořeno**: 2026-02-12
**Verze**: 1.0.0
**Autor**: Claude (frontend-design skill)
