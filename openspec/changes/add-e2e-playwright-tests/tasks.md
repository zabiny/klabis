## 1. Walking skeleton: login against a running instance (target A)

- [x] 1.1 Add `@playwright/test` and `playwright-bdd` devDependencies (current versions verified against the registry). Install the Chromium browser.
- [x] 1.2 Add `playwright.config.ts` (`E2E_BASE_URL` default `http://localhost:3000`, `ignoreHTTPSErrors`, Chromium desktop, HTML reporter, `defineBddConfig` for `e2e/features` and `e2e/steps`), and add the `test:e2e` script (`bddgen && playwright test`).
- [x] 1.3 Add `tsconfig.e2e.json` and reference it from `tsconfig.json`. Add `.features-gen/`, `playwright-report/`, `test-results/` and `e2e/.auth/` to `.gitignore` and ESLint ignores.
- [x] 1.4 Add the auth setup project for roles `admin` (`ZBM9000`) and `member` (`ZBM9500`), with credentials from `E2E_*` env and seeded defaults. Store `e2e/.auth/<role>.json`.
- [x] 1.5 Add the `Given I am logged in as "<role>"` step plus fixture. Verify silent re-login through the AS session cookie on both `:3000` and `:8443`. If it is unreliable, implement the `sessionStorage` + `addInitScript` fallback (design D4).
  - Outcome: cookie re-entry was flaky on `:8443` because the AS rotates `JSESSIONID` on concurrent bearer-authenticated API calls. The D4 fallback is in use.
- [x] 1.6 Verify isolation: `npm test` still runs only Vitest with an unchanged test count, and `npm run lint` and `npm run build` pass.

## 2. Read-only slice: role-based main menu

- [x] 2.1 Add `e2e/features/application-navigation.feature` (`@spec:application-navigation`) with the scenario "User sees only menu items they are authorized for", for admin and member (Scenario Outline).
- [x] 2.2 Implement the reusable navigation steps (`I open the application`, `the main menu shows "<item>"`, `the main menu does not show "<item>"`).
- [x] 2.3 Run against target A on `:3000` and on `:8443`. Make the scenario fail by asserting a wrong menu item, to confirm it really checks the menu, then revert.

## 3. Mutating slice: create event through the UI

- [x] 3.1 Add the run prefix helper (`E2E-<runId>-<worker>-`) as a fixture.
- [x] 3.2 Add `e2e/features/events.feature` (`@spec:events`) with the scenario "Manager creates an event with all required fields". The event name uses the run prefix.
- [x] 3.3 Implement the generic form steps (`I click "<label>"`, `I fill "<label>" with "<value>"`, `I see "<text>"`) and the event-specific steps where no generic step fits. The scenario runs as `admin` (`ZBM9000`, all permissions).
- [x] 3.4 Run the scenario twice in a row against target A and confirm both runs pass, which proves independence from previous runs.

## 4. API-setup slice: update event with `Given` via REST API

- [x] 4.1 Add the `api` fixture: read the token from the `oidc.user:*` key in `sessionStorage` and call `/api/...` relative to `E2E_BASE_URL` with Bearer + HAL-FORMS headers.
- [x] 4.2 Add the scenario "Manager updates a DRAFT event" with `Given a draft event "<name>" exists` created through the API, then edit through the UI and assert the change in the detail page.
- [x] 4.3 Run the full suite against target A on `:3000` and `:8443`.

## 5. Manual CI workflow (target B)

- [x] 5.1 Add `.github/workflows/e2e-tests.yml`: `workflow_dispatch` with `image_tag` input, GHCR login, `docker run` of `ghcr.io/zabiny/klabis-be:<tag>` with `h2,ssl,example-data` and the `KLABIS_*` env values copied from `runLocalEnvironment.sh` (without `local-dev`), and a health wait on `/actuator/health`.
- [x] 5.2 Install Node, dependencies and Chromium. Run `npm run test:e2e` with `E2E_BASE_URL=https://localhost:8443`. Always upload `playwright-report/` and the container log.
- [ ] 5.3 Trigger the workflow manually on `latest` and confirm a green run. Confirm the workflow does not run on PR or push.

## 6. Documentation

- [x] 6.1 Document E2E in `frontend/CLAUDE.md`: purpose, `npm run test:e2e`, `E2E_BASE_URL` and credential env vars, folder layout, run prefix rule, `Given`-via-API / `When`-`Then`-via-UI rule, `@spec:` tag and verbatim scenario titles, reusable step vocabulary, and that E2E never runs as part of `npm test`.
- [x] 6.2 Add a short pointer in root `CLAUDE.md` (how to run against a local instance, and the manual CI workflow).
