# React Code Review: frontend-2

**Date:** 2025-12-17
**Reviewed by:** Claude Code
**React Version:** 19.1.1
**TypeScript:** 5.8.3

---

## Overall Assessment

The codebase is **well-structured** with modern patterns: functional components, TypeScript strict mode, React Query, and proper hook usage. However, there are several high-impact improvements to consider.

**Strengths:**
- Modern React 19 with hooks
- Strong TypeScript usage (strict mode enabled)
- Good component composition
- React Query for server state management
- Error boundaries in place
- Clean separation of concerns (api, components, contexts, hooks, pages)

---

## High-Impact Improvements

### 1. No Unit Tests Written

**Severity:** Critical
**Location:** `src/` (no test files found)

**Issue:** Testing infrastructure exists (Jest + React Testing Library configured in `jest.config.cjs` and `setupTests.ts`) but no actual tests are written.

**Impact:** High risk of regressions, harder refactoring, reduced confidence in code changes.

**Recommendation:** Start with critical paths:
- `useAuth` hook - authentication flow
- `HalFormsForm` component - form rendering and submission
- Data fetching hooks (`useNavigationTargetResponse`)
- Type guard functions (`isHalResponse`, `isCollectionContent`) - easy wins, pure functions

---

### 2. Duplicate Fetch Logic (DRY Violation)

**Severity:** High
**Locations:**
- `src/components/HalNavigator/hooks.ts:29-46` (`fetchResource`)
- `src/components/HalNavigator/hooks.ts:101-135` (`useQueryNavigationTargetResponse`)
- `src/api/hateoas.ts:16-30` (`fetchHalFormsData`)

**Issue:** Same authentication + fetch pattern repeated in 3 places:

```typescript
// Repeated pattern:
const user = await userManager.getUser();
const res = await fetch(url, {
    headers: {
        Accept: "application/prs.hal-forms+json,application/hal+json",
        "Authorization": `Bearer ${user?.access_token}`
    },
});
```

**Recommendation:** Extract to a single `authorizedFetch` utility:

```typescript
// src/api/authorizedFetch.ts
export const authorizedFetch = async (
  url: string,
  options?: RequestInit
): Promise<Response> => {
  const user = await userManager.getUser();
  return fetch(url, {
    ...options,
    headers: {
      ...options?.headers,
      Authorization: `Bearer ${user?.access_token}`,
    },
  });
};
```

Alternatively, use the already configured `openapi-fetch` middleware consistently across all API calls.

---

### 3. Side Effect in `getInitialValues` (Bug)

**Severity:** High
**Location:** `src/components/HalFormsForm/HalFormsForm.tsx:29-30`

**Issue:** Function mutates its input parameter, violating React's immutability principle:

```typescript
// BUG: Mutates input parameter!
data[prop.name] = data[prop.name] === null ? [] : data[prop.name];
```

**Impact:** Can cause subtle bugs, unexpected re-renders, and makes debugging difficult.

**Fix:** Clone data before modification:

```typescript
function getInitialValues(
    template: HalFormsTemplate,
    inputData: FormData
): Record<string, unknown> {
    const data = { ...inputData }; // Clone first
    const initialValues: Record<string, unknown> = {};
    // ... rest of logic
    return initialValues;
}
```

---

### 4. `any` Type Usage (TypeScript Anti-pattern)

**Severity:** Medium
**Locations:**
- `src/components/HalFormsForm/HalFormsForm.tsx:17`: `type FormData = Record<string, any>;`
- `src/components/HalFormsForm/HalFormsForm.tsx:24-25`: `Record<string, any>` multiple times
- `src/components/HalFormsForm/HalFormsForm.tsx:41`: `Record<string, any>`
- `src/api/hateoas.ts:7,32`: `Record<string, any>`

**Issue:** Using `any` bypasses TypeScript's type checking, defeating the purpose of strict mode.

**Recommendation:** Use `unknown` with type guards or define proper interfaces:

```typescript
type FormData = Record<string, unknown>;

// Or better, define specific types:
interface HalFormValues {
  [fieldName: string]: string | number | boolean | null | HalFormValues | HalFormValues[];
}
```

---

### 5. Missing `useMemo` for Expensive Operations

**Severity:** Medium
**Location:** `src/components/HalFormsForm/HalFormsForm.tsx:236-237`

