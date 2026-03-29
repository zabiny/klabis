# Frontend Development Guide

## Recommended skills
- `hal-navigator-patterns`: use always when working with UI code. It contains knowledge about how to work with Hal+HalForms in the frontend.

## Localization

- **System:** In-house `src/localization/labels.ts` — no i18n framework (single language: Czech)
- **Usage:** `import { labels, getFieldLabel, getTemplateLabel, getNavLabel, getEnumLabel } from '@/localization'`
- **Field override:** `getFieldLabel('eventDate', { eventDate: 'Custom label' })` — per-form override via second arg
- **Legacy:** `constants/messages.ts` re-exports from localization — new code should import `@/localization` directly
- **HAL integration:** `getTemplateLabel` used in `HalFormTemplateButton`, `getFieldLabel` in `HalFormsInput`, `getNavLabel` in `useRootNavigation`

## Technology Stack

- **Framework:** React 19.1.1 + TypeScript 5.8.3
- **Build Tool:** Vite 7.1.2
- **State Management:** TanStack Query (React Query) 5.85.5
- **Routing:** React Router DOM 6.14.0
- **Styling:** Tailwind CSS 3.3.0
- **Forms:** Formik 2.4.6 + Yup 1.7.1
- **Authentication:** oidc-client-ts 3.3.0 (OAuth2/OIDC)
- **API Client:** openapi-fetch 0.14.0 + openapi-react-query 0.5.0
- **Testing:** Vitest 2.1.8 + Testing Library

## Project Structure

```
frontend/
├── src/
│   ├── api/              # API client and HATEOAS utilities
│   ├── components/       # React components
│   │   ├── UI/          # Reusable UI components
│   │   ├── members/     # Member-specific components
│   │   ├── events/      # Event-specific components
│   │   ├── KlabisTable/ # Table component with HAL support
│   │   └── HalNavigator2/ # HATEOAS navigator component
│   ├── contexts/        # React contexts (HalRouteContext, etc.)
│   ├── hooks/           # Custom React hooks
│   ├── pages/           # Page components
│   ├── theme/           # Theme configuration
│   ├── utils/           # Utility functions
│   └── constants/       # Application constants
├── docs/                # Documentation
└── dist/                # Build output (copied to backend/resources/static)
```

## Development Workflow

### Prerequisites

```bash
# Install dependencies
npm install
```

### Running the Application

```bash
# Development server (http://localhost:3000)
npm run dev

# Production build
npm run build

# Preview production build
npm run preview
```

**Important:** Backend must be running on `https://localhost:8443` for API proxying to work.

### Testing

**IMPORTANT:** Always use the `test-runner` agent to execute tests. Never run `npm test` directly.

**Test Configuration:**

- Test files: `src/**/*.{test,spec}.{ts,tsx}`
- Setup file: `src/setupTests.ts`
- Environment: jsdom
- Coverage provider: v8

### Deployment

```bash
# Build and copy to backend static resources
npm run refresh-backend-server-resources

# This command:
# 1. Runs all tests (npm run test) - MUST PASS before proceeding
# 2. Runs production build (npm run build)
# 3. Clears backend/src/main/resources/static/
# 4. Copies dist/* to backend static resources
# 5. Stages changes in git

# Note: If tests fail, the deployment is aborted (build won't run)
```

- Always run `refresh-backend-server-resources` before commit if frontend files have been changed. 

## API Integration

### OpenAPI Client Generation

```bash
# Generate TypeScript types from OpenAPI spec
npm run openapi

# Reads: ../docs/openapi/klabis-full.json
# Outputs: ./src/api/klabisApi.d.ts
```

**Important:** Regenerate types after backend API changes.

### API Setup

API client configured in `src/api/setup.ts`:

- **Base URL:** Auto-detected (`/api` in dev, proxied to backend)
- **Authentication:** OAuth2 Bearer tokens via `authorizedFetch`
- **HATEOAS:** HAL+JSON support in `src/api/hateoas.ts`
- **Klabis API types:** Type definitions in [klabisApi.d.ts](src/api/klabisApi.d.ts). Shall be used for typesafe work with requests and responses (export needed types through [index.ts](src/api/index.ts))

