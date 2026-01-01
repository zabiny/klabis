Klabis is a club management system for an orienteering sports club (specifically the Czech club "Zabiny").

# Main Purpose

It manages various aspects of club operations including:
- Member management - profiles, licenses (driving, referee, trainer), registration tracking
- Event registration - register members for orienteering events with SI cards
- Financial tracking - club accounts and transactions
- Groups/Teams - organize members into groups
- ORIS integration - connects with the Czech national orienteering information system
- Calendar management - event calendars

# Technology Stack

## Backend:
- Spring Boot 4.0 (Java 21)
- Domain-Driven Design (DDD) + Spring Modulith architecture
- PostgreSQL database
- OAuth2/OIDC authentication
- REST API with HATEOAS (HAL format)
- OpenAPI/Swagger documentation

## Frontend:
- React 19 with TypeScript
- Material-UI components
- Vite build tool
- React Query for data fetching

# Architecture

The project follows a modular monolith pattern with separate domains:
- Members, Events, Finance, Groups, Calendar, ORIS, Users

Each domain has a clean 3-layer architecture (Domain → Application → Infrastructure) following DDD principles with aggregate roots, value objects, and domain events.

The system is actively developed and includes comprehensive monitoring (Prometheus, Grafana, Zipkin) and CI/CD with GitHub Actions.
