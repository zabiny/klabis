---
name: gherkin-bdd
description: Write, review and refactor Gherkin (.feature) BDD scenarios that read as living documentation — declarative, one behaviour per scenario, business language, concrete data, organised by Rule. Use this skill whenever creating or editing any .feature file, turning acceptance criteria / user stories / spec scenarios into Given-When-Then, reviewing BDD or E2E scenarios, or when the user mentions Gherkin, Cucumber, BDD, feature files, Scenario Outline, Examples tables, step phrasing or example mapping — even if they only say "add an E2E scenario" or "write acceptance tests". Covers portable Gherkin only (works with Cucumber, playwright-bdd, Reqnroll/SpecFlow, Behave, Behat, Karate…), not step-definition code.
---

# Gherkin BDD

Gherkin scenarios are **specifications by example** first and tests second. A good scenario lets someone who has never seen the feature understand one business rule in a few seconds, and keeps passing unchanged while the UI, API or database underneath is rewritten. Every rule below follows from those two goals: *readable by the business* and *stable against implementation change*.

Stick to portable Gherkin (see `references/syntax.md`) — no runner-specific tags, keywords or step-text tricks — so the same `.feature` files work across Cucumber, playwright-bdd, Reqnroll, Behave, Behat, etc.

## Workflow

1. **Read what already exists.** Look at existing `.feature` files and step phrasing in the repo. Step text is matched literally against step definitions, so reusing established phrasing (with the same quoting, person and tense) keeps the step vocabulary small. If existing phrasing is imperative or UI-centric, flag it rather than silently copying the style into new scenarios.
2. **Find the rules.** From the story / spec / acceptance criteria, list the business rules (constraints, calculations, permissions, state transitions). One rule usually yields 1–4 examples. If a single rule needs many examples, it probably hides several rules — split it. Unanswered questions about expected behaviour are questions for the user, not something to guess in a scenario.
3. **Pick concrete examples per rule.** Choose the smallest set that illustrates the rule: the typical case, the boundary (just inside / just outside), and the interesting failure. Each example must differ in *behaviour*, not just in data.
4. **Name each example** after the behaviour it shows — a reader skimming titles alone should learn the rules ("Reservation is refused when the member already has two", not "Reserve book 3" or "Test case 7").
5. **Formulate** Given / When / Then declaratively (rules below).
6. **Review** against the checklist at the end. For bigger rewrites, compare with the before/after examples in `references/examples.md`.

## Core rules

### Describe behaviour, not interaction (declarative over imperative)
Write *what* the actor achieves, not *how* they click. Ask: "Would this line change if the UI were redesigned, or if this became an API/CLI?" If yes, it's implementation — push it down into step definitions.

```gherkin
# Imperative — brittle, long, hides the rule
When I fill "Email" with "frieda@example.com"
And I fill "Password" with "secret"
And I click "Log in"
And I click "Articles"
Then I see "Free article 1"
And I do not see "Paid article 1"

# Declarative — states the rule
Given Frieda has a free subscription
When Frieda opens the article list
Then she sees only free articles
```

Avoid UI vocabulary in steps: *click, button, field, page, link, dropdown, scroll, wait, URL, selector*. Exception: when the UI interaction **is** the behaviour under specification (keyboard navigation, accessibility, drag-and-drop ordering), interaction wording is the business language.

### One scenario, one behaviour
A scenario has exactly one `When` (possibly with `And`) followed by `Then`s. A second `When … Then` pair means a second behaviour — split it, and put what the first part established into the second scenario's `Given`. Scenarios that check several rules fail for unclear reasons and can't serve as documentation.

### Given = state, When = event, Then = observable outcome
- **Given** puts the world into a known state. Phrase it as a fact that is true ("Andrew has two active reservations"), not as a journey ("Andrew goes to the page and reserves two books"). No user interaction here.
- **When** is the single action or event — by a person, another system, or time ("the invoice becomes 30 days overdue").
- **Then** asserts something an actor or external system can **observe**: what the user sees, a message sent, a document produced, an error returned. Never assert internal state (database rows, internal flags) — that couples the spec to the implementation.
- Order is strictly Given → When → Then. Never `Given` after `When`, never `When` after `Then`. Gherkin has no "Or": variants become separate scenarios or an outline.

### Essential detail only, but concrete
- Include exactly the data that influences the outcome; hide everything else in step definitions. If the scenario is about a balance, the password does not belong in it.
- Use **concrete, realistic values** instead of vague states: `Given the cart total is 99.99 EUR`, not `Given the cart total is below the limit`. Concrete values expose boundaries and hidden assumptions.
- Don't depend on pre-existing production data ("customer 1234 exists") — the `Given` creates or names what it needs.
- Prefer memorable personas over labels (`Free Frieda`, `Paid Patty` rather than `User A`, `test_user_2`). A persona can carry its incidental data (credentials, address) implicitly.

### Keep it brief
Aim for 3–5 steps; investigate anything over ~7 — it is usually imperative detail or two behaviours. A feature with more than ~12 scenarios usually covers more than one capability. Steps should stay under ~100 characters.

### Each scenario stands alone
Every scenario must pass when run alone, in any order, in parallel. It sets up everything it needs in `Given` (or `Background`) and never relies on another scenario's side effects. Generate unique names/ids where data collides; that is a step-definition concern, not something to spell out in the text.

### Business language, ubiquitous and unambiguous
Use the domain's terms, the same ones the business and the specs use, and use each term consistently. Watch overloaded words (*user, account, date, address, status*) — qualify them when ambiguous ("billing address", "registration date"). Write the feature in the language the domain experts speak (`# language:` header), don't translate.

