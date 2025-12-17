# Project Overview

Klabis is a club management system for an orienteering sports club (Czech club "Zabiny"). It manages members, events,
finances, groups, calendars, and integrates with ORIS (Czech national orienteering information system).

## Build Commands

### Backend (Spring Boot / Gradle)

```bash
cd backend
./gradlew build              # Build and run tests
./gradlew test               # Run tests only
./gradlew bootRun            # Run the application
./gradlew generateOpenApiDocs # Generate OpenAPI documentation
./gradlew bootBuildImage     # Build Docker image
```

### Frontend-2 (new - React / Vite / MUI)

```bash
cd frontend-2
npm install
npm run dev                  # Development server (Vite)
npm run build                # Production build
npm run lint                 # ESLint
npm run test                 # Jest tests
npm run test:watch           # Jest watch mode
npm run openapi              # Generate API types from OpenAPI
npm run refresh-backend-server-resources  # Copy dist to backend static resources
```

### Supporting Services (Docker)

```bash
docker-compose up            # Start Prometheus, Grafana, Zipkin
```

- Prometheus: http://localhost:9090
- Grafana: http://localhost:9030 (admin:admin)
- Zipkin: http://localhost:9411

## Architecture

### Backend Structure

The backend follows **Domain-Driven Design (DDD)** with **Spring Modulith** architecture. Each domain module is in
`backend/src/main/java/club/klabis/`:

- `members/` - Member management (profiles, licenses, registration)
- `events/` - Event registration
- `finance/` - Club accounts and transactions
- `groups/` - Member groups/teams
- `calendar/` - Event calendars
- `oris/` - ORIS integration
- `users/` - User authentication
- `shared/` - Cross-cutting concerns (config, domain utilities)

Each module follows a 3-layer structure:

- `domain/` - Aggregate roots, value objects, domain events
- `application/` - Application services, use cases
- `infrastructure/` - Controllers, repositories, external adapters

### Key Technologies

- **Backend**: Spring Boot 4.0, Java 21, PostgreSQL, OAuth2/OIDC
- **API**: REST with HATEOAS (HAL+Forms format), OpenAPI/Swagger
- **Frontend-2**: React 19, TypeScript, Material-UI, Vite, React Query
- **Mapping**: MapStruct for DTO mapping
- **Testing**: JUnit 5, Testcontainers, ArchUnit

### HAL+Forms Conventions (from bestpractises.md)

- `_links` contains relations to other resources (GET endpoints)
- `_templates` (affordances) contain actions (PUT, POST, DELETE)
- Affordances are defined in `RepresentationModelProcessor` near the relevant Controller
- Resource structure with affordance should match the GET endpoint structure
- If affordance has same URL as current resource, use values from currently displayed resource (GET response) for form
  population
- If affordance has different URL, fetch target affordance URL using GET method to fetch init data to populate form
  values

### Event Sourcing Pattern

- **Write Model**: Validates operations and stores events to EventStore (one event = one aggregate root). Uses DDD
  Aggregate to handle operation.
- **Read Model**: CRUD updated by EventListeners, denormalized for efficient queries

## Configuration

- Spring profiles: `server` (default), `localhost`, `inmemorydb`, `springdoc`, `hateoas`
- Environment variables needed: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` for OAuth

## API Documentation

- Swagger UI: https://localhost:8443/swagger-ui/index.html
- OpenAPI spec generated to `docs/openapi/` during build
