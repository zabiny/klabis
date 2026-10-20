# E2E Tests (Playwright + playwright-bdd)

Gherkin scenarios that drive a **running** Klabis instance through the browser and trace back to OpenSpec scenarios. Never part of `npm test` or `refresh-backend-server-resources`. Part of the frontend module, see also [`../CLAUDE.md`](../CLAUDE.md).

```bash
npx playwright install chromium   # once
npm run test:e2e                  # bddgen && playwright test
```

| Env var | Default | Purpose |
|---|---|---|
| `E2E_BASE_URL` | `http://localhost:3000` | Target instance; `https://localhost:8443` also works (self-signed cert ignored) |
| `E2E_ADMIN_USER` / `E2E_ADMIN_PASSWORD` | `ZBM9000` / `password` | `admin` role (persona `Jan`) |
| `E2E_MEMBER_USER` / `E2E_MEMBER_PASSWORD` | `ZBM9500` / `password` | `member` role (persona `Eva`) |

Defaults are the `example-data` seed users. Report: `playwright-report/` (HTML); traces of failed tests in `test-results/`.

**Layout:**

```
e2e/
├── auth.setup.ts     # "setup" project: logs in each role once
├── features/         # *.feature, one per business capability, scenarios grouped by `Rule:`
├── steps/            # step definitions per area (navigation, events, membership-fee-catalog/-campaigns/-levels/-choice)
└── support/          # fixtures.ts (actAs, apiAs, feeState), parameters.ts, personas.ts, ui.ts (UI mechanics),
                      # halApi.ts, membershipFeesApi.ts, eventsApi.ts (API set-up), membershipFeesPages.ts (fee screens),
                      # dates.ts, session.ts, app.ts, roles.ts
```

Generated/ignored: `.features-gen/`, `e2e/.auth/`, `playwright-report/`, `test-results/`. Type-checked via `tsconfig.e2e.json`.

**Authentication:** the `setup` project logs in through the login form per role and saves cookies plus the `oidc.user:*` sessionStorage entries to `e2e/.auth/<role>.json`. The `actAs(persona)` fixture restores the cookies and the `oidc.user:*` entry of the persona's role, so the app starts signed in without the `/oauth2/authorize` redirect; it switches the session only when another role is signed in, so consecutive steps of one persona continue on the current page. The `apiAs(role)` fixture takes its bearer token from the same entry. Both share the worker-scoped `sessionOf(role)`; `apiAs` is worker-scoped too and caches only data that never changes during a run (the `/api` root, a persona's member identity), never scenario data.

**Writing scenarios:** follow the `gherkin-bdd` skill (`.claude/skills/gherkin-bdd`): declarative steps in business language, one `When` per scenario, `Given` = state, `Then` = observable outcome, data that decides the outcome visible in the scenario. UI mechanics (clicks, field labels, menu paths, waiting) live in step definitions and `support/ui.ts`, never in feature files.

**Rules:**

- **Personas.** Scenarios speak about named personas in the third person: `Jan` acts as the `admin` role, `Eva` as the `member` role (`support/personas.ts`). Each feature description introduces the personas it uses. Steps that only inspect the page the persona just opened check that the persona is the one acting (`expectActing`).
- **`Given` via API, `When`/`Then` via UI.** Set-up steps call the API (`apiAs`, HAL links/templates from `/api`); UI-driven set-up only where no API exists. `When` is a user action through the UI, or a time-driven event (`the fee selection campaign ends`) triggered through the API. `Then` observes through the UI and navigates to the place where the outcome is visible; it may switch to the persona that observes it (`Eva's account shows …`).
- **Unique names.** Entity names are `{name}` parameters: the parameter type prefixes them with `E2E-<runId>-<scenario>-`, so feature files contain plain names and no scenario reuses another scenario's entity. Only seeded users are assumed and nothing is cleaned up.
- **Traceability.** Tag each feature `@spec:<capability>` (OpenSpec capability name), copy each `Scenario:` title verbatim from the spec and group scenarios under a `Rule:` stating the spec requirement. An extra example of the same rule gets its own descriptive title. Spec scenarios that are not automated are listed in a comment at the top of the feature.
- **No generated OpenAPI types** in E2E code — `test:e2e` does not trigger the `openapi` pre-hook.
- **Access tokens expire after ~5 minutes**, shorter than a full run: `sessionOf` transparently re-logs in (`support/session.ts`) when the token is about to expire.
- **No deep links.** `page.goto('/some/path')` is bounced through the OAuth2 redirect back to `/`; step definitions reach pages through the menu or `navigateInApp` (SPA router push).
- **Shared backend state.** Only one fee campaign can be active and every close sanctions members without a choice, so campaign `Given` steps close any active campaign and publish a fresh year; the year is kept in the `feeState` fixture (`the campaign year`). Each run advances the campaign year by roughly the number of campaign scenarios; closed campaigns, charges and sanctions accumulate in the database. Because earlier scenarios leave Eva sanctioned, `Eva is registered for the open event "…"` lifts the block by an emergency assignment in the active campaign and withdraws the choice again — it needs an active campaign.
- **Wait for outcomes inside steps.** `submit` waits for the backend answer (and for the modal to close on success); negative assertions (`toHaveCount(0)`) are preceded by a positive anchor proving the checked content finished loading (a table row, the empty-state text or the awaited API response — not just a heading).
- Reuse existing steps before adding new ones; new steps go to the matching `steps/*.ts` file.