## Phrasing conventions

- **Present tense** everywhere. `Then the reservation is refused`, not `will be refused` / `should be refused`. Given describes a state (`the order is paid`), not a past action (`the user paid`).
- **Full subject–predicate sentences**, no fragments: `Then Andrew is charged 1.00 USD`, not `Then charged 1.00`.
- **Person**: be consistent within a project. Named personas in the third person scale best (multiple actors, clear who acts). First person (`I`) is acceptable for single-actor suites if used consistently. Don't mix within a scenario.
- **No conjunction steps**: `Given Andrew has shades and a Mustang` → two steps joined by `And`. One step, one fact or action.
- **Parameters in double quotes** (`"Andrew"`, `"2024-03-01"`) when the value varies; it is the most widely matched convention across step-definition styles. Don't quote fixed words of the step.
- **Distinct step texts**: keywords are ignored in matching, so `Given the order is paid` and `Then the order is paid` are the *same* step. Make setup and assertion wording different where the meaning differs (`Given a paid order` / `Then the order is marked as paid`).
- Lowercase step text (except proper nouns), no trailing punctuation, capitalised keywords, 2-space indentation, aligned table pipes.

## Structure

```gherkin
@billing
Feature: Book reservations
  Members can reserve books that are currently on loan.
  A reservation costs 1.00 USD per item.

  Background:
    Given Andrew is a library member

  Rule: Each reservation is charged 1.00 USD per item

    Scenario: Reserving a single book
      When Andrew reserves "The Hobbit"
      Then Andrew is charged 1.00 USD

  Rule: A member can hold at most two active reservations

    Scenario: The third reservation is refused
      Given Andrew has active reservations for "Dune" and "Emma"
      When Andrew reserves "The Hobbit"
      Then the reservation is refused because the limit of 2 is reached
```

- **One `Feature` per file**, named after a capability ("Book reservations"), not a screen or a ticket number.
- **Feature description** states the business rules and context. Skip the boilerplate `As a … I want … so that …` unless it says something non-obvious — the rules are the useful part.
- **`Rule`** groups scenarios by the business rule they illustrate. Use it whenever a feature covers more than one rule; the rule text should be a testable statement.
- **`Background`** only for short (≤ ~4 lines) `Given` context that *every* scenario under that Feature/Rule truly needs and that a reader benefits from seeing. If only some scenarios need it, or it is long and technical, put it into the scenarios or a higher-level step. Never put `When`/`Then` in a Background.
- **Tags**: lowercase, hyphenated, a small agreed set (traceability to specs/tickets, suite selection). Tags are inherited from Feature → Rule → Scenario → Examples.
- **Comments (`#`)** for things the reader of the documentation needs but the scenario can't express (e.g. why a spec scenario is not automated). Not for commented-out steps.

## Scenario Outlines — use sparingly

Use `Scenario Outline` + `Examples` only when the **same behaviour** is illustrated by several rows that each represent a **distinct equivalence class** or boundary, and the rows read as a meaningful table.

```gherkin
Scenario Outline: Shipping is free from 50.00 EUR
  Given the basket total is <total>
  When the customer checks out
  Then the shipping cost is <shipping>

  Examples:
    | total | shipping |
    | 49.99 | 4.90     |
    | 50.00 | 0.00     |
```

Sanity checks:
- Different rows lead to **different behaviours** (success vs. error)? → separate scenarios, possibly under the same `Rule`.
- A column is not used by any step, or two columns never interact? → drop or split.
- Rows that only repeat the same equivalence class? → delete; Gherkin is not a data-driven test harness. Exhaustive combinations belong in unit tests.
- Name `Examples` blocks (`Examples: Below the threshold`) when an outline has several groups.
- Placeholders may also appear in the scenario title, which makes reports readable.

**Data table vs. Examples table**: a *data table* passes structured data to **one step** in one run (`Given these books are on loan:` + table). An *Examples table* runs the **whole scenario** once per row. Don't use an outline when a single scenario with a data table says it better.

## Review checklist

Run through this before finishing any `.feature` work:

- [ ] Title states the behaviour/rule; titles alone document the feature
- [ ] Exactly one `When` block, followed by `Then`; strict Given → When → Then order
- [ ] No UI mechanics (click, field, page, URL, wait, selector) unless the UI interaction is the behaviour
- [ ] `Given` describes state, `Then` asserts something observable (no DB/internal state)
- [ ] Only data that affects the outcome; values concrete and realistic; no reliance on existing prod data
- [ ] 3–5 steps (justify > 7); no conjunction steps; present tense; consistent person
- [ ] Scenario runs alone and in any order
- [ ] Domain terms match the specs/business glossary; ambiguous terms qualified
- [ ] Background short, `Given`-only, needed by all scenarios; Rules used when > 1 rule
- [ ] Outlines only for equivalence classes of one behaviour; no unused columns
- [ ] New step texts reuse existing phrasing where meaning is the same; no two steps differ only in keyword
- [ ] Portable syntax only (see `references/syntax.md`)

## References

- `references/syntax.md` — portable Gherkin syntax: keywords, Rule, Background, Outline/Examples, data tables, doc strings, tags, comments, i18n, portability pitfalls. Read when unsure whether a construct is valid or portable.
- `references/anti-patterns.md` — catalogue of common smells with bad → good snippets. Read when reviewing existing feature files or when a draft "feels off".
- `references/examples.md` — full before/after rewrites (imperative UI script → declarative spec, multi-behaviour scenario → split, outline abuse → rules). Read when rewriting existing scenarios.
