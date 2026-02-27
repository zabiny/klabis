# Klabis best practises for frontend

This file describes best practises for Frontend (React + TypeScript). Use it whenever planning, designing, writing, updating, refactoring or reviewing frontend code.

See `./frontend/CLAUDE.md` for detailed frontend development workflow and conventions.

# General principles
- KISS: Keep components simple and focused
- Composition over inheritance
- TypeScript strict mode - avoid `any`

# Coding conventions

## Components
- Use functional components with hooks
- Prefer composition over prop drilling
- Keep components small - single responsibility
- Use TypeScript interfaces for props

## State management
- Use TanStack Query (React Query) for server state
- Use React Context for global UI state
- Keep local state in components (useState)

## Code organization
- Feature-based folder structure
- Co-locate related code (components, hooks, types)
- Separate business logic from UI components

## Styling
- Use Tailwind CSS for styling
- Prefer utility classes over custom CSS
- Use responsive design (mobile-first)

## Security
- Never store tokens in localStorage
- Use oidc-client-ts for OAuth2 flow
- Let backend handle authorization checks

# Common pitfalls


