# Spring Security Architecture

This document describes the Spring Security filter chain and OAuth2 configuration in the Klabis Backend application.

## Table of Contents

1. [Overview](#overview)
2. [Architecture Components](#architecture-components)
3. [Security Filter Chain](#security-filter-chain)
4. [OAuth2 Token Flow](#oauth2-token-flow)
5. [JWT Authentication Process](#jwt-authentication-process)
6. [Authorization Flow](#authorization-flow)
7. [Configuration Details](#configuration-details)

---

## Overview

The Klabis Backend uses **Spring Authorization Server** to provide OAuth2 authentication and **Spring Security Resource
Server** to protect API endpoints with JWT-based authentication.

### Key Features

- ✅ OAuth2 Authorization Server with JDBC-backed client repository
- ✅ JWT access tokens with custom claims (registrationNumber, authorities)
- ✅ Stateless session management
- ✅ Method-level security with `@PreAuthorize`
- ✅ Custom authentication/authorization exception handling
- ✅ User-centric authorization (roles → authorities mapping)

---

## Architecture Components

```mermaid
graph TB
    subgraph "Client Application"
        Client["Web/Mobile Client<br/>client_id: klabis-web"]
    end

    subgraph "Klabis Backend"
        subgraph "Authorization Server"
            AS["OAuth2 Authorization Server<br/>Port 8443"]
            TokenEndpoint["Token Endpoint<br/>/oauth2/token"]
            AuthEndpoint["Authorization Endpoint<br/>/oauth2/authorize"]
            JWKEndpoint["JWK Set Endpoint<br/>/oauth2/jwks"]
            IntrospectEndpoint["Introspection Endpoint<br/>/oauth2/introspect"]

            AS --> TokenEndpoint
            AS --> AuthEndpoint
            AS --> JWKEndpoint
            AS --> IntrospectEndpoint
        end

        subgraph "Resource Server"
            RS["OAuth2 Resource Server"]
            API["API Endpoints<br/>/api/**"]
            JwtDecoder["JWT Decoder<br/>RSA Public Key"]

            RS --> API
            RS --> JwtDecoder
        end

        subgraph "Data Layer"
            DB[("PostgreSQL")]
            Users["users table"]
            Roles["user_roles table"]
            Clients["oauth2_registered_client"]
            Authorizations["oauth2_authorization"]

            DB --> Users
            DB --> Roles
            DB --> Clients
            DB --> Authorizations
        end

        subgraph "Security Components"
            UDS["KlabisUserDetailsService"]
            AuthManager["AuthenticationManager<br/>DaoAuthenticationProvider"]
            PasswordEnc["BCryptPasswordEncoder"]

            UDS --> Users
            UDS --> Roles
            AuthManager --> UDS
            AuthManager --> PasswordEnc
        end
    end

    Client -->|1. Request Token| TokenEndpoint
    TokenEndpoint -->|2. Authenticate| AuthManager
    TokenEndpoint -->|3. Issue JWT| Client
    Client -->|4. API Request + JWT| API
    API -->|5. Validate JWT| JwtDecoder

    AS -.->|Read/Write| Clients
    AS -.->|Read/Write| Authorizations

    style AS fill:#e1f5ff
    style RS fill:#fff4e1
    style DB fill:#f0f0f0
```

---

## Security Filter Chain

Spring Security uses two separate filter chains:

### 1. Authorization Server Filter Chain

Handles OAuth2 protocol endpoints.

```mermaid
graph LR
    Request[HTTP Request] --> ASMatcher{Matches OAuth2<br/>endpoints?}

    ASMatcher -->|Yes| ASChain[Authorization Server<br/>Filter Chain]
    ASMatcher -->|No| NextChain[Next Chain]

    ASChain --> ASFilters[Default OAuth2<br/>Security Filters]

    ASFilters --> TokenFilter[OAuth2 Token<br/>Endpoint Filter]
    ASFilters --> AuthFilter[OAuth2 Authorization<br/>Endpoint Filter]
    ASFilters --> JWKFilter[JWK Set Endpoint<br/>Filter]

    style ASChain fill:#e1f5ff
    style ASFilters fill:#c5e8ff
```

**Configuration**: `AuthorizationServerConfiguration.authorizationServerSecurityFilterChain()`

### 2. Resource Server Filter Chain

Protects API endpoints with JWT authentication.

```mermaid
graph TD
    Request[HTTP Request<br/>to /api/**] --> SecurityContext[SecurityContextPersistenceFilter<br/>Establish security context]

    SecurityContext --> Matcher{Request Matcher}

    Matcher -->|/actuator/health<br/>/h2-console/**| PermitAll[Permit All]
    Matcher -->|/oauth2/**<br/>/login| PermitAll
    Matcher -->|/api/**| AuthRequired[Authentication Required]

    AuthRequired --> JwtFilter[OAuth2ResourceServerFilter<br/>JWT Authentication]

    JwtFilter --> ExtractJWT{Extract JWT<br/>from Authorization<br/>header}

    ExtractJWT -->|Missing/Invalid| AuthEntryPoint[AuthenticationEntryPoint<br/>Return 401 Unauthorized]
    ExtractJWT -->|Valid Token| JwtDecoder[JwtDecoder<br/>Validate & Decode JWT]

    JwtDecoder -->|Invalid Signature| AuthEntryPoint
    JwtDecoder -->|Expired| AuthEntryPoint
    JwtDecoder -->|Valid| JwtConverter[JwtAuthenticationConverter<br/>Extract authorities]

    JwtConverter --> CreateAuth[Create<br/>JwtAuthenticationToken<br/>with authorities]

    CreateAuth --> SecurityContextSet[Set Authentication<br/>in SecurityContext]

    SecurityContextSet --> AuthorizationFilter[AuthorizationFilter<br/>Check permissions]

    AuthorizationFilter --> MethodCheck{Method Security<br/>@PreAuthorize?}

    MethodCheck -->|Passed| Controller[Controller Method]
    MethodCheck -->|Failed| AccessDenied[AccessDeniedHandler<br/>Return 403 Forbidden]

    AuthEntryPoint --> ErrorResponse1[Problem Detail JSON<br/>type: unauthorized<br/>status: 401]
    AccessDenied --> ErrorResponse2[Problem Detail JSON<br/>type: forbidden<br/>status: 403]

    Controller --> Response[HTTP Response]

    style JwtFilter fill:#fff4e1
    style JwtDecoder fill:#ffe8b3
    style JwtConverter fill:#ffd480
    style AuthorizationFilter fill:#ffb84d
```

**Configuration**: `SecurityConfiguration.defaultSecurityFilterChain()`

**Filter Chain Details**:

```java
/api/**
├── SecurityContextPersistenceFilter
├── CsrfFilter (disabled)
├── OAuth2ResourceServerFilter
│   ├── BearerTokenAuthenticationFilter
│   │   ├── Extract JWT from "Authorization: Bearer <token>"
│   │   ├── JwtDecoder validates signature with RSA public key
│   │   └── JwtAuthenticationConverter converts to Authentication
│   └── Set Authentication in SecurityContext
├── AuthorizationFilter
│   ├── Check HttpSecurity authorize rules
│   └── @PreAuthorize method security
├── ExceptionTranslationFilter
│   ├── AuthenticationException → AuthenticationEntryPoint (401)
│   └── AccessDeniedException → AccessDeniedHandler (403)
└── FilterSecurityInterceptor
```

---

## OAuth2 Token Flow

### Client Credentials Flow

```mermaid
sequenceDiagram
    participant Client as Web/Mobile Client
    participant Token as /oauth2/token
    participant ClientRepo as RegisteredClientRepository
    participant AuthMgr as AuthenticationManager
    participant Encoder as JwtEncoder
    participant DB as Database

    Client->>Token: POST /oauth2/token<br/>grant_type=client_credentials<br/>Basic Auth (client_id:client_secret)

    Token->>ClientRepo: findByClientId("klabis-web")
    ClientRepo->>DB: SELECT * FROM oauth2_registered_client
    DB-->>ClientRepo: Client data
    ClientRepo-->>Token: RegisteredClient

    Token->>Token: Verify client_secret<br/>(BCrypt match)

    Token->>Encoder: Generate JWT with claims:<br/>- sub: klabis-web<br/>- scope: members.read members.write<br/>- iat, exp, jti

    Encoder->>Encoder: Sign with RSA private key
    Encoder-->>Token: Signed JWT

    Token->>DB: INSERT INTO oauth2_authorization

    Token-->>Client: 200 OK<br/>{<br/>  "access_token": "eyJhbG...",<br/>  "token_type": "Bearer",<br/>  "expires_in": 900<br/>}
```

### Resource Owner Password Credentials Flow

```mermaid
sequenceDiagram
    participant Client as Web/Mobile Client
    participant Token as /oauth2/token
    participant ClientRepo as RegisteredClientRepository
    participant AuthMgr as AuthenticationManager
    participant UDS as UserDetailsService
    participant DB as Database
    participant Encoder as JwtEncoder

    Client->>Token: POST /oauth2/token<br/>grant_type=password<br/>username=admin<br/>password=admin123<br/>Basic Auth (client_id:client_secret)

    Token->>ClientRepo: Verify client credentials
    ClientRepo-->>Token: Valid client

    Token->>AuthMgr: authenticate(username, password)
    AuthMgr->>UDS: loadUserByUsername("ZBM0001")
    UDS->>DB: SELECT * FROM users<br/>WHERE registration_number='ZBM0001'
    DB-->>UDS: User entity
    UDS->>DB: SELECT role FROM user_roles<br/>WHERE user_id=...
    DB-->>UDS: [ROLE_ADMIN]
    UDS-->>AuthMgr: UserDetails with authorities

    AuthMgr->>AuthMgr: BCrypt verify password
    AuthMgr-->>Token: Authentication (ROLE_ADMIN)

    Token->>Token: Map roles to authorities:<br/>ROLE_ADMIN → MEMBERS:CREATE,<br/>MEMBERS:READ, etc.

    Token->>Encoder: Generate JWT with custom claims:<br/>- sub: ZBM0001<br/>- registrationNumber: ZBM0001<br/>- authorities: [MEMBERS:CREATE, ...]<br/>- scope: members.read members.write

    Encoder->>Encoder: Sign with RSA private key
    Encoder-->>Token: Signed JWT

    Token->>DB: INSERT INTO oauth2_authorization

    Token-->>Client: 200 OK<br/>{<br/>  "access_token": "eyJhbG...",<br/>  "refresh_token": "...",<br/>  "token_type": "Bearer",<br/>  "expires_in": 900<br/>}
```

### Authorization Code Flow

```mermaid
sequenceDiagram
    participant User as User's Browser
    participant Client as Web Application
    participant Auth as /oauth2/authorize
    participant Login as Login Page
    participant Token as /oauth2/token
    participant DB as Database

    Client->>User: Redirect to Authorization URL
    User->>Auth: GET /oauth2/authorize?<br/>response_type=code&<br/>client_id=klabis-web&<br/>scope=members.read members.write&<br/>redirect_uri=http://localhost:8080/authorized

    Auth->>Auth: Check authentication
    Auth->>Login: Redirect to login (not authenticated)

    User->>Login: POST /login<br/>username=ZBM0001<br/>password=admin123
    Login->>DB: Authenticate user
    DB-->>Login: User authenticated
    Login-->>User: Redirect back to /oauth2/authorize

    User->>Auth: GET /oauth2/authorize (authenticated)
    Auth->>Auth: Generate authorization code
    Auth->>DB: Store authorization code
    Auth-->>User: Redirect to redirect_uri?code=ABC123

    User->>Client: Follow redirect
    Client->>Token: POST /oauth2/token<br/>grant_type=authorization_code&<br/>code=ABC123&<br/>redirect_uri=http://localhost:8080/authorized<br/>Basic Auth (client_id:client_secret)

    Token->>DB: Validate authorization code
    DB-->>Token: Code valid
    Token->>Token: Generate access token & refresh token
    Token->>DB: Store tokens, invalidate code

    Token-->>Client: 200 OK<br/>{<br/>  "access_token": "eyJhbG...",<br/>  "refresh_token": "...",<br/>  "token_type": "Bearer",<br/>  "expires_in": 900<br/>}

    Client-->>User: Application logged in
```

---

## JWT Authentication Process

### JWT Structure

```mermaid
graph LR
    JWT[JWT Token] --> Header[Header<br/>alg: RS256<br/>typ: JWT]
    JWT --> Payload[Payload Claims<br/>sub: ZBM0001<br/>registrationNumber: ZBM0001<br/>authorities: Array<br/>scope: String<br/>iat, exp, jti]
    JWT --> Signature[Signature<br/>RSA-SHA256]

    style JWT fill:#e1f5ff
    style Header fill:#fff4e1
    style Payload fill:#ffe8b3
    style Signature fill:#ffb84d
```

### JWT Token Customization

```mermaid
graph TD
    Context[JwtEncodingContext] --> Customizer[OAuth2TokenCustomizer]

    Customizer --> CheckType{Token Type?}

    CheckType -->|access_token| AddClaims[Add Custom Claims]
    CheckType -->|refresh_token| Skip[Skip customization]

    AddClaims --> RegNum[Claim: registrationNumber<br/>from Principal.name]
    AddClaims --> Authorities[Claim: authorities<br/>from Principal.authorities]

    RegNum --> FinalClaims[Final JWT Claims:<br/>- Standard: sub, iat, exp, jti, scope<br/>- Custom: registrationNumber, authorities]
    Authorities --> FinalClaims

    FinalClaims --> Sign[Sign with RSA Private Key]
    Sign --> Token[JWT Access Token]

    style Customizer fill:#fff4e1
    style AddClaims fill:#ffe8b3
    style Token fill:#ffb84d
```

**Configuration**: `AuthorizationServerConfiguration.jwtCustomizer()`

### JWT Validation

```mermaid
graph TD
    Request[API Request<br/>Authorization: Bearer eyJhbG...] --> Extract[Extract JWT Token]

    Extract --> Decoder[JwtDecoder<br/>NimbusJwtDecoder]

    Decoder --> ParseHeader[Parse Header]
    ParseHeader --> ValidateAlg{Algorithm = RS256?}

    ValidateAlg -->|No| Reject1[Reject: Invalid algorithm]
    ValidateAlg -->|Yes| ParsePayload[Parse Payload]

    ParsePayload --> CheckExp{Expired?<br/>exp < now}
    CheckExp -->|Yes| Reject2[Reject: Token expired]
    CheckExp -->|No| CheckIat{Issued in future?<br/>iat > now}

    CheckIat -->|Yes| Reject3[Reject: Invalid iat]
    CheckIat -->|No| VerifySignature[Verify Signature<br/>with RSA Public Key]

    VerifySignature --> ValidSignature{Valid?}
    ValidSignature -->|No| Reject4[Reject: Invalid signature]
    ValidSignature -->|Yes| DecodedJwt[Decoded Jwt]

    DecodedJwt --> Converter[JwtAuthenticationConverter]

    Converter --> ExtractClaims[Extract Claims]
    ExtractClaims --> GetAuthorities[Get authorities claim<br/>as GrantedAuthority list]

    GetAuthorities --> CreateToken[Create JwtAuthenticationToken<br/>principal: Jwt<br/>authorities: Collection<GrantedAuthority><br/>name: sub claim]

    CreateToken --> Authenticated[Authenticated = true]

    Reject1 --> Error401[401 Unauthorized]
    Reject2 --> Error401
    Reject3 --> Error401
    Reject4 --> Error401

    style Decoder fill:#fff4e1
    style Converter fill:#ffe8b3
    style Authenticated fill:#90EE90
    style Error401 fill:#FFB6C1
```

**Configuration**:

- `SecurityConfiguration.jwtDecoder()` - RSA public key validation
- `SecurityConfiguration.jwtAuthenticationConverter()` - Claims to authorities mapping

---

## Authorization Flow

### Roles to Authorities Mapping

```mermaid
graph LR
    subgraph "Database"
        UserRoles[(user_roles table<br/>user_id, role)]
    end

    subgraph "Application Layer"
        User[User Entity<br/>registrationNumber: ZBM0001]
        Roles[Roles Set<br/>ROLE_ADMIN]
    end

    subgraph "Security Layer"
        UDS[UserDetailsService]
        UD[UserDetails<br/>authorities: Collection]
        Auth[Authentication<br/>authorities]
    end

    subgraph "OAuth2 Token"
        JWT[JWT Claims<br/>authorities: Array<String>]
    end

    subgraph "Authorization"
        Check[@PreAuthorize<br/>hasAuthority]
        Decision{Granted?}
    end

    UserRoles -.->|Load| User
    UserRoles -.->|Load| Roles

    User --> UDS
    Roles --> UDS

    UDS -->|Create| UD
    UD -->|Map roles to authorities| Auth

    Auth -->|Include in token| JWT

    JWT -->|Extract on validation| Auth
    Auth --> Check
    Check --> Decision

    Decision -->|Yes| Allow[200 OK]
    Decision -->|No| Deny[403 Forbidden]

    style UDS fill:#e1f5ff
    style JWT fill:#fff4e1
    style Check fill:#ffe8b3
    style Allow fill:#90EE90
    style Deny fill:#FFB6C1
```

### Method Security Evaluation

```mermaid
graph TD
    Request["API Request<br/>POST /api/members"] --> FilterChain["Security Filter Chain"]

    FilterChain --> Authenticated{Authenticated?}

    Authenticated -->|No| Error401["401 Unauthorized<br/>AuthenticationEntryPoint"]
    Authenticated -->|Yes| MethodInterceptor["Method Security<br/>Interceptor"]

    MethodInterceptor --> ReadAnnotation["Read @PreAuthorize<br/>hasAuthority MEMBERS:CREATE"]

    ReadAnnotation --> GetAuth["Get Authentication.authorities"]

    GetAuth --> Evaluate["SpEL Expression Evaluator"]

    Evaluate --> CheckAuthority{"authorities contains<br/>MEMBERS:CREATE?"}

    CheckAuthority -->|No| Error403["403 Forbidden<br/>AccessDeniedHandler"]
    CheckAuthority -->|Yes| InvokeMethod["Invoke Controller Method"]

    InvokeMethod --> BusinessLogic["Business Logic"]
    BusinessLogic --> Success["200/201 Response"]

    Error401 --> Problem401["Problem Detail JSON<br/>type: unauthorized<br/>title: Unauthorized<br/>status: 401<br/>detail: Authentication required"]

    Error403 --> Problem403["Problem Detail JSON<br/>type: forbidden<br/>title: Forbidden<br/>status: 403<br/>detail: Insufficient authority"]

    style Authenticated fill:#fff4e1
    style CheckAuthority fill:#ffe8b3
    style Success fill:#90EE90
    style Error401 fill:#FFB6C1
    style Error403 fill:#FFB6C1
```

### Authority Hierarchy Example

```mermaid
graph TD
    RoleAdmin["ROLE_ADMIN"] --> AdminAuth["Granted Authorities"]

    AdminAuth --> MembersCreate["MEMBERS:CREATE"]
    AdminAuth --> MembersRead["MEMBERS:READ"]
    AdminAuth --> MembersUpdate["MEMBERS:UPDATE"]
    AdminAuth --> MembersDelete["MEMBERS:DELETE"]

    RoleMember["ROLE_MEMBER"] --> MemberAuth["Granted Authorities"]

    MemberAuth --> MembersReadOnly["MEMBERS:READ"]

    subgraph "Endpoint Authorization Requirements"
        EP1["POST /api/members<br/>@PreAuthorize hasAuthority<br/>MEMBERS:CREATE"]
        EP2["GET /api/members/{id}<br/>@PreAuthorize hasAuthority<br/>MEMBERS:READ"]
    end

    MembersCreate -.->|Allowed| EP1
    MembersRead -.->|Allowed| EP2
    MembersReadOnly -.->|Allowed| EP2

    style RoleAdmin fill:#FFB84D
    style RoleMember fill:#FFF4E1
    style MembersCreate fill:#90EE90
    style MembersReadOnly fill:#90EE90
```

---

## Configuration Details

### Security Configuration Classes

```mermaid
graph TB
    subgraph "Configuration Classes"
        SEC[SecurityConfiguration<br/>@EnableWebSecurity<br/>@EnableMethodSecurity]
        AS[AuthorizationServerConfiguration<br/>OAuth2 Authorization Server]
        EH[SecurityExceptionHandler<br/>@RestControllerAdvice]
    end

    subgraph "Beans from SecurityConfiguration"
        PWD[PasswordEncoder<br/>BCryptPasswordEncoder]
        AUTH[AuthenticationManager<br/>DaoAuthenticationProvider]
        KEYPAIR[KeyPair<br/>RSA 2048-bit]
        JWK[JWKSource<br/>RSA Key Set]
        DEC[JwtDecoder<br/>RSA Public Key]
        ENC[JwtEncoder<br/>RSA Private Key]
        CONV[JwtAuthenticationConverter<br/>authorities claim mapper]
        ENTRY[AuthenticationEntryPoint<br/>401 handler]
        DENIED[AccessDeniedHandler<br/>403 handler]
        CHAIN1[SecurityFilterChain<br/>/api/** - Resource Server]
    end

    subgraph "Beans from AuthorizationServerConfiguration"
        REPO[RegisteredClientRepository<br/>JDBC-backed]
        AUTHSVC[OAuth2AuthorizationService<br/>JDBC-backed]
        CONSENT[OAuth2AuthorizationConsentService<br/>JDBC-backed]
        SETTINGS[AuthorizationServerSettings<br/>Endpoints configuration]
        CUSTOM[OAuth2TokenCustomizer<br/>JWT claims customizer]
        CHAIN2[SecurityFilterChain<br/>OAuth2 endpoints]
    end

    subgraph "Exception Handlers"
        EH1[handleAuthenticationException<br/>→ 401 Problem Detail]
        EH2[handleAccessDeniedException<br/>→ 403 Problem Detail]
    end

    SEC --> PWD
    SEC --> AUTH
    SEC --> KEYPAIR
    SEC --> JWK
    SEC --> DEC
    SEC --> ENC
    SEC --> CONV
    SEC --> ENTRY
    SEC --> DENIED
    SEC --> CHAIN1

    AS --> REPO
    AS --> AUTHSVC
    AS --> CONSENT
    AS --> SETTINGS
    AS --> CUSTOM
    AS --> CHAIN2

    EH --> EH1
    EH --> EH2

    AUTH -.uses.-> PWD
    JWK -.uses.-> KEYPAIR
    DEC -.uses.-> KEYPAIR
    ENC -.uses.-> JWK
    CHAIN1 -.uses.-> DEC
    CHAIN1 -.uses.-> CONV
    CHAIN1 -.uses.-> ENTRY
    CHAIN1 -.uses.-> DENIED

    AUTHSVC -.uses.-> REPO
    CONSENT -.uses.-> REPO
    CHAIN2 -.uses.-> SETTINGS

    style SEC fill:#e1f5ff
    style AS fill:#fff4e1
    style EH fill:#ffe8b3
```

### Key Configuration Parameters

| Component                       | Configuration                                    | Value              |
|---------------------------------|--------------------------------------------------|--------------------|
| **Session Management**          | `sessionCreationPolicy`                          | `STATELESS`        |
| **CSRF Protection**             | `csrf`                                           | `disabled`         |
| **JWT Algorithm**               | RSA Signature                                    | `RS256` (2048-bit) |
| **Access Token Lifetime**       | `settings.token.access-token-time-to-live`       | 900s (15 min)      |
| **Refresh Token Lifetime**      | `settings.token.refresh-token-time-to-live`      | 2592000s (30 days) |
| **Authorization Code Lifetime** | `settings.token.authorization-code-time-to-live` | 300s (5 min)       |
| **Authorities Claim Name**      | `jwtAuthenticationConverter`                     | `"authorities"`    |
| **Authority Prefix**            | `jwtAuthenticationConverter`                     | `""` (empty)       |
| **Password Encoding**           | BCrypt                                           | Strength 10        |

### Endpoint Security Matrix

| Endpoint                | Authentication       | Authorization       | Description                    |
|-------------------------|----------------------|---------------------|--------------------------------|
| `/actuator/health`      | ❌ Not required       | ✅ Permit All        | Health check                   |
| `/h2-console/**`        | ❌ Not required       | ✅ Permit All        | H2 database console (dev only) |
| `/oauth2/token`         | ✅ Client credentials | ✅ Permit All        | Token endpoint                 |
| `/oauth2/authorize`     | ✅ User credentials   | ✅ Permit All        | Authorization endpoint         |
| `/oauth2/jwks`          | ❌ Not required       | ✅ Permit All        | JWK Set endpoint               |
| `/oauth2/introspect`    | ✅ Client credentials | ✅ Permit All        | Token introspection            |
| `/oauth2/revoke`        | ✅ Client credentials | ✅ Permit All        | Token revocation               |
| `/login`                | ❌ Not required       | ✅ Permit All        | Login page                     |
| `POST /api/members`     | ✅ JWT required       | 🔐 `MEMBERS:CREATE` | Register member                |
| `GET /api/members/{id}` | ✅ JWT required       | 🔐 `MEMBERS:READ`   | Get member                     |

### Database Schema References

**Users & Roles**:

- `users` - User accounts (registration_number, password_hash, account_status)
- `user_roles` - User role assignments (user_id, role)

**OAuth2**:

- `oauth2_registered_client` - OAuth2 client registrations
- `oauth2_authorization` - Active authorizations (codes, tokens)
- `oauth2_authorization_consent` - User consent records

See: `V002__create_users_and_oauth2_tables.sql`

---

## Request Flow Example

### Complete Request Flow: Register Member

```mermaid
sequenceDiagram
    participant Client
    participant FilterChain as Security Filter Chain
    participant OAuth2Filter as OAuth2ResourceServerFilter
    participant JwtDecoder
    participant JwtConverter as JwtAuthenticationConverter
    participant AuthFilter as AuthorizationFilter
    participant Controller as MemberController
    participant Handler as CommandHandler
    participant DB as Database

    Client->>FilterChain: POST /api/members<br/>Authorization: Bearer eyJhbG...

    FilterChain->>OAuth2Filter: Process request

    OAuth2Filter->>OAuth2Filter: Extract JWT from header

    OAuth2Filter->>JwtDecoder: Decode and validate JWT
    JwtDecoder->>JwtDecoder: Verify RSA signature
    JwtDecoder->>JwtDecoder: Check expiration
    JwtDecoder-->>OAuth2Filter: Valid Jwt

    OAuth2Filter->>JwtConverter: Convert to Authentication
    JwtConverter->>JwtConverter: Extract "authorities" claim:<br/>["MEMBERS:CREATE", "MEMBERS:READ"]
    JwtConverter-->>OAuth2Filter: JwtAuthenticationToken<br/>authenticated=true

    OAuth2Filter->>OAuth2Filter: Set in SecurityContext

    OAuth2Filter->>AuthFilter: Continue filter chain

    AuthFilter->>AuthFilter: Check HttpSecurity rules:<br/>/api/** requires authentication ✓

    AuthFilter->>Controller: Invoke method

    Controller->>Controller: @PreAuthorize evaluation:<br/>hasAuthority('MEMBERS:CREATE')
    Controller->>Controller: Check authorities: ✓ MEMBERS:CREATE present

    Controller->>Handler: handle(RegisterMemberCommand)
    Handler->>DB: INSERT INTO members
    DB-->>Handler: Member created
    Handler-->>Controller: UUID memberId

    Controller->>Controller: Build HATEOAS response
    Controller-->>Client: 201 Created<br/>Location: /api/members/{id}<br/>HAL+JSON response
```

---

## OpenID Connect (OIDC) Support

OpenID Connect Core 1.0 support is enabled through Spring Authorization Server configuration, providing a standardized
identity layer on top of OAuth2.

### Key Features

✅ **Discovery Endpoint**: `/.well-known/openid-configuration` returns OIDC metadata
✅ **ID Tokens**: JWT tokens with identity claims (sub, auth_time, registrationNumber) when `openid` scope requested
✅ **UserInfo Endpoint**: `/oauth2/userinfo` returns user profile claims with scope-based filtering (profile/email scopes)
✅ **Scope-Based Claims**: `profile` scope for given_name/family_name, `email` scope for email claims
✅ **RP-Initiated Logout**: `/oauth2/logout` for single sign-out flows
✅ **Backward Compatible**: Existing OAuth2 clients work unchanged (OIDC is opt-in via scope)

### ID Token vs Access Token

| Aspect           | ID Token                                 | Access Token                        |
|------------------|------------------------------------------|-------------------------------------|
| **Purpose**      | User Authentication                      | API Authorization                   |
| **Claims**       | `sub`, `auth_time`, `registrationNumber` | `registrationNumber`, `authorities` |
| **Scope**        | Requested when `openid` scope included   | Always included                     |
| **Usage**        | Frontend sessions, user identification   | API authorization checks            |
| **Profile Data** | Not included (minimal claims)            | Not included                        |

### UserInfo Endpoint: Scope-Based Claims

The UserInfo endpoint (`/oauth2/userinfo`) implements OIDC-compliant scope-based access control:

| Scope      | Claims Returned                                                      | Condition                           |
|------------|----------------------------------------------------------------------|-------------------------------------|
| `openid`   | `sub` (subject identifier)                                           | Always (required)                   |
| `profile`  | `given_name`, `family_name`, `registrationNumber`, `updated_at`      | Member entity exists                |
| `email`    | `email`, `email_verified`                                            | Member entity exists AND email != null |

**Behavior:**
- Claims are **omitted** (not returned as `null`) when data is unavailable (OIDC best practice)
- Admin users without Member entity return only `sub` claim regardless of scopes
- Members without email address omit email claims even when `email` scope is requested
- `email_verified` always returns `false` until email verification feature is implemented

**Example Responses:**

```json
// openid scope only
{
  "sub": "ZBM0501"
}

// openid + profile scopes
{
  "sub": "ZBM0501",
  "given_name": "Jan",
  "family_name": "Novák",
  "registrationNumber": "ZBM0501",
  "updated_at": "2026-02-09T12:00:00Z"
}

// openid + profile + email scopes (full profile)
{
  "sub": "ZBM0501",
  "given_name": "Jan",
  "family_name": "Novák",
  "registrationNumber": "ZBM0501",
  "updated_at": "2026-02-09T12:00:00Z",
  "email": "jan.novak@example.com",
  "email_verified": false
}
```

### OIDC Flow: Authorization Code with ID Token

```mermaid
sequenceDiagram
    participant Client as Web/Mobile Client
    participant Discovery as Discovery Endpoint
    participant AuthEndpoint as /oauth2/authorize
    participant TokenEndpoint as /oauth2/token
    participant UserInfoEndpoint as /oauth2/userinfo
    participant ResourceServer as API Server

    Client->>Discovery: GET /.well-known/openid-configuration
    Discovery-->>Client: OIDC metadata (issuer, endpoints, signing algorithms)

    Client->>AuthEndpoint: GET /oauth2/authorize?scope=openid members.read&response_type=code
    AuthEndpoint-->>Client: Redirect to login
    Client->>AuthEndpoint: POST /login (username/password)
    AuthEndpoint-->>Client: Redirect with authorization code

    Client->>TokenEndpoint: POST /oauth2/token<br/>grant_type=authorization_code<br/>code=AUTH_CODE<br/>scope=openid members.read
    TokenEndpoint-->>Client: 200 OK<br/>{<br/>  "access_token": "eyJhbG...",<br/>  "id_token": "eyJhbG...",<br/>  "token_type": "Bearer",<br/>  "expires_in": 900<br/>}

    Client->>UserInfoEndpoint: GET /oauth2/userinfo<br/>Authorization: Bearer ACCESS_TOKEN
    UserInfoEndpoint-->>Client: 200 OK<br/>{<br/>  "sub": "ZBM0501",<br/>  "given_name": "Jan",<br/>  "family_name": "Novák",<br/>  "registrationNumber": "ZBM0501",<br/>  "updated_at": "2026-02-09T12:00:00Z",<br/>  "email": "jan.novak@example.com",<br/>  "email_verified": false<br/>}

    Client->>ResourceServer: GET /api/members<br/>Authorization: Bearer ACCESS_TOKEN
    ResourceServer-->>Client: 200 OK (members list)
```

### ID Token Claims Structure

```json
{
  "iss": "https://localhost:8443",           // Issuer
  "sub": "user123",                          // Subject (user identifier)
  "aud": "klabis-web",                       // Audience (client_id)
  "exp": 1643547600,                         // Expiration time
  "iat": 1643544000,                         // Issued at
  "auth_time": 1643544000,                   // Authentication time (custom)
  "registrationNumber": "user123"            // Custom claim
}
```

### Configuration

**AuthorizationServerConfiguration.java**:

- `authorizationServerSettings()`: Sets issuer URL, enables OIDC
- `jwtCustomizer()`: Adds OIDC claims (`sub`, `auth_time`) for ID tokens
- Discovery endpoint automatically available when OIDC enabled

**BootstrapDataLoader.java**:

- `openid` scope added to default OAuth2 client

### Endpoints

| Endpoint                            | Method | Purpose                          | Requires Auth      |
|-------------------------------------|--------|----------------------------------|--------------------|
| `/.well-known/openid-configuration` | GET    | OIDC metadata                    | No                 |
| `/oauth2/authorize`                 | GET    | Start authorization flow         | No                 |
| `/oauth2/token`                     | POST   | Exchange code for tokens         | Client credentials |
| `/oauth2/userinfo`                  | GET    | Get user profile                 | Yes (access token) |
| `/oauth2/logout`                    | POST   | Logout / revoke tokens           | Yes                |
| `/oauth2/jwks`                      | GET    | JWK Set for signature validation | No                 |

---

## Security Best Practices Implemented

✅ **Stateless Authentication**: No server-side sessions, JWT in every request
✅ **Principle of Least Privilege**: Fine-grained authorities (MEMBERS:CREATE vs ROLE_ADMIN)
✅ **Defense in Depth**: Multiple layers (HTTP rules + method security)
✅ **Secure Password Storage**: BCrypt with salt
✅ **Asymmetric Cryptography**: RSA keys for JWT signing (private key never exposed)
✅ **Token Expiration**: Short-lived access tokens (15 min)
✅ **CSRF Protection**: Disabled for stateless API (not needed with JWT)
✅ **Standard Error Responses**: RFC 7807 Problem Detail format
✅ **Separation of Concerns**: Authorization Server ≠ Resource Server
✅ **JDBC-backed OAuth2**: Persistent storage of clients and authorizations

---

## References

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Authorization Server Reference](https://docs.spring.io/spring-authorization-server/reference/)
- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [JWT RFC 7519](https://tools.ietf.org/html/rfc7519)
- [RFC 7807 Problem Details](https://tools.ietf.org/html/rfc7807)

**Configuration Files**:

- `SecurityConfiguration.java` - Resource server and JWT configuration
- `AuthorizationServerConfiguration.java` - OAuth2 authorization server setup
- `KlabisUserDetailsService.java` - User authentication
- `V002__create_users_and_oauth2_tables.sql` - Database schema
