# Page inventory — mapa stránka → témata → zdroj

Tabulka pro identifikaci, která stránka manuálu se týká které části kódu. Při režimu 2 (úprava) i 3 (sync) tě tato tabulka navede přesně.

## Stránky a jejich obsah

### `00-prerequisites.html` — Předpoklady
Onboarding pro vývojáře bez Spring/DDD znalostí. Žádný projektový kód — jen odkazy na oficiální dokumentaci.

**Aktualizuj když:** se přidá nová používaná knihovna nebo se zásadně povýší verze (Spring Boot major upgrade, React major upgrade).

### `01-architecture.html` — Architektura projektu
- Modulární monolit, seznam modulů
- Hexagonální struktura (`domain` / `application` / `infrastructure`)
- jMolecules anotace
- Pravidla závislostí
- **Tabulka subpackages `common/` a v jaké kapitole jsou popsány**

**Zdroj:** `backend/src/main/java/com/klabis/*/package-info.java`, `backend/CLAUDE.md`

**Aktualizuj když:** přibude/zanikne modul, změní se package struktura, vytvoří se nový subpackage v `common/` (← aktualizuj tabulku).

### `02-domain.html` — Doménová vrstva
- `KlabisAggregateRoot<A, ID>` — base třída, audit, equality, domain events
- `AuditMetadata` — record + helpery
- Type-safe IDs (Identifier interface)
- Value objects, doporučení
- Domain events
- Repository interface (jen tvar, implementace v 04)
- `EncryptedString` jako value object (typ)

**Zdroj:** `backend/src/main/java/com/klabis/common/domain/`, `backend/src/main/java/com/klabis/common/encryption/EncryptedString.java`

### `03-services.html` — Aplikační vrstva (services)
- Konvence application services (@PrimaryPort, inner records, @Transactional, naming)
- Sdílené services v `common.users`: `UserService`, `PermissionService`, `PasswordSetupService`, `AuthorizationQueryService`
- `EncryptionService` (mimo persistenci)

**Zdroj:** `backend/src/main/java/com/klabis/common/users/UserService.java`, `common/users/application/*Service.java`, `common/encryption/EncryptionService.java`, `members/application/*Port.java` (jako příklady konvence)

### `04-persistence.html` — Perzistence
- `JdbcConfiguration` — auditing, custom converters
- Memento pattern — `Persistable<ID>`, `@DomainEvents`, `@AfterDomainEventPublication`
- Repository pattern (interface v domain, adapter v infrastructure, JdbcRepository)
- `TranslatedPageable`
- Custom queries s `JdbcAggregateTemplate`
- Migrace (V001, V002, V003)

**Zdroj:** `backend/src/main/java/com/klabis/common/jdbc/`, `common/pagination/TranslatedPageable.java`, `members/infrastructure/jdbc/MemberMemento.java`, `members/infrastructure/jdbc/MemberRepositoryAdapter.java`

### `05-rest-api.html` — REST API a HAL+FORMS
- HAL+FORMS principy
- `klabisLinkTo`, `klabisAfford`, `klabisAffordWithOptions`, `entityModelWithDomain`
- `@HalForms` annotation + `Access` enum
- `RootController` a navigace
- `EntityModelWithDomain` + `ModelWithDomainPostprocessor`
- `PatchField<T>` framework
- `@ValidOptionalSize`
- `MvcExceptionHandler` mapování výjimek na HTTP statusy
- `HateoasConfiguration`, `MvcConfiguration`, `SpaFallbackController`, `@MvcComponent`
- **Aktuální uživatel:** `@ActingUser`, `@ActingMember`, `CurrentUserData`, `CurrentUserArgumentResolver`

**Zdroj:** `backend/src/main/java/com/klabis/common/ui/`, `common/hateoas/`, `common/mvc/`, `common/patch/`, `common/validation/`, `members/ActingUser.java`, `members/ActingMember.java`, `members/CurrentUserData.java`, `members/infrastructure/authorizationserver/CurrentUserArgumentResolver.java`

### `06-security.html` — Bezpečnost
- OAuth2 AS + RS přehled
- JWT s vlastními claims (user_id, memberIdUuid, authorities, registrationNumber)
- `KlabisJwtAuthenticationToken`
- `Authority` enum (vč. Scope GLOBAL/CONTEXT_SPECIFIC)
- `@HasAuthority` (method-level)
- `@OwnerVisible` + `@OwnerId` (field-level)
- `OwnershipResolver`
- Pomocné komponenty: `AccountStatusValidationFilter`, `CorsConfiguration`, `FrontendProperties`, `PasswordEncoderConfiguration`
- Custom AuthenticationEntryPoint
- **OAuth2 customizace v `members`:** `KlabisAuthorizationServerCustomizer`, `KlabisUserDetailsService`, `MemberIdToUuidConverter`

**Zdroj:** `backend/src/main/java/com/klabis/common/security/`, `common/security/fieldsecurity/`, `common/users/Authority.java`, `common/users/HasAuthority.java`, `members/infrastructure/authorizationserver/KlabisAuthorizationServerCustomizer.java`, `members/infrastructure/authorizationserver/KlabisUserDetailsService.java`, `members/infrastructure/authorizationserver/MemberIdToUuidConverter.java`

