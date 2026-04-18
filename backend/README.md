# Klabis Backend API

Backend REST API for orienteering club management system with HATEOAS hypermedia controls.

## Table of Contents

- [Quick Start](#quick-start)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
    - [Generate SSL Certificate](#generate-ssl-certificate)
    - [Configuration](#configuration)
    - [Running the Application](#running-the-application)
    - [Building](#building)
    - [Testing](#testing)
- [API Quick Reference](#api-quick-reference)
- [Documentation](#documentation)
- [Troubleshooting](#troubleshooting)

## Quick Start

```bash
# 1. Generate development SSL certificate
./generate-dev-cert.sh

# 2. Set required environment variables
export KLABIS_ADMIN_USERNAME='admin'
export KLABIS_ADMIN_PASSWORD='admin123'
export KLABIS_OAUTH2_CLIENT_SECRET='test-secret-123'
export KLABIS_JASYPT_PASSWORD='test-key-123'

# 3. Run the application
./gradlew bootRun
```

API available at: `https://localhost:8443`

## Prerequisites

- **Java 21** or higher
- **Gradle 8.14+** (wrapper included)
- **PostgreSQL 14+** (for production profile)
- **SMTP server** (for email functionality)

## Getting Started

### Generate SSL Certificate

The application requires HTTPS. Generate a self-signed development certificate:

```bash
./generate-dev-cert.sh
```

**Generated certificate details:**

- **Location:** `keystore/dev-keystore.p12`
- **Password:** `changeit`
- **Format:** PKCS12
- **Validity:** 365 days

**Trust the certificate in your browser:**

1. Navigate to `https://localhost:8443`
2. Accept the security warning (development only)

### Configuration

Create a `.env` file from the example:

```bash
cp .env.example .env
```

**Required Environment Variables:**

```bash
# Encryption (REQUIRED for GDPR fields)
KLABIS_JASYPT_PASSWORD=your_secret_key_min_32_chars

# Bootstrap Admin User
KLABIS_ADMIN_USERNAME=admin
KLABIS_ADMIN_PASSWORD=admin123

# OAuth2 Client Secret
KLABIS_OAUTH2_CLIENT_SECRET=test-secret-123
```

**Optional Variables:**

```bash
# Database (PostgreSQL for production)
KLABIS_DB_USERNAME=klabis
KLABIS_DB_PASSWORD=your_secure_password

# SMTP Email
KLABIS_SMTP_HOST=smtp.example.com
KLABIS_SMTP_PORT=587
KLABIS_SMTP_USERNAME=your-smtp-user
KLABIS_SMTP_PASSWORD=your-smtp-password
KLABIS_EMAIL_FROM=noreply@klabis.cz
```

### Running the Application

**Development Mode (H2 in-memory database):**

```bash
./gradlew bootRun
```

**Production Mode (PostgreSQL):**

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

**Verify startup:**

```bash
# Health check
curl -k https://localhost:8443/actuator/health

# Expected response: {"status":"UP"}
```

**Available endpoints:**

- API: `https://localhost:8443/api`
- H2 Console (dev): `https://localhost:8443/h2-console`
- Actuator: `https://localhost:8443/actuator`
- Actuator Health: `https://localhost:8443/actuator/health`
- Actuator Modulith: `https://localhost:8443/actuator/modulith`

### Building

```bash
# Compile and package
./gradlew clean build

# Skip tests
./gradlew clean build -x test

# Create executable JAR
./gradlew bootJar
```

### Testing

```bash
# Run all tests
./gradlew test

# Run single test class
./gradlew test --tests "MemberTest"

# Run single test method
./gradlew test --tests "MemberTest.shouldCreateMemberWithValidData"
```

## API Quick Reference

### Authentication

The API uses **OAuth2** with JWT tokens.

**Default Credentials (Development Only):**

- **Admin User:** `admin` / `admin123` (ROLE_ADMIN, all authorities)
- **OAuth2 Client:** `klabis-web` / `test-secret-123`

⚠️ **Never use in production!**

### Quick Authentication Example

The authorization server only advertises the `authorization_code` flow with PKCE — tokens
cannot be fetched non-interactively via curl. Obtain an access token from the browser
(frontend at `http://localhost:3000` → DevTools → Application → session storage →
`oidc.user:...`) and export it:

```bash
export TOKEN=eyJhbGciOi...

curl -k -X POST https://localhost:8443/api/members \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jan",
    "lastName": "Novak",
    "gender": "MALE",
    "nationality": "CZ",
    "dateOfBirth": "1990-01-15",
    "email": "jan@test.cz",
    "phone": "+420123456789",
    "address": {
      "street": "Test 123",
      "city": "Praha",
      "postalCode": "11000",
      "country": "CZ"
    }
  }'
```

### Main Endpoints

| Endpoint                            | Method | Auth     | Description              |
|-------------------------------------|--------|----------|--------------------------|
| `/api/members`                      | POST   | Required | Create member            |
| `/api/members`                      | GET    | Required | List members (paginated) |
| `/api/members/{id}`                 | GET    | Required | Get member details       |
| `/api/members/{id}`                 | PATCH  | Required | Update member            |
| `/api/auth/password-setup/request`  | POST   | Public   | Request password setup   |
| `/api/auth/password-setup/validate` | GET    | Public   | Validate token           |
| `/api/auth/password-setup/complete` | POST   | Public   | Complete password setup  |

**For complete API specification, see** [../docs/openapi/klabis-full.json](../docs/openapi/klabis-full.json).

## Documentation

- **[docs/README.md](docs/README.md)** - Documentation index + curated external references
- **[docs/EVENT-DRIVEN-ARCHITECTURE.md](docs/EVENT-DRIVEN-ARCHITECTURE.md)** - Spring Modulith event flows and outbox pattern
- **[../docs/openapi/klabis-full.json](../docs/openapi/klabis-full.json)** - OpenAPI specification
- **[CLAUDE.md](CLAUDE.md)** - Build, test, run, security extension points, profiles
- Day-to-day backend conventions (aggregates, controllers, HATEOAS, JDBC mementos, field-level auth, testing) live in the `backend-patterns` skill, not in this repo

## Troubleshooting

### Server won't start

**Problem:** Port 8443 already in use

```bash
# Check what's using the port
lsof -i :8443

# Kill the process
kill -9 <PID>
```

**Problem:** SSL certificate errors

```bash
# Regenerate certificate
rm keystore/dev-keystore.p12
./generate-dev-cert.sh
```

### Authentication failures

**Problem:** "Invalid credentials"

```bash
# Check bootstrap data loaded
grep "Bootstrap data" /path/to/server.log

# Verify environment variables set
echo $KLABIS_ADMIN_PASSWORD
echo $KLABIS_OAUTH2_CLIENT_SECRET
```

### Email not sending

**Problem:** Email failures in logs

- Check SMTP configuration in `.env`
- Verify SMTP server is accessible
- Email failures don't break business operations (graceful degradation)

**Local email testing with MailHog:**

```bash
docker compose up mailhog -d
```

Start the backend with the `email` profile — sent emails are captured and visible at http://localhost:8025 (no actual delivery).

### Event processing issues

**Problem:** Events not processing

```bash
# Check event publication status
curl -k https://localhost:8443/actuator/modulith
```

**For more troubleshooting:**

- [CLAUDE.md](CLAUDE.md) - Development guidelines and common pitfalls

## Development Guidelines

For development guidelines, coding conventions, and testing strategies, see [CLAUDE.md](CLAUDE.md).

### Commit Convention

Follow Conventional Commits:

```
feat(members): add member registration endpoint
fix(email): correct welcome email template
test(members): add tests for registration number generation
docs(api): update authentication examples
refactor(users): extract password validation to service
```

## Links

- **OpenSpec Specifications:** `../openspec/`
- **Frontend Repository:** (separate repository)

## Related readings

- [DDD with jMolecules](https://ersantana.com/Software-Architecture/jmolecules/jmolecules-ddd-annotations-guide)
- [Hexagonal architecture with jMolecules](https://ersantana.com/Software-Architecture/jmolecules/jmolecules-hexagonal-architecture-guide)

## License

Proprietary - Klabis Orienteering Club Management System
