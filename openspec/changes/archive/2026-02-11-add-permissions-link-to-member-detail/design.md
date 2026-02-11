## Context

**Current State:**
- Member detail API (`GET /api/members/{id}`) vrací `MemberDetailsResponse` s `id` fieldem, který je UserId
- Permission management API existuje na `GET/PUT /api/users/{userId}/permissions`
- Frontend musí znát URL strukturu pro přístup k permissions (porušení HATEOAS)
- Member a User jsou oddělené agregáty (1:1 relace), Member.id je přímo UserId
- PermissionController je již public (viditelný z members modulu)

**Constraints:**
- HATEOAS mandatory - všechny response musí obsahovat navigační linky
- Spring Modulith dependency direction: members → users (OK), ale ne naopak
- RepresentationModelProcessor precedent existuje (`MembersRootPostprocessor`)
- Member ↔ User je 1:1, každý Member má vždy User

**Stakeholders:**
- Frontend team: potřebuje discoverable permission management link
- Backend developers: minimální change surface, čisté oddělení agregátů

## Goals / Non-Goals

**Goals:**
- Přidat HATEOAS link na permissions endpoint do Member detail response
- Link je conditional - zobrazí se pouze pokud má user `MEMBERS:PERMISSIONS` authority
- Zachovat clean architecture a agregátové hranice
- Type-safe linkTo() místo hardcoded URLs
- Použít existující `member.id` field (který je UserId) bez přidávání redundantního userId

**Non-Goals:**
- Embedding permission data do Member response (zbytečný overhead)
- Změna Permission API contract
- Změna Member DTOs/Response (member.id už je UserId, není třeba duplikovat)

## Decisions

### Decision 1: Použít existující member.id místo separátního userId

**Rozhodnutí:** Použít existující `MemberDetailsResponse.id` field (který je UserId) přímo v processoru. NEPŘIDÁVAT redundantní userId field.

**Alternativy:**
1. **Přidat separátní userId field do DTOs** ❌
   - Redundantní: member.id už je UserId (1:1 relace)
   - Porušuje DRY princip
   - Zbytečně zvětšuje API surface

2. **Query userId dynamicky v processoru** ❌
   - Extra database query při každém GET request
   - Performance overhead

3. **Použít existující id field** ✅ (zvoleno)
   - Zero redundance: member.id = userId
   - Žádné DTO změny potřeba
   - Clean: response.id() → permissions link

**Rationale:** Member.id JE UserId (1:1 relace), takže nepotřebujeme separátní field. Processor může přímo používat `response.id()`.

### Decision 2: RepresentationModelProcessor pattern

**Rozhodnutí:** Použít Spring HATEOAS `RepresentationModelProcessor` pro přidání linku místo inline logic v controlleru.

**Alternativy:**
1. **Link přímo v MemberController.getMember()** ❌
   - Controller ví o Permission API
   - Těžší testování
   - Porušuje single responsibility

2. **RepresentationModelProcessor** ✅ (zvoleno)
   - Separation of concerns
   - Precedent: `MembersRootPostprocessor`
   - Testovatelné izolovaně
   - Spring automatically applies processor

**Rationale:** Processor pattern je Spring HATEOAS best practice pro cross-cutting link enrichment.

### Decision 3: Package placement - members.management

**Rozhodnutí:** `MemberPermissionsLinkProcessor` v package `com.klabis.members.management`

**Alternativy:**
1. **common.hateoas** ❌
   - Porušuje Spring Modulith: common nesmí záviset na members/users

2. **users.integration** ❌
   - Reverse dependency: users → members (zakázáno)

3. **members.management** ✅ (zvoleno)
   - Legální závislost: members → users
   - Blízko MemberController
   - PermissionController je již public

**Rationale:** Jediná možnost respektující Spring Modulith dependency direction.

### Decision 4: Conditional link podle authority

**Rozhodnutí:** Link se zobrazí pouze pokud SecurityContext obsahuje `MEMBERS:PERMISSIONS` authority.

```java
private boolean hasMembersPermissionsAuthority() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
        return false;
    }
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(authority -> authority.equals("MEMBERS:PERMISSIONS"));
}
```

**Rationale:**
- HATEOAS principle: linky reprezentují dostupné akce
- Pokud user nemůže upravovat permissions, link by neměl být přítomen
- Frontend může použít přítomnost linku pro zobrazení UI prvků

## Risks / Trade-offs

### Risk 1: Cross-module coupling (members → users)

**Risk:** Members modul závisí na PermissionController z Users modulu.

**Mitigation:**
- Závislost je již přítomna (members závisí na users pro UserId)
- PermissionController je public REST API (stabilní interface)
- Pouze import pro linkTo() - není runtime coupling
- Spring Modulith dovoluje members → users direction

### Risk 2: Žádná změna API contract

**Risk:** None - pouze přidání HATEOAS linku, žádná změna struktury.

**Mitigation:**
- **Výhradně additive change** - přidání linku do `_links`
- Existující klienti ignorují nové linky (HAL+JSON standard)
- Zero breaking changes

### Risk 3: Security check v processoru

**Risk:** Processor provádí security check mimo controller security layer.

**Mitigation:**
- Pouze pro zobrazení linku, ne pro autorizaci akce
- Samotný Permission endpoint má vlastní security (`@HasAuthority`)
- Worst case: link se zobrazí, ale endpoint vrátí 403 (defense in depth)

### Trade-off: Conditional link overhead

**Trade-off:** Security check v processoru při každém GET request.

**Accepted because:**
- SecurityContext lookup je fast (ThreadLocal)
- Authority check je O(n) kde n = malý počet authorities (~3-5)
- Overhead < 1ms
- Benefit: Správná HATEOAS semantika > minimální overhead

## Data Flow Diagram

```mermaid
sequenceDiagram
    participant Client
    participant MemberController
    participant ManagementService
    participant Member
    participant Processor as MemberPermissionsLinkProcessor
    participant Security as SecurityContext

    Client->>MemberController: GET /api/members/{id}
    MemberController->>ManagementService: getMember(id)
    ManagementService->>Member: load from repository
    Member-->>ManagementService: Member entity
    ManagementService->>ManagementService: mapToMemberDetailsDTO()
    ManagementService-->>MemberController: MemberDetailsDTO
    MemberController->>MemberController: mapToResponse(dto)
    MemberController->>MemberController: EntityModel.of(response)
    MemberController-->>Processor: EntityModel<MemberDetailsResponse>
    Processor->>Processor: extract id from response<br/>(id = userId, protože 1:1 relace)
    Processor->>Security: getAuthentication()
    Security-->>Processor: authorities
    alt has MEMBERS:PERMISSIONS
        Processor->>Processor: add permissions link<br/>using response.id()
    end
    Processor-->>Client: HAL+JSON with _links
```

## Implementation Order

1. **Create RepresentationModelProcessor** (TDD: write test first)
   - `MemberPermissionsLinkProcessor`
   - Conditional link logic based on MEMBERS:PERMISSIONS authority
   - Security check helper method
   - Use `response.id()` directly for permissions link (protože member.id = userId)

2. **Integration test**
   - Test with/without MEMBERS:PERMISSIONS authority
   - Verify link presence/absence
   - Verify permissions link uses correct userId from member.id

## Open Questions

None - design is straightforward with clear precedents.