### HATEOAS Conventions

This frontend follows HAL (Hypertext Application Language) conventions:

1. **Navigate via Links, Not URLs:**
    - Use `_links` relations, never hardcode URLs
    - Example: `resource._links.self.href`

2. **HATEOAS Utilities** (`src/api/hateoas.ts`):
   ```typescript
   // Extract links
   getLink(resource, 'self')
   getLinkHref(resource, 'collection')

   // Follow links
   followLink(resource, 'members')

   // Embedded resources
   getEmbedded(resource, 'items')
   ```

3. **Reusable Components:**
    - `KlabisTableWithQuery` - Table with HAL pagination support
    - `HalSubresourceProvider` - allows to conditionally display data of resources from `_links` section. Fetched data and provides them to subcomponents through `useHalResource` hook.  

## Authentication (OAuth2/OIDC)

### Configuration

OAuth2 client configured in `src/api/klabisUserManager.ts`:

- **Flow:** Authorization Code with PKCE (public client, no client_secret)
- **Authority:** `/` (proxied to `https://localhost:8443` in dev)
- **Scopes:** `openid profile MEMBERS EVENTS`
- **Client ID:** `klabis-web`

### Usage

```typescript
import { klabisUserManager } from '@/api/klabisUserManager';

// Login
await klabisUserManager.signinRedirect();

// Get current user
const user = await klabisUserManager.getUser();

// Logout
await klabisUserManager.signoutRedirect();
```

**Authorized Fetch** (`src/api/authorizedFetch.ts`):

- Automatically adds Bearer token to requests
- Handles token refresh
- Redirects to login on 401

## Vite Proxy Configuration

Development server proxies backend requests:

```typescript
proxy: {
  '/api': {
    target: 'https://localhost:8443',
    secure: false,
    changeOrigin: true,
    rewrite: path => path.substr("/api".length)
  },
  '/.well-known': {  // OAuth2 discovery
    target: 'https://localhost:8443',
    secure: false,
    changeOrigin: true,
  }
}
```

## Path Aliases

```typescript
import { MyComponent } from '@/components/MyComponent';
import { useMyHook } from '@/hooks/useMyHook';
```

Configured in:

- `vite.config.ts` → `@` alias
- `tsconfig.json` → TypeScript path mapping

## Styling Guidelines

### Tailwind CSS

- **Utility-first:** Use Tailwind classes directly
- **Custom theme:** `tailwind.config.ts`
- **Plugins:**
    - `@tailwindcss/forms` - Form styling
    - `@tailwindcss/typography` - Rich text content

### Component Patterns

```tsx
// Prefer functional components with TypeScript
export const MyComponent: React.FC<MyComponentProps> = ({ prop1 }) => {
  return (
    <div className="flex items-center space-x-4">
      {/* Component content */}
    </div>
  );
};
```

## State Management

### TanStack Query (React Query)

- Global config in `src/main.tsx`: `refetchOnWindowFocus: false`, `retry: 1`
- Query key convention: domain-based arrays, e.g. `['members']`, `['members', id]`
- After mutations: `queryClient.invalidateQueries({ queryKey: ['members'] })`
- **DevTools:** Available in development mode (toggle button at `http://localhost:3000`)

## Forms

### Formik + Yup

- Validation schemas via Yup, bindings via `formik-mui` (MUI) and `formik-mui-x-date-pickers`

## Environment Variables

Create `.env.local` for local overrides:

```bash
VITE_HAL_ROOT_URI=/api
```

**Access in code:**

```typescript
const apiRoot = import.meta.env.VITE_HAL_ROOT_URI;
```

## Common Tasks

### Add a New Page

1. Create component in `src/pages/MyPage.tsx`
2. Add route in `src/App.tsx`
3. Update navigation links

### Add a New API Endpoint

1. Update OpenAPI spec: `../docs/openapi/klabis-full.json`
2. Regenerate types: `npm run openapi`
3. Create query/mutation hooks in `src/api/`

### Add a New Component

1. Create in appropriate directory:
    - Reusable → `src/components/UI/`
    - Domain-specific → `src/components/{domain}/`