**Issue:** Validation schema and initial values recalculated on every render:

```typescript
// Recalculated on every render
const initialValues = getInitialValues(template, data);
const validationSchema = createValidationSchema(template);
```

**Recommendation:** Memoize expensive computations:

```typescript
const initialValues = useMemo(
  () => getInitialValues(template, data),
  [template, data]
);

const validationSchema = useMemo(
  () => createValidationSchema(template),
  [template]
);
```

---

### 6. Missing Error Boundary Integration with React Query

**Severity:** Medium
**Location:** `src/components/HalNavigator/index.tsx:178`

**Issue:** Error boundaries exist but don't leverage React Query's `throwOnError` option for consistent error handling. Currently, errors are manually checked in each component.

**Current pattern:**
```typescript
if (response.isLoading) return <Loading />;
if (response.error) return <Error />;
```

**Better pattern:**
```typescript
// In query configuration
useQuery({
  queryKey: [...],
  queryFn: ...,
  throwOnError: true  // Errors bubble to ErrorBoundary
});

// ErrorBoundary handles all errors consistently
```

---

### 7. Hardcoded Czech Strings

**Severity:** Low
**Locations:** Throughout codebase

Examples:
- `"Povinné pole"` (Required field)
- `"Neplatný email"` (Invalid email)
- `"Nahravam data"` (Loading data)
- `"Nepovedlo se nacist data"` (Failed to load data)
- `"Zobraz zdrojovy JSON"` (Show source JSON)

**Recommendation:** Extract to constants file for future internationalization:

```typescript
// src/constants/messages.ts
export const VALIDATION_MESSAGES = {
  required: "Povinné pole",
  invalidEmail: "Neplatný email",
  mustBeNumber: "Musí být číslo",
  invalidFormat: "Nesprávný formát",
} as const;

export const UI_MESSAGES = {
  loading: "Nahrávám data",
  loadError: "Nepovedlo se načíst data",
  showSourceJson: "Zobraz zdrojový JSON",
} as const;
```

---

### 8. Unused Dependencies

**Severity:** Low
**Location:** `package.json`

**Unused packages:**
- `axios` (v1.4.0) - Native fetch is used instead throughout the codebase
- `remix-auth` (v4.2.0) - Not used, `oidc-client-ts` handles authentication
- `remix-auth-oauth2` (v3.4.1) - Not used

**Action:** Remove to reduce bundle size:

```bash
npm uninstall axios remix-auth remix-auth-oauth2
```

---

### 9. Missing Loading States with Proper UX

**Severity:** Low
**Location:** `src/App.tsx:16-18`

**Issue:** Basic loading state without styling or skeleton:

```typescript
if (isLoading) {
    return <div>Loading...</div>;  // No styling, no skeleton
}
```

**Recommendation:** Use MUI components for consistent UX:

```typescript
import { CircularProgress, Box } from '@mui/material';

if (isLoading) {
    return (
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh">
            <CircularProgress />
        </Box>
    );
}
```

---

### 10. Module-Level Singleton Makes Testing Difficult

**Severity:** Low
**Location:** `src/components/HalNavigator/hooks.ts:11`

**Issue:** Direct module-level access to singleton:

```typescript
const userManager: UserManager = klabisAuthUserManager;  // Module-level singleton
```

**Impact:** Makes unit testing harder due to implicit dependencies.

**Recommendation:** Pass via Context or use dependency injection pattern:

```typescript
// Option 1: Use existing AuthContext
const { userManager } = useAuth();

// Option 2: Create a dedicated context
const userManager = useUserManager();
```

---

## Quick Wins (Low Effort, High Value)

| Priority | Issue | File | Effort | Impact |
|----------|-------|------|--------|--------|
| 1 | Remove unused deps | `package.json` | 5 min | Bundle size |
| 2 | Fix `getInitialValues` mutation | `HalFormsForm.tsx:29` | 10 min | Bug fix |
| 3 | Add `useMemo` for validation schema | `HalFormsForm.tsx:236` | 5 min | Performance |
| 4 | Replace `any` with `unknown` | Multiple files | 30 min | Type safety |
| 5 | Add proper loading spinner | `App.tsx:16` | 10 min | UX |

---

## Architecture Recommendations

### Consider React Hook Form for New Forms

