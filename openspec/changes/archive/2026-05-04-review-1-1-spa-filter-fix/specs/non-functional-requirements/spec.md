## ADDED Requirements

### Requirement: Reserved URL Paths Not Captured by SPA Fallback

The system SHALL ensure that browser navigation to backend-served pages — Swagger UI, OpenAPI document, and developer manual — returns the actual page content from the corresponding backend handler. These URLs SHALL never return the React SPA shell, regardless of which client (browser, API consumer) issues the request.

#### Scenario: Developer reads Swagger UI in a browser

- **GIVEN** a developer opens `/swagger-ui.html` in a browser
- **THEN** the page redirects to `/swagger-ui/index.html`
- **AND** the page renders the Swagger UI listing the public API endpoints
- **AND** the page does not render the SPA shell or a SPA "page not found" message

#### Scenario: Developer downloads the OpenAPI document

- **GIVEN** a developer opens `/v3/api-docs` in a browser or via API client
- **THEN** the response is the OpenAPI document in JSON format
- **AND** the response is not the SPA shell

#### Scenario: Authenticated club user reads the developer manual

- **GIVEN** a club user has logged in via the login form (session is established)
- **WHEN** the user opens `/docs/index.html` in the browser
- **THEN** the page renders the developer manual home page
- **AND** the page does not render the SPA shell or a SPA "page not found" message

#### Scenario: SPA route serves the React application shell

- **WHEN** a user opens any SPA route (`/`, `/events`, `/members/{id}`) in the browser
- **THEN** the response is the React SPA shell page
- **AND** client-side routing renders the requested screen

#### Scenario: Unknown API path returns a JSON error, not the SPA shell

- **GIVEN** an API consumer requests `/api/foo-bar-not-existing` with `Accept: application/json`
- **THEN** the response status is 404
- **AND** the response body is JSON
- **AND** the response is not the SPA shell HTML
