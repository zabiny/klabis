## Why

`EventRegistrationDialog` dnes vyžaduje od každého volajícího osm propů — šablonu submitu, event kontext, prefill href a režim new/edit — a každé vstupní místo (detail eventu, seznam akcí, dashboard) je musí samo najít a poskládat. Reprezentace registrace (a její prefill varianta) přitom nese všechna data i affordance potřebné k sestavení formuláře; volající by měl znát jediný HAL link.

## What Changes

- **Backend**: prefill odpověď (`getRegistration?newRegistration=true`) nese affordanci `registerForEvent` (POST, prompted `categoryId` options) místo dnešních `editRegistration`/`unregisterFromEvent`; odpověď pro existující registraci se nemění. Affordance se na prefillu nabízí jen při otevřených registracích a nezablokovaném členu (parita s affordancemi na eventu).
- **Frontend**: nový hook `useRegistrationDialogData(registration: Link | null)` z linku registace zřetězeně dofetchnue reprezentaci registrace i navázaný event a připraví všechna data formuláře (mode, šablonu, initial values, event kontext, loading/error).
- **Frontend**: `EventRegistrationDialog` má jediný datový vstup `registration: Link | null` (null = zavřeno); propy `isOpen`, `mode`, `template`, `event`, `prefillHref`, `initialValuesHref` zanikají. Režim new/edit se odvozuje z affordance v reprezentaci registrace.
- **Frontend**: volající (detail eventu, seznam akcí, dashboard widget) předávají jen link — `newRegistration` link eventu pro novou registraci, self link řádku registrace pro editaci.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `event-registrations`: prefill odpověď (`new=true`) nese `registerForEvent` affordanci (s prompted `categoryId` options) a nenese `editRegistration`/`unregisterFromEvent`; registrační formulář je řízen affordancí z této odpovědi na všech vstupních místech.

## Impact

- Backend: `EventRegistrationController` (`RegistrationView` + `RegistrationDetailsPostprocessor`), spec `docs/openapi/spec/events.yaml` (x-hal-templates getRegistration), testy `EventRegistrationControllerTest`.
- Frontend: `components/events/EventRegistrationDialog.tsx`, nový `hooks/useRegistrationDialogData.ts`, `pages/events/EventDetailPage.tsx`, `pages/events/EventsPage.tsx`, `components/dashboard/UpcomingDeadlinesWidget.tsx`, `hooks/useUpcomingDeadlines.ts`, generované typy (`npm run openapi`).
- Žádná změna registračního chování (payload, validace, oprávnění submitu); jediná viditelná změna: prefill na eventu se zavřenými registracemi místo otevření formuláře s chybnou šablonou zobrazí chybu.
