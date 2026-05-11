# SI Prefill & Action Colors — QA Testing

## Scenarios

### N2 — SI card prefill in registration form
- [ ] **N2-1**: Member with SI card in profile opens registration form → SI field is prefilled with the profile value
- [ ] **N2-2**: Overwriting the prefilled SI value and submitting creates registration with the overwritten value; profile chip number remains unchanged
- [ ] **N2-3**: Member without SI card in profile opens registration form → SI field is empty

### K2 — Action button semantic colors
- [ ] **K2-1**: "Přihlásit se" button uses primary visual treatment (primary-ghost)
- [ ] **K2-2**: "Zrušit akci" button uses destructive treatment (danger-ghost)
- [ ] **K2-3**: "Odhlásit se z akce" button uses warning treatment (warning-ghost)
- [ ] **K2-4**: "Upravit" and "Synchronizovat" buttons use neutral treatment (ghost)
- [ ] **K2-5**: "Publikovat" button on DRAFT event uses primary treatment

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| N2-1 | PASS | Admin (chipNumber=665665) klikl Přihlásit → SI field předvyplněno hodnotou 665665. |
| N2-2 | **FAIL** | Submit po přepsání volá `PUT /registrations/{memberId}` místo `POST registerForEvent` → 404. Workaround z Iter 2 (předání editRegistration template + resourceData přes pathname) způsobí, že FE submituje přes edit flow místo create flow. |
| N2-3 | PASS | Eva (bez chipNumber) klikla Přihlásit → SI field prázdné. |
| K2-1 | PASS | "Přihlásit se" má text-primary + bg-primary-subtle. |
| K2-2 | PASS | "Zrušit akci" má text-red-600 + bg-red-50 (danger-ghost). |
| K2-3 | PASS | "Odhlásit se z akce" má text-warning + bg-warning-bg. |
| K2-4 | PASS | "Upravit" má bg-transparent + text-text-primary (ghost). Synchronizovat ověřen unit testem. |
| K2-5 | PASS | "Publikovat" má text-primary + bg-primary-subtle (uvidíme v DRAFT řádku). |

### Issues found

**ISSUE-1 (HIGH, frontend):** Submit nové registrace volá PUT místo POST.
- Symptom: Po kliknutí Odeslat formuláře předvyplněného přes affordance `newRegistration` se odešle `PUT /api/events/{eventId}/registrations/{memberId}` → 404 Not Found. Registrace se nevytvoří.
- Root cause: V `EventsPage.tsx` Iter 2 workaround předává GET-fetched resourceData + editRegistration template do `HalFormDisplay`. `HalFormDisplay` pak používá editRegistration template (HTTP method PUT) místo registerForEvent template (HTTP method POST/registerForEvent).
- Impact: N2-2 fail. Nová registrace přes affordance newRegistration je rozbitá end-to-end.
- Affected component: `frontend/src/pages/events/EventsPage.tsx` — sekce kolem fetchnutí defaults a předání do HalFormDisplay.
- Fix: použít registerForEvent template pro submit (POST), zachovat editRegistration template pouze pro získání properties s prefill values, nebo přepnout template name v submit phase.

### Iteration 2 (after fix)
| Scenario | Result | Note |
|----------|--------|------|
| N2-1 | PASS | Prefill funguje stále (SI=665665 z profilu Jana Nováka). |
| N2-2 | PASS | Po přepsání na 123456 + výběru kategorie M21 a Odeslat: registrace vytvořena (status 200), siCardNumber=123456, category=M21. Profile chipNumber zůstal 665665 — beze změny. |
| N2-3 | PASS | (ověřeno v Iter 1) |
| K2-1..K2-5 | PASS | (ověřeno v Iter 1; computed styles odpovídají variantám primary-ghost / danger-ghost / warning-ghost / ghost) |

**Result: all scenarios pass.**
