# Scope — co patří/nepatří do manuálu

Manuál pokrývá **framework features** Klabisu — sdílené stavební bloky, infrastrukturu a Klabis-specific patterny, které vývojář potřebuje při psaní libovolné nové feature. Cíleně **vynechává** byznys-logiku jednotlivých modulů.

## Co PATŘÍ do manuálu

### Backend

Vše v `backend/src/main/java/com/klabis/common/` — celý package:

- `common.bootstrap` — BootstrapDataInitializer SPI
- `common.domain` — KlabisAggregateRoot, AuditMetadata
- `common.email` — EmailService, EmailMessage
- `common.encryption` — EncryptedString, EncryptionService
- `common.exceptions` — hierarchie doménových výjimek
- `common.hateoas` — HateoasConfiguration
- `common.jdbc` — JdbcConfiguration, custom converters
- `common.logging` — RequestLoggingFilter (MDC)
- `common.mvc` — MvcExceptionHandler, MvcComponent, MvcConfiguration
- `common.observability` — Custom metriky pro Modulith
- `common.pagination` — TranslatedPageable
- `common.patch` — PatchField, PatchFieldDeserializer
- `common.ratelimit` — PerKeyRateLimiter
- `common.security` (vč. `fieldsecurity`) — SecurityConfiguration, Authority, OwnerVisible, OwnerId, KlabisJwtAuthenticationToken, OwnershipResolver
- `common.templating` — TemplateRenderer
- `common.ui` — HalFormsSupport, RootController, EntityModelWithDomain, SpaFallbackController
- `common.usergroup` — UserGroup, GroupMembership, Invitation, WithInvitations
- `common.users` — Authority enum, HasAuthority, UserId, UserService, PermissionService, PasswordSetupService, AuthorizationQueryService, User aggregate
- `common.validation` — @ValidOptionalSize

### Framework-like části `members` modulu

Tyto věci nejsou byznys o členech, jsou to infrastructural extension points:

- `members/ActingUser.java` — anotace pro injection
- `members/ActingMember.java` — anotace pro injection
- `members/CurrentUserData.java` — DTO s aktuálním uživatelem
- `members/infrastructure/authorizationserver/` — celý package:
  - `KlabisAuthorizationServerCustomizer` — wirne JWT claims
  - `KlabisUserDetailsService` — bridge mezi User a Spring Security
  - `MemberIdToUuidConverter` — Spring ConversionService converter
  - `CurrentUserArgumentResolver` — implementuje @ActingUser/@ActingMember

### Frontend

Vše frameworkové v `frontend/src/`:

- `src/api/` — setup, hateoas utils, klabisUserManager (OIDC), authorizedFetch, generated types
- `src/contexts/` — HalRouteContext, HalFormContext, AuthContext2, AdminModeContext
- `src/hooks/` — useHalPageData, useHalActions, useHalFormData, useAuthorizedFetch a další
- `src/components/HalNavigator2/` — všechny HAL+FORMS komponenty
- `src/components/KlabisTable/` — tabulková komponenta
- `src/components/UI/` — generic UI knihovna (Button, Modal, Alert, Spinner, Toast)
- `src/localization/` — labels.ts, getFieldLabel, getTemplateLabel, getNavLabel, getEnumLabel
- `src/utils/` — sdílené utility (halFormsUtils, navigationPath)

### Cross-cutting konvence

- Modulární monolit (Spring Modulith) — pravidla závislostí, hexagonální struktura
- Testovací patterny — @WithKlabisMockUser, @E2ETest, custom AssertJ asserts, memento round-trip
- OpenSpec workflow + Claude Code skills/agents/commands

## Co NEPATŘÍ do manuálu

### Backend byznys-logika modulů

Ignoruj cokoli v těchto package, kromě explicitně uvedených výjimek výše:

- `members/domain/` — Member aggregate, registrace, RegistrationNumber, BirthNumber atd.
- `members/application/` — RegistrationService, ManagementService (až na zmínku v rámci konvencí)
- `members/infrastructure/restapi/` — MemberController atd. (slouží jen jako příklad)
- `members/infrastructure/jdbc/` — MemberMemento (slouží jen jako příklad)
- `members/infrastructure/listeners/` — BirthNumberAuditService a podobné
- `members/infrastructure/bootstrap/` — MembersDataBootstrap (až na zmínku jako příklad bootstrapu)
- `events/`, `calendar/`, `groups/`, `oris/` — celé moduly jsou byznys
- Jednotlivá business specs v `openspec/specs/`

### Frontend byznys-logika

- `src/pages/` — konkrétní stránky modulů
- `src/components/members/`, `src/components/events/` — modulové komponenty
- Konkrétní use case komponenty

### Ostatní

- README.md soubory (existují odděleně)
- IntelliJ setup (`docs/INTELLIJ_SETUP.md` existuje)
- Smoke testy (`docs/smoke-test.md` existuje)
- Detailní testovací pyramida (`docs/testing-pyramid.md` existuje — manuál se na ni jen odkazuje)
- OpenSpec specifikace (samostatný systém)
- DB migrace, Flyway scripty (jen zmíníme jejich existenci)

## Rozhodovací heuristika pro režim 3 (sync)

Když projíždíš git diff a najdeš změnu, ptej se:

1. **Je to v `common/` nebo v explicitně uvedeném podseznamu z `members/`?** Ne → ignoruj.
2. **Mění to API, které jsem v manuálu uváděl?** (signatura metody, přidání/odebrání anotace, nový enum value, nová třída) → aktualizuj.
3. **Je to jen interní implementační detail?** (refactor private metody, rename lokální proměnné, zlepšení error message) → ignoruj.
4. **Přidává to úplně novou framework feature?** (nový subpackage v `common/`) → konzultuj s uživatelem, kam ji přidat.
5. **Mění to convention, kterou manuál popisuje?** (např. změna naming patternu, nová best practice) → aktualizuj sekci konvencí.

Když si nejsi jistý, raději zmiň uživateli a nechej ho rozhodnout. Lepší zeptat se než nasypat do manuálu nepodstatnost nebo přehlédnout změnu.
