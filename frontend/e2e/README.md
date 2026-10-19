# E2E Tests (Playwright + playwright-bdd)

Gherkin scenarios that drive a **running** Klabis instance through the browser and trace back to OpenSpec scenarios. Never part of `npm test` or `refresh-backend-server-resources`. Part of the frontend module, see also [`../CLAUDE.md`](../CLAUDE.md).

```bash
npx playwright install chromium   # once
npm run test:e2e                  # bddgen && playwright test
```

| Env var | Default | Purpose |
|---|---|---|
| `E2E_BASE_URL` | `http://localhost:3000` | Target instance; `https://localhost:8443` also works (self-signed cert ignored) |
| `E2E_ADMIN_USER` / `E2E_ADMIN_PASSWORD` | `ZBM9000` / `password` | `admin` role |
| `E2E_MEMBER_USER` / `E2E_MEMBER_PASSWORD` | `ZBM9500` / `password` | `member` role |

Defaults are the `example-data` seed users. Report: `playwright-report/` (HTML); traces of failed tests in `test-results/`.

**Layout:**

```
e2e/
├── auth.setup.ts     # "setup" project: logs in each role once
├── features/         # *.feature, one per capability area
├── steps/            # reusable step definitions (auth, navigation, form, events, membership-fees)
└── support/          # fixtures.ts (loginAs, api, apiAs, feeState), parameters.ts ({text}), dates.ts, session.ts, app.ts, roles.ts
```

Generated/ignored: `.features-gen/`, `e2e/.auth/`, `playwright-report/`, `test-results/`. Type-checked via `tsconfig.e2e.json`.

**Authentication:** the `setup` project logs in through the login form per role and saves cookies plus the `oidc.user:*` sessionStorage entries to `e2e/.auth/<role>.json`. `Given I am logged in as "<role>"` restores only the cookies: the app redirects to `/oauth2/authorize` and the authorization server session issues a code without the login form. The stored `oidc.user:*` entry is used only by the `api` fixture, which reads its bearer token from it.

**Rules:**

- **`Given` via API, `When`/`Then` via UI.** Data setup uses the `api` fixture (bearer token from the logged-in role's stored OIDC entry, navigates by HAL links/templates from `/api`). UI-driven setup only where no API exists.
- **Run prefix.** Every entity a scenario creates is named with `{prefix}` (→ `E2E-<runId>-<worker>-`); only seeded users are assumed and nothing is cleaned up. `{date+N}` / `{date-N}` resolve to today ± N days (`YYYY-MM-DD`). Both are resolved automatically by the `{text}` parameter type (`support/parameters.ts`) in every step argument; use `{text}` (quoted string) for all steps taking user text.
- **Traceability.** Tag each feature `@spec:<capability>` (OpenSpec capability name) and copy each `Scenario:` title verbatim from the spec.
- **No generated OpenAPI types** in E2E code — `test:e2e` does not trigger the `openapi` pre-hook.
- **Access tokens expire after ~5 minutes**, shorter than a full run: the `api` fixtures transparently re-login (`support/session.ts`) when the stored token is about to expire. `apiAs(role)` gives an API client for a role other than the logged-in one (e.g. admin-prepared data in a member scenario).
- **No deep links.** `page.goto('/some/path')` is bounced through the OAuth2 redirect back to `/`; reach pages through the menu or `When I navigate in the application to "<path>"` (does not wait for rendering).
- **Shared backend state.** Only one fee campaign can be active and every close sanctions members without a choice, so fee `Given` steps close any active campaign and publish a fresh year; the year is kept in the `feeState` fixture (`the scenario year`). Every campaign scenario gets a fresh year, so each run advances the campaign year by roughly the number of campaign scenarios; closed campaigns, charges and sanctions accumulate in the database and are never cleaned up.
- **Assert negatives after a positive anchor.** `I do not see ...` passes instantly on a page that has not rendered yet: always put a step proving the page rendered (`I see ...`, `the page shows section ...`) before it, and before `I continue as` wait for a positive result of the preceding action so the request is not aborted.
- Reuse existing steps before adding new ones; new steps go to the matching `steps/*.ts` file.

**Step vocabulary:**

| Step | Notes |
|---|---|
| `Given I am logged in as "<role>"` | `admin` or `member` |
| `Given a draft event "<name>" exists` | created via API |
| `Given a catalog fee tier "<name>" exists` (`... with a percentage rule for ranking "<r>" exists`) | admin API |
| `Given no fee selection campaign is active` | closes the active one, reserves a fresh year |
| `Given an active fee selection campaign for tier(s) "<a>"[ and "<b>"]` | closes the active one, creates missing tiers, publishes a fresh year |
| `Given a closed fee selection campaign for tier "<a>"` / `Given the fee selection campaign is closed` | |
| `Given the member has chosen tier "<t>"` / `has cancelled the tier choice` | as member via API |
| `Given the administrator has assigned the member to tier "<t>"` | emergency assignment via API |
| `Given a published event "<n>" with open registration exists` / `the member is registered for event "<n>"` | |
| `When I open the application` | waits for main navigation (`waitForApp`) |
| `When I click "<label>"` | button (exact name or `Select <label> form`) or link by exact name |
| `When I fill "<label>" with "<value>"` | form field by label |
| `When I select "<option>" in "<label>"` | select by option label |
| `When I open the list row "<text>"` | clicks a table cell |
| `When I fill "<label>" with the scenario year` | fee campaign year kept in scenario state |
| `When I pick "<option>" in "<label>"` | option of a searchable multi-select (types into the combobox, clicks the option, waits for its chip); field located via its label text |
| `When I choose option "<name>"` / `Then the option "<name>" is selected` | radio card (member fee choice) |
| `When I open the catalog tier "<name>"` | row of the fee tier catalog table |
| `When I open the closed campaign` / `When I open my profile` | |
| `When I navigate back` / `When I navigate in the application to "<path>"` | browser back / SPA router push |
| `When I navigate in the application to the scenario campaign` | SPA route of the campaign created by the `Given` step |
| `When I continue as "<role>"` | drops cookies and sessionStorage, restores the role's cookies; follow with `When I open the application` (needs a loaded page) |
| `Then I see "<text>"` | visible text, substring match |
| `Then I do not see "<text>"` / `Then I see an error message` | text absent / any `role=alert` |
| `Then I see the date "<YYYY-MM-DD>"` / `Then I do not see the date "<YYYY-MM-DD>"` | rendered as `d. M. yyyy` |
| `Then the page shows section "<h>"` / `Then the page does not show section "<h>"` | heading |
| `Then the (active\|past) campaigns section (contains\|does not contain) the (campaign\|closed campaign) year` | scoped to the section heading |
| `Then the active campaign lists fee group "<name>"` | |
| `Then the event registrations (contain\|do not contain) "<text>"` | registrations table on the event detail |
| `Then the account (shows\|does not show) the yearly membership fee of the scenario year` | |
| `Then the main menu (shows\|does not show) "<item>"` | navigation link |
