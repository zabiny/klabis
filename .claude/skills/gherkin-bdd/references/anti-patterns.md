# Gherkin anti-patterns

Each entry: symptom → why it hurts → fix. Use when reviewing feature files or when a draft "feels off".

## Contents
1. Test script in disguise (imperative UI steps)
2. Multiple behaviours in one scenario
3. Incidental detail
4. Vague / abstract data
5. Dependency on existing or production data
6. Asserting internal state
7. Given as a journey
8. Conjunction steps
9. Mixed abstraction levels
10. Bad or missing scenario names
11. Boilerplate feature description
12. Scenario coupling (order dependence)
13. Scenario Outline abuse
14. Bloated or irrelevant Background
15. Duplicate step text across keywords
16. Tense, person and grammar drift
17. "Or" / conditional logic
18. Code in feature files
19. Feature-coupled or ticket-shaped organisation
20. Writing scenarios after the code / in isolation

---

## 1. Test script in disguise
**Symptom:** `click`, `fill`, `button`, `page`, `navigate to /url`, `wait 2 seconds`, CSS ids in steps; 10+ steps.
**Why:** Breaks on every UI change, hides the rule, unreadable for business, slow to diagnose.
```gherkin
# Bad
When I navigate to "/login"
And I fill "username" with "bob"
And I fill "password" with "secret"
And I click "Sign in"
And I click "Orders"
Then I see "Order #1001"
# Good
Given Bob has placed order "1001"
When Bob views his orders
Then order "1001" is listed
```

## 2. Multiple behaviours in one scenario
**Symptom:** `When … Then … When … Then`; title with "and"; checks of several rules.
**Why:** A failure doesn't say which rule broke; the scenario can't be cited as documentation of one rule.
**Fix:** One `When`/`Then` pair per scenario. The outcome of the first part becomes the `Given` of the second scenario.

## 3. Incidental detail
**Symptom:** Passwords, addresses, timestamps, IDs, extra fields that don't affect the outcome.
**Why:** Noise hides the essential data; changes to irrelevant data break the scenario.
**Fix:** Keep only data that changes the outcome. Move the rest into step definitions or into a persona ("Given Paid Patty has a basic subscription").

## 4. Vague / abstract data
**Symptom:** `Given the user has a valid account`, `Given the amount is too high`, `Then it works correctly`.
**Why:** Hides the boundary; readers can't verify the rule; vague `Then`s pass without checking anything real.
**Fix:** Concrete values that reveal the rule: `Given the withdrawal limit is 500.00 EUR` / `When Ann withdraws 500.01 EUR` / `Then the withdrawal is refused`.

