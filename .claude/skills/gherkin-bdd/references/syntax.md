# Portable Gherkin syntax

Covers the Gherkin 6+ grammar as defined by the official Cucumber parser, which is the parser (or grammar) used by Cucumber (JVM/JS/Ruby/Go…), playwright-bdd, Reqnroll/SpecFlow, and closely followed by Behave and Behat. Stay within this to keep feature files runner-agnostic.

## Contents
1. File layout
2. Keywords
3. Step arguments (data tables, doc strings)
4. Tags
5. Comments and descriptions
6. Languages
7. Portability rules and pitfalls

## 1. File layout

```gherkin
# language: en
# (optional language header, must be the first line)
@capability-tag
Feature: Short capability name
  Free-text description (rules, context). Ignored at runtime, shown in reports.

  Background:
    Given ...

  Rule: A testable business rule

    # optional, one Background per Rule
    Background:
      Given ...

    Scenario: Behaviour name
      Given ...
      When ...
      Then ...

    Scenario Outline: Behaviour name with <param>
      Given ...<param>...

      Examples: Optional group name
        | param |
        | value |
```

- One `Feature` per `.feature` file, as the first keyword (after optional tags/comments/language header).
- Indentation: 2 spaces per level is the convention; spaces or tabs are both legal.
- Colons: `Feature:`, `Rule:`, `Background:`, `Scenario:`, `Scenario Outline:`, `Examples:` take a colon; step keywords (`Given`, `When`, `Then`, `And`, `But`, `*`) do **not**. A misplaced colon makes the parser ignore or reject lines.

## 2. Keywords

| Keyword | Synonyms | Purpose |
|---|---|---|
| `Feature` | `Business Need`, `Ability` | Capability under specification; groups rules/scenarios |
| `Rule` | – | One business rule; groups its examples (Gherkin 6+) |
| `Scenario` | `Example` | One concrete example of a rule |
| `Background` | – | `Given` steps run before each scenario of the Feature/Rule |
| `Scenario Outline` | `Scenario Template` | Scenario run once per row of `Examples` |
| `Examples` | `Scenarios` | Data rows for an outline |
| `Given`, `When`, `Then`, `And`, `But` | – | Steps |
| `*` | – | Bullet step (any type), occasionally useful for list-like Givens |

Prefer the canonical forms (`Feature`, `Scenario`, `Scenario Outline`, `Examples`). Synonyms are valid but less familiar to readers and less consistently supported by editors/linters. `Example` is fine when the team deliberately uses Example-Mapping vocabulary — just be consistent.

Semantics worth knowing:
- `And`/`But` inherit the type of the preceding step; they exist only for readability.
- The keyword is **not** part of step matching: `Given x` and `Then x` match the same definition.
- Only one `Background` per Feature and one per Rule. Feature Background runs before Rule Background.
- `Background` must come before the first `Scenario`/`Rule` at its level.
- `Rule` may contain `Background`, `Scenario`, `Scenario Outline` but not another `Rule`.
- An outline must have at least one `Examples` block; multiple `Examples` blocks (each optionally tagged/named) are allowed.
- `<placeholder>` substitution happens in step text, data tables, doc strings and the scenario title, before step matching.

## 3. Step arguments

### Data table
Attached to the step directly above it; passed as that step's last argument.

```gherkin
Given these books are on loan:
  | title      | due date   |
  | Dune       | 2026-03-01 |
  | The Hobbit | 2026-03-15 |
```
- Escapes inside cells: `\|` pipe, `\n` newline, `\\` backslash.
- Header row is a convention, not a requirement; whether the step reads it as list, map or objects is up to the step definition.
- Keep columns to what matters for the behaviour; align pipes.

### Doc string
For multi-line text (an email body, a JSON payload when the payload *is* the business artefact).

```gherkin
Then Andrew receives an email:
  """
  Your reservation for "Dune" is ready for pick-up.
  """
```
- Delimit with `"""` — the backtick form ```` ``` ```` and the media-type annotation (`"""json`) are newer and not supported by every tool/editor; use them only if all runners in use support them.
- Indentation is relative to the opening delimiter.

## 4. Tags

```gherkin
@payments @spec-reservations
Feature: ...
```
- Allowed above `Feature`, `Rule`, `Scenario`, `Scenario Outline`, `Examples`. Not on `Background` or steps.
- Inherited downward (Feature → Rule → Scenario → Examples rows).
- Format: `@` + no spaces. Convention: lowercase, hyphenated, short (`@smoke`, `@spec-membership-fees`). Some runners attach meaning to specific tags (e.g. skip/only/fixme/timeouts) — treat those as runner configuration and keep them out of feature files when portability matters, or isolate them in a documented convention.
- Typical legitimate uses: traceability to specs/tickets, suite selection (`@smoke`, `@slow`), work-in-progress markers (`@wip`), environment selection. Keep the set small and documented.

## 5. Comments and descriptions

- `#` comments must start a line (leading whitespace ok). No inline or block comments: `Given x # note` makes `# note` part of the step text.
- Descriptions: free text lines directly under `Feature:`, `Rule:`, `Scenario:`, `Background:`, `Examples:` (until the first step/keyword). Use them for the rule's prose; they appear in reports.

## 6. Languages

- Gherkin keywords are localised into 70+ languages; set via `# language: <code>` on line 1 (e.g. `# language: cs` → `Požadavek`, `Scénář`, `Pokud`, `Když`, `Pak`, `A`, `Ale`).
- Write the specs in the language domain experts use. Don't mix languages within a file (keywords and step text should agree).
- The step definitions must be written for the same language text.

## 7. Portability rules and pitfalls

- Use only Gherkin grammar constructs. No runner-specific syntax in step text (e.g. no fixture names, page-object names, selectors, regex fragments, template expressions).
- `Rule` requires Gherkin 6+ parsers (Cucumber ≥ 4/6 era, playwright-bdd, Reqnroll, SpecFlow 3.x+, Behave 1.2.7+). If an older runner must be supported, check before using it.
- Use `"""` doc strings, not backticks, for maximum compatibility.
- Keep placeholders as `<name>` with simple names (letters, digits, spaces/underscores); avoid special characters.
- Quote variable values in step text with double quotes — the most broadly matched convention (`{string}` in Cucumber Expressions, `"([^"]*)"` in regex definitions).
- Avoid relying on `*` steps and keyword synonyms if the team's linter or editor plugin doesn't know them.
- Gherkin has no conditionals, loops, variables or includes. Needing them is a design smell: split scenarios, use an outline, or move the logic into step definitions.
- Empty `Scenario`s (title only) are legal and can mark a documented-but-not-yet-formulated example; many runners report them as undefined/pending.
