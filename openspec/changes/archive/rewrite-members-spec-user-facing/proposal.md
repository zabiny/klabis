## Why

Stávající specs popisují chování systému z pohledu API (HTTP status kódy, JSON struktury, HATEOAS linky, value object implementace) místo z pohledu uživatele. To je v rozporu se stylem `user-groups` spec, která vznikla jako vzorový příklad správného formátu. Technické požadavky navíc nemají ve specs místo — patří do samostatné dokumentace. Cílem je sjednotit všechny specs do jednotného user-facing stylu.

## What Changes

- Přepsání všech specs do user-facing stylu (vzor: `user-groups` spec) — popis toho, co uživatel vidí a dělá, ne jaký HTTP status server vrátí
- Reorganizace požadavků podle UI kontextů a uživatelských workflow místo podle API endpointů
- Validační scénáře přepsány jako formulářové chování (co uživatel vidí při neplatném vstupu)
- Autorizační scénáře přepsány jako podmínky dostupnosti UI akcí
- Vytvoření nové capability `non-functional-requirements` se specs pro technické API požadavky přesunuté z ostatních specs (HAL+FORMS, ISO-8601 serializace, HATEOAS link struktura, pagination formát, apod.)

## Capabilities

### New Capabilities

- `non-functional-requirements`: Technické API požadavky vyjmuté ze specs — HAL+FORMS response formát, ISO-8601 serializace dat, HATEOAS link struktura, pagination metadata formát, concurrent update handling, value object technické detaily

### Modified Capabilities

- `members`: Přepsání do user-facing stylu, reorganizace podle UI kontextů (Registration Page, Member List, Member Detail, Edit Form, Suspension). Žádné funkční požadavky se nemění.
- `events`: Přepsání do user-facing stylu. Žádné funkční požadavky se nemění.
- `event-registrations`: Přepsání do user-facing stylu. Žádné funkční požadavky se nemění.
- `calendar-items`: Přepsání do user-facing stylu. Žádné funkční požadavky se nemění.
- `users`: Přepsání do user-facing stylu. Žádné funkční požadavky se nemění.
- `users-authentication`: Přepsání do user-facing stylu. Žádné funkční požadavky se nemění.
- `email-service`: Přepsání do user-facing stylu. Žádné funkční požadavky se nemění.
- `member-permissions-dialog`: Přepsání do user-facing stylu. Žádné funkční požadavky se nemění.
- `server-configuration`: Posoudit zda obsahuje user-facing požadavky; pokud ne, přesunout celý obsah do `non-functional-requirements`.

## Impact

- Všechny soubory v `openspec/specs/*/spec.md` — přepis formátu (bez změny funkčních požadavků)
- `openspec/specs/non-functional-requirements/spec.md` — nový soubor
- Žádný dopad na backend ani frontend kód — jde výhradně o dokumentaci
