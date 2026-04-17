## ADDED Requirements

### Requirement: Local Development OAuth2 Client

The system SHALL optionally register a second OAuth2 client `klabis-web-local` when the `local-dev` Spring profile is active, distinct from the production `klabis-web` public client, to enable refresh-token-based silent token renewal during development on a separate frontend origin.

#### Scenario: Local-dev profile registers a confidential client alongside the public client

- **WHEN** the system starts with the `local-dev` Spring profile active
- **THEN** both `klabis-web` (public, PKCE, no client secret) and `klabis-web-local` (confidential, PKCE required, `client_secret_post` authentication method) are registered
- **AND** `klabis-web-local` has `authorization_code` and `refresh_token` grant types enabled
- **AND** `klabis-web-local` redirect URIs are restricted to `http://localhost:*` addresses only

#### Scenario: Local-dev profile inactive means local client does not exist

- **WHEN** the system starts without the `local-dev` profile
- **THEN** only `klabis-web` (public client) is registered
- **AND** `klabis-web-local` does not exist in the registered client repository
- **AND** any authorization request using `client_id=klabis-web-local` is rejected as an unknown client

#### Scenario: Local-dev client receives a refresh token on authorization code exchange

- **WHEN** a client authenticates with `klabis-web-local` and exchanges an authorization code for tokens via `client_secret_post`
- **THEN** the token response contains `access_token`, `id_token`, and `refresh_token`
- **AND** refresh token rotation is in effect (the next refresh invalidates the previous refresh token)

#### Scenario: Production public client does not receive a refresh token on authorization code exchange

- **WHEN** a client authenticates with the public `klabis-web` client and exchanges an authorization code for tokens
- **THEN** the token response contains `access_token` and `id_token`
- **AND** the token response does not contain a `refresh_token`
- **AND** silent token renewal in same-origin deployments is served by the authorization endpoint's `prompt=none` mechanism (iframe silent renewal)

#### Scenario: Local-dev client redirect URIs cannot point outside localhost

- **WHEN** an authorization request for `klabis-web-local` specifies a `redirect_uri` that is not a `http://localhost:*` URL
- **THEN** the authorization server rejects the request with `invalid_request`
- **AND** no authorization code is issued

#### Scenario: Bootstrap logs a warning if local-dev profile is active in a non-local environment

- **WHEN** the system starts with the `local-dev` profile active
- **AND** the configured authorization server issuer URL is not a `localhost`-based URL
- **THEN** the bootstrap logs a warning indicating that `local-dev` should only be activated on local developer machines
- **AND** the client is still registered (the warning does not block startup, to allow legitimate edge cases like tunneled local sessions)