2. Export from `index.ts` if reusable
3. Add tests in `{ComponentName}.test.tsx`

### Update Tailwind Theme

1. Edit `tailwind.config.ts`
2. Restart dev server
3. Use new classes in components

## Code Quality

### Linting

```bash
# Run ESLint
npm run lint
```

**Configuration:** `eslint.config.js`

- TypeScript ESLint recommended rules
- React Hooks plugin
- React Refresh plugin

### Type Checking

```bash
# Type check (via build)
npm run build
```

**TypeScript Configs:**

- `tsconfig.json` - Base configuration
- `tsconfig.app.json` - App-specific settings
- `tsconfig.node.json` - Node/build scripts

## Gotchas & Best Practices

### 0. Process Management

Always check if frontend is already running before starting — see root `CLAUDE.md`.

### 1. HATEOAS Navigation

❌ **Don't hardcode URLs:**

```typescript
fetch('/members/123')  // BAD
```

✅ **Follow HAL links:**

```typescript
const href = getLinkHref(member, 'self');
fetch(href);  // GOOD
```

### 2. API Type Safety

❌ **Don't use `any`:**

```typescript
const data: any = await response.json();  // BAD
```

✅ **Use generated types:**

```typescript
import type { components } from '@/api/klabisApi';
type Member = components['schemas']['MemberDto'];
```

### 3. OAuth2 Errors

If getting 401 errors:

1. **Check backend is running on `https://localhost:8443`**
   ```bash
   curl -k https://localhost:8443/actuator/health
   ```

2. **Inspect actual OAuth2 responses** (don't assume):
   - Open browser DevTools → Network tab
   - Check `/oauth2/authorize` response
   - Decode `id_token` at jwt.io to see actual claims
   - Verify `access_token` scopes

3. **Common OAuth2 mistakes:**
   - Using password grant (NOT supported - use authorization code)
   - Wrong `client_id` (must match backend registration)
   - Requesting scopes not registered in backend
   - Backend running old code before OAuth2 changes

4. Clear browser cookies/local storage (last resort)

5. Check proxy configuration in `vite.config.ts`

### 4. Build Output

After `npm run build`, **always** run `npm run refresh-backend-server-resources` to update backend static files.

### 5. Testing

- Always mock API calls in tests
- Use `src/__mocks__/` for shared mocks
- Follow Testing Library best practices (queries by role/label)

### 6. TanStack Query + StrictMode

- `onSuccess` at `useMutation` hook level can fire multiple times in StrictMode (multiple observers)
- Use per-call callbacks instead: `mutate(data, { onSuccess: () => {...} })` — fires exactly once

### 7. Icon Library

- Project uses `lucide-react` for icons (not `@heroicons/react`)
- Lucide naming: `Pencil`, `Banknote`, `Shield`, `UserX`, `Check`, `UserPlus`

### 8. HAL+FORMS Template Names

- Template names are derived from Spring controller method names (e.g., `suspendMember`, `registerMember`, `resumeMember`)
- Frontend must use the exact backend method name when referencing templates in `HalFormButton name="..."`

## Debugging

### React Query DevTools

- Available in development mode
- Toggle panel to inspect queries/mutations
- View cache state, refetch, invalidate queries

### Browser DevTools

- Network tab: Check API requests/responses
- Console: OAuth2 redirect logs
- Application tab: Check OAuth2 tokens in localStorage

### Vite DevTools

- Hot Module Replacement (HMR) for instant updates
- Source maps enabled in development

## Related Documentation

- **Backend API:** `../backend/CLAUDE.md`
- **API Specification:** `../docs/openapi/klabis-full.json`
- **Main Project:** `../CLAUDE.md`

## Migration Notes

This modern React frontend is replacing the legacy vanilla JS UI mockup located at
`../backend/src/main/resources/static/mock/`. Key improvements:

- Type-safe API client (OpenAPI generated types)
- Modern state management (TanStack Query)
- Component-based architecture
- Comprehensive testing (Vitest + Testing Library)
- OAuth2/OIDC integration (oidc-client-ts)
- Full HATEOAS support (HAL+JSON)