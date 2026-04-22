# Get Registration by Member ID - QA Testing

## Scenarios

### Own Registration (member)
- [x] **OWN-1**: Přihlášený člen vidí vlastní registraci na event — zobrazí se SI karta
- [x] **OWN-2**: Na stránce vlastní registrace je akce "odregistrovat" (event je otevřený)
- [x] **OWN-3**: Na stránce vlastní registrace je akce "editovat registraci" (registrace jsou otevřené)
- [x] **ERR-1**: Člen naviguje na registraci eventu kde není registrován — zobrazí se not-found

### Coordinator access
- [x] **COORD-1**: Koordinátor (EVENTS:MANAGE) vidí registraci jiného člena — zobrazí se SI karta
- [x] **COORD-2**: Koordinátor vidí registraci jiného člena — NEzobrazí se akce unregister/edit

### API / URL correctness
- [x] **API-1**: GET /api/events/{id}/registrations/me vrátí 4xx (endpoint odstraněn)
- [x] **LINK-1**: Self link na registraci v HAL response obsahuje /{memberId} ne /me

---

## Results

### Iteration 1 (API testy)
| Scenario | Result | Note |
|----------|--------|------|
| OWN-1 | PASS | Eva vidí SI card `123456` na GET /{evaMemberId} |
| OWN-2 | PASS | `unregisterFromEvent` affordance přítomna |
| OWN-3 | PASS | `editRegistration` affordance přítomna |
| ERR-1 | PASS | 404 pro neregistrovaného člena |
| COORD-1 | PASS | Admin vidí Evinu SI card `123456` přes GET /{evaMemberId} |
| COORD-2 | PASS | Koordinátor nedostane affordances `[]` (žádné edit/unregister) |
| API-1 | PASS | GET /me vrací 400 (Spring routuje "me" jako neplatný UUID) |
| LINK-1 | PASS | Location header i self link obsahují `/{memberId}`, ne `/me` |