(Balance with #3: concrete for data that matters, absent for data that doesn't.)

## 5. Dependency on existing or production data
**Symptom:** `Given customer 1234 exists` where 1234 is assumed to be in the database already.
**Why:** Breaks when the data changes; meaningless to readers.
**Fix:** The `Given` creates/establishes the data it needs, using meaningful names.

## 6. Asserting internal state
**Symptom:** `Then the orders table contains a row with status 3`, `Then the flag "isPaid" is true`.
**Why:** Couples the spec to the implementation; the user can't observe it.
**Fix:** Assert what an actor or external system observes: `Then the order is shown as paid`, `Then the customer receives a payment confirmation`.

## 7. Given as a journey
**Symptom:** `Given I log in and go to settings and open the profile tab`; UI interaction in `Given`.
**Why:** `Given` is context, not action; journeys are slow and brittle.
**Fix:** State the resulting fact: `Given Ann is signed in` / `Given Ann's profile has no phone number`.

## 8. Conjunction steps
**Symptom:** `Given I have shades and a brand new Mustang`.
**Why:** Over-specialised, not reusable, step vocabulary explodes.
**Fix:** `Given I have shades` + `And I have a brand new Mustang`. If several preconditions always go together, name the concept instead (`Given Ann is a premium member`).

## 9. Mixed abstraction levels
**Symptom:** `Given Ann is a premium member` next to `And I POST {"x":1} to /api/orders` next to `And I click "Pay"`.
**Why:** Readers switch between domain and mechanics; the scenario fits neither audience.
**Fix:** Keep every step at domain level; mechanics belong in step definitions.

## 10. Bad or missing scenario names
**Symptom:** `Scenario: Test 1`, `Scenario: Login, add item, checkout, verify` (lists steps), duplicated titles.
**Why:** Titles are what reports, IDEs and readers see first.
**Fix:** Name the behaviour or rule outcome: `Scenario: Checkout is refused when the basket is empty`. "The one where …" style is a good mental test while drafting.

## 11. Boilerplate feature description
**Symptom:** `As a user I want to check my balance so that I know my balance`.
**Why:** Says nothing.
**Fix:** Describe the business rules and context, or omit the description; use `Rule:` for the rules themselves.

## 12. Scenario coupling
**Symptom:** Scenario B uses an entity created in scenario A; scenarios only pass in file order.
**Why:** Fails with parallel/random order or when running one scenario; failures cascade.
**Fix:** Every scenario establishes its own state in `Given`/`Background`.

## 13. Scenario Outline abuse
**Symptoms:**
- Rows that are the same equivalence class (`1`, `2`, `3`, `4` all "valid").
- Rows with different behaviours (some succeed, some error) driven by an `<outcome>` column with steps like `Then the result is <result>`.
- Unused columns, cartesian products, dozens of rows.
- An outline with a single row.
**Why:** Produces many meaningless, slow tests; hides which row is the important boundary; a generic `Then` hides the behaviour.
**Fix:** Keep rows that each show a distinct class/boundary of the same behaviour. Split success and failure behaviours into separate scenarios (or separate named `Examples` blocks with clearly different titles). Push exhaustive combinations down to unit tests.

## 14. Bloated or irrelevant Background
**Symptom:** 6+ lines; steps only some scenarios need; `When`/`Then` in Background; technical setup.
**Why:** Readers forget it while reading scenarios; hidden setup makes failures confusing.
**Fix:** ≤ ~4 short `Given` lines needed by every scenario. Otherwise move into scenarios, split into Rules/Features, or collapse into one higher-level domain step.

## 15. Duplicate step text across keywords
**Symptom:** `Given the order is paid` (setup) and `Then the order is paid` (assertion).
**Why:** Keywords are ignored when matching — both hit the same definition, which can't both set up and assert.
**Fix:** Distinct wording: `Given a paid order "1001"` vs. `Then order "1001" is shown as paid`.

## 16. Tense, person and grammar drift
**Symptom:** `Given the user logged in`, `When he will click`, `Then it should be displayed`; mixing `I` and `the user`; fragments like `Then error`.
**Fix:** Present tense, full sentences, one consistent person (prefer named personas), correct spelling.

## 17. "Or" / conditional logic
**Symptom:** `When Ann presses "A" or "B"`, `Then if premium the discount is 10%`.
**Why:** Gherkin has no branching; each scenario is one deterministic path.
**Fix:** Separate scenarios or an outline row per variant.

## 18. Code in feature files
**Symptom:** SQL, JSON fixtures, regexes, selectors, JS expressions in steps or tables.
**Fix:** Keep them in step definitions/fixtures. Exception: a payload that *is* the business artefact (an exported document, a public API contract) may appear in a doc string.

## 19. Feature-coupled or ticket-shaped organisation
**Symptom:** Files named `JIRA-1234.feature`, `sprint-5.feature`, one file per screen; steps phrased so they only fit one feature ("on the edit description page").
**Why:** Living documentation should be navigable by capability; feature-specific phrasing prevents step reuse.
**Fix:** One feature file per business capability; domain-oriented, reusable step phrasing; tickets/specs referenced by tag.

## 20. Writing scenarios after the code / in isolation
**Symptom:** Scenarios merely describe what was built; one person writes them alone.
**Why:** They stop testing shared understanding and miss edge cases a second perspective would raise.
**Fix:** Derive scenarios from rules and examples agreed before implementation (Example Mapping: story → rules → examples → questions). When generating scenarios as an assistant, surface open questions to the user instead of inventing expected behaviour.