**Current:** Formik v2.4.6 + Yup

**Issue:** Formik has slower development cycle and performance issues with complex forms. React Hook Form is the 2025 standard with:
- Better performance (uncontrolled inputs by default)
- Smaller bundle size (~8KB vs ~13KB)
- More active maintenance
- Better TypeScript support

**Trade-off:** Migration effort vs. long-term maintainability. Consider using React Hook Form for new forms while gradually migrating existing ones.

### Consider Zustand for Complex Shared State

**Current:** Custom `useNavigation` hook with useState

For shared navigation state across components, Zustand provides a cleaner API:

```typescript
import { create } from 'zustand';

interface NavigationState {
  history: NavigationTarget[];
  current: NavigationTarget | null;
  navigate: (target: NavigationTarget) => void;
  back: () => void;
  reset: (initial: NavigationTarget) => void;
}

const useNavigationStore = create<NavigationState>((set) => ({
  history: [],
  current: null,
  navigate: (target) => set((state) => ({
    history: [...state.history, state.current].filter(Boolean),
    current: target
  })),
  back: () => set((state) => ({
    current: state.history.at(-1) ?? null,
    history: state.history.slice(0, -1)
  })),
  reset: (initial) => set({ history: [], current: initial })
}));
```

---

## Summary

### Top 3 Priority Actions

1. **Write tests** - Start with hooks and utility functions (type guards, validation helpers)
2. **Fix mutation bug** in `getInitialValues` - This is an actual bug that can cause issues
3. **Consolidate fetch logic** into single utility - Reduces code duplication and maintenance burden

### Metrics to Track

- Test coverage (currently 0%)
- Bundle size (check with `npm run build`)
- TypeScript strict errors (should be 0)
- ESLint warnings/errors

---

---

## Additional Review: Layout.tsx & GenericHalPage.tsx (2025-12-22)

### Issue 1: CRITICAL - Duplicated `extractNavigationPath` Function

**Severity:** CRITICAL
**Files Affected:**
- `src/pages/GenericHalPage.tsx` (lines 18-37)
- `src/hooks/useRootNavigation.ts` (lines 21-40)

**Problem:**
The same function exists in two places with identical logic:

```typescript
// Duplicate in TWO files!
function extractNavigationPath(url: string): string {
  try {
    const parsedUrl = new URL(url);
    let path = parsedUrl.pathname;
    if (path.startsWith('/api')) {
      path = path.substring(4);
    }
    return path;
  } catch {
    if (url.startsWith('/api')) {
      return url.substring(4);
    }
    return url;
  }
}
```

**Why Critical:**
- **DRY Violation**: Any logic change must be updated in both locations
- **Bug Risk**: HIGH - Updates to one location easily forgotten in the other
- **Maintenance Burden**: Developers must remember both locations exist
- **Consistency Risk**: One location may diverge from the other

**Impact:**
If URL parsing logic needs adjustment (e.g., handling query parameters), it must be changed in TWO places. Easy to miss one, causing inconsistent behavior.

**Immediate Action Required:**
Create `src/utils/navigationPath.ts` and consolidate:

```typescript
/**
 * Extracts the navigation path from a full URL for React Router.
 * Removes the /api prefix since HalRouteContext adds it back.
 *
 * Example: https://localhost:8443/api/members/123 -> /members/123
 */
export function extractNavigationPath(url: string): string {
  try {
    const parsedUrl = new URL(url);
    let path = parsedUrl.pathname;
    if (path.startsWith('/api')) {
      path = path.substring(4);
    }
    return path;
  } catch {
    if (url.startsWith('/api')) {
      return url.substring(4);
    }
    return url;
  }
}
```

Then update both files:
```typescript
import { extractNavigationPath } from '../utils/navigationPath';
```

---

### Issue 2: HIGH - GenericHalPage Component Complexity & Duplication

**Severity:** HIGH
**Location:** `src/pages/GenericHalPage.tsx` (458 lines)

**Problems:**

#### A. Massive Code Duplication Between Two Sub-Components

`GenericCollectionDisplay` and `GenericItemDisplay` contain nearly identical logic:

**Duplicated: handleNavigateToItem**
- Lines 127-130 (Collection)
- Lines 307-310 (Item)
```typescript
// IDENTICAL - appears twice
const handleNavigateToItem = (href: string) => {
  const path = extractNavigationPath(href);
  navigate(path);
};
```

