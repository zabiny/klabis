## Why

Aktuální model oprávnění pro members rozlišuje tři separátní write autority (`MEMBERS:CREATE`, `MEMBERS:UPDATE`, `MEMBERS:DELETE`), které jsou vždy přiřazovány dohromady. Tato granularita přidává zbytečnou složitost správy oprávnění bez reálného přínosu — nikdo nepotřebuje moci mazat členy, ale ne je vytvářet.

## What Changes

- **BREAKING**: Authority `MEMBERS:CREATE` bude odstraněna a nahrazena `MEMBERS:MANAGE`
- **BREAKING**: Authority `MEMBERS:UPDATE` bude odstraněna a nahrazena `MEMBERS:MANAGE`
- **BREAKING**: Authority `MEMBERS:DELETE` bude odstraněna a nahrazena `MEMBERS:MANAGE`
- Nová autorita `MEMBERS:MANAGE` (CONTEXT_SPECIFIC) pokrývá veškeré write operace nad členy (registrace, editace, smazání, suspend/resume)
- Existující uložená oprávnění uživatelů budou migrována: `MEMBERS:CREATE` + `MEMBERS:UPDATE` + `MEMBERS:DELETE` → `MEMBERS:MANAGE`
- OAuth2 scope mapování `members.write` bude aktualizováno na `MEMBERS:MANAGE`
- Frontend PermissionsDialog bude aktualizován — 3 checkboxy → 1 checkbox

## Capabilities

### New Capabilities

- (žádné nové — změna zjednodušuje existující model)

### Modified Capabilities

- `users`: Změna sady validních autorit — odebrání MEMBERS:CREATE, MEMBERS:UPDATE, MEMBERS:DELETE, přidání MEMBERS:MANAGE

## Impact

- `Authority.java` enum — odebrání 3 hodnot, přidání 1 nové
- `RegistrationController`, `MemberController` — aktualizace `@HasAuthority` anotací
- Databázová migrace — konverze existujících uložených oprávnění
- Frontend `PermissionsDialog.tsx` — aktualizace PERMISSION_LABELS
- Testy — všechny reference na MEMBERS_CREATE, MEMBERS_UPDATE, MEMBERS_DELETE nahrazeny MEMBERS_MANAGE
- Admin bootstrap — výchozí admin oprávnění zahrnují MEMBERS:MANAGE místo tří separátních
