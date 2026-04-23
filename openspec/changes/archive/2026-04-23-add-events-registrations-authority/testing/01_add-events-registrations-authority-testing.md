# add-events-registrations-authority — QA Testing

## Scenarios

### Permissions Dialog (Group 7)
- [x] **DIALOG-1**: Admin (ZBM9000) otevře permissions dialog na běžném členovi a vidí toggle "Správa přihlášek" s popiskem "Editace přihlášek ostatních členů na akce"
- [x] **DIALOG-2**: Admin zapne `EVENTS:REGISTRATIONS` regulárnímu členovi a uloží — oprávnění se přiřadí (persistuje po reload)

### Stay-on-page after registering (Group 8)
- [x] **REG-1**: Přihlášený běžný člen se registruje na otevřenou akci — po potvrzení zůstane na detailu akce, objeví se záznam v seznamu přihlášek

### Edit affordance for EVENTS:REGISTRATIONS holder (Groups 2, 3, 4, 9)
- [x] **AUTH-1**: Běžný člen (ZBM9500) vidí "Upravit" pouze na svém řádku v seznamu přihlášek (ne u ostatních)
- [x] **AUTH-2**: Člen s `EVENTS:REGISTRATIONS` vidí "Upravit" na KAŽDÉM řádku v seznamu přihlášek
- [x] **AUTH-3**: Člen s `EVENTS:REGISTRATIONS` otevře edit formulář cizí registrace a změní SI číslo / kategorii — změna se uloží, původní registrační čas zůstává
- [x] **AUTH-4**: Běžný člen bez `EVENTS:REGISTRATIONS` obdrží 403 při pokusu o přímý GET `/api/events/{id}/registrations/{jiny-memberId}`
- [x] **AUTH-5**: Po odebrání `EVENTS:REGISTRATIONS` člen ztrácí affordance na cizích řádcích a dostává 403 na GET cizí registrace (ověřeno kombinací AUTH-1 + AUTH-4 po odebrání)

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| DIALOG-1 | PASS | Toggle "Správa přihlášek" s popisem přesně podle specs |
| DIALOG-2 | PASS | Toast "Oprávnění uložena", reload dialogu ukazuje checked |
| REG-1 | FAIL→PASS | Fix: přidáno `navigateOnSuccess={false}` na `registerForEvent` HalFormButton v `EventDetailPage.tsx:231` |
| AUTH-1 | PASS | ZBM9500 (bez authority) vidí Upravit pouze na svém řádku |
| AUTH-2 | PASS | Admin (Set.of(Authority.values())) vidí Upravit na všech řádcích |
| AUTH-3 | PASS | Admin upravil cizí SI na 999999, registeredAt zůstal |
| AUTH-4 | PASS | 403 s `"Required authority: EVENTS:REGISTRATIONS"` |
| AUTH-5 | PASS | Po odebrání oprávnění ZBM9500 znovu vidí jen svůj řádek a GET cizí vrací 403 |

### Fixes applied
- **frontend/src/pages/events/EventDetailPage.tsx:231** — `<HalFormButton name="registerForEvent" modal={true} navigateOnSuccess={false} …/>` (bez propu frontend defaultně navigoval podle Location headeru)

### Known pre-existing issues (nespadají do této proposal)
- `PermissionController.UpdatePermissionsRequest.authorities` má `@NotEmpty` → admin nemůže uložit úplně prázdný seznam oprávnění. Pokud má cílový člen jen `EVENTS:REGISTRATIONS` a admin je chce odebrat, dostane 400. Workaround: API volat s ponecháním aspoň `MEMBERS:READ`. Není součástí této proposal — nutná samostatná oprava.