**Duplicated: handleFormSubmit**
- Lines 132-153 (Collection)
- Lines 312-333 (Item)
Nearly 100% identical, only error message text differs

**Duplicated: Links Section Rendering**
- Lines 202-224 (Collection)
- Lines 369-391 (Item)
Same rendering logic, just different parent

**Duplicated: Templates/Forms Section Rendering**
- Lines 227-282 (Collection)
- Lines 394-449 (Item)
Same rendering logic, just different parent

#### B. Component is Too Large
458 lines in a single component makes it:
- Hard to understand
- Hard to test
- Hard to modify without side effects

#### C. Tight Coupling Between Logic and Rendering
Form submission and navigation logic mixed with JSX makes testing impossible without rendering.

**Why This Matters:**
- **Maintenance**: Bug fixes must be applied in 2+ places
- **Testing**: Can't test logic without rendering
- **Consistency Risk**: Updates may miss one location
- **Cognitive Load**: Hard to understand what component does

**Recommended Refactoring:**

**Step 1: Extract Shared Hook**
```typescript
// src/hooks/useHalActions.ts
export function useHalActions() {
  const navigate = useNavigate();
  const { pathname, refetch } = useHalRoute();
  const [selectedTemplate, setSelectedTemplate] = useState<HalFormsTemplate | null>(null);
  const [submitError, setSubmitError] = useState<Error | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleNavigateToItem = (href: string) => {
    const path = extractNavigationPath(href);
    navigate(path);
  };

  const handleFormSubmit = async (formData: Record<string, any>) => {
    if (!selectedTemplate) return;
    // ... existing logic
  };

  return {
    selectedTemplate,
    setSelectedTemplate,
    submitError,
    isSubmitting,
    handleNavigateToItem,
    handleFormSubmit,
  };
}
```

**Step 2: Extract Reusable Components**
```typescript
// src/components/HalLinksSection.tsx
export function HalLinksSection({
  links,
  onNavigate,
}: {
  links?: Record<string, any>;
  onNavigate: (href: string) => void;
}) {
  if (!links || Object.keys(links).length === 0) return null;

  return (
    <div className="mt-4 p-4 border rounded bg-blue-50 dark:bg-blue-900">
      <h3 className="font-semibold mb-2">Dostupné akce</h3>
      <div className="flex flex-wrap gap-2">
        {Object.entries(links)
          .filter(([rel]) => rel !== 'self')
          .map(([rel, link]: [string, any]) => {
            const links = Array.isArray(link) ? link : [link];
            return links.map((l: any, idx: number) => (
              <button
                key={`${rel}-${idx}`}
                onClick={() => onNavigate(l.href)}
                className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
              >
                {l.title || rel}
              </button>
            ));
          })}
      </div>
    </div>
  );
}

// src/components/HalFormsSection.tsx
export function HalFormsSection({
  templates,
  data,
  selectedTemplate,
  onSelectTemplate,
  onSubmit,
  submitError,
  isSubmitting,
}: HalFormsSectionProps) {
  if (!templates || Object.keys(templates).length === 0) return null;

  return (
    <div className="mt-4 p-4 border rounded bg-green-50 dark:bg-green-900">
      <h3 className="font-semibold mb-2">Dostupné formuláře</h3>
      {selectedTemplate ? (
        <div>{/* ... form UI ... */}</div>
      ) : (
        <div className="flex flex-wrap gap-2">
          {Object.entries(templates).map(([name, template]) => (
            <button
              key={name}
              onClick={() => onSelectTemplate(template)}
              className="px-3 py-1 bg-green-600 text-white rounded hover:bg-green-700"
            >
              {template.title || name}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
```

**Step 3: Simplify Components**
```typescript
// After refactoring, GenericCollectionDisplay becomes ~80 lines instead of ~200

const GenericCollectionDisplay = ({ data }: GenericCollectionDisplayProps) => {
  const actions = useHalActions();
  const items = Object.values(data._embedded || {}).flat();

  if (!items || items.length === 0) {
    return <Alert severity="info"><p>Kolekce je prázdná</p></Alert>;
  }

  return (
    <div className="space-y-4">
      <h2 className="text-2xl font-bold">Položky</h2>

      {/* Items table */}
      <div className="overflow-x-auto border rounded-lg">
        <table className="w-full text-sm">
          {/* ... table markup ... */}
        </table>
      </div>

      {/* Reuse components */}
      <HalLinksSection
        links={data._links}
        onNavigate={actions.handleNavigateToItem}
      />

      <HalFormsSection
        templates={data._templates}
        data={data}
        {...actions}
      />

      <JsonPreview data={data} />
    </div>
  );
};
```

