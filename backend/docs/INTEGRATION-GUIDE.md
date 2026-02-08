# Integration Guide

## Overview

This document describes how the Klabis Backend integrates with external systems and services, including email, OAuth2,
and event-driven communication.

## Table of Contents

1. [Email Service](#email-service)
2. [Event-Driven Integration](#event-driven-integration)
3. [OAuth2 Integration](#oauth2-integration)
4. [External API Integration Guidelines](#external-api-integration-guidelines)

---

## Email Service

### Template Engine

**Framework:** Thymeleaf

**Template Location:** `src/main/resources/templates/email/`

**Template Files:**

- `password-setup.html` - Password setup email (HTML version)
- `password-setup.txt` - Password setup email (plain text version)

### Email Templates

#### Password Setup Email

**Template Variables:**

- `firstName` - Member's first name
- `setupUrl` - Complete password setup URL with token
- `expirationHours` - Token validity period in hours
- `clubName` - Club name for branding

**HTML Template Example:**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <h1>Welcome, <span th:text="${firstName}">Member</span>!</h1>
    <p>You have been registered to <span th:text="${clubName}">Club</span>.</p>
    <p><a th:href="${setupUrl}">Set your password</a></p>
    <p>This link expires in <span th:text="${expirationHours}">4</span> hours.</p>
</body>
</html>
```

### Email Configuration

**application.yml:**

```yaml
spring:
  mail:
    host: ${SMTP_HOST:smtp.example.com}
    port: ${SMTP_PORT:587}
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    protocol: smtp
    tls: true
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000

# Email settings
email:
  from: ${EMAIL_FROM:noreply@klabis.example}
  activation:
    token-validity-hours: 72
    base-url: ${EMAIL_BASE_URL:https://localhost:8443}
```

**Environment Variables:**

```bash
# SMTP Configuration
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=your-smtp-user
SMTP_PASSWORD=your-smtp-password

# Email Settings
EMAIL_FROM=noreply@klabis.example
EMAIL_BASE_URL=https://localhost:8443
```

### Email Service Implementation

**Service Class:**

```java
@Service
public class EmailService {

    private final SpringTemplateEngine templateEngine;
    private final JavaMailSender mailSender;

    public void sendPasswordSetupEmail(Member member, String setupUrl) {
        // Prepare template variables
        Context context = new Context();
        context.setVariable("firstName", member.getFirstName());
        context.setVariable("setupUrl", setupUrl);
        context.setVariable("expirationHours", 4);
        context.setVariable("clubName", "Klub orientačního běhu");

        // Render HTML and text versions
        String htmlContent = templateEngine.process("email/password-setup", context);
        String textContent = templateEngine.process("email/password-setup", context);

        // Send multipart email
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(member.getEmail().value());
        helper.setSubject("Set Your Password");
        helper.setText(htmlContent, true); // HTML

        mailSender.send(message);
    }
}
```

### Graceful Failure Handling

**Principle:** Email service failures must not break business operations

**Implementation:**

```java
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendPasswordSetupEmail(Member member, String setupUrl) {
        try {
            // Attempt to send email
            MimeMessage message = buildEmail(member, setupUrl);
            mailSender.send(message);

            log.info("Password setup email sent successfully to registration: {}",
                    member.getRegistrationNumber());

        } catch (MailAuthenticationException e) {
            // SMTP authentication failure
            log.error("Failed to send password setup email - SMTP authentication failed. Registration: {}",
                     member.getRegistrationNumber());

        } catch (MailSendException e) {
            // SMTP connection failure or invalid address
            log.error("Failed to send password setup email - SMTP error. Registration: {}, Message: {}",
                     member.getRegistrationNumber(), e.getMessage());

        } catch (Exception e) {
            // Template rendering or other error
            log.warn("Failed to render password setup email template. Registration: {}",
                     member.getRegistrationNumber());

            try {
                sendPlainFallbackEmail(member, setupUrl);
            } catch (Exception fallbackException) {
                log.error("Failed to send plain text fallback email. Registration: {}",
                         member.getRegistrationNumber());
            }
        }
    }
}
```

**Key Principles:**

- Never throw exceptions from email service
- Log all failures (but no PII in logs - use registration number only)
- Attempt graceful fallback (plain-text if template fails)
- Business operations succeed even if email fails

### Email Testing

**Integration Test:**

```java
@SpringBootTest
@AutoConfigureMockMvc
class EmailServiceIntegrationTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void shouldSendPasswordSetupEmail() {
        // Given
        Member member = createTestMember();
        String setupUrl = "https://localhost:8443/password-setup?token=abc123";

        // When
        emailService.sendPasswordSetupEmail(member, setupUrl);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }
}
```

---

## Event-Driven Integration

The Klabis Backend uses event-driven communication for cross-module integration:

**Key Features:**

- **Transactional Safety:** Events persisted atomically with business data
- **Asynchronous:** Doesn't block API responses
- **Separate Transactions:** Event failures don't roll back business operations
- **Guaranteed Delivery:** Outbox pattern ensures no lost events
- **Automatic Retry:** Failed handlers are retried indefinitely

**For complete event-driven architecture documentation**, including event handler patterns, configuration, and
monitoring, see [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md)

---

## OAuth2 Integration

### OAuth2 Flows Supported

**Client Credentials Flow:**

- Used for machine-to-machine communication
- No user context
- Returns access token with client authorities

**Authorization Code Flow:**

- Used for user-facing applications
- Requires user authentication
- Returns access token with user authorities

**Resource Owner Password Flow:**

- Legacy flow (use authorization code instead)
- Direct username/password exchange
- Less secure than authorization code

### OAuth2 Endpoints

**Token Endpoint:** `POST /oauth2/token`
**Authorization Endpoint:** `GET /oauth2/authorize`
**JWKS Endpoint:** `GET /.well-known/jwks.json`
**Introspection Endpoint:** `POST /oauth2/introspect`

### Client Configuration

**Database Table:** `oauth2_registered_client`

**Default Clients:**

- `klabis-web` - Web application (authorization code flow)
- `klabis-web` - UI mock application (testing)

**Bootstrap Data:**

```java
// Created on application startup
RegisteredClient klabisWeb = RegisteredClient.withId("klabis-web")
    .clientId("klabis-web")
    .clientSecret("{bcrypt}hashedSecret")
    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
    .redirectUri("https://localhost:8443/auth/callback.html")
    .scope("members.read")
    .scope("members.write")
    .build();
```

### JWT Token Customization

**Custom Claims:**

```java
OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer = context -> {
    if (context.getTokenType().getValue().equals("access_token")) {
        Authentication principal = context.getPrincipal();

        // Add custom claims
        claims.claim("registrationNumber", principal.getName());
        claims.claim("authorities", principal.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList());
    }
};
```

**JWT Payload Example:**

```json
{
  "sub": "ZBM0001",
  "registrationNumber": "ZBM0001",
  "authorities": ["MEMBERS:CREATE", "MEMBERS:READ"],
  "scope": "members.read members.write",
  "iat": 1706204200,
  "exp": 1706205100,
  "jti": "abc-123-def"
}
```

**See:** [SPRING_SECURITY_ARCHITECTURE.md](./SPRING_SECURITY_ARCHITECTURE.md) for complete OAuth2 details

---

## External API Integration Guidelines

### Best Practices

#### 1. Use Dedicated Service Classes

```java
@Service
public class ExternalApiService {

    private final RestTemplate restTemplate;
    private final ExternalApiConfig config;

    public ExternalApiResponse callExternalApi(ExternalApiRequest request) {
        // Encapsulate external API logic
        // Handle errors gracefully
        // Log requests/responses appropriately
    }
}
```

#### 2. Configure Timeouts

```yaml
external:
  api:
    base-url: https://api.example.com
    connect-timeout: 5000  # 5 seconds
    read-timeout: 10000    # 10 seconds
```

```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplateBuilder()
        .setConnectTimeout(Duration.ofMillis(5000))
        .setReadTimeout(Duration.ofMillis(10000))
        .build();
}
```

#### 3. Use Circuit Breakers

```java
@Component
public class ExternalApiService {

    @CircuitBreaker(name = "externalApi", fallbackMethod = "fallback")
    public ExternalApiResponse callApi(ExternalApiRequest request) {
        // Call external API
    }

    private ExternalApiResponse fallback(ExternalApiRequest request, Exception ex) {
        // Return cached data or default response
    }
}
```

#### 4. Implement Retry Logic

```java
@Component
public class ExternalApiService {

    @Retry(name = "externalApi", maxAttempts = 3)
    public ExternalApiResponse callApi(ExternalApiRequest request) {
        // Call external API with automatic retry
    }
}
```

#### 5. Validate External Data

```java
@Service
public class ExternalApiService {

    public ExternalApiResponse callApi(ExternalApiRequest request) {
        ExternalApiResponse response = restTemplate.postForObject(
            config.getBaseUrl(),
            request,
            ExternalApiResponse.class
        );

        // Validate response
        if (response == null || !response.isValid()) {
            throw new ExternalApiException("Invalid response from external API");
        }

        return response;
    }
}
```

### Error Handling

**Never let external API failures break business operations:**

```java
@Service
public class ExternalApiService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);

    public void syncDataToExternalApi(Data data) {
        try {
            externalApiClient.sync(data);
            log.info("Successfully synced data to external API");

        } catch (HttpClientErrorException.UnprocessableEntity e) {
            // Business logic error (validation failed)
            log.warn("External API rejected data: {}", e.getResponseBodyAsString());

        } catch (HttpClientErrorException e) {
            // Client error (404, 401, etc.)
            log.error("External API client error: {} - {}",
                     e.getStatusCode(), e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            // Server error (500, 503, etc.) - use circuit breaker
            log.error("External API server error: {} - {}",
                     e.getStatusCode(), e.getResponseBodyAsString());

        } catch (RestClientException e) {
            // Network error, timeout - use circuit breaker
            log.error("External API network error: {}", e.getMessage());
        }
        // Business operation continues regardless of API failure
    }
}
```

### Logging Best Practices

**What to Log:**

- Request/response summaries (not full payloads for PII)
- Success/failure status
- Error messages from external APIs
- Timing metrics

**What NOT to Log:**

- Full request/response payloads (may contain PII)
- Sensitive data (passwords, tokens, personal data)
- Authentication credentials

**Example:**

```java
log.info("Called external API: method={}, endpoint={}, status={}, duration={}ms",
         "POST", "/api/v1/sync", "200", 123);
```

---

## Related Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Overall architecture overview
- [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md) - Spring Modulith and outbox pattern
- [SPRING_SECURITY_ARCHITECTURE.md](./SPRING_SECURITY_ARCHITECTURE.md) - OAuth2 and security
- [DOMAIN-MODEL.md](./DOMAIN-MODEL.md) - Domain events and aggregates

---

**Last Updated:** 2026-01-26
**Status:** Active
