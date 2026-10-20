# Before / after rewrites

Full rewrites showing how the rules combine. Use as models when refactoring existing feature files.

## Contents
1. Imperative UI script → declarative specification
2. Multi-behaviour scenario → focused scenarios grouped by Rule
3. Outline abuse → boundaries per rule
4. Permissions scenario with negative checks
5. From example map to feature file

---

## 1. Imperative UI script → declarative specification

**Before**
```gherkin
Feature: Articles

  Scenario: Test free user
    Given users with a free subscription can access "FreeArticle1" but not "PaidArticle1"
    When I type "free@example.com" in the email field
    And I type "validPassword123" in the password field
    And I press the "Submit" button
    Then I see "FreeArticle1" on the home page
    And I do not see "PaidArticle1" on the home page
```
Problems: UI mechanics, credentials as incidental detail, vague title, rule hidden in a long Given.

**After**
```gherkin
Feature: Article access by subscription level
  Free subscribers read free articles only; paid subscribers read all articles.

  Scenario: Free subscribers see only free articles
    Given Free Frieda has a free subscription
    When Free Frieda opens the article list
    Then she sees the free articles
    And she does not see the paid articles

  Scenario: Paid subscribers see free and paid articles
    Given Paid Patty has a basic paid subscription
    When Paid Patty opens the article list
    Then she sees the free articles and the paid articles
```
The which-article-is-paid detail and the login mechanics live in step definitions; adding a new subscription tier adds a scenario but changes none.

## 2. Multi-behaviour scenario → focused scenarios grouped by Rule

**Before**
```gherkin
Scenario: Reservations
  Given Andrew is logged in
  When he reserves "Dune"
  Then he is charged 1.00 USD
  When he reserves "Emma"
  Then he is charged 1.00 USD
  When he reserves "The Hobbit"
  Then he sees an error
```

**After**
```gherkin
Feature: Book reservations

  Background:
    Given Andrew is a library member

  Rule: Each reservation is charged 1.00 USD

    Scenario: Reserving a book charges the reservation fee
      When Andrew reserves "Dune"
      Then Andrew is charged 1.00 USD

  Rule: A member can hold at most two active reservations

    Scenario: A second reservation is accepted
      Given Andrew has an active reservation for "Dune"
      When Andrew reserves "Emma"
      Then the reservation for "Emma" is confirmed

    Scenario: A third reservation is refused
      Given Andrew has active reservations for "Dune" and "Emma"
      When Andrew reserves "The Hobbit"
      Then the reservation is refused because the limit of 2 is reached
```
Each scenario fails for exactly one reason; "sees an error" became a concrete, observable outcome.

## 3. Outline abuse → boundaries per rule

**Before**
```gherkin
Scenario Outline: Password validation
  When a user registers with password "<password>"
  Then the result is "<result>"

  Examples:
    | password       | result  |
    | abc            | error   |
    | abcd           | error   |
    | abcde          | error   |
    | Abcdef1!       | success |
    | Abcdefg1!      | success |
    | Abcdefgh1!     | success |
    | abcdefgh1!     | error   |
    | ABCDEFGH1!     | error   |
```
Problems: success and failure mixed behind a generic `Then`; redundant rows; which rule each error row illustrates is invisible.

**After**
```gherkin
Feature: Password policy

  Rule: Passwords have at least 8 characters

    Scenario Outline: Password length boundary
      When Nina registers with password "<password>"
      Then the registration is <outcome>

      Examples:
        | password | outcome                                    |
        | Abcde1!  | refused because the password is too short  |
        | Abcdef1! | accepted                                   |

  Rule: Passwords mix upper- and lowercase letters

    Scenario: A password without uppercase letters is refused
      When Nina registers with password "abcdefg1!"
      Then the registration is refused because an uppercase letter is missing

    Scenario: A password without lowercase letters is refused
      When Nina registers with password "ABCDEFG1!"
      Then the registration is refused because a lowercase letter is missing
```
The outline is kept only where rows are the two sides of one boundary, and the outcome column states the behaviour in words.

## 4. Permissions scenario with negative checks

**Before**
```gherkin
Scenario: Member cannot manage catalog
  Given I am logged in as "member"
  When I go to "/catalog"
  Then I see "Gold tier"
  And I do not see "Add tier"
  When I click "Gold tier"
  Then I do not see "Edit"
  And I do not see "Delete"
```

**After**
```gherkin
Rule: Only administrators manage the fee catalog

  Scenario: A member can browse the catalog but not change it
    Given the catalog contains the tier "Gold"
    When member Martin opens the fee catalog
    Then Martin sees the tier "Gold"
    And Martin cannot add, edit or delete tiers
```
The negative check is one observable outcome ("cannot manage"); how absence is verified (and that the page rendered first) is a step-definition concern. If editing a single tier needs its own spec, add a separate scenario with its own `When`.

## 5. From example map to feature file

Example map (story → rules → examples → questions):
- Story: *Late fees for overdue books*
- Rule: *0.10 USD per overdue day* — examples: "returned one day late", "returned on time"
- Rule: *Late fee is capped at 5.00 USD* — example: "returned 80 days late"
- Question: *Do weekends count?* → **ask before formulating; don't guess**

```gherkin
Feature: Late fees for overdue books

  Rule: Overdue books cost 0.10 USD per day

    Scenario: A book returned one day late
      Given Andrew borrowed "Dune" due on 2026-03-01
      When Andrew returns "Dune" on 2026-03-02
      Then Andrew owes a late fee of 0.10 USD

    Scenario: A book returned on the due date
      Given Andrew borrowed "Dune" due on 2026-03-01
      When Andrew returns "Dune" on 2026-03-01
      Then Andrew owes no late fee

  Rule: The late fee is capped at 5.00 USD

    Scenario: A very late return is charged the cap
      Given Andrew borrowed "Dune" due on 2026-03-01
      When Andrew returns "Dune" on 2026-05-20
      Then Andrew owes a late fee of 5.00 USD
```
Story card → Feature, rule cards → `Rule`, example cards → `Scenario` (named after the example), question cards → asked, never formulated.