---

### Issue 3: MEDIUM - Loose Type Safety in GenericHalPage.tsx

**Severity:** MEDIUM
**Location:** Throughout `src/pages/GenericHalPage.tsx`

**Examples:**
- Line 94: `function isHalCollection(data: any)`
- Line 100: `function isEmptyObject(obj: any)`
- Line 178: `items.map((item: any, index: number)`
- Lines 210, 374: Multiple `any` type annotations

**Impact:**
- Loss of IDE autocompletion
- Bugs not caught at compile time
- Makes refactoring difficult

**Solution:**
Define proper interfaces:
```typescript
export interface LinkRecord extends Link {
  title?: string;
}

export interface HalItemData extends HalResponse {
  id?: string | number;
  [key: string]: unknown;
}

// Update functions:
function isHalCollection(data: unknown): data is HalCollectionResponse
function isEmptyObject(obj: unknown): boolean
items.map((item: HalItemData, index: number) => ...)
```

---

### Issue 4: MEDIUM - Missing Error Handling in Layout.tsx

**Severity:** MEDIUM
**Location:** `src/pages/Layout.tsx` (lines 98-113)

**Current Code:**
```typescript
{menuLoading ? (
    <div>Loading menu...</div>
) : menuItems.length > 0 ? (
    // render menu
) : (
    <div>No menu items available</div>
)}
```

**Problem:**
`useRootNavigation` returns an `error` state, but Layout.tsx ignores it. Users see "No menu items available" when there's actually an API error (e.g., permission denied).

**Solution:**
```typescript
const { data: menuItems = [], isLoading: menuLoading, error: menuError } = useRootNavigation();

{menuLoading ? (
    <div className="text-gray-500 text-sm">Loading menu...</div>
) : menuError ? (
    <div className="text-red-600 text-sm">
      Failed to load menu: {menuError.message}
    </div>
) : menuItems.length > 0 ? (
    // render menu
) : (
    <div className="text-gray-500 text-sm">No menu items available</div>
)}
```

---

### Issue 5: MEDIUM - Magic Strings in GenericHalPage.tsx

**Severity:** MEDIUM
**Locations:** Throughout file

**Examples:**
- `'self'` used multiple times to filter links
- `'_'` used to filter properties
- Hardcoded Czech UI strings

**Solution:**
Create constants file:
```typescript
// src/constants/hal.ts
export const HAL_LINK_RELS = {
  SELF: 'self',
  CURIES: 'curies',
} as const;

export const UI_MESSAGES = {
  EMPTY_COLLECTION: 'Kolekce je prázdná',
  AVAILABLE_ACTIONS: 'Dostupné akce',
  AVAILABLE_FORMS: 'Dostupné formuláře',
} as const;
```

Then use:
```typescript
.filter(([rel]) => rel !== HAL_LINK_RELS.SELF)
<p>{UI_MESSAGES.EMPTY_COLLECTION}</p>
```

---

## Priority Summary

| Priority | Issue | Action |
|----------|-------|--------|
| CRITICAL | Duplicate `extractNavigationPath` | Extract to utility immediately |
| HIGH | GenericHalPage complexity | Refactor into 3 smaller components |
| MEDIUM | Missing type safety | Define interfaces in `types.ts` |
| MEDIUM | Missing error handling | Display menu errors in Layout |
| MEDIUM | Magic strings | Extract to constants |

---

## References

- [React Design Patterns and Best Practices for 2025](https://www.telerik.com/blogs/react-design-patterns-best-practices)
- [React State Management in 2025](https://www.developerway.com/posts/react-state-management-2025)
- [Modern React State Management Guide](https://dev.to/joodi/modern-react-state-management-in-2025-a-practical-guide-2j8f)
- [React Hook Form Documentation](https://react-hook-form.com/)
- [Zustand Documentation](https://zustand-demo.pmnd.rs/)
