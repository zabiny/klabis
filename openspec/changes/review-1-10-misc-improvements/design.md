## Context

Tři self-contained UX improvements bez ambicí na velký redesign. Každý lze nasadit nezávisle, ale technicky se sdružují do jednoho proposalu, protože každý sám o sobě je triviální.

## Goals / Non-Goals

**Goals:**
- N1: přihlášený uživatel může změnit své heslo z profilu (zadání current + new + confirm).
- N4: filter akcí podle roku.
- K1: UI label rename „Koordinátor" → „Vedoucí" v kontextu akcí.

**Non-Goals:**
- N1: žádné force change (po N dnech, po incidentu) — out of scope.
- N1: 2FA — out of scope, samostatná capability.
- N4: range slider, multi-year selection, "decade" filter — KISS, single year dropdown.
- K1: rename na úrovni API / domain modelu (`eventCoordinatorId` zůstává) — UI-only změna.

## Decisions

### Decision 1: N1 — endpoint v existujícím `password-setup` namespace nebo `me`

Volba: `POST /api/me/password-change`. Důvod:
- `password-setup` namespace je dnes pro PENDING_ACTIVATION users (token-based). Change password by tam nepatřil sémanticky.
- `me` je idiomatický REST namespace pro current authenticated user. Krásně se v něm hodí i `me/ical-token` z proposalu 1.9.

Body:
```json
{ "currentPassword": "<plain>", "newPassword": "<plain>" }
```

Service:
1. Resolve current authenticated user.
2. `passwordEncoder.matches(currentPassword, user.passwordHash)` — pokud false, vrátí 400 s chybou „Současné heslo se neshoduje".
3. `PasswordValidator.validate(newPassword, user.firstName, user.lastName, user.registrationNumber)` — stejné pravidla jako setup.
4. `user.changePassword(passwordEncoder.encode(newPassword))`, persist.
5. Audit log: `PasswordChangedEvent` (existing event mechanism v users module).

Žádné session invalidation po change — uživatel zůstává přihlášen v current session (běžné UX, např. Google to dělá taky).

### Decision 2: N4 — Year filter UX

Dropdown „Rok" v filter bar, vedle existujícího time window selectoru. Hodnoty:
- „—" (žádný rok = filter neaktivní, default)
- Roky `currentYear-10` ... `currentYear+2` (dynamický range, generovaný v frontendu)

Při výběru roku:
- `dateFrom = YYYY-01-01`, `dateTo = YYYY-12-31` se přidají do URL query.
- Time window selector se přepne na „Vše" (jinak by „Budoucí" zúžilo i rok 2024 na nic).
- Pokud uživatel po výběru roku klikne time window („Budoucí"), rok se neutrlizuje (oba filtry se vzájemně vylučují).

URL state:
- `?year=2024` (transformuje se na `dateFrom`/`dateTo`) **vs.** `?dateFrom=2024-01-01&dateTo=2024-12-31` (přímo).

Volba: **přímo `dateFrom`/`dateTo`** — backend filter už toto akceptuje, nepotřebujeme alias. Frontend si „rok" vyrobí z range při loadu URL (pokud range pokrývá celý rok, tak v UI zobrazí konkrétní rok jako selected; jinak „—").

### Decision 3: K1 — Single source of truth = `localization/labels.ts`

V `frontend/src/localization/labels.ts` najít všechny entries pro „Koordinátor" / `coordinator`:
- `getFieldLabel('coordinator')` → „Vedoucí"
- `getFieldLabel('eventCoordinator')` → „Vedoucí akce"
- `getNavLabel(...)` pro filter bar → „Vedoucí"
- header tabulky a detail page section labels.

Backend HAL+FORMS template může poslat label „Coordinator" (anglicky); frontend ho přemapuje na lokalizační key. Pokud server posílá CS string přímo (bez frontend lookup), je třeba ho upravit i tam (memory `project_localization_system.md` zmiňuje, že labels jsou centralizované).

**Otevřená otázka:** posílá backend label v HAL templates? Pokud ano, je potřeba zkontrolovat backend HalTemplate konfigurace pro Event endpoints. Default: frontend lokalizace je single source.

## Risks / Trade-offs

- **[Risk] N1 — change password bez 2FA / re-auth challenge** → Mitigation: ověření currentPassword v request body je standardní pattern. Pro vyšší security by stačilo přidat re-authentication step (re-enter password modal) — současný design ho má v dialogu, OK.
- **[Risk] N4 — rok vs. time window mutual exclusion** → Mitigation: clear UX dokumentace, explicit reset ve filter baru.
- **[Risk] K1 — backend HAL templates posílají „Coordinator" label** → Mitigation: ověřit a opravit v rámci tasks; pokud existuje, jednoduše přepsat string.

## Migration Plan

Tři vertikální sloty:
1. N1 — backend service, controller, frontend dialog.
2. N4 — frontend dropdown + URL state.
3. K1 — labels.ts + případně backend HAL template strings.

Každý slot je nezávislý a nasaditelný samostatně.
