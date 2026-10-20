@spec:membership-fees
Feature: Membership fee level catalog
  The catalog holds the fee levels a membership administrator reuses when starting a fee selection campaign.
  Each level has a yearly fee and payment rules that set the member's contribution towards event entries.
  Jan is a membership administrator, Eva a club member without administration permissions.

  Rule: Administrators maintain the catalog; editing it leaves already published levels unchanged

    Scenario: Administrator creates a fee level
      When Jan creates fee level "Created" with yearly fee 1800 CZK
      Then the catalog lists level "Created" with yearly fee 1800 CZK
      And catalog level "Created" has no payment rules

    Scenario: Administrator edits a catalog level
      Given an active fee selection campaign offers level "Edited" with yearly fee 1000 CZK
      When Jan changes the yearly fee of catalog level "Edited" to 1234 CZK
      Then the catalog lists level "Edited" with yearly fee 1234 CZK
      And the published level "Edited" has yearly fee 1000 CZK

    Scenario: Non-administrator cannot manage the catalog
      Given a catalog fee level "Member view" exists
      When Eva opens the membership fees page
      Then the membership fees page lists catalog level "Member view"
      And Eva is not offered to add a fee level
      And Eva is not offered to manage catalog level "Member view"

  Rule: A payment rule sets the contribution for one event type and ranking, as a percentage or a fixed amount

    Scenario: Administrator defines a percentage rule
      Given a catalog fee level "Percentage" exists
      When Jan adds a 50 % rule for races of ranking "Rank" to catalog level "Percentage"
      Then catalog level "Percentage" has a 50 % rule for races of ranking "Rank"

    Scenario: Administrator defines a fixed amount rule
      Given a catalog fee level "Fixed" exists
      When Jan adds a 300 CZK rule for races of ranking "Rank" to catalog level "Fixed"
      Then catalog level "Fixed" has a 300 CZK rule for races of ranking "Rank"

    Scenario: Rule combination is unique within a level
      Given a catalog fee level "Duplicate" exists
      And catalog level "Duplicate" includes a 50 % rule for races of ranking "Rank"
      When Jan adds a 20 % rule for races of ranking "Rank" to catalog level "Duplicate"
      Then the rule is refused
      And catalog level "Duplicate" has no 20 % rule for races of ranking "Rank"
