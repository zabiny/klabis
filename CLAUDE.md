## Project Structure

**Klabis** is a modular monolith with three main components:

1. **Backend** → `./backend/` (Spring Boot application)
    - API endpoints, OAuth2 server, business logic
    - See `./backend/CLAUDE.md` for build/test/run commands
    - Gradle build, Java 17+, Spring Boot 3.5.9

2. **Frontend** → `./frontend/` (React + TypeScript + Vite)
    - Modern React SPA with OpenAPI integration
    - OAuth2 authentication via oidc-client-ts
    - TanStack Query, React Router, Tailwind CSS
    - See `./frontend/CLAUDE.md` for development workflow

3. **Specifications** → `./openspec/` (OpenSpec change proposal workflow)
    - Feature specifications, design docs, tasks
    - Uses experimental artifact workflow (opsx commands)

## Quick Start

```bash
# Start backend (serves API)
cd backend
KLABIS_ADMIN_USERNAME='admin' \
KLABIS_ADMIN_PASSWORD='admin123' \
KLABIS_OAUTH2_CLIENT_SECRET='test-secret-123' \
KLABIS_JASYPT_PASSWORD='test-key-123' \
./gradlew bootRun

# Start modern frontend (React SPA on http://localhost:3000)
cd frontend
npm run dev
```

### Available users for testing

- Admin user (have all permissions) - registration number `ZBM9000`, password `password`
- Club member user (have standard permissions as club member) - reigstration number `ZBM9500`, password `password`

## Development Workflow Best Practices

### HAL+FORMS Template Names (HATEOAS 3.0)
- Templates use controller method name as key (e.g., `updateMember`, `createEvent`), NOT `"default"`
- Frontend accesses templates via `_templates.updateMember` etc.
- Backend affordances: `andAffordances(afford(methodOn(Controller.class).methodName(...)))` → template name = `methodName`

### Check specifications when expected behavior is in question

- Project uses OpenSpec - use `openspec` commands to check active specifications to find out how application should work if expected behavior is unclear. 
- Always check related openspec specifications when planning new tasks - if planned work would imply changing specifications, initiate openspec proposal instead.   

### Check Before Starting Services

**CRITICAL:** Always check if services are already running before starting new processes.

```bash
# Check backend (port 8443)
lsof -i :8443 || netstat -tulpn | grep 8443
ps aux | grep -E "bootRun" | grep -v grep

# Check frontend (port 3000)
lsof -i :3000 || netstat -tulpn | grep 3000
ps aux | grep -E "vite|npm.*dev" | grep -v grep

# Check IntelliJ Run tool window for existing processes
```

**Why this matters:**

- Avoids duplicate processes consuming resources
- Prevents port conflicts (e.g., frontend on 3001 instead of 3000)
- Ensures you're testing the correct running instance
- Saves debugging time

### Common best practises

- DO NOT create comments describing what code is doing. Use comments sparingly - exclusively to document intention behind implementation and only if is it necessary.
- use `/mnt/ramdisk/klabis` folder when need to save temporary file
- always do code review (use proper agent) before commiting changes involving CODE (either backend or frontend) to git. 
- use `http://localhost:3000` (NPM dev) for testing frontend using playwright. Never use port 8443 for UI testing.

#### Frontend testing

##### Authentication

The login page is at `https://localhost:8443/login` (redirected automatically from app). Fields:
- Registration number: `textbox "např. 12345"`
- Password: `textbox "••••••••"`
- Submit: `button "Přihlásit se"`

After login, wait for navigation menu to load before proceeding.

To switch users: click "Odhlásit" button, then log in with new credentials.

##### OIDC Token Access

To make direct API calls for verification:
```javascript
browser_evaluate: async () => {
  const user = JSON.parse(sessionStorage.getItem('oidc.user:http://localhost:3000/:klabis-web') || '{}');
  const token = user.access_token;
  const resp = await fetch('/api/...', {
    headers: { 'Authorization': 'Bearer ' + token, 'Accept': 'application/prs.hal-forms+json' }
  });
  return await resp.json();
}
```

### IDE Diagnostics vs Reality

- JetBrains IDE diagnostics sent via MCP are often stale/cached — always verify with actual compilation (`tsc --noEmit`) or test run before acting on them
- Common false positive: "import is declared but never used" when the import IS used — IDE cache hasn't refreshed

### Agent Output Verification

- After `backend-developer` agent completes, verify critical files were actually modified (read key files or check `git diff`) before running tests
- Agent may report "all changes made" while some files remain unmodified — especially when multiple files need coordinated updates

### Frontend Agent Quality Checks

- After frontend agent completes, verify imports are actually USED (not just added) — agents sometimes add imports without replacing the hardcoded strings
- Quick check: `grep -r "from '@/localization'" src/ | wc -l` vs actual usage count

### Refactoring Task Phases

- Prefer vertical slices (one feature/method end-to-end) over horizontal slices (one layer across all features) when breaking refactoring into phases
- Each phase should be independently committable and testable

### Sandbox Issues

- Direct `./gradlew` commands may fail with "bwrap: loopback: Failed RTM_NEWADDR"
- Use `test-runner` agent instead of direct Gradle commands for testing
- Gradle runs in sandbox mode by default - most operations work but some may be restricted
- Workaround: Use `dangerouslyDisableSandbox: true` for Bash tool when needed
- `curl` to localhost (e.g. `https://localhost:8443`) is blocked by sandbox — use `dangerouslyDisableSandbox: true`

### Git Quirks

- `git diff HEAD` fails with "ambiguous argument" — use `git diff HEAD -- .` instead (HEAD file exists in repo root)

### Git & 1Password Integration Issues

- 1Password socket errors ("Could not connect to socket") require user intervention
- If git commit fails with 1Password error, ask user to check 1Password agent status
- Short commit messages (without HEREDOC) are more reliable than multi-line messages

## IntelliJ HTTP Files

When testing API endpoints with `.http` files:

**Example OAuth2 authenticated call:**

```http request
GET {{apiBaseUrl}}/members
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
```

- `{{apiBaseUrl}}` - defined in `backend/http-client.env.json`
- `AuthorizationCode` - OAuth2 configuration in `backend/http-client.env.json`

## Backend Development

For backend development, use the `backend-developer` agent which leverages specialized skills:
- `backend-patterns` - Klabis-specific patterns (modules, services, controllers, JDBC, events)
- `developer:tdd-best-practices` - TDD workflow (RED-GREEN-REFACTOR)
- `developer:spring-modulith` - DDD patterns and Spring Modulith architecture
- `developer:spring-data-jdbc` - Repository and persistence patterns
- `developer:spring-hateoas-api` - HATEOAS and API patterns

## Component-Specific Instructions

Before starting work, check the appropriate CLAUDE.md:

- **Backend work** → `./backend/CLAUDE.md`
    - Build commands, architecture, coding practises, test strategies, security setup, gotchas

- **Frontend work** → `./frontend/CLAUDE.md`
    - React/TypeScript conventions, build/dev workflow, testing strategy

- **Specifications** → Reference `./openspec/` for change proposals
    - Feature specifications and design documents

## Commit Conventions

Follow Conventional Commits format:

```
<type>(<scope>): <description>

feat(members): add member registration endpoint
fix(email): correct welcome email template
test(members): add tests for registration number generation
docs(api): update authentication examples
refactor(users): extract password validation to service
```

**Common types:** feat, fix, test, docs, refactor, chore, perf, style, ci
