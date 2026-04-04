## Project Structure

**Klabis** is a modular monolith with three main components:

1. **Backend** → `./backend/` (Spring Boot application)
    - API endpoints, OAuth2 server, business logic
    - See `./backend/CLAUDE.md` for build/test/run commands
    - Gradle build, Java 17+, Spring Boot 4.0.5

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

### Check specifications when expected behavior is in question

- Project uses OpenSpec - use `openspec` commands to check active specifications to find out how application should work if expected behavior is unclear. 
- Always check related openspec specifications when planning new tasks - if planned work would imply changing specifications, initiate openspec proposal instead.   

### Check Before Starting Services

**CRITICAL:** Always check if services are already running before starting new processes (`lsof -i :8443`, `lsof -i :3000`, or check IntelliJ Run tool window).

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

### Refactoring Task Phases

- Prefer vertical slices (one feature/method end-to-end) over horizontal slices (one layer across all features) when breaking refactoring into phases
- Each phase should be independently committable and testable

## Backend Development

For backend development, use the `backend-developer` agent which always loads `backend-patterns`, `developer:tdd-best-practices`, and `developer:spring-conventions` skills, and loads additional skills on-demand (e.g., `developer:spring-modulith`, `developer:spring-data-jdbc`, `developer:spring-hateoas-api`).

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


## Additional instructions
- dont use Edit/Write/MultiEdit/Update/etc tools when jetbrains MCP alternatives are available (prefer jetbrains tools)