**Parameter types** (`support/parameters.ts`): `{persona}` (`Jan`, `Eva`), `{name}` (quoted entity name, prefixed), `{day}` (`today`, `yesterday`, `in N days`, `N days from today`), `{contribution}` (`50 %`, `300 CZK`), plus the built-in `{string}` and `{int}`.

**Step vocabulary:**

| Area | Steps |
|---|---|
| Navigation | `When {persona} opens the application` · `When {persona} navigates back` · `Then the main menu offers:` / `does not offer:` (data table) |
| Events | `Given a draft event {name} exists` · `Given event {name} is open for registration` · `Given {persona} is registered for the open event {name}` · `When {persona} creates event {name} filling in only the required fields` · `When {persona} changes the location of event {name} to {string}` · `Then event {name} is a draft` · `Then the event list contains {name}` · `Then event {name} takes place in {string}` · `Then {persona} remains registered for event {name}` · `Then {persona}'s registration for event {name} is cancelled` · `Then {persona} is [not] offered to register for event {name}` |
| Fee catalog | `Given a catalog fee level {name} [with yearly fee {int} CZK] exists` · `Given catalog level {name} includes a {contribution} rule for races of ranking {name}` · `When {persona} creates fee level {name} with yearly fee {int} CZK` · `When {persona} changes the yearly fee of catalog level {name} to {int} CZK` · `When {persona} adds a {contribution} rule for races of ranking {name} to catalog level {name}` · `Then the catalog lists level {name} with yearly fee {int} CZK` · `Then catalog level {name} has no payment rules` · `Then catalog level {name} has [no] {contribution} rule for races of ranking {name}` · `Then {persona} is not offered to add a fee level` · `Then {persona} is not offered to manage catalog level {name}` · `Then the rule is refused` |
| Campaigns | `Given no fee selection campaign is active` · `Given an active fee selection campaign offers level {name}` (also `levels {name} and {name}`, `… with yearly fee {int} CZK`, `… with the voting deadline {day}`) · `Given a closed fee selection campaign that offered level {name}` · `Given the fee selection campaign is closed` · `Given {persona} is viewing the closed campaign` · `When {persona} starts a fee selection campaign offering level {name}` / `levels {name} and {name}` `with the voting deadline {day}` · `When {persona} opens the membership fees page` / `the active campaign` / `the closed campaign` · `When {persona} changes the voting deadline to {day}` · `When {persona} closes the active campaign` · `When the fee selection campaign ends` · `Then the campaign is refused` · `Then the deadline change is refused` · `Then the active campaign has the voting deadline {day}` · `Then the active campaign offers level {name}` · `Then {persona} is [not] offered to change the voting deadline` / `to close the campaign` · `Then {persona} is not offered to start a fee selection campaign` · `Then {persona} can open the closed campaign` |
| Fees page | `Then the membership fees page lists catalog level {name}` · `… shows the active campaign of the campaign year` · `… shows no active campaign` · `… shows no past campaigns` · `… lists the closed campaign among the past campaigns` · `… does not list the active campaign among the past campaigns` · `Then the membership fees page is shown` |
| Published levels | `Given {persona} has assigned {persona} to level {name}` · `When {persona} assigns {persona} to level {name}` · `When {persona} changes the yearly fee of the published level {name} to {int} CZK` · `Then the published level {name} has yearly fee {int} CZK` · `Then the published level {name} has a {contribution} rule for races of ranking {name}` · `Then the published level {name} [does not list / lists] {persona} as a member` · `Then {persona} can open the published level {name}` |
| Member's choice | `Given {persona} has chosen level {name}` · `Given {persona} has not chosen a fee level` · `Given {persona} had level {name} in the previous fee selection campaign` · `Given {persona} is blocked for missing the fee level choice` · `When {persona} chooses level {name}` · `When {persona} changes the fee level choice to {name}` · `When {persona} withdraws the fee level choice` · `When {persona} views the profile` · `When {persona} opens the fee level choice form` · `Then {persona} can choose level {name}` · `Then level {name} is pre-selected` · `Then the choice form shows level {name} as locked` · `Then {persona}'s profile shows fee level {name} [as locked]` · `Then {persona}'s profile prompts to choose a fee level` · `Then {persona} is not offered to change the fee level choice` / `to withdraw the fee level choice` |
| Account | `Then {persona}'s account shows the yearly membership fee of {int} CZK for the campaign year` · `Then {persona}'s account shows no yearly membership fee for the campaign year` |
