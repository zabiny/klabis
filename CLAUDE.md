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

3. **Frontend (UI Mockup)** → `./backend/src/main/resources/static/mock` (Vanilla JS)
    - Legacy static HTML/CSS/JavaScript UI served by Spring Boot
    - See `./backend/src/main/resources/static/mock/CLAUDE.md` for development
    - OAuth2 Authorization Code flow integration

4. **Specifications** → `./openspec/` (OpenSpec change proposal workflow)
    - Feature specifications, design docs, tasks
    - Uses experimental artifact workflow (opsx commands)

## Quick Start

```bash
# Start backend (serves API and legacy UI mockup on https://localhost:8443)
cd backend
BOOTSTRAP_ADMIN_USERNAME='admin' \
BOOTSTRAP_ADMIN_PASSWORD='admin123' \
OAUTH2_CLIENT_SECRET='test-secret-123' \
./gradlew bootRun

# Legacy UI mockup: https://localhost:8443/mock-login.html

# Start modern frontend (React SPA on http://localhost:3000)
cd frontend
npm run dev
```

## Development Workflow Best Practices

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

### Sandbox Issues

- Direct `./gradlew` commands may fail with "bwrap: loopback: Failed RTM_NEWADDR"
- Use `test-runner` skill instead of direct Gradle commands for testing
- Gradle runs in sandbox mode by default - most operations work but some may be restricted 

## IntelliJ HTTP Files

When testing API endpoints with `.http` files:

- Always use `intellij-http-files` skill when working with .http files

**Example OAuth2 authenticated call:**

```http request
GET {{apiBaseUrl}}/members
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
```

- `{{apiBaseUrl}}` - defined in `backend/http-client.env.json`
- `AuthorizationCode` - OAuth2 configuration in `backend/http-client.env.json`

## Backend Development

For backend development, use the `backend-developer` agent which leverages specialized skills:
- `developer:tdd-best-practices` - TDD workflow (RED-GREEN-REFACTOR)
- `developer:spring-modulith` - DDD patterns and Spring Modulith architecture
- `developer:spring-data-jdbc` - Repository and persistence patterns
- `developer:backend-development` - HATEOAS and API patterns

## Component-Specific Instructions

Before starting work, check the appropriate CLAUDE.md:

- **Backend work** → `./backend/CLAUDE.md`
    - Build commands, architecture, coding practises, test strategies, security setup, gotchas

- **Frontend work** → `./frontend/CLAUDE.md`
    - React/TypeScript conventions, build/dev workflow, testing strategy

- **UI mockup work** → `./backend/src/main/resources/static/mock/CLAUDE.md`
    - Mockup frontend UI, OAuth2 flow, HTML/CSS/JS conventions

- **Specifications** → Reference `./openspec/` for change proposals
    - Feature specifications and design documents