### `07-common.html` — Common — sdílené stavební bloky
- `UserGroup` building block — kompozice, invariants
- `GroupMembership`
- `WithInvitations` interface
- `Invitation` entity
- Persistence sdíleného UserGroup (zmínka)
- `EmailService`, `JavaMailEmailService`, `LoggingEmailService`
- `EmailMessage`
- `TemplateRenderer`, `Template`, `ThymeleafTemplateRenderer`
- Hierarchie doménových výjimek (`BusinessRuleViolationException`, `InvalidDataException`, `AuthorizationException`, `InsufficientAuthorityException`, `ResourceNotFoundException`, `MemberProfileRequiredException`)

**Zdroj:** `backend/src/main/java/com/klabis/common/usergroup/`, `common/email/`, `common/templating/`, `common/exceptions/`

### `08-modules.html` — Spring Modulith a moduly
- Publikace eventu (`registerEvent`)
- `@ApplicationModuleListener`
- Transactional outbox (event_publication tabulka, completion-mode, republish, cleanup)
- Idempotence
- Konvence event flow
- Observability — Modulith metriky, `CustomMetricsConfiguration`, `CustomMetricsTrackingAspect`
- Verifikace pravidel závislostí (`ApplicationModules.verify()`)

**Zdroj:** `backend/src/main/java/com/klabis/common/observability/`, `common/users/infrastructure/listeners/UserCreatedEventHandler.java` (jako příklad), `backend/docs/EVENT-DRIVEN-ARCHITECTURE.md`

### `09-cross-cutting.html` — Cross-cutting concerns
- Bootstrap dat (`BootstrapDataInitializer`, `BootstrapDataLoader`)
- MDC logging (`RequestLoggingFilter`)
- `PerKeyRateLimiter`

**Zdroj:** `backend/src/main/java/com/klabis/common/bootstrap/`, `common/logging/`, `common/ratelimit/`

### `10-testing.html` — Testování
- Pyramidový přehled (odkaz na docs/testing-pyramid.md)
- `@WithKlabisMockUser`
- `@E2ETest` meta-annotation
- `@ApplicationModuleTest` + Scenario API
- Custom AssertJ asserts (`MemberAssert`, `UserAssert`, `EventAssert`, `CalendarItemAssert`)
- Memento round-trip test
- Test data buildery
- Spouštění (test-runner skill)
- Gotcha: `@WebMvcTest` + UserService

**Zdroj:** `backend/src/test/java/com/klabis/common/WithKlabisMockUser.java`, `backend/src/test/java/com/klabis/E2ETest.java`, `backend/src/test/java/com/klabis/**/*Assert.java`

### `11-frontend.html` — Frontend
- Tabulka klíčových stavebních bloků
- Práce s HAL+FORMS response (`getLink`, `getLinkHref`, `followLink`, `getEmbedded`)
- `useHalPageData` hook
- Formuláře — `HalFormButton`, `HalFormsSection`, `HalFormsPageLayout`, `HalFormDisplay`, `HalFormTemplateButton`, `MultiStepFormModal`
- Modal vs inline formulář
- Custom layout (children pattern, callback pattern)
- `KlabisTable` + `<TableCell>`
- Customizace stránky (GenericHalPage vs custom)
- Lokalizace (`labels.ts`, `getFieldLabel`, `getTemplateLabel`, `getNavLabel`, `getEnumLabel`)
- Autentizace (`klabisUserManager`, `authorizedFetch`, `useAuthorizedQuery`/`useAuthorizedMutation`, `useKlabisApiQuery`/`useKlabisApiMutation`)
- Lokální dev (port 3000 vs 8443)

**Zdroj:** `frontend/src/api/`, `frontend/src/hooks/`, `frontend/src/contexts/`, `frontend/src/components/HalNavigator2/`, `frontend/src/components/KlabisTable/`, `frontend/src/localization/`, `frontend/CLAUDE.md`, `frontend/src/components/HalNavigator2/COMPONENTS.md`, `frontend/src/components/HalNavigator2/MULTI_STEP_FORM.md`

### `12-claude-workflow.html` — Práce s Claude Code
- Filozofie projektu (specifikace první, konvence ve skillech, TDD a malé iterace)
- OpenSpec workflow (struktura, typický cyklus změny)
- Skills (projektové i marketplace)
- Agenti (`backend-developer`, `frontend-developer`, `git-operator`)
- Slash commands
- Postup pro feature, refactoring
- Lokální spouštění
- Sandbox a ostatní pravidla
- `/mnt/ramdisk/klabis/` pro temp soubory

**Zdroj:** `.claude/agents/*.md`, `.claude/commands/*.md`, `.claude/skills/`, `openspec/config.yaml`, `CLAUDE.md`, `backend/CLAUDE.md`

## Speciální soubory

### `index.html`
Obsah s linky na všechny stránky + krátké description. **Vždy aktualizuj při přečíslování nebo přejmenování stránek.**

### `style.css`
Kanonický stylesheet. Žádná stránka nemá vlastní styly. Pokud měníš style.css, vždy aktualizuj `references/html-conventions.md` v skillu i komentářové vocabulary v samotném CSS.

## Reverse mapa: zdrojový soubor → stránka manuálu

Pro režim 3 (sync) je užitečné rychle najít, kterou stránku změnit když se mění konkrétní zdrojový soubor. Když narazíš na změnu, projdi tabulku stránek výše a hledej, ve kterých sekcích "Zdroj" je tvůj soubor uveden. Soubor může být relevantní pro **více** stránek (např. `Authority.java` je relevantní pro 06-security i jako součást API v 03-services).
