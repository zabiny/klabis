@spec:membership-fees
Feature: Membership fee type catalog
  The catalog holds the fee types a membership administrator reuses when starting a fee selection campaign.
  Each type has a yearly fee and payment rules that set the member's contribution towards event entries.
  Jan is a membership administrator, Eva a club member without administration permissions.

  Rule: Administrators maintain the catalog; editing it leaves already published types unchanged

    Scenario: Administrator creates a fee type
      When Jan creates fee type "Created" with yearly fee 1800 CZK
      Then the catalog lists type "Created" with yearly fee 1800 CZK
      And catalog type "Created" has no payment rules

    Scenario: Administrator edits a catalog type
      Given an active fee selection campaign offers type "Edited" with yearly fee 1000 CZK
      When Jan changes the yearly fee of catalog type "Edited" to 1234 CZK
      Then the catalog lists type "Edited" with yearly fee 1234 CZK
      And the published type "Edited" has yearly fee 1000 CZK

    Scenario: Non-administrator cannot manage the catalog
      Given a catalog fee type "Member view" exists
      When Eva opens the membership fees page
      Then the membership fees page lists catalog type "Member view"
      And Eva is not offered to add a fee type
      And Eva is not offered to manage catalog type "Member view"

  Rule: A payment rule sets the contribution for one event type and ranking, as a percentage or a fixed amount

    Scenario: Administrator defines a percentage rule
      Given a catalog fee type "Percentage" exists
      When Jan adds a 50 % rule for races of ranking "Rank" to catalog type "Percentage"
      Then catalog type "Percentage" has a 50 % rule for races of ranking "Rank"

    Scenario: Administrator defines a fixed amount rule
      Given a catalog fee type "Fixed" exists
      When Jan adds a 300 CZK rule for races of ranking "Rank" to catalog type "Fixed"
      Then catalog type "Fixed" has a 300 CZK rule for races of ranking "Rank"

    Scenario: Rule combination is unique within a type
      Given a catalog fee type "Duplicate" exists
      And catalog type "Duplicate" includes a 50 % rule for races of ranking "Rank"
      When Jan adds a 20 % rule for races of ranking "Rank" to catalog type "Duplicate"
      Then the rule for races of ranking "Rank" is refused as a duplicate of an existing one
      And catalog type "Duplicate" has no 20 % rule for races of ranking "Rank"
