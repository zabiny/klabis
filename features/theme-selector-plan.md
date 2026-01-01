# Theme Selector Implementation Plan

## Overview

Add a theme selector icon button to the top bar that cycles through Light → Dark → System modes, with system preference
detection and localStorage persistence.

## User Requirements

- Icon button toggle (sun/moon/computer icons) between username and "Odhlásit" button
- Support 3 modes: Light, Dark, System (auto-detect OS preference)
- Follow TDD approach (tests before implementation)

## Implementation Steps

### Phase 1: Icon Components (No tests needed - simple wrappers)

**Create 3 new icon files** following the LogoutIcon.tsx pattern:

1. `frontend-2/src/components/Icons/SunIcon.tsx`
    - Import `SunIcon` from `@heroicons/react/24/outline`
    - Wrapper with IconProps interface (size, className, title)

2. `frontend-2/src/components/Icons/MoonIcon.tsx`
    - Import `MoonIcon` from `@heroicons/react/24/outline`
    - Same pattern as SunIcon

3. `frontend-2/src/components/Icons/ComputerDesktopIcon.tsx`
    - Import `ComputerDesktopIcon` from `@heroicons/react/24/outline`
    - Same pattern as SunIcon

4. Update `frontend-2/src/components/Icons/index.ts`
    - Export all three new icons

### Phase 2: Extend ThemeContext (TDD)

**Step 1: Write tests** - `frontend-2/src/theme/ThemeContext.test.tsx`

Test cases:

- Initialization: loads from localStorage, defaults to system preference
- Mode switching: setTheme('light'/'dark'/'system')
- Toggle cycling: light → dark → system → light
- System preference detection when theme='system'
- System preference change listener
- effectiveTheme returns 'light' or 'dark' (never 'system')
- localStorage persistence
- useTheme() throws error outside provider

**Step 2: Implement ThemeContext changes** - Modify `frontend-2/src/theme/ThemeContext.tsx`

Changes needed:

- Type: Change `Theme` from `'light' | 'dark'` to `'light' | 'dark' | 'system'`
- Interface: Add `effectiveTheme: 'light' | 'dark'` to ThemeContextType
- State: Add `effectiveTheme` state variable
- Helper: Create `getEffectiveTheme(theme: Theme)` to resolve 'system' to actual theme
- Listener: Add useEffect to listen for system preference changes when theme='system'
- Update: Modify `toggleTheme()` to cycle through 3 states
- Update: Modify `applyTheme()` to resolve system preference before applying class

**Step 3: Run tests** - Verify all ThemeContext tests pass

### Phase 3: ThemeToggle Component (TDD)

**Step 4: Write tests** - `frontend-2/src/components/ThemeToggle/ThemeToggle.test.tsx`

Test cases:

- Renders icon button
- Shows SunIcon when theme='light'
- Shows MoonIcon when theme='dark'
- Shows ComputerDesktopIcon when theme='system'
- Clicking calls toggleTheme()
- Has correct ARIA label for each mode
- Has title attribute
- Keyboard navigation (Enter/Space)
- Has focus ring styling
- Has hover state styling

**Step 5: Implement ThemeToggle** - Create `frontend-2/src/components/ThemeToggle/ThemeToggle.tsx`

Component structure:

- Import useTheme hook
- Import all three icon components
- Icon selection logic based on current theme
- Click handler: call toggleTheme()
- Accessibility: dynamic ARIA label, title attribute
- Styling: Match user name button pattern from Layout.tsx:
    - `px-3 py-2 text-sm`
    - `text-text-secondary hover:text-text-primary`
    - `hover:bg-surface-base`
    - `rounded-md`
    - `transition-colors duration-base`
    - `focus:outline-none focus:ring-2 focus:ring-accent focus:ring-offset-0`

**Step 6: Run tests** - Verify all ThemeToggle tests pass

### Phase 4: Layout Integration

**Step 7: Integrate into Layout** - Modify `frontend-2/src/pages/Layout.tsx`

Changes:

1. Add import: `import { ThemeToggle } from '../components/ThemeToggle/ThemeToggle'`
2. Insert ThemeToggle between user name button (line 68-75) and logout button (line 76-83):

```tsx
<div className="flex items-center gap-4">
  {userDetails && (
    <button ...>...</button>  // User name (existing)
  )}
  <ThemeToggle />              // NEW
  <Button variant="ghost">    // Odhlásit (existing)
    Odhlásit
  </Button>
</div>
```

**Step 8: Manual testing**

- Test all three theme modes in browser
- Test system preference detection
- Test localStorage persistence (refresh page)
- Test keyboard navigation (Tab to button, press Enter/Space)
- Test all three icons appear correctly
- Verify spacing looks correct with gap-4

## Critical Files

### Files to Create (6):

1. `frontend-2/src/components/Icons/SunIcon.tsx`
2. `frontend-2/src/components/Icons/MoonIcon.tsx`
3. `frontend-2/src/components/Icons/ComputerDesktopIcon.tsx`
4. `frontend-2/src/components/ThemeToggle/ThemeToggle.tsx`
5. `frontend-2/src/components/ThemeToggle/ThemeToggle.test.tsx`
6. `frontend-2/src/theme/ThemeContext.test.tsx`

### Files to Modify (3):

1. `frontend-2/src/theme/ThemeContext.tsx` - Extend to support 'system' mode
2. `frontend-2/src/components/Icons/index.ts` - Add icon exports
3. `frontend-2/src/pages/Layout.tsx` - Integrate ThemeToggle

## Key Design Decisions

**Icon Selection Logic:**

- Light mode → SunIcon (user is currently in light mode)
- Dark mode → MoonIcon (user is currently in dark mode)
- System mode → ComputerDesktopIcon (following system preference)

**Toggle Behavior:**

- Click cycles: Light → Dark → System → Light

**System Preference Handling:**

- When theme='system', detect OS preference via `window.matchMedia('(prefers-color-scheme: dark)')`
- Listen for OS preference changes and update effectiveTheme
- Clean up listener when theme changes away from 'system'

**Accessibility:**

- ARIA labels describe next action: "Switch to dark mode", "Switch to system preference", "Switch to light mode"
- Title attribute shows current mode for tooltip
- Purple focus ring (ring-accent) for keyboard navigation

## Edge Cases Handled

1. **localStorage unavailable** (private browsing): Fall back to system preference
2. **System preference changes**: Listener updates theme when in 'system' mode
3. **Memory leaks**: Clean up event listeners on unmount
4. **Initial mount flicker**: Use existing `mounted` state pattern
5. **Test environment**: Mock window.matchMedia (not in jsdom by default)

## Styling Alignment

Follows Klabis Design System from `docs/frontend/claude_styling_guide.md`:

- Color tokens: text-text-secondary, text-text-primary, bg-surface-base
- Focus ring: ring-accent (purple #8b5cf6)
- Spacing: 4px base scale (px-3 py-2, gap-4)
- Transitions: duration-base (200ms)
- Border radius: rounded-md (8px)
