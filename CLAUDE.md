## Project Structure

**Klabis** is a modular monolith with three main components:

1. **Backend** → `./backend/` (Spring Boot application)
    - API endpoints, OAuth2 server, business logic
    - See `./backend/CLAUDE.md` for build/test/run commands
    - Gradle build, Java 17+, Spring Boot 3.5.9

2. **Frontend (UI Mockup)** → `./backend/src/main/resources/static/mock` (Vanilla JS)
    - Static HTML/CSS/JavaScript UI served by Spring Boot
    - See `./backend/src/main/resources/static/mock/CLAUDE.md` for development
    - OAuth2 Authorization Code flow integration

3. **Specifications** → `./openspec/` (OpenSpec change proposal workflow)
    - Feature specifications, design docs, tasks
    - Uses experimental artifact workflow (opsx commands)

## Quick Start

```bash
# Start backend (serves both API and UI mockup on https://localhost:8443)
cd backend
BOOTSTRAP_ADMIN_USERNAME='admin' \
BOOTSTRAP_ADMIN_PASSWORD='admin123' \
OAUTH2_CLIENT_SECRET='test-secret-123' \
./gradlew bootRun

# UI accessible at: https://localhost:8443/mock-login.html
```

## OpenSpec Workflow

When planning or implementing new features:

- **Do NOT** change task descriptions in `tasks.md` - only add notes in braces `()` or subsections
- Follow [Creating OpenSpec Tasks](./claude_refs/PLANNING_TASKS.md) when planning implementation todos
- See individual component CLAUDE.md files for coding conventions

## IntelliJ HTTP Files

When testing API endpoints with `.http` files:

- Always use `intellij-http-files` skill when working with .http files
- Follow additional instructions from [IntelliJ HTTP file usage](./claude_refs/HTTP_FILES_USAGE.md)

**Example OAuth2 authenticated call:**

```http request
GET {{apiBaseUrl}}/members
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
```

- `{{apiBaseUrl}}` - defined in `backend/http-client.env.json`
- `AuthorizationCode` - OAuth2 configuration in `backend/http-client.env.json`

## Component-Specific Instructions

Before starting work, check the appropriate CLAUDE.md:

- **Backend work** → `./klabis/CLAUDE.md`
    - Build commands, test strategies, security setup, gotchas

- **UI mockup work** → `./klabis/src/main/resources/static/mock/CLAUDE.md`
    - Frontend structure, OAuth2 flow, HTML/CSS/JS conventions

- **Specifications** → Reference `./openspec/` for change proposals
    - Feature specifications and design documents

