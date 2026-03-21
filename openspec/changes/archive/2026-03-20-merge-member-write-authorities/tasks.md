## 1. Backend — Authority enum a persistence

- [x] 1.1 Přidat `MEMBERS_MANAGE("MEMBERS:MANAGE", Scope.CONTEXT_SPECIFIC)` do `Authority` enum
- [x] 1.2 Odebrat `MEMBERS_CREATE`, `MEMBERS_UPDATE`, `MEMBERS_DELETE` z `Authority` enum
- [x] 1.3 Aktualizovat Flyway migraci V001 — přidat SQL pro migraci existujících hodnot (`MEMBERS:CREATE`, `MEMBERS:UPDATE`, `MEMBERS:DELETE` → `MEMBERS:MANAGE`) a deduplikaci

## 2. Backend — Controllers a bootstrap

- [x] 2.1 Aktualizovat `RegistrationController` — nahradit `@HasAuthority(Authority.MEMBERS_CREATE)` za `MEMBERS_MANAGE`
- [x] 2.2 Aktualizovat `MemberController` — nahradit všechny `@HasAuthority(Authority.MEMBERS_UPDATE)` a `MEMBERS_DELETE` za `MEMBERS_MANAGE`
- [x] 2.3 Aktualizovat admin bootstrap (`BootstrapDataLoader`) — nahradit `MEMBERS_CREATE` za `MEMBERS_MANAGE`
- [x] 2.4 Aktualizovat OAuth2 scope mapování (`members.write` → `MEMBERS:MANAGE`)

## 3. Backend — Testy

- [x] 3.1 Aktualizovat `MemberControllerSecurityTest` — nahradit všechny reference na `MEMBERS_CREATE`, `MEMBERS_UPDATE`, `MEMBERS_DELETE`
- [x] 3.2 Aktualizovat `MemberControllerApiTest` — nahradit authority reference
- [x] 3.3 Aktualizovat `MemberLifecycleE2ETest` — nahradit kombinaci tří autorit za `MEMBERS_MANAGE`
- [x] 3.4 Aktualizovat `UpdateMemberIntegrationTest` — nahradit authority reference
- [x] 3.5 Aktualizovat `PermissionControllerTest` — aktualizovat validní autority v test datech
- [x] 3.6 Aktualizovat `AuthorizationQueryServiceTest`, `PermissionServiceTest`, `UserPermissionsTest`
- [x] 3.7 Aktualizovat `UserTestDataConstants` — nahradit authority konstanty
- [x] 3.8 Spustit všechny testy a ověřit, že procházejí

## 4. Frontend

- [x] 4.1 Aktualizovat `PERMISSION_LABELS` v `PermissionsDialog.tsx` — odebrat `MEMBERS:CREATE`, `MEMBERS:UPDATE`, `MEMBERS:DELETE`, přidat `MEMBERS:MANAGE` s popiskem "Správa členů"
- [x] 4.2 Ověřit, že PermissionsDialog správně zobrazuje jeden checkbox pro správu členů
