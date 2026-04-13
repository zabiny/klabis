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

Navigate via `_links` relations, never hardcode URLs. See `hal-navigator-patterns` skill for detailed component patterns.

Key utilities in `src/api/hateoas.ts`: `getLink()`, `getLinkHref()`, `followLink()`, `getEmbedded()`.

## Authentication (OAuth2/OIDC)

### Configuration

OAuth2 client configured in `src/api/klabisUserManager.ts`:

- **Flow:** Authorization Code with PKCE
- **Authority:** `/` (proxied to `https://localhost:8443` in dev)
- **Client ID / Secret / Scopes:** read from Vite env vars `VITE_OAUTH_CLIENT_ID`, `VITE_OAUTH_CLIENT_SECRET`, `VITE_OAUTH_SCOPE`
- **Production defaults** (in `frontend/.env`): `klabis-web` public client, no secret — matches the deployed configuration

### Vite environment variables for OAuth2

| Variable | Default (`.env`) | Local-dev (`.env.development.local`) |
|---|---|---|
| `VITE_OAUTH_CLIENT_ID` | `klabis-web` | `klabis-web-local` |
| `VITE_OAUTH_CLIENT_SECRET` | _(empty — public client)_ | `local-dev-secret-please-change-nothing` |
| `VITE_OAUTH_SCOPE` | `openid profile MEMBERS EVENTS` | `openid profile MEMBERS EVENTS` |

**Local development:** Copy `frontend/.env.development.local.example` to `frontend/.env.development.local` (gitignored via `*.local`). `runLocalEnvironment.sh` does this automatically on first run.

When `VITE_OAUTH_CLIENT_SECRET` is non-empty, `oidc-client-ts` uses `client_secret_post` authentication at the token endpoint, and the backend returns a `refresh_token`. This enables refresh-token-based silent renewal at `http://localhost:3000` (cross-origin from `:8443` where iframe-based renewal cannot carry the session cookie).

**Production** builds use the public `klabis-web` client (empty secret). Silent renewal in production works via the iframe path because the SPA runs same-origin with the backend on `:8443`.

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

Dev server proxies `/api` → `https://localhost:8443` (strips `/api` prefix) and `/.well-known` for OAuth2 discovery. See `vite.config.ts` for details.

## Key Conventions

- TanStack Query: `refetchOnWindowFocus: false`, `retry: 1`, query keys as domain-based arrays (`['members']`, `['members', id]`)
- Forms: Formik + Yup, bindings via `formik-mui` and `formik-mui-x-date-pickers`

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

If getting 401: verify backend is running, check Network tab for `/oauth2/authorize` response, verify `access_token` scopes. Only authorization code flow is supported (not password grant).

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

## Related Documentation

- **Backend API:** `../backend/CLAUDE.md`
- **API Specification:** `../docs/openapi/klabis-full.json`
- **Main Project:** `../CLAUDE.md`